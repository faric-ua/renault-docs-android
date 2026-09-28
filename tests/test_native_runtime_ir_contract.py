from pathlib import Path
import unittest


class NativeRuntimeIrContractTests(unittest.TestCase):
    def test_modern_section_uses_generic_runtime_ir_renderer(self):
        repo = Path(__file__).resolve().parents[1]

        reader = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/RuntimeIrReader.kt"
        ).read_text(encoding="utf-8")
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        volume = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo
            / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        self.assertIn('"_renault/runtime-ir-index.json"', reader)
        self.assertIn("schema >= 2", reader)
        self.assertIn('"section-ir-v2"', reader)

        self.assertIn(".readSection(", native)
        self.assertIn('"action-bar"', native)
        self.assertIn('"select"', native)
        self.assertIn('"document-list"', native)
        self.assertIn('"open-panel"', native)
        self.assertIn('"open-document"', native)
        self.assertIn('"structured-html"', native)
        self.assertIn('"composite-document"', native)
        self.assertIn('"legacy-javascript"', native)
        self.assertIn("openLegacyFallback", native)

        # Menu labels come from JSON. Do not special-case a 2001-only menu.
        self.assertNotIn('text = "SCH"', native)
        self.assertNotIn('text = "NM"', native)
        self.assertNotIn('text = "PC"', native)

        self.assertIn(".intentForSection(", volume)
        self.assertIn(".NativeSectionActivity", manifest)


if __name__ == "__main__":
    unittest.main()

