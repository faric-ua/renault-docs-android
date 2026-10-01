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
        self.assertIn('"+ стару Renault"', main)
        self.assertIn('"+ готова"', main)
        self.assertIn("buildHomeAddPanel()", main)
        self.assertIn("buildToolsPanel()", main)
        self.assertIn('"Новий том"', main)
        self.assertIn('"Новий проєкт"', main)
        self.assertIn("R.drawable.ic_folder", main)
        self.assertIn(
            "Gravity.END or Gravity.BOTTOM",
            main,
        )
        self.assertIn(
            "FrameLayout.LayoutParams(",
            main,
        )

    def test_global_visual_hierarchy_and_shared_actions(self):
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        chooser = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectChooserActivity.kt"
        )
        dataset = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        )
        volume = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )
        create = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/CreateProjectActivity.kt"
        )

        self.assertIn("val entityTitle: Int", ui)
        self.assertIn("fun actionButton(", ui)
        self.assertIn("fun modeButton(", ui)

        for text in (
            main,
            project,
            chooser,
            dataset,
            volume,
            create,
        ):
            self.assertIn("Ui.entityTitle", text)

        self.assertIn('"Modern"', dataset)
        self.assertIn("Ui.modeButton(", dataset)
        self.assertIn("stroke =\n                            Ui.accent", dataset)
        self.assertIn("Ui.modeButton(", volume)
        self.assertIn("Ui.actionButton(", create)

    def test_mode_switch_is_shared_across_modern_and_classic_surfaces(self):
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )
        dataset = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        )
        volume = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )
        native = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        )
        viewer = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        )
        web_client = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        )
        package_writer = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/AndroidDatasetPackageWriter.kt"
        )

        self.assertIn("fun modeButton(", ui)

        for text in (
            dataset,
            volume,
            native,
            viewer,
        ):
            self.assertIn("Ui.modeButton(", text)

        self.assertIn(
            "compactLegacyInfoTitle",
            viewer,
        )
        self.assertIn(
            "Classic-довідка · дані й кількість томів",
            viewer,
        )
        self.assertIn(
            "applyReadmeMobileLayout(",
            web_client,
        )
        self.assertIn(
            "__renaultReadmeMobileStyle",
            web_client,
        )
        self.assertIn(
            "__renaultReadmeTable",
            web_client,
        )
        self.assertIn(
            '<table class="file-table">',
            package_writer,
        )
        self.assertIn(
            "<title>Як користуватися —",
            package_writer,
        )

    def test_converter_uses_renault_docs_action_styling(self):
        converter = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )

        self.assertIn("fun applyActionStyle(", ui)
        self.assertGreaterEqual(
            converter.count("Ui.applyActionStyle("),
            6,
        )
        self.assertIn("Ui.entityTitle", converter)

    def test_stale_legacy_dataset_is_validated_before_open(self):
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        modern = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        )

        self.assertIn("openLegacyDataset(", main)
        self.assertIn("DatasetReader.read(", main)
        self.assertIn("більше недоступна за збереженим шляхом", main)
        self.assertIn("додай її знову через Legacy", main)
        self.assertIn("Ця стара бібліотека більше не читається", modern)

    def test_project_add_actions_are_one_tile_with_auto_and_manual(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("buildAddPanel()", project)
        self.assertIn('value =\n                        "Додати"', project)
        self.assertIn('"Авто"', project)
        self.assertIn('".rdpkg · один том"', project)
        self.assertIn('"Вручну"', project)
        self.assertIn('"Папка / SAF"', project)
        self.assertIn("openPackagePicker()", project)
        self.assertIn("openVolumePicker(", project)
        self.assertIn("HELP_ADD", project)
        self.assertIn("HELP_RAW", project)

    def test_action_tile_subtitles_use_one_compact_style(self):
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn(
            "const val actionSubtitleSp: Float = 11f",
            ui,
        )
        self.assertGreaterEqual(
            main.count("Ui.actionSubtitleSp"),
            2,
        )
        self.assertGreaterEqual(
            project.count("Ui.actionSubtitleSp"),
            2,
        )

    def test_settings_use_compact_secondary_typography(self):
        settings = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/SettingsActivity.kt"
        )

        self.assertIn('"Резервні копії"', settings)
        self.assertGreaterEqual(
            settings.count("Ui.secondaryTextSp"),
            4,
        )
        self.assertGreaterEqual(
            settings.count("Ui.valueTextSp"),
            2,
        )
        self.assertEqual(
            2,
            settings.count("Ui.compactButtonSp"),
        )
        self.assertIn("Ui.surfaceAlt", settings)
        self.assertIn("stroke =\n                                Ui.accent", settings)
        self.assertIn("stroke =\n                                Ui.border", settings)
        self.assertNotIn('sectionTitle(\n                "Backup"', settings)
        self.assertEqual(
            3,
            settings.count("DialogRole.CHOICE"),
        )
        self.assertGreaterEqual(
            settings.count("DialogUi.apply("),
            3,
        )

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
        self.assertIn("DialogUi.apply(", helper)
        self.assertIn("DialogRole.HELP", helper)

    def test_every_app_owned_alert_dialog_uses_shared_theme(self):
        base = (
            self.repo
            / "android/app/src/main/java/com/saney/renaultdocs"
        )

        audited = {}
        total_builders = 0

        for path in base.glob("*.kt"):
            text = path.read_text(encoding="utf-8")
            builders = text.count("AlertDialog.Builder")

            if not builders:
                continue

            applies = text.count("DialogUi.apply(")
            audited[path.name] = (builders, applies)
            total_builders += builders

            self.assertGreaterEqual(
                applies,
                builders,
                msg=f"{path.name}: every AlertDialog must use DialogUi",
            )

        self.assertEqual(
            {
                "LifecycleHelpDialogController.kt": (1, 1),
                "ProjectActivity.kt": (5, 5),
                "SettingsActivity.kt": (3, 3),
            },
            audited,
        )
        self.assertEqual(9, total_builders)

    def test_dialog_theme_defines_roles_and_shared_surface(self):
        dialog_ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/DialogUi.kt"
        )

        for role in (
            "HELP",
            "CHOICE",
            "CONFIRM",
            "DANGER",
            "PROGRESS",
        ):
            self.assertIn(role, dialog_ui)

        self.assertIn("Ui.surfaceAlt", dialog_ui)
        self.assertIn("Ui.border", dialog_ui)
        self.assertIn("Ui.accent", dialog_ui)
        self.assertIn("Ui.danger", dialog_ui)
        self.assertIn("Ui.compactButtonSp", dialog_ui)

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
