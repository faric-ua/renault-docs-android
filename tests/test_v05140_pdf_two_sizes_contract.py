"""v0.5.140 — PDF page in two sizes only (owner 2026-10-17 00:07).

«Коли зумуємо — одразу максимальний розмір, проміжні не рисуємо; зменшує —
із більшого зменшимо, не перерисовуючи.» Log 112: 1000 → 1600 → 2000/2400/2600 →
3000, each drawn and compressed again.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PDF = ROOT / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"


class PdfTwoSizesContract(unittest.TestCase):
    def setUp(self):
        self.pdf = PDF.read_text(encoding="utf-8")

    def test_zoomed_in_goes_straight_to_maximum(self):
        self.assertIn("const ZOOMED_IN = 1.05;", self.pdf)
        self.assertIn("function isZoomedIn()", self.pdf)
        self.assertIn("return isZoomedIn()\n                    ? ${maxRenderWidth}\n                    : screenWidth();", self.pdf)

    def test_screen_size_does_not_grow_with_zoom(self):
        self.assertIn("viewport * dpr * Math.min(1, zoom)", self.pdf)
        self.assertNotIn("viewport * dpr * zoom\n", self.pdf)

    def test_zoom_out_keeps_bigger_picture(self):
        # loadPage skips when the picture already there is at least as big.
        self.assertIn("currentWidth >=\n                        renderWidth", self.pdf)

    def test_no_intermediate_widths(self):
        self.assertNotIn("fastRenderWidth", self.pdf.replace("// v0.5.140: fastRenderWidth", ""))
        self.assertNotIn("${neighborRenderWidthCap}", self.pdf)
        self.assertNotIn("zoom >= 1.5", self.pdf)
        self.assertNotIn("zoom >= 2.5", self.pdf)


if __name__ == "__main__":
    unittest.main()
