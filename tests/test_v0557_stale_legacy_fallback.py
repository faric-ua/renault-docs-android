from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class StaleLegacyFallbackContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_dataset_store_can_remove_stale_record_without_touching_files(self):
        store = self.read("DatasetStore.kt")
        self.assertIn("fun remove(id: String)", store)
        self.assertIn(".filterNot {", store)
        self.assertNotIn("DocumentFile", store)

    def test_unavailable_migrated_legacy_record_opens_current_project(self):
        main = self.read("MainActivity.kt")
        start = main.index("private fun openLegacyDataset")
        end = main.index("private fun helpSpec", start)
        block = main[start:end]
        self.assertIn("findProjectForModel(", block)
        self.assertIn("projectStore", block)
        self.assertIn(".volumes(", block)
        self.assertIn("store.remove(", block)
        self.assertIn("ProjectActivity.intent(", block)
        self.assertIn("renderLibrary()", block)

    def test_unavailable_unmigrated_legacy_record_keeps_explicit_error(self):
        main = self.read("MainActivity.kt")
        self.assertIn("більше недоступна за збереженим шляхом", main)
        self.assertIn("додай її знову через Legacy", main)

if __name__ == "__main__":
    unittest.main()
