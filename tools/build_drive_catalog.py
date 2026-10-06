#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any
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


PUBLISH_PLAN_SCHEMA_VERSION = 1
CATALOG_SCHEMA_VERSION = 1


def _read_json_file(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path.name} must contain a JSON object")
    return value


def _read_json_entry(
    archive: ZipFile,
    name: str,
) -> dict[str, Any]:
    raw = archive.read(name)
    value = json.loads(raw.decode("utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{name} must contain a JSON object")
    return value


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _infer_document_type_from_archive(
    archive: ZipFile,
    volume: dict[str, Any],
) -> str | None:
    entrypoint = str(volume.get("entrypoint") or "").strip()
    if not entrypoint:
        return None

    names = archive.namelist()
    exact = entrypoint if entrypoint in names else None

    if exact is None:
        needle = entrypoint.casefold()
        exact = next(
            (name for name in names if name.casefold() == needle),
            None,
        )

    if exact is None:
        return None

    try:
        raw = archive.read(exact)[: 64 * 1024]
    except KeyError:
        return None

    text = raw.decode("latin-1", errors="ignore")
    if re.search(
        r"<title[^>]*>\s*Visu\s+Schema\b",
        text,
        re.IGNORECASE | re.DOTALL,
    ):
        return "Visu"

    return None


def _resolved_volume(
    package: dict[str, Any],
    dataset: dict[str, Any],
    archive: ZipFile,
) -> dict[str, Any]:
    package_volume = package.get("volume")
    if not isinstance(package_volume, dict):
        package_volume = {}

    dataset_volume: dict[str, Any] = {}
    dataset_volumes = dataset.get("volumes")

    if isinstance(dataset_volumes, list):
        if len(dataset_volumes) == 1 and isinstance(dataset_volumes[0], dict):
            dataset_volume = dataset_volumes[0]
        else:
            package_code = str(
                package_volume.get("document_code") or ""
            ).strip()
            package_id = str(
                package_volume.get("id") or ""
            ).strip()

            for candidate in dataset_volumes:
                if not isinstance(candidate, dict):
                    continue

                code = str(
                    candidate.get("document_code") or ""
                ).strip()
                volume_id = str(
                    candidate.get("id") or ""
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
        if value not in (None, "", []):
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
        if merged.get(key) in (None, "", []):
            merged[key] = value

    if not merged.get("document_type"):
        inferred_type = _infer_document_type_from_archive(
            archive,
            merged,
        )
        if inferred_type:
            merged["document_type"] = inferred_type

    return merged


def inspect_package(path: Path) -> dict[str, Any]:
    try:
        with ZipFile(path) as archive:
            names = set(archive.namelist())

            if RDPKG_MANIFEST not in names:
                raise ValueError("missing rdpkg.json")

            package = _read_json_entry(
                archive,
                RDPKG_MANIFEST,
            )

            if package.get("format") != RDPKG_FORMAT:
                raise ValueError("unsupported package format")

            if int(package.get("schema_version", 0)) != RDPKG_SCHEMA_VERSION:
                raise ValueError("unsupported package schema")

            dataset_name = str(
                package.get(
                    "dataset_manifest",
                    "renault-dataset.json",
                )
            )

            if dataset_name not in names:
                raise ValueError("missing dataset manifest")

            dataset = _read_json_entry(
                archive,
                dataset_name,
            )

            volume = _resolved_volume(
                package,
                dataset,
                archive,
            )

    except (
        BadZipFile,
        KeyError,
        json.JSONDecodeError,
        UnicodeDecodeError,
    ) as error:
        raise ValueError(
            f"invalid package {path.name}: {error}"
        ) from error

    project_id = str(
        package.get("project_id")
        or dataset.get("project_id")
        or ""
    ).strip()

    package_id = str(
        package.get("package_id")
        or ""
    ).strip()

    project_title = str(
        dataset.get("model")
        or dataset.get("title")
        or ""
    ).strip()

    document_code = str(
        volume.get("document_code")
        or ""
    ).strip()

    date = str(
        volume.get("date")
        or ""
    ).strip()

    if not project_id:
        raise ValueError(
            f"{path.name}: project_id is missing"
        )

    if not package_id:
        raise ValueError(
            f"{path.name}: package_id is missing"
        )

    if not project_title:
        raise ValueError(
            f"{path.name}: project title/model is missing"
        )

    if not document_code:
        raise ValueError(
            f"{path.name}: document_code is missing"
        )

    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", date):
        raise ValueError(
            f"{path.name}: valid YYYY-MM-DD date is required"
        )

    expected_name = default_package_filename(
        dataset,
        volume,
    )

    if path.name != expected_name:
        raise ValueError(
            f"{path.name}: non-canonical filename; "
            f"expected {expected_name}"
        )

    vehicle_codes = [
        str(code).strip()
        for code in (volume.get("vehicle_codes") or [])
        if str(code).strip()
    ]

    document_type = str(
        volume.get("document_type")
        or ""
    ).strip()

    document_version = str(
        volume.get("document_version")
        or ""
    ).strip()

    region = str(
        volume.get("region")
        or ""
    ).strip()

    return {
        "project_id": project_id,
        "project_title": project_title,
        "package_id": package_id,
        "document_code": document_code,
        "date": date,
        "vehicle_codes": vehicle_codes,
        "document_type": document_type or None,
        "document_version": document_version or None,
        "region": region or None,
        "file_name": path.name,
        "size_bytes": path.stat().st_size,
        "sha256": _sha256(path),
    }


def load_publish_plan(path: Path) -> dict[str, Any]:
    plan = _read_json_file(path)

    if int(plan.get("schema_version", 0)) != PUBLISH_PLAN_SCHEMA_VERSION:
        raise ValueError(
            "unsupported publish-plan schema_version"
        )

    catalog_id = str(
        plan.get("catalog_id")
        or ""
    ).strip()
    generated_at = str(
        plan.get("generated_at")
        or ""
    ).strip()

    try:
        catalog_version = int(
            plan.get("catalog_version")
        )
    except (TypeError, ValueError) as error:
        raise ValueError(
            "catalog_version must be an integer"
        ) from error

    if catalog_version < 1:
        raise ValueError(
            "catalog_version must be >= 1"
        )

    if not catalog_id:
        raise ValueError(
            "catalog_id is required"
        )

    if not generated_at:
        raise ValueError(
            "generated_at is required for deterministic output"
        )

    raw_packages = plan.get("packages")
    if not isinstance(raw_packages, list) or not raw_packages:
        raise ValueError(
            "packages must be a non-empty list"
        )

    packages: list[dict[str, str]] = []
    seen_names: set[str] = set()
    seen_drive_ids: set[str] = set()

    for index, item in enumerate(raw_packages, start=1):
        if not isinstance(item, dict):
            raise ValueError(
                f"packages[{index}] must be an object"
            )

        file_name = str(
            item.get("file_name")
            or ""
        ).strip()
        drive_file_id = str(
            item.get("drive_file_id")
            or ""
        ).strip()

        if not file_name:
            raise ValueError(
                f"packages[{index}].file_name is required"
            )

        if Path(file_name).name != file_name:
            raise ValueError(
                f"packages[{index}].file_name must be a basename"
            )

        if not file_name.endswith(".rdpkg"):
            raise ValueError(
                f"packages[{index}].file_name must end with .rdpkg"
            )

        if not drive_file_id:
            raise ValueError(
                f"packages[{index}].drive_file_id is required"
            )

        if file_name in seen_names:
            raise ValueError(
                f"duplicate file_name in publish plan: {file_name}"
            )

        if drive_file_id in seen_drive_ids:
            raise ValueError(
                f"duplicate drive_file_id in publish plan: {drive_file_id}"
            )

        seen_names.add(file_name)
        seen_drive_ids.add(drive_file_id)
        packages.append(
            {
                "file_name": file_name,
                "drive_file_id": drive_file_id,
            }
        )

    return {
        "schema_version": PUBLISH_PLAN_SCHEMA_VERSION,
        "catalog_id": catalog_id,
        "catalog_version": catalog_version,
        "generated_at": generated_at,
        "packages": packages,
    }


def build_catalog(
    packages_dir: Path,
    plan: dict[str, Any],
) -> dict[str, Any]:
    packages_dir = packages_dir.expanduser().resolve()

    if not packages_dir.is_dir():
        raise ValueError(
            "packages directory not found: "
            + str(packages_dir)
        )

    projects: dict[str, dict[str, Any]] = {}

    for publish_item in plan["packages"]:
        file_name = publish_item["file_name"]
        package_path = (
            packages_dir
            / file_name
        ).resolve()

        if package_path.parent != packages_dir:
            raise ValueError(
                f"package path escapes packages directory: {file_name}"
            )

        if not package_path.is_file():
            raise ValueError(
                f"package not found: {file_name}"
            )

        metadata = inspect_package(
            package_path,
        )

        project_id = metadata["project_id"]
        project = projects.get(project_id)

        if project is None:
            project = {
                "id": project_id,
                "title": metadata["project_title"],
                "vehicle_codes": set(),
                "years": [],
                "volumes": [],
            }
            projects[project_id] = project
        elif project["title"] != metadata["project_title"]:
            raise ValueError(
                f"{project_id}: conflicting project titles: "
                f"{project['title']} / {metadata['project_title']}"
            )

        project["vehicle_codes"].update(
            metadata["vehicle_codes"]
        )
        project["years"].append(
            int(metadata["date"][:4])
        )

        project["volumes"].append(
            {
                "id": metadata["package_id"],
                "document_code": metadata["document_code"],
                "date": metadata["date"],
                "document_type": metadata["document_type"],
                "document_version": metadata["document_version"],
                "region": metadata["region"],
                "file_name": metadata["file_name"],
                "drive_file_id": publish_item["drive_file_id"],
                "size_bytes": metadata["size_bytes"],
                "sha256": metadata["sha256"],
            }
        )

    output_projects: list[dict[str, Any]] = []

    for project in projects.values():
        project["volumes"].sort(
            key=lambda volume: (
                volume["date"],
                volume["document_code"].casefold(),
                volume["id"].casefold(),
            )
        )

        years = project["years"]

        output_projects.append(
            {
                "id": project["id"],
                "title": project["title"],
                "vehicle_codes": sorted(
                    project["vehicle_codes"],
                    key=str.casefold,
                ),
                "document_year_from": min(years),
                "document_year_to": max(years),
                "volumes": project["volumes"],
            }
        )

    output_projects.sort(
        key=lambda project: (
            project["title"].casefold(),
            project["id"].casefold(),
        )
    )

    return {
        "schema_version": CATALOG_SCHEMA_VERSION,
        "catalog_version": plan["catalog_version"],
        "generated_at": plan["generated_at"],
        "catalog_id": plan["catalog_id"],
        "projects": output_projects,
    }


def write_catalog(
    path: Path,
    catalog: dict[str, Any],
) -> None:
    path = path.expanduser().resolve()
    path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    temporary = path.with_name(
        "." + path.name + ".tmp"
    )

    temporary.write_text(
        json.dumps(
            catalog,
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )

    temporary.replace(path)


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Build the public Renault Docs catalog from validated "
            "canonical .rdpkg files and an explicit Drive publish plan."
        )
    )
    parser.add_argument(
        "--packages-dir",
        type=Path,
        required=True,
        help="Directory containing canonical .rdpkg files.",
    )
    parser.add_argument(
        "--publish-plan",
        type=Path,
        required=True,
        help=(
            "JSON mapping of selected canonical file names to "
            "Google Drive file IDs plus catalog version metadata."
        ),
    )
    parser.add_argument(
        "--output",
        type=Path,
        required=True,
        help="Output renault-docs-catalog.json path.",
    )
    args = parser.parse_args()

    try:
        plan = load_publish_plan(
            args.publish_plan,
        )
        catalog = build_catalog(
            args.packages_dir,
            plan,
        )
        write_catalog(
            args.output,
            catalog,
        )
    except (OSError, ValueError) as error:
        print(
            "Catalog build failed: "
            + str(error),
            file=sys.stderr,
        )
        return 1

    volume_count = sum(
        len(project["volumes"])
        for project in catalog["projects"]
    )

    print(
        "Catalog ready: "
        f"{len(catalog['projects'])} projects / "
        f"{volume_count} volumes"
    )
    print(
        str(
            args.output
            .expanduser()
            .resolve()
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
