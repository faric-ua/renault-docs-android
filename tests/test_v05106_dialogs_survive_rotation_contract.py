"""v0.5.106: every question/message window survives a screen rotation.

Phone QA (faric, 2026-10-12): the confirmation windows added in v0.5.104 and
v0.5.105 disappeared when the phone was rotated. Android recreates the screen
on rotation and silently drops an open dialog unless the screen remembers it.

Fix: RestorableDialogs — each window has a stable key saved with the screen
state; after rotation the same window is rebuilt from current data. Audit of
ALL app dialogs (each must have a restore path):

  ConversionActivity   cancelConversion, clearSelection        -> RestorableDialogs
  DriveCatalogActivity cancelCatalogImport                     -> RestorableDialogs
  SettingsActivity     resetBackupFolder                       -> RestorableDialogs
                       mode / PDF zoom / PDF step choosers     -> activeDialogKind (older)
  MainActivity         stopFolderExport, stopRdproject         -> RestorableDialogs
  Home controller      export messages (showMessage)           -> RestorableDialogs.showInfo
                       actions / remove / delete ⇧ / wizard    -> activeKind (older)
  ProjectActivity      stopImport/Export/Share, cancelNative,
                       cancelArchiveSelection, sourceReport,
                       «Неправильне джерело»                   -> RestorableDialogs
                       volume actions / remove / move / ⇧ /
                       chooser / mismatch                      -> activeProjectDialogKind (older)
                       archive source preflight                -> archiveSourcePreflightOpen (older)
                       «Томи в архіві» chooser                 -> refresh loop while waiting (older)
  Help (?) windows                                             -> LifecycleHelpDialogController (older)
  ViewerActivity / ModernVolumeActivity                        -> not recreated (configChanges)
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


SCREENS = {
    # v0.5.112: converter removed (archive branch archive/legacy-converter-v0.5.111).
    "DriveCatalogActivity.kt": "confirmDialogs",
    "SettingsActivity.kt": "confirmDialogs",
    "MainActivity.kt": "confirmDialogs",
    "ProjectActivity.kt": "projectConfirms",
}


class DialogsSurviveRotationContract(unittest.TestCase):
    def test_restorable_dialogs_keeps_key_only_across_rotation(self):
        host = read("RestorableDialogs.kt")
        listener = between(host, "dialog.setOnDismissListener {", "\n        }\n")
        # A real close forgets the window; a rotation keeps it.
        self.assertIn("!activity.isChangingConfigurations", listener)
        # A window replaced by a newer one never forgets the newer key.
        self.assertIn("if (current !== dialog) return@setOnDismissListener", listener)
        self.assertIn('outState.putString("$stateKey.key", openKey)', host)
        self.assertIn('openKey = state?.getString("$stateKey.key")', host)
        restore_open = between(host, "fun restoreOpen()", "\n    }\n")
        self.assertIn("show(key)", restore_open)
        # Message text is saved too, so a message returns word for word.
        for field in ("infoTitle", "infoMessage", "infoButton", "infoRole"):
            self.assertIn(f'outState.putString("$stateKey.{field}", {field})', host)
        # The info window uses the shared look.
        self.assertIn("DialogUi.apply(dialog, role)", host)

    def test_every_screen_wires_save_restore_reopen_and_destroy(self):
        for name, field in SCREENS.items():
            screen = read(name)
            with self.subTest(screen=name):
                self.assertIn(f"lateinit var {field}: RestorableDialogs", screen)
                self.assertIn(f"{field}.restore(\n            savedInstanceState,\n        )", screen)
                self.assertRegex(screen, rf"{field}\.save\(\s*outState,\s*\)")
                self.assertRegex(screen, rf"\.post \{{\s*{field}\.restoreOpen\(\)\s*\}}")
                self.assertIn(f"{field}.onDestroy()", screen)

    def test_no_confirmation_is_shown_outside_a_keyed_builder(self):
        """ConfirmActions windows are created only inside build* functions."""
        call = re.compile(r"ConfirmActions\s*\.\s*confirm(Stop)?\(")
        for path in ANDROID.glob("*.kt"):
            if path.name == "ConfirmActions.kt":
                continue
            text = path.read_text(encoding="utf-8")
            for match in call.finditer(text):
                head = text[:match.start()]
                fun = re.findall(r"\n    (?:private )?fun (\w+)", head)
                with self.subTest(file=path.name, at=match.start()):
                    self.assertTrue(fun, path.name)
                    self.assertTrue(
                        fun[-1].startswith("build"),
                        f"{path.name}: ConfirmActions call inside {fun[-1]}() is not restorable",
                    )

    def test_keys_route_to_the_right_windows(self):
        project = read("ProjectActivity.kt")
        builder = between(project, "private fun buildProjectConfirm(key: String)", "private fun buildStopOperationDialog(")
        for key in (
            "CONFIRM_STOP_IMPORT",
            "CONFIRM_STOP_EXPORT",
            "CONFIRM_STOP_SHARE",
            "CONFIRM_CANCEL_ARCHIVE",
            "CONFIRM_CANCEL_NATIVE",
            "CONFIRM_SOURCE_REPORT",
        ):
            with self.subTest(key=key):
                self.assertIn(f"{key} ->", builder)
                self.assertIn(f"projectConfirms.show({key})", project)
        main = read("MainActivity.kt")
        for key in ("CONFIRM_STOP_FOLDER_EXPORT", "CONFIRM_STOP_RDPROJECT"):
            self.assertIn(f"confirmDialogs.show({key})", main)
            self.assertIn(f"{key} ->", main)
        # v0.5.112: converter removed (archive branch archive/legacy-converter-v0.5.111).
        self.assertIn("CONFIRM_CANCEL_IMPORT) buildCancelImportDialog()", read("DriveCatalogActivity.kt"))
        # v0.5.149: Settings routes two keys (+ «Залишки обірваних імпортів») with `when`.
        self.assertIn("CONFIRM_RESET_BACKUP -> buildResetBackupDialog()", read("SettingsActivity.kt"))

    def test_stop_questions_skip_finished_operations_after_rotation(self):
        # v0.5.112: converter removed (archive branch archive/legacy-converter-v0.5.111).
        self.assertIn("if (!importStore.load().isRunning) return null", read("DriveCatalogActivity.kt"))
        main = between(read("MainActivity.kt"), "private fun buildConfirmDialog(key: String)", "override fun onDestroy()")
        self.assertIn("if (!folderExportRunStore.load().isRunning)", main)
        self.assertIn("if (!rdprojectShareRunStore.load().isRunning)", main)
        project = read("ProjectActivity.kt")
        self.assertIn("if (!running) return null", project)
        self.assertIn("if (!nativeRunStore.load().isWaitingForSelection) return null", project)
        self.assertIn("if (!nativeRunStore.load().isRunning) return null", project)

    def test_archive_chooser_stays_closed_while_its_cancel_question_is_open(self):
        project = read("ProjectActivity.kt")
        chooser = between(project, "private fun showArchiveVolumeChooser(", "val candidates =")
        self.assertIn("projectConfirms.isOpen(CONFIRM_CANCEL_ARCHIVE)", chooser)

    def test_messages_are_restorable(self):
        project = read("ProjectActivity.kt")
        rejected = between(project, "private fun showArchiveSourceRejected(message: String)", "\n    }\n")
        self.assertIn("projectConfirms.showInfo(", rejected)
        self.assertNotIn("AlertDialog.Builder", rejected)
        home = read("HomeProjectDialogController.kt")
        message = between(home, "private fun showMessage(", "\n    }\n")
        self.assertIn("infoDialogs.showInfo(", message)
        self.assertIn("infoDialogs.save(outState)", home)
        self.assertIn("infoDialogs.restore(state)", home)
        self.assertIn("infoDialogs.restoreOpen()", home)

    def test_older_dialogs_keep_their_restore_paths(self):
        project = read("ProjectActivity.kt")
        restore = between(project, "private fun restoreProjectDialog()", "private fun restorePreparedVolumeChooser()")
        for kind in ("DIALOG_VOLUME_ACTIONS", "DIALOG_REMOVE_VOLUME", "DIALOG_MOVE_VOLUME",
                     "DIALOG_DELETE_PREPARED_VOLUME", "DIALOG_VOLUME_CHOOSER", "DIALOG_PROJECT_MISMATCH"):
            self.assertIn(kind, restore)
        self.assertIn("showArchiveSourcePreflight()", between(project, "archiveSourcePreflightOpen &&", "}"))
        home = read("HomeProjectDialogController.kt")
        reopen = between(home, "fun restoreOpen()", "fun showActions(")
        for kind in ("DIALOG_ACTIONS", "DIALOG_REMOVE", "DIALOG_DELETE_PREPARED", "DIALOG_EXPORT_WIZARD"):
            self.assertIn(kind, reopen)
        settings = read("SettingsActivity.kt")
        for chooser in ("showModeChooser()", "showPdfZoomChooser()", "showPdfStepChooser()"):
            self.assertIn(chooser, between(settings, "when (activeDialogKind) {", "}"))
        manifest = MANIFEST.read_text(encoding="utf-8")
        for activity in (".ViewerActivity", ".ModernVolumeActivity"):
            block = between(manifest, f'android:name="{activity}"', "/>")
            self.assertIn('android:configChanges="orientation|screenSize"', block)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 122)


if __name__ == "__main__":
    unittest.main()
