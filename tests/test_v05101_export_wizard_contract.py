"""#178 step-by-step export window: volumes -> folder -> confirm (v0.5.101).

Phone QA of v0.5.100 found stacked/clipped buttons, low-contrast list,
scroll reset on rotation and everything mixed in one window.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class ExportWizardContract(unittest.TestCase):
    def read(self, name):
        return (ANDROID / name).read_text(encoding="utf-8")

    def test_three_explicit_steps(self):
        state = self.read("ProjectExportWizardState.kt")
        for step in ("VOLUMES(1)", "FOLDER(2)", "CONFIRM(3)"):
            self.assertIn(step, state)
        self.assertIn("Step.VOLUMES -> selected.isNotEmpty()", state)
        self.assertIn("Step.FOLDER -> selected.isNotEmpty() && folderUri != null", state)
        self.assertIn("step == Step.CONFIRM && selected.isNotEmpty() && folderUri != null", state)
        wizard = self.read("ProjectExportWizard.kt")
        for label in ('"Вибери томи"', '"Куди зберегти"', '"Перевір і почни"', '"Почати експорт"'):
            self.assertIn(label, wizard)

    def test_header_has_title_step_and_close(self):
        wizard = self.read("ProjectExportWizard.kt")
        header = wizard.split("private fun buildHeader()", 1)[1].split("private fun renderVolumes()", 1)[0]
        self.assertIn('"✕"', header)
        self.assertIn('contentDescription = "Закрити"', header)
        self.assertIn("dp(48)", header)
        self.assertIn("close()", header)

    def test_footer_is_fixed_row_and_body_is_the_only_scroll(self):
        wizard = self.read("ProjectExportWizard.kt")
        show = wizard.split("fun show(): AlertDialog", 1)[1].split("fun render()", 1)[0]
        self.assertIn("orientation = LinearLayout.HORIZONTAL", show)
        self.assertIn("LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)", show)
        self.assertIn("setCanceledOnTouchOutside(false)", show)
        self.assertNotIn("setPositiveButton", wizard)
        self.assertNotIn("setNeutralButton", wizard)

    def test_readable_tiles_not_grey_list(self):
        wizard = self.read("ProjectExportWizard.kt")
        self.assertIn("Ui.textView(activity, identity, 17f, Ui.text)", wizard)
        self.assertIn("stroke = if (selected) Ui.accent else Ui.border", wizard)
        self.assertNotIn("setMultiChoiceItems", wizard)

    def test_scroll_position_survives_rotation(self):
        wizard = self.read("ProjectExportWizard.kt")
        home = self.read("HomeProjectDialogController.kt")
        self.assertIn("state.scrollY = scrollY", wizard)
        self.assertIn("scroll.scrollTo(0, restoreY)", wizard)
        self.assertIn("STATE_EXPORT_SCROLL_Y", home)
        self.assertIn("STATE_EXPORT_STEP", home)
        self.assertIn("STATE_EXPORT_FOLDER", home)

    def test_folder_pick_returns_to_window_and_never_starts_export(self):
        main = self.read("MainActivity.kt")
        result = main.split("if (requestCode == REQUEST_FOLDER_EXPORT_DESTINATION) {", 1)[1].split(
            "if (requestCode != REQUEST_DATASET_FOLDER)", 1
        )[0]
        self.assertIn("takePersistableUriPermission(folder, flags)", result)
        self.assertIn("projectDialogs.onExportFolderPicked(folder.toString())", result)
        self.assertNotIn("ProjectFolderExportService.start", result)
        start = main.split("private fun startSeparateVolumeExport(", 1)[1].split("private fun ", 1)[0]
        self.assertIn("persistedUriPermissions", start)
        self.assertIn("isWritePermission", start)

    def test_status_and_progress_stay_outside_the_window(self):
        wizard = self.read("ProjectExportWizard.kt")
        self.assertNotIn("operationStatus", wizard)
        self.assertNotIn("ProjectFolderExportService", wizard)
        self.assertNotIn("ProjectFolderExportRunStore", wizard)
        main = self.read("MainActivity.kt")
        self.assertIn("folderExportRunStore.load()", main)


if __name__ == "__main__":
    unittest.main()
