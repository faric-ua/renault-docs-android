#!/usr/bin/env python3
"""Read-only, folder-allowlisted audit of source Drive metadata snapshots.

No network access, payload download, extraction, modification, or report writes.
The locally supplied allowlist retains private Drive IDs; stdout never shows them.
"""
from __future__ import annotations

import argparse
from collections import Counter
import json
import re
import sys
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.volumes import infer_volume_identity

ARCHIVE_FORMATS = {".zip": "ZIP", ".7z": "7Z", ".rar": "RAR"}
PROJECT_MODELS = {"megane-ii": "megane", "kangoo-ii": "kangoo", "laguna-ii": "laguna"}
MODEL_MARKERS = {
    "megane": re.compile(r"(?i)(?:^|[^a-z])megane[ _.-]*(?:ii|2)?(?=$|[^a-z])"),
    "kangoo": re.compile(r"(?i)(?:^|[^a-z])kangoo[ _.-]*(?:ii|2)?(?=$|[^a-z])"),
    "laguna": re.compile(r"(?i)(?:^|[^a-z])laguna[ _.-]*(?:ii|2)?(?=$|[^a-z])"),
}
ALIAS_RE = re.compile(r"^[a-z][a-z0-9-]{1,48}$")


def _object(value: Any, name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{name} must be an object")
    return value


def _nonblank(value: Any, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{name} must be a nonempty string")
    return value.strip()


def _bytes(value: Any) -> int:
    if isinstance(value, bool) or not isinstance(value, (str, int)):
        raise ValueError("file size must be a nonnegative integer")
    try:
        size = int(value)
    except ValueError as error:
        raise ValueError("file size must be a nonnegative integer") from error
    if size < 0 or (isinstance(value, str) and not value.isdecimal()):
        raise ValueError("file size must be a nonnegative integer")
    return size


def _explicit_models(filename: str) -> list[str]:
    return [name for name, regex in MODEL_MARKERS.items() if regex.search(filename)]


def audit_source_metadata(
    allowlist: dict[str, Any],
    snapshots: dict[str, Any],
) -> dict[str, Any]:
    allowlist = _object(allowlist, "allowlist")
    snapshots = _object(snapshots, "snapshots")
    if allowlist.get("schema_version") != 1 or snapshots.get("schema_version") != 1:
        raise ValueError("schema_version 1 is required")

    scopes = allowlist.get("scopes")
    listings = snapshots.get("listings")
    if not isinstance(scopes, list) or not scopes:
        raise ValueError("allowlist.scopes must be a nonempty list")
    if not isinstance(listings, list):
        raise ValueError("snapshots.listings must be a list")

    by_id: dict[str, dict[str, Any]] = {}
    for position, listing in enumerate(listings, 1):
        item = _object(listing, f"listing {position}")
        folder = _nonblank(item.get("folder_id"), "listing.folder_id")
        if folder in by_id:
            raise ValueError("duplicate listing folder ID")
        if not isinstance(item.get("complete"), bool):
            raise ValueError("listing.complete must be true or false")
        if not isinstance(item.get("files"), list):
            raise ValueError("listing.files must be an array")
        by_id[folder] = item

    scope_data: list[tuple[str, str, str, dict[str, Any] | None]] = []
    permitted_ids: set[str] = set()
    permitted_aliases: set[str] = set()
    for index, scope in enumerate(scopes, 1):
        scope = _object(scope, f"scope {index}")
        alias = _nonblank(scope.get("alias"), "scope.alias")
        folder = _nonblank(scope.get("folder_id"), "scope.folder_id")
        project = _nonblank(scope.get("project_id"), "scope.project_id")
        if not ALIAS_RE.fullmatch(alias):
            raise ValueError("scope.alias must be an opaque lowercase slug")
        if project not in PROJECT_MODELS:
            raise ValueError("scope.project_id must be an explicitly supported Renault project")
        if folder in permitted_ids or alias in permitted_aliases:
            raise ValueError("duplicate allowlisted folder or alias")
        permitted_ids.add(folder)
        permitted_aliases.add(alias)
        scope_data.append((alias, folder, project, by_id.get(folder)))

    # No surprise folder may appear, even if harmless: prevent root-wide scans.
    if set(by_id) - permitted_ids:
        raise ValueError("snapshot includes a folder outside the explicit allowlist")

    output: list[dict[str, Any]] = []
    warnings: list[dict[str, str]] = []
    for alias, folder, project, listing in sorted(scope_data, key=lambda x: x[0]):
        if listing is None:
            warnings.append({"scope": alias, "kind": "scope_not_in_snapshot"})
            continue

        if not listing["complete"]:
            warnings.append({"scope": alias, "kind": "partial_scope_not_complete"})

        seen_ids: set[str] = set()
        records: list[dict[str, Any]] = []
        for index, raw in enumerate(listing["files"], 1):
            item = _object(raw, f"{alias} file {index}")
            provider_id = _nonblank(item.get("id"), "file.id")
            filename = _nonblank(item.get("title") or item.get("name"), "file.title")
            if provider_id in seen_ids:
                raise ValueError("repeated file ID inside a scope")
            seen_ids.add(provider_id)

            kind = str(item.get("file_or_folder") or item.get("mime_type") or "").lower()
            if kind in {"folder", "application/vnd.google-apps.folder"}:
                # No recursion, regardless of whether children happen to exist.
                continue

            extension = Path(filename).suffix.lower()
            if extension not in ARCHIVE_FORMATS:
                continue

            size = _bytes(item.get("size"))
            explicit = _explicit_models(Path(filename).stem)
            target = PROJECT_MODELS[project]
            if any(mark != target for mark in explicit):
                status = "explicit_cross_model_conflict"
            elif len(explicit) == 0:
                status = "model_not_verified"
            else:
                status = "archive_metadata_only"

            records.append({
                "source_scope": alias,
                "project_id": project,
                "file_name": filename,
                "size_bytes": size,
                "declared_format": ARCHIVE_FORMATS[extension],
                "status": status,
                "identity": infer_volume_identity(Path(filename).stem),
                "identity_source": "filename_only_unverified",
            })

        output.extend(sorted(records, key=lambda r: (r["file_name"].casefold(), r["file_name"], r["size_bytes"])))

    # Never collapse two NT codes into a 'newer' canonical edition. Likewise, the
    # same NT in two source scopes need not mean identical underlying documentation.
    counts = Counter(item["status"] for item in output)
    return {
        "schema_version": 1,
        "mode": "offline_allowlisted_drive_metadata_only",
        "allowlisted_scopes": len(scope_data),
        "snapshotted_scopes": sum(listing is not None for _, _, _, listing in scope_data),
        "source_archive_count": len(output),
        "status_counts": dict(sorted(counts.items())),
        "scope_warnings": warnings,
        "archives": output,
        "notes": [
            "Every source folder was explicitly allowlisted; no root-wide search or recursion.",
            "No provider IDs, source folder IDs or URLs appear in this report.",
            "NT codes, dates, type, applicability and versions are filename-derived hints, not content validation.",
            "Different NT codes remain separate Renault documentation; no deduplication or supersession.",
            "No Google Drive access, file download, extraction, conversion or catalog changes were performed.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Privacy-safe read-only Renault Drive source metadata review")
    parser.add_argument("--allowlist", required=True, type=Path, help="Private JSON file with approved source folder IDs")
    parser.add_argument("--snapshots", required=True, type=Path, help="Private JSON file with provider metadata listings")
    args = parser.parse_args()
    try:
        allow = json.loads(args.allowlist.read_text(encoding="utf-8"))
        snapshot = json.loads(args.snapshots.read_text(encoding="utf-8"))
        report = audit_source_metadata(allow, snapshot)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        parser.error(str(error))
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
