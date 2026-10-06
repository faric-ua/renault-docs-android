from pathlib import Path
import unittest


class AndroidPdfPerformanceContractTests(unittest.TestCase):
    def test_viewer_uses_cached_documents_contract_resolver_and_pdf_renderer(self):
        repo = Path(__file__).resolve().parents[1]

        resolver = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetResolver.kt"
        ).read_text(encoding="utf-8")
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        self.assertIn("buildChildDocumentsUriUsingTree", resolver)
        self.assertIn("directoryCache", resolver)
        self.assertIn("nodeCache", resolver)
        self.assertNotIn("findFile(", client)

        self.assertIn("PdfRenderer", pdf)
        self.assertIn("LruCache", pdf)
        self.assertIn("IntersectionObserver", pdf)
        self.assertIn("RENDER_MODE_FOR_DISPLAY", pdf)
        self.assertIn("currentPage", pdf)
        self.assertIn("applyZoomLayout", pdf)
        self.assertIn("section.style.width", pdf)
        self.assertIn("zoomInput", pdf)
        self.assertIn("zoomMenuButton", pdf)
        self.assertIn("data-zoom=\"85\"", pdf)
        self.assertIn("data-zoom=\"100\"", pdf)
        self.assertIn("data-zoom=\"120\"", pdf)
        self.assertIn("data-zoom=\"150\"", pdf)
        self.assertIn("data-zoom=\"200\"", pdf)
        self.assertIn("parseZoomInput", pdf)
        self.assertIn("setZoomPercent", pdf)
        self.assertIn("id=\"fitWidth\"", pdf)
        self.assertIn("Вмістити PDF по ширині", pdf)
        self.assertIn("fit-width-content", pdf)
        self.assertIn("fit-width-icon", pdf)
        self.assertIn("<svg", pdf)
        self.assertIn("По ширині", pdf)
        self.assertIn("justify-content: center", pdf)
        self.assertIn("align-items: center", pdf)
        self.assertIn("overflow-x: auto", pdf)
        self.assertIn("position: fixed", pdf)
        self.assertIn("document.body.appendChild", pdf)
        self.assertIn("positionZoomMenu", pdf)
        self.assertIn("requestAnimationFrame", pdf)
        self.assertIn("zoomMenu.contains", pdf)

        self.assertIn("WebSettings.LOAD_DEFAULT", viewer)
        self.assertIn("AndroidPdfLayer", client)

        self.assertIn("android.permission.INTERNET", manifest)
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", manifest)


if __name__ == "__main__":
    unittest.main()
