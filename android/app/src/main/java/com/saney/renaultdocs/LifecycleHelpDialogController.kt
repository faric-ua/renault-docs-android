package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

data class HelpDialogSpec(
    val title: String,
    val message: String,
)

class LifecycleHelpDialogController(
    private val activity: Activity,
    private val resolve: (String) -> HelpDialogSpec?,
) {
    private var activeHelpId: String? = null
    private var dialog: AlertDialog? = null

    fun restore(
        savedInstanceState: Bundle?,
    ) {
        activeHelpId =
            savedInstanceState
                ?.getString(
                    STATE_ACTIVE_HELP_ID,
                )
                ?.takeIf {
                    it.isNotBlank()
                }
    }

    fun save(
        outState: Bundle,
    ) {
        outState.putString(
            STATE_ACTIVE_HELP_ID,
            activeHelpId,
        )
    }

    fun restoreOpen() {
        val helpId =
            activeHelpId
                ?: return

        activity.window
            .decorView
            .post {
                if (
                    !activity.isFinishing &&
                    !activity.isDestroyed &&
                    activeHelpId ==
                        helpId
                ) {
                    showInternal(
                        helpId,
                    )
                }
            }
    }

    fun show(
        helpId: String,
    ) {
        activeHelpId =
            helpId

        showInternal(
            helpId,
        )
    }

    private fun showInternal(
        helpId: String,
    ) {
        val spec =
            resolve(
                helpId,
            )
                ?: run {
                    activeHelpId =
                        null
                    return
                }

        dialog
            ?.let {
                previous ->
                previous.setOnDismissListener(
                    null,
                )
                previous.dismiss()
            }

        val created =
            AlertDialog.Builder(
                activity,
            )
                .setTitle(
                    spec.title,
                )
                .setMessage(
                    spec.message,
                )
                .setPositiveButton(
                    "Зрозуміло",
                    null,
                )
                .create()

        created.setOnDismissListener {
            if (
                dialog ===
                created
            ) {
                dialog =
                    null

                if (
                    !activity
                        .isChangingConfigurations
                ) {
                    activeHelpId =
                        null
                }
            }
        }

        dialog =
            created
        created.show()

        DialogUi.apply(
            dialog =
                created,
            role =
                DialogRole.HELP,
        )
    }

    companion object {
        private const val STATE_ACTIVE_HELP_ID =
            "lifecycleHelpDialog.activeId"
    }
}
