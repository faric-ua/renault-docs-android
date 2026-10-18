"""v0.5.124 — owner 2026-10-13 19:37: «скрипт, щоб сам взяв ті, яких ще немає
на диску, і загрузив, щоб я не шукав імена і не переплутав».

Termux cannot reach the .rdpkg files (they live inside the app) and has no
Google Drive login, so the help is in the app's own export window: one button
ticks exactly the volumes that are not yet in the public catalog.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class ExportOnlyNewContract(unittest.TestCase):
    def test_filter_matches_exact_code_and_date_or_file_name(self):
        f = (SRC / "CatalogPublishedFilter.kt").read_text(encoding="utf-8")
        self.assertIn("(code to date) in codesAndDates", f)
        self.assertIn("in names", f)
        self.assertIn("NT8340A ≠ NT8342A", f)

    def test_wizard_has_only_new_button_and_marks_published(self):
        w = (SRC / "ProjectExportWizard.kt").read_text(encoding="utf-8")
        self.assertIn('"Вибрати лише нові для каталогу"', w)
        # 0.6.6: loadCatalog takes a context (closed catalogue through the account).
        self.assertIn("DriveCatalogClient.loadCatalog(activity.applicationContext)", w)
        self.assertIn("state.selectOnly(planIds) { it !in published }", w)
        self.assertIn("уже в каталозі «Готові проєкти»", w)
        self.assertIn("Вибір не змінено.", w)
        self.assertNotIn("✓", w)
        state = (SRC / "ProjectExportWizardState.kt").read_text(encoding="utf-8")
        self.assertIn("fun selectOnly(", state)

    def test_jvm_test_exists(self):
        t = (ROOT / "android/app/src/test/java/com/saney/renaultdocs/CatalogPublishedFilterTest.kt").read_text(encoding="utf-8")
        self.assertIn("sameCodeAndDateIsPublishedButNeighbourNtIsNot", t)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 140)


if __name__ == "__main__":
    unittest.main()
