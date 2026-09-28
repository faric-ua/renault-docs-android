from pathlib import Path
import unittest


class V0536FullscreenButtonAckContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_pdf_setter_returns_ack_and_reapplies_on_view_lifecycle_events(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )

        self.assertIn(
            "window.renaultNativeFullscreenExpected",
            pdf,
        )
        self.assertIn(
            "return (",
            pdf,
        )
        self.assertIn(
            "reapplyNativeFullscreenState",
            pdf,
        )
        self.assertIn(
            "'resize'",
            pdf,
        )
        self.assertIn(
            "'pageshow'",
            pdf,
        )
        self.assertIn(
            "'visibilitychange'",
            pdf,
        )

    def test_android_retries_until_pdf_button_acknowledges_state(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn(
            "fullscreenControlSyncGeneration",
            viewer,
        )
        self.assertIn(
            "FULLSCREEN_CONTROL_SYNC_MAX_ATTEMPTS",
            viewer,
        )
        self.assertIn(
            "FULLSCREEN_CONTROL_SYNC_BASE_DELAY_MS",
            viewer,
        )
        self.assertIn(
            "FULLSCREEN_CONTROL_SYNC_STEP_DELAY_MS",
            viewer,
        )
        self.assertIn(
            "window.renaultSetFullscreen",
            viewer,
        )
        self.assertIn(
            "result == \"true\"",
            viewer,
        )
        self.assertIn(
            "target.postDelayed(",
            viewer,
        )

    def test_stale_retries_cannot_overwrite_new_fullscreen_state(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        helper = viewer.split(
            "private fun syncPdfFullscreenControl(\n        target: WebView",
            1,
        )[1].split(
            "private fun applyPdfFullscreen(",
            1,
        )[0]

        self.assertIn(
            "generation !=",
            helper,
        )
        self.assertIn(
            "expected !=",
            helper,
        )
        self.assertIn(
            "fullscreenControlSyncGeneration",
            helper,
        )
        self.assertIn(
            "pdfFullscreen",
            helper,
        )

    def test_rotation_preserve_fix_stays_enabled(self):
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn(
            'android:configChanges="orientation|screenSize"',
            manifest,
        )
        self.assertIn(
            "override fun onConfigurationChanged(",
            viewer,
        )

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
