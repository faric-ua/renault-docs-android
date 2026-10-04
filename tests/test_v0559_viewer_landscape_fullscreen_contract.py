from pathlib import Path
import unittest


class V0559ViewerLandscapeFullscreenContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]

    def read(self, path: str) -> str:
        return (self.repo / path).read_text(encoding="utf-8")

    def test_pdf_viewer_does_not_force_system_bars_visible_in_landscape(self):
        viewer = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        block = viewer.split(
            "private fun applyPdfFullscreen(",
            1,
        )[1].split(
            "private fun setSystemBarsHidden(",
            1,
        )[0]

        self.assertIn("Ui.applyOrientationSystemBars(", block)
        self.assertIn("setSystemBarsHidden(", block)
        self.assertNotIn("hidden = enabled", block)


if __name__ == "__main__":
    unittest.main()
