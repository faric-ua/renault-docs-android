"""v0.5.126 — sound when a long operation ends.

Owner 2026-10-13 21:27: «щоб ти додав якийсь звук, коли він доробив якусь
операцію в шторці, або якась помилка — щоб звукові повідомлення були».
Progress stays silent; only the final notification moves to a sound channel.
A cancel the owner pressed himself stays silent.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


class SoundChannels(unittest.TestCase):
    def test_two_new_sound_channels(self):
        alert = read("OperationAlert.kt")
        self.assertIn('"renault_alert_done_v1"', alert)
        self.assertIn('"renault_alert_error_v1"', alert)
        self.assertIn("NotificationManager.IMPORTANCE_DEFAULT", alert)
        self.assertIn("NotificationManager.IMPORTANCE_HIGH", alert)
        self.assertNotIn("IMPORTANCE_LOW", alert)
        # the final notification replaces the progress one: it must be allowed to alert
        self.assertIn(".setOnlyAlertOnce(false)", alert)
        self.assertIn("OperationTerminalOutcome.CANCELLED -> Kind.SILENT", alert)

    def test_every_operation_service_sounds_only_on_its_final_notification(self):
        for name, var in (
            ("RdpkgShareService.kt", "text"),
            ("RdpkgExportService.kt", "text"),
            ("RdprojectShareService.kt", "text"),
            ("CatalogImportService.kt", "text"),
            ("ProjectFolderExportService.kt", "message"),
        ):
            source = read(name)
            builder = source.split("private fun notification(", 1)[1].split(".build()", 1)[0]
            self.assertIn(f"OperationAlert.alertIfFinal(this, it, ongoing, {var})", builder, name)
            # progress notifications keep «alert once» and the silent channel
            self.assertIn(".setOnlyAlertOnce", builder, name)
            self.assertLess(builder.index(".setOnlyAlertOnce"), builder.index("OperationAlert.alertIfFinal"), name)

    def test_alert_if_final_leaves_progress_untouched(self):
        alert = read("OperationAlert.kt")
        self.assertIn("if (ongoing) builder else terminal(context, builder, text)", alert)

    def test_completed_history_sounds_for_new_results_not_restored_ones(self):
        history = read("CompletedNotificationHistory.kt")
        publish = history.split("private fun publishSafely(", 1)[1].split("fun restoreAfterPackageUpdate(", 1)[0]
        self.assertIn("buildChild(app, record, alert = true)", publish)
        restore = history.split("fun restoreAfterPackageUpdate(", 1)[1].split("private fun ", 1)[0]
        self.assertIn("buildChild(app, record)", restore)
        self.assertNotIn("alert = true", restore)
        self.assertIn("if (alert) OperationAlert.terminal(context, it, record.title) else it", history)


class Version(unittest.TestCase):
    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn("versionName = \"0.5.1", gradle)
        self.assertEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)) >= 142, True)


if __name__ == "__main__":
    unittest.main()
