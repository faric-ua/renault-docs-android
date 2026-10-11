"""Issue #30: exact folder allowlist and immutable Renault NT source identity."""
from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from tools.audit_drive_source_metadata import audit_source_metadata, main


def allowlist(*scopes):
    return {"schema_version": 1, "scopes": list(scopes)}


def scope(alias, folder_id, project_id):
    return {"alias": alias, "folder_id": folder_id, "project_id": project_id}


def snapshots(*listings):
    return {"schema_version": 1, "listings": list(listings)}


def listing(folder_id, *files, complete=True):
    return {"folder_id": folder_id, "complete": complete, "files": list(files)}


def file(file_id, name, size):
    return {"id": file_id, "title": name, "file_or_folder": "file", "size": str(size)}


class DriveSourceMetadataAuditTests(unittest.TestCase):
    def test_distinct_nt_documents_are_never_collapsed(self):
        folder = "private-folder-id"
        metadata = snapshots(listing(folder,
            file("private-file-id-1", "Megane II E,L,K 84_NT8340A_Visu v3.0_2006.04.18.zip", 100),
            file("private-file-id-2", "Megane II E,L,K 84 Europe_NT8342A_Visu v3.0_2006.10.09.zip", 200),
        ))
        result = audit_source_metadata(
            allowlist(scope("megane-schemes", folder, "megane-ii")), metadata
        )
        self.assertEqual(2, result["source_archive_count"])
        docs = [x["identity"]["document_code"] for x in result["archives"]]
        self.assertEqual(["NT8340A", "NT8342A"], docs)
        self.assertEqual(["archive_metadata_only"] * 2,
                         [x["status"] for x in result["archives"]])
        self.assertEqual("2006-04-18", result["archives"][0]["identity"]["date"])
        self.assertEqual("Europe", result["archives"][1]["identity"]["region"])
        self.assertEqual(result, audit_source_metadata(
            allowlist(scope("megane-schemes", folder, "megane-ii")), metadata
        ))
        serialized = json.dumps(result)
        self.assertNotIn("private-folder-id", serialized)
        self.assertNotIn("private-file-id", serialized)

    def test_filenames_do_not_override_allowed_project(self):
        result = audit_source_metadata(
            allowlist(scope("megane-source", "private-root", "megane-ii")),
            snapshots(listing("private-root",
                file("private-1", "KangooII X61_NT8486_Visu v5.0_2009.08.31.zip", 80),
                file("private-2", "X61_NT8486_2009.08.31.zip", 80),
            )),
        )
        self.assertEqual("explicit_cross_model_conflict", result["archives"][0]["status"])
        self.assertEqual("model_not_verified", result["archives"][1]["status"])
        self.assertEqual("megane-ii", result["archives"][0]["project_id"])

    def test_no_recursion_unknown_folder_or_accidental_catalog_import(self):
        spec = allowlist(scope("kangoo-originals", "private-kangoo", "kangoo-ii"))
        bogus = snapshots(listing("private-other", file("private-1", "NT8486.zip", 100)))
        with self.assertRaisesRegex(ValueError, "outside the explicit allowlist"):
            audit_source_metadata(spec, bogus)
        single = snapshots(listing("private-kangoo",
            {"id": "nested-folder", "title": "Schematics", "file_or_folder": "folder"},
            file("unknown-id", "NT8486.rdpkg", 500),
            file("zip-id", "KangooII_X61_NT8486.zip", 600),
        ))
        result = audit_source_metadata(spec, single)
        self.assertEqual(1, result["source_archive_count"])
        self.assertEqual("ZIP", result["archives"][0]["declared_format"])
        self.assertNotIn("Schematics", json.dumps(result))
        self.assertNotIn(".rdpkg", json.dumps(result))

    def test_missing_scope_and_partial_listings_remain_unverified(self):
        spec = allowlist(
            scope("laguna-originals", "private-laguna", "laguna-ii"),
            scope("megane-originals", "private-megane", "megane-ii"),
        )
        result = audit_source_metadata(spec, snapshots(listing("private-laguna",
            file("private-1", "laguna 2 2001-2006.zip", 500000), complete=False,
        )))
        self.assertEqual(2, result["allowlisted_scopes"])
        self.assertEqual(1, result["snapshotted_scopes"])
        self.assertEqual(
            [{"scope": "laguna-originals", "kind": "partial_scope_not_complete"},
             {"scope": "megane-originals", "kind": "scope_not_in_snapshot"}],
            result["scope_warnings"],
        )
        self.assertEqual("archive_metadata_only", result["archives"][0]["status"])
        self.assertNotIn("NT", result["archives"][0]["identity"])

    def test_reject_bad_schema_duplicate_ids_aliases_and_unexpected_projects(self):
        s = scope("megane-schemes", "private-root", "megane-ii")
        with self.assertRaisesRegex(ValueError, "schema_version"):
            audit_source_metadata({"schema_version": 2, "scopes": [s]}, snapshots())
        with self.assertRaisesRegex(ValueError, "duplicate allowlisted"):
            audit_source_metadata(allowlist(s, s), snapshots())
        with self.assertRaisesRegex(ValueError, "supported Renault project"):
            audit_source_metadata(
                allowlist(scope("untrusted", "folder-2", "other-project")), snapshots()
            )
        with self.assertRaisesRegex(ValueError, "repeated file ID"):
            audit_source_metadata(allowlist(s), snapshots(listing("private-root",
                file("duplicated-private-id", "A_NT8340A.zip", 10),
                file("duplicated-private-id", "B_NT8340A.zip", 11),
            )))

    def test_negative_sizes_and_bad_listing_completeness_rejected(self):
        s = scope("megane-schemes", "private-root", "megane-ii")
        with self.assertRaisesRegex(ValueError, "nonnegative integer"):
            audit_source_metadata(allowlist(s), snapshots(listing("private-root",
                file("file1", "Megane II_NT8340A.zip", -1),
            )))
        with self.assertRaisesRegex(ValueError, "true or false"):
            audit_source_metadata(
                allowlist(s),
                snapshots({"folder_id": "private-root", "complete": "yes", "files": []}),
            )

    def test_cli_stdout_only_and_no_source_file_mutations(self):
        with tempfile.TemporaryDirectory() as temp:
            a, b = Path(temp) / "private-allowlist.json", Path(temp) / "private-snapshot.json"
            a.write_text(json.dumps(allowlist(scope("kangoo-archives", "private-kk", "kangoo-ii"))))
            b.write_text(json.dumps(snapshots(listing("private-kk",
                file("secret-id", "KangooII X61_NT8486.zip", 880),
            ))))
            original = {p.name: p.read_bytes() for p in (a, b)}
            output = io.StringIO()
            with patch("sys.argv", ["audit_drive_source_metadata.py",
                                    "--allowlist", str(a), "--snapshots", str(b)]):
                with contextlib.redirect_stdout(output):
                    self.assertEqual(0, main())
            report = json.loads(output.getvalue())
            self.assertEqual(1, report["source_archive_count"])
            self.assertEqual(original, {p.name: p.read_bytes() for p in (a, b)})
            self.assertNotIn("private-kk", output.getvalue())
            self.assertNotIn("secret-id", output.getvalue())


if __name__ == "__main__":
    unittest.main()
