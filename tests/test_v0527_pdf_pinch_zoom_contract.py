from pathlib import Path
import unittest


class V0527PdfPinchZoomContractTests(unittest.TestCase):
    def _pdf_source(self) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

    def test_browser_page_zoom_is_disabled_but_document_viewport_handles_pinch(self):
        pdf = self._pdf_source()

        self.assertIn(
            'maximum-scale=1,user-scalable=no',
            pdf,
        )
        self.assertIn("touch-action: pan-x pan-y;", pdf)
        self.assertIn("'touchstart'", pdf)
        self.assertIn("'touchmove'", pdf)
        self.assertIn("function beginPinch(event)", pdf)
        self.assertIn("function movePinch(event)", pdf)
        self.assertIn("event.preventDefault();", pdf)

    def test_pinch_updates_live_percent_and_keeps_focal_page_position(self):
        pdf = self._pdf_source()

        self.assertIn("function updateZoomDisplay()", pdf)
        self.assertIn("zoomInput.value =", pdf)
        self.assertIn("pinchStartZoom", pdf)
        self.assertIn("pinchAnchorSection", pdf)
        self.assertIn("pinchAnchorXRatio", pdf)
        self.assertIn("pinchAnchorYRatio", pdf)
        self.assertIn("pdfViewport.scrollLeft =", pdf)
        self.assertIn("pdfViewport.scrollTop =", pdf)

    def test_quality_rerender_is_deferred_until_zoom_interaction_settles(self):
        pdf = self._pdf_source()

        self.assertIn("function scheduleZoomQualityRender(", pdf)
        self.assertIn("delayMs = 180", pdf)
        self.assertIn("if (pinchActive)", pdf)
        self.assertIn("rerenderForZoom();", pdf)
        self.assertIn("scheduleZoomQualityRender();", pdf)
        self.assertIn("neighborLoadTimer", pdf)
        self.assertIn("documentScrollIdleTimer", pdf)

        set_zoom = pdf.split(
            "function setZoomPercent(percent)",
            1,
        )[1].split(
            "function touchDistance",
            1,
        )[0]
        self.assertIn("scheduleZoomQualityRender();", set_zoom)
        self.assertNotIn("rerenderForZoom();", set_zoom)

    def test_toolbar_remains_outside_zoomable_pdf_viewport(self):
        pdf = self._pdf_source()

        toolbar = pdf.index('id="toolbar"')
        viewport = pdf.index('id="pdfViewport"')
        pages = pdf.index('id="pages"')

        self.assertLess(toolbar, viewport)
        self.assertLess(viewport, pages)
        self.assertIn("#toolbar {", pdf)
        self.assertIn("#pdfViewport {", pdf)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo / "android/app/build.gradle.kts"
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
