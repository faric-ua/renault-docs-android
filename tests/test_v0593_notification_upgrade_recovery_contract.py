"""Completed notification history survives an in-place APK update without fabricated legacy entries."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

def read(name): return (SRC / name).read_text(encoding="utf-8")

class CompletedNotificationUpgradeTests(unittest.TestCase):
    def test_records_include_real_content_and_dismissal(self):
        history = read("CompletedNotificationHistory.kt")
        self.assertIn('private const val KEY_RECORD_PREFIX = "result_record_"', history)
        self.assertIn(".putString(KEY_RECORD_PREFIX + slot, encode(record))", history)
        self.assertIn('.put("title", record.title)', history)
        self.assertIn('.put("text", record.text)', history)
        self.assertIn('.put("project_id", record.projectId)', history)
        self.assertIn('.put("dismissed", record.dismissed)', history)
        self.assertIn("KEY_SEQUENCE", history)
        self.assertIn("CompletedNotificationSlotPolicy.MAX_RESULTS", history)

    def test_no_fake_recovery_of_preexisting_id_only_history(self):
        history = read("CompletedNotificationHistory.kt")
        self.assertIn("if (previousVersion < 0)", history)
        self.assertIn("return", history)
        self.assertIn("prefs.getString(KEY_SLOT_PREFIX + slot, null) == it.eventKey", history)
        self.assertIn("CompletedNotificationRecoveryPolicy.missingUndismissed(", history)
        self.assertIn("manager.activeNotifications.map { it.id }", history)

    def test_user_dismissal_and_recovery_are_separate(self):
        history = read("CompletedNotificationHistory.kt")
        policy = read("CompletedNotificationRecoveryPolicy.kt")
        app = read("RenaultDocsApplication.kt")
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("setDeleteIntent(delete)", history)
        self.assertIn("ACTION_DISMISS_RESULT", history)
        self.assertIn("ACTION_DISMISS_GROUP", history)
        self.assertIn("record.copy(dismissed = true)", history)
        self.assertIn("!it.dismissed", policy)
        self.assertIn("CompletedNotificationHistory.restoreAfterPackageUpdate(activity)", app)
        self.assertIn('android:name=".CompletedNotificationDismissReceiver"', manifest)
        self.assertIn('android:exported="false"', manifest)

    def test_does_not_restart_conversion_or_misuse_progress_namespace(self):
        history = read("CompletedNotificationHistory.kt")
        self.assertNotIn("startForeground(", history)
        self.assertNotIn("startService(", history)
        self.assertNotIn("startForegroundService(", history)
        self.assertIn("CompletedNotificationSlotPolicy.SUMMARY_ID", history)
        self.assertIn("CompletedNotificationSlotPolicy.notificationId(record.sequence)", history)
        self.assertIn("if (previousVersion == currentVersion) return", history)
        self.assertIn("if (!manager.areNotificationsEnabled()) return", history)

if __name__ == "__main__":
    unittest.main()
