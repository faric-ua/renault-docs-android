"""v0.5.131: system «Назад» on Android 16 + small fixes batch (TODO.md).

Phone QA 2026-10-14 07:22: «Назад» in a guide section closed the whole guide
(landed in Settings). The app targets Android 16 (targetSdk 36), where the
system «Назад» no longer calls onBackPressed().
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def src(name):
    return (APP / name).read_text(encoding="utf-8")


class BackOnAndroid16(unittest.TestCase):
    def test_helper_registers_callback_and_finishes_when_unhandled(self):
        s = src("SystemBack.kt")
        self.assertIn("Build.VERSION.SDK_INT < 33", s)
        self.assertIn("registerOnBackInvokedCallback(", s)
        self.assertIn("if (!handle()) {\n                    activity.finish()", s)
        self.assertIn("unregisterOnBackInvokedCallback(", s)

    def test_every_screen_with_its_own_back_rule_uses_it(self):
        for name in ("NativeSectionActivity.kt", "VolumeDocumentationActivity.kt", "ViewerActivity.kt"):
            s = src(name)
            self.assertIn("SystemBack.register(", s, name)
            self.assertIn("SystemBack.unregister(", s, name)
            self.assertIn("private fun handleBack(): Boolean", s, name)
            back = s.split("override fun onBackPressed()", 1)[1].split("private fun handleBack", 1)[0]
            self.assertIn("handleBack()", back, name)
        # Any screen overriding onBackPressed must also handle Android 16.
        for f in APP.glob("*.kt"):
            s = f.read_text(encoding="utf-8")
            if "override fun onBackPressed()" in s:
                self.assertTrue("SystemBack.register(" in s or "OnBackInvokedCallback" in s, f.name)

    def test_viewer_keeps_its_steps(self):
        s = src("ViewerActivity.kt")
        inside = s.split("private fun navigateBackInside(): Boolean", 1)[1].split("\n    }\n", 1)[0]
        for marker in ("pdfCompanionVisible", "companionWebView.goBack()", "hidePdfCompanion()", "webView.goBack()", "return false"):
            self.assertIn(marker, inside)
        handle = s.split("private fun handleBack(): Boolean", 1)[1].split("\n    }\n", 1)[0]
        self.assertIn("pdfFullscreen", handle)

    def test_guide_sections_do_not_use_history(self):
        page = (ROOT / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertIn("location.replace(h)", page)
        act = src("GuideActivity.kt")
        self.assertIn("location.replace('#')", act)
        self.assertIn(".fragment", act)


class SmallFixesBatch(unittest.TestCase):
    def test_settings_tiles_have_a_gap(self):
        s = src("SettingsActivity.kt")
        self.assertIn("private fun LinearLayout.addCard(", s)
        for card in ("settingRow(", "backupCard()", "guideCard(),", "supportCard(),", "infoCard("):
            self.assertIn("body.addCard(\n            " + card, s)
            self.assertNotIn("body.addView(\n            " + card, s)

    def test_no_line_break_after_check_mark(self):
        page = (ROOT / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertNotIn("✓ Встановлено", page)
        self.assertIn("✓ Встановлено", page)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)), 147)


if __name__ == "__main__":
    unittest.main()
