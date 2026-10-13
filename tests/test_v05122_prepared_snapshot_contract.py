"""v0.5.122 — #173 (HIGH): adding a volume silently deleted the prepared .rdproject.

Owner timeline 2026-10-12: prepared a 21-volume Megane II .rdproject → added the
22nd volume → the prepared file (and its menu actions) vanished.
Cause: ProjectStore.upsertVolume → invalidatePreparedProject →
PreparedShareStore.deleteProject physically deleted it (also on metadata repair,
remove/move volume, remove project).

Fix: a prepared .rdproject is an unchanging snapshot. It is found by the
project id inside its rdproject.json (not by a name built from current
volumes), lookups never rename/delete, only the explicit «Видалити
підготовлений .rdproject» deletes it, and the menu says when it was made for
an older set of volumes.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


class PreparedSnapshotContract(unittest.TestCase):
    def test_project_store_never_deletes_prepared_files(self):
        store = read("ProjectStore.kt")
        code = "\n".join(l for l in store.splitlines() if not l.strip().startswith("//"))
        self.assertNotIn("invalidatePreparedProject", code)
        self.assertNotIn("PreparedShareStore", code)

    def test_lookup_is_by_project_id_and_read_only(self):
        prepared = read("PreparedShareStore.kt")
        lookup = between(prepared, "internal fun projectSnapshotsIn(", "fun projectSnapshot(")
        self.assertIn("info.projectId == project.id", lookup)
        self.assertNotIn("renameTo", lookup)
        self.assertNotIn("delete", lookup)
        manifest = between(prepared, "private fun manifestInfo(", "internal fun parseManifestForTest(")
        self.assertIn('"rdproject.json"', manifest)
        self.assertIn("java.util.zip.ZipFile(file)", manifest)
        self.assertNotIn("delete", manifest)
        existing = between(prepared, "fun existingProjectFile(", "fun hasProjectFile(")
        self.assertNotIn("renameTo", existing)
        self.assertNotIn("fun deleteProject(", prepared)
        self.assertIn("fun deleteProjectSnapshot(", prepared)

    def test_snapshot_knows_when_it_is_older(self):
        prepared = read("PreparedShareStore.kt")
        self.assertIn("data class ProjectSnapshot(", prepared)
        self.assertIn("fun matches(", prepared)
        controller = read("HomeProjectDialogController.kt")
        title = between(controller, "private fun preparedShareTitle(", "private fun volumeCountLabel(")
        self.assertIn("підготовлено раніше, зараз у проєкті", title)
        self.assertIn('"Поділитися проєктом одним файлом (.rdproject) · офлайн"', title)
        actions = between(controller, "fun showActions(projectId: String)", "private fun showExportWizard")
        self.assertIn("PreparedShareStore.projectSnapshot(", actions)
        self.assertIn("Перепакувати .rdproject на ", actions)
        main = read("MainActivity.kt")
        self.assertIn("preparedIsOlder", main)

    def test_delete_is_explicit_and_exact(self):
        controller = read("HomeProjectDialogController.kt")
        delete = between(controller, "private fun confirmDeletePreparedProject(", "private fun sharePreparedProject(")
        self.assertIn('.setTitle("Видалити підготовлений файл?")', delete)
        self.assertIn("PreparedShareStore.deleteProjectSnapshot(", delete)
        remove = between(controller, "private fun showRemoveConfirmation(", ".setNegativeButton(")
        self.assertIn("Підготовлений .rdproject (⇧) цього проєкту теж лишиться", remove)

    def test_new_package_still_needs_old_one_deleted_first(self):
        service = read("RdprojectShareService.kt")
        self.assertIn("PreparedShareStore.existingProjectFile(applicationContext, project, volumes) == null", service)
        prepared = read("PreparedShareStore.kt")
        self.assertIn("require(!finalFile.exists())", prepared)

    def test_jvm_regression_test_exists(self):
        test = (ROOT / "android/app/src/test/java/com/saney/renaultdocs/PreparedProjectSnapshotTest.kt").read_text(encoding="utf-8")
        self.assertIn("snapshotOf21VolumesIsFoundAfterThe22ndVolumeWasAdded", test)
        self.assertIn("unreadableManifestFallsBackToLegacyNameWithoutRenaming", test)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 138)


if __name__ == "__main__":
    unittest.main()
