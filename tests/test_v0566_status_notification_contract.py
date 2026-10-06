from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class V0566StatusNotificationContractTests(unittest.TestCase):
    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_terminal_status_does_not_hide_full_detail(self):
        status = self.read_java("OperationStatusView.kt")

        self.assertIn("terminal = true", status)
        self.assertIn("detailView.ellipsize =", status)
        self.assertIn("null", status)
        self.assertIn("Configuration.ORIENTATION_LANDSCAPE", status)
        self.assertIn("detailView.maxLines", status)

    def test_status_text_can_copy_the_full_card(self):
        status = self.read_java("OperationStatusView.kt")

        self.assertIn("ClipboardManager::class.java", status)
        self.assertIn("ClipData.newPlainText(", status)
        self.assertIn('"Renault Docs status"', status)
        self.assertIn('"Статус скопійовано"', status)
        self.assertIn("copyCurrentText()", status)
        self.assertGreaterEqual(
            status.count("setOnClickListener"),
            4,
        )
        self.assertIn("titleView.text", status)
        self.assertIn("subjectView.text", status)
        self.assertIn("detailView.text", status)

    def test_android_13_notification_permission_is_requested(self):
        main = self.read_java("MainActivity.kt")
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(
            encoding="utf-8"
        )

        self.assertIn("android.permission.POST_NOTIFICATIONS", manifest)
        self.assertIn("Manifest.permission.POST_NOTIFICATIONS", main)
        self.assertIn("Build.VERSION_CODES.TIRAMISU", main)
        self.assertIn("requestNotificationPermissionIfNeeded()", main)
        self.assertIn("requestPermissions(", main)
        self.assertIn("onRequestPermissionsResult(", main)
        self.assertIn(
            "Фонові процеси працюватимуть, але прогрес може не показуватися у шторці.",
            main,
        )

    def test_release_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(
            encoding="utf-8"
        )

        self.assertIn('versionName = "0.5.68"', gradle)
        self.assertIn("versionCode = 84", gradle)


if __name__ == "__main__":
    unittest.main()
