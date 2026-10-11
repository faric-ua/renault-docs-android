#!/usr/bin/env python3
"""Read-only validation of one prepared Renault volume's Modern/Runtime graph.

Every section must have its source HTML, sharded Runtime JSON and a matching
section code across generated indexes. Every Fast Pack member must match its
outer .rdpkg source byte-for-byte. No package extraction, write or Drive API.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import sys
from typing import Any
from zipfile import BadZipFile, ZipFile

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from tools.validate_rdpkg_payload import PackageIntegrityError, validate_rdpkg_payload


def audit_native_graph(path: Path, *, max_sections: int = 30000) -> dict[str, Any]:
    if type(max_sections) is not int or max_sections < 1:
        raise ValueError("max_sections must be a positive integer")
    validate_rdpkg_payload(path)
    problems: list[str] = []
    with ZipFile(path) as package:
        names = {info.filename for info in package.infolist() if not info.is_dir()}
        manifest = json.loads(package.read("rdpkg.json"))
        dataset = json.loads(package.read("renault-dataset.json"))
        volume = manifest["volume"]
        nt = volume.get("document_code")
        if not isinstance(nt, str) or not nt:
            raise ValueError("package is missing full document code")

        def load_dataset_index(key: str) -> dict[str, Any]:
            name = dataset.get(key)
            if not isinstance(name, str) or not name.startswith("_renault/") or name not in names:
                raise ValueError("missing generated index: " + key)
            info = package.getinfo(name)
            if info.file_size > 32 * 1024**2:
                raise ValueError("generated index too large: " + key)
            result = json.loads(package.read(name))
            if not isinstance(result, dict):
                raise ValueError("generated index is not an object: " + key)
            return result

        modern = load_dataset_index("modern_index")
        sections = load_dataset_index("modern_sections")
        ir_index = load_dataset_index("runtime_ir_index")
        coverage = load_dataset_index("runtime_ir_coverage")

        if len(modern.get("navigation", {}).get("volumes", [])) != 1:
            problems.append("modern_index_not_one_volume")
        else:
            v = modern["navigation"]["volumes"][0]
            if v.get("document_code") != nt:
                problems.append("modern_index_nt_mismatch")

        modern_volumes = sections.get("volumes")
        runtime_volumes = ir_index.get("volumes")
        if not isinstance(modern_volumes, list) or len(modern_volumes) != 1:
            raise ValueError("modern sections must include exactly one volume")
        if not isinstance(runtime_volumes, list) or len(runtime_volumes) != 1:
            raise ValueError("runtime index must include exactly one volume")

        sv = modern_volumes[0]
        rv = runtime_volumes[0]
        if sv.get("document_code") != nt or rv.get("document_code") != nt:
            problems.append("generated_section_nt_mismatch")
        modern_sections = sv.get("sections")
        mapping = rv.get("sections")
        entries = rv.get("section_entries")
        if not isinstance(modern_sections, list) or not isinstance(mapping, dict) or not isinstance(entries, list):
            raise ValueError("generated section maps are malformed")
        if len(modern_sections) > max_sections:
            raise ValueError("section count exceeds allowed audit maximum")
        modern_codes = []
        checked_section_html = 0
        for section in modern_sections:
            if not isinstance(section, dict):
                raise ValueError("invalid modern section object")
            code = str(section.get("code") or "")
            entrypoint = section.get("entrypoint")
            if not code:
                problems.append("modern_section_code_missing")
            modern_codes.append(code)
            if isinstance(entrypoint, str) and entrypoint in names:
                checked_section_html += 1
            else:
                problems.append("modern_section_html_missing")
        if len(set(modern_codes)) != len(modern_codes):
            problems.append("modern_section_code_duplicated")
        runtime_codes = set(mapping)
        if set(modern_codes) != runtime_codes:
            problems.append("runtime_modern_code_set_mismatch")
        entry_codes = {str(x.get("code")) for x in entries if isinstance(x, dict)}
        if entry_codes != set(modern_codes) or len(entries) != len(modern_codes):
            problems.append("runtime_section_entries_mismatch")

        counts = [sections.get("section_count"), ir_index.get("section_count"),
                  coverage.get("section_count"), rv.get("section_count")]
        if any(count != len(modern_codes) for count in counts):
            problems.append("generated_section_counts_mismatch")

        checked_shards = 0
        for code, shard in mapping.items():
            if not isinstance(shard, str) or shard not in names:
                problems.append("runtime_section_shard_missing")
                continue
            if package.getinfo(shard).file_size > 64 * 1024**2:
                problems.append("runtime_section_shard_oversized")
                continue
            try:
                document = json.loads(package.read(shard))
                if not isinstance(document, dict):
                    problems.append("runtime_section_shard_not_object")
                else:
                    checked_shards += 1
            except (ValueError, UnicodeDecodeError):
                problems.append("runtime_section_shard_json_invalid")
        doc_path = rv.get("documentation_path")
        if not isinstance(doc_path, str) or doc_path not in names:
            problems.append("runtime_documentation_shard_missing")
        if volume.get("entrypoint") not in names or rv.get("classic_entrypoint") not in names:
            problems.append("classic_entrypoint_missing")

        fast = dataset.get("fast_pack")
        if not isinstance(fast, dict):
            raise ValueError("fast pack metadata missing")
        fast_path = fast.get("path")
        if not isinstance(fast_path, str) or fast_path not in names:
            raise ValueError("fast pack ZIP missing")
        raw = package.read(fast_path)
        if hashlib.sha256(raw).hexdigest() != fast.get("sha256"):
            problems.append("fast_pack_sha256_mismatch")
        fast_members = 0
        fast_bytes_mismatched = 0
        fast_missing = 0
        with ZipFile(io.BytesIO(raw)) as nested:
            if nested.testzip() is not None:
                problems.append("fast_pack_zip_crc_failed")
            for item in nested.infolist():
                if item.is_dir():
                    continue
                fast_members += 1
                if item.filename not in names:
                    fast_missing += 1
                elif nested.read(item) != package.read(item.filename):
                    fast_bytes_mismatched += 1
        if fast_members != fast.get("file_count"):
            problems.append("fast_pack_count_mismatch")
        if fast_missing:
            problems.append("fast_pack_missing_outer_assets")
        if fast_bytes_mismatched:
            problems.append("fast_pack_outer_asset_byte_mismatch")

        compiler_unsupported = coverage.get("unsupported_action_count")
        compiler_warnings = coverage.get("warning_count")
        if not isinstance(compiler_unsupported, int) or not isinstance(compiler_warnings, int):
            problems.append("compiler_coverage_counters_missing")
            compiler_unsupported = compiler_unsupported if isinstance(compiler_unsupported, int) else None
            compiler_warnings = compiler_warnings if isinstance(compiler_warnings, int) else None
        problems = sorted(set(problems))
        status = "FAIL" if problems else (
            "REVIEW_REQUIRED" if compiler_unsupported or compiler_warnings else "PASS"
        )
        return {
            "schema_version": 1,
            "status": status,
            "document_code": nt,
            "sections": len(modern_codes),
            "section_html_found": checked_section_html,
            "runtime_shard_json_objects_parsed": checked_shards,
            "fast_pack_files_checked": fast_members,
            "fast_pack_missing_files": fast_missing,
            "fast_pack_byte_mismatches": fast_bytes_mismatched,
            "unsupported_actions": compiler_unsupported,
            "compiler_warnings": compiler_warnings,
            "problems": problems,
            "notes": [
                "Derived Modern/Runtime paths and Fast Pack asset bytes verified inside the .rdpkg.",
                "Does not execute Android UI, JavaScript, PDF page rendering, or validate scheme semantics.",
                "No files extracted, modified, written, uploaded, or removed.",
            ],
        }


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only Renault .rdpkg Modern/Runtime/Fast Pack audit")
    parser.add_argument("--package", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = audit_native_graph(args.package)
    except (ValueError, OSError, RuntimeError, PackageIntegrityError, BadZipFile, KeyError, TypeError):
        parser.error("unable to validate prepared package graph; inspect input privately")
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 0 if result["status"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
