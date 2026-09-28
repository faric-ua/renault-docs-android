from pathlib import Path
import unittest


class V0535ViewerRotationPreserveContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_viewer_handles_rotation_without_activity_recreation(self):
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )

        viewer_block = manifest.split(
            'android:name=".ViewerActivity"',
            1,
        )[1].split(
            "/>",
            1,
        )[0]

        self.assertIn(
            'android:configChanges="orientation|screenSize"',
            viewer_block,
        )

    def test_configuration_change_reapplies_fullscreen_and_split(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn(
            "override fun onConfigurationChanged(",
            viewer,
        )
        block = viewer.split(
            "override fun onConfigurationChanged(",
            1,
        )[1].split(
            "override fun onWindowFocusChanged",
            1,
        )[0]

        self.assertIn(
            "reconcilePdfFullscreenPresentation()",
            block,
        )
        self.assertIn(
            "applySplitRatio()",
            block,
        )
        self.assertIn(
            "window.decorView.post {",
            block,
        )
        self.assertIn(
            "window.decorView.postDelayed(",
            block,
        )
        self.assertIn(
            "180L",
            block,
        )

    def test_single_and_dual_document_fullscreen_share_same_rotation_path(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        block = viewer.split(
            "override fun onConfigurationChanged(",
            1,
        )[1].split(
            "override fun onWindowFocusChanged",
            1,
        )[0]

        self.assertIn(
            "reconcilePdfFullscreenPresentation()",
            block,
        )
        self.assertIn(
            "if (\n            pdfCompanionVisible",
            block,
        )

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
