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

    def test_duplicate_fast_path_can_skip_full_extraction(self):
        intake = self.read_java("ArchiveIntake.kt")
        stager = self.read_java("ArchiveIntakeStager.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("fun inspectRawRoots(", intake)
        self.assertIn("data class RawRootHint", intake)
        self.assertIn("fun stageSource(", stager)
        self.assertIn("fun extract(", stager)
        self.assertIn("ArchiveIntake.inspectRawRoots(", service)
        self.assertIn("sourceNameHint", intake)
        self.assertIn("documentCode", intake)
        self.assertIn("MAX_IDENTITY_PROBE_BYTES", intake)
        self.assertIn("hint.documentCode", service)
        self.assertIn("installedVolumesForArchiveHints(", service)
        self.assertIn("Розпакування і конвертацію пропущено", service)
        self.assertLess(
            service.index("ArchiveIntake.inspectRawRoots("),
            service.index("stager.extract("),
        )

    def test_duplicate_preflight_runs_before_native_conversion(self):
        preflight = self.read_java("VolumeDuplicatePreflight.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")
        store = self.read_java("NativeRdpkgRunStore.kt")

        self.assertIn("documentCode", preflight)
        self.assertIn("sourceDate", preflight)
        self.assertIn("VolumeDuplicatePreflight", service)
        self.assertIn("ProjectStore(", service)
        self.assertIn("markAlreadyPresent(", service)
        self.assertLess(
            service.index("VolumeDuplicatePreflight"),
            service.index("engine.prepareLocal("),
        )
        self.assertIn("ALREADY_PRESENT", store)

    def test_multi_volume_archive_waits_for_explicit_selection(self):
        store = self.read_java("NativeRdpkgRunStore.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("WAITING_SELECTION", store)
        self.assertIn("markWaitingForArchiveSelection", store)
        self.assertIn("archiveCandidates", store)
        self.assertIn("updateArchiveCandidateSelection", store)
        self.assertIn("beginArchiveSelectionProcessing", store)
        self.assertIn("candidates.size >", service)
        self.assertIn("resumeArchiveSelection", service)
        self.assertIn("runArchiveSelection()", service)
        self.assertIn('"Створити вибрані"', project)
        self.assertIn("✓ Уже встановлено", project)
        self.assertIn("⚠ Схожий том уже є", project)

    def test_archive_multi_volume_uses_destination_folder_and_per_volume_files(self):
        project = self.read_java("ProjectActivity.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("Intent.ACTION_OPEN_DOCUMENT_TREE", project)
        self.assertIn("createArchiveDestination(", service)
        self.assertIn("DocumentFile.fromTreeUri(", service)
        self.assertIn(".createFile(", service)
        self.assertIn("RenaultVolumeIdentity", service)
        self.assertIn("canonicalFileName", service)

    def test_archive_chooser_state_survives_activity_recreation(self):
        store = self.read_java("NativeRdpkgRunStore.kt")
        project = self.read_java("ProjectActivity.kt")

        self.assertIn("KEY_ARCHIVE_CANDIDATES", store)
        self.assertIn("KEY_ARCHIVE_EXTRACTION_ROOT", store)
        self.assertIn("encodeArchiveCandidates", store)
        self.assertIn("decodeArchiveCandidates", store)
        self.assertIn("state.isWaitingForSelection", project)
        self.assertIn("showArchiveVolumeChooser(", project)

    def test_archive_partial_output_is_persisted_for_crash_cleanup(self):
        store = self.read_java("NativeRdpkgRunStore.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("archiveCurrentOutputUri", store)
        self.assertIn("KEY_ARCHIVE_CURRENT_OUTPUT_URI", store)
        self.assertIn("setArchiveCurrentOutput", store)
        self.assertIn("clearArchiveCurrentOutput", store)
        self.assertIn("cleanupStaleArchiveOutput()", service)
        self.assertIn("runStore.setArchiveCurrentOutput(", service)
        self.assertIn("runStore.clearArchiveCurrentOutput()", service)
        self.assertLess(
            service.index("cleanupStaleArchiveOutput()"),
            service.index("stager.stageSource("),
        )

    def test_archive_waiting_cancel_and_activity_destroy_are_safe(self):
        project = self.read_java("ProjectActivity.kt")
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("override fun onDestroy()", project)
        self.assertIn("archiveChooserDialog", project)
        self.assertIn("state.isWaitingForSelection", service)
        self.assertIn("cleanupWaitingArchiveWorkspace(", service)
        self.assertIn("private staging очищено", service)

    def test_archive_batch_resume_reuses_extracted_staging(self):
        service = self.read_java("NativeRdpkgPreparationService.kt")

        self.assertIn("ACTION_RESUME_ARCHIVE", service)
        self.assertIn("resolvePersistedArchiveRoot", service)
        self.assertIn("resolveArchiveCandidateRoot", service)
        self.assertIn("startArchiveResumeService", service)
        self.assertIn("cleanupPersistedArchiveWorkspace", service)

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
        self.assertIn('versionName = "0.5.87"', gradle)
        self.assertIn("versionCode = 103", gradle)


if __name__ == "__main__":
    unittest.main()
