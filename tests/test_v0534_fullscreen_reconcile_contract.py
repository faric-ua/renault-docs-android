from pathlib import Path
import unittest


class V0534FullscreenReconcileContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_restore_reconciles_after_main_webview_state(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        restore = viewer.index("val restoredHistory = savedInstanceState")
        reconcile = viewer.index(
            "reconcilePdfFullscreenPresentation()",
            restore,
        )

        self.assertGreater(reconcile, restore)
        self.assertIn(
            "webView.post {\n            reconcilePdfFullscreenPresentation()",
            viewer,
        )

    def test_reconcile_uses_android_fullscreen_state_as_source_of_truth(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        block = viewer.split(
            "private fun reconcilePdfFullscreenPresentation()",
            1,
        )[1].split(
            "private fun togglePdfFullscreen()",
            1,
        )[0]

        self.assertIn("applyPdfFullscreen(", block)
        self.assertIn("pdfFullscreen", block)
        self.assertIn("syncPdfCompanionControl()", block)

    def test_closing_companion_reconciles_without_toggling_fullscreen(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        block = viewer.split(
            "private fun hidePdfCompanion()",
            1,
        )[1].split(
            "private fun companionBack()",
            1,
        )[0]

        self.assertIn("pdfCompanionVisible = false", block)
        self.assertIn("reconcilePdfFullscreenPresentation()", block)
        self.assertNotIn("pdfFullscreen =", block)

    def test_window_focus_reconciles_true_and_false_states(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        block = viewer.split(
            "override fun onWindowFocusChanged",
            1,
        )[1].split(
            "override fun onDestroy",
            1,
        )[0]

        self.assertIn("if (hasFocus)", block)
        self.assertIn("reconcilePdfFullscreenPresentation()", block)
        self.assertNotIn("hasFocus &&\n            pdfFullscreen", block)

    def test_pdf_title_callbacks_reconcile_after_js_is_available(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertGreaterEqual(
            viewer.count("reconcilePdfFullscreenPresentation()"),
            6,
        )

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
