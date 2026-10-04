from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class StaleLegacyFallbackContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_unavailable_migrated_legacy_record_can_open_current_project_without_deleting_data(self):
        main = self.read("MainActivity.kt")
        start = main.index("private fun openLegacyDataset")
        end = main.index("private fun helpSpec", start)
        block = main[start:end]
        self.assertIn("findProjectForModel(", block)
        self.assertIn("projectStore", block)
        self.assertIn(".volumes(", block)
        self.assertIn("ProjectActivity.intent(", block)
        self.assertNotIn("store.remove(", block)

    def test_populated_current_project_hides_matching_legacy_dataset_from_home(self):
        main = self.read("MainActivity.kt")
        self.assertIn("visibleLegacyRecords", main)
        self.assertIn("findProjectForModel(", main)
        self.assertIn(".isEmpty()", main)
        self.assertIn("visibleLegacyRecords.forEach", main)
        self.assertNotIn("LEGACY_COMPAT_PROJECT_IDS", main)
        self.assertNotIn("buildLegacyProjectAliasTile", main)

    def test_unavailable_unmigrated_legacy_record_keeps_explicit_error(self):
        main = self.read("MainActivity.kt")
        self.assertIn("більше недоступна за збереженим шляхом", main)
        self.assertIn("додай її знову через Legacy", main)

if __name__ == "__main__":
    unittest.main()
