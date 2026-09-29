from pathlib import Path
import unittest


class V0526ClassicCatalogParityContractTests(unittest.TestCase):
    def test_converter_treats_section_ids_as_opaque_and_preserves_source_order(self):
        repo = Path(__file__).resolve().parents[1]
        sections = (
            repo / "core/sections.py"
        ).read_text(encoding="utf-8")

        self.assertIn("MODERN_SECTIONS_SCHEMA_VERSION = 2", sections)
        self.assertIn("_SECTION_ID_RE", sections)
        self.assertIn("parsed.entries", sections)
        self.assertIn("section_positions", sections)
        self.assertIn('section["entrypoint"].casefold()', sections)
        self.assertNotIn("int(item[\"code\"])", sections)

    def test_runtime_shards_preserve_duplicate_display_codes(self):
        repo = Path(__file__).resolve().parents[1]
        shards = (
            repo / "core/runtime_ir_shards.py"
        ).read_text(encoding="utf-8")
        reader = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/RuntimeIrReader.kt"
        ).read_text(encoding="utf-8")
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn('"section_entries"', shards)
        self.assertIn("section_paths.setdefault(", shards)
        self.assertIn("len(section_entries)", shards)
        self.assertIn('"section_entries"', reader)
        self.assertIn("sectionEntrypoint", reader)
        self.assertIn("sectionEntrypoint =", native)

    def test_android_keeps_converter_source_order(self):
        repo = Path(__file__).resolve().parents[1]
        reader = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernSectionsReader.kt"
        ).read_text(encoding="utf-8")

        self.assertNotIn("sortedWith(", reader)
        self.assertIn("buildList", reader)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 67)
        self.assertGreaterEqual(version_name, (0, 5, 51))


if __name__ == "__main__":
    unittest.main()
