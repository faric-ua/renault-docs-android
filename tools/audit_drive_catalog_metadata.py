#!/usr/bin/env python3
"""Read-only audit of a Google Drive prepared-package listing against catalog JSON.

This consumes separately supplied JSON snapshots. It never connects to Google
Drive, copies archives, changes metadata, or modifies any input files.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
from typing import Any


def _document(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be a JSON object")
    return value


def _entries(listing: dict[str, Any]) -> list[dict[str, Any]]:
    raw = listing.get("files")
    if raw is None and isinstance(listing.get("result"), dict):
        raw = listing["result"].get("files")
    if not isinstance(raw, list):
        raise ValueError("Drive listing must contain a files array")
    if any(not isinstance(file, dict) for file in raw):
        raise ValueError("Drive listing files must be JSON objects")
    return raw


def _size(value: Any, label: str) -> int:
    if isinstance(value, bool):
        raise ValueError(f"{label} must be a nonnegative integer")
    try:
        converted = int(value)
    except (ValueError, TypeError, OverflowError) as error:
        raise ValueError(f"{label} must be a nonnegative integer") from error
    if converted < 0 or (isinstance(value, float) and not value.is_integer()):
        raise ValueError(f"{label} must be a nonnegative integer")
    return converted


def audit_drive_metadata(
    catalog: dict[str, Any],
    listing: dict[str, Any],
    *,
    listing_complete: bool = False,
) -> dict[str, Any]:
    """Return a deterministic metadata-only report; never output Drive IDs."""
    catalog = _document(catalog, "catalog")
    listing = _document(listing, "listing")
    projects = catalog.get("projects")
    if not isinstance(projects, list):
        raise ValueError("catalog.projects must be an array")

    by_id: dict[str, dict[str, Any]] = {}
    by_name: dict[str, list[dict[str, Any]]] = defaultdict(list)
    findings: list[dict[str, Any]] = []
    seen_listing_ids: set[str] = set()

    for index, item in enumerate(_entries(listing)):
        ident = str(item.get("id") or item.get("file_id") or "").strip()
        name = str(item.get("title") or item.get("name") or "").strip()
        if not ident or not name:
            raise ValueError(f"listing file #{index + 1} lacks ID or title")
        if ident in seen_listing_ids:
            raise ValueError("listing includes repeated provider file ID")
        seen_listing_ids.add(ident)
        kind = str(item.get("file_or_folder") or item.get("mime_type") or "").lower()
        # Direct listing may contain subfolders. They are not package entries.
        if kind in ("folder", "application/vnd.google-apps.folder"):
            continue
        size = _size(item.get("size"), f"listing file #{index + 1} size")
        record = {"name": name, "size": size}
        by_id[ident] = record
        by_name[name].append({"id": ident, **record})

    reference_ids: set[str] = set()
    seen_catalog_volume_ids: set[tuple[str, str]] = set()
    count_catalog_volumes = 0
    for project in projects:
        project = _document(project, "catalog project")
        project_id = str(project.get("id") or "").strip()
        if not project_id:
            raise ValueError("catalog project must have id")
        volumes = project.get("volumes")
        if not isinstance(volumes, list):
            raise ValueError("catalog project.volumes must be an array")

        for volume in volumes:
            volume = _document(volume, "catalog volume")
            volume_id = str(volume.get("id") or "").strip()
            file_id = str(volume.get("drive_file_id") or "").strip()
            name = str(volume.get("file_name") or "").strip()
            if not volume_id or not file_id or not name:
                raise ValueError("catalog volume must include id, drive_file_id and file_name")
            if not name.lower().endswith(".rdpkg") or name != Path(name).name:
                raise ValueError("catalog volume must reference a .rdpkg basename")
            volume_key = (project_id, volume_id)
            if volume_key in seen_catalog_volume_ids:
                raise ValueError("catalog has a repeated project/volume ID")
            seen_catalog_volume_ids.add(volume_key)
            if file_id in reference_ids:
                raise ValueError("catalog references the same Drive file ID more than once")
            reference_ids.add(file_id)
            count_catalog_volumes += 1

            found = by_id.get(file_id)
            if found is None:
                findings.append({
                    "kind": "referenced_file_not_in_snapshot",
                    "severity": "error" if listing_complete else "unverified",
                    "file_name": name,
                    "note": "Missing from confirmed complete listing" if listing_complete
                        else "Not in supplied listing; listing may be incomplete",
                })
                continue
            if found["name"] != name:
                findings.append({
                    "kind": "catalog_filename_mismatch",
                    "severity": "error",
                    "file_name": name,
                    "listing_file_name": found["name"],
                })
            declared_size = _size(volume.get("size_bytes"), "catalog size_bytes")
            if found["size"] != declared_size:
                findings.append({
                    "kind": "catalog_size_mismatch",
                    "severity": "error",
                    "file_name": name,
                    "catalog_size_bytes": declared_size,
                    "listed_size_bytes": found["size"],
                })

    for name, members in sorted(by_name.items(), key=lambda pair: (pair[0].casefold(), pair[0])):
        if not name.lower().endswith(".rdpkg"):
            if name.lower().endswith((".zip", ".7z", ".rar")):
                findings.append({
                    "kind": "raw_archive_in_prepared_catalog",
                    "severity": "warning",
                    "file_name": name,
                })
            continue
        if len(members) > 1:
            findings.append({
                "kind": "duplicate_package_filename",
                "severity": "warning",
                "file_name": name,
                "copies": len(members),
                "size_bytes": sorted(member["size"] for member in members),
                "catalog_references": sum(member["id"] in reference_ids for member in members),
                "note": "Names matching do not prove equal payloads; do not delete automatically",
            })
        for member in members:
            if member["id"] not in reference_ids:
                findings.append({
                    "kind": "unreferenced_prepared_package",
                    "severity": "review",
                    "file_name": name,
                    "size_bytes": member["size"],
                })

    priority = {"error": 0, "warning": 1, "review": 2, "unverified": 3}
    findings.sort(key=lambda x: (
        priority.get(x["severity"], 9),
        x["kind"],
        x.get("file_name", "").casefold(),
        x.get("size_bytes") if isinstance(x.get("size_bytes"), int) else -1,
    ))
    counts = Counter(item["kind"] for item in findings)
    return {
        "schema_version": 1,
        "mode": "metadata_only_no_download_no_mutation",
        "listing_complete": listing_complete,
        "listed_files": len(by_id),
        "catalog_referenced_volumes": count_catalog_volumes,
        "referenced_in_listing": sum(file_id in by_id for file_id in reference_ids),
        "finding_counts": dict(sorted(counts.items())),
        "findings": findings,
        "notes": [
            "Drive file IDs, source URLs and permissions are intentionally omitted from the report.",
            "This checks metadata IDs, names, and sizes, NOT checksum, package bytes or PDF contents.",
            "No file is removed, copied, renamed, downloaded, or published.",
            "An incomplete folder snapshot cannot prove that a catalog reference is missing from Drive.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Read-only .rdpkg Drive folder-listing versus catalog JSON audit."
    )
    parser.add_argument("--catalog", required=True, type=Path, help="Local renault-docs-catalog.json")
    parser.add_argument("--listing", required=True, type=Path, help="Local Drive folder metadata JSON")
    parser.add_argument(
        "--complete-listing", action="store_true",
        help="Assert all immediate folder children are included; never guess this",
    )
    args = parser.parse_args()
    try:
        catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
        listing = json.loads(args.listing.read_text(encoding="utf-8"))
        report = audit_drive_metadata(catalog, listing, listing_complete=args.complete_listing)
    except (ValueError, OSError, json.JSONDecodeError) as error:
        parser.error(str(error))
    print(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
