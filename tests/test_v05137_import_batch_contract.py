"""v0.5.137 — TODO №11, №12, №13, №15 and stage 0 of the read-from-.rdpkg plan (№16).

Phone QA (M30s, import of 18 Megane II volumes to the memory card, 2026-10-15/16):
- №11: the import notification could not be cancelled;
- №12: volume files on the card must not be indexed (owner's rule 2026-10-15);
- №13: a run broken off between «unpacked» and «added» leaves an invisible volume;
- №15: «Етап: 100%» while the last volume installed for 2 more hours;
- №16: card volumes open very slowly — measure first (stage 0).
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class ImportBatchContract(unittest.TestCase):
    def test_notification_has_cancel_with_the_same_question(self):
        service = source("RdprojectImportService.kt")
        self.assertIn('"Скасувати"', service)
        self.assertIn("RdprojectImportActivity.askStopIntent(", service)
        self.assertIn("!runStore.isCancelRequested()", service)
        activity = source("RdprojectImportActivity.kt")
        self.assertIn("fun askStopIntent(", activity)
        self.assertIn("EXTRA_ASK_STOP", activity)
        self.assertIn("confirmDialogs.show(CONFIRM_STOP)", activity)

    def test_card_volume_folder_gets_nomedia_first(self):
        storage = source("VolumeStorage.kt")
        self.assertIn('const val NO_MEDIA = ".nomedia"', storage)
        self.assertIn("fun ensureNoMedia(", storage)
        self.assertIn("fun ensureCardNoMedia(", storage)
        importer = source("RdpkgImporter.kt")
        nomedia = importer.index("VolumeStorage.ensureNoMedia(packagesDirectory)")
        staging = importer.index('".import-"')
        self.assertLess(nomedia, staging, ".nomedia must be written before the first volume file")
        app = source("RenaultDocsApplication.kt")
        self.assertIn("VolumeStorage.ensureCardNoMedia(this)", app)
        # Nothing is deleted by the marker code.
        start = storage.index("fun ensureNoMedia(")
        end = storage.index("fun ensureCardNoMedia(")
        self.assertNotIn("delete", storage[start:end])

    def test_invisible_volume_is_recovered_not_unpacked_again(self):
        service = source("RdprojectImportService.kt")
        self.assertIn("RdpkgImporter.peekPackageId(file)", service)
        self.assertIn("VolumeStorage.locate(applicationContext, peekedId)", service)
        self.assertIn("private fun recoverOrSkip(", service)
        peek = service.index("RdpkgImporter.peekPackageId(file)")
        install = service.index("RdpkgImporter.install(")
        self.assertLess(peek, install, "seen before unpacking")
        self.assertIn("is PackageAlreadyInstalledException", service)
        self.assertIn("recoverOrSkip(store, manifest, error.packageId)", service)
        self.assertIn("PreparedVolumeReader.read(applicationContext, treeUri)", service)
        importer = source("RdpkgImporter.kt")
        self.assertIn("fun peekPackageId(", importer)
        self.assertIn("fun isSafePackageId(", importer)

    def test_two_labelled_bars(self):
        view = source("OperationStatusView.kt")
        self.assertIn("counterLabel: String? = null", view)
        self.assertIn("secondaryLabel: String? = null", view)
        self.assertIn("private fun hideSecondary()", view)
        self.assertGreaterEqual(view.count("hideSecondary()"), 4)
        activity = source("RdprojectImportActivity.kt")
        self.assertIn("RdprojectImportProgress.overallLabel(", activity)
        self.assertIn("RdprojectImportProgress.volumeLabel(", activity)
        store = source("RdprojectImportRunStore.kt")
        self.assertIn("fun volume(", store)
        self.assertIn("KEY_DONE_VOLUMES", store)

    def test_stage0_read_timing(self):
        resolver = source("SafDatasetResolver.kt")
        self.assertIn("VolumeReadStats.recordListing(", resolver)
        self.assertIn("VolumeReadStats.recordOpen(", resolver)
        self.assertIn("VolumeReadStats.TAG", resolver)
        stats = source("VolumeReadStats.kt")
        self.assertIn('const val TAG = "RenaultRead"', stats)
        viewer = source("SafDatasetWebViewClient.kt")
        self.assertIn('"FAST тека=', viewer)
        menu = (ROOT / "tools/termux/phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("33) renault_read_timing ;;", menu)
        self.assertIn("logcat -d -v time -s RenaultRead:I", menu)


if __name__ == "__main__":
    unittest.main()
