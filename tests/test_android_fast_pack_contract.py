from pathlib import Path
import unittest


class AndroidFastPackContractTests(unittest.TestCase):
    def test_legacy_resources_prefer_local_fast_pack_before_saf(self):
        repo = Path(__file__).resolve().parents[1]

        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")
        archive = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/FastContentArchive.kt"
        ).read_text(encoding="utf-8")
        modern = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        pdf_pos = client.index("pdfLayer.intercept")
        fast_pos = client.index("fastArchive.value")
        saf_pos = client.index("resolver.resolve")

        self.assertLess(pdf_pos, fast_pos)
        self.assertLess(fast_pos, saf_pos)

        self.assertIn("ZipFile", archive)
        self.assertIn("context.cacheDir", archive)
        self.assertIn("SHA-256", archive)
        self.assertIn('"zip-web-v1"', archive)
        self.assertIn("FastContentArchive", modern)
        self.assertIn(".prepare(", modern)
        self.assertIn("Fast Pack активний", modern)

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
