#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from zipfile import BadZipFile, ZipFile

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.rdpkg import (
    RDPKG_FORMAT,
    RDPKG_MANIFEST,
    RDPKG_SCHEMA_VERSION,
    default_package_filename,
)
from core.volumes import infer_volume_identity


DEFAULT_PACKAGE_DIR = Path(
    "/storage/emulated/0/Documents/Renault/packages/rdpkg"
)


@dataclass(frozen=True)
class MigrationItem:
    source: Path
    target: Path
    status: str
    reason: str = ""


def _read_json_entry(
    archive: ZipFile,
    name: str,
) -> dict:
    raw = archive.read(name)
    value = json.loads(raw.decode("utf-8"))
    if not isinstance(value, dict):
        raise ValueError(
            f"{name} must contain a JSON object"
        )
    return value


def _infer_document_type_from_archive(
    archive: ZipFile,
    volume: dict,
) -> str | None:
    entrypoint = str(
        volume.get("entrypoint")
        or ""
    ).strip()

    if not entrypoint:
        return None

    names = archive.namelist()
    exact = entrypoint if entrypoint in names else None

    if exact is None:
        needle = entrypoint.casefold()
        exact = next(
            (
                name
                for name in names
                if name.casefold() == needle
            ),
            None,
        )

    if exact is None:
        return None

    try:
        raw = archive.read(
            exact,
        )[: 64 * 1024]
    except KeyError:
        return None

    text = raw.decode(
        "latin-1",
        errors="ignore",
    )

    if re.search(
        r"<title[^>]*>\s*Visu\s+Schema\b",
        text,
        re.IGNORECASE | re.DOTALL,
    ):
        return "Visu"

    return None


def _merge_volume_metadata(
    package: dict,
    dataset: dict,
) -> dict:
    package_volume = package.get("volume")
    if not isinstance(package_volume, dict):
        package_volume = {}

    dataset_volumes = dataset.get("volumes")
    dataset_volume = {}
    if isinstance(dataset_volumes, list):
        if len(dataset_volumes) == 1 and isinstance(
            dataset_volumes[0],
            dict,
        ):
            dataset_volume = dataset_volumes[0]
        else:
            package_code = str(
                package_volume.get("document_code")
                or ""
            ).strip()
            package_id = str(
                package_volume.get("id")
                or ""
            ).strip()
            for candidate in dataset_volumes:
                if not isinstance(candidate, dict):
                    continue
                code = str(
                    candidate.get("document_code")
                    or ""
                ).strip()
                volume_id = str(
                    candidate.get("id")
                    or ""
                ).strip()
                if (
                    package_code
                    and code == package_code
                ) or (
                    package_id
                    and volume_id == package_id
                ):
                    dataset_volume = candidate
                    break

    merged = dict(dataset_volume)
    for key, value in package_volume.items():
        if value not in (
            None,
            "",
            [],
        ):
            merged[key] = value

    inferred = infer_volume_identity(
        merged.get("source_folder"),
        merged.get("entrypoint"),
        merged.get("title"),
        package.get("dataset_title"),
        dataset.get("title"),
        dataset.get("model"),
        dataset.get("platform"),
    )

    for key, value in inferred.items():
        if merged.get(key) in (
            None,
            "",
            [],
        ):
            merged[key] = value

    return merged


def inspect_package(
    path: Path,
) -> tuple[dict, dict]:
    try:
        with ZipFile(path) as archive:
            names = set(
                archive.namelist()
            )
            if RDPKG_MANIFEST not in names:
                raise ValueError(
                    "missing rdpkg.json"
                )

            package = _read_json_entry(
                archive,
                RDPKG_MANIFEST,
            )

            if (
                package.get("format")
                != RDPKG_FORMAT
            ):
                raise ValueError(
                    "unsupported package format"
                )
            if int(
                package.get(
                    "schema_version",
                    0,
                )
            ) != RDPKG_SCHEMA_VERSION:
                raise ValueError(
                    "unsupported package schema"
                )

            dataset_name = str(
                package.get(
                    "dataset_manifest",
                    "renault-dataset.json",
                )
            )
            if dataset_name not in names:
                raise ValueError(
                    "missing dataset manifest"
                )

            dataset = _read_json_entry(
                archive,
                dataset_name,
            )

            merged_volume = _merge_volume_metadata(
                package,
                dataset,
            )

            if not merged_volume.get(
                "document_type"
            ):
                inferred_document_type = _infer_document_type_from_archive(
                    archive,
                    merged_volume,
                )
                if inferred_document_type:
                    package_volume = package.get(
                        "volume"
                    )
                    if not isinstance(
                        package_volume,
                        dict,
                    ):
                        package_volume = {}
                        package["volume"] = package_volume
                    package_volume[
                        "document_type"
                    ] = inferred_document_type
    except (
        BadZipFile,
        KeyError,
        json.JSONDecodeError,
        UnicodeDecodeError,
    ) as error:
        raise ValueError(
            f"invalid package: {error}"
        ) from error

    return package, dataset


def canonical_name(
    path: Path,
) -> str:
    package, dataset = inspect_package(
        path,
    )
    volume = _merge_volume_metadata(
        package,
        dataset,
    )

    if not (
        volume.get("document_code")
        or volume.get("id")
    ):
        raise ValueError(
            "volume identity is missing"
        )

    return default_package_filename(
        dataset,
        volume,
    )


def plan_migration(
    directory: Path,
) -> list[MigrationItem]:
    directory = (
        directory
        .expanduser()
        .resolve()
    )

    if not directory.is_dir():
        raise ValueError(
            "Package directory not found: "
            + str(directory)
        )

    items: list[MigrationItem] = []

    for source in sorted(
        directory.glob("*.rdpkg"),
        key=lambda value: value.name.casefold(),
    ):
        try:
            target_name = canonical_name(
                source,
            )
        except ValueError as error:
            items.append(
                MigrationItem(
                    source=source,
                    target=source,
                    status="invalid",
                    reason=str(error),
                )
            )
            continue

        target = source.with_name(
            target_name,
        )

        if source.name == target.name:
            items.append(
                MigrationItem(
                    source=source,
                    target=target,
                    status="already",
                )
            )
            continue

        if target.exists():
            items.append(
                MigrationItem(
                    source=source,
                    target=target,
                    status="conflict",
                    reason="target already exists",
                )
            )
            continue

        items.append(
            MigrationItem(
                source=source,
                target=target,
                status="rename",
            )
        )

    return items


def apply_migration(
    items: list[MigrationItem],
) -> list[MigrationItem]:
    results: list[MigrationItem] = []

    for item in items:
        if item.status != "rename":
            results.append(item)
            continue

        if item.target.exists():
            results.append(
                MigrationItem(
                    source=item.source,
                    target=item.target,
                    status="conflict",
                    reason="target appeared before rename",
                )
            )
            continue

        item.source.rename(
            item.target,
        )
        results.append(
            MigrationItem(
                source=item.source,
                target=item.target,
                status="renamed",
            )
        )

    return results


def print_report(
    items: list[MigrationItem],
) -> None:
    labels = {
        "rename": "RENAME",
        "renamed": "RENAMED",
        "already": "OK",
        "conflict": "CONFLICT",
        "invalid": "SKIP",
    }

    for item in items:
        label = labels.get(
            item.status,
            item.status.upper(),
        )
        if item.source.name == item.target.name:
            print(
                f"{label}: {item.source.name}"
            )
        else:
            print(
                f"{label}: {item.source.name}"
            )
            print(
                f"     -> {item.target.name}"
            )
        if item.reason:
            print(
                f"     {item.reason}"
            )

    counts: dict[str, int] = {}
    for item in items:
        counts[item.status] = (
            counts.get(
                item.status,
                0,
            )
            + 1
        )

    print("")
    print(
        "Summary: "
        + " · ".join(
            f"{key}={counts[key]}"
            for key in sorted(
                counts,
            )
        )
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Safely migrate Renault Docs public .rdpkg archive "
            "filenames to canonical identity names."
        )
    )
    parser.add_argument(
        "--directory",
        type=Path,
        default=DEFAULT_PACKAGE_DIR,
        help=(
            "Known Renault Docs public .rdpkg archive directory."
        ),
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Apply safe non-overwriting renames.",
    )
    args = parser.parse_args()

    items = plan_migration(
        args.directory,
    )

    if args.apply:
        items = apply_migration(
            items,
        )

    print_report(
        items,
    )

    if any(
        item.status == "invalid"
        for item in items
    ):
        return 2

    if any(
        item.status == "conflict"
        for item in items
    ):
        return 3

    return 0


if __name__ == "__main__":
    raise SystemExit(
        main(),
    )
