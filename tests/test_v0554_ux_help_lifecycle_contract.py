from pathlib import Path
import unittest


RETIRED_V05112 = "v0.5.112: «Конвертер»/«Legacy»/«Старі бібліотеки» removed by owner decision 2026-10-12; code kept in branch archive/legacy-converter-v0.5.111"

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
        # v0.5.112: tool cards «+ стару Renault» / «+ готова» removed with
        # «Конвертер» and «Legacy» (archive branch archive/legacy-converter-v0.5.111).
        self.assertNotIn('"+ стару Renault"', main)
        self.assertNotIn('"+ готова"', main)
        self.assertIn("buildHomeAddPanel()", main)
        self.assertNotIn("buildToolsPanel()", main)
        self.assertIn('"Новий том"', main)
        self.assertIn('"Новий проєкт"', main)
        # "R.drawable.ic_folder" and its "Gravity.END or Gravity.BOTTOM" placement
        # belonged to the tool cards — removed in v0.5.112.
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
        # v0.5.112: converter removed (archive branch archive/legacy-converter-v0.5.111).
        # AndroidDatasetPackageWriter (README/«Як користуватися» page) went with it.

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
            "compactReadmeResponse(",
            web_client,
        )
        self.assertIn(
            "renderCompactReadmeHtml(",
            web_client,
        )
        self.assertIn(
            '"X-Renault-Source" to',
            web_client,
        )
        self.assertIn(
            '"compact-readme"',
            web_client,
        )
        self.assertIn(
            "README_LAYOUT_QUERY",
            web_client,
        )
        self.assertIn(
            '"compact-v2"',
            web_client,
        )
        self.assertIn(
            "appendQueryParameter(",
            web_client,
        )
        self.assertNotIn(
            "applyReadmeMobileLayout(",
            web_client,
        )

    @unittest.skip(RETIRED_V05112)
    def test_converter_uses_renault_docs_action_styling(self):
        converter = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )

        self.assertIn("fun applyActionStyle(", ui)
        # v0.5.108: «Додати готову папку в бібліотеку» removed (owner).
        self.assertGreaterEqual(
            converter.count("Ui.applyActionStyle("),
            5,
        )
        self.assertIn("Ui.entityTitle", converter)

    @unittest.skip(RETIRED_V05112)
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

    def test_home_status_survives_rotation(self):
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )

        self.assertIn("STATE_STATUS_TEXT", main)
        self.assertIn("STATE_STATUS_COLOR", main)
        self.assertIn("statusText.currentTextColor", main)
        self.assertIn("outState.putCharSequence(", main)
        self.assertIn("outState.putInt(", main)
        self.assertIn("restoredStatusText", main)
        self.assertIn("restoredStatusColor", main)

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
        # 2 until v0.5.111; the tool cards (Конвертер/Legacy) were removed in v0.5.112.
        self.assertGreaterEqual(
            main.count("Ui.actionSubtitleSp"),
            1,
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
            4,  # v0.5.135: + «Де зберігати нові томи»
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

    def test_prepared_share_delete_confirmations_restore_after_rotation(self):
        home = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn("DIALOG_DELETE_PREPARED", home)
        self.assertIn(
            "DIALOG_DELETE_PREPARED ->\n                confirmDeletePreparedProject(project)",
            home,
        )
        prepared_project = home.split(
            "private fun confirmDeletePreparedProject", 1
        )[1].split("private fun sharePreparedProject", 1)[0]
        self.assertIn("activeKind = DIALOG_DELETE_PREPARED", prepared_project)
        self.assertIn("track(", prepared_project)
        self.assertIn("clear(DIALOG_DELETE_PREPARED)", prepared_project)

        self.assertIn("DIALOG_DELETE_PREPARED_VOLUME", project)
        restore = project.split("private fun restoreProjectDialog()", 1)[1]
        restore = restore.split("private fun restorePreparedVolumeChooser", 1)[0]
        self.assertIn("DIALOG_DELETE_PREPARED_VOLUME ->", restore)
        self.assertIn("confirmDeletePreparedVolume(volume)", restore)
        prepared_volume = project.split(
            "private fun confirmDeletePreparedVolume", 1
        )[1].split("private fun sharePreparedRdpkg", 1)[0]
        self.assertIn("setProjectDialogState(", prepared_volume)
        self.assertIn("trackProjectDialog(", prepared_volume)
        self.assertIn(
            "clearProjectDialogState(DIALOG_DELETE_PREPARED_VOLUME)",
            prepared_volume,
        )

    def test_prepared_volume_delete_uses_single_dialog_and_returns_to_actions(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        self.assertIn("STATE_ACTIVE_DIALOG_PARENT_KIND", project)
        delete_action = project.split(
            'title = "Видалити підготовлений .rdpkg"', 1
        )[1].split('addAction("Перемістити в інший проєкт")', 1)[0]
        self.assertIn(
            "clearProjectDialogState(DIALOG_VOLUME_ACTIONS)",
            delete_action,
        )
        self.assertIn(
            "activeProjectDialogParentKind = DIALOG_VOLUME_ACTIONS",
            delete_action,
        )
        self.assertIn(
            "confirmDeletePreparedVolume(volume)",
            delete_action,
        )
        self.assertNotIn(
            "showVolumeActionsBehindPreparedDelete",
            project,
        )
        restore = project.split("private fun restoreProjectDialog()", 1)[1]
        restore = restore.split("private fun restorePreparedVolumeChooser", 1)[0]
        self.assertIn(
            "DIALOG_DELETE_PREPARED_VOLUME ->\n                        confirmDeletePreparedVolume(volume)",
            restore,
        )
        prepared = project.split(
            "private fun confirmDeletePreparedVolume", 1
        )[1].split("private fun sharePreparedRdpkg", 1)[0]
        self.assertIn('setNegativeButton("Скасувати")', prepared)
        self.assertIn("showVolumeActions(volume)", prepared)

    def test_project_destructive_confirmations_return_to_project_actions(self):
        home = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )
        self.assertIn("STATE_PARENT_KIND", home)
        prepared_action = home.split(
            'title = "Видалити підготовлений .rdproject"', 1
        )[1].split('title = "Видалити проєкт"', 1)[0]
        self.assertIn("clear(DIALOG_ACTIONS)", prepared_action)
        self.assertIn("activeParentKind = DIALOG_ACTIONS", prepared_action)
        remove_action = home.split(
            'title = "Видалити проєкт"', 1
        )[1].split('addAction("Скасувати")', 1)[0]
        self.assertIn("clear(DIALOG_ACTIONS)", remove_action)
        self.assertIn("activeParentKind = DIALOG_ACTIONS", remove_action)
        prepared = home.split(
            "private fun confirmDeletePreparedProject", 1
        )[1].split("private fun sharePreparedProject", 1)[0]
        self.assertIn('setNegativeButton("Скасувати")', prepared)
        self.assertIn("showActions(project.id)", prepared)
        remove = home.split(
            "private fun showRemoveConfirmation", 1
        )[1].split("private fun track(", 1)[0]
        self.assertIn('"Скасувати"', remove)
        self.assertIn("showActions(project.id)", remove)

    def test_project_share_uses_ukrainian_volume_plural(self):
        service = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/RdprojectShareService.kt"
        )
        self.assertNotIn("том(ів)", service)
        self.assertIn("volumeCountLabel(", service)
        self.assertIn("volumes.size", service)
        self.assertIn("mod100 in", service)
        self.assertIn('"том"', service)
        self.assertIn('"томи"', service)
        self.assertIn('"томів"', service)

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
                # v0.5.104: Home's stop question moved into shared ConfirmActions.
                "ConfirmActions.kt": (1, 1),
                # v0.5.106: Home messages and the «Неправильне джерело» message
                # moved into shared RestorableDialogs (they survive rotation).
                "HomeProjectDialogController.kt": (5, 5),  # v0.5.147: delete volumes with files — list + confirmation
                "RestorableDialogs.kt": (1, 1),
                "LifecycleHelpDialogController.kt": (1, 1),
                "ProjectActivity.kt": (9, 9),
                "ProjectExportWizard.kt": (1, 1),
                "SettingsActivity.kt": (4, 4),  # v0.5.135: + «Де зберігати нові томи»
                # v0.5.150: update windows — one builder, shown and themed in one place.
                "AppUpdateUi.kt": (1, 1),
            },
            audited,
        )
        # v0.5.101 (#178): two export dialogs replaced by one step-by-step window.
        # v0.5.102 (#181): one confirmation before cancelling a long operation.
        # v0.5.135 (TODO №1): + Settings «Де зберігати нові томи».
        # v0.5.147: + «Видалити томи разом з файлами» (list + confirmation).
        # v0.5.150: + AppUpdateUi (one builder for all update windows).
        self.assertEqual(23, total_builders)

    def test_dialog_action_buttons_are_readable(self):
        dialog_ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/DialogUi.kt"
        )
        ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/Ui.kt"
        )

        self.assertIn("color = Ui.text", dialog_ui)
        self.assertIn("Ui.danger", dialog_ui)
        self.assertIn("Ui.secondaryButtonTextSp", dialog_ui)
        self.assertIn(
            "const val secondaryButtonTextSp: Float = 14f",
            ui,
        )

    def test_project_actions_use_real_dialog_buttons(self):
        controller = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )

        show_actions = controller.split("fun showActions(projectId: String)", 1)[1]
        show_actions = show_actions.split("private fun showExportWizard", 1)[0]

        self.assertNotIn(".setItems(", show_actions)
        self.assertIn('"Експортувати томи окремо (.rdpkg)"', show_actions)
        # v0.5.122 (#173): the share title comes from preparedShareTitle().
        self.assertIn("preparedShareTitle(", show_actions)
        self.assertIn('"Поділитися проєктом одним файлом (.rdproject) · офлайн"', controller)
        self.assertIn('"Видалити проєкт"', show_actions)
        self.assertIn('"Скасувати"', show_actions)
        self.assertIn("role = DialogRole.CHOICE", show_actions)
        # The *actions panel* must not be a list. The separate-volume export is a
        # step-by-step window with checkbox tiles (#178).
        wizard = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectExportWizard.kt"
        )
        self.assertIn("CheckBox(activity)", wizard)
        self.assertIn("DialogUi.apply(dialog, DialogRole.CHOICE)", wizard)

    def test_dialog_actions_have_visible_button_chrome(self):
        dialog_ui = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/DialogUi.kt"
        )

        self.assertIn("stroke = Ui.border", dialog_ui)
        self.assertIn("DialogRole.DANGER", dialog_ui)
        self.assertIn("Ui.danger", dialog_ui)
        self.assertIn("background =", dialog_ui)
        self.assertIn("radiusDp = 10", dialog_ui)
        self.assertIn("minHeight = Ui.dp(context, 44)", dialog_ui)

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
        self.assertIn("Ui.secondaryButtonTextSp", dialog_ui)

    def test_complex_surfaces_use_same_help_lifecycle(self):
        paths = [
            "MainActivity.kt",
            "ProjectActivity.kt",
            # v0.5.112: converter removed (archive branch archive/legacy-converter-v0.5.111).
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

    def test_project_preserves_scroll_position_on_rotation(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )

        self.assertIn(
            "STATE_SCROLL_Y",
            project,
        )
        self.assertIn(
            "projectScroll.scrollY",
            project,
        )
        self.assertIn(
            "restoreProjectScroll()",
            project,
        )
        self.assertIn(
            "projectScroll.scrollTo(",
            project,
        )

    def test_native_section_keeps_parent_controls_with_detail_tables(self):
        native = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        )

        self.assertIn(
            "prepareDocumentBody(",
            native,
        )
        self.assertIn(
            "currentDocumentParentPanelId",
            native,
        )
        self.assertIn(
            "STATE_DOCUMENT_PARENT_PANEL_ID",
            native,
        )
        self.assertIn(
            "showSectionHeading =",
            native,
        )
        self.assertIn(
            "parentPanelId =",
            native,
        )

    def test_connector_composite_actions_use_native_styling(self):
        native = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        )

        composite = native.split(
            "private fun addCompositePartButton",
            1,
        )[1].split(
            "private fun prepareDocumentBody",
            1,
        )[0]

        self.assertIn("Ui.surfaceAlt", composite)
        self.assertIn("Ui.accent", composite)
        self.assertIn("Ui.text", composite)
        self.assertIn("radiusDp = 10", composite)

    def test_runtime_heavy_surfaces_reuse_loaded_data_on_rotation(self):
        native = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        )
        documentation = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/VolumeDocumentationActivity.kt"
        )
        dataset = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        )

        for text in (
            native,
            documentation,
            dataset,
        ):
            self.assertIn(
                "onRetainNonConfigurationInstance",
                text,
            )
            self.assertIn(
                "lastNonConfigurationInstance",
                text,
            )
            self.assertIn(
                "STATE_SCROLL_Y",
                text,
            )
            self.assertIn(
                ".scrollY",
                text,
            )
            self.assertIn(
                ".scrollTo(",
                text,
            )

    def test_modern_volume_retains_loaded_sections_on_rotation(self):
        volume = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )

        self.assertIn(
            "onRetainNonConfigurationInstance",
            volume,
        )
        self.assertIn(
            "lastNonConfigurationInstance",
            volume,
        )
        self.assertIn(
            "showLoadedSections(",
            volume,
        )
        self.assertIn(
            "STATE_SCROLL_Y",
            volume,
        )
        self.assertIn(
            "sectionScroll.scrollY",
            volume,
        )
        self.assertIn(
            "sectionScroll.scrollTo(",
            volume,
        )

        manifest = self.read(
            "android/app/src/main/AndroidManifest.xml"
        )
        modern_volume_manifest = manifest.split(
            'android:name=".ModernVolumeActivity"',
            1,
        )[1].split(
            "/>",
            1,
        )[0]
        self.assertIn(
            'android:configChanges="orientation|screenSize"',
            modern_volume_manifest,
        )

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
