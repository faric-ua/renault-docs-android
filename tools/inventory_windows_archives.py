#!/usr/bin/env python3
"""Read-only, filename-based preflight for locally available Renault source archives.

This is NOT a Google Drive downloader, converter, or publication workflow.
No extraction or file writes; output is JSON printed to stdout.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any
from zipfile import BadZipFile, ZipFile

from core.volumes import infer_volume_identity


SCHEMA_VERSION = 1
SOURCE_EXTENSIONS = {".zip": "ZIP", ".7z": "7Z", ".rar": "RAR"}
KNOWN_EXTENSIONS = SOURCE_EXTENSIONS.keys() | {".rdpkg"}
MAGIC = {
    "ZIP": (b"PK\x03\x04", b"PK\x05\x06", b"PK\x07\x08"),
    "7Z": (b"7z\xbc\xaf\x27\x1c",),
    "RAR": (b"Rar!\x1a\x07\x00", b"Rar!\x1a\x07\x01\x00"),
}
EXPLICIT_MODELS = (
    ("kangoo", re.compile(r"(?i)(?:^|[^a-z])kangoo[ _.-]*(?:ii|2)?(?=$|[^a-z])")),
    ("megane", re.compile(r"(?i)(?:^|[^a-z])megane[ _.-]*(?:ii|2)?(?=$|[^a-z])")),
    ("laguna", re.compile(r"(?i)(?:^|[^a-z])laguna[ _.-]*(?:ii|2)?(?=$|[^a-z])")),
)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _models(filename: str) -> list[str]:
    return [model for model, pattern in EXPLICIT_MODELS if pattern.search(filename)]


def _inspect(path: Path, relative_path: str, include_sha256: bool) -> dict[str, Any]:
    extension = path.suffix.lower()
    size = path.stat().st_size
    identity = infer_volume_identity(path.stem)
    entry: dict[str, Any] = {
        "relative_path": relative_path,
        "file_name": path.name,
        "size_bytes": size,
        "declared_format": SOURCE_EXTENSIONS.get(extension, "RDPKG"),
        "identity_source": "filename_only_unverified",
        "identity": identity,
        "explicit_models": _models(path.stem),
    }
    if include_sha256:
        entry["sha256"] = _sha256(path)

    # Never pass prepared files to raw Windows archive conversion.
    if extension == ".rdpkg":
        entry["status"] = "prepared_package_not_source"
        return entry

    try:
        with path.open("rb") as source:
            header = source.read(8)
    except OSError:
        entry["status"] = "unreadable"
        return entry

    kind = SOURCE_EXTENSIONS[extension]
    if not any(header.startswith(signature) for signature in MAGIC[kind]):
        entry["status"] = "invalid_header"
        return entry

    if kind == "ZIP":
        try:
            with ZipFile(path) as archive:
                roots = set(archive.namelist())
                if "rdpkg.json" in roots or "renault-dataset.json" in roots:
                    entry["status"] = "prepared_payload_not_source"
                    return entry
        except (BadZipFile, OSError, ValueError):
            entry["status"] = "invalid_zip_structure"
            return entry

    # 7Z/RAR are only header-checked here; payload has NOT been validated.
    entry["status"] = "needs_model_review" if len(entry["explicit_models"]) > 1 else "candidate_unverified"
    return entry


def build_inventory(
    source_dir: Path,
    *,
    recursive: bool = False,
    include_sha256: bool = False,
) -> dict[str, Any]:
    source_dir = Path(source_dir)
    if not source_dir.is_dir() or source_dir.is_symlink():
        raise ValueError("source_dir must be an existing, non-symlink directory")

    iterator = source_dir.rglob("*") if recursive else source_dir.iterdir()
    candidates: list[tuple[str, Path]] = []
    ignored_count = 0
    skipped_symlinks = 0

    for path in iterator:
        if path.is_symlink():
            skipped_symlinks += 1
            continue
        if not path.is_file():
            continue
        if path.suffix.lower() not in KNOWN_EXTENSIONS:
            ignored_count += 1
            continue
        relative = path.relative_to(source_dir).as_posix()
        candidates.append((relative, path))

    files = [
        _inspect(path, relative, include_sha256)
        for relative, path in sorted(candidates, key=lambda pair: (pair[0].casefold(), pair[0]))
    ]

    # NT + date are hints, not proof of duplicate content or matching model.
    by_identity: dict[tuple[str, str], list[str]] = defaultdict(list)
    for entry in files:
        if entry["status"] not in {"candidate_unverified", "needs_model_review"}:
            continue
        identity = entry["identity"]
        code, date = identity.get("document_code"), identity.get("date")
        if code and date:
            by_identity[(code, date)].append(entry["relative_path"])

    for entry in files:
        identity = entry["identity"]
        key = (identity.get("document_code"), identity.get("date"))
        if entry["status"] in {"candidate_unverified", "needs_model_review"}:
            same = by_identity.get(key, [])
            if len(same) > 1:
                entry["possible_duplicate_paths"] = [x for x in same if x != entry["relative_path"]]

    return {
        "schema_version": SCHEMA_VERSION,
        "source_folder": source_dir.name,
        "scan_scope": "recursive" if recursive else "top_level_only",
        "archive_count": len(files),
        "ignored_files": ignored_count,
        "skipped_symlinks": skipped_symlinks,
        "status_counts": dict(sorted(Counter(entry["status"] for entry in files).items())),
        "notes": [
            "Read-only local inventory: no extraction, conversion, deletion, renaming, or upload.",
            "Filename identity, NT/date match, and file header are NOT package/content verification.",
            "No remote Google Drive or Android installed-volume state is checked.",
            "Prepared or malformed archives must never enter the original-source converter.",
        ],
        "archives": files,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Read-only Renault ZIP/7Z/RAR source inventory; writes JSON to stdout only."
    )
    parser.add_argument("source_dir", type=Path, help="Local directory of original Windows archives")
    parser.add_argument("--recursive", action="store_true", help="Include descendant folders (not default)")
    parser.add_argument("--sha256", action="store_true", help="Opt in to reading every archive for its hash")
    args = parser.parse_args()
    try:
        result = build_inventory(
            args.source_dir,
            recursive=args.recursive,
            include_sha256=args.sha256,
        )
    except (ValueError, OSError) as error:
        parser.error(str(error))
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
