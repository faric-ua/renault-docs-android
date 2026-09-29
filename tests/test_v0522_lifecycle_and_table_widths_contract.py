from pathlib import Path
import unittest


class V0522LifecycleAndTableWidthsContractTests(unittest.TestCase):
    def test_native_section_restores_inline_child_after_rotation(self):
        repo = Path(__file__).resolve().parents[1]
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("STATE_VIEW_KIND", native)
        self.assertIn("STATE_VIEW_ID", native)
        self.assertIn("restoreViewAfterRotation()", native)
        self.assertIn("VIEW_PANEL", native)
        self.assertIn("VIEW_DOCUMENT", native)
        self.assertIn("VIEW_DOCUMENTATION", native)
        self.assertIn("outState.putString(\n            STATE_VIEW_KIND", native)
        self.assertIn("renderPanel(\n                        restoredViewId", native)
        self.assertIn("renderOrOpenDocument(", native)

    def test_native_compact_columns_are_measured_and_abbreviations_are_bold(self):
        repo = Path(__file__).resolve().parents[1]
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("measuredCompactColumnWidth", native)
        self.assertIn("paint.measureText(", native)
        self.assertIn("compactWidths[column]", native)
        self.assertIn("columnCount == 2", native)
        self.assertIn("emphasize", native)
        self.assertIn("header ||\n                emphasize", native)

        # Generic two-column PDF abbreviations/designations are bold too.
        self.assertIn("columnCount == 2 &&", exporter)
        self.assertIn("cellPaint", exporter)
        self.assertIn("boldPaint", exporter)

    def test_settings_dialogs_restore_after_rotation(self):
        repo = Path(__file__).resolve().parents[1]
        settings = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SettingsActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("STATE_DIALOG_KIND", settings)
        self.assertIn("DIALOG_MODE", settings)
        self.assertIn("DIALOG_PDF_ZOOM", settings)
        self.assertIn("DIALOG_PDF_STEP", settings)
        self.assertIn("override fun onSaveInstanceState", settings)
        self.assertIn("when (activeDialogKind)", settings)
        self.assertIn("dialog.setOnDismissListener", settings)

    def test_viewer_dialogs_and_search_restore_after_rotation(self):
        repo = Path(__file__).resolve().parents[1]
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("STATE_DIALOG_KIND", viewer)
        self.assertIn("STATE_SECTION_NAV_QUERY", viewer)
        self.assertIn("restoreTransientWindow()", viewer)
        self.assertIn("DIALOG_SECTION_NAVIGATOR", viewer)
        self.assertIn("DIALOG_FRAME_DEBUG", viewer)
        self.assertIn("webView.saveState(outState)", viewer)
        self.assertIn("webView.restoreState(it)", viewer)
        self.assertIn("restoredSectionNavigatorQuery", viewer)

    def test_existing_modern_search_screens_already_persist_query(self):
        repo = Path(__file__).resolve().parents[1]
        dataset = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        ).read_text(encoding="utf-8")
        volume = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("STATE_QUERY", dataset)
        self.assertIn("override fun onSaveInstanceState", dataset)
        self.assertIn("STATE_QUERY", volume)
        self.assertIn("override fun onSaveInstanceState", volume)

    def test_no_custom_modal_in_main_or_conversion(self):
        repo = Path(__file__).resolve().parents[1]
        main = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        ).read_text(encoding="utf-8")
        conversion = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertNotIn("AlertDialog", main)
        self.assertNotIn("AlertDialog", conversion)
        self.assertIn("ACTION_OPEN_DOCUMENT_TREE", main)
        self.assertIn("ACTION_OPEN_DOCUMENT_TREE", conversion)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("versionCode = 67", gradle)
        self.assertIn('versionName = "0.5.51"', gradle)


if __name__ == "__main__":
    unittest.main()
