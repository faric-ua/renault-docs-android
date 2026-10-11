"""Focused non-destructive contract for arbitrary subset SAF .rdpkg export.

v0.5.100 introduced checkbox multi-select; v0.5.101 (#178) moved it into a
step-by-step window. The safety contract below is unchanged.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class MultiVolumeExportContract(unittest.TestCase):
    def read(self, name):
        return (ANDROID / name).read_text(encoding="utf-8")

    def test_checks_never_start_saf_or_export(self):
        wizard = self.read("ProjectExportWizard.kt")
        volumes = wizard.split("private fun renderVolumes()", 1)[1].split("private fun renderFolder()", 1)[0]
        self.assertIn("state.setSelected(item.volumeId, !state.isSelected(item.volumeId))", volumes)
        self.assertIn("state.toggleAll(planIds)", volumes)
        self.assertIn('"Очистити"', volumes)
        self.assertIn('"Вибрати всі"', volumes)
        self.assertNotIn("onPickFolder", volumes)
        self.assertNotIn("onStart", volumes)

    def test_rotation_preserves_exact_opaque_selected_volume_ids(self):
        home = self.read("HomeProjectDialogController.kt")
        self.assertIn("STATE_SELECTED_EXPORT_VOLUMES", home)
        self.assertIn("ArrayList(exportWizardState.selectedVolumeIds)", home)
        self.assertIn("state?.getStringArrayList(STATE_SELECTED_EXPORT_VOLUMES)", home)
        self.assertIn("DIALOG_EXPORT_WIZARD ->", home)
        self.assertIn("showExportWizard(project)", home)
        self.assertIn("activeKind == DIALOG_EXPORT_WIZARD", home)
        self.assertIn("isConsistentWith(plan.map { it.volumeId })", home)

    def test_selection_cannot_expand_to_full_project_before_service(self):
        home = self.read("HomeProjectDialogController.kt")
        main = self.read("MainActivity.kt")
        service = self.read("ProjectFolderExportService.kt")
        plan = self.read("ProjectVolumeExportPlan.kt")
        self.assertIn("ProjectVolumeExportPlan.select(fresh, volumeIds)", home)
        self.assertIn("ProjectVolumeExportPlan.select(plan, volumeIds)", main)
        self.assertIn(
            "ProjectFolderExportService.start(this, projectId, folder, selected.map { it.volumeId })",
            main,
        )
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
