"""v0.5.135 — TODO №1: new volumes in the phone or on the memory card.

Owner 2026-10-14 20:15 («вибрати, куди зберегти — напр. на флешку»), plan agreed
2026-10-15 01:55. A volume is opened by its package id, so only the place where a
package lies changes; nothing is deleted when the card is out.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name):
    return (APP / name).read_text(encoding="utf-8")


class VolumeStorageContract(unittest.TestCase):
    def test_card_folder_needs_no_permission_and_is_removable_only(self):
        storage = source("VolumeStorage.kt")
        self.assertIn("getExternalFilesDirs(null)", storage)
        self.assertIn("Environment.isExternalStorageRemovable(dir)", storage)
        self.assertIn("Environment.MEDIA_MOUNTED", storage)
        self.assertIn("File(context.applicationContext.noBackupFilesDir, DIR)", storage)
        for forbidden in ("deleteRecursively", ".delete()", "MANAGE_EXTERNAL_STORAGE"):
            self.assertNotIn(forbidden, storage)
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", manifest)

    def test_every_open_goes_through_locate(self):
        provider = source("LocalDatasetDocumentsProvider.kt")
        self.assertIn("VolumeStorage.locate(context, packageId)", provider)
        self.assertIn("VolumeStoragePolicy.CARD_MISSING_MESSAGE", provider)
        # Only the provider and the importer know package folders.
        users = [p.name for p in APP.glob("*.kt") if "noBackupFilesDir,\n                    PACKAGE_DIRECTORY" in p.read_text(encoding="utf-8")]
        self.assertEqual(users, [])

    def test_importer_installs_in_the_chosen_place_and_never_crosses_disks(self):
        importer = source("RdpkgImporter.kt")
        self.assertIn("VolumeStorage.chosenPlace(appContext)", importer)
        self.assertIn("VolumeStoragePolicy.CARD_NOT_AVAILABLE_FOR_INSTALL", importer)
        self.assertIn("VolumeStorage.locate(", importer)
        # The old copy is set aside in its own folder (rename stays on one disk).
        self.assertIn("previousDirectory?.parentFile ?: packagesDirectory", importer)
        self.assertIn("previousDirectory.renameTo(", importer)
        self.assertIn("previousDirectory ?: finalDirectory", importer)
        self.assertIn("VolumeStorage.rememberPlace(", importer)
        # refuseExistingPackage (.rdproject import) looks in both places.
        self.assertRegex(importer, r"refuseExistingPackage &&\s+previousDirectory != null")

    def test_settings_row_and_import_screen_choose_the_place(self):
        settings = source("SettingsActivity.kt")
        self.assertIn('"Де зберігати нові томи"', settings)
        self.assertIn("DIALOG_STORAGE ->", settings)
        self.assertIn("VolumeStorage.setChosenPlace(this, place)", settings)
        self.assertIn("VolumeStorage.choiceLine(this)", settings)
        activity = source("RdprojectImportActivity.kt")
        self.assertIn('"Куди: " + VolumeStorage.choiceLine(this)', activity)
        self.assertIn('"Зберегти на карту пам\'яті"', activity)
        self.assertIn('"Зберегти в телефон"', activity)
        support = source("RdprojectImportSupport.kt")
        self.assertIn("VolumeStorage.freeBytes(context, VolumeStorage.chosenPlace(context))", support)

    def test_volume_on_card_is_marked_and_card_out_does_not_open(self):
        project = source("ProjectActivity.kt")
        self.assertIn("volumePlaceNote(volume)", project)
        self.assertIn('"▣ на карті пам\'яті"', project)
        self.assertIn("VolumeState.CARD_MISSING", project)
        self.assertIn("VolumeStoragePolicy.CARD_MISSING_MESSAGE", project)


if __name__ == "__main__":
    unittest.main()
