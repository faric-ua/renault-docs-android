package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle

/**
 * v0.5.106 (owner, phone QA 2026-10-12): every question and message window
 * must survive a screen rotation. Android recreates the Activity on rotation
 * and silently drops any open dialog, so each window gets a stable key:
 * the key is saved in the Activity state, and after rotation the same window
 * is rebuilt from current data (never from a stale snapshot).
 *
 * [build] creates and shows the window (any Dialog) for a key, or returns null when it no
 * longer makes sense (for example, the operation already finished).
 */
class RestorableDialogs(
    private val activity: Activity,
    private val stateKey: String,
    private val build: (String) -> Dialog?,
) {
    private var openKey: String? = null
    private var infoTitle: String? = null
    private var infoMessage: String? = null
    private var infoButton: String? = null
    private var infoRole: String? = null
    private var current: Dialog? = null

    fun isOpen(key: String): Boolean = openKey == key

    fun show(key: String) {
        dismissCurrent()
        val dialog = if (key == KEY_INFO) buildInfo() else build(key)
        if (dialog == null) {
            clear()
            return
        }
        track(key, dialog)
    }

    /** A plain message with one button; its text is saved, so it returns as it was. */
    fun showInfo(
        title: String,
        message: String,
        button: String = "Зрозуміло",
        role: DialogRole = DialogRole.HELP,
    ) {
        infoTitle = title
        infoMessage = message
        infoButton = button
        infoRole = role.name
        show(KEY_INFO)
    }

    fun save(outState: Bundle) {
        outState.putString("$stateKey.key", openKey)
        outState.putString("$stateKey.infoTitle", infoTitle)
        outState.putString("$stateKey.infoMessage", infoMessage)
        outState.putString("$stateKey.infoButton", infoButton)
        outState.putString("$stateKey.infoRole", infoRole)
    }

    fun restore(state: Bundle?) {
        openKey = state?.getString("$stateKey.key")
        infoTitle = state?.getString("$stateKey.infoTitle")
        infoMessage = state?.getString("$stateKey.infoMessage")
        infoButton = state?.getString("$stateKey.infoButton")
        infoRole = state?.getString("$stateKey.infoRole")
    }

    /** Call once the new screen is built (posted), after rotation. */
    fun restoreOpen() {
        val key = openKey ?: return
        openKey = null
        show(key)
    }

    /** Call from onDestroy: closes the window without forgetting it during rotation. */
    fun onDestroy() {
        dismissCurrent()
    }

    private fun track(key: String, dialog: Dialog) {
        openKey = key
        current = dialog
        dialog.setOnDismissListener {
            // Android delivers dismiss later; a window replaced by a newer one
            // (or closed by onDestroy) must not forget the newer key.
            if (current !== dialog) return@setOnDismissListener
            current = null
            // Rotation keeps the key; any real close (button, back, outside) forgets it.
            if (!activity.isChangingConfigurations && openKey == key) {
                clear()
            }
        }
    }

    private fun dismissCurrent() {
        val dialog = current ?: return
        current = null
        runCatching {
            if (dialog.isShowing) dialog.dismiss()
        }
    }

    private fun clear() {
        openKey = null
        infoTitle = null
        infoMessage = null
        infoButton = null
        infoRole = null
    }

    private fun buildInfo(): AlertDialog? {
        val title = infoTitle ?: return null
        val message = infoMessage ?: return null
        if (activity.isFinishing || activity.isDestroyed) return null
        val role = runCatching { DialogRole.valueOf(infoRole.orEmpty()) }.getOrDefault(DialogRole.HELP)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(infoButton ?: "Зрозуміло", null)
            .create()
        dialog.show()
        DialogUi.apply(dialog, role)
        return dialog
    }

    companion object {
        const val KEY_INFO = "info"
    }
}
