from pathlib import Path
import unittest


class V0532PdfCompanionContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_main_pdf_exposes_companion_toggle(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )
        client = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        )

        self.assertIn("companionControlEnabled", pdf)
        self.assertIn('id="companion"', pdf)
        self.assertIn("window.renaultSetCompanion", pdf)
        self.assertIn("renaultcompanion://toggle", pdf)

        self.assertIn("PDF_COMPANION_SCHEME", client)
        self.assertIn("onToggleCompanion", client)

    def test_main_and_companion_pdf_state_are_scoped_separately(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("viewerStateScope", pdf)
        self.assertIn('pdfStateScope =\n                    "main"', viewer)
        self.assertIn('pdfStateScope =\n                    "companion"', viewer)

    def test_companion_uses_compact_render_budget(self):
        pdf = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        )
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("if (compactMode) 1800 else 3000", pdf)
        self.assertIn("if (compactMode) 1200 else 1600", pdf)
        self.assertIn("if (compactMode) 900 else 1200", pdf)
        self.assertIn("if (compactMode) 0 else 1", pdf)
        self.assertIn("pdfCompactMode =\n                    true", viewer)

    def test_viewer_contains_split_workspace_and_persists_companion(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        self.assertIn("viewerWorkspace", viewer)
        self.assertIn("companionContainer", viewer)
        self.assertIn("companionWebView", viewer)
        self.assertIn("togglePdfCompanion()", viewer)
        self.assertIn("showPdfCompanion(", viewer)
        self.assertIn("hidePdfCompanion()", viewer)
        self.assertIn("STATE_PDF_COMPANION_VISIBLE", viewer)
        self.assertIn("STATE_PDF_COMPANION_WEBVIEW", viewer)
        self.assertIn("companionWebView.saveState(", viewer)
        self.assertIn("restoreState(it)", viewer)

    def test_companion_loads_same_volume_documentation_in_embedded_page(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )
        page = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/VolumeDocumentationWebPage.kt"
        )

        self.assertIn("readVolumeDocumentationForVolume(", viewer)
        self.assertIn("modernVolumeEntrypoint", viewer)
        self.assertIn("VolumeDocumentationWebPage", viewer)

        self.assertIn("documentation.menu_items", page)
        self.assertIn("renderPanel(", page)
        self.assertIn("openDocument(", page)
        self.assertIn("https://renault.local/", page)

    def test_companion_pdf_does_not_offer_recursive_companion_button(self):
        viewer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )

        companion_block = viewer.split(
            "companionClient =",
            1,
        )[1].split(
            "companionWebView =",
            1,
        )[0]

        self.assertIn(
            "pdfCompanionControlEnabled =\n                    false",
            companion_block,
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
