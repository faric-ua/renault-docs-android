"""v0.5.147 — owner 2026-10-17 05:02: the 18 old unpacked Megane II volumes on the M30s card
are not archived on the phone; they are deleted and imported again from A26 as one file each.

Until now the app never deleted volume files. Contract:
- deleting happens only from «Видалити томи разом з файлами…» in the project menu, after a
  list with ticks AND a separate «Видалити назавжди»;
- files first (rename into `rdpkg-trash` of the same place), the record only after that;
- the background eraser works only inside the trash folders and resumes on app start;
- the old rules stay: «Видалити з проєкту» / «Видалити проєкт» still never touch files, and
  VolumeStorage / VolumeArchiveAccess still delete nothing.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class DeleteVolumeFilesContract(unittest.TestCase):
    def test_menu_list_and_confirmation(self):
        dialogs = source("HomeProjectDialogController.kt")
        self.assertIn('"Видалити томи разом з файлами…"', dialogs)
        self.assertIn("VolumeDeleteChoice.defaultSelection(items)", dialogs)
        self.assertIn('"Видалити назавжди"', dialogs)
        self.assertIn("DialogRole.DANGER", dialogs)
        # The actual deleting is reached only from the confirmation button.
        confirm = dialogs.split("private fun showDeleteFilesConfirm")[1].split("private fun deleteVolumes")[0]
        self.assertIn("deleteVolumes(project, selected)", confirm)
        chooser = dialogs.split("private fun showDeleteFilesChooser")[1].split("private fun showDeleteFilesConfirm")[0]
        self.assertNotIn("deleteVolumes(", chooser)
        self.assertNotIn("moveVolumeToTrash", chooser)

    def test_files_first_then_record(self):
        dialogs = source("HomeProjectDialogController.kt")
        body = dialogs.split("private fun deleteVolumes")[1].split("/** v0.5.106")[0]
        self.assertIn("VolumeTrash.deleteVolumesInBackground(", body)
        trash = source("VolumeTrash.kt")
        work = trash.split("fun deleteVolumesInBackground")[1].split("fun startErase")[0]
        trash_at = work.index("moveVolumeToTrash(appContext")
        record_at = work.index("store.removeVolume", trash_at)
        self.assertLess(trash_at, record_at)
        self.assertIn("startErase(appContext)", work)
        self.assertIn("Thread(", work, "renames on the card run off the main thread")

    def test_eraser_stays_in_the_trash(self):
        core = source("VolumeTrashCore.kt")
        self.assertIn('const val TRASH_DIR = "rdpkg-trash"', core)
        self.assertIn("Files.isSymbolicLink", core)
        self.assertNotIn("deleteRecursively", core)
        trash = source("VolumeTrash.kt")
        self.assertIn("VolumeTrashCore.moveIn(item, trash, stamp)", trash)
        self.assertIn('!packageId.contains(\'/\')', trash)
        self.assertNotIn(".delete()", trash, "VolumeTrash only renames; erasing is in the core")
        self.assertNotIn("Thread {", source("HomeProjectDialogController.kt"))
        # v0.5.148: resumed when the home screen opens (a foreground service cannot be started
        # from Application.onCreate in the background).
        self.assertIn("VolumeTrash.startErase(this)", source("MainActivity.kt"))

    def test_old_rules_unchanged(self):
        project = source("ProjectActivity.kt")
        self.assertIn("Файли на телефоні залишаться без змін.", project)
        self.assertNotIn("VolumeTrash", project)
        store = source("ProjectStore.kt")
        self.assertNotIn("VolumeTrash", store)
        for name in ("VolumeStorage.kt", "VolumeArchiveAccess.kt"):
            text = source(name)
            self.assertNotIn("deleteRecursively", text, name)
            self.assertNotIn("VolumeTrash", text, name)

    def test_home_shows_the_result(self):
        main = source("MainActivity.kt")
        self.assertIn("onVolumesDeleted = { _, message ->", main)

    def test_jvm_test_present(self):
        test = ROOT / "android/app/src/test/java/com/saney/renaultdocs/VolumeTrashTest.kt"
        text = test.read_text(encoding="utf-8")
        self.assertIn("eraseAllEmptiesOnlyTheTrash", text)
        self.assertIn("eraseDoesNotFollowSymbolicLinks", text)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 163)


if __name__ == "__main__":
    unittest.main()
