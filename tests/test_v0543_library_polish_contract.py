from pathlib import Path
import unittest


class V0543LibraryPolishContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_home_is_project_first_and_compact(self):
        main = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )

        self.assertIn('"Додати том"', main)
        self.assertIn('"Новий проєкт"', main)
        self.assertIn('"Мої Renault"', main)
        self.assertIn('"Інструменти"', main)
        self.assertIn('"Конвертер"', main)
        self.assertIn('"Legacy"', main)
        self.assertIn("buildHomeActionCard(", main)
        self.assertIn("buildToolCard(", main)
        self.assertIn('"Порожній · додай том"', main)

    def test_project_volume_cards_do_not_repeat_document_code_and_date(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("volume.documentCode", activity)
        self.assertIn("val primaryTitle", activity)
        self.assertIn("volume.date", activity)
        self.assertIn('"Вручну"', activity)
        self.assertIn("openVolumePicker(", activity)

    def test_legacy_single_volume_metadata_is_repaired(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn("PreparedVolumeReader", store)
        self.assertIn("Uri.parse(", store)
        self.assertIn("pair.second.treeUri ==", store)
        self.assertIn("volume.treeUri", store)
        self.assertLess(
            store.index('"megane-ii"'),
            store.index('"laguna-ii"'),
        )
        self.assertLess(
            store.index('"laguna-ii"'),
            store.index('"kangoo-ii"'),
        )

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
