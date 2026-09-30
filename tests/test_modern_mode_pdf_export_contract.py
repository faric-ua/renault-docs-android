from pathlib import Path
import unittest


class ModernModePdfExportContractTests(unittest.TestCase):
    def test_modern_mode_is_default_and_classic_remains_available(self):
        repo = Path(__file__).resolve().parents[1]

        main = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        ).read_text(encoding="utf-8")
        modern = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "ModernDatasetActivity.intent",
            main,
        )
        self.assertIn(
            'label =\n                    "Classic"',
            modern,
        )
        self.assertIn(
            "Ui.actionButton(",
            modern,
        )
        self.assertIn(
            "ModernCatalogReader.read",
            modern,
        )
        self.assertIn(
            ".ModernDatasetActivity",
            manifest,
        )
        self.assertIn(
            ".ModernVolumeActivity",
            manifest,
        )
        self.assertIn(
            "ModernVolumeActivity",
            modern,
        )

    def test_pdf_export_uses_system_create_document_and_original_bytes(self):
        repo = Path(__file__).resolve().parents[1]

        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "Intent.ACTION_CREATE_DOCUMENT",
            viewer,
        )
        self.assertIn(
            '"application/pdf"',
            viewer,
        )
        self.assertIn(
            "STATE_PENDING_PDF_PATH",
            viewer,
        )
        self.assertIn(
            "copyPdfTo",
            client,
        )
        self.assertIn(
            "source.copyTo",
            client,
        )
        self.assertIn(
            "renaultsavepdf://save?path=",
            pdf,
        )

        self.assertNotIn(
            "MANAGE_EXTERNAL_STORAGE",
            manifest,
        )
        self.assertNotIn(
            "WRITE_EXTERNAL_STORAGE",
            manifest,
        )


if __name__ == "__main__":
    unittest.main()
