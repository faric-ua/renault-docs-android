from pathlib import Path
import unittest


class AndroidViewerContractTests(unittest.TestCase):
    def test_viewer_uses_saf_virtual_origin_not_placeholder(self):
        repo = Path(__file__).resolve().parents[1]
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")

        self.assertIn("WebView(this)", viewer)
        self.assertIn("DatasetVirtualUrl.urlFor(entrypoint)", viewer)
        self.assertIn("webView.restoreState", viewer)
        self.assertIn("webView.canGoBack()", viewer)
        self.assertIn("SafDatasetResolver", client)
        self.assertIn("shouldInterceptRequest", client)
        self.assertIn("AndroidPdfLayer", client)
        self.assertIn("legacyStandaloneMode", client)
        self.assertIn("applyStandaloneLegacyCompat", client)
        self.assertIn("backgroundColor =", client)
        self.assertIn("a[target], form[target], base[target]", client)
        self.assertIn("MutationObserver", client)
        self.assertIn("__renaultStandaloneOpenPatched", client)
        self.assertIn("__renaultFrameBridgeHost", client)
        self.assertIn("ensureNumericBridges", client)
        self.assertIn("ensureNamedBridge", client)
        self.assertIn("__renaultSelectFallback", client)
        self.assertIn("__renaultClickFallback", client)
        self.assertIn("extractCandidate", client)
        self.assertIn("discoverFrameReferences", client)
        self.assertIn("legacyStandaloneMode =", viewer)
        self.assertIn("legacySectionCode =", viewer)
        self.assertIn("legacySectionRootEntrypoint =", viewer)
        self.assertIn("applyHybridSectionShell", client)
        self.assertIn("findNavigationWindow", client)
        self.assertIn("findTargetLeaf", client)
        self.assertIn("triggerSection", client)
        self.assertIn("targetSelected", client)
        self.assertIn("projectClassicNavigationBranch", client)
        self.assertIn("frameByName('org')", client)
        self.assertIn("frameByName('titre')", client)
        self.assertIn("frameByName('menu')", client)
        self.assertIn("frameByName('nav')", client)
        self.assertIn("frameByName('doc')", client)
        self.assertIn("pollHybridSectionReady", client)
        self.assertIn("onHybridSectionReady", client)
        self.assertIn("hybridSectionWarmup", viewer)
        self.assertIn("webView.animate()", viewer)
        self.assertIn("alpha =", viewer)
        self.assertIn("switchHybridSection", client)
        self.assertIn("__renaultLiveSectionSwitch", client)
        self.assertIn("activeHybridSectionCode", client)
        self.assertIn("fastPackPrepareMs", client)
        self.assertIn("showSectionNavigator", viewer)
        self.assertIn("showSectionNavigatorDialog", viewer)
        self.assertIn("switchLiveSection", viewer)
        self.assertIn("ModernSectionsReader", viewer)
        self.assertIn('"Розділи"', viewer)
        self.assertIn("collectFrameDebugReport", client)
        self.assertIn("Frame debug", viewer)
        self.assertIn("showFrameDebugReport", viewer)
        self.assertIn("ClipboardManager", viewer)
        self.assertIn("android.permission.INTERNET", manifest)
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", manifest)


if __name__ == "__main__":
    unittest.main()
