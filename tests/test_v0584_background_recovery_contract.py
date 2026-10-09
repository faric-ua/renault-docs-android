"""Issue #40: service restart/redelivery safety for all long-running Renault operations."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"

SERVICES = (
    "ConversionService",
    "NativeRdpkgPreparationService",
    "CatalogImportService",
    "RdpkgImportService",
    "RdpkgExportService",
    "RdpkgShareService",
    "RdprojectShareService",
)


def source(name: str) -> str:
    return (JAVA / f"{name}.kt").read_text(encoding="utf-8")


class BackgroundRecoveryContractTests(unittest.TestCase):
    def test_seven_services_have_background_lifecycle_not_screen_wakelock(self):
        manifest = MANIFEST.read_text(encoding="utf-8")
        self.assertIn('android.permission.FOREGROUND_SERVICE_DATA_SYNC', manifest)
        self.assertIn('android.permission.WAKE_LOCK', manifest)
        for service in SERVICES:
            with self.subTest(service=service):
                match = re.search(
                    rf'<service\s+android:name="\.{service}"[^>]+/>',
                    manifest, flags=re.DOTALL,
                )
                self.assertIsNotNone(match)
                self.assertIn('android:foregroundServiceType="dataSync"', match.group())
                self.assertIn('android:stopWithTask="false"', match.group())
                code = source(service)
                self.assertIn('startForeground(', code)
                self.assertIn('START_REDELIVER_INTENT', code)
                self.assertIn('stopForeground(', code)
                self.assertIn('onDestroy()', code)
                self.assertTrue(
                    'BackgroundWorkWakeLock' in code
                    or 'PowerManager.PARTIAL_WAKE_LOCK' in code
                )
        for name in ('ConversionActivity', 'ProjectActivity', 'MainActivity'):
            self.assertNotIn('FLAG_KEEP_SCREEN_ON', source(name))

    def test_conversion_recovery_does_not_erase_user_cancel_progress_or_start(self):
        store = source('ConversionRunStore')
        resume = store.split('fun resumeAfterProcessRestart()', 1)[1].split('fun update(', 1)[0]
        self.assertIn('if (!previous.isRunning)', resume)
        self.assertIn('KEY_MESSAGE', resume)
        self.assertNotIn('KEY_CANCEL_REQUESTED', resume)
        self.assertNotIn('KEY_STARTED_AT', resume)
        self.assertNotIn('KEY_FILES_DONE', resume)
        self.assertNotIn('KEY_PHASE', resume)
        self.assertIn('.commit()', resume)
        begin = store.split('fun begin(', 1)[1].split('fun resumeAfterProcessRestart()', 1)[0]
        self.assertIn('.commit()', begin)

    def test_conversion_rejects_stale_redelivery_and_wrong_plan(self):
        service = source('ConversionService')
        entry = service.split('override fun onStartCommand(', 1)[1].split('override fun onBind(', 1)[0]
        self.assertIn('flags and START_FLAG_REDELIVERY != 0', entry)
        self.assertIn('!persistedState.isRunning', entry)
        self.assertIn('stopSelf(startId)', entry)
        self.assertIn('persistedState.sourceUri != sourceUri', entry)
        self.assertIn('persistedState.destinationUri != destinationUri', entry)
        self.assertIn('persistedState.outputFolderName != outputFolderName', entry)
        self.assertIn('runStore.resumeAfterProcessRestart()', entry)
        self.assertIn('runStore.begin(', entry)
        self.assertLess(entry.index('runStore.resumeAfterProcessRestart()'), entry.index('runStore.begin('))
        self.assertEqual(entry.count('runStore.begin('), 1)

    def test_native_recovery_preserves_cancel_and_stale_redelivery_is_noop(self):
        store = source('NativeRdpkgRunStore')
        block = store.split('fun resumeAfterProcessRestart()', 1)[1].split('fun updateProgress(', 1)[0]
        self.assertNotIn('KEY_CANCEL_REQUESTED', block)
        self.assertIn('.commit()', block)
        service = source('NativeRdpkgPreparationService')
        startup = service.split('override fun onStartCommand(', 1)[1].split('private fun startArchiveResumeCommand()', 1)[0]
        self.assertIn('flags and START_FLAG_REDELIVERY != 0', startup)
        self.assertIn('!persistedState.isRunning', startup)
        self.assertIn('stopSelf(startId)', startup)
        self.assertIn('runStore.resumeAfterProcessRestart()', startup)
        self.assertIn('startInFlight.set(false)', startup)

    def test_remaining_five_services_guard_persisted_running_state(self):
        for name in (
            "CatalogImportService", "RdpkgImportService",
            "RdpkgExportService", "RdpkgShareService",
            "RdprojectShareService",
        ):
            with self.subTest(service=name):
                code = source(name)
                self.assertIn('runStore.load()', code)
                self.assertIn('!state.isRunning', code)
                self.assertIn('START_REDELIVER_INTENT', code)
                self.assertIn('workerRunning', code)
                self.assertIn('workWakeLock.acquire()', code)
                self.assertIn('workWakeLock.release()', code)


if __name__ == '__main__':
    unittest.main()
