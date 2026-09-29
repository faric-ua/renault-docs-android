from pathlib import Path
import unittest


class V0528Pdf400FullscreenContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_zoom_range_extends_to_400_percent(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )

        self.assertIn('data-zoom="300"', pdf)
        self.assertIn('data-zoom="400"', pdf)
        self.assertIn("Math.min(\n                          4,", pdf)
        self.assertIn("Math.min(\n                      400,", pdf)
        self.assertIn("coerceIn(\n            50,\n            400,", pdf)

    def test_very_high_zoom_limits_render_width_and_uses_light_neighbors(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )

        self.assertIn("zoom >= 2.5", pdf)
        self.assertIn("1200", pdf)
        self.assertIn("3000", pdf)
        self.assertIn("if (compactMode) 1800 else 3000", pdf)
        self.assertIn(".coerceIn(600, maxRenderWidth)", pdf)
        self.assertIn("1600", pdf)

    def test_pdf_toolbar_exposes_fullscreen_toggle(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )

        self.assertIn('id="fullscreen"', pdf)
        self.assertIn("renaultfullscreen://toggle", pdf)

    def test_fullscreen_scheme_routes_to_activity_and_hides_app_system_chrome(self):
        client = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        )
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("PDF_FULLSCREEN_SCHEME", client)
        self.assertIn("onToggleFullscreen", client)
        self.assertIn("togglePdfFullscreen()", viewer)
        self.assertIn("appToolbar.visibility =", viewer)
        self.assertIn("WindowInsets.Type", viewer)
        self.assertIn("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE", viewer)
        self.assertIn("STATE_PDF_FULLSCREEN", viewer)
        self.assertIn("if (pdfFullscreen)", viewer)

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

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
