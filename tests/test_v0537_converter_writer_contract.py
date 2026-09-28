from pathlib import Path
import unittest


class V0537ConverterWriterContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_converter_runs_as_foreground_data_sync_service(self):
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )

        self.assertIn("android.permission.FOREGROUND_SERVICE", manifest)
        self.assertIn("android.permission.FOREGROUND_SERVICE_DATA_SYNC", manifest)
        self.assertIn('android:name=".ConversionService"', manifest)
        self.assertIn('android:foregroundServiceType="dataSync"', manifest)
        self.assertIn("startForeground(", service)
        self.assertIn("context.startForegroundService(", service)

    def test_writer_uses_staging_and_only_renames_after_validation(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        staging = engine.index(".renault-staging")
        validate = engine.index("validateOutput(")
        rename = engine.index("staging.renameTo(")

        self.assertLess(staging, validate)
        self.assertLess(validate, rename)
        self.assertIn("if (!finalized)", engine)
        self.assertIn("staging.delete()", engine)

    def test_source_is_read_only_and_never_deleted(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        source_picker = activity.split(
            "REQUEST_SOURCE_FOLDER",
            1,
        )[1].split(
            "REQUEST_DESTINATION_FOLDER",
            1,
        )[0]

        self.assertNotIn("FLAG_GRANT_WRITE_URI_PERMISSION", source_picker)
        self.assertNotIn("source.delete()", engine)
        self.assertIn("Оригінальна source-папка залишена без змін.", activity)

    def test_path_normalizer_is_used_while_copying_text_files(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("ConverterPathNormalizer", engine)
        self.assertIn("patchText(", engine)
        self.assertIn("Charsets.ISO_8859_1", engine)
        self.assertIn('"htm"', engine)
        self.assertIn('"html"', engine)
        self.assertIn('"js"', engine)

    def test_base_package_is_generated_and_validated(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )
        package = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidDatasetPackageWriter.kt"
        )

        self.assertIn("AndroidDatasetPackageWriter.write(", engine)
        self.assertIn('"renault-dataset.json"', package)
        self.assertIn('"modern-index.json"', package)
        self.assertIn('"volumes.json"', package)
        self.assertIn('"START.html"', package)
        self.assertIn('"modern_runtime_compiled"', package)
        self.assertIn("false", package)

    def test_cancel_and_rotation_reconnect_use_persistent_run_state(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionRunStore.kt"
        )

        self.assertIn("ConversionRunStore", activity)
        self.assertIn("REFRESH_INTERVAL_MS", activity)
        self.assertIn("requestCancel(", activity)
        self.assertIn("SharedPreferences", store)
        self.assertIn("cancelRequested", store)

    def test_output_can_be_registered_in_library(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn("DatasetReader.read(", activity)
        self.assertIn("datasetStore.upsert(", activity)
        self.assertIn("outputTreeUri", activity)

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
