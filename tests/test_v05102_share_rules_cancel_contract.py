"""v0.5.102: one prepared file per volume/project, ⇧ after folder export,
cancel for long Home operations, status kept expanded on rotation.

Phone QA v0.5.101 (faric): status collapsed on rotation (#159), no ⇧ after
exporting 3 volumes to a folder (#182), no «Скасувати» while packing a
22-volume .rdproject (#181). Owner rule: only one prepared file per volume
and per project; a new one only after deleting the old one.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class ShareRulesCancelContract(unittest.TestCase):
    def read(self, name):
        return (ANDROID / name).read_text(encoding="utf-8")

    def test_prepared_files_are_committed_from_temporary_files(self):
        store = self.read("PreparedShareStore.kt")
        self.assertIn('INCOMING = ".incoming"', store)
        self.assertIn("fun incomingFile(", store)
        self.assertIn("fun commitIncoming(", store)
        self.assertIn("require(!finalFile.exists())", store)
        self.assertIn('it.name.endsWith(".partial")', store)
        for name in ("RdpkgShareService.kt", "RdprojectShareService.kt", "ProjectFolderExportService.kt"):
            source = self.read(name)
            self.assertIn("PreparedShareStore.incomingFile(", source, name)
            self.assertIn("PreparedShareStore.commitIncoming(", source, name)

    def test_one_prepared_file_per_volume_and_project(self):
        volume_service = self.read("RdpkgShareService.kt")
        self.assertIn("PreparedShareStore.existingVolumeFile(applicationContext, project, volume)", volume_service)
        project_service = self.read("RdprojectShareService.kt")
        self.assertIn("PreparedShareStore.existingProjectFile(applicationContext, project, volumes) == null", project_service)
        project = self.read("ProjectActivity.kt")
        menu = project.split("private fun showVolumeActions(", 1)[1].split("private fun showMoveVolumeDialog(", 1)[0]
        self.assertIn('"Підготувати том · уже підготовлено ⇧"', menu)
        self.assertIn("enabled = false", menu)
        home = self.read("HomeProjectDialogController.kt")
        self.assertIn('"Перепакувати .rdproject · спершу видали підготовлений ⇧"', home)

    def test_folder_export_copies_the_prepared_volume(self):
        service = self.read("ProjectFolderExportService.kt")
        self.assertIn("PreparedShareStore.existingVolumeFile(applicationContext, project, volume)", service)
        self.assertIn('record.put("prepared", "REUSED")', service)
        self.assertIn('record.put("prepared", "CREATED")', service)
        self.assertIn("copyToUri(prepared, created.uri)", service)
        self.assertIn("sha.equals(localSha, ignoreCase = true)", service)
        self.assertIn("EXISTING_UNVERIFIED", service)
        self.assertNotIn("PreparedShareStore.deleteProject", service)

    def test_cancel_is_persisted_and_checked_between_files(self):
        folder_store = self.read("ProjectFolderExportRunStore.kt")
        project_store = self.read("RdprojectShareRunStore.kt")
        for store in (folder_store, project_store):
            self.assertIn("fun requestCancel()", store)
            self.assertIn("fun isCancelRequested()", store)
            self.assertIn("CANCELLED", store)
        folder = self.read("ProjectFolderExportService.kt")
        self.assertIn("if (runStore.isCancelRequested()) throw OperationCancelledException()", folder)
        self.assertIn("runStore.cancelled(done, skip,", folder)
        self.assertIn("runCatching { created.delete() }", folder)
        project = self.read("RdprojectShareService.kt")
        self.assertIn("throw OperationCancelledException()", project)
        self.assertIn("runStore.cancelled(", project)
        self.assertIn("temp?.let { runCatching { it.delete() } }", project)

    def test_home_offers_confirmed_cancel_and_shows_cancelled(self):
        main = self.read("MainActivity.kt")
        self.assertEqual(2, main.count("projectDialogs.confirmCancelOperation("))
        self.assertIn("folderExportRunStore.requestCancel()", main)
        self.assertIn("rdprojectShareRunStore.requestCancel()", main)
        self.assertIn('"Окремі томи · скасовано"', main)
        self.assertIn('"Підготовка проєкту · скасовано"', main)
        home = self.read("HomeProjectDialogController.kt")
        confirm = home.split("fun confirmCancelOperation(", 1)[1].split("private fun track(", 1)[0]
        # v0.5.104: the same question now lives in the shared ConfirmActions.
        self.assertIn("ConfirmActions.confirmStop(", confirm)
        shared = self.read("ConfirmActions.kt")
        self.assertIn('KEEP_RUNNING_LABEL = "Продовжити"', shared)
        self.assertIn(".setNegativeButton(keepLabel, null)", shared)
        self.assertIn("DialogUi.apply(dialog, DialogRole.DANGER)", shared)

    def test_home_status_expansion_survives_rotation(self):
        main = self.read("MainActivity.kt")
        self.assertIn("useProjectCompactLayout(restoredStatusDetailsExpanded)", main)
        self.assertIn("operationStatus.isProjectDetailsExpanded()", main)
        self.assertIn("getBoolean(STATE_STATUS_DETAILS_EXPANDED, false)", main)


if __name__ == "__main__":
    unittest.main()
