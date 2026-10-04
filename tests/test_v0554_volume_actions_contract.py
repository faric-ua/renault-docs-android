from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class VolumeActionsContractTest(unittest.TestCase):
    def read(self, path):
        return (ROOT / path).read_text(encoding="utf-8")

    def test_volume_actions_are_panel_buttons_not_set_items(self):
        src = self.read("android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt")
        block = src.split("private fun showVolumeActions(", 1)[1].split("private fun showMoveVolumeDialog(", 1)[0]
        self.assertNotIn(".setItems(", block)
        for label in (
            "Експортувати .rdpkg",
            "Поділитися томом",
            "Перемістити в інший проєкт",
            "Видалити з проєкту",
            "Скасувати",
        ):
            self.assertIn(label, block)

    def test_move_is_identity_preserving_and_rotation_restorable(self):
        activity = self.read("android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt")
        store = self.read("android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt")
        self.assertIn("DIALOG_MOVE_VOLUME", activity)
        self.assertIn("showMoveVolumeDialog(volume)", activity)
        self.assertIn("fun moveVolume(", store)
        self.assertIn("toProjectId to volume", store)

    def test_share_uses_rdpkg_exporter_and_secure_file_provider(self):
        activity = self.read("android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt")
        share_service = self.read("android/app/src/main/java/com/saney/renaultdocs/RdpkgShareService.kt")
        manifest = self.read("android/app/src/main/AndroidManifest.xml")
        paths = self.read("android/app/src/main/res/xml/file_paths.xml")
        self.assertIn("RdpkgShareService.start(", activity)
        self.assertIn("RdpkgExporter.export(", share_service)
        self.assertIn("Intent.ACTION_SEND", activity)
        self.assertIn("FileProvider.getUriForFile(", activity)
        self.assertIn("androidx.core.content.FileProvider", manifest)
        self.assertIn('path="shared-rdpkg/"', paths)

if __name__ == "__main__":
    unittest.main()
