"""Renodocs 0.6.4 (owner 2026-10-18 05:36, screenshot of the shade after
updating to 0.6.3): «время одинакове… я два томи додав, а записів 4».

Cause: Android removes the app's notifications when the APK is replaced;
CompletedNotificationHistory.restoreAfterPackageUpdate posts the old results
again — and without a stored time the shade showed the re-post moment, so the
three earlier imports (04:0x) looked like new ones at 05:34.

Also found while reading: the import service marked itself free only after the
project screen could already see «done» — with several volumes the next start
could arrive too early. Now: free first, then write the result, and stop only if
no newer start came in.
"""

import unittest
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / f"{name}.kt").read_text(encoding="utf-8")


class NotificationTime(unittest.TestCase):
    def test_record_keeps_finish_time(self):
        policy = read("CompletedNotificationRecoveryPolicy")
        self.assertIn("val finishedAtMs: Long = 0L", policy)
        history = read("CompletedNotificationHistory")
        self.assertIn("finishedAtMs = System.currentTimeMillis()", history)
        self.assertIn('.put("finished_at_ms", record.finishedAtMs)', history)
        self.assertIn('finishedAtMs = json.optLong("finished_at_ms", 0L)', history)

    def test_shade_shows_real_time_or_none(self):
        history = read("CompletedNotificationHistory")
        start = history.index("private fun buildChild(")
        child = history[start:history.index("private fun buildSummary(")]
        self.assertIn("setWhen(record.finishedAtMs)", child)
        self.assertIn("setShowWhen(false)", child)


class ImportServiceHandoff(unittest.TestCase):
    def test_free_before_result_and_stop_only_latest(self):
        service = read("RdpkgImportService")
        free = service.index("workerRunning.set(false)")
        result = service.index("finish()")
        self.assertLess(free, result)
        self.assertIn("if (stopSelfResult(startId))", service)
        worker = service[service.index("Thread {"):service.index("}.start()")]
        self.assertNotIn("stopSelf()", worker)

    def test_no_success_after_timeout(self):
        service = read("RdpkgImportService")
        guard = service.index("if (dataSyncTimeout.isExpired) return@Thread")
        self.assertLess(guard, service.index("finish()"))


if __name__ == "__main__":
    unittest.main()
