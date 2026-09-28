from pathlib import Path
import unittest


class V0547VolumeIdentityAndRemovalContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_underscore_delimited_nt_code_and_date_are_supported(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn('"NT[0-9A-Z]+"', store)
        self.assertIn(
            '"(20\\\\d{2})[._-](\\\\d{2})[._-](\\\\d{2})"',
            store,
        )
        self.assertNotIn(r'"\\bNT[0-9A-Z]+\\b"', store)

    def test_legacy_dataset_migration_runs_once(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn("KEY_LEGACY_MIGRATION_DONE", store)
        self.assertIn('"legacy_single_volume_migration_done"', store)

    def test_prepared_metadata_is_normalized_even_when_reader_succeeds(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn(
            "?.let {\n                            inferVolumeMetadata(\n                                it,",
            store,
        )

    def test_volume_can_be_removed_without_file_deletion(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("setOnLongClickListener", activity)
        self.assertIn('"Видалити том з проєкту?"', activity)
        self.assertIn("store.removeVolume(", activity)
        self.assertIn("Файли на телефоні залишаться без змін.", activity)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
