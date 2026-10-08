from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"

class V0577LandscapeAddPanelContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = ACTIVITY.read_text(encoding="utf-8")

    def test_collapse_on_entering_landscape_and_restore_pinned_portrait(self):
        s = self.source
        self.assertIn("previousOrientation == Configuration.ORIENTATION_LANDSCAPE", s)
        self.assertIn("previouslyExpanded", s)
        self.assertIn("addPanelPinned ->\n                    true", s)
        self.assertIn("STATE_ADD_PANEL_ORIENTATION", s)
        self.assertIn("resources.configuration.orientation", s)

    def test_landscape_effective_unpin_does_not_persist(self):
        s = self.source
        self.assertIn("addPanelPinned && !isLandscapeOrientation()", s)
        self.assertIn("if (!isLandscapeOrientation()) {", s)
        self.assertIn("addPanelPinButton.isEnabled = !landscape", s)
        self.assertIn("if (effectivePinned)", s)
        self.assertIn("!effectivePinned", s)

    def test_fixed_volume_scroll_and_emoji_preserved(self):
        s = self.source
        self.assertIn("root.addView(\n            buildAddPanel()", s)
        self.assertIn("projectScroll.addView(\n            volumeContainer", s)
        self.assertIn('label =\n                        "📌"', s)
        self.assertIn("monochromePinPaint", s)

    def test_activity_recreates_when_rotating(self):
        s = MANIFEST.read_text(encoding="utf-8")
        attrs = s.split('android:name=".ProjectActivity"',1)[1].split("/>",1)[0]
        self.assertNotIn("configChanges=", attrs)

if __name__ == "__main__":
    unittest.main()
