from pathlib import Path
import unittest


class V0562DriveCatalogContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]

    def read(self, path: str) -> str:
        return (self.repo / path).read_text(encoding="utf-8")

    def test_release_version(self):
        gradle = self.read("android/app/build.gradle.kts")
        self.assertIn('versionName = "0.5.62"', gradle)
        self.assertIn("versionCode = 78", gradle)

    def test_catalog_manifest_endpoint_is_centralized(self):
        links = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ExternalLinks.kt"
        )
        self.assertIn("PROJECT_CATALOG_MANIFEST_FILE_ID", links)
        self.assertIn("PROJECT_CATALOG_MANIFEST_URL", links)
        self.assertIn("driveDownloadUrl(", links)
        self.assertIn("drive.usercontent.google.com", links)

    def test_catalog_has_strict_schema_and_volume_identity(self):
        catalog = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/DriveCatalog.kt"
        )
        self.assertIn("SCHEMA_VERSION = 1", catalog)
        self.assertIn("data class DriveCatalogProject", catalog)
        self.assertIn("data class DriveCatalogVolume", catalog)
        for field in (
            "documentCode",
            "date",
            "documentType",
            "documentVersion",
            "region",
            "driveFileId",
            "sizeBytes",
            "sha256",
        ):
            self.assertIn(field, catalog)

    def test_home_opens_app_catalog_not_raw_drive_folder(self):
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        block = main.split("private fun buildProjectCatalogCard()", 1)[1]
        block = block.split("private fun buildHomeAddPanel()", 1)[0]
        self.assertIn("DriveCatalogActivity::class.java", block)
        self.assertNotIn("ExternalLinks.openWeb(", block)

    def test_catalog_ui_groups_projects_and_marks_installed_volumes(self):
        activity = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/DriveCatalogActivity.kt"
        )
        self.assertIn('"Каталог Renault Docs"', activity)
        self.assertIn('"Імпортувати вибране"', activity)
        self.assertIn("projectCard(", activity)
        self.assertIn("volumeRow(", activity)
        self.assertIn('"✓ Встановлено"', activity)
        self.assertIn("selectedIds", activity)
        self.assertIn("documentYearFrom", activity)
        self.assertIn("vehicleCodes", activity)

    def test_catalog_import_uses_existing_rdpkg_validator_and_project_store(self):
        service = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/CatalogImportService.kt"
        )
        self.assertIn("DriveCatalogClient", service)
        self.assertIn("RdpkgImporter", service)
        self.assertIn(".install(", service)
        self.assertIn("ProjectStore(", service)
        self.assertIn("store.upsertVolume(", service)
        self.assertIn("FileProvider", service)

    def test_catalog_import_is_foreground_and_persistent(self):
        manifest = self.read("android/app/src/main/AndroidManifest.xml")
        store = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/CatalogImportRunStore.kt"
        )
        self.assertIn('android:name=".CatalogImportService"', manifest)
        self.assertIn("foregroundServiceType=\"dataSync\"", manifest)
        self.assertIn("CatalogImportPhase.DOWNLOADING", store)
        self.assertIn("CatalogImportPhase.IMPORTING", store)
        self.assertIn("KEY_ITEMS", store)
        self.assertIn("KEY_ITEM_INDEX", store)

    def test_network_and_fileprovider_contracts_are_present(self):
        manifest = self.read("android/app/src/main/AndroidManifest.xml")
        paths = self.read("android/app/src/main/res/xml/file_paths.xml")
        self.assertIn("android.permission.INTERNET", manifest)
        self.assertIn('name="catalog_downloads"', paths)
        self.assertIn('path="catalog-downloads/"', paths)


if __name__ == "__main__":
    unittest.main()
