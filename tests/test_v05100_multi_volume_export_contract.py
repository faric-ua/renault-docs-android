"""Focused non-destructive contract for arbitrary subset SAF .rdpkg export."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class MultiVolumeExportContract(unittest.TestCase):
    def read(self, name):
        return (ANDROID / name).read_text(encoding="utf-8")

    def test_chooser_is_checkbox_multi_select_not_single_tap_export(self):
        home = self.read("HomeProjectDialogController.kt")
        self.assertIn('setNeutralButton("Вибрати томи")', home)
        self.assertIn('setPositiveButton("Усі томи")', home)
        chooser = home.split("private fun showMultiVolumeChooser(", 1)[1].split(
            "private fun shareProject(", 1
        )[0]
        self.assertIn(".setMultiChoiceItems(labels, checked)", chooser)
        self.assertNotIn(".setItems(labels)", chooser)
        self.assertIn("selectedExportVolumeIds.add(id)", chooser)
        self.assertIn("selectedExportVolumeIds.remove(id)", chooser)
        self.assertIn('setNeutralButton("Вибрати всі", null)', chooser)
        self.assertIn('"Очистити"', chooser)
        self.assertIn('setPositiveButton("Вибрати папку", null)', chooser)
        self.assertIn("selectedExportVolumeIds.isNotEmpty()", chooser)
        self.assertIn("ProjectVolumeExportPlan.select(fresh, selectedExportVolumeIds.toList())", chooser)
        self.assertIn("onSeparateVolumeExportRequested(project.id, selectedIds)", chooser)
        self.assertIn("DialogUi.apply(dialog, DialogRole.CHOICE)", chooser)

    def test_rotation_preserves_exact_opaque_selected_volume_ids(self):
        home = self.read("HomeProjectDialogController.kt")
        self.assertIn("STATE_SELECTED_EXPORT_VOLUMES", home)
        self.assertIn("ArrayList(selectedExportVolumeIds)", home)
        self.assertIn("state?.getStringArrayList(STATE_SELECTED_EXPORT_VOLUMES)", home)
        self.assertIn("DIALOG_MULTI_VOLUME ->", home)
        self.assertIn("showMultiVolumeChooser(project)", home)
        self.assertIn("activeKind == DIALOG_MULTI_VOLUME", home)
        self.assertIn("selectedExportVolumeIds.clear()", home)

    def test_selection_cannot_expand_to_full_project_during_saf_handoff(self):
        main = self.read("MainActivity.kt")
        service = self.read("ProjectFolderExportService.kt")
        plan = self.read("ProjectVolumeExportPlan.kt")
        self.assertIn("STATE_PENDING_FOLDER_EXPORT_VOLUMES", main)
        self.assertIn("ProjectFolderExportPlan", service.replace("ProjectVolumeExportPlan", "ProjectFolderExportPlan"))
        self.assertIn("ProjectVolumeExportPlan.select(plan, volumeIds)", main)
        self.assertIn("ProjectFolderExportService.start(this, projectId, folder, volumeIds)", main)
        self.assertIn("ProjectVolumeExportPlan.select(allPlan, selectedVolumeIds)", service)
        self.assertIn("ProjectVolumeExportPlan.select(allPlan, state.volumeIds)", service)
        self.assertIn("selectedVolumeIds.isNotEmpty()", plan)
        self.assertIn("ids.all { id -> plan.any { it.volumeId == id } }", plan)
        self.assertIn('record.put("status", "EXPORTED_SHA256_VERIFIED")', service)
        self.assertIn("EXISTING_UNVERIFIED", service)
        self.assertNotIn("PreparedShareStore.deleteProject", service)
        self.assertNotIn("RdprojectExporter.export", service)


if __name__ == "__main__":
    unittest.main()
