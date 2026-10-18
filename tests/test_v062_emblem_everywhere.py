"""Renodocs 0.6.2 (owner 2026-10-18 03:33): the emblem on every app screen and in
the menus, tiles half transparent so it shows through; text stays opaque.

Not on the document viewer and the guide (WebView pages): panels over a document
stay opaque, the documents themselves stay clean.
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

EMBLEM_SCREENS = [
    "MainActivity", "ProjectActivity", "SettingsActivity", "CreateProjectActivity",
    "ProjectChooserActivity", "DriveCatalogActivity", "RdprojectImportActivity",
    "ModernDatasetActivity", "ModernVolumeActivity", "NativeSectionActivity",
    "VolumeDocumentationActivity",
]


def read(name):
    return (SRC / f"{name}.kt").read_text(encoding="utf-8")


class EmblemEverywhere(unittest.TestCase):
    def test_every_app_screen_uses_the_emblem_background(self):
        for name in EMBLEM_SCREENS:
            kt = read(name)
            self.assertIn("Ui.emblemScreenBackground(context)", kt, name)
            self.assertIsNone(re.search(r"setBackgroundColor\(\s*Ui\.background", kt), name)

    def test_viewer_and_guide_stay_clean(self):
        for name in ["ViewerActivity", "GuideActivity"]:
            self.assertNotIn("emblemScreenBackground", read(name), name)
        ui = read("Ui")
        self.assertIn("activity !is ViewerActivity && activity !is GuideActivity", ui)

    def test_tiles_half_transparent_text_untouched(self):
        ui = read("Ui")
        self.assertIn("const val TILE_ALPHA: Int = 0x80", ui)
        self.assertIn("setOf(surface, surfaceAlt, selectedFill)", ui)
        self.assertIn("setColor(if (opaque) fill else tileFill(context, fill))", ui)
        self.assertIn("setColor(tileFill(context, selectedFill))", ui)
        # strokes (frames) and text colours are not made transparent
        self.assertIn("setStroke(dp(context, 1), stroke)", ui)

    def test_menu_panel_opaque_with_emblem_inside(self):
        dialog = read("DialogUi")
        start = dialog.index("Ui.emblemDialogBackground(")
        block = dialog[start:start + 600]
        self.assertIn("opaque =\n                        true", block)

    def test_one_emblem_per_launch_decoded_once(self):
        kt = read("HomeEmblem")
        self.assertIn("val current: Int by lazy", kt)
        self.assertIn("private var cached: Bitmap?", kt)
        alpha = float(re.search(r"ALPHA: Float = ([0-9.]+)f", kt).group(1))
        self.assertTrue(0.15 <= alpha <= 0.35, alpha)
        self.assertIn("class EmblemBackgroundDrawable", kt)

    def test_home_has_no_second_emblem(self):
        main = read("MainActivity")
        self.assertNotIn("setImageResource(HomeEmblem.current)", main)


if __name__ == "__main__":
    unittest.main()
