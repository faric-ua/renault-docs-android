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

    def test_pin_keeps_previous_diagonal_shape_with_state_tint(self):
        project = self.read_java("ProjectActivity.kt")
        pin = (DRAWABLE / "ic_push_pin.xml").read_text(encoding="utf-8")

        self.assertIn('android:rotation="-45"', pin)
        self.assertIn("Ui.danger", project)
        self.assertIn("Ui.text", project)
        self.assertIn("R.drawable.ic_push_pin", project)

    def test_help_explains_neutral_and_red_pin_states_without_emoji(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("Кнопка закріплення світла", project)
        self.assertIn("червона", project)
        self.assertNotIn("📌 закріплює панель", project)


if __name__ == "__main__":
    unittest.main()
