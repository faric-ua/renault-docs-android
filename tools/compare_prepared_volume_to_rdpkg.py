#!/usr/bin/env python3
"""Read-only SHA-256 parity audit: prepared Renault volume ↔ .rdpkg payload.

This compares the *already prepared* normalized dataset to the portable package.
It does NOT establish parity with the original Windows ZIP/7Z/RAR.
No extraction, package rebuilding, file deletion, output files, or Drive calls.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
from typing import Any
from zipfile import ZipFile

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.dataset_manifest import load_manifest
from core.rdpkg import list_packageable_volumes, select_volume
from tools.validate_rdpkg_payload import PackageIntegrityError, validate_rdpkg_payload


CHUNK_SIZE = 1024 * 1024


def _hash_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while block := stream.read(CHUNK_SIZE):
            digest.update(block)
    return digest.hexdigest()


def _hash_member(archive: ZipFile, member: str) -> str:
    digest = hashlib.sha256()
    with archive.open(member) as stream:
        while block := stream.read(CHUNK_SIZE):
            digest.update(block)
    return digest.hexdigest()


def _safe_folder(value: Any) -> str:
    if not isinstance(value, str) or not value or "\\" in value:
        raise ValueError("unsafe source volume folder")
    if value.startswith("/") or value.rstrip("/") != value:
        raise ValueError("unsafe source volume folder")
    parts = value.split("/")
    if any(part in ("", ".", "..") for part in parts) or ":" in parts[0]:
        raise ValueError("unsafe source volume folder")
    return value


def compare_prepared_volume(
    prepared_root: Path,
    package_path: Path,
    *,
    volume_selector: str,
    max_files: int = 100000,
    max_uncompressed_bytes: int = 32 * 1024**3,
) -> dict[str, Any]:
    """Full byte hash match for source volume files in the prepared dataset.

    Reports names of mismatched relative files, but never absolute paths, data
    bytes or provider IDs. Fail closed on incomplete/missing/extra files.
    """
    if type(max_files) is not int or max_files < 1:
        raise ValueError("max_files must be a positive integer")
    if type(max_uncompressed_bytes) is not int or max_uncompressed_bytes < 0:
        raise ValueError("max_uncompressed_bytes must be nonnegative")

    root_arg = Path(prepared_root)
    if root_arg.is_symlink():
        raise ValueError("prepared root must not be a symbolic link")
    root = root_arg.resolve()
    if not root.is_dir():
        raise ValueError("prepared dataset directory not found")
    package = Path(package_path)
    if package.is_symlink() or not package.is_file():
        raise ValueError("prepared .rdpkg path missing or unsafe")

    # A specific volume selection is required; never guess the newest NT.
    if not isinstance(volume_selector, str) or not volume_selector.strip():
        raise ValueError("explicit volume selector required")
    dataset = load_manifest(root / "renault-dataset.json")
    selected = select_volume(list_packageable_volumes(root), volume_selector)
    folder = _safe_folder(selected.get("source_folder"))
    folder_path = root / folder

    # Validate each source path before any hashing; forbid traversal and symlinks.
    if folder_path.resolve() == root or root not in folder_path.resolve().parents:
        raise ValueError("source folder escapes prepared dataset")
    current = root
    for segment in folder.split("/"):
        current = current / segment
        if current.is_symlink():
            raise ValueError("source folder contains symbolic links")
    if not folder_path.is_dir():
        raise ValueError("selected volume directory not found")

    source: dict[str, Path] = {}
    total_bytes = 0
    for path in folder_path.rglob("*"):
        if path.is_symlink():
            raise ValueError("source volume contains symbolic links")
        if path.is_dir():
            continue
        if not path.is_file():
            raise ValueError("source volume contains special files")
        name = path.relative_to(root).as_posix()
        if name in source:
            raise ValueError("duplicate source relative path")
        source[name] = path
        total_bytes += path.stat().st_size
        if len(source) > max_files or total_bytes > max_uncompressed_bytes:
            raise ValueError("source volume exceeds configured audit limit")
    if not source:
        raise ValueError("selected volume contains no files")

    # Validate the full .rdpkg before comparing; a digest alone does not prove
    # that ZIP CRC, manifest size and all other entries are internally sound.
    validate_rdpkg_payload(package, max_uncompressed_bytes=max_uncompressed_bytes)

    errors: list[str] = []
    mismatches: list[dict[str, str]] = []
    matched_files = 0
    with ZipFile(package, "r") as archive:
        pkg = json.loads(archive.read("rdpkg.json").decode("utf-8"))
        volume = pkg["volume"]
        checks = (
            ("project_id", dataset.get("project_id"), pkg.get("project_id")),
            ("document_code", selected.get("document_code"), volume.get("document_code")),
            ("date", selected.get("date"), volume.get("date")),
            ("volume_id", selected.get("id"), volume.get("id")),
            ("source_folder", folder, volume.get("source_folder")),
            ("entrypoint", selected.get("entrypoint"), volume.get("entrypoint")),
        )
        for key, local, packaged in checks:
            if local is not None and str(local) != str(packaged):
                errors.append(key + "_mismatch")

        # Only compare the selected original source-folder subtree. Files in
        # _renault/ and generated renault-dataset.json belong to derived output.
        names = {
            info.filename for info in archive.infolist()
            if not info.is_dir() and info.filename.startswith(folder + "/")
        }
        missing = sorted(set(source) - names)
        extra = sorted(names - set(source))
        for name in missing:
            mismatches.append({"relative_path": name, "kind": "missing_from_package"})
        for name in extra:
            mismatches.append({"relative_path": name, "kind": "unexpected_in_package"})

        # Hash matching file bytes without extracting anything to disk.
        for name in sorted(set(source) & names):
            if _hash_file(source[name]) == _hash_member(archive, name):
                matched_files += 1
            else:
                mismatches.append({"relative_path": name, "kind": "different_sha256"})

    return {
        "schema_version": 1,
        "scope": "normalized_prepared_volume_to_rdpkg_not_original_windows_archive",
        "project_id": dataset.get("project_id"),
        "document_code": selected.get("document_code"),
        "date": selected.get("date"),
        "source_files": len(source),
        "matching_files": matched_files,
        "mismatch_count": len(mismatches),
        "metadata_problems": sorted(errors),
        "result": "PASS" if not errors and not mismatches else "FAIL",
        "mismatches": mismatches[:25],
        "mismatches_truncated": len(mismatches) > 25,
        "notes": [
            "Prepared/normalized source bytes only; original Windows archive parity NOT checked.",
            "All selected source-folder files hashed and compared to the .rdpkg payload.",
            "Generated Modern/runtime/index files are outside this file-parity check.",
            "No files were extracted, written, moved, renamed, deleted or uploaded.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only prepared Renault volume vs .rdpkg file parity")
    parser.add_argument("--prepared-root", required=True, type=Path)
    parser.add_argument("--package", required=True, type=Path)
    parser.add_argument("--volume", required=True, help="Exact source folder, NT, title or volume ID")
    args = parser.parse_args()
    try:
        report = compare_prepared_volume(
            args.prepared_root, args.package, volume_selector=args.volume
        )
    except (ValueError, OSError, PackageIntegrityError, json.JSONDecodeError, KeyError):
        parser.error("parity preflight could not validate input; inspect prepared files privately")
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 0 if report["result"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
