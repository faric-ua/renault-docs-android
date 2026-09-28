from pathlib import Path
import unittest


class V0521PdfPolishContractTests(unittest.TestCase):
    def test_native_pin_table_has_header_and_compact_columns(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        layout = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTableLayout.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("ensurePinHeader", native)
        self.assertIn('"(pines)"', native)
        self.assertIn('"№"', native)
        self.assertIn('"мм²"', native)
        self.assertIn('"Код"', native)
        self.assertIn('"Опис"', native)

        # Compact technical columns consume less space than before.
        self.assertIn("maxTotal = 0.34f", layout)
        self.assertIn("0.075f", layout)
        self.assertIn("0.16f", layout)

    def test_pdf_uses_two_cm_margins_centered_title_and_colored_icons(self):
        repo = Path(__file__).resolve().parents[1]

        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        # 57pt is approximately 2cm at 72dpi.
        self.assertIn("private const val MARGIN = 57f", exporter)

        # Generic PDFs center the title/criteria and leave breathing room before tables.
        self.assertIn("drawCenteredWrapped(\n                    data.title", exporter)
        self.assertIn("y += titleHeight + 12f", exporter)

        # Pines icons use a restrained modern color palette and soft badges.
        self.assertIn("connectorAccent", exporter)
        self.assertIn("pinNumberAccent", exporter)
        self.assertIn("wireSectionAccent", exporter)
        self.assertIn("wireCodeAccent", exporter)
        self.assertIn("descriptionAccent", exporter)
        self.assertIn("Color.argb(", exporter)
        self.assertIn("drawIconBadge", exporter)

        # Pines PDF compact columns are measured tightly, final column keeps remainder.
        self.assertIn("floatArrayOf(\n                    30f,\n                    36f,\n                    36f", exporter)
        self.assertIn("total * 0.34f", exporter)


if __name__ == "__main__":
    unittest.main()
