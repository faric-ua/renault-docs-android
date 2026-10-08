"""Home -> New Volume -> Project Add safety/navigation contract for v0.5.83."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


class NewVolumeRouteContract(unittest.TestCase):
    def test_home_opens_project_chooser_not_saf(self):
        main = read("MainActivity.kt")
        chooser = main.split("private fun chooseProjectForVolume()", 1)[1].split("private fun showCreateProjectDialog()", 1)[0]
        self.assertIn("ProjectChooserActivity::class.java", chooser)
        self.assertNotIn("ACTION_OPEN_DOCUMENT", chooser)
        self.assertNotIn("openPackagePicker(", chooser)

    def test_chooser_opens_project_add_options_not_auto_picker(self):
        src = read("ProjectChooserActivity.kt")
        project_choice = src.split("setOnClickListener {", 2)[-1]  # card click
        self.assertIn("ProjectActivity.intent(", project_choice)
        self.assertIn("project.id", project_choice)
        self.assertIn("showAddPanel =", project_choice)
        self.assertIn("true,", project_choice.split("showAddPanel =", 1)[1].split(")", 1)[0])
        self.assertNotIn("openPicker =", project_choice)
        self.assertIn("finish()", project_choice)

    def test_project_add_is_forced_expanded_only_on_initial_navigation(self):
        project = read("ProjectActivity.kt")
        block = project.split("settings.projectAddPanelPinned", 1)[1].split("nativeRunStore =", 1)[0]
        self.assertIn("savedInstanceState == null", block)
        self.assertIn("getBooleanExtra(EXTRA_SHOW_ADD_PANEL, false)", block)
        self.assertIn("addPanelExpanded = true", block)
        self.assertIn("addPanelLandscapeExpanded = true", block)
        self.assertIn("settings.projectAddPanelPinned", project)
        self.assertNotIn("settings.projectAddPanelPinned =", block)
        self.assertIn("STATE_ADD_PANEL_EXPANDED", project)
        self.assertIn("outState.putBoolean(", project)

    def test_existing_explicit_file_picker_intent_remains_unchanged(self):
        project = read("ProjectActivity.kt")
        self.assertIn('private const val EXTRA_OPEN_PICKER =', project)
        self.assertIn('"openPicker"', project)
        self.assertIn('private const val EXTRA_SHOW_ADD_PANEL =', project)
        self.assertIn('"showAddPanel"', project)
        intent = project.split("fun intent(", 1)[1]
        self.assertIn("openPicker: Boolean = false,", intent)
        self.assertIn("showAddPanel: Boolean = false,", intent)
        self.assertIn("EXTRA_OPEN_PICKER,", intent)
        self.assertIn("EXTRA_SHOW_ADD_PANEL,", intent)
        old_route = project.split("if (", 1)[1]
        self.assertIn("openPackagePicker()", project)
        self.assertIn("intent.getBooleanExtra(\n                EXTRA_OPEN_PICKER,", project)
        self.assertIn("savedInstanceState ==\n                null &&", project)

    def test_user_controls_methods_and_cancel_back(self):
        project = read("ProjectActivity.kt")
        panel = project.split("private fun buildAddPanel()", 1)[1].split("private fun addPanelHeaderButton(", 1)[0]
        for marker in ("Авто", "Вручну", "Створити .rdpkg з raw", "Створити .rdpkg з архіву"):
            self.assertIn(marker, panel)
        chooser = read("ProjectChooserActivity.kt")
        self.assertIn("finish()", chooser)
        self.assertNotIn("openPackagePicker()", chooser)
        self.assertNotIn("startNativeArchiveRdpkgFlow()", chooser)


if __name__ == "__main__":
    unittest.main()
