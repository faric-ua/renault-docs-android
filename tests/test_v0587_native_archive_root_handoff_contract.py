"""v0.5.87: real wrapped Renault ZIP safe root handoff and deterministic error details."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
TESTS = ROOT / "android/app/src/test/java/com/saney/renaultdocs"

class ArchiveNativeRootHandoffContract(unittest.TestCase):
    def test_unique_nested_root_is_used_before_local_native_scan(self):
        code = (JAVA / "NativePreparationStager.kt").read_text(encoding="utf-8")
        resolver = (JAVA / "ArchiveNativeRawRoot.kt").read_text(encoding="utf-8")
        self.assertIn("ArchiveNativeRawRoot.resolve(sourceRoot)", code)
        self.assertIn("scanLocalSource(\n                actualRoot,", code)
        self.assertIn("sourceRoot = actualRoot", code)
        self.assertIn("if (excludedNestedRawRoots.isEmpty())", code)
        self.assertIn("require(ArchiveNativeRawRoot.hasDirectEntrypoint(sourceRoot))", code)
        self.assertIn("fun resolve(sourceRoot: File): File", resolver)
        self.assertIn("ArchiveIntake.findRenaultRawRoots(root)", resolver)
        self.assertIn("require(children.size == 1)", resolver)
        self.assertIn("hasDirectEntrypoint(selected)", resolver)
        self.assertNotIn("deleteRecursively()", resolver)
        self.assertNotIn("outputStream()", resolver)

    def test_failed_scan_is_actionable_without_exposing_saf_uri(self):
        code = (JAVA / "NativePreparationStager.kt").read_text(encoding="utf-8")
        self.assertIn("ArchiveNativeRawRoot.describeMismatch(sourceRoot, exactFiles)", code)
        diagnostic = (JAVA / "ArchiveNativeRawRoot.kt").read_text(encoding="utf-8")
        self.assertIn("файли верхнього рівня:", diagnostic)
        self.assertIn("знайдені INDEX:", diagnostic)
        self.assertNotIn("canonicalPath", diagnostic)
        self.assertNotIn("absolutePath", diagnostic)
        self.assertNotIn("sourceUri", diagnostic)

    def test_synthetic_real_world_zip_and_ambiguity_jvm_cases(self):
        tests = (TESTS / "ArchiveNativeRawRootTest.kt").read_text(encoding="utf-8")
        self.assertIn("actualNestedRenaultLayoutResolvesWithoutMovingOriginalArchive", tests)
        self.assertIn("ambiguousWrapperIsNeverFlattenedOrChosenArbitrarily", tests)
        self.assertIn("noEntrypointOrPreparedDatasetDoesNotPassAsRaw", tests)
        self.assertIn('folder + "INDEX.HTM"', tests)
        self.assertIn('folder + "COMMUN/img.bin"', tests)
        self.assertIn('folder + "RUS/menu.htm"', tests)
        self.assertIn("ArchiveIntake.extract(archive, extractionRoot)", tests)
        self.assertIn("ArchiveNativeRawRoot.resolve(extractionRoot)", tests)

if __name__ == "__main__":
    unittest.main()
