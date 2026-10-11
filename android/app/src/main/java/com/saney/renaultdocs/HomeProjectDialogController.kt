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
    private val onSeparateVolumeExportRequested: (String, List<String>) -> Unit,
) {
    private var activeKind: String = ""
    private var activeProjectId: String? = null
    private var activeParentKind: String? = null

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

            DIALOG_SEPARATE_VOLUMES ->
                showSeparateVolumePreview(project)

            DIALOG_SINGLE_VOLUME ->
                showSingleVolumeChooser(project)

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
            action: () -> Unit,
        ) {
            panel.addView(
                Ui.textView(
                    context = activity,
                    value = "› " + title,
                    sizeSp = 18f,
                    color = if (danger) Ui.danger else Ui.text,
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    background =
                        Ui.roundedBackground(
                            context = activity,
                            fill = Ui.surfaceAlt,
                            stroke = if (danger) Ui.danger else Ui.accent,
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
            showSeparateVolumePreview(project)
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
            addAction("Перепакувати .rdproject") {
                clear(DIALOG_ACTIONS)
                shareProject(
                    project,
                )
            }
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

    private fun showSeparateVolumePreview(
        project: RenaultProject,
    ) {
        activeKind = DIALOG_SEPARATE_VOLUMES
        activeProjectId = project.id
        val volumes = store.volumes(project.id)
        val supported = volumes.count(RdpkgExporter::canFastExport)
        val blocked = volumes.size - supported
        val duplicateNames = runCatching {
            ProjectVolumeExportPlan.build(project, volumes)
        }.isFailure && blocked == 0 && volumes.isNotEmpty()

        val message = buildString {
            append("Основний спосіб: кожен том — окремий .rdpkg у вибраній папці.\n")
            append("Один великий .rdproject НЕ створюється.\n\n")
            append("Томів у проєкті: ${volumes.size}\n")
            append("Готові до окремого експорту: $supported\n")
            if (blocked > 0) {
                append("Потребують підготовки: $blocked\n")
            }
            if (duplicateNames) {
                append("Є збіг назв файлів, експортування призупинено.\n")
            }
            append("\nНаявні однойменні файли НЕ перезаписуються і не вважаються перевіреними. ")
            append("У кінці буде створено невеликий JSON-звіт. ")
            append("Це ще не публікація каталогу Google Drive.")
        }
        val canProceed = supported == volumes.size && supported > 0 && !duplicateNames
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Експортувати окремі томи")
            .setMessage(message)
            .setNegativeButton("Назад") { _, _ ->
                clear(DIALOG_SEPARATE_VOLUMES)
                showActions(project.id)
            }
            .setNeutralButton("Один том") { _, _ ->
                clear(DIALOG_SEPARATE_VOLUMES)
                showSingleVolumeChooser(project)
            }
            .setPositiveButton("Усі томи") { _, _ ->
                clear(DIALOG_SEPARATE_VOLUMES)
                val plan = runCatching {
                    ProjectVolumeExportPlan.build(project, store.volumes(project.id))
                }.getOrElse {
                    showMessage("Експорт томів", it.message ?: "Помилка перевірки томів.")
                    return@setPositiveButton
                }
                onSeparateVolumeExportRequested(project.id, plan.map { it.volumeId })
            }
            .create()
        track(dialog, DIALOG_SEPARATE_VOLUMES, project.id)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = canProceed
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = canProceed
        DialogUi.apply(dialog, DialogRole.CONFIRM)
    }

    /** Choosing a row opens SAF for exactly one immutable volume ID, not all tomes. */
    private fun showSingleVolumeChooser(project: RenaultProject) {
        val plan = runCatching {
            ProjectVolumeExportPlan.build(project, store.volumes(project.id))
        }.getOrElse {
            clear(DIALOG_SINGLE_VOLUME)
            showMessage("Експорт томів", it.message ?: "Помилка перевірки томів.")
            return
        }
        activeKind = DIALOG_SINGLE_VOLUME
        activeProjectId = project.id
        val labels = plan.map { item ->
            val identity = listOfNotNull(
                item.documentCode?.takeIf { it.isNotBlank() },
                item.date?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { item.fileName }
            "$identity\n${item.fileName}"
        }.toTypedArray()
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Вибрати один том · ${plan.size}")
            .setItems(labels) { _, index ->
                clear(DIALOG_SINGLE_VOLUME)
                onSeparateVolumeExportRequested(project.id, listOf(plan[index].volumeId))
            }
            .setNegativeButton("Назад") { _, _ ->
                clear(DIALOG_SINGLE_VOLUME)
                showSeparateVolumePreview(project)
            }
            .create()
        track(dialog, DIALOG_SINGLE_VOLUME, project.id)
        dialog.show()
        DialogUi.apply(dialog, DialogRole.CHOICE)
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
        private const val DIALOG_SEPARATE_VOLUMES =
            "separateVolumesPreview"
        private const val DIALOG_SINGLE_VOLUME =
            "singleVolumeChooser"
    }
}
