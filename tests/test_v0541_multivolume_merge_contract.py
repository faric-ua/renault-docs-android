from pathlib import Path
import unittest


class V0541MultiVolumeMergeContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_writer_exposes_dataset_identity_and_volume_roots(self):
        writer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidDatasetPackageWriter.kt"
        )

        self.assertIn("fun datasetIdFor(", writer)
        self.assertIn("fun discoverVolumeRoots(", writer)
        self.assertIn('"source_folder"', writer)

    def test_converter_merges_into_compatible_existing_output(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("mergeIntoExistingOutput(", engine)
        self.assertIn("Існуючий dataset знайдено", engine)
        self.assertIn(".renault-merge-", engine)
        self.assertIn("Том уже є", engine)
        self.assertIn("Оновлюю package для всіх томів", engine)
        self.assertIn("combinedFilePaths", engine)
        self.assertIn("packageResult.volumeCount", engine)

    def test_existing_output_must_have_same_dataset_identity(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        self.assertIn("expectedDatasetId", activity)
        self.assertIn("existingRecord.id", activity)
        self.assertIn("existingRecord.id", engine)
        self.assertIn("належить іншому dataset", activity)
        self.assertIn("належить іншому dataset", engine)

    def test_plan_explains_merge_instead_of_rejecting_existing_output(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn(
            "Існуючий dataset буде доповнено новими томами",
            activity,
        )
        self.assertIn(
            "Наявні томи не видаляються.",
            activity,
        )
        self.assertNotIn(
            '"Output уже існує: " +\n                    plan.outputFolderName',
            activity,
        )

    def test_conversion_report_is_replaced_when_dataset_is_merged(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )

        report_pos = engine.index('findFile(\n                "conversion-report.json"')
        create_pos = engine.index(
            'createFile(\n                "application/json",\n                "conversion-report.json"',
            report_pos,
        )
        self.assertLess(report_pos, create_pos)


    def test_interrupted_merge_is_not_mistaken_for_completed_output(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionRunStore.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn("val mergeExisting: Boolean = false", store)
        self.assertIn("KEY_MERGE_EXISTING", store)
        self.assertIn("mergeExisting =", service)
        self.assertIn("state.mergeExisting", activity)
        self.assertIn("Попереднє додавання томів було перервано", activity)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
