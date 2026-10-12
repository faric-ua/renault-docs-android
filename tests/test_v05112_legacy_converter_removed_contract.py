"""v0.5.112 — «Конвертер», «Legacy», «Інструменти» and «Старі бібліотеки» removed.

Owner decision (faric, 2026-10-12): the old-style copy (…_android) did not open
in a phone browser without the Termux server, and inside the app it is faster
to turn an old Renault folder into a new volume («Створити .rdpkg з raw» /
«… з архіву»). So the converter and everything around it was removed.

History is kept: branch archive/legacy-converter-v0.5.111 holds the full code,
docs/ua/CONVERTER.md tells the story, older contract tests are skipped with
RETIRED_V05112 instead of being deleted.

Data safety: nothing on the phone is deleted. Saved old-library records stay
in their preferences; the one-time migration of old single-volume datasets
into projects still runs.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"
REMOVED_FILES = (
    "ConversionActivity.kt",
    "ConversionService.kt",
    "ConversionDraftStore.kt",
    "ConversionPlan.kt",
    "ConversionRunStore.kt",
    "SafConversionEngine.kt",
    "AndroidDatasetPackageWriter.kt",
)


class LegacyConverterRemovedContract(unittest.TestCase):
    def test_converter_code_and_manifest_entries_are_gone(self):
        for name in REMOVED_FILES:
            with self.subTest(file=name):
                self.assertFalse((SRC / name).exists())
        manifest = MANIFEST.read_text(encoding="utf-8")
        self.assertNotIn('android:name=".ConversionActivity"', manifest)
        self.assertNotIn('android:name=".ConversionService"', manifest)

    def test_home_has_no_tools_legacy_or_old_libraries(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        for gone in (
            '"Інструменти"', '"Конвертер"', '"Legacy"', '"Старі бібліотеки"',
            "buildToolsPanel(", "buildToolCard(", "buildDatasetTile(",
            "openLegacyDataset(", "openDatasetPicker(", "REQUEST_DATASET_FOLDER =",
            "ConversionActivity",
        ):
            with self.subTest(gone=gone):
                self.assertNotIn(gone, main)

    def test_no_visible_text_points_to_removed_buttons(self):
        literal = re.compile(r'"([^"\n]*)"')
        for path in SRC.glob("*.kt"):
            text = path.read_text(encoding="utf-8")
            for value in literal.findall(text):
                with self.subTest(file=path.name, text=value[:60]):
                    self.assertNotIn("через Legacy", value)
                    self.assertNotIn("«Legacy»", value)
                    self.assertNotIn("«Конвертер»", value)
                    self.assertNotIn("Старі бібліотеки", value)

    def test_help_points_to_new_volume_from_old_folder(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("«Створити .rdpkg з raw»", main)
        project = (SRC / "ProjectActivity.kt").read_text(encoding="utf-8")
        self.assertIn('"Створити .rdpkg з raw"', project)

    def test_saved_data_is_not_touched(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        # The one-time migration still reads the old records; nothing wipes them.
        self.assertIn("projectStore.migrateLegacySingleVolumeDatasets(\n            store.load(),", main)
        for wipe in ("store.clear(", "store.remove(", "store.delete("):
            self.assertNotIn(wipe, main)

    def test_shared_cancel_exception_survived(self):
        exception = (SRC / "ConversionCancelledException.kt").read_text(encoding="utf-8")
        self.assertIn("class ConversionCancelledException", exception)
        self.assertIn('RuntimeException("Конвертацію скасовано.")', exception)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 128)


if __name__ == "__main__":
    unittest.main()
