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
    private val onShareProgress: (Int, Int, String) -> Unit,
    private val onShareFinished: (String) -> Unit,
) {
    @Volatile
    private var shareInProgress: Boolean = false
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

        addAction("Поділитися проєктом") {
            clear(DIALOG_ACTIONS)
            shareProject(project)
        }
        val prepared =
            PreparedShareStore.projectFile(
                activity,
                project,
            )
        if (prepared.exists()) {
            addAction("Поділитися підготовленим .rdproject") {
                clear(DIALOG_ACTIONS)
                sharePreparedProject(
                    project,
                    prepared,
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

        if (shareInProgress) {
            showMessage(
                title = "Поділитися проєктом",
                message = "Пакування проєкту вже виконується.",
            )
            return
        }
        shareInProgress = true
        activity.runOnUiThread {
            onShareProgress(
                0,
                volumes.size,
                "Готую проєкт 0/" + volumes.size + "…",
            )
        }

        Thread {
            val file =
                PreparedShareStore.projectFile(
                    activity,
                    project,
                )
            val uri =
                FileProvider.getUriForFile(
                    activity,
                    activity.packageName + ".files",
                    file,
                )
            val result =
                RdprojectExporter.export(
                    context = activity,
                    project = project,
                    volumes = volumes,
                    destinationUri = uri,
                    progress = { message ->
                        val current =
                            Regex("""Готую том (\d+)/""")
                                .find(message)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.toIntOrNull()
                                ?: 0
                        activity.runOnUiThread {
                            if (shareInProgress) {
                                onShareProgress(
                                    current,
                                    volumes.size,
                                    message,
                                )
                            }
                        }
                    },
                )

            activity.runOnUiThread {
                shareInProgress = false
                result.onSuccess {
                    onShareFinished(
                        "Проєкт " + project.title +
                            " підготовлено · " + volumeCountLabel(volumes.size) + ".",
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
                            "Поділитися проєктом",
                        ),
                    )
                }.onFailure { error ->
                    onShareFinished("Не вдалося підготувати проєкт.")
                    showMessage(
                        title = "Не вдалося поділитися",
                        message =
                            error.message
                                ?: "Невідома помилка.",
                    )
                }
            }
        }.start()
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
                        )
                    showMessage(
                        title = "Підготовлений файл",
                        message =
                            if (deleted) {
                                "Підготовлений .rdproject видалено. " +
                                    "Сам проєкт і його томи не змінено."
                            } else {
                                "Не вдалося видалити підготовлений .rdproject."
                            },
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
    }
}
