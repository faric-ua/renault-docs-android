from pathlib import Path
import unittest


class V0514NativeTablesPdfContractTests(unittest.TestCase):
    def test_shared_structured_table_renderer_and_pdf_export(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")

        # One shared table model/rendering path is used for glossary and pin data.
        self.assertIn("NativeTableData", native)
        self.assertIn("NativeTableRow", native)
        self.assertIn("structuredTables(", native)
        self.assertIn("buildStructuredTable(", native)
        self.assertIn("measuredCompactColumnWidth", native)
        self.assertIn("GradientDrawable", native)

        # Structured documents no longer expose legacy CMP titles directly.
        self.assertIn('"CMP" + sectionCode', native)
        self.assertIn('sectionCode +\n                        " — " +\n                        sectionTitle', native)

        # Connector criteria/metadata are separated from the table itself.
        self.assertIn("structuredHeader(", native)
        self.assertIn("renderStructuredHeader(", native)
        self.assertIn("metadata", native)
        self.assertIn("criteria", native)

        # Native table can be saved as an actual PDF through Android's document picker.
        self.assertIn("Зберегти таблицю PDF", native)
        self.assertIn("Intent.ACTION_CREATE_DOCUMENT", native)
        self.assertIn('"application/pdf"', native)
        self.assertIn("REQUEST_SAVE_TABLE_PDF", native)
        self.assertIn("NativeTablePdfExporter", native)

        self.assertIn("PdfDocument", exporter)
        self.assertIn("openOutputStream", exporter)
        self.assertIn("drawTable(", exporter)
        self.assertIn("drawDocumentHeader()", exporter)
        self.assertIn("NativeTableLayout", exporter)


if __name__ == "__main__":
    unittest.main()
