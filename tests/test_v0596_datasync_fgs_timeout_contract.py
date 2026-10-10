"""Android dataSync FGS quota handling source contract — v0.5.96."""
from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"android/app/src/main/java/com/saney/renaultdocs"
SERVICES=(
    "ConversionService.kt", "NativeRdpkgPreparationService.kt",
    "CatalogImportService.kt", "RdpkgImportService.kt",
    "RdpkgExportService.kt", "RdpkgShareService.kt",
    "RdprojectShareService.kt",
)

class DataSyncTimeoutContractTests(unittest.TestCase):
    def code(self, name):
        return (BASE / name).read_text(encoding="utf-8")

    def test_all_seven_stop_immediately_when_system_calls_timeout(self):
        for file in SERVICES:
            with self.subTest(file=file):
                code=self.code(file)
                self.assertIn("override fun onTimeout(startId: Int, fgsType: Int)", code)
                block=code.split("override fun onTimeout(startId: Int, fgsType: Int)",1)[1].split("override fun onBind(",1)[0]
                self.assertIn("dataSyncTimeout.expire()", block)
                self.assertIn("runStore.fail(DataSyncTimeoutUi.MESSAGE)", block)
                self.assertIn("if (runStore.load().isRunning)", block)
                self.assertIn("stopForeground(STOP_FOREGROUND_REMOVE)", block)
                self.assertIn("stopSelf()", block)
                self.assertIn("finally", block)
                self.assertNotIn("Thread {", block)
                self.assertNotIn("startForeground(", block)

    def test_timeout_signal_stops_worker_progress_and_success(self):
        for file in SERVICES:
            with self.subTest(file=file):
                code=self.code(file)
                self.assertIn("DataSyncTimeoutGate()", code)
                self.assertIn("dataSyncTimeout.checkActive()", code)
                self.assertIn("if (dataSyncTimeout.isExpired) return", code)
                ontimeout=code.index("override fun onTimeout")
                success=code.count("dataSyncTimeout.checkActive()")
                self.assertGreaterEqual(success, 2, file)
        self.assertIn("isCancelled = { dataSyncTimeout.isExpired }",
                      self.code("RdpkgImportService.kt"))
        self.assertIn("isCancelled = { dataSyncTimeout.isExpired }",
                      self.code("CatalogImportService.kt"))
        self.assertIn("dataSyncTimeout.isExpired || runStore.isCancelRequested()",
                      self.code("NativeRdpkgPreparationService.kt"))

    def test_native_batch_keeps_existing_committed_results(self):
        code=self.code("NativeRdpkgPreparationService.kt")
        self.assertIn("runStore.recordBatchCompleted(", code)
        self.assertIn("runStore.clearArchiveCurrentOutput()", code)
        self.assertIn("dataSyncTimeout.checkActive()", code)
        self.assertIn("if (dataSyncTimeout.isExpired) return", code)
        self.assertNotIn("runStore.clearFinished()", code)

    def test_gate_does_not_claim_arbitrary_interrupt_or_auto_retry(self):
        gate=self.code("DataSyncTimeoutGate.kt")
        self.assertIn("AtomicBoolean", gate)
        self.assertIn("fun expire(): Boolean", gate)
        self.assertIn("fun checkActive()", gate)
        self.assertIn("DataSyncQuotaExpiredException", gate)
        self.assertNotIn("Thread.stop(", gate)
        self.assertNotIn("startForegroundService(", gate)
        self.assertNotIn("forceStop", gate)

if __name__=="__main__":
    unittest.main()
