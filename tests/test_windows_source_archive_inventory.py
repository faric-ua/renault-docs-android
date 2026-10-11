"""Issue #30: read-only Windows Renault archive inventory foundation."""
from __future__ import annotations

import contextlib
import hashlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from zipfile import ZipFile

from tools.inventory_windows_archives import build_inventory, main


class WindowsArchiveInventoryTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name) / "Original Windows Renault"
        self.root.mkdir()

    def tearDown(self):
        self.tmp.cleanup()

    def zip(self, filename, member="INDEX.HTM"):
        path = self.root / filename
        with ZipFile(path, "w") as archive:
            archive.writestr(member, "index")
        return path

    def test_filename_identity_is_unverified_and_duplicates_not_merged(self):
        k = self.zip("KangooII X61_NT8486_Visu v5.0_2009.08.31(RUS).zip")
        m = self.zip("Megane-II_X61_NT8486_Visu v5.0_2009.08.31.zip")
        original = {path.name: path.read_bytes() for path in (k, m)}
        first = build_inventory(self.root)
        self.assertEqual(2, first["archive_count"])
        self.assertEqual("top_level_only", first["scan_scope"])
        self.assertEqual(first, build_inventory(self.root))
        results = {item["file_name"]: item for item in first["archives"]}
        kangoo = results[k.name]
        self.assertEqual("candidate_unverified", kangoo["status"])
        self.assertEqual(["kangoo"], kangoo["explicit_models"])
        self.assertEqual("NT8486", kangoo["identity"]["document_code"])
        self.assertEqual("2009-08-31", kangoo["identity"]["date"])
        self.assertEqual("filename_only_unverified", kangoo["identity_source"])
        self.assertEqual([m.name], kangoo["possible_duplicate_paths"])
        self.assertEqual([k.name], results[m.name]["possible_duplicate_paths"])
        self.assertNotIn("sha256", kangoo)
        self.assertEqual(original, {path.name: path.read_bytes() for path in (k, m)})
        self.assertEqual(2, len(list(self.root.iterdir())))

    def test_prepared_packages_even_when_disguised_as_zip_are_not_sources(self):
        embedded = self.zip("Megane-II_NT8340A_2006.04.18.zip", member="rdpkg.json")
        ready = self.root / "Megane-II_NT8340A_2006-04-18.rdpkg"
        ready.write_bytes(b"PK\x03\x04 already prepared")
        result = build_inventory(self.root)
        status = {entry["file_name"]: entry["status"] for entry in result["archives"]}
        self.assertEqual("prepared_payload_not_source", status[embedded.name])
        self.assertEqual("prepared_package_not_source", status[ready.name])

    def test_bad_magic_and_corrupt_zip_never_count_as_candidates(self):
        incorrect = self.root / "fake_NT8340A.zip"
        incorrect.write_bytes(b"not a zip")
        corrupt = self.root / "corrupt_NT8341A.zip"
        corrupt.write_bytes(b"PK\x03\x04" + b"badzip")
        statuses = {x["file_name"]: x["status"] for x in build_inventory(self.root)["archives"]}
        self.assertEqual("invalid_header", statuses[incorrect.name])
        self.assertEqual("invalid_zip_structure", statuses[corrupt.name])

    def test_header_only_7z_rar_do_not_claim_payload_valid(self):
        a = self.root / "X61_NT8486.7z"
        a.write_bytes(b"7z\xbc\xaf\x27\x1c" + b"headeronly")
        r = self.root / "X74_NT8340A.rar"
        r.write_bytes(b"Rar!\x1a\x07\x00" + b"headeronly")
        out = build_inventory(self.root)
        self.assertEqual({"candidate_unverified": 2}, out["status_counts"])
        self.assertEqual([], out["archives"][0]["explicit_models"])
        self.assertIn("No remote Google Drive", out["notes"][2])

    def test_recursive_is_opt_in_and_symlinks_are_skipped(self):
        sub = self.root / "Nested"
        sub.mkdir()
        original = sub / "Laguna-II_NT8183A_2001.01.01.zip"
        with ZipFile(original, "w") as z:
            z.writestr("INDEX.HTM", "hello")
        self.assertEqual(0, build_inventory(self.root)["archive_count"])
        linked = self.root / "alias.zip"
        try:
            linked.symlink_to(original)
        except (OSError, NotImplementedError):
            pass
        deep = build_inventory(self.root, recursive=True)
        self.assertEqual(1, deep["archive_count"])
        self.assertEqual("Nested/Laguna-II_NT8183A_2001.01.01.zip", deep["archives"][0]["relative_path"])
        if linked.is_symlink():
            self.assertEqual(1, deep["skipped_symlinks"])

    def test_sha_is_opt_in_and_stdout_only(self):
        package = self.zip("NT8340A_2006.04.18.zip")
        expected = hashlib.sha256(package.read_bytes()).hexdigest()
        self.assertEqual(expected, build_inventory(self.root, include_sha256=True)["archives"][0]["sha256"])
        before = {p.name for p in self.root.iterdir()}
        stream = io.StringIO()
        with patch("sys.argv", ["inventory_windows_archives.py", str(self.root)]):
            with contextlib.redirect_stdout(stream):
                self.assertEqual(0, main())
        report = json.loads(stream.getvalue())
        self.assertEqual(1, report["archive_count"])
        self.assertNotIn("sha256", report["archives"][0])
        self.assertEqual(before, {p.name for p in self.root.iterdir()})
        self.assertNotIn(str(self.root.parent), stream.getvalue())

    def test_requires_existing_directory_and_does_not_create_one(self):
        nonexistent = self.root / "does-not-exist"
        with self.assertRaises(ValueError):
            build_inventory(nonexistent)
        self.assertFalse(nonexistent.exists())


if __name__ == "__main__":
    unittest.main()
