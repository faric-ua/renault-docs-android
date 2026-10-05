from pathlib import Path
import unittest


class V0561LiveProgressContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        root = Path(__file__).resolve().parents[1]
        return (root / path).read_text(encoding="utf-8")

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")
        self.assertIn('versionName = "0.5.61"', gradle)
        self.assertIn("versionCode = 77", gradle)

    def test_shared_progress_model_is_compact_and_measured(self):
        model = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/OperationProgress.kt"
        )
        self.assertIn("data class OperationProgress", model)
        self.assertIn("val current: Int?", model)
        self.assertIn("val total: Int?", model)
        self.assertIn("fun compactStage()", model)
        self.assertIn("fun displayText()", model)
        self.assertIn("itemCurrent", model)
        self.assertIn("itemTotal", model)
        self.assertIn("fun measured(", model)
        self.assertIn("fun indeterminate(", model)

    def test_operation_status_keeps_thin_bar_and_animates_progress(self):
        view = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/OperationStatusView.kt"
        )
        self.assertIn("progressBarStyleHorizontal", view)
        self.assertIn("Ui.dp(context, 5)", view)
        self.assertIn("setProgress(", view)
        self.assertIn("PROGRESS_SCALE", view)

    def test_rdpkg_import_export_and_share_persist_real_progress(self):
        importer = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgImporter.kt"
        )
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgExporter.kt"
        )
        import_store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgImportRunStore.kt"
        )
        export_store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgExportRunStore.kt"
        )
        share_store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdpkgShareRunStore.kt"
        )

        self.assertIn("progressState: ((OperationProgress) -> Unit)?", importer)
        self.assertIn('"payload_file_count"', importer)
        self.assertIn("PROGRESS_THROTTLE_MS", importer)
        self.assertIn("CountingInputStream", importer)
        self.assertIn('"Файлів"', importer)

        self.assertIn("progressState: ((OperationProgress) -> Unit)?", exporter)
        self.assertIn("OperationProgress.weightedItemsAndBytes(", exporter)

        for store in (import_store, export_store, share_store):
            self.assertIn("progressCurrent", store)
            self.assertIn("progressTotal", store)
            self.assertIn("progressStage", store)
            self.assertIn("fun updateProgress(", store)

    def test_native_preparation_exposes_staging_and_packaging_progress(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationEngine.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgRunStore.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgPreparationService.kt"
        )

        self.assertIn("onProgressState: (OperationProgress) -> Unit", engine)
        self.assertIn("progress.filesDone", engine)
        self.assertIn("progress.filesTotal", engine)
        self.assertIn("progress.bytesDone", engine)
        self.assertIn("progress.bytesTotal", engine)
        self.assertIn('"Пакую…"', engine)
        self.assertIn("progressCurrent", store)
        self.assertIn("progressTotal", store)
        self.assertIn("runStore.updateProgress(", service)

    def test_rdproject_preparation_is_service_owned_and_lifecycle_durable(self):
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectShareService.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectShareRunStore.kt"
        )
        controller = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )
        main = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        manifest = self._read("android/app/src/main/AndroidManifest.xml")

        self.assertIn("progressState: ((OperationProgress) -> Unit)?", exporter)
        self.assertIn("RdprojectShareService.start(", controller)
        self.assertNotIn("Thread {", controller)
        self.assertIn("class RdprojectShareRunStore", store)
        self.assertIn("class RdprojectShareService", service)
        self.assertIn("refreshRdprojectShareRunState()", main)
        self.assertIn("markChooserLaunched(", main)
        self.assertIn("RDPROJECT_RUN_REFRESH_MS", main)
        self.assertIn("100L", main)
        self.assertIn('android:name=".RdprojectShareService"', manifest)

    def test_project_screen_feeds_persisted_counts_to_shared_bar(self):
        project = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        self.assertIn("state.progressCurrent", project)
        self.assertIn("state.progressTotal", project)
        self.assertIn("NATIVE_RUN_REFRESH_MS =", project)
        self.assertIn("100L", project)

    def test_user_video_feedback_keeps_progress_balanced_and_non_bouncing(self):
        model = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/OperationProgress.kt"
        )
        status = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/OperationStatusView.kt"
        )
        project_export = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )

        self.assertIn("fun weightedItemsAndBytes(", model)
        self.assertIn("ITEM_WEIGHT", model)
        self.assertIn("7_500L", model)
        self.assertIn("BYTE_WEIGHT", model)
        self.assertIn("2_500L", model)
        self.assertIn("progressView.isIndeterminate =", status)
        self.assertIn("false", status)
        self.assertNotIn("progressView.isIndeterminate = !determinate", status)
        self.assertIn('"Пакую том"', project_export)
        self.assertIn("(index + 1)", project_export)
        self.assertIn("volumes.size", project_export)

    def test_project_share_reuses_current_prepared_bundle(self):
        controller = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        share = controller.split(
            'addAction("Поділитися проєктом")',
            1,
        )[1].split(
            'addAction("Перепакувати .rdproject")',
            1,
        )[0]

        self.assertIn("prepared !=", share)
        self.assertIn("sharePreparedProject(", share)
        self.assertIn("shareProject(", share)
        self.assertIn('"Перепакувати .rdproject"', controller)

        self.assertIn("private fun invalidatePreparedProject(", store)
        self.assertIn("invalidatePreparedProject(\n            projectId,", store)
        self.assertIn("invalidatePreparedProject(\n            fromProjectId,", store)
        self.assertIn("invalidatePreparedProject(\n            toProjectId,", store)

    def test_project_bar_matches_current_stage_instead_of_whole_bundle(self):
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )

        self.assertNotIn("progressUnitsPerPhase", exporter)
        self.assertNotIn("progressTotal", exporter)
        self.assertIn("volumeProgress.normalizedCurrent", exporter)
        self.assertIn("weighted.normalizedCurrent", exporter)

    def test_progress_identifies_volume_and_adapts_to_orientation(self):
        exporter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt"
        )
        status = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/OperationStatusView.kt"
        )

        self.assertIn("internal fun progressVolumeLabel(", exporter)
        self.assertIn("volume.documentCode", exporter)
        self.assertIn("volume.date", exporter)
        self.assertIn("internal fun volumeProgressStage(", exporter)
        self.assertIn('"Пакую том"', exporter)
        self.assertIn('(index + 1)', exporter)
        self.assertIn('" - "', exporter)

        self.assertIn("Configuration.ORIENTATION_LANDSCAPE", status)
        self.assertIn("detailView.maxLines =", status)
        self.assertIn("TextUtils.TruncateAt.MIDDLE", status)
        self.assertIn("TextUtils.TruncateAt.END", status)
        self.assertIn("configureDetailLayout()", status)

    def test_converter_ui_uses_compact_stage_copy(self):
        converter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        for label in (
            '"Сканую…"',
            '"Готую…"',
            '"Копіюю…"',
            '"Пакую…"',
            '"Перевіряю…"',
            '"Завершую…"',
        ):
            self.assertIn(label, converter)

        self.assertIn('" · Файлів: "', converter)
        self.assertNotIn('" · змінено файлів: "', converter)
        self.assertNotIn('" · виправлень: "', converter)


if __name__ == "__main__":
    unittest.main()
