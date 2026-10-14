"""v0.5.130: user guide inside the app (owner 2026-10-14: «як у Track Catcher…
вшити там, де про програму, щоб кнопка була і інструкцію можна було читати»).
"""
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
GUIDE = ROOT / "android/app/src/main/assets/guide/index.html"
BUILDER = ROOT / "docs/ua/user-guide"


class InAppGuide(unittest.TestCase):
    def setUp(self):
        self.page = GUIDE.read_text(encoding="utf-8")

    def test_page_is_generated_from_the_same_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "index.html"
            subprocess.run([sys.executable, "-I", str(BUILDER / "build_app_guide.py"), str(out)],
                           check=True, capture_output=True)
            self.assertEqual(out.read_text(encoding="utf-8"), self.page,
                             "assets/guide/index.html is stale: run docs/ua/user-guide/build.sh")

    def test_every_section_has_a_home_entry_and_page(self):
        sys.path.insert(0, str(BUILDER))
        import build_guide
        self.assertGreaterEqual(len(build_guide.S), 11)
        for sid, title, _ in build_guide.S:
            self.assertIn(f'href="#{sid}"', self.page)
            self.assertIn(f'<section id="{sid}" class="page stack" hidden>', self.page)
        self.assertIn('id="katalog"', self.page)
        self.assertIn("Готові проєкти з Google Диска", self.page)
        self.assertIn("✓\u00a0Встановлено", self.page)  # v0.5.131: no break after «✓»
        self.assertIn(build_guide.VERSION, self.page)

    def test_offline_no_external_resources(self):
        self.assertNotRegex(self.page, r'(src|href)="https?://')
        self.assertNotIn("<img", self.page)
        self.assertNotIn("mermaid", self.page.lower())

    def test_markdown_guide_is_current(self):
        md = (ROOT / "docs/ua/USER_GUIDE.md").read_text(encoding="utf-8")
        self.assertIn("v0.5.130", md)
        self.assertIn('<a id="katalog"></a>', md)
        self.assertIn("Інструкція користувача", md)

    def test_activity_serves_only_guide_assets_over_https_origin(self):
        act = (APP / "GuideActivity.kt").read_text(encoding="utf-8")
        self.assertIn('ASSET_HOST = "appassets.androidplatform.net"', act)
        self.assertIn("assets/guide/index.html", act)
        self.assertIn("allowFileAccess =\n                    false", act)
        self.assertIn('!path.startsWith(GUIDE_DIR) || path.contains("..")', act)
        self.assertIn("location.replace('#')", act)  # v0.5.131: section → list → close
        self.assertIn("OnBackInvokedCallback", act)
        self.assertIn("KEY_URL", act)  # section survives rotation
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertRegex(manifest, r'android:name="\.GuideActivity"\s+android:exported="false"')

    def test_settings_about_has_guide_button(self):
        s = (APP / "SettingsActivity.kt").read_text(encoding="utf-8")
        about = s.split('"Про програму",', 1)[1]
        self.assertLess(about.index("guideCard()"), about.index("supportCard()"))
        self.assertIn("📖 Інструкція користувача", s)
        self.assertIn("GuideActivity.open(", s)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)), 146)


if __name__ == "__main__":
    unittest.main()
