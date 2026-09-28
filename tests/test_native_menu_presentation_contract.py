from pathlib import Path
import unittest


class NativeMenuPresentationContractTests(unittest.TestCase):
    def test_friendly_labels_docs_group_and_link_states(self):
        repo = Path(__file__).resolve().parents[1]

        presentation = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeRuntimePresentation.kt"
        ).read_text(encoding="utf-8")
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo
            / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")
        foreground = (
            repo
            / "android/app/src/main/res/drawable/ic_launcher_foreground.xml"
        ).read_text(encoding="utf-8")

        self.assertIn('"SCH" -> "Схеми"', presentation)
        self.assertIn('"NM" -> "Розʼєм"', presentation)
        self.assertIn('"PC" -> "Положення на авто"', presentation)
        self.assertIn('"GENE"', presentation)
        self.assertIn('"PLATFUSI"', presentation)
        self.assertIn('"AIDE"', presentation)
        self.assertIn('"BLANK"', presentation)

        self.assertIn('label = "Документація"', native)
        self.assertIn("renderDocumentationMenu", native)
        self.assertIn("isActionResolvable", native)
        self.assertIn("— недоступно", native)
        self.assertIn('"› " + label', native)

        # Classic button is now pure Classic, not the old hybrid target flow.
        fallback = native.split("private fun openLegacyFallback()", 1)[1]
        fallback = fallback.split("private fun firstOpenPanelAction", 1)[0]
        self.assertNotIn("modernSectionCode", fallback)
        self.assertNotIn("modernSectionLegacyEntrypoint", fallback)

        self.assertIn('android:icon="@mipmap/ic_launcher"', manifest)
        self.assertIn('android:roundIcon="@mipmap/ic_launcher_round"', manifest)
        self.assertIn("#76BDFF", foreground)
        self.assertIn("#F4C542", foreground)


if __name__ == "__main__":
    unittest.main()
