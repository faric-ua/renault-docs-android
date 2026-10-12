package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog

/**
 * Owner rule (2026-10-12, v0.5.104): every action that stops a running
 * operation or throws away the owner's choice asks once before it runs.
 * "Keep" is always the safe default and does nothing.
 */
object ConfirmActions {
    const val STOP_LABEL = "Зупинити"
    const val KEEP_RUNNING_LABEL = "Продовжити"

    fun confirm(
        activity: Activity,
        title: String,
        message: String,
        confirmLabel: String,
        keepLabel: String,
        onConfirm: () -> Unit,
    ): AlertDialog? {
        if (activity.isFinishing || activity.isDestroyed) {
            return null
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(keepLabel, null)
            .setPositiveButton(confirmLabel) { _, _ -> onConfirm() }
            .create()
        dialog.show()
        DialogUi.apply(dialog, DialogRole.DANGER)
        return dialog
    }

    /** Stopping a long operation: «Продовжити» keeps it, «Зупинити» stops it. */
    fun confirmStop(
        activity: Activity,
        title: String,
        message: String,
        onConfirm: () -> Unit,
    ): AlertDialog? =
        confirm(
            activity = activity,
            title = title,
            message = message,
            confirmLabel = STOP_LABEL,
            keepLabel = KEEP_RUNNING_LABEL,
            onConfirm = onConfirm,
        )
}
