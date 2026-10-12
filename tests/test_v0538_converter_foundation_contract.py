from pathlib import Path
import subprocess
import unittest

# v0.5.107: converter wording translated to Ukrainian (owner request); checks unchanged.


RETIRED_V05112 = "v0.5.112: «Конвертер»/«Legacy»/«Старі бібліотеки» removed by owner decision 2026-10-12; code kept in branch archive/legacy-converter-v0.5.111"

class V0538ConverterFoundationContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    @unittest.skip(RETIRED_V05112)
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

    @unittest.skip(RETIRED_V05112)
    def test_existing_output_gets_nomedia_when_registered(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        # v0.5.108 (owner): the *_android copy is no longer added to the
        # library, so the manual «Додати готову папку» path is gone. The
        # recovered copy still gets .nomedia and is checked in place.
        block = activity.split(
            "val recovered =",
            1,
        )[1].split("runOnUiThread", 1)[0]

        self.assertIn("DatasetMediaIsolation.ensure(", block)
        self.assertIn("DatasetReader.readFolder(", block)

    @unittest.skip(RETIRED_V05112)
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

    @unittest.skip(RETIRED_V05112)
    def test_documentfile_scanner_is_only_compatibility_fallback(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("scanSourceFallback(", engine)
        self.assertIn(
            "Швидке сканування недоступне · сканую повільнішим способом",
            engine,
        )

    @unittest.skip(RETIRED_V05112)
    def test_scan_progress_does_not_render_as_zero_over_found_count(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn(
            "state.phase == ConversionRunPhase.SCANNING",
            activity,
        )
        self.assertIn('"Сканую…"', activity)
        shared = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SharedOperationProgressBar.kt"
        )
        self.assertIn("state.phase == ConversionRunPhase.SCANNING", activity)
        self.assertIn("SharedOperationProgressBar.render(progressBar, null, null)", activity)
        self.assertIn('itemLabel = "Знайдено файлів"', activity)
        self.assertIn("bar.isIndeterminate = false", shared)

    @unittest.skip(RETIRED_V05112)
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
        # v0.5.110: SafDisplayPath also shows a folder inside a tree (converter copy).
        self.assertIn("rootDocumentIdOf(", formatter)
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
