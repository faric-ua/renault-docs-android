#!/usr/bin/env python3
"""Offline, read-only preflight before changing a Renault Docs Drive catalog.

Checks a local publish plan against locally packaged .rdpkg content and an
owner-provided Drive folder metadata snapshot. NO API, uploads, catalog writes
or Drive file mutations. Reports exclude private Drive IDs and absolute paths.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import sys
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from tools.build_drive_catalog import inspect_package, load_publish_plan


def _document(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    return value


def _size(value: Any) -> int:
    if isinstance(value, bool) or not isinstance(value, (int, str)):
        raise ValueError("invalid Drive file byte size")
    if isinstance(value, str) and (not value.isdecimal() or not value):
        raise ValueError("invalid Drive file byte size")
    result = int(value)
    if result < 0:
        raise ValueError("invalid Drive file byte size")
    return result


def verify_publish_candidate(
    packages_dir: Path,
    plan: dict[str, Any],
    drive_snapshot: dict[str, Any],
) -> dict[str, Any]:
    """Check exact Drive IDs, local package identity, size and optional SHA256.

    Drive absence means BLOCKED even with an incomplete listing; it never
    establishes remote deletion. No report contains private provider IDs.
    """
    drive_snapshot = _document(drive_snapshot, "Drive snapshot")
    files = drive_snapshot.get("files")
    if files is None and isinstance(drive_snapshot.get("result"), dict):
        files = drive_snapshot["result"].get("files")
    if not isinstance(files, list):
        raise ValueError("Drive snapshot must contain a files array")
    complete = drive_snapshot.get("complete", False)
    if not isinstance(complete, bool):
        raise ValueError("snapshot.complete must be boolean")
    root = Path(packages_dir).expanduser().resolve()
    if not root.is_dir():
        raise ValueError("package directory does not exist")

    by_id: dict[str, dict[str, Any]] = {}
    by_name: dict[str, list[str]] = defaultdict(list)
    for number, item in enumerate(files, 1):
        item = _document(item, f"snapshot file #{number}")
        file_id = str(item.get("id") or item.get("file_id") or "").strip()
        name = str(item.get("title") or item.get("name") or "").strip()
        if not file_id or not name:
            raise ValueError("snapshot entry lacks file ID or name")
        if file_id in by_id:
            raise ValueError("snapshot contains duplicate file ID")
        kind = str(item.get("file_or_folder") or item.get("mime_type") or "").lower()
        if kind in ("folder", "application/vnd.google-apps.folder"):
            continue
        record = {
            "name": name,
            "size": _size(item.get("size")),
            "sha256": item.get("sha256_checksum") or item.get("sha256Checksum"),
        }
        if record["sha256"] is not None:
            digest = record["sha256"]
            if not isinstance(digest, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", digest):
                raise ValueError("invalid Drive SHA256 metadata")
            record["sha256"] = digest.lower()
        by_id[file_id] = record
        by_name[name].append(file_id)

    checks: list[dict[str, Any]] = []
    seen_local_package_keys: set[tuple[str, str]] = set()
    for selected in plan["packages"]:
        name, drive_id = selected["file_name"], selected["drive_file_id"]
        if name != Path(name).name or not name.endswith(".rdpkg"):
            raise ValueError("publish plan contains an unsafe package basename")

        path = root / name
        if path.is_symlink() or not path.is_file() or path.resolve().parent != root:
            checks.append({"file_name": name, "result": "BLOCKED",
                           "problems": ["local_package_missing_or_unsafe"]})
            continue
        try:
            metadata = inspect_package(path)
        except (ValueError, OSError, TypeError) as error:
            # Avoid absolute filesystem paths in a public report.
            checks.append({"file_name": name, "result": "BLOCKED",
                           "problems": ["local_package_invalid"], "details": type(error).__name__})
            continue

        key = (metadata["project_id"], metadata["package_id"])
        problems: list[str] = []
        warnings: list[str] = []
        if key in seen_local_package_keys:
            problems.append("repeated_package_identity_in_publish_plan")
        seen_local_package_keys.add(key)

        remote = by_id.get(drive_id)
        remote_sha_verified = False
        if remote is None:
            problems.append("selected_drive_id_not_in_supplied_snapshot")
        else:
            if remote["name"] != name:
                problems.append("selected_drive_filename_mismatch")
            if remote["size"] != metadata["size_bytes"]:
                problems.append("selected_drive_byte_size_mismatch")
            if remote["sha256"] is None:
                warnings.append("remote_sha256_not_available_content_unverified")
            elif remote["sha256"] != metadata["sha256"]:
                problems.append("selected_drive_sha256_mismatch")
            else:
                remote_sha_verified = True

        if len(by_name.get(name, [])) > 1:
            warnings.append("same_filename_multiple_drive_objects_manual_review")

        # Even fully matching metadata is not a license to publish third-party
        # documentation or proof of existing user-data compatibility.
        if problems:
            status = "BLOCKED"
        elif "same_filename_multiple_drive_objects_manual_review" in warnings:
            status = "REVIEW_REQUIRED"
        elif remote_sha_verified:
            status = "METADATA_AND_HASH_MATCH"
        else:
            status = "METADATA_MATCH_CONTENT_UNVERIFIED"
        checks.append({
            "file_name": name,
            "project_id": metadata["project_id"],
            "document_code": metadata["document_code"],
            "date": metadata["date"],
            "result": status,
            "problems": sorted(problems),
            "warnings": sorted(warnings),
        })

    checks.sort(key=lambda x: (x["file_name"].casefold(), x["file_name"]))
    results = Counter(item["result"] for item in checks)
    return {
        "schema_version": 1,
        "mode": "offline_read_only_prepublication_preflight",
        "drive_snapshot_complete": complete,
        "packages_selected": len(checks),
        "result_counts": dict(sorted(results.items())),
        "can_autopublish": False,
        "packages": checks,
        "notes": [
            "Selected Google Drive file identity is checked by exact provider ID, never filename fallback.",
            "Missing from supplied snapshot blocks preflight but does not prove Drive file is missing.",
            "Same-name copies require manual review; distinct NT codes remain distinct documents.",
            "Drive metadata without SHA256 does not prove package contents equal local .rdpkg bytes.",
            "No Google Drive reads/writes, file deletion, catalog publication or access-control changes.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Compare locally prepared .rdpkg to an existing Drive metadata snapshot without writes."
    )
    parser.add_argument("--packages-dir", required=True, type=Path)
    parser.add_argument("--publish-plan", required=True, type=Path)
    parser.add_argument("--drive-snapshot", required=True, type=Path)
    args = parser.parse_args()
    try:
        # Existing loader is authoritative for publish plan schema; sanitize
        # its exception text so private IDs cannot leak into CLI diagnostics.
        try:
            plan = load_publish_plan(args.publish_plan)
        except (ValueError, TypeError, OSError, json.JSONDecodeError) as error:
            raise ValueError("invalid publish plan; inspect entries privately") from error
        snapshot = json.loads(args.drive_snapshot.read_text(encoding="utf-8"))
        report = verify_publish_candidate(args.packages_dir, plan, snapshot)
    except (OSError, ValueError, TypeError, json.JSONDecodeError) as error:
        parser.error(str(error))
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    # CLI success signals complete preflight, never an automatic publish permit.
    return 2 if report["result_counts"].get("BLOCKED", 0) else 0


if __name__ == "__main__":
    raise SystemExit(main())
