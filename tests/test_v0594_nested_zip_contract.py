"""v0.5.94 safe one-level ZIP-in-ZIP Romanian/Renault multi-volume intake."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"android/app/src/main/java/com/saney/renaultdocs"

class NestedZipContracts(unittest.TestCase):
    def code(self,name): return (SRC/name).read_text(encoding="utf-8")

    def test_outer_does_not_need_direct_raw_roots(self):
        intake=self.code("ArchiveIntake.kt")
        stager=self.code("ArchiveIntakeStager.kt")
        self.assertIn("allowNoRawRoots: Boolean = false", intake)
        self.assertIn("allowNoRawRoots || rawRoots.isNotEmpty()", intake)
        self.assertIn("allowNoRawRoots = true", stager)
        self.assertIn("NestedZipVolumeIntake.expandOneLevel(", stager)
        self.assertIn("extracted.rawRoots.isEmpty()", stager)
        self.assertIn("discoveredRoots.isNotEmpty()", stager)
        self.assertIn("rawRoots =\n                    discoveredRoots", stager)

    def test_bounded_one_level_security_and_cancellation(self):
        nested=self.code("NestedZipVolumeIntake.kt")
        self.assertIn("MAX_INNER_ARCHIVES = 20", nested)
        self.assertIn("MAX_TOTAL_EXPANDED_BYTES", nested)
        self.assertIn("MAX_TOTAL_ENTRIES", nested)
        self.assertIn("MAX_SOURCE_BYTES", nested)
        self.assertIn("ArchiveIntake.EntryPathGuard()", nested)
        self.assertIn("ArchiveIntake.safeTarget(destination, entry.name)", nested)
        self.assertIn("checkCancelled(isCancelled)", nested)
        self.assertIn("ArchiveIntake.findRenaultRawRoots(destination)", nested)
        self.assertIn("Глибше вкладені архіви не розпаковуються автоматично", nested)
        self.assertNotIn("File(context.getExternalFilesDir", nested)

    def test_source_archives_never_mutated(self):
        nested=self.code("NestedZipVolumeIntake.kt")
        self.assertIn("ZipFile(inner).use", nested)
        self.assertIn("require(!nestedRoot.exists())", nested)
        self.assertIn("extractionRoot.walkTopDown()", nested)
        self.assertNotIn("inner.delete(", nested)
        self.assertNotIn("archive.delete(", nested)
        self.assertNotIn("FileOutputStream(inner", nested)

    def test_selector_will_receive_canonically_contained_roots(self):
        service=self.code("NativeRdpkgPreparationService.kt")
        self.assertIn("buildArchiveCandidates(", service)
        self.assertIn("ArchiveIntake.resolveRawRoot(", service)
        self.assertIn("candidates.size >", service)
        self.assertIn("markWaitingForArchiveSelection(", service)

if __name__=="__main__": unittest.main()
