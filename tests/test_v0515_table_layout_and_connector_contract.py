from pathlib import Path
import unittest


class V0515TableLayoutAndConnectorContractTests(unittest.TestCase):
    def test_adaptive_table_layout_combined_connector_and_export_names(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        layout = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTableLayout.kt"
        ).read_text(encoding="utf-8")
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")

        # Column widths depend on source content, not only on column count.
        self.assertIn("maxColumnLength(", layout)
        self.assertIn("visualLength(", layout)
        self.assertIn("columnFractions(", layout)
        self.assertIn("measuredCompactColumnWidth", native)
        self.assertIn("NativeTableLayout", exporter)

        # Two-column glossary PDFs use portrait pages while wider pin tables
        # can keep landscape layout.
        self.assertIn("PORTRAIT_PAGE_WIDTH", exporter)
        self.assertIn("LANDSCAPE_PAGE_WIDTH", exporter)
        self.assertIn("widestColumnCount <= 2", exporter)

        # Multi-line rows keep the short code cell vertically centered.
        self.assertIn("Gravity.CENTER_VERTICAL", native)
        self.assertIn("MATCH_PARENT", native)
        self.assertIn("textBlockHeight", exporter)

        # Connector composite exposes a combined Classic-like view in addition
        # to the two separate child documents.
        self.assertIn("Схема + піни розʼєма", native)
        self.assertIn("compositePath", native)
        self.assertIn("hasDrawing", native)
        self.assertIn("hasPins", native)

        # Suggested export filename uses source connector identity when present.
        self.assertIn("structuredPdfFileName(", native)
        # Exact connector filename semantics were tightened in v0.5.16.
        self.assertIn('"abbreviations"', native)
        self.assertIn("suggestedFileName", native)
        self.assertIn("suggestedFileName", exporter)
        self.assertIn("export.suggestedFileName", native)


if __name__ == "__main__":
    unittest.main()
