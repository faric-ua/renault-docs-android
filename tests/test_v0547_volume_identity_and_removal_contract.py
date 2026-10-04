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

        parser = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RenaultVolumeIdentity.kt"
        )
        self.assertIn("RenaultVolumeIdentity.parse(", store)
        self.assertIn("ntRegex", parser)
        self.assertIn("dateRegex", parser)
        self.assertNotIn(r'"\\bNT[0-9A-Z]+\\b"', parser)

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
