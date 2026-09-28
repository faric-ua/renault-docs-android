from pathlib import Path
import unittest


class DeterministicFrameProjectionContractTests(unittest.TestCase):
    def test_phone_frame_tree_drives_named_projection(self):
        repo = Path(__file__).resolve().parents[1]
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        # v0.5.5 phone evidence showed that INDEX.HTM settles on ENTREE.HTM,
        # so the hybrid shell must be injected for the resulting legacy HTML,
        # not only for the original root entrypoint.
        self.assertIn(
            "activeHybridSectionCode.isNotBlank() &&",
            client,
        )
        self.assertIn(
            "isLegacyHtml(\n                relativePath,",
            client,
        )

        # Real NT8183A frame names from the phone capture.
        self.assertIn("frameByName('titre')", client)
        self.assertIn("frameByName('org')", client)
        self.assertIn("frameByName('menu')", client)
        self.assertIn("frameByName('nav')", client)
        self.assertIn("frameByName('doc')", client)

        # org is the section-code branch; menu/nav/doc stay alive.
        self.assertIn("findTargetLeaf", client)
        self.assertIn("targetSelected", client)
        self.assertIn("projectClassicNavigationBranch", client)
        self.assertIn("preservedFrames", client)
        self.assertIn("'menu'", client)
        self.assertIn("'nav'", client)
        self.assertIn("'doc'", client)

        # Collapse the entire left nested FRAMESET at its outer geometry
        # boundary instead of zeroing only the org child row.
        self.assertIn("const leftBranch =", client)
        self.assertIn("const outer =", client)
        self.assertIn("parts[branchIndex] =", client)
        self.assertIn("'0';", client)
        self.assertIn("MutationObserver", client)
        self.assertIn("state.phase =", client)
        self.assertIn("'projected';", client)


if __name__ == "__main__":
    unittest.main()
