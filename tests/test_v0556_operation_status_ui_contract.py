from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class OperationStatusUiContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_shared_surface_has_title_detail_progress_and_terminal_close(self):
        ui = self.read("OperationStatusView.kt")
        self.assertIn("class OperationStatusView", ui)
        self.assertIn('"Renault Docs"', ui)
        self.assertIn("progressBarStyleHorizontal", ui)
        self.assertIn("fun showRunning(", ui)
        self.assertIn("fun showTerminal(", ui)
        self.assertIn('contentDescription = "Закрити статус"', ui)

    def test_project_activity_uses_shared_surface_for_durable_volume_flows(self):
        activity = self.read("ProjectActivity.kt")
        self.assertIn('operationStatus.showRunning("Імпорт тому"', activity)
        self.assertIn('"Створення .rdpkg"', activity)

    def test_home_project_share_uses_shared_surface(self):
        home = self.read("MainActivity.kt")
        self.assertIn('"Підготовка проєкту"', home)
        self.assertIn("operationStatus.showTerminal(", home)


if __name__ == "__main__":
    unittest.main()
