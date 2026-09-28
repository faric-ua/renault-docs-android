from pathlib import Path
import unittest


class V0523LandscapeAndPdfTypographyContractTests(unittest.TestCase):
    def test_landscape_focus_keeps_only_active_top_menu_chrome(self):
        repo = Path(__file__).resolve().parents[1]
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("STATE_MENU_LABEL", native)
        self.assertIn("STATE_MENU_ACTION_ID", native)
        self.assertIn("isLandscapeFocusMode()", native)
        self.assertIn("applyLandscapeFocusChrome()", native)
        self.assertIn("globalBar.visibility", native)
        self.assertIn("contextRow.visibility", native)
        self.assertIn("View.GONE", native)
        self.assertIn("currentMenuLabel.isNotBlank()", native)
        self.assertIn("addFullWidthMenuButton(\n                activeButton", native)
        self.assertIn('currentMenuLabel ==\n                    "Документація"', native)

    def test_portrait_restores_full_menu_after_rotation(self):
        repo = Path(__file__).resolve().parents[1]
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("ORIENTATION_LANDSCAPE", native)
        self.assertIn("View.VISIBLE", native)
        self.assertIn("selectTopMenu(", native)
        self.assertIn("renderMenu()", native)
        self.assertIn("outState.putString(\n            STATE_MENU_LABEL", native)

    def test_pines_pdf_uses_14pt_headers_and_10pt_bold_technical_columns(self):
        repo = Path(__file__).resolve().parents[1]
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("BODY_TEXT_SIZE = 10f", exporter)
        self.assertIn("HEADER_TEXT_SIZE = 10f", exporter)
        self.assertIn("TABLE_HEADER_TEXT_SIZE = 14f", exporter)
        self.assertIn("val tableHeaderPaint", exporter)
        self.assertIn("tableHeaderPaint.measureText", exporter)

        # Pin rows: columns 1-3 (indexes 0-2) use the bold 10pt body paint.
        self.assertIn("if (column < 3)", exporter)
        self.assertIn("boldPaint\n                                        .measureText", exporter)
        self.assertIn("baseline,\n                        boldPaint", exporter)

        # The compact upper connector info card uses the same 14pt header paint.
        self.assertIn("tableHeaderPaint.textSize", exporter)
        self.assertIn("topBaseline", exporter)
        self.assertIn("bottomBaseline", exporter)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
