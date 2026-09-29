from pathlib import Path
import subprocess
import unittest


class V0538ConverterFoundationContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_converter_output_is_hidden_from_media_scanner(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )
        helper = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/DatasetMediaIsolation.kt"
        )

        self.assertIn("DatasetMediaIsolation.ensure(", engine)
        self.assertIn('".nomedia"', helper)
        self.assertIn("createFile(", helper)
        self.assertIn("application/octet-stream", helper)

    def test_existing_output_gets_nomedia_when_registered(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        block = activity.split(
            "private fun registerConvertedDataset()",
            1,
        )[1]

        self.assertIn("DatasetMediaIsolation.ensure(", block)
        self.assertIn("DatasetReader.read(", block)

    def test_converter_uses_direct_documents_provider_scan(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("scanSourceFast(", engine)
        self.assertIn("buildChildDocumentsUriUsingTree(", engine)
        self.assertIn("COLUMN_DOCUMENT_ID", engine)
        self.assertIn("COLUMN_DISPLAY_NAME", engine)
        self.assertIn("COLUMN_MIME_TYPE", engine)
        self.assertIn("COLUMN_SIZE", engine)
        self.assertIn("contentResolver", engine)
        self.assertIn(".query(", engine)

    def test_documentfile_scanner_is_only_compatibility_fallback(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("scanSourceFallback(", engine)
        self.assertIn(
            "Швидкий scanner недоступний · використовую compatibility scan",
            engine,
        )

    def test_scan_progress_does_not_render_as_zero_over_found_count(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn(
            "state.phase ==\n                ConversionRunPhase.SCANNING",
            activity,
        )
        self.assertIn("Знайдено файлів:", activity)
        self.assertIn("progressBar.isIndeterminate", activity)

    def test_saf_paths_are_user_friendly(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        formatter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafDisplayPath.kt"
        )
        settings = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SettingsActivity.kt"
        )

        self.assertIn("SafDisplayPath", activity)
        self.assertIn("SafDisplayPath.tree(", settings)
        self.assertIn("getTreeDocumentId(", formatter)
        self.assertIn('"primary"', formatter)
        self.assertNotIn(
            'append(\n                uri,',
            activity,
        )

    def test_termux_version_parser_resolves_current_gradle_version(self):
        repo = Path(__file__).resolve().parents[1]
        script = self._read("tools/termux/reno-download-apk.sh")
        command = """awk -F'"' '/^[[:space:]]*versionName[[:space:]]*=/ {print $2; exit}' android/app/build.gradle.kts"""

        result = subprocess.run(
            ["bash", "-lc", command],
            cwd=repo,
            check=True,
            capture_output=True,
            text=True,
        )

        expected_version = (
            self._read("android/app/build.gradle.kts")
            .split('versionName = "', 1)[1]
            .split('"', 1)[0]
        )
        self.assertEqual(expected_version, result.stdout.strip())
        self.assertIn("awk -F'\"'", script)
        self.assertIn("versionName[[:space:]]*=", script)

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

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
