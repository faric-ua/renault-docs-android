"""v0.5.86: isolate raw files of each selected Renault volume in nested archives."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
TESTS = ROOT / "android/app/src/test/java/com/saney/renaultdocs"


class ArchiveVolumeIsolationContract(unittest.TestCase):
    def test_selected_volume_isolated_from_all_nested_candidates(self):
        isolation = (JAVA / "ArchiveRawVolumeIsolation.kt").read_text(encoding="utf-8")
        self.assertIn("fun excludedDescendantRoots(", isolation)
        self.assertIn("ArchiveIntake.resolveRawRoot(stagingRoot, it)", isolation)
        self.assertIn("candidate.path.startsWith(selectedPrefix)", isolation)
        self.assertIn("candidate != selected", isolation)
        self.assertIn("fun walkSelectedRoot(", isolation)
        self.assertIn(".onEnter { directory ->", isolation)
        self.assertIn("directory.canonicalFile !in excluded", isolation)
        self.assertIn("excluded.all { it.isDirectory && it.path.startsWith(selectedPrefix) }", isolation)
        self.assertNotIn("deleteRecursively()", isolation)
        self.assertNotIn("outputStream(", isolation)

    def test_archive_selection_threads_exclusions_to_native_stage(self):
        service = (JAVA / "NativeRdpkgPreparationService.kt").read_text(encoding="utf-8")
        self.assertIn("ArchiveRawVolumeIsolation.excludedDescendantRoots(", service)
        self.assertIn("initialState.archiveCandidates.map {", service)
        self.assertIn("excludedNestedRawRoots =", service)
        self.assertIn("listOf(rawSourceName), project", service)
        engine = (JAVA / "NativeRdpkgPreparationEngine.kt").read_text(encoding="utf-8")
        self.assertIn("excludedNestedRawRoots: Set<File> = emptySet()", engine)
        self.assertIn("excludedNestedRawRoots =\n                        excludedNestedRawRoots,", engine)
        stager = (JAVA / "NativePreparationStager.kt").read_text(encoding="utf-8")
        self.assertIn("excludedNestedRawRoots: Set<File> = emptySet()", stager)
        self.assertIn("ArchiveRawVolumeIsolation\n            .walkSelectedRoot(", stager)
        self.assertIn("excludedNestedRawRoots,", stager)
        self.assertIn("scanLocalSource(", stager)

    def test_kotlin_unit_tests_exercise_both_directions_and_boundaries(self):
        code = (TESTS / "ArchiveRawVolumeIsolationTest.kt").read_text(encoding="utf-8")
        for name in (
            "selectingParentExcludesIndependentNestedVolumesWithoutTouchingTheirFiles",
            "recursivelyNestedSeparateVolumeIsNotCopiedIntoParentVolume",
            "rejectsEscapeOrSourceRootExclusionRatherThanDroppingWholeVolume",
        ):
            self.assertIn(f"fun {name}()", code)
        self.assertIn("assertEquals(setOf(", code)
        self.assertIn("ArchiveRawVolumeIsolation.walkSelectedRoot(", code)
        self.assertIn("ArchiveRawVolumeIsolation.excludedDescendantRoots(", code)


if __name__ == "__main__":
    unittest.main()
