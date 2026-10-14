"""v0.5.132: import of a whole project from one .rdproject file.

Owner (2026-10-14): open the file from Telegram / Drive / a file manager or via
«Додати» → show the project and the volumes already on the phone → after «так»
create the project and install volumes, skip those present, overwrite nothing.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"


def source(name):
    return (APP / name).read_text(encoding="utf-8")


class RdprojectImportContract(unittest.TestCase):
    def test_format_matches_the_exporter(self):
        exporter = source("RdprojectExporter.kt")
        parser = source("RdprojectManifest.kt")
        fmt = re.search(r'const val FORMAT = "([^"]+)"', exporter).group(1)
        self.assertIn(f'const val FORMAT = "{fmt}"', parser)
        self.assertIn('const val MANIFEST_NAME = "rdproject.json"', parser)
        self.assertIn('File(tempRoot, "rdproject.json")', exporter)
        # rdproject.json is the first ZIP entry only while entries are sorted by path.
        self.assertIn(".sortedBy {", exporter)
        self.assertLess("rdproject.json", "volumes/")

    def test_paths_from_the_file_never_decide_where_to_write(self):
        parser = source("RdprojectManifest.kt")
        self.assertIn("fun safeVolumePath", parser)
        self.assertIn('val target = File(workDir, "volume-" + (index + 1) + ".rdpkg")', parser)
        self.assertIn("target.delete()", parser)

    def test_existing_packages_are_never_replaced(self):
        importer = source("RdpkgImporter.kt")
        self.assertIn("refuseExistingPackage: Boolean = false", importer)
        self.assertIn("throw PackageAlreadyInstalledException(", importer)
        guard = importer.index("refuseExistingPackage &&")
        rename = importer.index("finalDirectory.renameTo(")
        self.assertLess(guard, rename)
        service = source("RdprojectImportService.kt")
        self.assertIn("refuseExistingPackage = true", service)
        self.assertIn("is PackageAlreadyInstalledException", service)
        # Re-checked right before each install (a repeated run skips what was added).
        self.assertIn("RdprojectImportPlanner.isSameVolume(volume, it)", service)

    def test_preview_writes_nothing_and_project_is_created_lazily(self):
        activity = source("RdprojectImportActivity.kt")
        support = source("RdprojectImportSupport.kt")
        self.assertNotIn("createProject", activity)
        self.assertNotIn("createProject", support)
        service = source("RdprojectImportService.kt")
        install = service.index("RdpkgImporter.install(")
        create = service.index("store.createProject(")
        ensure_call = service.index("val project = ensureProject(store, manifest)")
        self.assertLess(install, ensure_call)
        self.assertGreater(create, ensure_call)

    def test_service_only_deletes_its_own_work_folder(self):
        service = source("RdprojectImportService.kt")
        deletes = re.findall(r"(\w+)\.(?:deleteRecursively|delete)\(\)", service)
        self.assertEqual(set(deletes), {"workDir"})
        self.assertIn('private const val WORK_DIR = "rdproject-import"', service)
        self.assertIn("File(cacheDir, WORK_DIR)", service)

    def test_service_is_a_background_operation_like_the_others(self):
        service = source("RdprojectImportService.kt")
        for marker in (
            "override fun onTimeout(",
            "DataSyncTimeoutUi.MESSAGE",
            "BackgroundWorkWakeLock(this,",
            "runStore.isCancelRequested()",
            "OperationAlert.alertIfFinal",
            "FLAG_GRANT_READ_URI_PERMISSION",
        ):
            self.assertIn(marker, service)
        manifest = MANIFEST.read_text(encoding="utf-8")
        self.assertRegex(
            manifest,
            r'android:name="\.RdprojectImportService"\s+android:exported="false"\s+'
            r'android:foregroundServiceType="dataSync"',
        )

    def test_opens_from_other_apps_and_from_home(self):
        manifest = MANIFEST.read_text(encoding="utf-8")
        block = manifest.split('android:name=".RdprojectImportActivity"', 1)[1].split("</activity>", 1)[0]
        self.assertIn('android:exported="true"', block)
        for action in ("android.intent.action.VIEW", "android.intent.action.SEND"):
            self.assertIn(action, block)
        for mime in ("application/octet-stream", "application/zip"):
            self.assertIn(f'android:mimeType="{mime}"', block)
        self.assertNotIn('android:scheme="file"', block)
        main = source("MainActivity.kt")
        self.assertIn('"Проєкт із файлу"', main)
        self.assertIn("RdprojectImportActivity.pickIntent(this@MainActivity)", main)

    def test_screen_survives_rotation_and_asks_before_stopping(self):
        activity = source("RdprojectImportActivity.kt")
        self.assertIn("onRetainNonConfigurationInstance", activity)
        self.assertIn("confirmDialogs.restoreOpen()", activity)
        self.assertIn("ConfirmActions.confirmStop(", activity)
        self.assertIn('"Імпортувати $toImport', activity)


if __name__ == "__main__":
    unittest.main()
