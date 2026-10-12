"""v0.5.118 — «Схема + піни»: print only the lower document; no window zoom.

Owner 2026-10-13 00:44 (screenshot of the print preview + screen video):
- v0.5.116 printing worked, but printed a picture of the whole window
  (PDF toolbar and all). Owner: print only the lower document, as it is.
- Video: on the two-document page the scheme zoomed by itself, then the
  whole window zoomed too, the lower document moved away and vanished.

Now the original printer icon sends the URL of its own frame and only that
document is printed (a hidden page loads it and prints it). While a page
holds a PDF frame (which has its own zoom), whole-window zoom is off.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


class CompositePrintAndZoomContract(unittest.TestCase):
    def test_print_sends_the_frame_document(self):
        client = read("SafDatasetWebViewClient.kt")
        bridge = between(client, "private fun applyPrintBridge(", "fun writePdf(")
        self.assertIn("$PAGE_PRINT_SCHEME://print?url=", bridge)
        self.assertIn("own = w.location.href", bridge)
        route = between(client, "override fun shouldOverrideUrlLoading(", "val localPath =")
        self.assertIn('getQueryParameter(\n                    "url",', route)
        self.assertIn("onPrintPage(view, documentUrl)", route)

    def test_viewer_prints_only_that_document(self):
        viewer = read("ViewerActivity.kt")
        doc = between(viewer, "private fun printDocument(", "private fun releasePrintPage()")
        self.assertIn("WebView(this)", doc)
        self.assertIn("printer.loadUrl(documentUrl)", doc)
        self.assertIn("onPageReady = { loaded ->", doc)
        self.assertIn("if (!printed)", doc)
        self.assertIn("PdfPrinter.printPage(this, page)", doc)  # whole page only as fallback
        self.assertIn("releasePrintPage()", between(viewer, "override fun onDestroy()", "super.onDestroy()"))

    def test_whole_window_zoom_is_locked_when_a_pdf_frame_is_inside(self):
        layer = read("AndroidPdfLayer.kt")
        self.assertIn("window.__renaultPdfViewer = true;", layer)
        client = read("SafDatasetWebViewClient.kt")
        bridge = between(client, "private fun applyPrintBridge(", "fun writePdf(")
        self.assertIn("function hasPdfFrame(w)", bridge)
        self.assertIn("'$ZOOM_SCHEME://' + (lock ? 'lock' : 'unlock')", bridge)
        self.assertIn('ZOOM_SCHEME =\n            "renaultzoom"', client)
        viewer = read("ViewerActivity.kt")
        lock = between(viewer, "private fun lockPageZoom(", "/** v0.5.115")
        self.assertLess(lock.index("page.zoomOut()"), lock.index("setSupportZoom(!lock)"))
        self.assertIn("builtInZoomControls = !lock", lock)
        self.assertEqual(viewer.count("onPageZoomLock = { page, lock ->"), 2)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 134)


if __name__ == "__main__":
    unittest.main()
