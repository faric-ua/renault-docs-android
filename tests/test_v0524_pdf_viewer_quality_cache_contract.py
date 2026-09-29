from pathlib import Path
import unittest


class V0524PdfViewerQualityCacheContractTests(unittest.TestCase):
    def test_pdf_pages_use_lossless_png_and_higher_adaptive_resolution(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("Bitmap.CompressFormat.PNG", pdf)
        self.assertNotIn("Bitmap.CompressFormat.JPEG", pdf)
        self.assertIn('"image/png"', pdf)
        self.assertIn("Math.min(\n                      2.5,", pdf)
        self.assertIn("3000", pdf)
        self.assertIn("800", pdf)

    def test_render_cache_survives_activity_recreation_and_is_dataset_namespaced(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("private val cacheNamespace: String", pdf)
        self.assertIn("sharedImageCache", pdf)
        self.assertIn("32 * 1024 * 1024", pdf)
        self.assertIn("sharedRenderLock", pdf)
        self.assertIn('"$cacheNamespace|$path|$pageIndex|$width"', pdf)
        self.assertIn("cacheNamespace =", client)
        self.assertIn("treeUri.toString()", client)

    def test_visible_page_is_prioritized_and_old_bitmap_stays_visible_during_upgrade(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("loadPage(\n                    index,", pdf)
        self.assertIn("neighborLoadTimer", pdf)
        self.assertIn("window.setTimeout(", pdf)
        self.assertIn("60", pdf)
        self.assertIn("if (!current)", pdf)
        # v0.5.25 may evict far pages, but the current page replacement path
        # still keeps the old bitmap visible while a sharper render loads.
        rerender = pdf[
            pdf.index("function rerenderForZoom()"):
            pdf.index("function setZoomPercent")
        ]
        self.assertNotIn("removeAttribute", rerender)
        self.assertIn("rootMargin:", pdf)

    def test_resize_only_requests_new_resolution_after_debounce(self):
        repo = Path(__file__).resolve().parents[1]
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("lastRenderWidth", pdf)
        self.assertIn("resizeRenderTimer", pdf)
        self.assertIn(">= 100", pdf)
        self.assertIn("120", pdf)

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
