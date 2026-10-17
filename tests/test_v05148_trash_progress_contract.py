"""v0.5.148 — owner 2026-10-17 05:25 (after deleting 23 volumes on M30s): «як подивитися, чи
вони досі стираються… і скільки це времені займе… чому немає прогрес-бара?»

Contract:
- the eraser counts the files of each volume first, then erases from that list — the share
  done and the time left are known («Том 5 з 23 · 34 % · залишилось ~12 хв»);
- it runs in a foreground service with a bar in the shade (keeps going with the screen off);
- the home screen has its own card (next to import cards) and shows the result until closed;
- «Файли 23 томів» — the right case.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class TrashProgressContract(unittest.TestCase):
    def test_core_counts_then_erases(self):
        core = source("VolumeTrashCore.kt")
        self.assertIn("data class EraseProgress(", core)
        self.assertIn("val remainingMs: Long?", core)
        self.assertIn("val order = ArrayList<Pair<File, Boolean>>()", core)
        self.assertIn("Files.isSymbolicLink", core)

    def test_service_with_bar_in_the_shade(self):
        service = source("VolumeTrashService.kt")
        self.assertIn("startForeground(NOTIFICATION_ID", service)
        self.assertIn("setProgress(1000,", service)
        self.assertIn("VolumeDeleteChoice.eraseCounter(step)", service)
        self.assertIn("override fun onTimeout(", service)
        self.assertNotIn(".delete()", service, "the service only calls the eraser")
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name=".VolumeTrashService"', manifest)
        trash = source("VolumeTrash.kt")
        self.assertIn("startForegroundService(Intent(appContext, VolumeTrashService::class.java))", trash)

    def test_home_card(self):
        main = source("MainActivity.kt")
        self.assertIn("private lateinit var trashStatus: OperationStatusView", main)
        self.assertIn("refreshTrashCard()", main)
        card = main.split("private fun refreshTrashCard()")[1].split("private fun requestNotificationPermissionIfNeeded")[0]
        self.assertIn("VolumeTrash.progress", card)
        self.assertIn("VolumeDeleteChoice.eraseCounter(step)", card)
        self.assertIn("VolumeTrash.lastSummary(this)", card)
        self.assertIn("VolumeTrash.dismissSummary(", card)
        self.assertNotIn("listFiles", card, "the card reads memory only (refreshed every 100 ms)")

    def test_words(self):
        words = source("VolumeDeleteChoice.kt")
        self.assertIn("fun volumeCountOf(count: Int)", words)
        self.assertIn("append(volumeCountOf(outcome.withFiles.size))", words)
        self.assertIn('"Стираю файли видалених томів"', words)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 164)


if __name__ == "__main__":
    unittest.main()
