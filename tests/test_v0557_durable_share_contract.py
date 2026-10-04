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

    def test_share_terminal_survives_chooser_and_rotation_without_relaunch(self):
        activity = self.read("ProjectActivity.kt")
        store = self.read("RdpkgShareRunStore.kt")
        self.assertIn("markChooserLaunched(state.finishedAtMs)", activity)
        self.assertIn("dismissTerminal(state.finishedAtMs)", activity)
        self.assertIn("isChooserLaunched", store)
        self.assertIn("isTerminalDismissed", store)
        refresh = activity[activity.index("private fun refreshRdpkgShareRunState"):activity.index("private fun sharePreparedRdpkg")]
        self.assertNotIn("consume(state.finishedAtMs)", refresh)

    def test_existing_prepared_package_is_reused_instead_of_reexported(self):
        activity = self.read("ProjectActivity.kt")
        share = activity[activity.index("private fun shareRdpkg"):activity.index("private fun refreshRdpkgShareRunState")]
        self.assertIn("PreparedShareStore.existingVolumeFile(", share)
        self.assertIn("preparedFile != null", share)
        self.assertIn("sharePreparedRdpkg(preparedFile)", share)

    def test_manifest_registers_share_service(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name=".RdpkgShareService"', manifest)
        self.assertIn('android:foregroundServiceType="dataSync"', manifest)

if __name__ == "__main__":
    unittest.main()
