from pathlib import Path
import unittest


class TechnicalBlueHeaderContractTests(unittest.TestCase):
    def test_viewer_has_modern_bridge_top_actions_and_page_search(self):
        repo = Path(__file__).resolve().parents[1]

        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")
        modern = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn('"Modern"', viewer)
        self.assertIn('"Розділи"', viewer)
        self.assertIn("openModern()", viewer)
        self.assertIn("R.drawable.ic_home", viewer)
        self.assertIn("R.drawable.ic_search", viewer)
        self.assertIn("R.drawable.ic_settings", viewer)

        self.assertIn("findAllAsync", viewer)
        self.assertIn("findNext", viewer)
        self.assertIn("clearMatches", viewer)
        self.assertIn("STATE_SEARCH_QUERY", viewer)
        self.assertIn("STATE_SEARCH_VISIBLE", viewer)
        self.assertIn("existingQuery", viewer)
        self.assertIn("webView.findAllAsync(\n                existingQuery", viewer)
        self.assertIn("val queryLine", viewer)
        self.assertIn("val controls", viewer)
        self.assertIn("LinearLayout.VERTICAL", viewer)

        self.assertIn("intentForDataset", modern)
        self.assertIn("focusEntrypoint", modern)
        self.assertIn("focusEntrypoint", modern)
        self.assertIn("modernVolumeEntrypoint", viewer)
        self.assertIn("smoothScrollTo", modern)


if __name__ == "__main__":
    unittest.main()
