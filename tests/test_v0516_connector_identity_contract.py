from pathlib import Path
import unittest


class V0516ConnectorIdentityContractTests(unittest.TestCase):
    def test_connector_identity_is_opaque_and_pin_export_uses_pines(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn('fileStem.startsWith(', native)
        self.assertIn('"T_"', native)
        self.assertIn('ignoreCase = true', native)
        self.assertIn('fileStem.substring(2)', native)
        self.assertNotIn("sectionPattern", native)
        self.assertIn('"pines"', native)
        self.assertIn("isPinTable", native)


if __name__ == "__main__":
    unittest.main()
