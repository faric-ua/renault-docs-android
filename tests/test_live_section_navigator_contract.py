from pathlib import Path
import unittest


class LiveSectionNavigatorContractTests(unittest.TestCase):
    def test_live_section_navigation_reuses_loaded_runtime(self):
        repo = Path(__file__).resolve().parents[1]

        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        self.assertIn('"Розділи"', viewer)
        self.assertIn("ModernSectionsReader", viewer)
        self.assertIn("showSectionNavigatorDialog", viewer)
        self.assertIn("Пошук: 101, генератор, ABS", viewer)
        self.assertIn("switchLiveSection", viewer)
        self.assertIn("sectionNavigatorSections", viewer)

        self.assertIn("fun switchHybridSection(", client)
        self.assertIn("__renaultLiveSectionSwitch", client)
        self.assertIn("frameByName('org')", client)
        self.assertIn("frameByName('menu')", client)
        self.assertIn("frameByName('nav')", client)
        self.assertIn("targetSelected", client)
        self.assertIn("activeHybridSectionCode", client)

        switch_block = client.split("fun switchHybridSection(", 1)[1].split(
            "fun collectFrameDebugReport(", 1
        )[0]
        self.assertNotIn("view.loadUrl(", switch_block)

    def test_timing_diagnostics_are_exposed(self):
        repo = Path(__file__).resolve().parents[1]
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("SystemClock.elapsedRealtime()", client)
        self.assertIn("fastPackPrepareStartedAtMs", client)
        self.assertIn("fastPackCopiedToLocalCache", client)
        self.assertIn("runtimeTiming", client)
        self.assertIn("navigationTiming", client)
        self.assertIn("durationMs", client)


if __name__ == "__main__":
    unittest.main()
