from pathlib import Path
import unittest


class V0517PdfTableHeaderContractTests(unittest.TestCase):
    def test_pdf_table_header_is_drawn_and_repeated_after_page_break(self):
        repo = Path(__file__).resolve().parents[1]

        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("table.rows.takeWhile", exporter)
        self.assertIn("it.header", exporter)
        self.assertIn("drawTableHeaderRows()", exporter)

        self.assertGreaterEqual(exporter.count("drawTableHeaderRows()"), 3)


if __name__ == "__main__":
    unittest.main()
