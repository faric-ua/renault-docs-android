"""v0.5.149 — small fixes in one go (owner 2026-10-17 15:35: «спочатку всі дрібниці підчищаємо»).

- TODO №23 (M30s 13:58): the import card cut the date in the middle («200…6-28») — the stage
  line and the «Цей том» line now wrap to two lines in portrait (kept at two once needed).
- TODO №21 п. 2: while a volume's files are listed (no measure yet) the card says
  «Шукаю файли тому… · Знайдено файлів: N»; the STORED path no longer lists the folder twice.
- TODO №10: Settings → «Залишки обірваних імпортів» — list, then «Прибрати»; only `.import-…`
  and `*.rdpkg.part`, never `.backup-…`; nothing while an import / archive runs.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class SmallFixesContract(unittest.TestCase):
    def test_card_wraps_instead_of_middle_cut(self):
        card = source("OperationStatusView.kt")
        self.assertIn("private var twoLineDetail = false", card)
        self.assertIn("detailView.maxLines = 2", card)
        self.assertIn("keepTwoLinesOnceNeeded()", card)
        secondary = card.split("secondaryLabelView = Ui.textView")[1].split("addView(secondaryLabelView")[0]
        self.assertIn("maxLines = 2", secondary)
        self.assertNotIn("TruncateAt.MIDDLE", secondary)

    def test_listing_counter_and_single_listing(self):
        writer = source("VolumeArchiveWriter.kt")
        self.assertIn("fun listFiles(", writer)
        self.assertIn("val files = files ?: listFiles(root, listing)", writer)
        zip_writer = source("RdpkgZipWriter.kt")
        self.assertIn("VolumeArchiveWriter.listFiles(", zip_writer)
        self.assertIn("files = files,", zip_writer)
        self.assertNotIn(".walkTopDown()", zip_writer.split("fun writeDirectory(")[1].split("private fun writeStored(")[0])
        exporter = source("RdpkgExporter.kt")
        self.assertIn('const val LISTING_STAGE = "Шукаю файли тому…"', exporter)
        self.assertIn('const val LISTING_LABEL = "Знайдено файлів"', exporter)
        self.assertEqual(2, exporter.count("listingReporter(progressState)"))

    def test_leftovers_only_with_yes(self):
        core = source("VolumeTrashCore.kt")
        self.assertIn("fun findLeftovers(", core)
        finder = core.split("fun findLeftovers(")[1].split("fun pendingItems(")[0]
        self.assertNotIn(".backup-", finder.split("Never")[1].split("*/")[1] if "Never" in finder else finder)
        trash = source("VolumeTrash.kt")
        self.assertIn("fun leftoverWriterRunning(", trash)
        clear = trash.split("fun clearLeftovers(")[1].split("/** The last finished erase")[0]
        self.assertIn("if (leftoverWriterRunning(appContext)) return null", clear)
        self.assertIn("VolumeTrashCore.moveIn(", clear)
        self.assertNotIn(".delete()", clear)
        settings = source("SettingsActivity.kt")
        self.assertIn('"Залишки обірваних імпортів"', settings)
        self.assertIn("CONFIRM_LEFTOVERS -> buildLeftoversDialog()", settings)
        dialog = settings.split("private fun buildLeftoversDialog()")[1].split("private fun onLeftoversClicked()")[0]
        self.assertIn("ConfirmActions.confirm(", dialog)
        self.assertIn("VolumeTrash.clearLeftovers(this)", dialog)
        clicked = settings.split("private fun onLeftoversClicked()")[1].split("override fun onDestroy()")[0]
        self.assertNotIn("clearLeftovers", clicked, "a click only shows the list")

    def test_jvm_tests_present(self):
        test = (ROOT / "android/app/src/test/java/com/saney/renaultdocs/ListingAndLeftoversTest.kt").read_text(encoding="utf-8")
        self.assertIn("leftoversAreOnlyUnfinishedWork", test)
        self.assertIn("listFilesKeepsPackageOrderAndReportsCount", test)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 165)


if __name__ == "__main__":
    unittest.main()
