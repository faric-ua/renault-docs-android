"""v0.5.121 — owner phone QA 2026-10-13 03:21 (v0.5.120).

Navigator in the lower window — «все добре, класно, навігація проходить».
PDF toolbar at 0.72 felt too small: «збільш на 15–20%» → base 0.84 (+~17%),
the frame correction (top.visualViewport.scale) stays.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
LAYER = ROOT / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"


class ToolbarPlus17Contract(unittest.TestCase):
    def test_base_scale_is_15_to_20_percent_above_072(self):
        layer = LAYER.read_text(encoding="utf-8")
        base = float(re.search(r"const BASE_UI_SCALE = ([0-9.]+);", layer).group(1))
        fallback = float(re.search(r"zoom: var\(--ui-scale, ([0-9.]+)\);", layer).group(1))
        self.assertEqual(base, fallback)
        self.assertGreaterEqual(base, 0.72 * 1.15 - 1e-9)
        self.assertLessEqual(base, 0.72 * 1.20 + 1e-9)
        self.assertIn("window.top.visualViewport", layer)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 137)


if __name__ == "__main__":
    unittest.main()
