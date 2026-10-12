"""v0.5.119 — owner requests 2026-10-13 01:01–01:10, collected TODO batch.

- PDF toolbar: one medium size everywhere (owner picked «Середня»). It was
  big on the separate PDF screen and tiny inside «Схема + піни» — a page with
  frames is laid out wider than the screen and shrunk, so the viewer scales
  its toolbar back by that ratio.
- PDF zoom: maximum 500% (was 400%), with 450% and 500% in the ▾ menu.
- «Схема + піни»: the page never zooms (v0.5.118 lock); each document zooms
  by itself — the PDF frame with its own zoom, other frames (pins table) get
  a two-finger zoom of their own.
- «Документація поруч»: the ✕ close button in the app style.
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


class ToolbarZoomFrameContract(unittest.TestCase):
    def test_toolbar_has_one_medium_size(self):
        layer = read("AndroidPdfLayer.kt")
        # v0.5.120: still one size for all, but smaller (0.72) and the frame
        # ratio comes from top.visualViewport.scale (screen.width was wrong
        # in landscape). See test_v05120_companion_navigator_contract.py.
        self.assertIn("zoom: var(--ui-scale, 0.72);", layer)
        self.assertIn("const BASE_UI_SCALE = 0.72;", layer)
        self.assertIn("'--ui-scale'", layer)
        menu = between(layer, "function positionZoomMenu()", "zoomMenu.style.top =")
        self.assertIn("left / uiScale", menu)

    def test_max_zoom_is_500_with_steps(self):
        layer = read("AndroidPdfLayer.kt")
        for z in ("400", "450", "500"):
            self.assertIn(f'data-zoom="{z}"', layer)
        self.assertLess(layer.index('data-zoom="400"'), layer.index('data-zoom="450"'))
        self.assertLess(layer.index('data-zoom="450"'), layer.index('data-zoom="500"'))
        normalize = between(layer, "function normalizeZoomPercent(percent)", "function updateZoomDisplay()")
        self.assertIn("500", normalize)
        self.assertNotIn("400", normalize)

    def test_each_frame_zooms_by_itself(self):
        client = read("SafDatasetWebViewClient.kt")
        bridge = between(client, "private fun applyPrintBridge(", "fun writePdf(")
        zoom = between(bridge, "function installFrameZoom(w)", "function patch(w)")
        for marker in ("w.__renaultPdfViewer", "w === window.top", "tagName === 'FRAMESET'",
                       "e.touches.length === 2", "e.preventDefault();", "doc.body.style.zoom",
                       "Math.min(5, next)", "{ passive: false }"):
            self.assertIn(marker, zoom)
        self.assertIn("zoomAllFrames(top);", bridge)
        self.assertIn("if (top.__renaultZoomLock) {\n                    installFrameZoom(w);", bridge)

    def test_companion_close_button_in_app_style(self):
        viewer = read("ViewerActivity.kt")
        close = between(viewer, 'text = "✕"', "hidePdfCompanion()")
        self.assertIn("Ui.roundedBackground(", close)
        self.assertIn("radiusDp = 22", close)
        self.assertIn("setTextColor(Ui.text)", close)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 135)


if __name__ == "__main__":
    unittest.main()
