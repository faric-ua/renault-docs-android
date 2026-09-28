from pathlib import Path
import unittest


class FrameDebugContractTests(unittest.TestCase):
    def test_frame_debug_report_captures_runtime_structure(self):
        repo = Path(__file__).resolve().parents[1]

        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("generatedAt", client)
        self.assertIn("hybridStates", client)
        self.assertIn("parentFrameset", client)
        self.assertIn("sectionCodeCount", client)
        self.assertIn("selectCount", client)
        self.assertIn("pdfLinkCount", client)
        self.assertIn("imageCount", client)
        self.assertIn("buttonCount", client)
        self.assertIn("bodyBackground", client)
        self.assertIn("textFingerprint", client)
        self.assertIn("menuCandidates", client)
        self.assertIn("comboCandidates", client)
        self.assertIn("roleHints", client)

        self.assertIn('text = "DBG"', viewer)
        self.assertIn("showFrameDebugDialog", viewer)
        self.assertIn("setTextIsSelectable", viewer)
        self.assertIn("Frame debug скопійовано", viewer)


if __name__ == "__main__":
    unittest.main()
