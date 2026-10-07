from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class V0569ArchiveIntakeContractTests(unittest.TestCase):
    def read_java(self, name: str) -> str:
        return (JAVA / name).read_text(encoding="utf-8")

    def test_archive_formats_and_dependencies_are_explicit(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        intake = self.read_java("ArchiveIntake.kt")

        self.assertIn('implementation("org.apache.commons:commons-compress:1.28.0")', gradle)
        self.assertIn('implementation("org.tukaani:xz:1.10")', gradle)
        self.assertIn('implementation("com.github.junrar:junrar:8.1.1")', gradle)
        self.assertIn("ZIP", intake)
        self.assertIn("SEVEN_Z", intake)
        self.assertIn("RAR", intake)

    def test_archive_extraction_has_path_traversal_and_resource_guards(self):
        intake = self.read_java("ArchiveIntake.kt")

        self.assertIn("safeTarget(", intake)
        self.assertIn('it ==', intake)
        self.assertIn('".."', intake)
        self.assertIn("canonicalFile", intake)
        self.assertIn("MAX_ENTRIES", intake)
        self.assertIn("MAX_EXPANDED_BYTES", intake)
        self.assertIn("MIN_FREE_SPACE_BYTES", intake)

    def test_archive_source_is_copied_to_private_staging(self):
        stager = self.read_java("ArchiveIntakeStager.kt")

        self.assertIn("appContext.noBackupFilesDir", stager)
        self.assertIn('"archive-intake"', stager)
        self.assertIn("openInputStream(", stager)
        self.assertIn("ArchiveIntake.extract(", stager)
        self.assertIn("workRoot.deleteRecursively()", stager)

    def test_archive_flow_reuses_native_engine(self):
        engine = self.read_java("NativeRdpkgPreparationEngine.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("fun prepareLocal(", engine)
        self.assertIn(".prepareLocal(", engine)
        self.assertIn("ArchiveIntakeStager(", service)
        self.assertIn("engine.prepareLocal(", service)
        self.assertIn("NativeRdpkgSourceKind.ARCHIVE_FILE", service)

    def test_archive_source_kind_survives_service_restart(self):
        store = self.read_java("NativeRdpkgRunStore.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("enum class NativeRdpkgSourceKind", store)
        self.assertIn("KEY_SOURCE_KIND", store)
        self.assertIn("sourceKind =", store)
        self.assertIn("state.sourceKind", service)
        self.assertIn("EXTRA_SOURCE_KIND", service)

    def test_project_ui_exposes_archive_picker(self):
        project = self.read_java("ProjectActivity.kt")

        self.assertIn('"Створити .rdpkg з архіву"', project)
        self.assertIn('"ZIP · 7Z · RAR · без ручної розпаковки"', project)
        self.assertIn("startNativeArchiveRdpkgFlow()", project)
        self.assertIn("REQUEST_NATIVE_RDPKG_ARCHIVE_SOURCE", project)
        self.assertIn("STATE_PENDING_NATIVE_SOURCE_KIND", project)
        self.assertIn("HELP_ARCHIVE", project)

    def test_release_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.69"', gradle)
        self.assertIn("versionCode = 85", gradle)


if __name__ == "__main__":
    unittest.main()
