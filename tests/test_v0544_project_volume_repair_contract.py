from pathlib import Path
import unittest


class V0544ProjectVolumeRepairContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_saved_project_volumes_can_repair_incomplete_metadata(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn("repairIncompleteVolumeMetadata(", store)
        self.assertIn("PreparedVolumeReader", store)
        self.assertIn("volume.documentCode", store)
        self.assertIn("volume.date", store)
        self.assertIn("prepared.copy(", store)

    def test_project_volume_opens_directly_in_modern_mode(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("ModernVolumeActivity", activity)
        self.assertIn("intentForVolume(", activity)
        self.assertIn("volume.entrypoint", activity)
        self.assertNotIn(
            "ModernDatasetActivity\n                        .intent(",
            activity,
        )

    def test_backup_is_not_a_real_volume(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernCatalogReader.kt"
        )
        writer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidDatasetPackageWriter.kt"
        )

        self.assertIn('"backup"', reader)
        self.assertIn("shouldIgnoreSyntheticVolume", reader)
        self.assertIn('"backup"', writer)
        self.assertIn('"packages"', writer)
        self.assertIn('"_renault"', writer)

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
