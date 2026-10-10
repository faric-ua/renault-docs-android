"""Regression boundaries for v0.5.95 persistent batch diagnostics."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
CODE=ROOT/"android/app/src/main/java/com/saney/renaultdocs"

def src(name):
    return (CODE/name).read_text(encoding="utf-8")

class BatchNativeReportContract(unittest.TestCase):
    def test_results_preserved_after_each_committed_tome(self):
        service=src("NativeRdpkgPreparationService.kt")
        segment=service.split("private fun runArchiveSelection()",1)[1].split("private fun processPreparedSource(",1)[0]
        self.assertIn("runStore.recordBatchCompleted(", segment)
        self.assertIn("runStore.recordBatchSkipped(", segment)
        self.assertLess(segment.index("processed += completed"),
                        segment.index("runStore.recordBatchCompleted("))
        self.assertLess(segment.index("runStore.recordBatchCompleted("),
                        segment.index("publishCompletedBatchVolume("))
        self.assertIn("completed.destination.toString()", segment)
        self.assertIn("completed.prepared.sha256", segment)

    def test_persistent_versioned_bounded_report_survives_and_is_cleared_on_new_run(self):
        store=src("NativeRdpkgRunStore.kt")
        self.assertIn('private const val KEY_BATCH_REPORT',store)
        self.assertIn('private fun decodeBatchReport(',store)
        self.assertIn('private fun encodeBatchReport(',store)
        self.assertIn('put("schema_version", NativeRdpkgBatchReport.SCHEMA_VERSION)',store)
        self.assertIn('val batchReport: NativeRdpkgBatchReport? = null',store)
        self.assertIn('prefs.getString(KEY_BATCH_REPORT, null)',store)
        begin=store.split("fun begin(",1)[1].split("fun resumeAfterProcessRestart()",1)[0]
        self.assertIn(".remove(KEY_BATCH_REPORT)",begin)
        complete=store.split("fun complete(",1)[1].split("fun markAlreadyPresent(",1)[0]
        self.assertNotIn(".remove(KEY_BATCH_REPORT)",complete)
        fail=store.split("private fun finish(",1)[1].split("fun dismissTerminal(",1)[0]
        self.assertNotIn(".remove(KEY_BATCH_REPORT)",fail)

    def test_original_scalar_id_not_used_as_batch_identity(self):
        service=src("NativeRdpkgPreparationService.kt")
        batch=service.split("private fun completeArchiveBatch(",1)[1].split("private fun publishCompletedBatchVolume(",1)[0]
        self.assertIn('packageId = if (processed.size == 1) last.imported.packageId else ""',batch)
        self.assertIn('volumeId = if (processed.size == 1) last.imported.volume.id else ""',batch)
        self.assertIn('"Томів створено: "',batch)
        self.assertNotIn('" томів"',batch)

    def test_ui_is_read_only_and_exposes_all_per_volume_results(self):
        activity=src("ProjectActivity.kt")
        self.assertIn("NativeRdpkgBatchReportFormatter.reportLines(last)",activity)
        formatter=src("NativeRdpkgBatchReport.kt")
        for field in ("Томів створено:", "Package ID:", "Volume ID:", "SHA-256:", "URI пакета:",
                      "Дані про всі пакети в цьому старому звіті недоступні"):
            self.assertIn(field,formatter)
        dialog=activity.split("private fun showLastNativeSourceDiagnostics()",1)[1].split(
            "private fun handleNativeRdpkgSourceResult(",1)[0]
        for forbidden in ("startForegroundService(", "nativeRunStore.begin(", "contentResolver.openInputStream(", ".delete("):
            self.assertNotIn(forbidden,dialog)

if __name__=="__main__": unittest.main()
