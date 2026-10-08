from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"


class V0577LandscapeAddContract(unittest.TestCase):
    def source(self):
        return APP.read_text(encoding="utf-8")

    def test_landscape_uses_ephemeral_collapsed_panel(self):
        src = self.source()
        self.assertIn("Configuration.ORIENTATION_LANDSCAPE", src)
        self.assertIn("addPanelLandscapeExpanded =\n        false", src)
        self.assertIn("val pinnedHere = addPanelPinned && !landscape", src)
        self.assertIn("addPanelLandscapeExpanded\n            } else {\n                addPanelExpanded", src)
        self.assertIn("if (expandedHere) View.VISIBLE else View.GONE", src)

    def test_portrait_pinned_preference_survives_rotation(self):
        src = self.source()
        self.assertIn("settings.projectAddPanelPinned =", src)
        self.assertIn("if (!isLandscapeLayout()) {", src)
        self.assertIn("STATE_ADD_PANEL_EXPANDED", src)
        self.assertIn("savedInstanceState", src)
        self.assertNotIn("settings.projectAddPanelPinned =\n                            false", src)

    def test_manual_landscape_expand_is_allowed_without_pin(self):
        src = self.source()
        self.assertIn("private fun toggleAddPanel()", src)
        self.assertIn("addPanelLandscapeExpanded =\n                !addPanelLandscapeExpanded", src)
        self.assertIn("addPanelPinButton.isEnabled =\n            !landscape", src)
        self.assertIn("toggleAddPanel()", src)

    def test_existing_layout_emoji_and_help_remain(self):
        src = self.source()
        self.assertIn('label =\n                        "📌"', src)
        self.assertIn("monochromePinPaint", src)
        self.assertIn("root.addView(\n            buildAddPanel()", src)
        self.assertIn("projectScroll.addView(\n            volumeContainer", src)
        self.assertIn("В альбомному режимі панель автоматично згортається", src)
        self.assertNotIn("android:configChanges=\"orientation|screenSize\" />\n\n        <activity\n            android:name=\".ConversionActivity\"", MANIFEST.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
