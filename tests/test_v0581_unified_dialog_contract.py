"""Contract checks for canonical Renault Docs Android dialogs (v0.5.81)."""

from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

# UI sources with AlertDialog.Builder in the v0.5.81 audit.
DIALOG_COUNTS = {
    "ProjectActivity.kt": 10,
    "HomeProjectDialogController.kt": 4,
    "SettingsActivity.kt": 3,
    "LifecycleHelpDialogController.kt": 1,
    "ViewerActivity.kt": 2,
}

BUILDER = re.compile(r"\bAlertDialog\s*\.\s*Builder\s*\(")


class UnifiedDialogContract(unittest.TestCase):
    def test_every_android_alert_dialog_uses_dialog_ui(self):
        for name, expected in DIALOG_COUNTS.items():
            source = (SOURCE / name).read_text(encoding="utf-8")
            found = list(BUILDER.finditer(source))
            self.assertEqual(expected, len(found), name)
            for index, match in enumerate(found):
                end = found[index + 1].start() if index + 1 < len(found) else len(source)
                block = source[match.start():end]
                self.assertIn(".create()", block, (name, index))
                self.assertRegex(block, r"DialogUi\s*\.\s*apply\s*\(", (name, index))

    def test_viewer_chrome_uses_same_style_as_project_and_help_dialogs(self):
        viewer = (SOURCE / "ViewerActivity.kt").read_text(encoding="utf-8")
        sections = viewer.split("private fun showSectionNavigatorDialog()", 1)[1]
        sections = sections.split("private fun switchLiveSection", 1)[0]
        report = viewer.split("private fun showFrameDebugDialog(", 1)[1]
        report = report.split("private fun openHome()", 1)[0]
        self.assertIn("DialogUi.apply(dialog, DialogRole.CHOICE)", sections)
        self.assertIn("DialogUi.apply(dialog, DialogRole.HELP)", report)

    def test_preflight_stays_short_and_details_expand_without_new_action(self):
        project = (SOURCE / "ProjectActivity.kt").read_text(encoding="utf-8")
        preflight = project.split("private fun showArchiveSourcePreflight()", 1)[1]
        preflight = preflight.split("private fun handleNativeRdpkgDestinationResult(", 1)[0]
        for required in (
            "Можливий дублікат",
            "Вміст архівів не порівнювався.",
            "Технічні деталі ▼",
            "Фактичний Document ID:",
            "не серед усіх файлів телефона",
            "addView(detailsToggle)",
            "addView(detailsText)",
            'setTitle("Підтвердь джерело архіву")',
            'setPositiveButton("Продовжити")',
            'setNegativeButton("Скасувати")',
            "pendingNativeSourceUri == source.toString()",
            "DialogUi.apply(dialog, DialogRole.CONFIRM)",
        ):
            self.assertIn(required, preflight)
        self.assertNotIn("setNeutralButton(", preflight)
        self.assertIn("archiveSourceDetailsExpanded = !archiveSourceDetailsExpanded", preflight)

    def test_details_and_confirm_state_survive_rotation(self):
        project = (SOURCE / "ProjectActivity.kt").read_text(encoding="utf-8")
        self.assertIn('getBoolean("archiveSourceDetailsExpanded", false)', project)
        self.assertIn('outState.putBoolean("archiveSourceDetailsExpanded", archiveSourceDetailsExpanded)', project)
        self.assertIn('outState.putBoolean("archiveSourcePreflightOpen", archiveSourcePreflightOpen)', project)
        self.assertIn("if (archiveSourcePreflightOpen && !pendingNativeSourceUri.isNullOrBlank())", project)

    def test_canonical_roles_keep_danger_different_from_regular_confirmation(self):
        dialog_ui = (SOURCE / "DialogUi.kt").read_text(encoding="utf-8")
        for role in ("HELP", "CHOICE", "CONFIRM", "DANGER", "PROGRESS"):
            self.assertIn(role, dialog_ui)
        self.assertIn("DialogRole.DANGER,", dialog_ui)
        self.assertIn("else -> Ui.accent", dialog_ui)


if __name__ == "__main__":
    unittest.main()
