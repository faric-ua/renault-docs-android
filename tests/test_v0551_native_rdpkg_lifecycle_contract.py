from pathlib import Path
import unittest


class V0551NativeRdpkgLifecycleContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_native_run_state_is_persistent_and_separate(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgRunStore.kt"
        )

        self.assertIn("NativeRdpkgRunPhase", store)
        self.assertIn("renault_docs_native_rdpkg_run", store)
        self.assertIn("cancelRequested", store)
        self.assertIn("finishedAtMs", store)
        self.assertIn("projectId", store)
        self.assertIn("destinationUri", store)

    def test_native_pipeline_runs_in_foreground_service(self):
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )
        manifest = self._read("android/app/src/main/AndroidManifest.xml")

        self.assertIn("startForeground(", service)
        self.assertIn("PowerManager.PARTIAL_WAKE_LOCK", service)
        self.assertIn("NativeRdpkgPreparationEngine(", service)
        self.assertIn("runStore.isCancelRequested()", service)
        self.assertIn("ACTION_CANCEL", service)
        self.assertIn("RdpkgImporter", service)
        self.assertIn(".install(", service)
        self.assertIn("projectStore.upsertVolume(", service)
        self.assertIn("NativeRdpkgPreparationService", manifest)
        self.assertIn('android:foregroundServiceType="dataSync"', manifest)

    def test_generated_package_is_validated_before_project_upsert(self):
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )

        install_at = service.index("RdpkgImporter")
        upsert_at = service.index("projectStore.upsertVolume(")
        self.assertLess(install_at, upsert_at)
        self.assertIn(
            "imported.packageId ==\n                    prepared.packageId",
            service,
        )

    def test_project_ui_has_two_step_raw_to_rdpkg_picker(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn('"Створити .rdpkg з raw"', activity)
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT_TREE", activity)
        self.assertIn("REQUEST_NATIVE_RDPKG_SOURCE", activity)
        self.assertIn("Intent.ACTION_CREATE_DOCUMENT", activity)
        self.assertIn("REQUEST_NATIVE_RDPKG_DESTINATION", activity)
        self.assertIn("NativeRdpkgPreparationService.start(", activity)
        self.assertIn("STATE_PENDING_NATIVE_SOURCE_URI", activity)
        self.assertIn("STATE_PENDING_NATIVE_SOURCE_NAME", activity)
        self.assertIn("STATE_PENDING_NATIVE_REQUEST_ID", activity)
        self.assertIn("UUID.randomUUID()", activity)

    def test_project_activity_reattaches_to_persistent_run_state(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("NativeRdpkgRunStore(", activity)
        self.assertIn("refreshNativeRunState()", activity)
        self.assertIn("nativeRunHandler.postDelayed(", activity)
        self.assertIn("nativeRunHandler.removeCallbacks(", activity)
        self.assertIn("state.finishedAtMs", activity)
        self.assertIn("NativeRdpkgRunPhase.COMPLETE", activity)

    def test_cancel_is_not_activity_thread_ownership(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )

        self.assertIn("NativeRdpkgPreparationService", activity)
        self.assertIn(".requestCancel(", activity)
        self.assertIn("runStore.requestCancel()", service)
        self.assertNotIn(
            "Thread {\n            val result =\n                NativeRdpkgPreparationEngine",
            activity,
        )


    def test_stale_running_state_recovers_after_process_loss(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("NativeRdpkgPreparationService", activity)
        self.assertIn(".isActive()", activity)
        self.assertIn("NATIVE_RUN_STARTUP_GRACE_MS", activity)
        self.assertIn("nativeRunStore.fail(", activity)
        self.assertIn("Попередню native .rdpkg підготовку було перервано", activity)
        self.assertIn("30_000L", activity)

    def test_native_packager_prevents_automatic_screen_timeout(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("FLAG_KEEP_SCREEN_ON", activity)
        self.assertIn("setNativeKeepScreenOn(", activity)
        self.assertIn("window.addFlags(", activity)
        self.assertIn("window.clearFlags(", activity)
        self.assertIn(
            "override fun onStop()",
            activity,
        )

    def test_process_restart_rebuild_is_safe_for_staging_and_destination(self):
        stager = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativePreparationStager.kt"
        )
        writer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgZipWriter.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgRunStore.kt"
        )

        self.assertIn("staging.deleteRecursively()", stager)
        self.assertIn('"w"', writer)
        self.assertIn("resumeAfterProcessRestart()", store)
        self.assertIn("Відновлюю підготовку після перезапуску Android", store)

    def test_runtime_ir_json_is_streamed_without_giant_string(self):
        repo = Path(__file__).resolve().parents[1]
        compiler = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeRuntimeIrCompiler.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("BufferedWriter(", compiler)
        self.assertIn("private fun writeJsonFile(", compiler)
        self.assertIn("writeJsonValue(", compiler)
        self.assertNotIn("runtimeTree.toString(", compiler)

    def test_failed_native_package_cleanup_uses_documents_contract_fallback(self):
        repo = Path(__file__).resolve().parents[1]
        service = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("DocumentsContract.deleteDocument(", service)
        self.assertIn("resolver.delete(", service)
        self.assertIn("неповний .rdpkg не вдалося видалити автоматично", service)

    def test_raw_flow_rejects_parent_source_and_nested_destination(self):
        repo = Path(__file__).resolve().parents[1]
        stager = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativePreparationStager.kt"
        ).read_text(encoding="utf-8")
        project = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("Вибрано батьківську або змішану папку.", stager)
        self.assertIn("hasRootEntrypoint(", stager)
        self.assertIn("destinationIsInsideSourceTree(", project)
        self.assertIn("getTreeDocumentId(", project)
        self.assertIn("getDocumentId(", project)
        self.assertIn("Не зберігай .rdpkg всередині raw source.", project)
        self.assertIn("deleteCreatedDestination(", project)

    def test_native_start_is_idempotent_during_activity_handoffs(self):
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )

        self.assertIn("startInFlight.compareAndSet(", service)
        self.assertIn("persistedState.isRunning", service)
        self.assertIn("workerRunning.compareAndSet(", service)
        self.assertIn("START_REDELIVER_INTENT", service)
        self.assertIn("resumeAfterProcessRestart()", service)
        self.assertIn("resumingAfterProcessRestart", service)

    def test_completed_request_id_cannot_be_replayed_after_window_handoff(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgRunStore.kt"
        )

        self.assertIn("requestId =", activity)
        self.assertIn("pendingNativeRequestId", activity)
        self.assertIn("val requestId: String", service)
        self.assertIn("claimStartRequest(", service)
        self.assertIn("renault_docs_native_rdpkg_launch_guard", store)
        self.assertIn("last_start_request_id", store)
        self.assertIn("@Synchronized", store)


if __name__ == "__main__":
    unittest.main()
