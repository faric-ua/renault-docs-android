"""v0.5.150 — update Renault Docs from Google Drive (owner 2026-10-17 15:49–15:50).

«Перевірити оновлення → вікно: є нова версія, коротко що змінилося, «Детальніше» → людина
вирішує сама». Ported from Track Catcher (phone PASS). Contract:
- a switch for a future Google Play build (Play forbids self-updates);
- the window shows what changed, «Детальніше», «Оновити» / «Не зараз»;
- the APK is installed only after size, SHA-256, package, version and signing key matched;
- the changelog lives in assets/whats_new.txt and starts with the app's version.
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app"
SRC = APP / "src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class DriveUpdateContract(unittest.TestCase):
    def test_switch_for_play(self):
        gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('buildConfigField("boolean", "DRIVE_UPDATES", "true")', gradle)
        self.assertIn("BuildConfig.DRIVE_UPDATES", source("AppUpdateUi.kt"))
        self.assertIn("if (AppUpdateSwitch.ENABLED)", source("SettingsActivity.kt"))
        self.assertIn("if (AppUpdateSwitch.ENABLED)", source("MainActivity.kt"))
        manifest = (APP / "src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("android.permission.REQUEST_INSTALL_PACKAGES", manifest)
        paths = (APP / "src/main/res/xml/file_paths.xml").read_text(encoding="utf-8")
        self.assertIn('path="app-update/"', paths)

    def test_window_with_whats_new(self):
        ui = source("AppUpdateUi.kt")
        verdict = ui.split("fun showVerdict(")[1].split("private fun showDetails(")[0]
        self.assertIn('title = "Є нова версія ${m.versionName}"', verdict)
        self.assertIn("AppUpdate.newerMessage(", verdict)
        self.assertIn('positive = "Оновити"', verdict)
        self.assertIn('negative = "Не зараз"', verdict)
        self.assertIn('neutral = if (m.details.isNotBlank()) "Детальніше" else null', verdict)
        self.assertIn("download(m)", verdict)
        words = source("AppUpdate.kt")
        self.assertIn('append("\\n\\nЩо змінилось:\\n")', words)

    def test_install_only_after_checks(self):
        ui = source("AppUpdateUi.kt")
        download = ui.split("private fun download(")[1].split("fun install(")[0]
        check_at = download.index("AppUpdate.checkDownloaded(")
        install_at = download.index("install(file)")
        self.assertLess(check_at, install_at)
        self.assertIn("if (problem != null)", download)
        core = source("AppUpdate.kt")
        checks = core.split("fun checkDownloaded(")[1].split("\n    }\n")[0]
        for needle in ("m.apkSha256.equals(sha256", "apkPackage != m.packageName", "apkVersionCode != m.versionCode", "installedCertSha256 == null"):
            self.assertIn(needle, checks)

    def test_publish_writes_whats_new(self):
        ui = source("AppUpdateUi.kt")
        publish = ui.split("private fun publish()")[1].split("fun manifestId()")[0]
        self.assertIn("WhatsNew.forVersion(whatsNew, BuildConfig.VERSION_NAME)", publish)
        self.assertIn("WhatsNew.recent(whatsNew, DETAILS_VERSIONS)", publish)

    def test_changelog_starts_with_this_version(self):
        gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
        version = re.search(r'versionName = "([0-9.]+)"', gradle).group(1)
        whats_new = (APP / "src/main/assets/whats_new.txt").read_text(encoding="utf-8")
        self.assertEqual(version, whats_new.strip().splitlines()[0].strip())

    def test_reset_asks_first(self):
        ui = source("AppUpdateUi.kt")
        reset = ui.split("private fun confirmReset()")[1].split("private fun publish()")[0]
        self.assertIn('title = "Почати заново?"', reset)
        self.assertIn("role = DialogRole.DANGER", reset)

    def test_jvm_test_present(self):
        test = (APP / "src/test/java/com/saney/renaultdocs/AppUpdateTest.kt").read_text(encoding="utf-8")
        self.assertIn("downloadedApkIsCheckedFully", test)
        self.assertIn("builtInWhatsNewHasThisVersion", test)

    def test_version(self):
        gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 166)


if __name__ == "__main__":
    unittest.main()
