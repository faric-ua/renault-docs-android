from pathlib import Path
import unittest


class ClassicCatalogRuntimeContractTests(unittest.TestCase):
    def test_classic_package_catalog_is_sorted_by_date_without_rebuild(self):
        repo = Path(__file__).resolve().parents[1]

        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("onPageFinished", client)
        self.assertIn('"_renault/START.html"', client)
        self.assertIn("sortClassicCatalogByDate", client)
        self.assertIn("querySelector('.grid')", client)
        self.assertIn("querySelectorAll('.card')", client)
        self.assertIn("localeCompare", client)
        self.assertIn("grid.appendChild(card)", client)


if __name__ == "__main__":
    unittest.main()
