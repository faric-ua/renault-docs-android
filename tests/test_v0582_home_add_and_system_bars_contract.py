"""v0.5.82 Home Add and orientation system bar regression contracts."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

def source(name):
    return (APP / name).read_text(encoding="utf-8")

class V0582HomeAndBars(unittest.TestCase):
    def test_orientation_rule_excludes_ime_and_is_both_ways(self):
        ui = source("Ui.kt")
        self.assertIn("fun applyOrientationSystemBars(window: Window?, orientation: Int)", ui)
        self.assertIn("Configuration.ORIENTATION_LANDSCAPE", ui)
        self.assertIn("controller.hide(WindowInsetsCompat.Type.systemBars())", ui)
        self.assertIn("controller.show(WindowInsetsCompat.Type.systemBars())", ui)
        self.assertIn("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE", ui)
        self.assertNotIn("Type.ime()", ui.split("fun applyOrientationSystemBars(window:", 1)[1].split("fun applySystemInsets(", 1)[0])
        app = source("RenaultDocsApplication.kt")
        for marker in ("onActivityCreated(", "onActivityResumed(", "onConfigurationChanged(", "DialogUi.reapplyOrientation"):
            self.assertIn(marker, app)
        self.assertIn("activity is ViewerActivity && activity.isPdfFullscreenActive()", app)
        dialogs = source("DialogUi.kt")
        self.assertIn("registeredDialogs[dialog] = Unit", dialogs)
        self.assertIn("dialog.window", dialogs)
        self.assertIn("Ui.applyOrientationSystemBars(", dialogs)

    def test_config_change_safe_activities_keep_rule(self):
        for file in ("ViewerActivity.kt", "ModernVolumeActivity.kt"):
            s = source(file)
            self.assertIn("override fun onConfigurationChanged(", s)
            self.assertIn("DialogUi.reapplyOrientation(this)", s)
            self.assertIn("Ui.applyOrientationSystemBars(this)", s)

    def test_home_add_is_fixed_and_project_list_independently_scrolls(self):
        main = source("MainActivity.kt")
        layout = main.split("private fun buildContent(): View", 1)[1].split("private fun buildProjectCatalogCard(", 1)[0]
        self.assertIn("root.addView(\n            buildHomeAddPanel()", layout)
        self.assertIn("scrollContent.addView(\n            libraryContainer", layout)
        self.assertNotIn("scrollContent.addView(\n            buildHomeAddPanel()", layout)
        self.assertNotIn("scrollContent.addView(\n            buildProjectCatalogCard()", layout)
        panel = main.split("private fun buildHomeAddPanel(): View", 1)[1].split("private fun buildHomeChoiceCard(", 1)[0]
        for marker in (
            "Додати", "📌", "toggleHomeAddPanel()",
            'settings.homeAddPanelPinned = homeAddPanelPinned',
            'buildHomeChoiceCard("Новий том", "До проєкту", true)',
            'buildHomeChoiceCard("Новий проєкт", "Створити модель", false)',
            "buildProjectCatalogCard()",
            "buildToolsPanel()",
            "legacyAddContainer",
            "BoundedAddActionsScrollView",
            "OperationStatusView(this@MainActivity)",
        ):
            self.assertIn(marker, panel)
        # Status remains outside the scrollable/collapsible action body.
        self.assertLess(panel.index("addView(\n                homeAddPanelBody,"), panel.index("operationStatus = OperationStatusView("))
        self.assertIn("val pinnedHere = homeAddPanelPinned && !landscape", main)
        self.assertIn("homeAddLandscapeExpanded = !homeAddLandscapeExpanded", main)
        self.assertIn("STATE_HOME_ADD_EXPANDED", main)

    def test_home_preserves_data_and_original_navigation(self):
        main = source("MainActivity.kt")
        renderer = main.split("private fun renderLibrary()", 1)[1].split("private fun buildProjectTile(", 1)[0]
        self.assertIn("projectStore.projects()", renderer)
        self.assertIn('value = "Мої Renault"', renderer)
        self.assertIn("buildProjectTile(", renderer)
        self.assertNotIn("buildToolsPanel()", renderer)
        self.assertIn("legacyAddContainer.addView(", renderer)
        for entry in ("chooseProjectForVolume()", "showCreateProjectDialog()",
                      "DriveCatalogActivity::class.java",
                      "ConversionActivity::class.java", "openDatasetPicker()"):
            self.assertIn(entry, main)
        self.assertIn("KEY_HOME_ADD_PANEL_PINNED", source("AppSettings.kt"))
        self.assertIn("KEY_PROJECT_ADD_PANEL_PINNED", source("AppSettings.kt"))

    def test_legacy_is_not_discarded(self):
        main = source("MainActivity.kt")
        self.assertIn("visibleLegacyRecords.forEach", main)
        self.assertIn("buildDatasetTile(record)", main)
        self.assertIn("legacyAddContainer.removeAllViews()", main)
        self.assertIn("legacyAddContainer.addView(", main)
        bound = source("BoundedAddActionsScrollView.kt")
        self.assertIn("View.MeasureSpec.AT_MOST", bound)
        self.assertIn("Configuration.ORIENTATION_LANDSCAPE", bound)

if __name__ == "__main__":
    unittest.main()
