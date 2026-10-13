"""v0.5.123 — #162 stage 2: public catalog from the phone export report.

Owner 2026-10-13 19:27: «на Google диску кожен том окремо; людина тягне один
або два, а не всі 22; хто хоче всі офлайн — пакує і передає з телефона на
телефон» (.rdproject). The catalog gains the remaining Megane II volumes as
independent .rdpkg entries — only verified ones, nothing removed.
"""
from pathlib import Path
import copy
import json
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import catalog_from_phone_export as tool  # noqa: E402

SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
SHA_A = "a" * 64
SHA_B = "b" * 64
SHA_C = "c" * 64

CATALOG = {
    "schema_version": 1,
    "catalog_version": 4,
    "generated_at": "2026-10-06T22:15:00+03:00",
    "catalog_id": "renault-docs-public",
    "projects": [{
        "id": "megane-ii", "title": "Megane II", "vehicle_codes": ["E84"],
        "document_year_from": 2006, "document_year_to": 2006,
        "volumes": [{
            "id": "megane-ii-nt8340a-2006-04-18", "document_code": "NT8340A", "date": "2006-04-18",
            "vehicle_codes": ["E84"], "document_type": "Visu", "document_version": "3.0", "region": None,
            "file_name": "Megane-II_E84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
            "drive_file_id": "OLD1", "size_bytes": 100, "sha256": SHA_A,
        }],
    }],
}


def volume(code, date, sha, size=200, status="EXPORTED_SHA256_VERIFIED", **extra):
    item = {
        "volume_id": "v-" + code, "package_id": "megane-ii-" + code.lower() + "-" + date,
        "file_name": f"Megane-II_E84_Europe_{code}_Visu-v3.0_{date}.rdpkg",
        "document_code": code, "date": date, "status": status, "sha256": sha, "size_bytes": size,
        "vehicle_codes": ["E84", "L84"], "document_type": "Visu", "document_version": "3.0", "region": "Europe",
    }
    item.update(extra)
    return item


def report(*volumes):
    return {"format": "renault-docs-folder-export-v1", "schema_version": 2,
            "project": {"id": "megane-ii", "title": "Megane II", "model": "Megane II"},
            "volumes": list(volumes)}


def listing(*vols, extra=()):
    files = [{"id": "D-" + v["document_code"], "title": v["file_name"], "fileSize": str(v["size_bytes"])} for v in vols]
    return {"files": files + list(extra)}


class CatalogFromPhoneExportTests(unittest.TestCase):
    def test_distinct_nt_codes_are_added_and_nothing_removed(self):
        a = volume("NT8342A", "2006-10-09", SHA_B)
        b = volume("NT8341A", "2006-10-09", SHA_C, file_name="Megane-II_B84_Europe_NT8341A_Visu-v3.0_2006-10-09.rdpkg")
        before = copy.deepcopy(CATALOG)
        additions, findings = tool.plan_additions(CATALOG, report(a, b), listing(a, b))
        self.assertEqual(CATALOG, before)
        self.assertEqual({x["document_code"] for x in additions}, {"NT8342A", "NT8341A"})
        self.assertEqual({f["status"] for f in findings}, {"READY"})
        merged = tool.merge_catalog(CATALOG, report()["project"], additions, 5, "2026-10-13T20:00:00+03:00")
        ids = [v["id"] for v in merged["projects"][0]["volumes"]]
        self.assertIn("megane-ii-nt8340a-2006-04-18", ids)
        self.assertEqual(len(ids), 3)
        self.assertEqual(merged["catalog_version"], 5)
        self.assertEqual(merged["projects"][0]["document_year_to"], 2006)
        self.assertIn("L84", merged["projects"][0]["vehicle_codes"])
        new = next(v for v in merged["projects"][0]["volumes"] if v["document_code"] == "NT8342A")
        self.assertEqual(new["drive_file_id"], "D-NT8342A")
        self.assertEqual(new["sha256"], SHA_B)

    def test_same_name_twice_on_drive_needs_review(self):
        a = volume("NT8342A", "2006-10-09", SHA_B)
        dup = {"id": "OTHER", "title": a["file_name"], "fileSize": "200"}
        _, findings = tool.plan_additions(CATALOG, report(a), listing(a, extra=[dup]))
        self.assertEqual(findings[0]["status"], "REVIEW_REQUIRED")

    def test_size_mismatch_missing_file_and_unverified_are_blocked(self):
        a = volume("NT8342A", "2006-10-09", SHA_B)
        wrong = {"files": [{"id": "X", "title": a["file_name"], "fileSize": "999"}]}
        self.assertEqual(tool.plan_additions(CATALOG, report(a), wrong)[1][0]["status"], "BLOCKED")
        self.assertEqual(tool.plan_additions(CATALOG, report(a), {"files": []})[1][0]["status"], "BLOCKED")
        u = volume("NT8344", "2007-05-02", SHA_B, status="EXISTING_UNVERIFIED")
        self.assertEqual(tool.plan_additions(CATALOG, report(u), listing(u))[1][0]["status"], "BLOCKED")

    def test_already_published_same_sha_is_skipped_other_sha_blocked(self):
        same = volume("NT8340A", "2006-04-18", SHA_A, package_id="megane-ii-nt8340a-2006-04-18",
                      file_name="Megane-II_E84_NT8340A_Visu-v3.0_2006-04-18.rdpkg")
        adds, findings = tool.plan_additions(CATALOG, report(same), listing(same))
        self.assertEqual(adds, [])
        self.assertEqual(findings[0]["status"], "ALREADY_PUBLISHED")
        other = dict(same, sha256=SHA_C)
        self.assertEqual(tool.plan_additions(CATALOG, report(other), listing(other))[1][0]["status"], "BLOCKED")

    def test_old_report_schema_is_refused(self):
        old = report(volume("NT8342A", "2006-10-09", SHA_B))
        old["schema_version"] = 1
        with self.assertRaises(ValueError):
            tool.plan_additions(CATALOG, old, {"files": []})

    def test_cli_writes_new_file_only_when_everything_passes(self):
        a = volume("NT8342A", "2006-10-09", SHA_B)
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp)
            (t / "c.json").write_text(json.dumps(CATALOG), encoding="utf-8")
            (t / "r.json").write_text(json.dumps(report(a)), encoding="utf-8")
            (t / "l.json").write_text(json.dumps(listing(a)), encoding="utf-8")
            cmd = [sys.executable, str(ROOT / "tools/catalog_from_phone_export.py"), "--catalog", str(t / "c.json"),
                   "--report", str(t / "r.json"), "--drive-listing", str(t / "l.json"),
                   "--output", str(t / "new.json"), "--generated-at", "2026-10-13T20:00:00+03:00"]
            self.assertEqual(subprocess.run(cmd, capture_output=True).returncode, 0)
            self.assertEqual(json.loads((t / "new.json").read_text())["catalog_version"], 5)
            self.assertEqual(json.loads((t / "c.json").read_text()), CATALOG)
            (t / "l.json").write_text(json.dumps({"files": []}), encoding="utf-8")
            (t / "new.json").unlink()
            self.assertEqual(subprocess.run(cmd, capture_output=True).returncode, 2)
            self.assertFalse((t / "new.json").exists())


class AndroidStage2Contract(unittest.TestCase):
    def test_export_report_carries_catalog_identity(self):
        service = (SRC / "ProjectFolderExportService.kt").read_text(encoding="utf-8")
        self.assertIn('.put("schema_version", 2)', service)
        helper = service.split("private fun putCatalogIdentity(", 1)[1].split("private fun sha256OfFile(", 1)[0]
        for key in ('"package_id"', '"vehicle_codes"', '"document_type"', '"document_version"', '"region"', '"rdpkg.json"'):
            self.assertIn(key, helper)
        self.assertIn('.put("ready_for_public_catalog", false)', service)

    def test_download_is_checked_against_catalog_sha256(self):
        client = (SRC / "DriveCatalogClient.kt").read_text(encoding="utf-8")
        self.assertIn("expectedSha256: String? = null", client)
        self.assertIn('MessageDigest.getInstance(\n                "SHA-256"', client)
        self.assertIn("Контрольна сума .rdpkg не збігається з каталогом", client)
        service = (SRC / "CatalogImportService.kt").read_text(encoding="utf-8")
        self.assertIn("expectedSha256 =\n                        item.sha256", service)
        store = (SRC / "CatalogImportRunStore.kt").read_text(encoding="utf-8")
        self.assertIn("val sha256: String? = null", store)
        activity = (SRC / "DriveCatalogActivity.kt").read_text(encoding="utf-8")
        self.assertIn("sha256 =\n                                    volume.sha256", activity)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 139)


if __name__ == "__main__":
    unittest.main()
