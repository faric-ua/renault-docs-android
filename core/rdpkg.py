from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import unicodedata
import re
from pathlib import Path
from typing import Any, Callable
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from core.dataset_manifest import load_manifest
from core.dataset_package import build_dataset_package
from core.volumes import discover_volumes


RDPKG_SCHEMA_VERSION = 1
RDPKG_FORMAT = "renault-volume-package-v1"
RDPKG_MANIFEST = "rdpkg.json"
RDPKG_SUFFIX = ".rdpkg"

ProgressCallback = Callable[[str], None]


def list_packageable_volumes(
    prepared_root: Path,
) -> list[dict[str, Any]]:
    prepared_root = prepared_root.expanduser().resolve()
    _require_prepared_root(prepared_root)
    return [
        volume
        for volume in discover_volumes(
            prepared_root,
        )
        if not _is_synthetic_volume(
            volume,
        )
    ]


def build_rdpkg(
    prepared_root: Path,
    output_path: Path,
    *,
    volume_selector: str | None = None,
    progress: ProgressCallback | None = None,
) -> dict[str, Any]:
    prepared_root = prepared_root.expanduser().resolve()
    output_path = output_path.expanduser().resolve()

    manifest_path = _require_prepared_root(prepared_root)
    source_manifest = load_manifest(manifest_path)
    volumes = list_packageable_volumes(
        prepared_root,
    )
    volume = select_volume(
        volumes,
        volume_selector,
    )

    source_folder = str(volume["source_folder"])
    source_volume_root = prepared_root / source_folder

    _emit(
        progress,
        "RDPKG: готую один том: "
        + _volume_label(volume),
    )

    with tempfile.TemporaryDirectory(
        prefix="renault-rdpkg-",
    ) as temp_dir:
        staging = Path(temp_dir) / "dataset"
        staging.mkdir(parents=True)

        _emit(
            progress,
            "RDPKG: копіюю файли тому...",
        )

        shutil.copytree(
            source_volume_root,
            staging / source_folder,
            copy_function=shutil.copy2,
        )

        dataset = _single_volume_dataset_metadata(
            source_manifest,
            volume,
        )

        _emit(
            progress,
            "RDPKG: будую single-volume manifest / Modern / Runtime / Fast Pack...",
        )

        package_result = build_dataset_package(
            dataset,
            staging,
            {
                "files_total": _count_files(
                    staging / source_folder,
                ),
                "changed_files": 0,
                "changes_total": 0,
            },
            progress=progress,
        )

        rebuilt_volumes = package_result["volumes"]
        if len(rebuilt_volumes) != 1:
            raise ValueError(
                "Single-volume staging має містити рівно один том, знайдено: "
                + str(len(rebuilt_volumes))
            )

        rebuilt_volume = rebuilt_volumes[0]
        package_id = _package_id(
            dataset,
            rebuilt_volume,
        )

        payload_files = sorted(
            (
                path
                for path in staging.rglob("*")
                if path.is_file()
            ),
            key=lambda path: path
                .relative_to(staging)
                .as_posix()
                .casefold(),
        )
        payload_bytes = sum(
            path.stat().st_size
            for path in payload_files
        )

        package_manifest = {
            "schema_version": RDPKG_SCHEMA_VERSION,
            "format": RDPKG_FORMAT,
            "package_id": package_id,
            "project_id": dataset.get("project_id"),
            "dataset_id": dataset.get("id"),
            "dataset_title": dataset.get("title"),
            "volume": {
                key: rebuilt_volume.get(key)
                for key in (
                    "id",
                    "title",
                    "document_code",
                    "date",
                    "vehicle_codes",
                    "document_type",
                    "document_version",
                    "region",
                    "kind",
                    "source_folder",
                    "entrypoint",
                )
                if rebuilt_volume.get(key) is not None
            },
            "dataset_manifest": "renault-dataset.json",
            "payload_file_count": len(payload_files),
            "payload_bytes": payload_bytes,
        }

        output_path.parent.mkdir(
            parents=True,
            exist_ok=True,
        )

        temporary_output = output_path.with_name(
            "." + output_path.name + ".tmp",
        )
        temporary_output.unlink(
            missing_ok=True,
        )

        try:
            _emit(
                progress,
                "RDPKG: пакую один файл...",
            )

            with ZipFile(
                temporary_output,
                "w",
                compression=ZIP_DEFLATED,
                compresslevel=6,
                allowZip64=True,
            ) as archive:
                _write_text_entry(
                    archive,
                    RDPKG_MANIFEST,
                    json.dumps(
                        package_manifest,
                        ensure_ascii=False,
                        indent=2,
                    ) + "\n",
                )

                total = len(payload_files)

                for index, source in enumerate(
                    payload_files,
                    start=1,
                ):
                    relative = source.relative_to(
                        staging,
                    ).as_posix()

                    _write_file_entry(
                        archive,
                        relative,
                        source,
                    )

                    if (
                        index == 1
                        or index % 1000 == 0
                        or index == total
                    ):
                        _emit(
                            progress,
                            f"RDPKG: пакую {index}/{total}...",
                        )

            digest = _sha256(
                temporary_output,
            )

            output_path.unlink(
                missing_ok=True,
            )
            temporary_output.replace(
                output_path,
            )
        finally:
            temporary_output.unlink(
                missing_ok=True,
            )

    result = {
        "path": output_path,
        "package_id": package_id,
        "project_id": dataset.get("project_id"),
        "volume": rebuilt_volume,
        "sha256": digest,
        "bytes": output_path.stat().st_size,
        "payload_file_count": len(payload_files),
        "payload_bytes": payload_bytes,
    }

    _emit(
        progress,
        "RDPKG: готово — "
        + output_path.name
        + " · "
        + str(result["bytes"])
        + " bytes",
    )

    return result


def select_volume(
    volumes: list[dict[str, Any]],
    selector: str | None,
) -> dict[str, Any]:
    if not volumes:
        raise ValueError(
            "У підготовленому dataset не знайдено томів."
        )

    clean = (selector or "").strip()

    if not clean:
        if len(volumes) == 1:
            return volumes[0]

        raise ValueError(
            "Dataset містить кілька томів. Вкажи --volume. Доступні: "
            + ", ".join(
                _volume_label(volume)
                for volume in volumes
            )
        )

    needle = clean.casefold()
    matches = [
        volume
        for volume in volumes
        if needle in {
            str(volume.get("id") or "").casefold(),
            str(volume.get("document_code") or "").casefold(),
            str(volume.get("source_folder") or "").casefold(),
            str(volume.get("title") or "").casefold(),
        }
    ]

    if len(matches) == 1:
        return matches[0]

    if not matches:
        raise ValueError(
            "Том не знайдено: "
            + clean
            + ". Доступні: "
            + ", ".join(
                _volume_label(volume)
                for volume in volumes
            )
        )

    raise ValueError(
        "Селектор тому неоднозначний: "
        + clean
    )


def default_package_filename(
    dataset: dict[str, Any],
    volume: dict[str, Any],
) -> str:
    model = str(
        dataset.get("model")
        or dataset.get("title")
        or "Renault"
    )

    parts: list[str] = [
        _safe_filename_part(model),
    ]

    vehicle_codes = [
        str(code).strip()
        for code in (volume.get("vehicle_codes") or [])
        if str(code).strip()
    ]
    if vehicle_codes:
        parts.append(
            "-".join(
                _safe_filename_part(code)
                for code in vehicle_codes
            )
        )

    region = str(volume.get("region") or "").strip()
    if region:
        parts.append(
            _safe_filename_part(region)
        )

    parts.append(
        _safe_filename_part(
            str(
                volume.get("document_code")
                or volume.get("id")
                or "volume"
            )
        )
    )

    document_type = str(
        volume.get("document_type")
        or ""
    ).strip()
    document_version = str(
        volume.get("document_version")
        or ""
    ).strip().removeprefix("v")

    if document_type:
        descriptor = document_type
        if document_version:
            descriptor += "-v" + document_version
        parts.append(
            _safe_filename_part(
                descriptor,
            )
        )

    date = volume.get("date")
    if date:
        parts.append(
            _safe_filename_part(
                str(date),
            )
        )

    return "_".join(
        part
        for part in parts
        if part
    ) + RDPKG_SUFFIX


def _single_volume_dataset_metadata(
    source: dict[str, Any],
    volume: dict[str, Any],
) -> dict[str, Any]:
    volume_id = str(
        volume.get("id")
        or "volume"
    )
    source_id = str(
        source.get("id")
        or "renault"
    )

    dataset = {
        "id": source_id + "-" + volume_id,
        "title": source.get("title")
        or source.get("model")
        or "Renault",
        "manufacturer": source.get(
            "manufacturer",
            "Renault",
        ),
        "model": source.get("model")
        or source.get("title")
        or "Renault",
        "project_id": source.get(
            "project_id",
        )
        or _slugify(
            str(
                source.get("model")
                or source.get("title")
                or "Renault"
            )
        ),
        "platform": source.get(
            "platform",
        ),
        "years": source.get(
            "years",
        ),
        "content_type": source.get(
            "content_type",
            "technical-documentation",
        ),
        "entrypoint": volume["entrypoint"],
        "viewer_profile": source.get(
            "viewer_profile",
            "renault-legacy-web-v1",
        ),
    }

    return {
        key: value
        for key, value in dataset.items()
        if value is not None
    }


def _package_id(
    dataset: dict[str, Any],
    volume: dict[str, Any],
) -> str:
    identity = "-".join(
        part
        for part in (
            str(
                dataset.get("project_id")
                or dataset.get("id")
                or "renault"
            ),
            str(
                volume.get("document_code")
                or volume.get("id")
                or "volume"
            ),
            str(
                volume.get("date")
                or ""
            ),
        )
        if part
    )

    return _slugify(
        identity,
    )


def _require_prepared_root(
    prepared_root: Path,
) -> Path:
    if not prepared_root.is_dir():
        raise ValueError(
            "Prepared dataset folder не знайдено: "
            + str(prepared_root)
        )

    manifest_path = (
        prepared_root
        / "renault-dataset.json"
    )

    if not manifest_path.is_file():
        raise ValueError(
            "У prepared dataset немає renault-dataset.json: "
            + str(prepared_root)
        )

    return manifest_path


def _write_text_entry(
    archive: ZipFile,
    name: str,
    value: str,
) -> None:
    info = _zip_info(
        name,
    )
    archive.writestr(
        info,
        value.encode(
            "utf-8",
        ),
    )


def _write_file_entry(
    archive: ZipFile,
    name: str,
    source: Path,
) -> None:
    info = _zip_info(
        name,
    )

    with (
        source.open(
            "rb",
        ) as input_stream,
        archive.open(
            info,
            mode="w",
            force_zip64=True,
        ) as output_stream,
    ):
        shutil.copyfileobj(
            input_stream,
            output_stream,
            length=1024 * 1024,
        )


def _zip_info(
    name: str,
) -> ZipInfo:
    info = ZipInfo(
        filename=name,
        date_time=(
            1980,
            1,
            1,
            0,
            0,
            0,
        ),
    )
    info.compress_type = ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    return info


def _count_files(
    root: Path,
) -> int:
    return sum(
        1
        for path in root.rglob("*")
        if path.is_file()
    )


def _is_synthetic_volume(
    volume: dict[str, Any],
) -> bool:
    if (
        volume.get("document_code")
        or volume.get("date")
    ):
        return False

    source_folder = str(
        volume.get("source_folder")
        or ""
    ).strip().casefold()

    return source_folder in {
        "backup",
        "_renault",
        "packages",
    }


def _volume_label(
    volume: dict[str, Any],
) -> str:
    return " · ".join(
        str(part)
        for part in (
            volume.get("document_code")
            or volume.get("title")
            or volume.get("source_folder"),
            volume.get("date"),
        )
        if part
    )


def _safe_filename_part(
    value: str,
) -> str:
    normalized = unicodedata.normalize(
        "NFKD",
        value,
    )
    ascii_value = normalized.encode(
        "ascii",
        "ignore",
    ).decode(
        "ascii",
    )
    clean = re.sub(
        r"[^A-Za-z0-9._-]+",
        "-",
        ascii_value,
    ).strip(
        "-._",
    )
    return clean or "Renault"


def _slugify(
    value: str,
) -> str:
    return _safe_filename_part(
        value,
    ).lower()


def _sha256(
    path: Path,
) -> str:
    digest = hashlib.sha256()

    with path.open(
        "rb",
    ) as stream:
        for chunk in iter(
            lambda: stream.read(
                1024 * 1024,
            ),
            b"",
        ):
            digest.update(
                chunk,
            )

    return digest.hexdigest()


def _emit(
    progress: ProgressCallback | None,
    message: str,
) -> None:
    if progress is not None:
        progress(
            message,
        )
