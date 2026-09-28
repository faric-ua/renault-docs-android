from pathlib import Path
import unittest


class V0550NativeRdpkgExportContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_export_is_android_native_and_reuses_managed_package(self):
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgExporter.kt"
        )

        self.assertIn("ZipOutputStream(", exporter)
        self.assertIn("MessageDigest.getInstance(", exporter)
        self.assertIn('"SHA-256"', exporter)
        self.assertIn("packageIdFromTreeUri(", exporter)
        self.assertIn("packageDirectory(", exporter)
        self.assertIn('"rdpkg.json"', exporter)
        self.assertIn('"renault-dataset.json"', exporter)
        self.assertNotIn("python", exporter.lower())
        self.assertNotIn("termux", exporter.lower())

    def test_export_uses_android_create_document(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn('"Експортувати .rdpkg"', activity)
        self.assertIn("Intent.ACTION_CREATE_DOCUMENT", activity)
        self.assertIn("defaultFileName(", activity)
        self.assertIn("RdpkgExporter.export(", activity)
        self.assertIn("REQUEST_RDPKG_EXPORT", activity)
        self.assertIn("STATE_PENDING_RDPKG_EXPORT_VOLUME_ID", activity)
        self.assertIn("override fun onSaveInstanceState", activity)

    def test_managed_provider_can_resolve_package_identity(self):
        provider = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/LocalDatasetDocumentsProvider.kt"
        )

        self.assertIn("fun packageIdFromTreeUri(", provider)
        self.assertIn("DocumentsContract", provider)
        self.assertIn(".getTreeDocumentId(", provider)

    def test_architecture_records_fast_export_and_future_converter(self):
        doc = self._read(
            "docs/architecture/RDPKG_DISTRIBUTION_AND_ANDROID_EXPORT.md"
        )

        self.assertIn("Android-native fast export", doc)
        self.assertIn("Python/Termux remain developer/reference tools", doc)
        self.assertIn("Future Kotlin-native converter", doc)
        self.assertIn("ACTION_CREATE_DOCUMENT", doc)
        self.assertIn("ZipOutputStream", doc)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
