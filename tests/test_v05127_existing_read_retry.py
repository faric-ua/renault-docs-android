"""v0.5.127 — files already in the Drive folder are re-read with pauses.

Phone QA 2026-10-13 22:06 (v0.5.125): repeat export into «Renault Docs
Projects» — «Готово: 11 із 17 (уже були в папці й збіглися: 4). Не замінено
(файл уже є, але інший або не прочитався): 6». The report: all 6 are
EXISTING_UNVERIFIED — the phone could not READ them (not a different content);
these same files had passed the sha256 check before rename in the first run.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class ExistingFilesAreReReadWithPauses(unittest.TestCase):
    def test_existing_branch_uses_retries_and_records_reason(self):
        service = (SRC / "ProjectFolderExportService.kt").read_text(encoding="utf-8")
        block = service.split("if (key in existingNames) {", 1)[1].split("return@forEachIndexed", 1)[0]
        self.assertIn("tryVerify(file.uri, localSha, localSize)", block)
        self.assertIn('.put("reason",', block)
        self.assertIn("unreadable++", block)
        self.assertNotIn("openOutputStream", block)
        self.assertNotIn("renameTo", block)

    def test_try_verify_never_throws_for_read_errors_but_passes_cancel(self):
        service = (SRC / "ProjectFolderExportService.kt").read_text(encoding="utf-8")
        body = service.split("private fun tryVerify(", 1)[1].split("private fun verifyCopy(", 1)[0]
        self.assertIn("runCatching { readBack(uri) }", body)
        self.assertIn("OperationCancelledException || dataSyncTimeout.isExpired", body)
        self.assertIn("return VerifyAttempt(last, lastError)", body)
        verify = service.split("private fun verifyCopy(", 1)[1].split("private fun sleepWithCheckpoints(", 1)[0]
        self.assertIn("tryVerify(uri, expectedSha, expectedSize, onWait)", verify)

    def test_summary_separates_different_and_unreadable(self):
        service = (SRC / "ProjectFolderExportService.kt").read_text(encoding="utf-8")
        self.assertIn("ExportCopyCheck.summary(completed, plan.size, verifiedExisting, skipped - unreadable, unreadable)", service)
        check = (SRC / "ExportCopyCheck.kt").read_text(encoding="utf-8")
        self.assertIn("не прочитались — не замінено", check)
        self.assertIn("але інші — не замінено", check)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        version = tuple(int(x) for x in gradle.split('versionName = "')[1].split('"')[0].split("."))
        self.assertGreaterEqual(version, (0, 5, 125))  # 0.6.0: Renodocs line moved on
        self.assertEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)) >= 143, True)


if __name__ == "__main__":
    unittest.main()
