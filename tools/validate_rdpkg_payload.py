#!/usr/bin/env python3
"""Read-only full-payload integrity checks for an existing Renault .rdpkg.

No extraction, on-disk writes, catalog changes, or package modifications.
Inspects ZIP entry safety, single-volume manifest counts, entrypoint and
streams every payload entry to force CRC validation.
"""
from __future__ import annotations

import json
from pathlib import Path
import re
import stat
from typing import Any
from zipfile import BadZipFile, ZipFile

from core.rdpkg import RDPKG_FORMAT, RDPKG_MANIFEST, RDPKG_SCHEMA_VERSION


class PackageIntegrityError(ValueError):
    """A prepared Renault package failed a non-mutating content validation."""


def _safe_archive_name(name: str) -> bool:
    if not name or name.startswith("/") or "\\" in name or "\x00" in name:
        return False
    segments = name.rstrip("/").split("/")
    return bool(segments) and all(part not in {"", ".", ".."} for part in segments) and not (
        re.match(r"^[A-Za-z]:", segments[0])
    )


def _positive_or_zero(value: Any, field: str) -> int:
    if type(value) is not int or value < 0:
        raise PackageIntegrityError(f"{field} must be a nonnegative integer")
    return value


def validate_rdpkg_payload(
    path: Path,
    *,
    max_uncompressed_bytes: int = 32 * 1024**3,
) -> dict[str, int | str]:
    """Validate actual ZIP payload and counters; no archive extraction.

    Metadata is validated before streaming decompression; max size acts as
    a guardrail against intentionally malformed claims / extreme workloads.
    """
    path = Path(path)
    limit = _positive_or_zero(max_uncompressed_bytes, "max_uncompressed_bytes")
    try:
        with ZipFile(path, "r") as archive:
            infos = archive.infolist()
            names: set[str] = set()
            folded: set[str] = set()
            payload = []
            for info in infos:
                name = info.filename
                if not _safe_archive_name(name):
                    raise PackageIntegrityError("unsafe archive entry path")
                canonical = name.rstrip("/")
                lowered = canonical.casefold()
                if lowered in folded or name in names:
                    raise PackageIntegrityError("duplicate archive entry name")
                names.add(name)
                folded.add(lowered)

                mode = info.external_attr >> 16
                if stat.S_IFMT(mode) == stat.S_IFLNK:
                    raise PackageIntegrityError("symlink entries not supported")
                if info.flag_bits & 1:
                    raise PackageIntegrityError("encrypted archive entries not supported")
                if info.is_dir():
                    continue
                if name != RDPKG_MANIFEST:
                    payload.append(info)

            if RDPKG_MANIFEST not in names:
                raise PackageIntegrityError("missing rdpkg.json")
            try:
                package = json.loads(archive.read(RDPKG_MANIFEST).decode("utf-8"))
            except (json.JSONDecodeError, UnicodeDecodeError) as error:
                raise PackageIntegrityError("invalid rdpkg.json") from error
            if not isinstance(package, dict):
                raise PackageIntegrityError("rdpkg.json must be an object")
            if package.get("format") != RDPKG_FORMAT or package.get("schema_version") != RDPKG_SCHEMA_VERSION:
                raise PackageIntegrityError("unsupported package schema")
            volume = package.get("volume")
            if not isinstance(volume, dict):
                raise PackageIntegrityError("volume metadata missing")
            entrypoint = volume.get("entrypoint")
            dataset_name = package.get("dataset_manifest", "renault-dataset.json")
            if not isinstance(entrypoint, str) or not entrypoint or not _safe_archive_name(entrypoint):
                raise PackageIntegrityError("volume entrypoint missing or unsafe")
            if not isinstance(dataset_name, str) or not _safe_archive_name(dataset_name):
                raise PackageIntegrityError("dataset manifest name missing or unsafe")
            if entrypoint not in names or dataset_name not in names:
                raise PackageIntegrityError("dataset manifest or entrypoint not in payload")
            if entrypoint.endswith("/") or dataset_name.endswith("/"):
                raise PackageIntegrityError("entrypoint or dataset manifest is a directory")

            declared_count = _positive_or_zero(package.get("payload_file_count"), "payload_file_count")
            declared_bytes = _positive_or_zero(package.get("payload_bytes"), "payload_bytes")
            actual_bytes = sum(info.file_size for info in payload)
            if declared_count != len(payload):
                raise PackageIntegrityError("payload file count mismatch")
            if declared_bytes != actual_bytes:
                raise PackageIntegrityError("payload bytes mismatch")
            if actual_bytes > limit:
                raise PackageIntegrityError("declared payload exceeds size limit")

            try:
                dataset = json.loads(archive.read(dataset_name).decode("utf-8"))
            except (json.JSONDecodeError, UnicodeDecodeError) as error:
                raise PackageIntegrityError("invalid dataset manifest JSON") from error
            if not isinstance(dataset, dict):
                raise PackageIntegrityError("dataset manifest must be an object")
            project = package.get("project_id")
            if not isinstance(project, str) or not project or dataset.get("project_id") != project:
                raise PackageIntegrityError("project_id differs between package and dataset")

            # Streaming ZipFile.open() checks ZIP CRC when entry is consumed.
            # No decompressed bytes are ever stored on disk.
            for info in payload:
                total = 0
                with archive.open(info, "r") as stream:
                    while True:
                        chunk = stream.read(1024 * 1024)
                        if not chunk:
                            break
                        total += len(chunk)
                        if total > info.file_size:
                            raise PackageIntegrityError("payload entry exceeds declared size")
                if total != info.file_size:
                    raise PackageIntegrityError("payload entry length mismatch")

            return {
                "status": "PAYLOAD_INTEGRITY_PASS",
                "payload_file_count": len(payload),
                "payload_bytes": actual_bytes,
                "zip_entries_inspected": len(infos),
            }
    except (BadZipFile, EOFError, OSError, RuntimeError, NotImplementedError) as error:
        raise PackageIntegrityError("invalid or unreadable ZIP payload") from error
