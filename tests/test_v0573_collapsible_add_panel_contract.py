from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class V0573CollapsibleAddPanelContractTests(unittest.TestCase):
    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_project_add_panel_is_collapsible_and_pinnable(self):
        project = self.read_java("ProjectActivity.kt")
        settings = self.read_java("AppSettings.kt")

        self.assertIn("addPanelExpanded", project)
        self.assertIn("addPanelPinned", project)
        self.assertIn("updateAddPanelUi()", project)
        self.assertIn("STATE_ADD_PANEL_EXPANDED", project)
        self.assertIn("projectAddPanelPinned", settings)
        self.assertIn("project_add_panel_pinned", settings)

    def test_all_add_actions_live_inside_single_panel(self):
        project = self.read_java("ProjectActivity.kt")

        build_start = project.index("private fun buildAddPanel")
        build_end = project.index("private fun addChoiceButton", build_start)
        panel = project[build_start:build_end]

        self.assertIn('"Авто"', panel)
        self.assertIn('"Вручну"', panel)
        self.assertIn('"Створити .rdpkg з raw"', panel)
        self.assertIn('"Створити .rdpkg з архіву"', panel)
        self.assertIn("HELP_RAW", panel)
        self.assertIn("HELP_ARCHIVE", panel)
        self.assertIn("HELP_ADD", panel)

    def test_guidance_moves_inside_panel_and_default_status_is_blank(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("ADD_GUIDANCE_TEXT", project)
        self.assertIn('private const val DEFAULT_STATUS_TEXT =\n            ""', project)
        self.assertIn("addPanelBody.addView(", project)
        self.assertIn("ADD_GUIDANCE_TEXT", project)

    def test_pin_forces_expanded_and_persists(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("settings.projectAddPanelPinned =", project)
        self.assertIn("if (\n                        addPanelPinned", project)
        self.assertIn("addPanelExpanded =\n                            true", project)

    def test_operation_status_remains_outside_add_panel(self):
        project = self.read_java("ProjectActivity.kt")

        panel_end = project.index("private fun addChoiceButton")
        status_index = project.index("operationStatus = OperationStatusView")
        self.assertLess(panel_end, status_index)


if __name__ == "__main__":
    unittest.main()
