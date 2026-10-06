from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class V0565BackgroundOperationsContractTests(unittest.TestCase):
    LONG_RUNNING_SERVICES = (
        "ConversionService.kt",
        "NativeRdpkgPreparationService.kt",
        "CatalogImportService.kt",
        "RdpkgImportService.kt",
        "RdpkgExportService.kt",
        "RdpkgShareService.kt",
        "RdprojectShareService.kt",
    )

    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_every_long_running_operation_is_foreground_and_restartable(self):
        for name in self.LONG_RUNNING_SERVICES:
            with self.subTest(service=name):
                service = self.read_java(name)
                self.assertIn("startForeground(", service)
                self.assertIn("START_REDELIVER_INTENT", service)

    def test_every_long_running_operation_holds_cpu_awake(self):
        for name in self.LONG_RUNNING_SERVICES:
            with self.subTest(service=name):
                service = self.read_java(name)
                has_wake_lock = (
                    "PowerManager.PARTIAL_WAKE_LOCK" in service
                    or "BackgroundWorkWakeLock" in service
                )
                self.assertTrue(
                    has_wake_lock,
                    f"{name} must hold a partial wake lock while active",
                )

        helper = self.read_java("BackgroundWorkWakeLock.kt")
        self.assertIn("PowerManager.PARTIAL_WAKE_LOCK", helper)
        self.assertIn("wakeLock.acquire(", helper)
        self.assertIn("wakeLock.release()", helper)

    def test_long_running_activities_do_not_force_screen_on(self):
        for name in (
            "ProjectActivity.kt",
            "ConversionActivity.kt",
        ):
            with self.subTest(activity=name):
                activity = self.read_java(name)
                self.assertNotIn("FLAG_KEEP_SCREEN_ON", activity)

    def test_services_survive_task_removal(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(
            encoding="utf-8"
        )

        for service in (
            "ConversionService",
            "NativeRdpkgPreparationService",
            "CatalogImportService",
            "RdpkgImportService",
            "RdpkgExportService",
            "RdpkgShareService",
            "RdprojectShareService",
        ):
            with self.subTest(service=service):
                start = manifest.index(f'android:name=".{service}"')
                block = manifest[start : start + 260]
                self.assertIn('android:stopWithTask="false"', block)

    def test_conversion_has_background_restart_contract(self):
        conversion = self.read_java("ConversionService.kt")
        self.assertIn("PowerManager.PARTIAL_WAKE_LOCK", conversion)
        self.assertGreaterEqual(
            conversion.count("START_REDELIVER_INTENT"),
            2,
        )


if __name__ == "__main__":
    unittest.main()
