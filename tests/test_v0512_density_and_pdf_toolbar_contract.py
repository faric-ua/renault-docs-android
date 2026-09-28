from pathlib import Path
import unittest


class V0512DensityAndPdfToolbarContractTests(unittest.TestCase):
    def test_compact_native_header_scrollable_contact_table_and_pdf_toolbar(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        # v0.5.13 supersedes the old v0.5.12 top header, while retaining
        # the compact section heading and fixed menu-row contract.
        self.assertIn("sectionCode +", native)
        self.assertIn('" — " +', native)
        self.assertIn("menuRowParams()", native)

        # v0.5.14 supersedes the old one-line contact stream with a shared
        # native table renderer. Keep the PDF-toolbar part of this contract.
        self.assertIn("buildStructuredTable(", native)
        self.assertIn("measuredCompactColumnWidth", native)

        # v0.5.25 separates PDF controls from the zoomed document viewport.
        self.assertIn('id="pdfViewport"', pdf)
        self.assertIn("pdfViewport.clientWidth", pdf)
        self.assertNotIn("toolbar.style.width =", pdf)

        # Plus sits immediately after the zoom picker and before fit-width.
        plus = pdf.index('id="plus"')
        fit_width = pdf.index('id="fitWidth"')
        self.assertLess(plus, fit_width)


if __name__ == "__main__":
    unittest.main()
