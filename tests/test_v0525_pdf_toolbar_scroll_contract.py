from pathlib import Path
import unittest


class V0525PdfToolbarScrollContractTests(unittest.TestCase):
    def test_pdf_controls_are_outside_the_document_scroll_viewport(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        toolbar = pdf.index('id="toolbar"')
        viewport = pdf.index('id="pdfViewport"')
        pages = pdf.index('id="pages"')
        self.assertLess(toolbar, viewport)
        self.assertLess(viewport, pages)

        self.assertIn("#pdfViewport {", pdf)
        self.assertIn("overflow: auto;", pdf)
        self.assertIn("html, body {", pdf)
        self.assertIn("overflow: hidden;", pdf)
        self.assertIn("pdfViewport.addEventListener(", pdf)
        self.assertNotIn("window.addEventListener(\n                  'scroll'", pdf)

    def test_portrait_toolbar_is_two_rows_and_landscape_is_one_row(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn('id="toolbarNav"', pdf)
        self.assertIn('id="toolbarZoom"', pdf)
        self.assertIn("flex-direction: column;", pdf)
        self.assertIn("@media (orientation: landscape)", pdf)
        self.assertIn("flex-direction: row;", pdf)
        self.assertIn("flex-wrap: nowrap;", pdf)

    def test_high_zoom_limits_prefetch_and_evicts_far_decoded_pages(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("function fastTargetWidth()", pdf)
        self.assertIn("1600", pdf)
        self.assertIn("documentScrollActive", pdf)
        self.assertIn("180", pdf)
        self.assertIn("function neighborRadius()", pdf)
        self.assertIn("zoom >= 1.5", pdf)
        self.assertIn("function retainRadius()", pdf)
        self.assertIn("function evictFarPages(index)", pdf)
        self.assertIn("img.removeAttribute('src')", pdf)
        self.assertIn("'300px 0px'", pdf)
        self.assertIn("root:\n                        pdfViewport", pdf)

    def test_nested_pdf_scroll_state_survives_rotation_recreation(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("quotedViewerStateKey", pdf)
        self.assertIn("window.localStorage", pdf)
        self.assertIn("5 * 60 * 1000", pdf)
        self.assertIn("pageOffsetRatio:", pdf)
        self.assertIn("horizontalRatio:", pdf)
        self.assertIn("section.offsetTop", pdf)
        self.assertIn("section.offsetHeight", pdf)
        self.assertIn("schedulePersistViewerState()", pdf)
        self.assertIn("restoredViewerState", pdf)
        self.assertIn("startPageObservers()", pdf)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 67)
        self.assertGreaterEqual(version_name, (0, 5, 51))


if __name__ == "__main__":
    unittest.main()
