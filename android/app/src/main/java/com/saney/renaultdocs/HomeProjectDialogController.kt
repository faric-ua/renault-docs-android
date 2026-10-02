package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

class HomeProjectDialogController(
    private val activity: Activity,
    private val store: ProjectStore,
    private val onProjectRemoved: (RenaultProject) -> Unit,
) {
    private var activeKind: String = ""
    private var activeProjectId: String? = null

    fun restore(state: Bundle?) {
        activeKind =
            state
                ?.getString(STATE_KIND)
                .orEmpty()
        activeProjectId =
            state
                ?.getString(STATE_PROJECT_ID)
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

        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(project.title)
                .setItems(
                    arrayOf(
                        "Видалити проєкт",
                    ),
                ) {
                    _,
                    which ->
                    if (which == 0) {
                        showRemoveConfirmation(
                            project,
                        )
                    }
                }
                .setNegativeButton(
                    "Скасувати",
                    null,
                )
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
        (dialog.listView?.getChildAt(0) as? android.widget.TextView)
            ?.let {
                Ui.applyActionStyle(
                    view = it,
                    dangerAction = true,
                )
            }
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
                        volumeCount +
                        " том(ів) буде прибрано з бібліотеки Renault Docs. " +
                        "Вихідні .rdpkg, SAF-папки та оригінальні Renault-файли на телефоні не видаляються.",
                )
                .setNegativeButton(
                    "Скасувати",
                    null,
                )
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
    }

    companion object {
        private const val STATE_KIND =
            "homeProjectDialogKind"
        private const val STATE_PROJECT_ID =
            "homeProjectDialogProjectId"
        private const val DIALOG_ACTIONS =
            "projectActions"
        private const val DIALOG_REMOVE =
            "removeProject"
    }
}
