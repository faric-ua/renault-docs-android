from pathlib import Path
import unittest


class V0520PinesPdfLayoutContractTests(unittest.TestCase):
    def test_pines_pdf_uses_composed_portrait_layout_with_icons(self):
        repo = Path(__file__).resolve().parents[1]

        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        # Pines exports are a dedicated composed document, not a raw generic table dump.
        self.assertIn("isPinesExport", exporter)
        self.assertIn('"(pines)"', exporter)
        self.assertIn("drawPinesDocument", exporter)
        self.assertIn("drawConnectorCard", exporter)
        self.assertIn("drawPinIdentityBlock", exporter)
        self.assertIn("drawPinHeaderRow", exporter)

        # Pines use portrait page geometry even though the pin table has 4 columns.
        self.assertIn("isPinesExport ||", exporter)
        self.assertIn("PORTRAIT_PAGE_WIDTH", exporter)
        self.assertIn("PORTRAIT_PAGE_HEIGHT", exporter)

        # Connector/application info is extracted from the non-pin tables and kept.
        self.assertIn("connectorInfoLines", exporter)
        self.assertIn("if (table === pinTable)", exporter)

        # Vector icons exist for connector and each pin-table header role.
        self.assertIn("drawConnectorIcon", exporter)
        self.assertIn("drawPinNumberIcon", exporter)
        self.assertIn("drawWireSectionIcon", exporter)
        self.assertIn("drawWireCodeIcon", exporter)
        self.assertIn("drawDescriptionIcon", exporter)

        # Text header stays present together with icons.
        self.assertIn('"№"', exporter)
        self.assertIn('"мм²"', exporter)
        self.assertIn('"Код"', exporter)
        self.assertIn('"Опис"', exporter)

        # First three columns are content-sized, final description gets remainder.
        self.assertIn("pinColumnWidths", exporter)
        self.assertIn("maxCompactTotal", exporter)
        self.assertIn("total -", exporter)

        # Continuation pages use a compact identity header + repeated pin header.
        self.assertIn("drawPinContinuationHeader", exporter)


if __name__ == "__main__":
    unittest.main()
