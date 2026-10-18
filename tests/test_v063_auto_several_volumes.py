"""Renodocs 0.6.3 (owner 2026-10-18 04:55): «Авто» takes several .rdpkg at once.

The service still installs one package at a time and the project screen adds the
finished volume (unchanged path); RdpkgImportQueue only remembers what waits.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def between(text, start, end):
    a = text.index(start)
    return text[a:text.index(end, a)]


class AutoSeveralVolumes(unittest.TestCase):
    def setUp(self):
        self.project = (SRC / "ProjectActivity.kt").read_text(encoding="utf-8")

    def test_picker_allows_several_files(self):
        picker = between(self.project, "private fun openPackagePicker()", "private fun openVolumePicker(")
        self.assertIn("Intent.EXTRA_ALLOW_MULTIPLE", picker)
        self.assertIn("REQUEST_RDPKG_FILE", picker)

    def test_result_reads_clip_data_and_queues_the_rest(self):
        handler = between(self.project, "private fun handleRdpkgResult(", "private fun refreshRdpkgImportRunState()")
        self.assertIn("clipData", handler)
        self.assertIn("ImportQueueState.start(project.id", handler)
        self.assertIn("rdpkgImportQueue.save(rest)", handler)
        # one file — old path, no queue left behind
        self.assertIn("rdpkgImportQueue.clear()", handler)

    def test_next_volume_starts_after_success_and_failure(self):
        refresh = between(self.project, "private fun refreshRdpkgImportRunState()", "private fun handleRdpkgExportResult(")
        self.assertIn("continueRdpkgQueue(succeeded = true", refresh)
        self.assertIn("continueRdpkgQueue(succeeded = false", refresh)
        # «Скасувати» drops the rest of the queue
        cancelled = between(refresh, "RdpkgImportRunPhase.CANCELLED", "RdpkgImportRunPhase.FAILED")
        self.assertIn("rdpkgImportQueue.clear()", cancelled)
        self.assertIn("Решту томів", cancelled)

    def test_service_still_one_package(self):
        service = (SRC / "RdpkgImportService.kt").read_text(encoding="utf-8")
        self.assertNotIn("ImportQueue", service)

    def test_pure_queue_has_jvm_test(self):
        self.assertTrue((SRC / "ImportQueueState.kt").is_file())
        self.assertNotIn("import android.", (SRC / "ImportQueueState.kt").read_text(encoding="utf-8"))
        self.assertTrue((ROOT / "android/app/src/test/java/com/saney/renaultdocs/ImportQueueStateTest.kt").is_file())


if __name__ == "__main__":
    unittest.main()
