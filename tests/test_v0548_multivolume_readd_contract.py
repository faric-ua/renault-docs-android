from pathlib import Path
import unittest


class V0548MultiVolumeReaddContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_prepared_reader_exposes_real_volumes_and_filters_support_folders(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/PreparedVolumeReader.kt"
        )

        self.assertIn("fun readAll(", reader)
        self.assertIn("shouldIgnoreSyntheticVolume(", reader)
        self.assertIn('"backup"', reader)
        self.assertIn('"_renault"', reader)
        self.assertIn('"packages"', reader)
        self.assertIn('volume.optString(\n                                "entrypoint"', reader)
        self.assertIn("entrypoint =\n                                    volumeEntrypoint", reader)

    def test_single_volume_reader_keeps_backward_compatible_contract(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/PreparedVolumeReader.kt"
        )

        self.assertIn("readAll(", reader)
        self.assertIn("volumes.size ==\n                        1", reader)
        self.assertIn(
            '"Очікується один підготовлений том, а знайдено: "',
            reader,
        )

    def test_project_screen_chooses_between_multiple_real_volumes(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("PreparedVolumeReader.readAll(", activity)
        self.assertIn("importPreparedVolumes(", activity)
        self.assertIn('"Вибери том"', activity)
        self.assertIn(".setItems(", activity)
        self.assertIn("volumes[which]", activity)

    def test_same_tree_can_hold_multiple_distinct_real_volumes(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn("legacyPlaceholderFromSameTree", store)
        self.assertIn("pair.second.treeUri ==\n                            volume.treeUri", store)
        self.assertIn("pair.second.entrypoint ==\n                                volume.entrypoint", store)
        self.assertIn("pair.second.title ==\n                            pair.second.datasetTitle", store)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
