from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
DRAWABLE = ROOT / "android/app/src/main/res/drawable"


class V0575StickyAddPanelContractTests(unittest.TestCase):
    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_add_panel_is_outside_volume_scroll(self):
        project = self.read_java("ProjectActivity.kt")

        add_root = project.index("root.addView(\n            buildAddPanel()")
        scroll_create = project.index("projectScroll =")
        scroll_root = project.index("root.addView(\n            projectScroll", scroll_create)
        volume_add = project.index("projectScroll.addView(\n            volumeContainer")

        self.assertLess(add_root, scroll_create)
        self.assertLess(volume_add, scroll_root)
        self.assertNotIn("scrollContent.addView(\n            buildAddPanel()", project)

    def test_volume_scroll_has_small_gap_below_fixed_area(self):
        project = self.read_java("ProjectActivity.kt")
        scroll_root = project.index("root.addView(\n            projectScroll")
        section = project[scroll_root:scroll_root + 700]

        self.assertIn("topMargin =", section)
        self.assertIn("10,", section)

    def test_pin_restores_original_colored_emoji_with_grayscale_unpinned(self):
        project = self.read_java("ProjectActivity.kt")
        self.assertIn('label =\\n                        "📌"', project)
        self.assertIn("monochromePinPaint", project)
        self.assertIn("View.LAYER_TYPE_HARDWARE", project)
        self.assertIn("setSaturation(0f)", project)
        self.assertIn("if (addPanelPinned)", project)

    def test_help_explains_true_emoji_and_two_pin_states(self):
        project = self.read_java("ProjectActivity.kt")
        self.assertIn("📌 закріплює панель", project)
        self.assertIn("чорно-біла", project)
        self.assertIn("кольорова", project)


if __name__ == "__main__":
    unittest.main()
