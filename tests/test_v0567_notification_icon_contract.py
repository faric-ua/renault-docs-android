from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
ICON = ROOT / "android/app/src/main/res/drawable/ic_notification_document.xml"


class V0567NotificationIconContractTests(unittest.TestCase):
    SERVICES = (
        "ConversionService.kt",
        "NativeRdpkgPreparationService.kt",
        "CatalogImportService.kt",
        "RdpkgImportService.kt",
        "RdpkgExportService.kt",
        "RdpkgShareService.kt",
        "RdprojectShareService.kt",
    )

    def test_all_long_running_services_use_neutral_app_icon(self):
        for name in self.SERVICES:
            with self.subTest(service=name):
                text = (JAVA / name).read_text(encoding="utf-8")
                self.assertIn("R.drawable.ic_notification_document", text)
                self.assertNotIn("stat_sys_download", text)
                self.assertNotIn("stat_sys_upload", text)

    def test_notification_icon_is_monochrome_vector(self):
        text = ICON.read_text(encoding="utf-8")
        self.assertIn("<vector", text)
        self.assertIn('android:width="24dp"', text)
        self.assertIn('android:height="24dp"', text)
        self.assertIn('android:viewportWidth="108"', text)
        self.assertIn('android:viewportHeight="108"', text)
        self.assertIn('android:strokeColor="#FFFFFFFF"', text)
        self.assertIn('android:fillColor="#FFFFFFFF"', text)
        self.assertIn('M31,22 L67,22 L82,37 L82,86 L31,86 Z', text)
        self.assertIn('M67,22 L67,39 L82,39', text)
        self.assertIn('M41,51 L64,51 M41,63 L71,63 M41,75 L59,75', text)

    def test_release_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.76"', gradle)
        self.assertIn("versionCode = 92", gradle)


if __name__ == "__main__":
    unittest.main()
