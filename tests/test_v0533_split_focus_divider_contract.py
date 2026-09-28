from pathlib import Path
import unittest


class V0533SplitFocusDividerContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_fullscreen_split_uses_overlay_pdf_toolbar_mode(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("renault-split-focus", pdf)
        self.assertIn("controls-hidden", pdf)
        self.assertIn("position: fixed;", pdf)
        self.assertIn("window.renaultSetSplitFocus", pdf)

        self.assertIn("applySplitFocusMode()", viewer)
        self.assertIn(
            "pdfFullscreen &&\n                pdfCompanionVisible",
            viewer,
        )

    def test_double_tap_temporarily_reveals_controls(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("GestureDetector", viewer)
        self.assertIn("onDown(", viewer)
        self.assertIn("onDoubleTap(", viewer)
        self.assertIn("showSplitControlsTemporarily()", viewer)
        self.assertIn("SPLIT_CONTROLS_TIMEOUT_MS", viewer)
        self.assertIn("3200L", viewer)

    def test_split_divider_is_draggable_and_clamped(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("buildSplitDivider()", viewer)
        self.assertIn("MotionEvent.ACTION_MOVE", viewer)
        self.assertIn("applySplitRatio()", viewer)
        self.assertIn("0.20f", viewer)
        self.assertIn("0.80f", viewer)
        self.assertIn("splitDivider.visibility", viewer)

    def test_split_ratio_and_controls_survive_recreation(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("STATE_PDF_SPLIT_RATIO", viewer)
        self.assertIn("STATE_PDF_SPLIT_CONTROLS_VISIBLE", viewer)
        self.assertIn(
            "outState.putFloat(\n            STATE_PDF_SPLIT_RATIO",
            viewer,
        )
        self.assertIn(
            "outState.putBoolean(\n            STATE_PDF_SPLIT_CONTROLS_VISIBLE",
            viewer,
        )

    def test_companion_header_is_hidden_only_in_split_focus(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("companionHeader", viewer)
        self.assertIn(
            "if (active) {\n                    View.GONE\n                } else {\n                    View.VISIBLE",
            viewer,
        )
        self.assertIn("splitOverlayControls", viewer)

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
