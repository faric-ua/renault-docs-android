#!/usr/bin/env python3
"""Offline and non-mutating verification of a Renault Docs .rdproject archive.

Check outer ZIP, exact manifest file list and every embedded one-volume .rdpkg,
including ZIP CRC, manifest consistency, and embedded dataset/entrypoint presence.
Individual nested packages are temporarily spooled and automatically deleted.
This checks archive integrity, not original Windows source or Android UI behavior.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import stat
import tempfile
from zipfile import BadZipFile, ZipFile

CHUNK = 1024 * 1024
PACKAGE_SUFFIX = ".rdpkg"
MAX_ARCHIVE_FILES = 250000
MAX_PACKAGE_BYTES = 32 * (1024 ** 3)


def valid_name(name: str) -> bool:
    if not name or name.startswith("/") or "\\" in name or "\x00" in name:
        return False
    parts = name.rstrip("/").split("/")
    return bool(parts) and all(x not in {"", ".", ".."} for x in parts) and not re.match(r"^[A-Za-z]:", parts[0])


def zip_entries(z: ZipFile, limit: int = MAX_ARCHIVE_FILES) -> set[str]:
    files: set[str] = set()
    canonical: set[str] = set()
    for entry in z.infolist():
        name = entry.filename
        if not valid_name(name) or (entry.flag_bits & 1):
            raise ValueError("ZIP contains unsafe or encrypted entry")
        if stat.S_IFMT(entry.external_attr >> 16) == stat.S_IFLNK:
            raise ValueError("ZIP contains symbolic link")
        key = name.rstrip("/").casefold()
        if key in canonical:
            raise ValueError("ZIP contains duplicate or case-conflicting paths")
        canonical.add(key)
        if not entry.is_dir():
            files.add(name)
        if len(canonical) > limit:
            raise ValueError("ZIP contains too many entries")
    return files


def manifest_object(z: ZipFile, name: str, max_size=2 * 1024**2) -> dict:
    if name not in z.namelist() or z.getinfo(name).file_size > max_size:
        raise ValueError("required manifest missing or exceeds size limit")
    obj = json.loads(z.read(name).decode("utf-8"))
    if not isinstance(obj, dict):
        raise ValueError("manifest is not a JSON object")
    return obj


def inspect_embedded(path: Path, meta: dict, project: dict) -> dict:
    with ZipFile(path, "r") as nested:
        names = zip_entries(nested)
        if not {"rdpkg.json", "renault-dataset.json"}.issubset(names):
            raise ValueError("inner rdpkg lacks required manifests")
        package = manifest_object(nested, "rdpkg.json")
        dataset = manifest_object(nested, "renault-dataset.json", max_size=16 * 1024**2)
        if package.get("schema_version") != 1 or package.get("format") != "renault-volume-package-v1":
            raise ValueError("inner rdpkg format/schema does not match")
        pvol = package.get("volume")
        dvols = dataset.get("volumes")
        if not isinstance(pvol, dict) or not isinstance(dvols, list) or len(dvols) != 1:
            raise ValueError("inner rdpkg is not a single-volume package")
        if not isinstance(dvols[0], dict):
            raise ValueError("inner dataset volume invalid")
        if package.get("project_id") != dataset.get("project_id"):
            raise ValueError("inner dataset/package project mismatch")
        if project.get("id") and package.get("project_id") != project.get("id"):
            raise ValueError("outer project vs inner rdpkg mismatch")
        for key in ("document_code", "date"):
            if meta.get(key) and pvol.get(key) and meta[key] != pvol[key]:
                raise ValueError("outer project vs inner volume identity mismatch: " + key)
            if pvol.get(key) and dvols[0].get(key) and pvol[key] != dvols[0][key]:
                raise ValueError("inner volume identity mismatch: " + key)
        entry = pvol.get("entrypoint")
        if not isinstance(entry, str) or entry not in names:
            raise ValueError("inner rdpkg missing volume entrypoint")
        payload = [e for e in nested.infolist() if not e.is_dir() and e.filename != "rdpkg.json"]
        if package.get("payload_file_count") != len(payload) or package.get("payload_bytes") != sum(e.file_size for e in payload):
            raise ValueError("inner rdpkg manifest payload counts mismatch")
        if sum(e.file_size for e in payload) > MAX_PACKAGE_BYTES:
            raise ValueError("inner rdpkg uncompressed size exceeds safety limit")
        bad = nested.testzip()
        if bad is not None:
            raise ValueError("inner rdpkg ZIP CRC mismatch")
        return {
            "document_code": pvol.get("document_code"),
            "date": pvol.get("date"),
            "package_id": package.get("package_id"),
            "payload_files": len(payload),
            "payload_bytes": sum(e.file_size for e in payload),
            "crc": "PASS",
        }


def audit_rdproject(path: Path, *, expected_volumes: int | None = None) -> dict:
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("archive not found or path is symbolic link")
    if expected_volumes is not None and (type(expected_volumes) is not int or expected_volumes < 1):
        raise ValueError("expected_volumes must be positive")
    with ZipFile(path, "r") as archive:
        names = zip_entries(archive)
        if "rdproject.json" not in names:
            raise ValueError("rdproject.json missing")
        doc = manifest_object(archive, "rdproject.json")
        if doc.get("schema_version") != 1 or doc.get("format") != "renault-docs-project":
            raise ValueError("invalid rdproject format/schema")
        project = doc.get("project")
        volumes = doc.get("volumes")
        if not isinstance(project, dict) or not isinstance(volumes, list) or not volumes:
            raise ValueError("missing project or volume list")
        if expected_volumes is not None and len(volumes) != expected_volumes:
            raise ValueError("manifest volume count differs from expected")
        declared: set[str] = set()
        records = []
        for entry in volumes:
            if not isinstance(entry, dict):
                raise ValueError("invalid volume record")
            filename = entry.get("file")
            if not isinstance(filename, str) or not valid_name(filename):
                raise ValueError("unsafe volume file name")
            if not filename.startswith("volumes/") or not filename.endswith(PACKAGE_SUFFIX) or filename.count("/") != 1:
                raise ValueError("volume path not a direct volumes/*.rdpkg")
            if filename in declared:
                raise ValueError("duplicate volume in project manifest")
            declared.add(filename)
        actual = names - {"rdproject.json"}
        if actual != declared:
            raise ValueError("outer project ZIP file set does not match manifest")
        package_ids: set[str] = set()
        for idx, meta in enumerate(volumes, 1):
            name = meta["file"]
            h = hashlib.sha256()
            with tempfile.TemporaryDirectory(prefix="renault-rdproject-check-") as tmp:
                individual = Path(tmp) / "volume.rdpkg"
                with archive.open(name, "r") as source, individual.open("wb") as output:
                    while block := source.read(CHUNK):
                        h.update(block)
                        output.write(block)
                details = inspect_embedded(individual, meta, project)
            if not isinstance(details["package_id"], str) or not details["package_id"]:
                raise ValueError("invalid embedded package id")
            if details["package_id"] in package_ids:
                raise ValueError("duplicated package ID inside project")
            package_ids.add(details["package_id"])
            records.append({"index": idx, "file_name": name.removeprefix("volumes/"),
                            "sha256": h.hexdigest(), **details})
        return {"status": "PASS", "project": project.get("title"),
                "volume_count": len(volumes), "outer_size_bytes": path.stat().st_size,
                "volumes": records, "notes": [
                    "Integrity and manifest checks only; original Windows sources not compared.",
                    "Did not verify interactive navigation or visually render PDF pages.",
                    "No user source files or archive data modified."]}


def main() -> int:
    p = argparse.ArgumentParser(description="Read-only integrity audit of full Renault Docs .rdproject")
    p.add_argument("--file", type=Path, required=True)
    p.add_argument("--expected-volumes", type=int)
    args = p.parse_args()
    try:
        result = audit_rdproject(args.file, expected_volumes=args.expected_volumes)
    except (ValueError, BadZipFile, EOFError, OSError, RuntimeError, KeyError, UnicodeError, TypeError):
        print(json.dumps({"status": "BLOCKED", "reason": "project_archive_integrity_or_manifest_failed"}))
        return 2
    print(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
