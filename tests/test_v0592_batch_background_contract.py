"""Non-destructive batch/cancellation and background worker contract for v0.5.92."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
SERVICES = (
    "ConversionService.kt",
    "NativeRdpkgPreparationService.kt",
    "CatalogImportService.kt",
    "RdpkgImportService.kt",
    "RdpkgExportService.kt",
    "RdpkgShareService.kt",
    "RdprojectShareService.kt",
)

class BatchBackgroundContractTests(unittest.TestCase):
    def code(self, file):
        return (SRC / file).read_text(encoding="utf-8")

    def test_all_seven_workers_have_foreground_and_partial_wakelock(self):
        for name in SERVICES:
            with self.subTest(service=name):
                source = self.code(name)
                self.assertIn("startForeground(", source)
                self.assertIn("START_REDELIVER_INTENT", source)
                self.assertIn("releaseWakeLock()", source) if name in (
                    "ConversionService.kt",
                    "NativeRdpkgPreparationService.kt",
                ) else self.assertIn("workWakeLock.release()", source)
                if name not in ("ConversionService.kt", "NativeRdpkgPreparationService.kt"):
                    self.assertIn("BackgroundWorkWakeLock(", source)
                self.assertIn("stopSelf()", source)

    def test_manifest_declares_non_task_bound_datasync_workers(self):
        import xml.etree.ElementTree as ET
        manifest = ET.parse(ROOT / "android/app/src/main/AndroidManifest.xml")
        ns = "{http://schemas.android.com/apk/res/android}"
        services = manifest.findall(".//service")
        entries = {
            svc.attrib.get(ns + "name"): svc.attrib for svc in services
        }
        self.assertEqual(7, len([x for x in SERVICES if "." + x.removesuffix(".kt") in entries]))
        for name in SERVICES:
            with self.subTest(service=name):
                row = entries["." + name.removesuffix(".kt")]
                self.assertEqual("dataSync", row[ns + "foregroundServiceType"])
                self.assertEqual("false", row[ns + "stopWithTask"])
                self.assertEqual("false", row[ns + "exported"])
        permissions = [p.attrib.get(ns + "name") for p in manifest.findall("uses-permission")]
        self.assertIn("android.permission.WAKE_LOCK", permissions)
        self.assertIn("android.permission.FOREGROUND_SERVICE_DATA_SYNC", permissions)

    def test_cancel_applies_to_native_import_phase_too(self):
        service = self.code("NativeRdpkgPreparationService.kt")
        importer = self.code("RdpkgImporter.kt")
        self.assertIn("state.isRunning", service)
        self.assertIn("runStore.requestCancel()", service)
        self.assertIn("isCancelled = {\n                            dataSyncTimeout.isExpired || runStore.isCancelRequested()", service)
        self.assertIn("isCancelled: () -> Boolean = { false }", importer)
        self.assertIn("if (isCancelled()) throw ConversionCancelledException()", importer)
        self.assertNotIn(
            "state.phase ==\n                    NativeRdpkgRunPhase.PREPARING\n                ) {\n                    runStore.requestCancel()",
            service
        )

    def test_importer_cancel_checkpoints_precede_atomic_activation(self):
        importer = self.code("RdpkgImporter.kt")
        extract_start = importer.index("private fun extract(")
        extract = importer[extract_start:]
        self.assertIn("if (isCancelled()) throw ConversionCancelledException()", extract)
        self.assertLess(importer.index("if (isCancelled()) throw ConversionCancelledException()\n                backupDirectory"),
                        importer.index("finalDirectory.renameTo("))
        self.assertIn("finally {\n                staging.deleteRecursively()", importer)
        self.assertIn("backupDirectory.renameTo(", importer)

    def test_batch_finished_results_are_published_per_committed_volume(self):
        source = self.code("NativeRdpkgPreparationService.kt")
        batch = source[source.index("private fun runArchiveSelection()"):source.index("private fun processPreparedSource(")]
        self.assertIn("processed += completed", batch)
        self.assertLess(batch.index("processed += completed"), batch.index("publishCompletedBatchVolume("))
        self.assertLess(batch.index("publishCompletedBatchVolume("), batch.index("runStore.isCancelRequested()", batch.index("processed += completed")))
        self.assertIn("processed.imported.packageId", source)
        self.assertIn('"native-batch"', source)
        summary = source[source.index("private fun completeArchiveBatch("):source.index("private fun buildArchiveCandidates(")]
        self.assertNotIn("processed.forEach { volume ->", summary)
        self.assertIn("CompletedNotificationHistory.publish(", summary)

    def test_selected_volume_is_explicit_and_other_roots_are_isolated(self):
        service = self.code("NativeRdpkgPreparationService.kt")
        store = self.code("NativeRdpkgRunStore.kt")
        self.assertIn("beginArchiveSelectionProcessing()", service)
        self.assertIn("it.selected &&", service)
        self.assertIn("!it.installed", service)
        self.assertIn("ArchiveRawVolumeIsolation.excludedDescendantRoots(", service)
        self.assertIn("ArchiveIntake.resolveRawRoot(", service)
        self.assertIn("runStore.clearArchiveSelectionData()", service)
        self.assertIn("cleanupPersistedArchiveWorkspace(", service)
        self.assertIn("startArchiveResumeService(", service)
        self.assertIn("KEY_ARCHIVE_CANDIDATES", store)

    def test_zip_7z_rar_without_claiming_phone_validation(self):
        intake = self.code("ArchiveIntake.kt")
        for symbol in ("SEVEN_Z", "ZIP", "RAR", "safeTarget(", "MAX_ENTRIES"):
            self.assertIn(symbol, intake)

if __name__ == "__main__":
    unittest.main()
