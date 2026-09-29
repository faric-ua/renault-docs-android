from pathlib import Path
import unittest


class V0542ProjectVolumeLibraryContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_default_projects_are_seeded(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn('id = "megane-ii"', store)
        self.assertIn('title = "Megane II"', store)
        self.assertIn('id = "laguna-ii"', store)
        self.assertIn('title = "Laguna II"', store)
        self.assertIn('id = "kangoo-ii"', store)
        self.assertIn('title = "Kangoo II"', store)

    def test_main_library_uses_projects_and_targeted_volume_add(self):
        main = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )

        self.assertIn('"Додати том"', main)
        self.assertIn('"Новий проєкт"', main)
        chooser = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectChooserActivity.kt"
        )

        self.assertIn('"До якого проєкту додати том?"', chooser)
        self.assertIn("ProjectActivity.intent(", chooser)
        self.assertIn('"Мої Renault"', main)
        self.assertIn("ProjectChooserActivity::class.java", main)
        self.assertIn("CreateProjectActivity::class.java", main)

    def test_project_screen_supports_prepared_and_manual_volume_add(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn('"Додати том"', activity)
        self.assertIn('"Ручне додавання"', activity)
        self.assertIn("PreparedVolumeReader.readAll(", activity)
        self.assertIn('"Інший проєкт"', activity)
        self.assertIn('"Додати сюди"', activity)
        self.assertIn("store.upsertVolume(", activity)

    def test_prepared_volume_requires_one_volume_and_has_project_hint(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/PreparedVolumeReader.kt"
        )

        self.assertIn("fun readAll(", reader)
        self.assertIn("shouldIgnoreSyntheticVolume(", reader)
        self.assertIn('"project_id"', reader)
        self.assertIn("ProjectStore.slugify(", reader)
        self.assertIn('"document_code"', reader)

    def test_fast_preparation_can_stamp_project_id(self):
        converter = self._read("tools/fast_convert_dataset.py")
        launcher = self._read("tools/termux/reno-fast-convert.sh")
        manifest = self._read("core/dataset_manifest.py")

        self.assertIn('"--project-id"', converter)
        self.assertIn('"project_id": project_id or slugify(model_value)', converter)
        self.assertIn('"project_id": dataset.get("project_id")', manifest)
        self.assertIn('project_id="megane-ii"', launcher)
        self.assertIn('project_id="laguna-ii"', launcher)
        self.assertIn('project_id="kangoo-ii"', launcher)
        self.assertIn('--project-id "$project_id"', launcher)

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
