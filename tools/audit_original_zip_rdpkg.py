#!/usr/bin/env python3
"""Read-only audit: original Renault Windows ZIP versus one prepared .rdpkg.

For each original source entry, require a packaged counterpart with identical
bytes, or (HTML/JS only) bytes resulting exactly from the project's known
path-case normalization rules. Never infer two adjacent NT codes are the same.
No extraction, conversion, deletion, Drive API, or output report writes.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import html
import json
from pathlib import Path, PurePosixPath
import posixpath
import re
import stat
import sys
import urllib.parse
from typing import Any
from zipfile import BadZipFile, ZipFile

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.convert_paths import TEXT_EXTENSIONS, patch_text, split_suffix
from core.volumes import infer_volume_identity
from tools.analyze_archive import extract_references
from tools.validate_rdpkg_payload import validate_rdpkg_payload


def _hash(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _safe_name(name: str) -> bool:
    if not name or name.startswith("/") or "\\" in name or "\x00" in name:
        return False
    segments = name.rstrip("/").split("/")
    return bool(segments) and all(part not in {"", ".", ".."} for part in segments) and not re.match(
        r"^[A-Za-z]:", segments[0]
    )


def _original_entries(archive: ZipFile) -> dict[str, Any]:
    result: dict[str, Any] = {}
    casefolded: set[str] = set()
    for item in archive.infolist():
        if not _safe_name(item.filename):
            raise ValueError("original ZIP contains an unsafe entry name")
        fold = item.filename.rstrip("/").casefold()
        if fold in casefolded:
            raise ValueError("original ZIP has a duplicate/case-colliding entry")
        casefolded.add(fold)
        if stat.S_IFMT(item.external_attr >> 16) == stat.S_IFLNK:
            raise ValueError("original ZIP contains symbolic-link entries")
        if item.flag_bits & 1:
            raise ValueError("original ZIP contains encrypted entries")
        if not item.is_dir():
            result[item.filename] = item
    return result


def _reference_counts(
    archive: ZipFile,
    filenames: set[str],
    lower_map: dict[str, list[str]],
    *,
    max_examples: int = 8,
) -> dict[str, Any]:
    counts: Counter[str] = Counter()
    sample: list[str] = []
    for name in sorted(filenames):
        if PurePosixPath(name).suffix.lower() not in {".htm", ".html"}:
            continue
        text = archive.read(name).decode("latin-1")
        for ref in extract_references(text):
            raw = html.unescape(ref)
            if raw.lower().startswith(("http:", "https:", "mailto:", "javascript:", "data:", "//")):
                counts["external_or_dynamic"] += 1
                continue
            path, _ = split_suffix(raw)
            path = urllib.parse.unquote(path).replace("\\", "/")
            if not path:
                continue
            # Old VISU HTML forms MENU/COD + ".HTM" in JavaScript.
            # The extension fragment is not a literal missing file.
            if re.fullmatch(r"\\.(?:htm|html|pdf|gif|ico|js)", path, re.IGNORECASE):
                counts["dynamic_suffix_marker"] += 1
                continue
            if path.startswith("/"):
                target = posixpath.normpath(path.lstrip("/"))
            else:
                target = posixpath.normpath(posixpath.join(posixpath.dirname(name), path))
            if target.startswith("../") or target == "..":
                counts["outside_selected_volume"] += 1
            elif target in filenames:
                counts["resolved_exact"] += 1
            elif len(lower_map.get(target.casefold(), [])) == 1:
                counts["case_sensitive_mismatch"] += 1
            else:
                counts["potential_unresolved"] += 1
                if len(sample) < max_examples:
                    sample.append(name)
    return {"counts": dict(sorted(counts.items())), "potential_unresolved_source_examples": sample}


def audit_original_zip_to_rdpkg(
    original_zip: Path,
    package_path: Path,
    *,
    check_static_links: bool = True,
) -> dict[str, Any]:
    original_zip = Path(original_zip)
    package_path = Path(package_path)
    if original_zip.is_symlink() or package_path.is_symlink():
        raise ValueError("symbolic-link archive paths are not supported")
    if not original_zip.is_file() or not package_path.is_file():
        raise ValueError("original ZIP or prepared package not found")

    # Verify every embedded package file CRC before comparing original content.
    validate_rdpkg_payload(package_path)
    with ZipFile(original_zip, "r") as source, ZipFile(package_path, "r") as package:
        original_files = _original_entries(source)
        if source.testzip() is not None:
            raise ValueError("original ZIP CRC check failed")
        manifest = json.loads(package.read("rdpkg.json").decode("utf-8"))
        volume = manifest.get("volume")
        if not isinstance(volume, dict):
            raise ValueError("prepared volume metadata missing")
        folder = volume.get("source_folder")
        if not isinstance(folder, str) or not _safe_name(folder) or folder.endswith("/"):
            raise ValueError("prepared volume source folder is invalid")
        prefix = folder + "/"
        original_info = infer_volume_identity(original_zip.stem)
        document_code = volume.get("document_code")
        if original_info.get("document_code") and original_info["document_code"] != document_code:
            raise ValueError("original ZIP NT differs from prepared package")
        if original_info.get("date") and volume.get("date") and original_info["date"] != volume["date"]:
            raise ValueError("original ZIP date differs from prepared package")

        packaged = {
            item.filename for item in package.infolist()
            if not item.is_dir() and item.filename.startswith(prefix)
        }
        source_names = set(original_files)
        # A real one-volume ZIP must be scoped to its named volume. Do not
        # silently merge separate NTs or unknown sibling roots.
        outside = sorted(name for name in source_names if not name.startswith(prefix))
        if outside:
            raise ValueError("original ZIP contains files outside the selected source volume")
        missing = sorted(source_names - packaged)
        extra = sorted(packaged - source_names)

        lower_map: dict[str, list[str]] = defaultdict(list)
        for name in source_names:
            lower_map[name.casefold()].append(name)

        counts: Counter[str] = Counter()
        changed_expected: Counter[str] = Counter()
        mismatches: list[dict[str, str]] = []
        for name in sorted(source_names & packaged):
            original = source.read(name)
            converted = package.read(name)
            extension = PurePosixPath(name).suffix.lower()
            if original == converted:
                counts["identical"] += 1
                counts["identical_" + (extension.lstrip(".") or "other")] += 1
                continue
            if extension in TEXT_EXTENSIONS:
                text = original.decode("latin-1")
                normalized, changes = patch_text(name, text, source_names, lower_map)
                if normalized.encode("latin-1") == converted and changes:
                    counts["allowed_text_normalization"] += 1
                    for change in changes:
                        changed_expected[change.get("kind") or "unknown"] += 1
                    continue
                reason = "unexpected_html_or_js_modification"
            else:
                reason = "modified_nontext_asset"
            counts["unexplained_modification"] += 1
            mismatches.append({"relative_path": name, "kind": reason})

        for name in missing:
            mismatches.append({"relative_path": name, "kind": "missing_in_prepared_package"})
        for name in extra:
            mismatches.append({"relative_path": name, "kind": "unexpected_in_prepared_package"})
        status = "PASS" if not mismatches else "FAIL"
        report: dict[str, Any] = {
            "schema_version": 1,
            "scope": "real_original_windows_zip_to_prepared_rdpkg_volume",
            "document_code": document_code,
            "date": volume.get("date"),
            "status": status,
            "original_files": len(source_names),
            "packaged_source_files": len(packaged),
            "missing": len(missing),
            "extra": len(extra),
            "unexplained_modifications": counts["unexplained_modification"],
            "identical_files": counts["identical"],
            "allowed_text_normalizations": counts["allowed_text_normalization"],
            "accepted_normalization_kinds": dict(sorted(changed_expected.items())),
            "mismatches": mismatches[:25],
            "mismatches_truncated": len(mismatches) > 25,
            "no_writes": True,
            "notes": [
                "Every original ZIP file is checked against matching .rdpkg volume content.",
                "Only exact byte preservation or converter-defined HTML/JS normalization is accepted.",
                "Generated _renault/ Modern/Runtime/Fast Pack are outside original-file parity.",
                "Static HTML reference counts are diagnostic, not proof of browser/JavaScript behavior.",
                "Actual PDF rendering, native viewer UX and licensing rights are separate checks.",
            ],
        }
        if check_static_links:
            report["static_links"] = _reference_counts(package, packaged, lower_map)
        return report


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only real Renault original ZIP versus .rdpkg audit")
    parser.add_argument("--original", required=True, type=Path)
    parser.add_argument("--package", required=True, type=Path)
    parser.add_argument("--skip-link-scan", action="store_true")
    args = parser.parse_args()
    try:
        report = audit_original_zip_to_rdpkg(
            args.original, args.package, check_static_links=not args.skip_link_scan
        )
    except (ValueError, OSError, BadZipFile, RuntimeError, KeyError, UnicodeError):
        parser.error("read-only original/package audit failed; inspect source files privately")
    print(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2))
    return 0 if report["status"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
