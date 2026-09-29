from pathlib import Path
import unittest


class V0530PdfHighZoomScrollContractTests(unittest.TestCase):
    def _pdf(self) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

    def test_high_zoom_keeps_one_neighbor_on_each_side(self):
        pdf = self._pdf()

        neighbor = pdf.split(
            "function neighborRadius()",
            1,
        )[1].split(
            "function retainRadius()",
            1,
        )[0]
        retain = pdf.split(
            "function retainRadius()",
            1,
        )[1].split(
            "function neighborRenderWidth",
            1,
        )[0]

        self.assertIn("highZoomNeighborRadius", neighbor)
        self.assertIn("highZoomRetainRadius", retain)
        self.assertIn("if (compactMode) 0 else 1", pdf)

    def test_high_zoom_neighbors_use_lightweight_preload(self):
        pdf = self._pdf()

        self.assertIn("function neighborRenderWidth(", pdf)
        self.assertIn("if (zoom >= 2.5)", pdf)
        self.assertIn("1200", pdf)
        self.assertIn("neighborWidth", pdf)

    def test_scroll_tracks_current_page_from_viewport_center(self):
        pdf = self._pdf()

        self.assertIn(
            "function updateCurrentPageFromViewportCenter()",
            pdf,
        )
        self.assertIn(
            "pdfViewport.scrollTop +",
            pdf,
        )
        self.assertIn(
            "pdfViewport.clientHeight / 2",
            pdf,
        )
        self.assertIn(
            "centerY >= top",
            pdf,
        )
        self.assertIn(
            "updateCurrentPageFromViewportCenter();",
            pdf,
        )

    def test_current_page_setter_drives_counter(self):
        pdf = self._pdf()

        self.assertIn("function setCurrentPage(", pdf)
        self.assertIn("counter.textContent =", pdf)
        self.assertIn("setCurrentPage(", pdf)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("versionCode = 67", gradle)
        self.assertIn('versionName = "0.5.51"', gradle)


if __name__ == "__main__":
    unittest.main()
