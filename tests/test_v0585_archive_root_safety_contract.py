"""v0.5.85: archive chooser root and cross-format extraction collision contracts."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
TESTS = ROOT / "android/app/src/test/java/com/saney/renaultdocs"


class ArchiveRootAndCollisionContract(unittest.TestCase):
    def test_blank_archive_root_candidate_allowed_but_traversal_refused(self):
        source = (JAVA / "ArchiveIntake.kt").read_text(encoding="utf-8")
        root = source.split("internal fun resolveRawRoot(", 1)[1].split("internal fun safeTarget(", 1)[0]
        self.assertIn("if (relativePath.isEmpty())", root)
        self.assertIn("safeTarget(root, relativePath)", root)
        self.assertIn("target.isDirectory", root)
        self.assertIn("root.path + File.separator", root)
        service = (JAVA / "NativeRdpkgPreparationService.kt").read_text(encoding="utf-8")
        self.assertIn("ArchiveIntake.resolveRawRoot(", service)
        self.assertIn("ArchiveIntake.resolveRawRoot(\n                        extractionRoot,\n                        relativePath,", service)
        self.assertNotIn("relativePath.isNotBlank() &&", service)

    def test_conflicting_archive_member_names_rejected_in_all_formats(self):
        code = (JAVA / "ArchiveIntake.kt").read_text(encoding="utf-8")
        guard = code.split("internal class EntryPathGuard {", 1)[1].split("fun extract(", 1)[0]
        self.assertIn("validatedEntrySegments(name)", guard)
        self.assertIn("files.add(key)", guard)
        self.assertIn("key !in directories", guard)
        self.assertIn("key !in files", guard)
        self.assertIn("prefixKey !in files", guard)
        self.assertIn("lowercase(Locale.ROOT)", guard)
        self.assertIn("previous == null || previous == prefix", guard)
        self.assertIn("val inspectedPaths = EntryPathGuard()", code)
        self.assertIn("inspectedPaths.check(entryName, isDirectory)", code)
        for name, following in (
            ("extractZip(", "extractSevenZ("),
            ("extractSevenZ(", "extractRar("),
            ("extractRar(", "private class LimitedOutputStream"),
        ):
            # Choose the function declarations, not invocations at call sites.
            segment = code.split("private fun " + name, 1)[1].split(
                "private fun " + following, 1
            )[0] if following != "private class LimitedOutputStream" else code.split(
                "private fun " + name, 1
            )[1].split(following, 1)[0]
            self.assertIn("val entryPaths = EntryPathGuard()", segment)
            self.assertIn("entryPaths.check(", segment)

    def test_android_unit_cases_for_real_zip_and_root_layout(self):
        code = (TESTS / "ArchiveIntakeTest.kt").read_text(encoding="utf-8")
        for name in (
            "multiVolumeChooserResolvesArchiveRootAndNestedRawSafely",
            "archivePathsRejectDuplicatePayloadsAndCaseConflicts",
            "zipPreflightRejectsCaseCollisionsInsteadOfSilentlyOverwriting",
        ):
            self.assertIn("fun " + name + "()", code)
        self.assertIn("ArchiveIntake.resolveRawRoot(staging, \"\")", code)
        self.assertIn("ArchiveIntake.extract(archive, staging)", code)
        self.assertIn("ArchiveIntake.inspectRawRoots(archive)", code)


if __name__ == "__main__":
    unittest.main()
