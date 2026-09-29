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

    def test_release_version(self):
        self.assertIn('versionName = "0.5.52"', self.gradle)
        self.assertIn("versionCode = 68", self.gradle)

    def test_terminal_status_has_explicit_dismiss_action(self):
        self.assertIn('value =\n                            "×"', self.project)
        self.assertIn('"Закрити статус"', self.project)
        self.assertIn("nativeRunStore.dismissTerminal(", self.project)

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
