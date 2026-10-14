"""v0.5.129: in «Каталог Renault Docs» the «✓ Встановлено» status is a
separate line under the volume details, in the green success colour
(owner 2026-10-14 04:50: «перенесемо на новий рядок … зеленкуватим»).
"""
import re
import unittest
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "android/app/src/main/java/com/saney/renaultdocs"


class CatalogInstalledGreenLine(unittest.TestCase):
    def setUp(self):
        self.activity = (SRC / "DriveCatalogActivity.kt").read_text(encoding="utf-8")

    def test_installed_is_own_text_view_in_success_colour(self):
        m = re.search(r'value\s*=\s*"✓ Встановлено",\s*sizeSp\s*=\s*Ui\.secondaryTextSp,\s*color\s*=\s*Ui\.success', self.activity)
        self.assertIsNotNone(m, "«✓ Встановлено» must be its own Ui.textView coloured Ui.success")

    def test_installed_not_joined_into_detail_line(self):
        detail = self.activity.split("val detail =", 1)[1].split("text.addView(", 1)[0]
        self.assertNotIn('"✓ Встановлено"', detail)
        self.assertIn("formatSize(", detail)

    def test_version(self):
        gradle = (SRC.parents[5] / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)), 145)

    def test_success_colour_is_green(self):
        ui = (SRC / "Ui.kt").read_text(encoding="utf-8")
        self.assertIn('val success: Int = Color.parseColor("#62D296")', ui)


if __name__ == "__main__":
    unittest.main()
