package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import java.io.File

class HomeProjectDialogController(
    private val activity: Activity,
    private val store: ProjectStore,
    private val onProjectRemoved: (RenaultProject) -> Unit,
    private val onPreparedProjectDeleted: (RenaultProject, Boolean) -> Unit,
    private val onShareProgress: (Int, Int, String) -> Unit,
    private val onShareFinished: (String) -> Unit,
    private val onExportFolderPickRequested: (String) -> Unit,
    private val onSeparateVolumeExportStart: (String, String, List<String>) -> Unit,
) {
    private var activeKind: String = ""
    private var activeProjectId: String? = null
    private var activeParentKind: String? = null
    // Step-by-step export window state (#178). Presentation only: nothing is exported
    // until "Почати експорт", and MainActivity revalidates exact IDs before the service.
    private var exportWizardState = ProjectExportWizardState()
    private var exportWizard: ProjectExportWizard? = null

    fun restore(state: Bundle?) {
        activeKind =
            state
                ?.getString(STATE_KIND)
                .orEmpty()
        activeProjectId =
            state
                ?.getString(STATE_PROJECT_ID)
        activeParentKind =
            state
                ?.getString(STATE_PARENT_KIND)
        exportWizardState =
            if (activeKind == DIALOG_EXPORT_WIZARD) {
                ProjectExportWizardState.restore(
                    stepName = state?.getString(STATE_EXPORT_STEP),
                    selectedVolumeIds = state?.getStringArrayList(STATE_SELECTED_EXPORT_VOLUMES),
                    folderUri = state?.getString(STATE_EXPORT_FOLDER),
                    scrollY = state?.getInt(STATE_EXPORT_SCROLL_Y) ?: 0,
                )
            } else {
                ProjectExportWizardState()
            }
    }

    fun save(outState: Bundle) {
        outState.putString(
            STATE_KIND,
            activeKind,
        )
        outState.putString(
            STATE_PROJECT_ID,
            activeProjectId,
        )
        outState.putString(
            STATE_PARENT_KIND,
            activeParentKind,
        )
        outState.putStringArrayList(
            STATE_SELECTED_EXPORT_VOLUMES,
            ArrayList(exportWizardState.selectedVolumeIds),
        )
        outState.putString(STATE_EXPORT_STEP, exportWizardState.step.name)
        outState.putString(STATE_EXPORT_FOLDER, exportWizardState.folderUri)
        outState.putInt(STATE_EXPORT_SCROLL_Y, exportWizardState.scrollY)
    }

    /**
     * MainActivity hands back a SAF folder whose read/write grant is already persisted.
     * null (cancelled picker) keeps the previous choice. Never starts an export.
     */
    fun onExportFolderPicked(folderUri: String?) {
        if (activeKind != DIALOG_EXPORT_WIZARD) return
        exportWizardState.onFolderPicked(folderUri)
        exportWizard?.render()
    }

    fun restoreOpen() {
        val project =
            activeProjectId
                ?.let(store::project)
                ?: run {
                    clear()
                    return
                }

        when (activeKind) {
            DIALOG_ACTIONS ->
                showActions(project.id)

            DIALOG_REMOVE ->
                showRemoveConfirmation(project)

            DIALOG_DELETE_PREPARED ->
                confirmDeletePreparedProject(project)

            DIALOG_EXPORT_WIZARD ->
                showExportWizard(project)

            else ->
                clear()
        }
    }

    fun showActions(projectId: String) {
        val project =
            store.project(projectId)
                ?: return

        activeKind = DIALOG_ACTIONS
        activeProjectId = project.id

        val panel =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(activity, 18),
                    Ui.dp(activity, 8),
                    Ui.dp(activity, 18),
                    Ui.dp(activity, 8),
                )
            }

        lateinit var dialog: AlertDialog

        fun addAction(
            title: String,
            danger: Boolean = false,
            enabled: Boolean = true,
            action: () -> Unit,
        ) {
            panel.addView(
                Ui.textView(
                    context = activity,
                    value = "› " + title,
                    sizeSp = 18f,
                    color = when {
                        !enabled -> Ui.muted
                        danger -> Ui.danger
                        else -> Ui.text
                    },
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = enabled
                    isFocusable = enabled
                    isEnabled = enabled
                    background =
                        Ui.roundedBackground(
                            context = activity,
                            fill = Ui.surfaceAlt,
                            stroke = when {
                                !enabled -> Ui.border
                                danger -> Ui.danger
                                else -> Ui.accent
                            },
                            radiusDp = 11,
                        )
                    setPadding(
                        Ui.dp(activity, 14),
                        Ui.dp(activity, 12),
                        Ui.dp(activity, 14),
                        Ui.dp(activity, 12),
                    )
                    minHeight = Ui.dp(activity, 52)
                    setOnClickListener {
                        dialog.dismiss()
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = Ui.dp(activity, 10)
                },
            )
        }

        val projectVolumes =
            store.volumes(
                project.id,
            )
        val prepared =
            PreparedShareStore.existingProjectFile(
                activity,
                project,
                projectVolumes,
            )

        addAction("Експортувати томи окремо (.rdpkg)") {
            clear(DIALOG_ACTIONS)
            exportWizardState = ProjectExportWizardState()
            showExportWizard(project)
        }

        addAction("Поділитися проєктом одним файлом (.rdproject) · офлайн") {
            clear(DIALOG_ACTIONS)
            if (prepared != null) {
                sharePreparedProject(project, prepared)
            } else {
                shareProject(project)
            }
        }

        if (prepared != null) {
            // One prepared .rdproject per project: a new one only after deleting this one.
            addAction(
                title = "Перепакувати .rdproject · спершу видали підготовлений ⇧",
                enabled = false,
            ) {}
            addAction(
                title = "Видалити підготовлений .rdproject",
                danger = true,
            ) {
                clear(DIALOG_ACTIONS)
                activeParentKind = DIALOG_ACTIONS
                confirmDeletePreparedProject(project)
            }
        }
        addAction(
            title = "Видалити проєкт",
            danger = true,
        ) {
            clear(DIALOG_ACTIONS)
            activeParentKind = DIALOG_ACTIONS
            showRemoveConfirmation(project)
        }
        addAction("Скасувати") {
            clear(DIALOG_ACTIONS)
        }

        dialog =
            AlertDialog.Builder(activity)
                .setTitle(project.title)
                .setView(panel)
                .create()

        track(
            dialog = dialog,
            kind = DIALOG_ACTIONS,
            projectId = project.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.CHOICE,
        )
    }

    /**
     * Step-by-step export of separate volumes (#178): volumes → folder → confirm.
     * Checks never start SAF or export; stored opaque IDs survive rotation, not list indices.
     */
    private fun showExportWizard(project: RenaultProject) {
        val plan = runCatching {
            ProjectVolumeExportPlan.build(project, store.volumes(project.id))
        }.getOrElse {
            clear(DIALOG_EXPORT_WIZARD)
            showMessage("Експорт томів", it.message ?: "Помилка перевірки томів.")
            return
        }
        if (plan.isEmpty()) {
            clear(DIALOG_EXPORT_WIZARD)
            showMessage("Експорт томів", "У проєкті немає томів для експорту.")
            return
        }
        if (!exportWizardState.isConsistentWith(plan.map { it.volumeId })) {
            clear(DIALOG_EXPORT_WIZARD)
            showMessage("Вибір томів змінився", "Склад проєкту змінився. Вибери томи ще раз.")
            return
        }
        activeKind = DIALOG_EXPORT_WIZARD
        activeProjectId = project.id
        val wizard = ProjectExportWizard(
            activity = activity,
            project = project,
            plan = plan,
            state = exportWizardState,
            onClose = { clear(DIALOG_EXPORT_WIZARD) },
            onPickFolder = { onExportFolderPickRequested(project.id) },
            onStart = { folderUri, volumeIds ->
                // Validate current project and its exact selected IDs again before the service.
                val selected = runCatching {
                    val fresh = ProjectVolumeExportPlan.build(project, store.volumes(project.id))
                    ProjectVolumeExportPlan.select(fresh, volumeIds)
                }.getOrElse {
                    clear(DIALOG_EXPORT_WIZARD)
                    showMessage("Експорт томів", it.message ?: "Некоректний вибір томів.")
                    return@ProjectExportWizard
                }
                clear(DIALOG_EXPORT_WIZARD)
                onSeparateVolumeExportStart(project.id, folderUri, selected.map { it.volumeId })
            },
        )
        exportWizard = wizard
        val dialog = wizard.show()
        track(dialog, DIALOG_EXPORT_WIZARD, project.id)
    }

    private fun shareProject(
        project: RenaultProject,
    ) {
        val volumes = store.volumes(project.id)
        if (volumes.isEmpty()) {
            showMessage(
                title = "Поділитися проєктом",
                message = "У проєкті немає томів для поширення.",
            )
            return
        }

        val blocked =
            RdprojectExporter.blockingVolumes(volumes)
        if (blocked.isNotEmpty()) {
            val labels =
                blocked.joinToString("\n") {
                    "• " +
                        listOfNotNull(
                            it.documentCode,
                            it.date,
                        ).joinToString(" · ").ifBlank {
                            it.title
                        }
                }
            showMessage(
                title = "Не можна поділитися проєктом",
                message =
                    "Спочатку потрібні .rdpkg для таких томів:\n\n" +
                        labels,
            )
            return
        }

        if (ProjectFolderExportRunStore(activity).load().isRunning) {
            showMessage(
                title = "Експорт окремих томів",
                message = "Спочатку дочекайтеся завершення експорту томів.",
            )
            return
        }

        val existing =
            RdprojectShareRunStore(
                activity,
            ).load()

        if (
            existing.isRunning
        ) {
            showMessage(
                title = "Поділитися проєктом",
                message = "Пакування проєкту вже виконується.",
            )
            return
        }

        val started =
            RdprojectShareService.start(
                context = activity,
                projectId = project.id,
            )

        if (
            started
        ) {
            onShareProgress(
                0,
                0,
                "Готую…",
            )
        } else {
            onShareFinished(
                "Не вдалося запустити підготовку проєкту.",
            )
        }
    }

    private fun confirmDeletePreparedProject(
        project: RenaultProject,
    ) {
        activeKind = DIALOG_DELETE_PREPARED
        activeProjectId = project.id

        val dialog =
            AlertDialog.Builder(activity)
                .setTitle("Видалити підготовлений файл?")
                .setMessage(
                    "Буде видалено тільки підготовлений .rdproject для поширення. " +
                        "Сам проєкт, його томи та вихідні Renault-файли залишаться.",
                )
                .setNegativeButton("Скасувати") { _, _ ->
                    val returnToActions =
                        activeParentKind == DIALOG_ACTIONS
                    clear(DIALOG_DELETE_PREPARED)
                    if (returnToActions) {
                        showActions(project.id)
                    }
                }
                .setPositiveButton("Видалити") { _, _ ->
                    clear(DIALOG_DELETE_PREPARED)
                    val deleted =
                        PreparedShareStore.deleteProject(
                            activity,
                            project,
                            store.volumes(
                                project.id,
                            ),
                        )
                    onPreparedProjectDeleted(
                        project,
                        deleted,
                    )
                }
                .create()
        track(
            dialog = dialog,
            kind = DIALOG_DELETE_PREPARED,
            projectId = project.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.DANGER,
        )
    }

    private fun sharePreparedProject(
        project: RenaultProject,
        file: File,
    ) {
        if (!file.exists()) {
            showMessage(
                title = "Підготовлений файл",
                message = "Підготовлений .rdproject уже відсутній.",
            )
            return
        }
        val uri =
            FileProvider.getUriForFile(
                activity,
                activity.packageName + ".files",
                file,
            )
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        activity.startActivity(
            Intent.createChooser(
                send,
                "Поділитися проєктом " + project.title,
            ),
        )
    }

    private fun showMessage(
        title: String,
        message: String,
    ) {
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Закрити", null)
                .create()
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.CONFIRM,
        )
    }

    private fun showRemoveConfirmation(
        project: RenaultProject,
    ) {
        activeKind = DIALOG_REMOVE
        activeProjectId = project.id

        val volumeCount =
            store.volumes(project.id).size

        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(
                    "Видалити проєкт?",
                )
                .setMessage(
                    project.title +
                        "\n\nПроєкт і " +
                        volumeCountLabel(volumeCount) +
                        " буде прибрано з бібліотеки Renault Docs. " +
                        "Вихідні .rdpkg, SAF-папки та оригінальні Renault-файли на телефоні не видаляються.",
                )
                .setNegativeButton(
                    "Скасувати",
                ) { _, _ ->
                    val returnToActions =
                        activeParentKind == DIALOG_ACTIONS
                    clear(DIALOG_REMOVE)
                    if (returnToActions) {
                        showActions(project.id)
                    }
                }
                .setPositiveButton(
                    "Видалити проєкт",
                ) {
                    _,
                    _ ->
                    clear(DIALOG_REMOVE)
                    store.removeProject(project.id)
                    onProjectRemoved(project)
                }
                .create()

        track(
            dialog = dialog,
            kind = DIALOG_REMOVE,
            projectId = project.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.DANGER,
        )
    }

    /** #181: one confirmation before stopping a long Home operation; nothing else runs. */
    fun confirmCancelOperation(
        title: String,
        message: String,
        onConfirm: () -> Unit,
    ) {
        ConfirmActions.confirmStop(
            activity = activity,
            title = title,
            message = message,
            onConfirm = onConfirm,
        )
    }

    private fun track(
        dialog: AlertDialog,
        kind: String,
        projectId: String,
    ) {
        dialog.setOnDismissListener {
            if (
                !activity.isChangingConfigurations &&
                activeKind == kind &&
                activeProjectId == projectId
            ) {
                clear(kind)
            }
        }
    }

    private fun clear(
        expectedKind: String? = null,
    ) {
        if (
            expectedKind != null &&
            activeKind != expectedKind
        ) {
            return
        }
        if (activeKind == DIALOG_EXPORT_WIZARD) {
            exportWizardState = ProjectExportWizardState()
            exportWizard = null
        }
        activeKind = ""
        activeProjectId = null
        activeParentKind = null
    }

    private fun volumeCountLabel(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val noun =
            when {
                mod100 in 11..14 -> "томів"
                mod10 == 1 -> "том"
                mod10 in 2..4 -> "томи"
                else -> "томів"
            }
        return count.toString() + " " + noun
    }

    companion object {
        private const val STATE_KIND =
            "homeProjectDialogKind"
        private const val STATE_PROJECT_ID =
            "homeProjectDialogProjectId"
        private const val STATE_PARENT_KIND =
            "homeProjectDialogParentKind"
        private const val DIALOG_ACTIONS =
            "projectActions"
        private const val DIALOG_REMOVE =
            "removeProject"
        private const val DIALOG_DELETE_PREPARED =
            "deletePreparedProject"
        private const val DIALOG_EXPORT_WIZARD =
            "separateVolumesExportWizard"
        private const val STATE_SELECTED_EXPORT_VOLUMES =
            "homeProjectDialogSelectedExportVolumes"
        private const val STATE_EXPORT_STEP =
            "homeProjectDialogExportStep"
        private const val STATE_EXPORT_FOLDER =
            "homeProjectDialogExportFolder"
        private const val STATE_EXPORT_SCROLL_Y =
            "homeProjectDialogExportScrollY"
    }
}
