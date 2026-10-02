from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ProjectShareContractTest(unittest.TestCase):
    def read(self, path):
        return (ROOT / path).read_text(encoding="utf-8")

    def test_project_menu_is_modern_panel_and_has_share(self):
        src = self.read("android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt")
        block = src.split("fun showActions(projectId: String)", 1)[1].split("private fun shareProject", 1)[0]
        self.assertNotIn(".setItems(", block)
        self.assertIn("Поділитися проєктом", block)
        self.assertIn("Видалити проєкт", block)
        self.assertIn("Скасувати", block)
        self.assertIn("stroke = if (danger) Ui.danger else Ui.accent", block)

    def test_project_share_keeps_rdpkg_one_volume_contract(self):
        exporter = self.read("android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt")
        rdpkg = self.read("android/app/src/main/java/com/saney/renaultdocs/RdpkgExporter.kt")
        self.assertIn('const val FORMAT = "renault-docs-project"', exporter)
        self.assertIn('".rdproject"', exporter)
        self.assertIn("RdpkgExporter.export(", exporter)
        self.assertIn('"volumes"', exporter)
        self.assertIn("volumes.length() ==", rdpkg)
        self.assertIn("1,", rdpkg)

    def test_project_share_refuses_partial_saf_project(self):
        exporter = self.read("android/app/src/main/java/com/saney/renaultdocs/RdprojectExporter.kt")
        controller = self.read("android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt")
        self.assertIn("filterNot(RdpkgExporter::canFastExport)", exporter)
        self.assertIn("blocked.isNotEmpty()", controller)
        self.assertIn("Спочатку потрібні .rdpkg", controller)

if __name__ == "__main__":
    unittest.main()
