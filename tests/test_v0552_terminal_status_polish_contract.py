from pathlib import Path
import unittest


class V0552TerminalStatusPolishContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]
        cls.project = (
            cls.repo
            / "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        ).read_text(encoding="utf-8")
        cls.run_store = (
            cls.repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeRdpkgRunStore.kt"
        ).read_text(encoding="utf-8")
        cls.importer = (
            cls.repo
            / "android/app/src/main/java/com/saney/renaultdocs/RdpkgImporter.kt"
        ).read_text(encoding="utf-8")
        cls.gradle = (
            cls.repo / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")
        cls.operation_status = (
            cls.repo
            / "android/app/src/main/java/com/saney/renaultdocs/OperationStatusView.kt"
        ).read_text(encoding="utf-8")

    def test_release_version(self):
        version_code = int(
            self.gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in self.gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 68)
        self.assertGreaterEqual(version_name, (0, 5, 52))

    def test_terminal_status_has_explicit_dismiss_action(self):
        self.assertIn('"×"', self.operation_status)
        self.assertIn('"Закрити статус"', self.operation_status)
        self.assertIn("nativeRunStore.dismissTerminal(", self.project)
        self.assertNotIn('value =\n                            "×"', self.project)

    def test_terminal_dismissal_is_persistent_and_scoped_to_finished_run(self):
        self.assertIn("dismissedFinishedAtMs", self.run_store)
        self.assertIn("isTerminalDismissed", self.run_store)
        self.assertIn("KEY_DISMISSED_FINISHED_AT", self.run_store)
        self.assertIn("state.finishedAtMs != finishedAtMs", self.run_store)
        self.assertIn(".commit()", self.run_store)

    def test_active_run_never_exposes_terminal_dismiss_row(self):
        refresh = self.project.split(
            "private fun refreshNativeRunState()",
            1,
        )[1].split(
            "private fun showNativeTerminalStatus",
            1,
        )[0]

        self.assertIn("state.isRunning", refresh)
        self.assertIn("hideNativeTerminalStatus()", refresh)

    def test_dismissed_terminal_state_is_not_restored_after_recreation(self):
        self.assertIn("if (\n            state.isTerminalDismissed", self.project)
        self.assertIn("hideNativeTerminalStatus()", self.project)

    def test_active_status_surface_tracks_live_run_message(self):
        refresh = self.project.split(
            "private fun refreshNativeRunState()",
            1,
        )[1].split(
            "private fun showNativeTerminalStatus",
            1,
        )[0]
        self.assertIn("operationStatus.showRunning(", refresh)
        self.assertIn("state.message.ifBlank", refresh)
        self.assertIn(
            "private const val NATIVE_RUN_REFRESH_MS =\n            750L",
            self.project,
        )
        self.assertNotIn("nativeRunProgressDialog", self.project)

    def test_terminal_status_replaces_running_surface(self):
        self.assertIn("fun showTerminal(", self.operation_status)
        self.assertIn("cancelView.visibility = View.GONE", self.operation_status)
        self.assertIn("closeView.visibility = View.VISIBLE", self.operation_status)

    def test_rdpkg_progress_uses_ukrainian_file_count_helper(self):
        self.assertIn("ukrainianFileCount(", self.importer)
        self.assertNotIn('" файлів",\n                    )', self.importer)
        wording_test = (
            self.repo
            / "android/app/src/test/java/com/saney/renaultdocs/RdpkgImporterWordingTest.kt"
        ).read_text(encoding="utf-8")
        self.assertIn('"1 файл"', wording_test)
        self.assertIn('"2 файли"', wording_test)
        self.assertIn('"11 файлів"', wording_test)
        self.assertIn('"21 файл"', wording_test)


if __name__ == "__main__":
    unittest.main()
