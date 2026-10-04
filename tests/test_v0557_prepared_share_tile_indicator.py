from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"

class PreparedShareTileIndicatorContractTest(unittest.TestCase):
    def read(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_project_tile_reflects_prepared_rdproject_file(self):
        main = self.read("MainActivity.kt")
        self.assertIn("PreparedShareStore.projectFile(", main)
        self.assertIn("hasPreparedShare", main)
        self.assertIn("Підготовлений .rdproject готовий для передачі", main)

    def test_volume_tile_reflects_prepared_rdpkg_file(self):
        project = self.read("ProjectActivity.kt")
        card = project[project.index("private fun buildVolumeCard"):project.index("private fun openVolume", project.index("private fun buildVolumeCard"))]
        self.assertIn("PreparedShareStore.hasVolumeFile(", card)
        self.assertIn("Підготовлений .rdpkg готовий для передачі", card)

    def test_volume_indicator_refreshes_immediately_after_prepared_file_delete(self):
        project = self.read("ProjectActivity.kt")
        delete = project[project.index("private fun confirmDeletePreparedVolume"):project.index("private fun sharePreparedRdpkg(", project.index("private fun confirmDeletePreparedVolume"))]
        self.assertIn("if (deleted) {", delete)
        self.assertIn("render()", delete)

if __name__ == "__main__":
    unittest.main()
