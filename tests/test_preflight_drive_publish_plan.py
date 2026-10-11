"""Offline Drive catalog prepublication safety checks; no remote calls or writes."""
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

from core.rdpkg import default_package_filename
from tools.build_drive_catalog import inspect_package, load_publish_plan
from tools.preflight_drive_publish_plan import verify_publish_candidate, main


class PreflightDrivePublishPlanTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.packages = self.root / "packages"
        self.packages.mkdir()

    def tearDown(self):
        self.temporary.cleanup()

    def make_package(self, code="NT8340A", date="2006-04-18", package_id=None):
        volume = {
            "id": f"megane-ii-{code.lower()}-{date}",
            "title": f"{code} · {date}",
            "document_code": code,
            "date": date,
            "vehicle_codes": ["E84", "L84", "K84"],
            "document_type": "Visu",
            "document_version": "3.0",
            "region": "Europe",
            "source_folder": f"Megane II {code} {date}",
            "entrypoint": f"Megane II {code} {date}/INDEX.HTM",
        }
        dataset = {
            "id": "megane-ii-dataset", "project_id": "megane-ii",
            "title": "Megane II", "model": "Megane II",
            "manufacturer": "Renault", "volumes": [volume],
        }
        package = {
            "schema_version": 1,
            "format": "renault-volume-package-v1",
            "package_id": package_id or volume["id"],
            "project_id": "megane-ii",
            "dataset_id": dataset["id"],
            "dataset_title": "Megane II",
            "volume": volume,
            "dataset_manifest": "renault-dataset.json",
            "payload_file_count": 1,
            "payload_bytes": 5,
        }
        path = self.packages / default_package_filename(dataset, volume)
        with ZipFile(path, "w") as archive:
            archive.writestr("rdpkg.json", json.dumps(package))
            archive.writestr("renault-dataset.json", json.dumps(dataset))
            archive.writestr(volume["entrypoint"], "<html></html>")
        return path

    def plan(self, *records):
        return {
            "schema_version": 1, "catalog_id": "local-test", "catalog_version": 5,
            "generated_at": "2026-10-11T12:00:00+03:00",
            "packages": [{"file_name": p.name, "drive_file_id": ident} for p, ident in records],
        }

    def file(self, ident, name, size, sha=None):
        record = {"id": ident, "title": name, "size": str(size),
                  "mime_type": "application/x-zip"}
        if sha is not None:
            record["sha256Checksum"] = sha
        return record

    def test_matches_by_exact_drive_id_with_similar_filename_collision(self):
        p = self.make_package()
        meta = inspect_package(p)
        plan = self.plan((p, "private-catalog-chosen"))
        snapshot = {"complete": True, "files": [
            self.file("private-catalog-chosen", p.name, meta["size_bytes"]),
            self.file("private-other", p.name, meta["size_bytes"] + 1200),
        ]}
        result = verify_publish_candidate(self.packages, plan, snapshot)
        row = result["packages"][0]
        self.assertEqual("REVIEW_REQUIRED", row["result"])
        self.assertIn("same_filename_multiple_drive_objects_manual_review", row["warnings"])
        self.assertIn("remote_sha256_not_available_content_unverified", row["warnings"])
        self.assertEqual([], row["problems"])
        self.assertFalse(result["can_autopublish"])
        rendered = json.dumps(result)
        self.assertNotIn("private-catalog-chosen", rendered)
        self.assertNotIn("private-other", rendered)
        self.assertNotIn(str(self.root), rendered)

    def test_selected_id_wrong_never_fallback_to_same_basename(self):
        p = self.make_package()
        result = verify_publish_candidate(self.packages, self.plan((p, "private-selected")), {
            "complete": False, "files": [self.file("private-different", p.name, p.stat().st_size)]
        })
        self.assertEqual("BLOCKED", result["packages"][0]["result"])
        self.assertIn("selected_drive_id_not_in_supplied_snapshot",
                      result["packages"][0]["problems"])
        self.assertFalse(result["drive_snapshot_complete"])
        self.assertIn("does not prove Drive file is missing", result["notes"][1])

    def test_remote_size_or_name_mismatch_blocks(self):
        p = self.make_package()
        for name, size, expected in [
            (p.name, p.stat().st_size + 1, "selected_drive_byte_size_mismatch"),
            ("Wrong-NT.rdpkg", p.stat().st_size, "selected_drive_filename_mismatch"),
        ]:
            with self.subTest(expected=expected):
                result = verify_publish_candidate(self.packages, self.plan((p, "id-1")),
                    {"files": [self.file("id-1", name, size)]})
                self.assertEqual("BLOCKED", result["packages"][0]["result"])
                self.assertIn(expected, result["packages"][0]["problems"])

    def test_hash_match_is_metadata_and_hash_not_publishing_permission(self):
        p = self.make_package()
        sha = hashlib.sha256(p.read_bytes()).hexdigest()
        snapshot = {"files": [self.file("id-one", p.name, p.stat().st_size, sha)]}
        result = verify_publish_candidate(self.packages, self.plan((p, "id-one")), snapshot)
        self.assertEqual("METADATA_AND_HASH_MATCH", result["packages"][0]["result"])
        self.assertFalse(result["can_autopublish"])
        snapshot["files"][0]["sha256Checksum"] = "0" * 64
        blocked = verify_publish_candidate(self.packages, self.plan((p, "id-one")), snapshot)
        self.assertIn("selected_drive_sha256_mismatch", blocked["packages"][0]["problems"])

    def test_no_remote_sha_falls_back_to_metadata_only_not_verification(self):
        p = self.make_package()
        result = verify_publish_candidate(self.packages, self.plan((p, "id-one")),
            {"files": [self.file("id-one", p.name, p.stat().st_size)]})
        self.assertEqual("METADATA_MATCH_CONTENT_UNVERIFIED",
                         result["packages"][0]["result"])

    def test_adjacent_distinct_nt_documents_not_collapsed(self):
        a = self.make_package("NT8340A", "2006-04-18")
        b = self.make_package("NT8342A", "2006-10-09")
        result = verify_publish_candidate(self.packages,
            self.plan((a, "id-a"), (b, "id-b")),
            {"files": [self.file("id-a", a.name, a.stat().st_size),
                       self.file("id-b", b.name, b.stat().st_size)]})
        self.assertEqual(2, result["packages_selected"])
        self.assertEqual(["NT8340A", "NT8342A"],
                         sorted(x["document_code"] for x in result["packages"]))
        self.assertEqual(2, result["result_counts"]["METADATA_MATCH_CONTENT_UNVERIFIED"])

    def test_same_project_and_package_id_is_blocked_even_with_distinct_nt(self):
        a = self.make_package("NT8340A", "2006-04-18", "duplicated-package-id")
        b = self.make_package("NT8342A", "2006-10-09", "duplicated-package-id")
        result = verify_publish_candidate(self.packages,
            self.plan((a, "id-a"), (b, "id-b")),
            {"files": [self.file("id-a", a.name, a.stat().st_size),
                       self.file("id-b", b.name, b.stat().st_size)]})
        self.assertEqual(1, result["result_counts"]["BLOCKED"])
        self.assertIn("repeated_package_identity_in_publish_plan",
                      [k for row in result["packages"] for k in row["problems"]])

    def test_missing_and_bad_local_packages_are_not_processed(self):
        p = self.make_package()
        p.write_bytes(b"invalid-not-zip")
        result = verify_publish_candidate(self.packages, self.plan((p, "id-a")),
            {"files": [self.file("id-a", p.name, p.stat().st_size)]})
        self.assertEqual(["local_package_invalid"], result["packages"][0]["problems"])
        p.unlink()
        result = verify_publish_candidate(self.packages, self.plan((p, "id-a")), {"files": []})
        self.assertEqual(["local_package_missing_or_unsafe"], result["packages"][0]["problems"])

    def test_cli_stdout_no_writes_and_bad_id_exit_status(self):
        p = self.make_package()
        plan_path = self.root / "publish-plan.json"
        snapshot_path = self.root / "listing.json"
        plan_path.write_text(json.dumps(self.plan((p, "secret-missing-id"))))
        snapshot_path.write_text(json.dumps({"complete": False, "files": []}))
        before = {x.name: x.read_bytes() for x in [p, plan_path, snapshot_path]}
        stream = io.StringIO()
        with patch("sys.argv", [
            "preflight_drive_publish_plan.py",
            "--packages-dir", str(self.packages),
            "--publish-plan", str(plan_path),
            "--drive-snapshot", str(snapshot_path),
        ]):
            with contextlib.redirect_stdout(stream):
                self.assertEqual(2, main())
        report = json.loads(stream.getvalue())
        self.assertEqual(1, report["result_counts"]["BLOCKED"])
        self.assertNotIn("secret-missing-id", stream.getvalue())
        self.assertEqual(before, {x.name: x.read_bytes() for x in [p, plan_path, snapshot_path]})


if __name__ == "__main__":
    unittest.main()
