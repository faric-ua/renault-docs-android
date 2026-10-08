from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
DRAWABLE = ROOT / "android/app/src/main/res/drawable"


class V0574AddPanelStatusRefinementTests(unittest.TestCase):
    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_transient_status_is_inside_add_panel(self):
        project = self.read_java("ProjectActivity.kt")

        build_start = project.index("private fun buildAddPanel")
        build_end = project.index("private fun addChoiceButton", build_start)
        panel = project[build_start:build_end]

        self.assertIn("statusText =", panel)
        self.assertIn("Ui.statusWarmFill", panel)
        self.assertIn("Ui.statusWarmBorder", panel)
        self.assertIn("Ui.statusWarmText", panel)
        self.assertIn("addPanelBody.addView(", panel)
        self.assertIn("statusText", panel)
        self.assertNotIn("ADD_GUIDANCE_TEXT", project)

    def test_blank_status_hides_and_nonblank_status_shows(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("addTextChangedListener(", project)
        self.assertIn("View.GONE", project)
        self.assertIn("View.VISIBLE", project)
        self.assertIn("isBlank()", project)

    def test_pin_uses_original_emoji_and_monochrome_inactive_state(self):
        project = self.read_java("ProjectActivity.kt")
        self.assertIn('label =\n                        "📌"', project)
        self.assertIn("monochromePinPaint", project)
        self.assertIn("setLayerType(", project)
        self.assertIn("setSaturation(0f)", project)
        self.assertNotIn("R.drawable.ic_push_pin", project)

    def test_expand_chevron_is_heavier(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn('"▲"', project)
        self.assertIn('"▼"', project)
        self.assertIn("android.graphics.Typeface.BOLD", project)

    def test_operation_progress_remains_outside_collapsible_body(self):
        project = self.read_java("ProjectActivity.kt")

        build_start = project.index("private fun buildAddPanel")
        build_end = project.index("private fun addChoiceButton", build_start)
        panel = project[build_start:build_end]
        self.assertIn("OperationStatusView(", panel)
        self.assertIn("operationStatus =", panel)
        self.assertIn("addView(\n                addPanelBody", panel)
        self.assertIn("addView(\n                operationStatus", panel)
        self.assertLess(panel.index("addView(\n                addPanelBody"), panel.index("operationStatus ="))


if __name__ == "__main__":
    unittest.main()
