from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class DurableRdpkgExportContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_export_uses_foreground_service_and_run_store(self):
        activity = self.read("ProjectActivity.kt")
        service = self.read("RdpkgExportService.kt")
        store = self.read("RdpkgExportRunStore.kt")
        self.assertIn("RdpkgExportService.start(", activity)
        self.assertIn("refreshRdpkgExportRunState()", activity)
        self.assertNotIn('Thread {\n            val result =\n                RdpkgExporter.export(', activity[activity.index("private fun handleRdpkgExportResult"):activity.index("private fun refreshRdpkgExportRunState")])
        self.assertIn("class RdpkgExportService : Service()", service)
        self.assertIn("startForegroundService(intent)", service)
        self.assertIn("START_REDELIVER_INTENT", service)
        self.assertIn("class RdpkgExportRunStore", store)

    def test_export_terminal_state_is_dismissible_and_persistent(self):
        activity = self.read("ProjectActivity.kt")
        store = self.read("RdpkgExportRunStore.kt")
        self.assertIn('operationStatus.showTerminal("Експорт тому завершено"', activity)
        self.assertIn('operationStatus.showTerminal("Експорт тому · помилка"', activity)
        self.assertIn("rdpkgExportRunStore.dismissTerminal(state.finishedAtMs)", activity)
        self.assertIn("dismissedFinishedAtMs", store)
        self.assertIn("isTerminalDismissed", store)

    def test_manifest_registers_export_service(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name=".RdpkgExportService"', manifest)
        self.assertIn('android:foregroundServiceType="dataSync"', manifest)


if __name__ == "__main__":
    unittest.main()
