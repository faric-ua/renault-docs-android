from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class StaleLegacyFallbackContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_unavailable_migrated_legacy_record_opens_current_project_without_deleting_tile(self):
        main = self.read("MainActivity.kt")
        start = main.index("private fun openLegacyDataset")
        end = main.index("private fun helpSpec", start)
        block = main[start:end]
        self.assertIn("findProjectForModel(", block)
        self.assertIn("projectStore", block)
        self.assertIn(".volumes(", block)
        self.assertIn("ProjectActivity.intent(", block)
        self.assertNotIn("store.remove(", block)

    def test_already_deleted_megane_legacy_tile_self_heals_as_compatibility_alias(self):
        main = self.read("MainActivity.kt")
        self.assertIn("LEGACY_COMPAT_PROJECT_IDS", main)
        self.assertIn('"megane-ii"', main)
        self.assertIn("compatibilityProjects", main)
        self.assertIn("buildLegacyProjectAliasTile(project)", main)
        self.assertIn("Перенесено в актуальний проєкт", main)
        self.assertIn("Томів: ", main)
        self.assertIn("відкриття: проєкт", main)

    def test_unavailable_unmigrated_legacy_record_keeps_explicit_error(self):
        main = self.read("MainActivity.kt")
        self.assertIn("більше недоступна за збереженим шляхом", main)
        self.assertIn("додай її знову через Legacy", main)

if __name__ == "__main__":
    unittest.main()
