"""v0.5.104: every stop / clear / reset action asks once (faric, 2026-10-12).

Phone QA on the converter: «Скасувати конвертацію» stopped the run at once,
with no question. Owner rule: wherever something is deleted or an action is
cancelled, the app asks first. Audit found six places without a question:

1. Converter — «Скасувати конвертацію».
2. Converter — «Очистити вибір».
3. Project screen status card — «Скасувати» while a volume is being created.
4. «Томи в архіві» chooser — «Скасувати».
5. Notification — «Скасувати» (a notification button cannot ask, so it now
   opens the project screen, which asks).
6. Settings — «Скинути вибір» of the backup folder.

Already confirmed before v0.5.104 (kept): delete project, remove volume,
delete prepared .rdpkg/.rdproject, stop folder export, stop .rdproject.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


def between(text, start, end):
    return text.split(start, 1)[1].split(end, 1)[0]


class ConfirmCancelAndClearContract(unittest.TestCase):
    def test_shared_confirmation_keeps_safe_default(self):
        shared = read("ConfirmActions.kt")
        self.assertIn('STOP_LABEL = "Зупинити"', shared)
        self.assertIn('KEEP_RUNNING_LABEL = "Продовжити"', shared)
        # «Keep» does nothing; only the confirm button runs the action.
        self.assertIn(".setNegativeButton(keepLabel, null)", shared)
        self.assertIn(".setPositiveButton(confirmLabel) { _, _ -> onConfirm() }", shared)
        self.assertIn("DialogUi.apply(dialog, DialogRole.DANGER)", shared)

    def test_converter_cancel_asks_first(self):
        activity = read("ConversionActivity.kt")
        button = between(activity, '"Скасувати конвертацію"', "body.addView(")
        self.assertIn("confirmCancelConversion()", button)
        self.assertNotIn("requestCancel", button)
        confirm = between(activity, "private fun confirmCancelConversion()", "private fun confirmClearSelection()")
        self.assertIn("ConfirmActions.confirmStop(", confirm)
        self.assertIn('"Зупинити конвертацію?"', confirm)
        self.assertIn("ConversionService\n                .requestCancel(", confirm)
        # A run that finished while the question was open is not "cancelled".
        self.assertIn("if (!runStore.load().isRunning)", confirm)

    def test_converter_clear_asks_first(self):
        activity = read("ConversionActivity.kt")
        button = between(activity, '"Очистити вибір"', "body.addView(")
        self.assertIn("confirmClearSelection()", button)
        self.assertNotIn("draftStore.clear()", button)
        confirm = between(activity, "private fun confirmClearSelection()", "private fun startConversion()")
        self.assertIn('"Очистити вибір?"', confirm)
        self.assertIn("Файли на телефоні не видаляються.", confirm)
        self.assertIn("draftStore.clear()", confirm)

    def test_project_status_cancel_asks_first(self):
        project = read("ProjectActivity.kt")
        card = between(project, "if (state.phase == NativeRdpkgRunPhase.PREPARING) {", "} else {")
        self.assertIn("confirmCancelNativePreparation()", card)
        self.assertNotIn("requestCancel", card)
        confirm = between(project, "private fun confirmCancelNativePreparation()", "private fun showArchiveVolumeChooser(")
        self.assertIn('"Зупинити створення тому?"', confirm)
        self.assertIn("NativeRdpkgPreparationService.requestCancel(this)", confirm)

    def test_archive_chooser_cancel_asks_first(self):
        project = read("ProjectActivity.kt")
        chooser = between(project, '"Томи в архіві"', "private fun showNativeTerminalStatus(")
        # The system negative button no longer cancels by itself.
        self.assertIn('"Скасувати",\n                    null,', chooser)
        self.assertIn("AlertDialog.BUTTON_NEGATIVE", chooser)
        self.assertIn('"Скасувати вибір томів?"', chooser)
        # Cancel first, then close, so the refresh loop cannot reopen it.
        tail = chooser.split('"Скасувати вибір томів?"', 1)[1]
        self.assertLess(tail.index(".requestCancel("), tail.index("dialog.dismiss()"))

    def test_notification_cancel_opens_question_instead_of_stopping(self):
        service = read("NativeRdpkgPreparationService.kt")
        builder = between(service, "private fun buildNotification(", "private fun ensureNotificationChannel()")
        self.assertIn("ProjectActivity.confirmCancelIntent(", builder)
        self.assertIn("PendingIntent.getActivity(", builder)
        self.assertNotIn("PendingIntent.getService(", builder)
        project = read("ProjectActivity.kt")
        self.assertIn('EXTRA_CONFIRM_CANCEL =\n            "confirmCancel"', project)
        on_create = between(project, "intent.getBooleanExtra(\n                EXTRA_CONFIRM_CANCEL,", "override fun onStart()")
        self.assertIn("confirmCancelNativePreparation()", on_create)
        # Not asked again after rotation.
        self.assertIn("intent.removeExtra(EXTRA_CONFIRM_CANCEL)", on_create)

    def test_settings_backup_reset_asks_first(self):
        settings = read("SettingsActivity.kt")
        button = between(settings, '"Скинути вибір"', "backupResetButton,")
        self.assertIn("ConfirmActions.confirm(", button)
        self.assertIn('"Скинути папку резервних копій?"', button)
        self.assertLess(button.index("ConfirmActions.confirm("), button.index(".clearBackupFolder()"))

    def test_home_stop_questions_use_the_same_dialog(self):
        home = read("HomeProjectDialogController.kt")
        confirm = between(home, "fun confirmCancelOperation(", "private fun track(")
        self.assertIn("ConfirmActions.confirmStop(", confirm)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.104"', gradle)
        self.assertIn("versionCode = 120", gradle)


if __name__ == "__main__":
    unittest.main()
