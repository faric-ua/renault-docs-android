from pathlib import Path
import unittest


class V0531VolumeDocumentationScreenContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_manifest_registers_dedicated_volume_documentation_activity(self):
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )

        self.assertIn(
            'android:name=".VolumeDocumentationActivity"',
            manifest,
        )

    def test_runtime_reader_resolves_documentation_by_volume_without_section(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RuntimeIrReader.kt"
        )

        self.assertIn(
            "fun readVolumeDocumentationForVolume(",
            reader,
        )
        self.assertIn(
            "fun findVolumeDocumentationShard(",
            reader,
        )
        self.assertIn('"documentation_path"', reader)

        direct_reader = reader.split(
            "fun readVolumeDocumentationForVolume(",
            1,
        )[1].split(
            "data class VolumeDocumentationLookup",
            1,
        )[0]

        self.assertIn("volumeEntrypoint", direct_reader)
        self.assertNotIn("sectionCode", direct_reader)
        self.assertNotIn("sectionEntrypoint", direct_reader)

    def test_volume_screen_has_direct_documentation_entry(self):
        volume = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )

        self.assertIn('"Документація"', volume)
        self.assertIn("openVolumeDocumentation()", volume)
        self.assertIn(
            "VolumeDocumentationActivity",
            volume,
        )
        self.assertIn(
            ".intentForVolume(",
            volume,
        )

    def test_section_documentation_is_shortcut_to_same_volume_activity(self):
        section = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        )

        self.assertIn(
            "hasVolumeDocumentation",
            section,
        )
        self.assertIn(
            "openVolumeDocumentation()",
            section,
        )
        self.assertIn(
            "VolumeDocumentationActivity",
            section,
        )

        tile = section.split(
            "private fun buildDocumentationTile",
            1,
        )[1].split(
            "private fun menuTileButton",
            1,
        )[0]

        self.assertIn(
            "if (hasVolumeDocumentation)",
            tile,
        )
        self.assertIn(
            "openVolumeDocumentation()",
            tile,
        )
        self.assertIn(
            "openDocumentation(",
            tile,
        )

    def test_dedicated_activity_owns_navigation_and_restores_panel_stack(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/VolumeDocumentationActivity.kt"
        )

        self.assertIn(
            "readVolumeDocumentationForVolume(",
            activity,
        )
        self.assertIn(
            "STATE_PANEL_STACK",
            activity,
        )
        self.assertIn(
            "putStringArrayList(",
            activity,
        )
        self.assertIn(
            "getStringArrayList(",
            activity,
        )
        self.assertIn(
            "renderRoot()",
            activity,
        )
        self.assertIn(
            "renderPanel(",
            activity,
        )

    def test_final_documents_open_in_existing_viewer_with_volume_context(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/VolumeDocumentationActivity.kt"
        )

        self.assertIn(
            "ViewerActivity",
            activity,
        )
        self.assertIn(
            "modernVolumeEntrypoint =",
            activity,
        )
        self.assertIn(
            "modernFocusEntrypoint =",
            activity,
        )

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

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
