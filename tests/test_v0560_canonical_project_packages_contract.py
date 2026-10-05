from pathlib import Path
import unittest


class V0560CanonicalProjectPackagesContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]

    def read(self, path: str) -> str:
        return (self.repo / path).read_text(encoding="utf-8")

    def test_rdproject_filename_uses_project_and_vehicle_identity(self):
        src = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )

        self.assertIn("fun defaultFileName(", src)
        self.assertIn("volumes: List<ProjectVolumeRecord>", src)
        self.assertIn("projectVehicleCodes(", src)
        self.assertIn("RenaultVolumeIdentity.safeFilePart(", src)
        self.assertIn('".rdproject"', src)

    def test_rdproject_manifest_carries_rich_volume_metadata(self):
        src = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )

        for token in (
            '"vehicle_codes"',
            '"document_code"',
            '"date"',
            '"document_type"',
            '"document_version"',
            '"region"',
        ):
            self.assertIn(token, src)

        self.assertIn("RdpkgExporter.resolvedMetadata(", src)
        self.assertIn("RdpkgExporter.defaultFileName(", src)

    def test_prepared_project_legacy_name_is_migrated_to_canonical_name(self):
        store = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/PreparedShareStore.kt"
        )
        home = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        controller = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )
        service = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectShareService.kt"
        )

        self.assertIn("existingProjectFile(", store)
        self.assertIn("legacyProjectFileName(", store)
        self.assertIn("legacy.renameTo(", store)
        self.assertIn("hasProjectFile(", store)
        self.assertIn("PreparedShareStore.hasProjectFile(", home)
        self.assertIn("PreparedShareStore.existingProjectFile(", controller)
        self.assertIn("PreparedShareStore.existingProjectFile(", controller)
        self.assertIn("PreparedShareStore", service)
        self.assertIn(".projectFile(", service)

    def test_rdpkg_filename_and_rdproject_manifest_share_one_identity_resolver(self):
        exporter = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgExporter.kt"
        )
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )

        self.assertIn("fun resolvedMetadata(", exporter)
        self.assertIn("resolvedMetadata(", exporter)
        self.assertIn("RdpkgExporter.resolvedMetadata(", project)

    def test_release_version_is_v0560_or_newer(self):
        gradle = self.read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )

        self.assertGreaterEqual(version_name, (0, 5, 60))
        self.assertGreaterEqual(version_code, 76)


if __name__ == "__main__":
    unittest.main()
