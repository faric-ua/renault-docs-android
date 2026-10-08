from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"


class V0576OriginalEmojiPinTest(unittest.TestCase):
    def test_real_emoji_restored_and_desaturated_when_unpinned(self):
        project = APP.read_text(encoding="utf-8")
        self.assertIn('label =\n                        "📌"', project)
        self.assertIn("ColorMatrixColorFilter(", project)
        self.assertIn("setSaturation(0f)", project)
        self.assertIn("View.LAYER_TYPE_HARDWARE", project)
        self.assertIn("monochromePinPaint", project)
        self.assertNotIn("R.drawable.ic_push_pin", project)

    def test_pin_behavior_and_fixed_volume_scroll_remains(self):
        project = APP.read_text(encoding="utf-8")
        self.assertIn("settings.projectAddPanelPinned =", project)
        self.assertIn("root.addView(\n            buildAddPanel()", project)
        self.assertIn("projectScroll.addView(\n            volumeContainer", project)


if __name__ == "__main__":
    unittest.main()
