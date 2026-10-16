"""v0.5.139 — PDF pages faster (owner 2026-10-16 21:37–21:44: «хотів би ще швидше»).

After v0.5.138 files on the card are read fast (log 111). Left: every page / width
opened the PDF again, each page was compressed with Android's slow PNG encoder, drawn
pages were kept only in RAM.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class PdfSpeedContract(unittest.TestCase):
    def test_pdf_stays_open_while_viewed(self):
        pdf = source("AndroidPdfLayer.kt")
        self.assertIn("private fun openPdf(", pdf)
        self.assertIn("MAX_OPEN_PDFS = 2", pdf)
        self.assertEqual(pdf.count("PdfRenderer(pfd)"), 1, "one place opens a PDF")
        self.assertNotIn(".use { renderer ->", pdf, "renderer is no longer closed after each page")
        self.assertIn("VolumeFileIndex.onForget", pdf)

    def test_fast_lossless_png(self):
        pdf = source("AndroidPdfLayer.kt")
        self.assertIn("FastPngEncoder.encodeRows(width, height)", pdf)
        self.assertNotIn("bitmap.compress(", pdf)
        encoder = source("FastPngEncoder.kt")
        self.assertIn("Deflater.BEST_SPEED", encoder)
        self.assertIn("STRIPE_ROWS = 64", encoder)

    def test_drawn_pages_kept_in_app_cache_only(self):
        pdf = source("AndroidPdfLayer.kt")
        self.assertIn("pageDiskCache?.get(diskKey)", pdf)
        self.assertIn("pageDiskCache?.put(diskKey, it)", pdf)
        self.assertIn("attributes.size, attributes.lastModified", pdf)
        client = source("SafDatasetWebViewClient.kt")
        self.assertIn("context.applicationContext.cacheDir", client)
        cache = source("PdfPageDiskCache.kt")
        self.assertIn('DIR_NAME = "renault-pdf-pages"', cache)
        self.assertIn('f.name.endsWith(SUFFIX)', cache, "trim touches only its own pictures")

    def test_timings_logged(self):
        pdf = source("AndroidPdfLayer.kt")
        self.assertIn('"PDF відкрито файл=', pdf)
        self.assertIn("мс_малювання=", pdf)
        self.assertIn("мс_стиснення=", pdf)


if __name__ == "__main__":
    unittest.main()
