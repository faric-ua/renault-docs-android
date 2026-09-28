from pathlib import Path
import unittest


class V0511NmAndClassicContractTests(unittest.TestCase):
    def test_native_menu_rows_nm_split_and_classic_canvas(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        presentation = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeRuntimePresentation.kt"
        ).read_text(encoding="utf-8")
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")

        # Primary Modern menu layout:
        # SCH + NM on row 1, PC row 2, Documentation row 3.
        self.assertIn('"SCH"', native)
        self.assertIn('"NM"', native)
        self.assertIn('"PC"', native)
        self.assertIn('label = "Документація"', native)
        self.assertIn("LinearLayout.VERTICAL", native)
        self.assertIn('"SCH" -> "Схеми"', presentation)

        # Nomenclature composite keeps drawing and contact-description
        # as separate user-visible documents instead of flattening them.
        self.assertIn('"Схема розʼєму"', native)
        self.assertIn('"Опис контактів"', native)
        self.assertIn("addCompositePartButton(", native)
        self.assertNotIn(
            '"structured-html" ->\n                    appendStructuredDocument(',
            native,
        )

        # Structured pin data is static text, not a fake button/card.
        self.assertIn("buildStructuredTable(", native)
        self.assertIn("cells.any", native)

        # Classic top-level volume uses a white WebView canvas so transparent
        # nested pin-description frames remain readable.
        self.assertIn("classicVolumeMode", viewer)
        self.assertIn("android.graphics.Color.WHITE", viewer)


if __name__ == "__main__":
    unittest.main()
