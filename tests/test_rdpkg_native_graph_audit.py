"""Read-only Modern/Runtime/Fast Pack consistency, synthetic 1-section package."""
from __future__ import annotations

import hashlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from tools.audit_rdpkg_native_graph import audit_native_graph


class RdpkgNativeGraphAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.file = self.root / "Megane-II_NT8340A.rdpkg"

    def tearDown(self):
        self.temp.cleanup()

    def make(self, *, missing_html=False, wrong_section_path=False,
             different_fast_pack_asset=False, unsupported=0, warning=0,
             wrong_modern_nt=False):
        folder = "Megane-II_NT8340A_2006.04.18"
        index_path = folder + "/INDEX.HTM"
        page = folder + "/RUS/HTM/MENU/101.HTM"
        shard = "_renault/runtime-ir/sections/megane-nt8340a/101.json"
        document = "_renault/runtime-ir/documentation/megane-nt8340a.json"
        classic = b"<html><a href='RUS/HTM/MENU/101.HTM'>section</a></html>"
        section = b"<html>101</html>"
        assets = {index_path: classic, page: section}
        fast_assets = {index_path: classic, page: b"ALTERED" if different_fast_pack_asset else section}
        fast_io = io.BytesIO()
        with ZipFile(fast_io, "w") as nested:
            for name, data in fast_assets.items():
                nested.writestr(name, data)
        fast_bytes = fast_io.getvalue()
        fast_path = "_renault/fast-content-test.zip"
        volume = {
            "id": "megane-nt8340a", "document_code": "NT8340A",
            "date": "2006-04-18", "entrypoint": index_path,
            "source_folder": folder,
        }
        dataset = {
            "schema_version": 1, "project_id": "megane-ii",
            "volumes": [volume], "entrypoint": index_path,
            "modern_index": "_renault/modern-index.json",
            "modern_sections": "_renault/modern-sections.json",
            "runtime_ir_index": "_renault/runtime-ir-index.json",
            "runtime_ir_coverage": "_renault/runtime-ir-coverage.json",
            "fast_pack": {"path": fast_path,
                          "sha256": hashlib.sha256(fast_bytes).hexdigest(),
                          "file_count": 2},
        }
        modern = {"navigation": {"volumes": [{"document_code":
                    "NT8342A" if wrong_modern_nt else "NT8340A"}]}}
        sections = {
            "section_count": 1, "volumes": [
                {"document_code": "NT8340A",
                 "sections": [{"code": "101",
                               "entrypoint": "missing/101.HTM" if missing_html else page}]}
            ],
        }
        runtime_index = {
            "section_count": 1, "volumes": [{
                "document_code": "NT8340A", "section_count": 1,
                "sections": {"101": "missing/shard.json" if wrong_section_path else shard},
                "section_entries": [{"code": "101", "path": shard, "entrypoint": page}],
                "documentation_path": document, "classic_entrypoint": index_path,
            }],
        }
        coverage = {"section_count": 1, "unsupported_action_count": unsupported,
                    "warning_count": warning}
        payload = {**assets,
                   "_renault/modern-index.json": json.dumps(modern).encode(),
                   "_renault/modern-sections.json": json.dumps(sections).encode(),
                   "_renault/runtime-ir-index.json": json.dumps(runtime_index).encode(),
                   "_renault/runtime-ir-coverage.json": json.dumps(coverage).encode(),
                   shard: b"{}", document: b"{}", fast_path: fast_bytes,
                   "renault-dataset.json": json.dumps(dataset).encode()}
        manifest = {
            "format": "renault-volume-package-v1", "schema_version": 1,
            "package_id": "megane-ii-nt8340a-2006-04-18",
            "project_id": "megane-ii", "volume": volume,
            "dataset_manifest": "renault-dataset.json",
            "payload_file_count": len(payload),
            "payload_bytes": sum(len(x) for x in payload.values()),
        }
        with ZipFile(self.file, "w") as archive:
            archive.writestr("rdpkg.json", json.dumps(manifest))
            for name, data in payload.items():
                archive.writestr(name, data)

    def test_valid_graph_and_fast_pack_full_binary_parity_read_only(self):
        self.make()
        before = self.file.read_bytes()
        result = audit_native_graph(self.file)
        self.assertEqual("PASS", result["status"])
        self.assertEqual(1, result["sections"])
        self.assertEqual(1, result["runtime_shard_json_objects_parsed"])
        self.assertEqual(1, result["section_html_found"])
        self.assertEqual(2, result["fast_pack_files_checked"])
        self.assertEqual(0, result["fast_pack_byte_mismatches"])
        self.assertEqual(before, self.file.read_bytes())
        self.assertEqual([self.file.name], [x.name for x in self.root.iterdir()])

    def test_missing_modern_html_fails(self):
        self.make(missing_html=True)
        self.assertIn("modern_section_html_missing",
                      audit_native_graph(self.file)["problems"])

    def test_wrong_shard_fails(self):
        self.make(wrong_section_path=True)
        self.assertIn("runtime_section_shard_missing",
                      audit_native_graph(self.file)["problems"])

    def test_fast_pack_corrupted_asset_detected_even_if_nested_sha_is_valid(self):
        self.make(different_fast_pack_asset=True)
        result = audit_native_graph(self.file)
        self.assertEqual("FAIL", result["status"])
        self.assertEqual(1, result["fast_pack_byte_mismatches"])

    def test_unsupported_runtime_action_requires_review(self):
        self.make(unsupported=1)
        result = audit_native_graph(self.file)
        self.assertEqual("REVIEW_REQUIRED", result["status"])
        self.assertEqual(1, result["unsupported_actions"])

    def test_generated_nt_cannot_claim_wrong_volume(self):
        self.make(wrong_modern_nt=True)
        self.assertIn("modern_index_nt_mismatch",
                      audit_native_graph(self.file)["problems"])

    def test_missing_input_or_broken_zip_rejected(self):
        with self.assertRaises(Exception):
            audit_native_graph(self.file)
        self.file.write_bytes(b"corrupt")
        with self.assertRaises(Exception):
            audit_native_graph(self.file)


if __name__ == "__main__":
    unittest.main()
