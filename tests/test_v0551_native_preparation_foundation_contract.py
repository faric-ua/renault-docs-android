from pathlib import Path
import unittest


class V0551NativePreparationFoundationContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_outer_rdpkg_writer_is_streaming_and_speed_oriented(self):
        writer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgZipWriter.kt"
        )

        self.assertIn("DigestOutputStream(", writer)
        self.assertIn("MessageDigest.getInstance(", writer)
        self.assertIn('"SHA-256"', writer)
        self.assertIn("Deflater.BEST_SPEED", writer)
        self.assertIn("Deflater.NO_COMPRESSION", writer)
        self.assertIn("archive.setLevel(", writer)
        self.assertIn("ZIP_EPOCH_MILLIS", writer)

    def test_existing_exporter_reuses_shared_writer(self):
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgExporter.kt"
        )

        self.assertIn("RdpkgZipWriter", exporter)
        self.assertIn(".writeDirectory(", exporter)
        self.assertNotIn("ZipOutputStream(", exporter)

    def test_native_fast_pack_matches_reference_selection_contract(self):
        writer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeFastPackWriter.kt"
        )

        self.assertIn('FORMAT =\n        "zip-web-v1"', writer)
        self.assertIn("DigestOutputStream(", writer)
        self.assertIn("Deflater.BEST_SPEED", writer)
        self.assertIn('"fast-content-"', writer)
        self.assertIn('"_renault"', writer)
        self.assertIn("fun shouldPack(", writer)
        self.assertIn('"renault-dataset.json"', writer)

    def test_architecture_uses_private_staging_not_public_intermediate_dataset(self):
        doc = self._read(
            "docs/architecture/KOTLIN_NATIVE_RAW_TO_RDPKG.md"
        )

        self.assertIn("one copy/patch into app-private staging", doc)
        self.assertIn("Python/Termux remain the reference implementation", doc)
        self.assertIn("No public `*_android` output", doc)
        self.assertIn("section discovery parity", doc)
        self.assertIn("Section IR compiler parity", doc)
        self.assertIn("Runtime IR shards/index + coverage", doc)
        self.assertIn("NT8340A · 2006-04-18 → 347 · native", doc)


    def test_raw_source_is_copied_once_into_private_staging(self):
        stager = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativePreparationStager.kt"
        )

        self.assertIn("context.noBackupFilesDir", stager)
        self.assertIn("DocumentsContract", stager)
        self.assertIn("ContentResolver", stager.replace("context.contentResolver", "ContentResolver"))
        self.assertIn("ConverterPathNormalizer", stager)
        self.assertIn("scanSourceFast(", stager)
        self.assertIn("copyAndPatch(", stager)
        self.assertIn("staging.deleteRecursively()", stager)
        self.assertNotIn("_android", stager)


    def test_section_discovery_is_native_and_preserves_opaque_identifiers(self):
        compiler = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionCompiler.kt"
        )
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("org.jsoup:jsoup:", gradle)
        self.assertIn("legacy-html-navigation", compiler)
        self.assertIn("looksLikeSectionId(", compiler)
        self.assertIn("source_file", compiler)
        self.assertIn("entrypoint", compiler)
        self.assertIn("fallbackCandidates(", compiler)
        self.assertIn("section_count", compiler)
        self.assertIn("sectionIdRegex", compiler)


    def test_native_pipeline_is_wired_end_to_end_without_public_android_copy(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationEngine.kt"
        )

        self.assertIn("NativePreparationStager", engine)
        self.assertIn("NativeVolumeCompiler", engine)
        self.assertIn("NativeSectionCompiler", engine)
        self.assertIn("NativeRuntimeIrCompiler", engine)
        self.assertIn("NativeFastPackWriter", engine)
        self.assertIn("RdpkgZipWriter", engine)
        self.assertIn('"rdpkg.json"', engine)
        self.assertIn('"renault-dataset.json"', engine)
        self.assertIn('"modern_runtime_compiled"', engine)
        self.assertIn("root.deleteRecursively()", engine)
        self.assertNotIn("_android", engine)

    def test_runtime_ir_compiler_writes_shards_documentation_and_coverage(self):
        compiler = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRuntimeIrCompiler.kt"
        )

        self.assertIn('"runtime-tree.json"', compiler)
        self.assertIn('"runtime-ir-index.json"', compiler)
        self.assertIn('"runtime-ir-coverage.json"', compiler)
        self.assertIn('"sharded-section-json"', compiler)
        self.assertIn('"documentation_path"', compiler)
        self.assertIn('"section_entries"', compiler)
        self.assertIn('"unsupported_action_count"', compiler)
        self.assertIn("NativeSectionIrCompiler", compiler)


if __name__ == "__main__":
    unittest.main()
