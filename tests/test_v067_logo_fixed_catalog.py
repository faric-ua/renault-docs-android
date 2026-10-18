"""Catalog v6 preparation — Laguna II volumes with the new logo (owner 2026-10-18 19:21).

Phone side: tools/logo_fixed_report.py (menu 27) writes logo-fixed-report.json.
Cloud side: tools/catalog_replace_volumes.py builds a NEW catalog from v5 + report +
Drive listing, replacing only drive_file_id / size_bytes / sha256.
"""
import copy
import hashlib
import json
import tempfile
import unittest
import unittest.mock
from pathlib import Path

from tests.test_fix_volume_logo import BAD, GOOD, make_rdpkg
from tools import catalog_replace_volumes as cr
from tools import fix_volume_logo as fx
from tools import logo_fixed_report as lr

ROOT = Path(__file__).resolve().parents[1]
V5 = json.loads((ROOT / "config/renault-docs-catalog.v5.json").read_text(encoding="utf-8"))


def laguna(catalog, volume_id):
    for project in catalog["projects"]:
        for volume in project["volumes"]:
            if volume["id"] == volume_id:
                return volume
    raise KeyError(volume_id)


class PhoneReport(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.folder = self.root / "rdpkg" / fx.OUTPUT_FOLDER
        self.folder.mkdir(parents=True)
        self.catalog = self.root / "catalog.json"
        cat = {"projects": [{"id": "laguna-2", "volumes": [
            {"id": "laguna-2-nt8183a", "file_name": "Laguna-II_NT8183A.rdpkg", "sha256": "0" * 64},
        ]}]}
        self.catalog.write_text(json.dumps(cat), encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def run_report(self):
        return lr.main([str(self.root), "--catalog", str(self.catalog)])

    def test_good_file_gives_report_and_exit_0(self):
        path = self.folder / "Laguna-II_NT8183A.rdpkg"
        make_rdpkg(path, logo=GOOD)
        before = path.read_bytes()
        self.assertEqual(self.run_report(), 0)
        report = json.loads((self.folder / lr.REPORT_NAME).read_text(encoding="utf-8"))
        self.assertEqual(report["format"], cr.REPORT_FORMAT)
        entry = report["files"][0]
        self.assertEqual(entry["package_id"], "laguna-2-nt8183a")
        self.assertEqual(entry["catalog_id"], "laguna-2-nt8183a")
        self.assertEqual(entry["bad_logo_places"], 0)
        self.assertEqual(entry["size_bytes"], len(before))
        self.assertEqual(entry["sha256"], hashlib.sha256(before).hexdigest())
        self.assertEqual(path.read_bytes(), before, "the volume file must not change")

    def test_bad_logo_left_is_flagged(self):
        make_rdpkg(self.folder / "Laguna-II_NT8183A.rdpkg", logo=BAD)
        with unittest.mock.patch.object(fx, "BAD_SIZE", len(BAD)), \
                unittest.mock.patch.object(fx, "BAD_SHA256", hashlib.sha256(BAD).hexdigest()):
            self.assertEqual(self.run_report(), 2)
        report = json.loads((self.folder / lr.REPORT_NAME).read_text(encoding="utf-8"))
        self.assertGreater(report["files"][0]["bad_logo_places"], 0)

    def test_unknown_file_is_flagged(self):
        make_rdpkg(self.folder / "Something-else.rdpkg", logo=GOOD)
        self.assertEqual(self.run_report(), 2)

    def test_no_folder(self):
        empty = self.root / "empty"
        empty.mkdir()
        self.assertEqual(lr.main([str(empty), "--catalog", str(self.catalog)]), 2)


class CatalogReplace(unittest.TestCase):
    def setUp(self):
        self.old = laguna(V5, "laguna-ii-nt8236a-2002-11-18")
        self.report = {"format": cr.REPORT_FORMAT, "files": [{
            "file_name": self.old["file_name"], "package_id": self.old["id"], "bad_logo_places": 0,
            "size_bytes": 41_167_200, "sha256": "a" * 64, "catalog_id": self.old["id"]}]}
        self.listing = {"files": [{"id": "NEWID", "title": self.old["file_name"], "fileSize": "41167200"}]}

    def test_replaces_only_three_fields(self):
        plan, findings = cr.plan_replacements(V5, self.report, self.listing)
        self.assertEqual([f["status"] for f in findings], ["READY"])
        result = cr.apply_plan(V5, plan, 6, "2026-10-18T20:00:00+03:00")
        new = laguna(result, self.old["id"])
        self.assertEqual((new["drive_file_id"], new["size_bytes"], new["sha256"]), ("NEWID", 41_167_200, "a" * 64))
        for key in self.old:
            if key not in ("drive_file_id", "size_bytes", "sha256"):
                self.assertEqual(new[key], self.old[key])
        self.assertEqual(result["catalog_version"], 6)
        self.assertEqual(laguna(V5, self.old["id"]), self.old, "v5 must stay untouched")
        # every other volume identical
        others_before = [v for p in V5["projects"] for v in p["volumes"] if v["id"] != self.old["id"]]
        others_after = [v for p in result["projects"] for v in p["volumes"] if v["id"] != self.old["id"]]
        self.assertEqual(others_before, others_after)

    def blocked(self, report=None, listing=None):
        plan, findings = cr.plan_replacements(V5, report or self.report, listing or self.listing)
        self.assertEqual(plan, {})
        return findings[0]["status"]

    def test_size_mismatch_blocks(self):
        listing = copy.deepcopy(self.listing)
        listing["files"][0]["fileSize"] = "1"
        self.assertEqual(self.blocked(listing=listing), "BLOCKED")

    def test_two_drive_files_block(self):
        listing = copy.deepcopy(self.listing)
        listing["files"].append(dict(listing["files"][0], id="OTHER"))
        self.assertEqual(self.blocked(listing=listing), "BLOCKED")

    def test_bad_logo_blocks(self):
        report = copy.deepcopy(self.report)
        report["files"][0]["bad_logo_places"] = 2
        self.assertEqual(self.blocked(report=report), "BLOCKED")

    def test_similar_nt_code_is_not_merged(self):
        report = copy.deepcopy(self.report)
        report["files"][0]["package_id"] = "laguna-ii-nt8236b-2002-11-18"
        self.assertEqual(self.blocked(report=report), "BLOCKED")

    def test_same_sha_is_already_published(self):
        report = copy.deepcopy(self.report)
        report["files"][0]["sha256"] = self.old["sha256"]
        self.assertEqual(self.blocked(report=report), "ALREADY_PUBLISHED")

    def test_cli_writes_new_file_only_when_all_ready(self):
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp)
            (t / "r.json").write_text(json.dumps(self.report), encoding="utf-8")
            (t / "l.json").write_text(json.dumps(self.listing), encoding="utf-8")
            args = ["--catalog", str(ROOT / "config/renault-docs-catalog.v5.json"), "--report", str(t / "r.json"),
                    "--drive-listing", str(t / "l.json"), "--output", str(t / "v6.json"),
                    "--catalog-version", "6", "--generated-at", "2026-10-18T20:00:00+03:00"]
            self.assertEqual(cr.main(args + ["--expect", "10"]), 2)
            self.assertFalse((t / "v6.json").exists())
            self.assertEqual(cr.main(args + ["--expect", "1"]), 0)
            self.assertEqual(json.loads((t / "v6.json").read_text(encoding="utf-8"))["catalog_version"], 6)
            self.assertEqual(cr.main(args), 2, "never overwrite an existing output")


class Menu(unittest.TestCase):
    def test_item_27(self):
        menu = (ROOT / "menu.sh").read_text(encoding="utf-8")
        self.assertIn('echo "27 — Звіт для Диска: томи Laguna II з новим лого"', menu)
        self.assertIn("reno-logo-fixed-report.sh", menu)
        self.assertTrue((ROOT / "tools/termux/reno-logo-fixed-report.sh").is_file())


if __name__ == "__main__":
    unittest.main()
