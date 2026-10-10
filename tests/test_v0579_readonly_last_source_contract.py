from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class LastSourceReadOnlyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.project = (APP / "ProjectActivity.kt").read_text(encoding="utf-8")
        cls.store = (APP / "NativeRdpkgRunStore.kt").read_text(encoding="utf-8")
        cls.dialog = cls.project.split(
            "private fun showLastNativeSourceDiagnostics()", 1
        )[1].split("private fun handleNativeRdpkgSourceResult(", 1)[0]

    def test_explicit_project_add_action(self):
        self.assertIn("Джерело останньої .rdpkg · діагностика", self.project)
        self.assertIn("setOnClickListener { showLastNativeSourceDiagnostics() }", self.project)

    def test_report_contains_source_evidence(self):
        for field in ("nativeRunStore.load()", "last.sourceUri", "last.sourceName",
                      "last.sourceKind", "last.projectId", "last.destinationUri",
                      "NativeRdpkgBatchReportFormatter.reportLines(last)", "last.startedAtMs", "last.finishedAtMs",
                      "DocumentsContract.getDocumentId", "DocumentFile.fromSingleUri",
                      "Копіювати звіт", "ClipData.newPlainText"):
            self.assertIn(field, self.dialog)

    def test_report_has_no_side_effects(self):
        for forbidden in ("nativeRunStore.clearFinished(", "nativeRunStore.begin(",
                          "NativeRdpkgPreparationService.start(", "resumePersisted(",
                          "contentResolver.openInputStream(", ".delete(", "stageSource(",
                          "openPackagePicker(", "requestCancel("):
            self.assertNotIn(forbidden, self.dialog)

    def test_last_source_survives_completion_until_next_start(self):
        complete = self.store.split("fun complete(", 1)[1].split("fun markAlreadyPresent(", 1)[0]
        self.assertNotIn("KEY_SOURCE_URI", complete)
        self.assertNotIn("clearFinished()", complete)
        self.assertIn("fun clearFinished()", self.store)

if __name__ == "__main__":
    unittest.main()
