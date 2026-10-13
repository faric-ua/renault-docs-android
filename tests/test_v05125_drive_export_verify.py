"""v0.5.125 — export into a Google Drive folder (#162 stage 2, #261).

Phone QA 2026-10-13 20:46 (v0.5.124): «Експортувати томи окремо» into a Drive
folder — volumes 1–8 copied and verified, the 9th (NT8343) stopped with
«Контрольна сума скопійованого тому не збігається». The 8 good files stayed on
Drive (checked through the Drive connector), but no JSON report was written, so
the catalog had no sha256 for them. Also the window showed only «Папку вибрано» —
the owner could not see which folder (it was the parent «Renault Docs Projects»,
not the planned sub-folder).
"""
from pathlib import Path
import re
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
sys.path.insert(0, str(ROOT / "tests"))
import catalog_from_phone_export as tool  # noqa: E402
from test_v05123_catalog_from_phone_export import CATALOG, SHA_B, SHA_C, listing, report, volume  # noqa: E402

SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


class DriveCopyIsReReadBeforeFailing(unittest.TestCase):
    def test_copy_is_rechecked_with_pauses(self):
        service = read("ProjectFolderExportService.kt")
        self.assertIn("verifyCopy(created.uri, localSha, localSize)", service)
        self.assertIn("ExportCopyCheck.DELAYS_MS", service)
        self.assertIn("sleepWithCheckpoints(delay)", service)
        # cancel still works while waiting
        sleep = service.split("private fun sleepWithCheckpoints(", 1)[1].split("\n    }\n", 1)[0]
        self.assertIn("checkpoint()", sleep)
        # size and content both have to match before the real name is given
        self.assertIn("sha.equals(localSha, ignoreCase = true) && read.size == localSize", service)
        self.assertLess(service.index("verifyCopy(created.uri"), service.index("created.renameTo(targetName)"))

    def test_delays_start_immediately_and_stay_about_a_minute(self):
        check = read("ExportCopyCheck.kt")
        raw = check.split("DELAYS_MS: List<Long> = listOf(", 1)[1].split(")", 1)[0]
        delays = [int(x.strip().rstrip("L").replace("_", "")) for x in raw.split(",")]
        self.assertEqual(delays[0], 0)
        self.assertEqual(delays, sorted(delays))
        self.assertLessEqual(sum(delays), 90_000)
        self.assertGreaterEqual(len(delays), 4)


class ExistingFilesAreComparedNotTrusted(unittest.TestCase):
    def test_existing_file_is_verified_by_sha_and_never_overwritten(self):
        service = read("ProjectFolderExportService.kt")
        self.assertIn('"EXISTING_SHA256_VERIFIED"', service)
        self.assertIn('"EXISTING_SHA256_MISMATCH"', service)
        self.assertIn("EXISTING_UNVERIFIED", service)
        block = service.split("if (key in existingNames) {", 1)[1].split("return@forEachIndexed", 1)[0]
        self.assertIn("readBack(file.uri)", block)
        self.assertNotIn("openOutputStream", block)
        self.assertNotIn("delete()", block)
        self.assertNotIn("renameTo", block)


class ReportIsWrittenEvenWhenARunStops(unittest.TestCase):
    def test_failed_or_cancelled_run_still_writes_report(self):
        service = read("ProjectFolderExportService.kt")
        self.assertIn('.put("result", result)', service)
        catch = service.split("} catch (error: Throwable) {\n            // v0.5.125", 1)[1].split("dataSyncTimeout.checkActive()", 1)[0]
        self.assertIn("writeReport(", catch)
        self.assertIn('"cancelled" else "failed"', catch)
        self.assertIn('"CANCELLED" else "FAILED"', catch)
        # the error names the volume, cancellation passes through unchanged
        self.assertIn("if (cancelled || item == null) throw error", catch)
        self.assertIn('"Том ${currentIndex + 1}/${plan.size}', catch)
        self.assertIn('writeReport(tree, project, plan.size, completed, skipped, records, "complete")', service)


class FolderNameIsShown(unittest.TestCase):
    def test_wizard_shows_folder_name_on_both_steps(self):
        wizard = read("ProjectExportWizard.kt")
        self.assertIn("val folderLabel = resolvedFolderLabel()", wizard)
        self.assertIn('"Папка: " + (resolvedFolderLabel() ?: "вибрана")', wizard)
        helper = wizard.split("private fun resolvedFolderLabel()", 1)[1].split("private fun renderFolder()", 1)[0]
        # another app is asked off the main thread; the step re-draws afterwards
        self.assertIn("Thread {", helper)
        self.assertIn("SafFolderLabel.resolve(appContext, uri)", helper)
        self.assertIn("activity.runOnUiThread", helper)

    def test_label_asks_the_owning_app_for_the_name(self):
        label = read("SafFolderLabel.kt")
        self.assertIn("DocumentsContract.Document.COLUMN_DISPLAY_NAME", label)
        self.assertIn("buildDocumentUriUsingTree", label)
        self.assertIn("resolveContentProvider(authority, 0)", label)
        # the account is shown only when Android gives it, never guessed
        self.assertIn("runCatching { accountOf(", label)


class CatalogToolAcceptsTheNewReport(unittest.TestCase):
    def test_existing_verified_volume_is_ready(self):
        a = volume("NT8342A", "2006-10-09", SHA_B, status="EXISTING_SHA256_VERIFIED")
        adds, findings = tool.plan_additions(CATALOG, report(a), listing(a))
        self.assertEqual([f["status"] for f in findings], ["READY"])
        self.assertEqual(adds[0]["sha256"], SHA_B)

    def test_failed_mismatch_and_cancelled_records_are_blocked(self):
        for status in ("FAILED", "CANCELLED", "EXISTING_SHA256_MISMATCH", "EXISTING_UNVERIFIED"):
            a = volume("NT8342A", "2006-10-09", SHA_B, status=status)
            self.assertEqual(tool.plan_additions(CATALOG, report(a), listing(a))[1][0]["status"], "BLOCKED", status)

    def test_volume_without_date_is_allowed_with_null_date(self):
        a = dict(volume("NT8206A", "", SHA_C, package_id="megane-ii-nt8206a",
                        file_name="Megane-II_NT8206A.rdpkg"), date=None)
        adds, findings = tool.plan_additions(CATALOG, report(a), listing(a))
        self.assertEqual([f["status"] for f in findings], ["READY"], findings)
        self.assertIsNone(adds[0]["date"])
        merged = tool.merge_catalog(CATALOG, report()["project"], adds, 5, "2026-10-13T21:00:00+03:00")
        self.assertEqual(len(merged["projects"][0]["volumes"]), 2)

    def test_bad_date_or_code_missing_from_name_is_blocked(self):
        a = dict(volume("NT8342A", "2006-10-09", SHA_B), date="10.09.2006")
        self.assertEqual(tool.plan_additions(CATALOG, report(a), listing(a))[1][0]["status"], "BLOCKED")
        b = dict(volume("NT8206A", "", SHA_C, file_name="Megane-II_other.rdpkg"), date=None)
        self.assertEqual(tool.plan_additions(CATALOG, report(b), listing(b))[1][0]["status"], "BLOCKED")


class Version(unittest.TestCase):
    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn("versionName = \"0.5.1", gradle)
        self.assertGreaterEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)), 141)


if __name__ == "__main__":
    unittest.main()
