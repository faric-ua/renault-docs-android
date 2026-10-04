from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class DurableRdpkgShareContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_share_uses_foreground_service_and_run_store(self):
        activity = self.read("ProjectActivity.kt")
        service = self.read("RdpkgShareService.kt")
        store = self.read("RdpkgShareRunStore.kt")
        share = activity[activity.index("private fun shareRdpkg"):activity.index("private fun confirmDeletePreparedVolume")]
        self.assertIn("RdpkgShareService.start(", share)
        self.assertIn("refreshRdpkgShareRunState()", activity)
        self.assertNotIn("Thread {", share)
        self.assertIn("class RdpkgShareService : Service()", service)
        self.assertIn("startForegroundService(", service)
        self.assertIn("START_REDELIVER_INTENT", service)
        self.assertIn("class RdpkgShareRunStore", store)

    def test_share_terminal_is_consumed_before_chooser_relaunch(self):
        activity = self.read("ProjectActivity.kt")
        self.assertIn("rdpkgShareRunStore.consume(state.finishedAtMs)", activity)
        self.assertIn('startActivity(Intent.createChooser(send, "Поділитися томом"))', activity)

    def test_manifest_registers_share_service(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name=".RdpkgShareService"', manifest)
        self.assertIn('android:foregroundServiceType="dataSync"', manifest)

if __name__ == "__main__":
    unittest.main()
