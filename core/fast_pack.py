from __future__ import annotations

import hashlib
import shutil
from pathlib import Path
from typing import Any, Callable
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


FAST_PACK_SCHEMA_VERSION = 1
FAST_PACK_PREFIX = "fast-content-"
FAST_PACK_SUFFIX = ".zip"
FAST_PACK_EXTENSIONS = {
    ".htm",
    ".html",
    ".js",
    ".css",
    ".gif",
    ".ico",
    ".png",
    ".jpg",
    ".jpeg",
    ".svg",
    ".json",
}


ProgressCallback = Callable[[str], None]


def build_fast_pack(
    output_root: Path,
    package_root: Path,
    progress: ProgressCallback | None = None,
) -> dict[str, Any]:
    output_root = output_root.resolve()
    package_root = package_root.resolve()
    package_root.mkdir(parents=True, exist_ok=True)

    _emit(
        progress,
        "Fast Pack: сканую dataset...",
    )

    sources: list[Path] = []
    scanned = 0

    for path in output_root.rglob("*"):
        scanned += 1

        if (
            scanned == 1
            or scanned % 5000 == 0
        ):
            _emit(
                progress,
                f"Fast Pack: сканую dataset... {scanned} entries",
            )

        if _should_pack(
            path=path,
            output_root=output_root,
            package_root=package_root,
        ):
            sources.append(path)

    sources.sort(
        key=lambda item: item.as_posix().casefold(),
    )

    _emit(
        progress,
        f"Fast Pack: знайдено {len(sources)} web-файлів.",
    )

    temp_path = package_root / ".fast-content-building.zip"
    temp_path.unlink(missing_ok=True)

    try:
        with ZipFile(
            temp_path,
            "w",
            compression=ZIP_DEFLATED,
            compresslevel=6,
            allowZip64=True,
        ) as archive:
            total = len(sources)

            for index, source in enumerate(
                sources,
                start=1,
            ):
                relative = source.relative_to(
                    output_root,
                ).as_posix()

                info = ZipInfo(
                    filename=relative,
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

                with (
                    source.open("rb") as source_stream,
                    archive.open(
                        info,
                        mode="w",
                        force_zip64=True,
                    ) as archive_stream,
                ):
                    shutil.copyfileobj(
                        source_stream,
                        archive_stream,
                        length=1024 * 1024,
                    )

                if (
                    index == 1
                    or index % 1000 == 0
                    or index == total
                ):
                    _emit(
                        progress,
                        f"Fast Pack: пакую {index}/{total}...",
                    )

        _emit(
            progress,
            "Fast Pack: обчислюю SHA-256...",
        )
        digest = _sha256(temp_path)
        final_name = (
            FAST_PACK_PREFIX
            + digest[:16]
            + FAST_PACK_SUFFIX
        )
        final_path = package_root / final_name

        if final_path.exists():
            final_path.unlink()

        temp_path.replace(final_path)

        for stale in package_root.glob(
            f"{FAST_PACK_PREFIX}*{FAST_PACK_SUFFIX}"
        ):
            if stale != final_path:
                stale.unlink()

        _emit(
            progress,
            f"Fast Pack: готово — {len(sources)} файлів, {final_path.stat().st_size} bytes.",
        )

        return {
            "schema_version": FAST_PACK_SCHEMA_VERSION,
            "format": "zip-web-v1",
            "path": final_path
                .relative_to(output_root)
                .as_posix(),
            "sha256": digest,
            "bytes": final_path.stat().st_size,
            "file_count": len(sources),
        }
    finally:
        temp_path.unlink(missing_ok=True)


def _should_pack(
    path: Path,
    output_root: Path,
    package_root: Path,
) -> bool:
    if not path.is_file():
        return False

    if path.suffix.lower() not in FAST_PACK_EXTENSIONS:
        return False

    if path.name.startswith(FAST_PACK_PREFIX):
        return False

    try:
        relative = path.relative_to(output_root)
    except ValueError:
        return False

    if relative.as_posix() == "renault-dataset.json":
        return False

    try:
        package_relative = path.relative_to(
            package_root,
        )
    except ValueError:
        package_relative = None

    if package_relative is not None:
        if package_relative.name.startswith(
            FAST_PACK_PREFIX
        ):
            return False

        # _renault JSON files are native/compiler metadata, not legacy web
        # runtime assets. Keeping the full Runtime IR tree in Fast Pack can
        # add hundreds of MB and duplicates data that Android reads directly.
        if path.suffix.lower() == ".json":
            return False

    return True


def _sha256(
    path: Path,
) -> str:
    digest = hashlib.sha256()

    with path.open("rb") as stream:
        for chunk in iter(
            lambda: stream.read(
                1024 * 1024,
            ),
            b"",
        ):
            digest.update(chunk)

    return digest.hexdigest()


def _emit(
    progress: ProgressCallback | None,
    message: str,
) -> None:
    if progress is not None:
        progress(message)
