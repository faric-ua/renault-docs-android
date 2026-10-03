from pathlib import Path
import unittest


class V0549RdpkgContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_builder_creates_one_volume_portable_package(self):
        builder = self._read("core/rdpkg.py")

        self.assertIn(
            'RDPKG_FORMAT = "renault-volume-package-v1"',
            builder,
        )
        self.assertIn("select_volume(", builder)
        self.assertIn("shutil.copytree(", builder)
        self.assertIn("build_dataset_package(", builder)
        self.assertIn('"rdpkg.json"', builder)
        self.assertIn('RDPKG_SUFFIX = ".rdpkg"', builder)

    def test_android_imports_package_into_managed_storage(self):
        importer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgImporter.kt"
        )

        self.assertIn("ZipInputStream(", importer)
        self.assertIn('"renault-volume-package-v1"', importer)
        self.assertIn("LocalDatasetDocumentsProvider", importer)
        self.assertIn("PreparedVolumeReader", importer)
        self.assertIn("staging.renameTo(", importer)
        self.assertIn("backupDirectory", importer)

    def test_local_documents_provider_keeps_existing_saf_runtime(self):
        provider = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/LocalDatasetDocumentsProvider.kt"
        )
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )

        self.assertIn("DocumentsProvider()", provider)
        self.assertIn("buildTreeDocumentUri(", provider)
        self.assertIn("openDocument(", provider)
        self.assertIn("queryChildDocuments(", provider)
        self.assertIn("context.noBackupFilesDir", provider)
        self.assertIn(
            'android:name=".LocalDatasetDocumentsProvider"',
            manifest,
        )
        self.assertIn(
            'android:authorities="${applicationId}.datasets"',
            manifest,
        )
        self.assertIn(
            'android:permission="android.permission.MANAGE_DOCUMENTS"',
            manifest,
        )
        self.assertIn(
            'android:grantUriPermissions="true"',
            manifest,
        )
        self.assertIn(
            'android.content.action.DOCUMENTS_PROVIDER',
            manifest,
        )

    def test_rdpkg_is_primary_but_folder_import_remains_available(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn('"Авто"', activity)
        self.assertIn("openPackagePicker()", activity)
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT,", activity)
        self.assertIn("RdpkgImporter.install(", activity)
        self.assertIn('"Вручну"', activity)
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT_TREE,", activity)
        self.assertIn("PreparedVolumeReader.readAll(", activity)

    def test_termux_menu_can_build_rdpkg(self):
        menu = self._read("menu.sh")
        launcher = self._read(
            "tools/termux/reno-build-rdpkg.sh"
        )
        builder = self._read(
            "tools/build_rdpkg.py"
        )

        self.assertIn(
            "15 — Створити single-volume .rdpkg",
            menu,
        )
        self.assertIn("build_rdpkg", menu)
        self.assertIn(
            "tools/build_rdpkg.py",
            launcher,
        )
        self.assertIn(
            "/storage/emulated/0/Documents/Renault",
            launcher,
        )
        self.assertIn("Вибери dataset номером:", launcher)
        self.assertIn("dataset_paths=()", launcher)
        self.assertIn("volume_lines=()", launcher)
        self.assertIn("choose_number", launcher)
        self.assertIn("0 — Назад", launcher)
        self.assertIn("Enter — створити .rdpkg; 0 — назад:", launcher)
        self.assertIn("A — Усі томи окремими .rdpkg", launcher)
        self.assertIn("args+=(--all)", launcher)
        self.assertIn('"--all"', builder)
        self.assertIn('"READY · RDPKG BATCH"', builder)
        self.assertIn('"package_count": len(results)', builder)
        self.assertIn('"sha256": result["sha256"]', builder)
        self.assertNotIn("Prepared dataset folder:", launcher)
        self.assertNotIn("Том (наприклад", launcher)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 67)
        self.assertGreaterEqual(version_name, (0, 5, 51))


if __name__ == "__main__":
    unittest.main()
