"""Read-only integrity checks against real-format synthetic prepared Renault .rdpkg."""
from __future__ import annotations

import json
import stat
import tempfile
import unittest
import warnings
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile, ZipInfo

from tools.validate_rdpkg_payload import (
    PackageIntegrityError,
    validate_rdpkg_payload,
)


class ValidateRdpkgPayloadTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.path = self.root / "MeganeII-NT8340A.rdpkg"

    def tearDown(self):
        self.temporary.cleanup()

    def make(self, *, count=None, byte_count=None, entrypoint=None,
             dataset_project="megane-ii", extra=None, format_version=1):
        payload_path = "MeganeII_NT8340A_2006.04.18/INDEX.HTM"
        dataset = {"schema_version": 1, "id": "megane-ii",
                   "project_id": dataset_project, "title": "Megane II",
                   "volumes": [{"id": "megane-ii-nt8340a", "document_code": "NT8340A"}]}
        dataset_bytes = json.dumps(dataset).encode("utf-8")
        html_bytes = b"<html>document-payload</html>"
        files = [("renault-dataset.json", dataset_bytes), (payload_path, html_bytes)]
        files.extend(extra or [])
        package = {
            "schema_version": format_version,
            "format": "renault-volume-package-v1",
            "project_id": "megane-ii",
            "package_id": "megane-ii-nt8340a-2006-04-18",
            "dataset_manifest": "renault-dataset.json",
            "volume": {"id": "megane-ii-nt8340a", "document_code": "NT8340A",
                       "date": "2006-04-18", "entrypoint": entrypoint or payload_path},
            "payload_file_count": len(files) if count is None else count,
            "payload_bytes": sum(len(data) for _, data in files) if byte_count is None else byte_count,
        }
        with ZipFile(self.path, "w", compression=ZIP_STORED) as archive:
            archive.writestr("rdpkg.json", json.dumps(package))
            for name, data in files:
                archive.writestr(name, data)
        return self.path

    def assert_invalid(self, expected):
        with self.assertRaisesRegex(PackageIntegrityError, expected):
            validate_rdpkg_payload(self.path)

    def test_valid_payload_crc_all_bytes_and_original_unchanged(self):
        path = self.make()
        before = path.read_bytes()
        result = validate_rdpkg_payload(path)
        self.assertEqual("PAYLOAD_INTEGRITY_PASS", result["status"])
        self.assertEqual(2, result["payload_file_count"])
        self.assertGreater(result["payload_bytes"], 20)
        self.assertEqual(before, path.read_bytes())
        self.assertEqual([path.name], [x.name for x in self.root.iterdir()])

    def test_payload_file_count_in_manifest_must_be_exact(self):
        self.make(count=1)
        self.assert_invalid("payload file count mismatch")

    def test_payload_bytes_in_manifest_must_be_exact(self):
        self.make(byte_count=1)
        self.assert_invalid("payload bytes mismatch")

    def test_missing_entrypoint_rejected_without_extracting(self):
        self.make(entrypoint="missing/INDEX.HTM")
        self.assert_invalid("entrypoint not in payload")

    def test_mismatched_project_rejected(self):
        self.make(dataset_project="kangoo-ii")
        self.assert_invalid("project_id differs")

    def test_zip_path_traversal_rejected(self):
        self.make(extra=[("../outside.txt", b"do not extract")])
        self.assert_invalid("unsafe archive entry path")

    def test_slash_backslash_and_drive_paths_rejected(self):
        for name in ("C:/outside.txt", "root\\evil.txt", "./index.htm", "/tmp/evil"):
            with self.subTest(name=name):
                self.make(extra=[(name, b"bad")])
                self.assert_invalid("unsafe archive entry path")

    def test_duplicate_case_insensitive_entry_rejected(self):
        self.make(extra=[("RENAULT-DATASET.JSON", b"not metadata")])
        self.assert_invalid("duplicate archive entry name")

    def test_symlink_entry_rejected(self):
        self.make()
        sym = ZipInfo("MeganeII_NT8340A_2006.04.18/fake-link")
        sym.create_system = 3
        sym.external_attr = (stat.S_IFLNK | 0o777) << 16
        with ZipFile(self.path, "a") as archive:
            archive.writestr(sym, "target")
        self.assert_invalid("symlink entries not supported")

    def test_modified_payload_crc_fails_even_if_manifest_counters_match(self):
        path = self.make()
        before = path.read_bytes()
        self.assertIn(b"document-payload", before)
        corrupt = before.replace(b"document-payload", b"document-payloae", 1)
        self.assertEqual(len(corrupt), len(before))
        path.write_bytes(corrupt)
        self.assert_invalid("invalid or unreadable ZIP payload")

    def test_declared_payload_size_above_limit_rejected(self):
        self.make()
        with self.assertRaisesRegex(PackageIntegrityError, "exceeds size limit"):
            validate_rdpkg_payload(self.path, max_uncompressed_bytes=1)

    def test_invalid_boolean_counters_rejected(self):
        self.make(count=True)
        self.assert_invalid("payload_file_count must be a nonnegative integer")

    def test_invalid_zip_header_rejected(self):
        self.path.write_bytes(b"not a zip")
        self.assert_invalid("invalid or unreadable ZIP payload")


if __name__ == "__main__":
    unittest.main()
