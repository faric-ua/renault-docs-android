package com.saney.renaultdocs

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

enum class DialogRole {
    HELP,
    CHOICE,
    CONFIRM,
    DANGER,
    PROGRESS,
}

object DialogUi {
    fun apply(
        dialog: AlertDialog,
        role: DialogRole,
    ) {
        val context =
            dialog.context

        val rounded =
            Ui.roundedBackground(
                context =
                    context,
                fill =
                    Ui.surfaceAlt,
                stroke =
                    Ui.border,
                radiusDp =
                    16,
            )

        val parentPanel =
            dialog.findViewById<View>(
                androidResourceId(
                    context =
                        context,
                    name =
                        "parentPanel",
                ),
            )

        if (
            parentPanel !=
            null
        ) {
            dialog.window
                ?.setBackgroundDrawable(
                    ColorDrawable(
                        Color.TRANSPARENT,
                    ),
                )
            parentPanel.background =
                rounded
        } else {
            dialog.window
                ?.setBackgroundDrawable(
                    rounded,
                )
        }

        listOf(
            "topPanel",
            "contentPanel",
            "buttonPanel",
            "customPanel",
        )
            .forEach {
                name ->
                dialog.findViewById<View>(
                    androidResourceId(
                        context =
                            context,
                        name =
                            name,
                    ),
                )
                    ?.setBackgroundColor(
                        Color.TRANSPARENT,
                    )
            }

        dialog.findViewById<TextView>(
            android.R.id.message,
        )
            ?.apply {
                setTextColor(
                    Ui.text,
                )
                textSize =
                    Ui.secondaryTextSp
            }

        dialog.findViewById<TextView>(
            androidResourceId(
                context =
                    context,
                name =
                    "alertTitle",
            ),
        )
            ?.apply {
                setTextColor(
                    Ui.text,
                )
            }

        dialog.listView
            ?.apply {
                setBackgroundColor(
                    Color.TRANSPARENT,
                )
                divider =
                    ColorDrawable(
                        Ui.border,
                    )
                dividerHeight =
                    Ui.dp(
                        context,
                        1,
                    )

                for (
                    index in
                    0 until childCount
                ) {
                    styleTextTree(
                        getChildAt(
                            index,
                        )
                    )
                }

                setOnHierarchyChangeListener(
                    object :
                        ViewGroup.OnHierarchyChangeListener {
                        override fun onChildViewAdded(
                            parent: View?,
                            child: View?,
                        ) {
                            child
                                ?.let(
                                    ::styleTextTree,
                                )
                        }

                        override fun onChildViewRemoved(
                            parent: View?,
                            child: View?,
                        ) = Unit
                    }
                )
            }

        styleButton(
            dialog =
                dialog,
            which =
                AlertDialog.BUTTON_NEGATIVE,
            color =
                Ui.text,
        )
        styleButton(
            dialog =
                dialog,
            which =
                AlertDialog.BUTTON_NEUTRAL,
            color =
                Ui.text,
        )
        styleButton(
            dialog =
                dialog,
            which =
                AlertDialog.BUTTON_POSITIVE,
            color =
                when (
                    role
                ) {
                    DialogRole.DANGER,
                    DialogRole.PROGRESS ->
                        Ui.danger

                    else ->
                        Ui.accent
                },
        )
    }

    private fun styleButton(
        dialog: AlertDialog,
        which: Int,
        color: Int,
    ) {
        dialog.getButton(
            which,
        )
            ?.apply {
                setTextColor(
                    color,
                )
                isAllCaps =
                    false
                textSize =
                    Ui.secondaryButtonTextSp
            }
    }

    private fun styleTextTree(
        view: View,
    ) {
        if (
            view is
            TextView
        ) {
            view.setTextColor(
                Ui.text,
            )
        }

        if (
            view is
            ViewGroup
        ) {
            for (
                index in
                0 until view.childCount
            ) {
                styleTextTree(
                    view.getChildAt(
                        index,
                    )
                )
            }
        }
    }

    private fun androidResourceId(
        context: android.content.Context,
        name: String,
    ): Int =
        context.resources
            .getIdentifier(
                name,
                "id",
                "android",
            )
}
