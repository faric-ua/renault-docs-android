from pathlib import Path
import unittest


class V0558VolumeIdentityContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_android_parser_preserves_renault_codes_without_translation(self):
        parser = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RenaultVolumeIdentity.kt"
        )

        self.assertIn("vehicleCodes", parser)
        self.assertIn("groupedVehicleCodeRegex", parser)
        self.assertIn('"Visu"', parser)
        self.assertIn('"Europe"', parser)
        self.assertIn("canonicalFileName(", parser)
        self.assertNotIn("Cabriolet", parser)
        self.assertNotIn("Sedan", parser)
        self.assertNotIn("Estate", parser)

    def test_project_volume_tile_has_three_identity_layers(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn('joinToString(\n                        " · ",', activity)
        self.assertIn("volume.vehicleCodes", activity)
        self.assertIn("documentDescriptor()", activity)
        self.assertIn("volume.region", activity)

    def test_project_store_persists_identity_metadata(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        for token in (
            "vehicleCodes",
            "documentType",
            "documentVersion",
            "region",
        ):
            self.assertIn(token, store)

    def test_native_manifest_carries_identity_metadata(self):
        compiler = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeVolumeCompiler.kt"
        )
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationEngine.kt"
        )

        for token in (
            '"vehicle_codes"',
            '"document_type"',
            '"document_version"',
            '"region"',
        ):
            self.assertIn(token, compiler)
            self.assertIn(token, engine)

    def test_legacy_visu_schema_content_is_a_metadata_fallback(self):
        parser = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RenaultVolumeIdentity.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/PreparedVolumeReader.kt"
        )
        compiler = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeVolumeCompiler.kt"
        )

        self.assertIn("documentTypeFromHtml(", parser)
        self.assertIn("Visu\\s+Schema", parser)
        self.assertIn("inferDocumentTypeFromEntrypoint(", store)
        self.assertIn("inferDocumentTypeFromEntrypoint(", reader)
        self.assertIn("inferDocumentTypeFromEntrypoint(", compiler)


    def test_release_version_is_v0558_or_newer(self):
        gradle = self._read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )

        self.assertGreaterEqual(version_code, 74)
        self.assertGreaterEqual(version_name, (0, 5, 58))


if __name__ == "__main__":
    unittest.main()
