"""Regressions for Samsung status font clipping and terminal foreground cleanup."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class StatusAndNotificationLifecycleContractTests(unittest.TestCase):
    def source(self, name):
        return (SRC / name).read_text(encoding="utf-8")

    def test_running_label_and_counter_never_use_hard_fixed_height(self):
        view = self.source("OperationStatusView.kt")
        self.assertIn("val minRowHeight = Ui.dp(context, 32)", view)
        self.assertIn("detailView.minHeight = minRowHeight", view)
        self.assertIn("counterView.minHeight = minRowHeight", view)
        self.assertIn("LayoutParams.WRAP_CONTENT, 1f", view)
        self.assertIn("Ui.dp(context, 3)", view)
        self.assertNotIn("val rowHeight = Ui.dp(context, 20)", view)

    def test_long_sha_is_explicitly_wrapped_without_clipboard_mutation(self):
        view = self.source("OperationStatusView.kt")
        fmt = self.source("OperationStatusDisplayFormat.kt")
        self.assertIn("digest.chunked(24)", fmt)
        self.assertIn('prefix.trimEnd() + "\\n"', fmt)
        self.assertIn("detailView.setHorizontallyScrolling(false)", view)
        self.assertIn("fullDetail.trim()", view)
        self.assertIn("OperationStatusDisplayFormat.wrapHashesForDisplay(detail)", view)

    def test_native_terminal_progress_removed_not_detached(self):
        native = self.source("NativeRdpkgPreparationService.kt")
        self.assertIn("removeCompletedForegroundStatus()", native)
        self.assertIn("stopForeground(STOP_FOREGROUND_REMOVE)", native)
        self.assertIn("notificationManager().cancel(NOTIFICATION_ID)", native)
        self.assertIn("if (runStore.load().isTerminal) return", native)
        self.assertIn("CompletedNotificationHistory.publish(", native)
        self.assertIn("stopForeground(STOP_FOREGROUND_DETACH)", native) # pause state only

    def test_independent_import_service_clears_its_live_progress(self):
        importer = self.source("RdpkgImportService.kt")
        self.assertIn("stopForeground(STOP_FOREGROUND_REMOVE)", importer)
        self.assertNotIn("stopForeground(STOP_FOREGROUND_DETACH)", importer)

    def test_notification_color_is_only_optional_system_accent(self):
        native = self.source("NativeRdpkgPreparationService.kt")
        importer = self.source("RdpkgImportService.kt")
        history = self.source("CompletedNotificationHistory.kt")
        for v in (native, importer):
            self.assertIn(".setColor(Ui.success)", v)
        self.assertIn("OperationTerminalOutcome.FAILED -> Ui.danger", history)
        self.assertIn("OperationTerminalOutcome.SUCCESS -> Ui.success", history)


if __name__ == "__main__":
    unittest.main()
