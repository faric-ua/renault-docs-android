"""Regression contract: RAW_TREE source model checking before any .rdpkg output."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class V0597RawModelSafety(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = (ANDROID / "ProjectActivity.kt").read_text(encoding="utf-8")
        cls.worker = (ANDROID / "NativeRdpkgPreparationService.kt").read_text(encoding="utf-8")
        cls.guard = (ANDROID / "ArchiveSourceGuard.kt").read_text(encoding="utf-8")

    def test_picker_guards_explicit_model_before_destination_and_run(self):
        ui = self.ui.split("private fun handleNativeRdpkgSourceResult(", 1)[1].split(
            "private fun handleNativeRdpkgArchiveSourceResult(", 1)[0]
        assert ui.index("ArchiveSourceGuard.conflictingModel(sourceName, project)") < ui.index(
            "openNativeRdpkgDestinationPicker("
        )
        assert ui.index("ArchiveSourceGuard.conflictingModel(sourceName, project)") < ui.index(
            "pendingNativeSourceUri =\n            uri.toString()"
        )
        self.assertIn("showArchiveSourceRejected(problem)", ui)

    def test_native_worker_guards_source_even_without_picker(self):
        worker = self.worker.split("private fun runRawPreparation(", 1)[1].split(
            "private fun runArchiveInitial(", 1)[0]
        self.assertLess(
            worker.index("ArchiveSourceGuard.conflictingModel(request.sourceName, project)"),
            worker.index("processPreparedSource("),
        )
        self.assertIn("ProjectStore(this).project(request.projectId)", worker)
        self.assertIn("runStore.fail(", worker)

    def test_no_speculative_vehicle_inference(self):
        self.assertIn("if (models.isEmpty()) return null", self.guard)
        self.assertIn("fun conflictingModel(name: String, project: RenaultProject)", self.guard)
        self.assertNotIn("X61" to Regex", self.guard)
        self.assertIn('return conflictingModel(name, project)', self.guard)

if __name__ == "__main__":
    unittest.main()
