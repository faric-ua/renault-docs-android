"""Issue #30: verify normalized prepared volume files against one built .rdpkg.

All source/package fixtures live in temporary directories; no real Renault
Drive or user files are involved.
"""
from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from core.rdpkg import build_rdpkg
from tools.compare_prepared_volume_to_rdpkg import compare_prepared_volume, main
from tools.validate_rdpkg_payload import PackageIntegrityError


class PreparedVolumeRdpkgParityTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(self.temp.name)
        self.prepared = self.base / "Megane II_android"
        self.prepared.mkdir()
        self.first = self.prepared / "Megane II_NT8340A_2006.04.18"
        self.second = self.prepared / "Megane II_NT8342A_2006.10.09"
        for folder, marker in ((self.first, "NT8340A"), (self.second, "NT8342A")):
            folder.mkdir()
            (folder / "INDEX.HTM").write_bytes(("<html>" + marker + "</html>").encode("utf-8"))
            (folder / "PDF").mkdir()
            (folder / "PDF" / "101(pines).pdf").write_bytes(b"%PDF-1.4\n" + marker.encode())
        (self.prepared / "renault-dataset.json").write_text(json.dumps({
            "schema_version": 1, "id": "megane-ii",
            "project_id": "megane-ii", "title": "Megane II",
            "manufacturer": "Renault", "model": "Megane II",
            "entrypoint": "Megane II_NT8340A_2006.04.18/INDEX.HTM",
        }), encoding="utf-8")
        self.out = self.base / "volume.rdpkg"

    def tearDown(self):
        self.temp.cleanup()

    def build(self, code="NT8340A"):
        build_rdpkg(self.prepared, self.out, volume_selector=code)
        return self.out

    def check(self, code="NT8340A"):
        return compare_prepared_volume(self.prepared, self.out, volume_selector=code)

    def test_real_packager_single_volume_parity_pass_and_no_mutations(self):
        self.build()
        files = [self.first / "INDEX.HTM", self.first / "PDF" / "101(pines).pdf",
                 self.second / "INDEX.HTM", self.out]
        before = {str(x): x.read_bytes() for x in files}
        result = self.check()
        self.assertEqual("PASS", result["result"])
        self.assertEqual("NT8340A", result["document_code"])
        self.assertEqual(2, result["source_files"])
        self.assertEqual(2, result["matching_files"])
        self.assertEqual(0, result["mismatch_count"])
        self.assertEqual([], result["metadata_problems"])
        self.assertEqual(before, {str(x): x.read_bytes() for x in files})
        self.assertNotIn(str(self.base), json.dumps(result))

    def test_detects_original_prepared_file_modified_after_packaging(self):
        self.build()
        (self.first / "INDEX.HTM").write_text("<html>different</html>")
        result = self.check()
        self.assertEqual("FAIL", result["result"])
        self.assertIn({"relative_path": self.first.name + "/INDEX.HTM",
                       "kind": "different_sha256"}, result["mismatches"])

    def test_detects_missing_prepared_file_versus_package(self):
        self.build()
        (self.first / "PDF" / "101(pines).pdf").unlink()
        result = self.check()
        self.assertEqual("FAIL", result["result"])
        self.assertIn("unexpected_in_package", [x["kind"] for x in result["mismatches"]])

    def test_detects_added_prepared_file_not_in_package(self):
        self.build()
        (self.first / "PDF" / "102(pines).pdf").write_bytes(b"extra")
        result = self.check()
        self.assertEqual("FAIL", result["result"])
        self.assertIn("missing_from_package", [x["kind"] for x in result["mismatches"]])

    def test_distinct_renault_nt_documents_are_not_interchangeable(self):
        self.build("NT8340A")
        result = self.check("NT8342A")
        self.assertEqual("FAIL", result["result"])
        self.assertIn("document_code_mismatch", result["metadata_problems"])
        self.assertEqual("NT8342A", result["document_code"])
        self.assertTrue(result["mismatch_count"] > 0)

    def test_selecting_other_nt_is_allowed_when_built_for_it(self):
        self.build("NT8342A")
        result = self.check("NT8342A")
        self.assertEqual("PASS", result["result"])
        self.assertEqual("NT8342A", result["document_code"])

    def test_rejects_corrupt_rdpkg(self):
        self.out.write_bytes(b"not a .rdpkg")
        with self.assertRaises(PackageIntegrityError):
            self.check()

    def test_rejects_symlink_within_source_folder(self):
        self.build()
        link = self.first / "sneaky.html"
        try:
            link.symlink_to(self.second / "INDEX.HTM")
        except (NotImplementedError, OSError):
            self.skipTest("symlink creation unavailable")
        with self.assertRaisesRegex(ValueError, "symbolic links"):
            self.check()

    def test_requires_volume_and_bounds(self):
        self.build()
        with self.assertRaisesRegex(ValueError, "selector required"):
            compare_prepared_volume(self.prepared, self.out, volume_selector="")
        with self.assertRaisesRegex(ValueError, "audit limit"):
            compare_prepared_volume(self.prepared, self.out, volume_selector="NT8340A",
                                    max_files=1)
        with self.assertRaisesRegex(ValueError, "audit limit"):
            compare_prepared_volume(self.prepared, self.out, volume_selector="NT8340A",
                                    max_uncompressed_bytes=1)

    def test_cli_stdout_only_and_nonzero_on_mismatch(self):
        self.build()
        original = self.out.read_bytes()
        args = ["compare_prepared_volume_to_rdpkg.py", "--prepared-root",
                str(self.prepared), "--package", str(self.out), "--volume", "NT8340A"]
        stream = io.StringIO()
        with patch("sys.argv", args), contextlib.redirect_stdout(stream):
            self.assertEqual(0, main())
        self.assertEqual("PASS", json.loads(stream.getvalue())["result"])
        self.assertEqual(original, self.out.read_bytes())
        (self.first / "INDEX.HTM").write_text("changed", encoding="utf-8")
        with patch("sys.argv", args), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(2, main())


if __name__ == "__main__":
    unittest.main()
