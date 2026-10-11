"""Issue #30: verify read-only catalog/Drive metadata reconciliation."""
from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from tools.audit_drive_catalog_metadata import audit_drive_metadata, main


def volume(ident="v1", file_id="secret-drive-id-1", name="Megane-II_NT8340A.rdpkg", size=100):
    return {
        "id": ident,
        "drive_file_id": file_id,
        "file_name": name,
        "size_bytes": size,
        "document_code": "NT8340A",
    }


def catalog(*volumes):
    return {"schema_version": 1, "catalog_version": 4, "projects": [
        {"id": "megane-ii", "title": "Megane II", "volumes": list(volumes)}
    ]}


def listing(*files):
    return {"files": list(files)}


def file(file_id, name, size, mime="application/x-zip"):
    return {"id": file_id, "title": name, "size": str(size), "mime_type": mime}


class DriveMetadataAuditTests(unittest.TestCase):
    def test_equal_filename_different_size_detected_without_deletion(self):
        name = "Megane-II_NT8340A.rdpkg"
        source = catalog(volume())
        files = listing(
            file("secret-drive-id-1", name, 100),
            file("unreferenced-private-id", name, 120),
            file("catalog-json-private-id", "renault-docs-catalog.json", 9000, "application/json"),
        )
        result = audit_drive_metadata(source, files)
        self.assertEqual(1, result["catalog_referenced_volumes"])
        self.assertEqual(3, result["listed_files"])
        self.assertEqual(1, result["referenced_in_listing"])
        self.assertEqual(1, result["finding_counts"]["duplicate_package_filename"])
        self.assertEqual(1, result["finding_counts"]["unreferenced_prepared_package"])
        dupe = next(x for x in result["findings"] if x["kind"] == "duplicate_package_filename")
        self.assertEqual([100, 120], dupe["size_bytes"])
        self.assertEqual(1, dupe["catalog_references"])
        self.assertEqual(2, dupe["copies"])
        self.assertNotIn("secret-drive-id", json.dumps(result))
        self.assertNotIn("unreferenced-private-id", json.dumps(result))

    def test_catalog_file_id_is_authoritative_not_just_filename(self):
        item = volume()
        files = listing(file("different-secret-id", item["file_name"], 100))
        default = audit_drive_metadata(catalog(item), files)
        warning = default["findings"][0]
        self.assertEqual("referenced_file_not_in_snapshot", warning["kind"])
        self.assertEqual("unverified", warning["severity"])
        complete = audit_drive_metadata(catalog(item), files, listing_complete=True)
        self.assertEqual("error", complete["findings"][0]["severity"])

    def test_size_and_name_differences_are_errors(self):
        result = audit_drive_metadata(
            catalog(volume()),
            listing(file("secret-drive-id-1", "Wrong_Name.rdpkg", 999)),
        )
        kinds = {x["kind"] for x in result["findings"]}
        self.assertIn("catalog_filename_mismatch", kinds)
        self.assertIn("catalog_size_mismatch", kinds)

    def test_do_not_confuse_raw_source_archives_with_prepared_catalog(self):
        result = audit_drive_metadata(
            catalog(volume()),
            listing(
                file("secret-drive-id-1", "Megane-II_NT8340A.rdpkg", 100),
                file("raw-source-id", "KangooII_NT8486.zip", 700000),
            ),
        )
        self.assertEqual(["raw_archive_in_prepared_catalog"],
                         [x["kind"] for x in result["findings"]])
        self.assertNotIn("raw-source-id", json.dumps(result))

    def test_supports_wrapped_connector_result_without_network(self):
        x = audit_drive_metadata(catalog(volume()), {"result": listing(
            file("secret-drive-id-1", "Megane-II_NT8340A.rdpkg", 100)
        )})
        self.assertEqual([], x["findings"])
        self.assertEqual(1, x["referenced_in_listing"])

    def test_rejects_duplicate_ids_and_unsafe_catalog_entries(self):
        a = file("repeated-id", "A.rdpkg", 10)
        b = file("repeated-id", "B.rdpkg", 20)
        with self.assertRaisesRegex(ValueError, "repeated provider file ID"):
            audit_drive_metadata(catalog(volume()), listing(a, b))
        with self.assertRaisesRegex(ValueError, "basename"):
            audit_drive_metadata(catalog(volume(name="../Megane.rdpkg")), listing(a))
        with self.assertRaisesRegex(ValueError, "same Drive file ID"):
            audit_drive_metadata(catalog(volume(), volume("v2")), listing(a))

    def test_cli_prints_report_and_does_not_touch_source_files(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            a = root / "catalog.json"
            b = root / "snapshot.json"
            a.write_text(json.dumps(catalog(volume())), encoding="utf-8")
            b.write_text(json.dumps(listing(file(
                "secret-drive-id-1", "Megane-II_NT8340A.rdpkg", 100
            ))), encoding="utf-8")
            originals = {p.name: p.read_bytes() for p in root.iterdir()}
            stream = io.StringIO()
            with patch("sys.argv", [
                "audit_drive_catalog_metadata.py",
                "--catalog", str(a),
                "--listing", str(b),
            ]):
                with contextlib.redirect_stdout(stream):
                    self.assertEqual(0, main())
            rendered = json.loads(stream.getvalue())
            self.assertEqual([], rendered["findings"])
            self.assertEqual(originals, {p.name: p.read_bytes() for p in root.iterdir()})
            self.assertNotIn("secret-drive-id-1", stream.getvalue())

if __name__ == "__main__":
    unittest.main()
