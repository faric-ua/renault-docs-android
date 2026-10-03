from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class OperationStatusUiContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_shared_surface_has_title_detail_progress_and_terminal_close(self):
        ui = self.read("OperationStatusView.kt")
        self.assertIn("class OperationStatusView", ui)
        self.assertIn('"Renault Docs"', ui)
        self.assertIn("progressBarStyleHorizontal", ui)
        self.assertIn("fun showRunning(", ui)
        self.assertIn("fun showTerminal(", ui)
        self.assertIn('contentDescription = "Закрити статус"', ui)
        self.assertIn('"Скасувати"', ui)
        self.assertIn("onCancel: (() -> Unit)? = null", ui)

    def test_project_activity_uses_shared_surface_for_durable_volume_flows(self):
        activity = self.read("ProjectActivity.kt")
        self.assertIn('operationStatus.showRunning("Імпорт тому"', activity)
        self.assertIn('"Створення .rdpkg"', activity)
        self.assertIn("NativeRdpkgPreparationService.requestCancel(this)", activity)
        self.assertIn("if (!::operationStatus.isInitialized)", activity)
        self.assertNotIn('value =\n                            "×"', activity)
        terminal = activity[activity.index("private fun showNativeTerminalStatus"):activity.index("private fun hideNativeTerminalStatus")]
        self.assertIn("operationStatus.showTerminal", terminal)
        self.assertNotIn("nativeTerminalStatusText.text", terminal)
        self.assertIn('operationStatus.showRunning("Підготовка тому"', activity)
        self.assertIn('operationStatus.showRunning("Експорт тому"', activity)
        self.assertIn("statusText.text = DEFAULT_STATUS_TEXT", activity)
        running = activity[activity.index("private fun refreshNativeRunState"):activity.index("private fun showNativeTerminalStatus")]
        self.assertNotIn("updateNativeRunProgressDialog(\n                state,", running)

    def test_home_content_uses_one_scroll_surface_below_top_bar(self):
        activity = self.read("MainActivity.kt")
        build = activity[activity.index("private fun buildContent"):activity.index("private fun buildHomeAddPanel")]
        self.assertIn("val scrollContent =", build)
        self.assertIn("scrollContent.addView(\n            buildHomeAddPanel()", build)
        self.assertIn("scrollContent.addView(statusText)", build)
        self.assertIn("scrollContent.addView(\n            operationStatus", build)
        self.assertIn("scrollContent.addView(\n            libraryContainer", build)
        self.assertIn("scroll.addView(\n            scrollContent", build)
        self.assertNotIn("scroll.addView(\n            libraryContainer", build)

    def test_project_status_is_inside_scrollable_content(self):
        activity = self.read("ProjectActivity.kt")
        build = activity[activity.index("private fun buildContent"):activity.index("private fun buildAddPanel")]
        self.assertIn("val scrollContent =", build)
        self.assertIn("scrollContent.addView(\n            operationStatus", build)
        self.assertIn("scrollContent.addView(\n            volumeContainer", build)
        self.assertIn("projectScroll.addView(\n            scrollContent", build)
        self.assertNotIn("root.addView(\n            operationStatus", build)

    def test_project_content_initializes_scroll_before_use(self):
        activity = self.read("ProjectActivity.kt")
        build = activity[activity.index("private fun buildContent"):activity.index("private fun buildAddPanel")]
        init_pos = build.index("projectScroll =")
        add_pos = build.index("projectScroll.addView(")
        self.assertLess(init_pos, add_pos)
        self.assertIn("ScrollView(this)", build)

    def test_home_project_share_uses_shared_surface(self):
        home = self.read("MainActivity.kt")
        self.assertIn('"Підготовка проєкту"', home)
        self.assertIn("operationStatus.showTerminal(", home)


if __name__ == "__main__":
    unittest.main()
