package com.saney.renaultdocs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class OperationStatusView(context: Context) : LinearLayout(context) {
    private val titleView: TextView
    private val subjectView: TextView
    private val detailView: TextView
    private val progressView: ProgressBar
    private val closeView: TextView
    private val cancelView: TextView

    init {
        orientation = VERTICAL
        visibility = View.GONE
        background = Ui.roundedBackground(context, Ui.surfaceAlt, Ui.border, 16)
        setPadding(Ui.dp(context, 16), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12))

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = Ui.textView(context, "", 16f, Ui.text).apply {
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = "Натисни, щоб скопіювати весь статус"
            setOnClickListener {
                copyCurrentText()
            }
        }
        header.addView(titleView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        cancelView = Ui.textView(context, "Скасувати", 13f, Ui.muted).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
            minHeight = Ui.dp(context, 44)
            setPadding(Ui.dp(context, 10), 0, Ui.dp(context, 10), 0)
        }
        header.addView(cancelView, LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(context, 44)))
        closeView = Ui.textView(context, "×", 24f, Ui.muted).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
            contentDescription = "Закрити статус"
            minWidth = Ui.dp(context, 44)
            minHeight = Ui.dp(context, 44)
        }
        header.addView(closeView, LayoutParams(Ui.dp(context, 44), Ui.dp(context, 44)))
        addView(header)

        subjectView =
            Ui.textView(
                context,
                "Renault Docs",
                12f,
                Ui.muted,
            ).apply {
                maxLines =
                    1
                ellipsize =
                    TextUtils.TruncateAt.MIDDLE
                contentDescription =
                    "Натисни, щоб скопіювати весь статус"
                setOnClickListener {
                    copyCurrentText()
                }
                setPadding(
                    0,
                    0,
                    0,
                    Ui.dp(
                        context,
                        6,
                    ),
                )
            }
        addView(
            subjectView,
        )

        detailView = Ui.textView(context, "", 13f, Ui.muted).apply {
            contentDescription = "Натисни, щоб скопіювати весь статус"
            setOnClickListener {
                copyCurrentText()
            }
        }
        configureDetailLayout(
            terminal = false,
        )
        addView(detailView)

        progressView = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = PROGRESS_SCALE
            progress = 0
            isIndeterminate = true
        }
        addView(progressView, LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(context, 5)).apply {
            topMargin = Ui.dp(context, 10)
        })
    }

    fun showRunning(
        title: String,
        detail: String,
        current: Int? = null,
        total: Int? = null,
        onCancel: (() -> Unit)? = null,
        subject: String = "Renault Docs",
    ) {
        visibility = View.VISIBLE
        titleView.text = title
        subjectView.text =
            subject
                .trim()
                .ifBlank {
                    "Renault Docs"
                }
        configureDetailLayout(
            terminal = false,
        )
        detailView.text = detail
        closeView.visibility = View.GONE
        cancelView.visibility = if (onCancel == null) View.GONE else View.VISIBLE
        cancelView.setOnClickListener(if (onCancel == null) null else View.OnClickListener { onCancel() })
        progressView.visibility = View.VISIBLE
        val determinate =
            current !=
                null &&
                total !=
                    null &&
                total >
                    0

        if (
            determinate
        ) {
            progressView.isIndeterminate =
                false

            val normalized =
                (
                    current!!
                        .coerceIn(
                            0,
                            total!!,
                        )
                        .toLong() *
                        PROGRESS_SCALE /
                        total
                )
                    .toInt()
                    .coerceIn(
                        0,
                        PROGRESS_SCALE,
                    )

            progressView.max =
                PROGRESS_SCALE

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.N
            ) {
                progressView.setProgress(
                    normalized,
                    true,
                )
            } else {
                progressView.progress =
                    normalized
            }
        } else {
            val isNewRun =
                titleView.text
                    .toString() !=
                    title ||
                    closeView.visibility ==
                        View.VISIBLE ||
                    visibility !=
                        View.VISIBLE

            progressView.isIndeterminate =
                false
            progressView.max =
                PROGRESS_SCALE

            if (
                isNewRun
            ) {
                progressView.progress =
                    0
            }
        }
    }

    fun showTerminal(
        title: String,
        detail: String,
        subject: String = "Renault Docs",
        onClose: () -> Unit,
    ) {
        visibility = View.VISIBLE
        titleView.text = title
        subjectView.text =
            subject
                .trim()
                .ifBlank {
                    "Renault Docs"
                }
        configureDetailLayout(
            terminal = true,
        )
        detailView.text = detail
        progressView.isIndeterminate = false
        progressView.max = PROGRESS_SCALE
        progressView.progress = PROGRESS_SCALE
        cancelView.visibility = View.GONE
        cancelView.setOnClickListener(null)
        closeView.visibility = View.VISIBLE
        closeView.setOnClickListener {
            visibility = View.GONE
            onClose()
        }
    }

    private fun configureDetailLayout(
        terminal: Boolean,
    ) {
        if (
            terminal
        ) {
            detailView.maxLines =
                if (
                    resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
                ) {
                    4
                } else {
                    6
                }
            detailView.ellipsize =
                null
            return
        }

        val landscape =
            resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        if (
            landscape
        ) {
            detailView.maxLines =
                1
            detailView.ellipsize =
                TextUtils.TruncateAt.MIDDLE
        } else {
            detailView.maxLines =
                2
            detailView.ellipsize =
                TextUtils.TruncateAt.END
        }
    }

    private fun copyCurrentText() {
        val fullText =
            listOf(
                titleView.text
                    .toString()
                    .trim(),
                subjectView.text
                    .toString()
                    .trim(),
                detailView.text
                    .toString()
                    .trim(),
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    "\n",
                )

        if (
            fullText.isBlank()
        ) {
            return
        }

        context.getSystemService(
            ClipboardManager::class.java,
        )
            .setPrimaryClip(
                ClipData.newPlainText(
                    "Renault Docs status",
                    fullText,
                ),
            )

        Toast.makeText(
            context,
            "Статус скопійовано",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun hide() {
        visibility = View.GONE
        closeView.setOnClickListener(null)
        cancelView.setOnClickListener(null)
    }

    companion object {
        private const val PROGRESS_SCALE =
            1_000
    }
}
