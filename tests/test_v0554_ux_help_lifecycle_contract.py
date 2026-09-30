from pathlib import Path
import unittest


class V0554UxHelpLifecycleContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]

    def read(self, path: str) -> str:
        return (self.repo / path).read_text(encoding="utf-8")

    def test_home_copy_and_empty_project_copy(self):
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )

        self.assertIn('"До проєкту"', main)
        self.assertIn('"Порожній · додай том"', main)
        self.assertNotIn('"У вибраний проєкт"', main)
        self.assertNotIn('"Порожній · додай потрібний том"', main)

    def test_project_add_actions_are_one_tile_with_auto_and_manual(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("buildAddPanel()", project)
        self.assertIn('value =\n                        "Додати"', project)
        self.assertIn('"Авто"', project)
        self.assertIn('"Вручну"', project)
        self.assertIn("openPackagePicker()", project)
        self.assertIn("openVolumePicker(", project)
        self.assertIn("HELP_ADD", project)
        self.assertIn("HELP_RAW", project)

    def test_help_controller_restores_only_presentation_state(self):
        helper = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/LifecycleHelpDialogController.kt"
        )

        self.assertIn("STATE_ACTIVE_HELP_ID", helper)
        self.assertIn("fun restore(", helper)
        self.assertIn("fun save(", helper)
        self.assertIn("fun restoreOpen()", helper)
        self.assertIn("isChangingConfigurations", helper)
        self.assertIn('"Зрозуміло"', helper)

    def test_complex_surfaces_use_same_help_lifecycle(self):
        paths = [
            "MainActivity.kt",
            "ProjectActivity.kt",
            "ConversionActivity.kt",
            "ModernVolumeActivity.kt",
            "NativeSectionActivity.kt",
            "VolumeDocumentationActivity.kt",
        ]

        base = (
            self.repo
            / "android/app/src/main/java/com/saney/renaultdocs"
        )

        for name in paths:
            text = (base / name).read_text(encoding="utf-8")
            self.assertIn(
                "LifecycleHelpDialogController",
                text,
                msg=name,
            )
            self.assertIn(
                "helpDialogs.restore(",
                text,
                msg=name,
            )
            self.assertIn(
                "helpDialogs.restoreOpen()",
                text,
                msg=name,
            )
            self.assertIn(
                "helpDialogs.save(",
                text,
                msg=name,
            )

    def test_project_dialogs_restore_without_automatic_actions(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("STATE_ACTIVE_DIALOG_KIND", project)
        self.assertIn("restoreProjectDialog()", project)
        self.assertIn("restorePreparedVolumeChooser()", project)
        self.assertIn("restoreProjectMismatchDialog()", project)
        self.assertIn("trackProjectDialog(", project)
        self.assertIn("isChangingConfigurations", project)
        self.assertIn("STATE_PENDING_MANUAL_IMPORT", project)
        self.assertIn("savedInstanceState !=\n                null", project)

    def test_create_project_keeps_typed_name_on_rotation(self):
        create = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/CreateProjectActivity.kt"
        )

        self.assertIn("STATE_PROJECT_NAME", create)
        self.assertIn("onSaveInstanceState(", create)
        self.assertIn("nameInput.text", create)
        self.assertIn("nameInput.setText(", create)

    def test_release_version(self):
        gradle = self.read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )

        self.assertGreaterEqual(version_code, 70)
        self.assertGreaterEqual(version_name, (0, 5, 54))


if __name__ == "__main__":
    unittest.main()
