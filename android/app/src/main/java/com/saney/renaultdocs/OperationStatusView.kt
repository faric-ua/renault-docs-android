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
    private val counterView: TextView
    private val detailRow: LinearLayout
    private var fullDetail: String = ""
    private val progressView: ProgressBar
    private val closeView: TextView
    private val cancelView: TextView
    private val detailsToggleView: TextView
    private val copyButtonView: TextView
    private var projectCompact = false
    private var projectDetailsExpanded = false

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

        detailsToggleView = Ui.textView(context, "▾", 18f, Ui.accent).apply {
            gravity = Gravity.CENTER
            minWidth = Ui.dp(context, 44)
            minHeight = Ui.dp(context, 44)
            visibility = View.GONE
            contentDescription = "Розгорнути деталі статусу"
            setOnClickListener {
                projectDetailsExpanded = !projectDetailsExpanded
                updateProjectCompactUi()
            }
        }
        header.addView(detailsToggleView, LayoutParams(Ui.dp(context, 44), Ui.dp(context, 44)))

        copyButtonView = Ui.textView(context, "⧉", 20f, Ui.accent).apply {
            gravity = Gravity.CENTER
            minWidth = Ui.dp(context, 44)
            minHeight = Ui.dp(context, 44)
            visibility = View.GONE
            contentDescription = "Копіювати повний статус"
            setOnClickListener { copyCurrentText() }
        }
        header.addView(copyButtonView, LayoutParams(Ui.dp(context, 44), Ui.dp(context, 44)))

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

        detailRow = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        detailView = Ui.textView(context, "", 13f, Ui.muted).apply {
            contentDescription = "Натисни, щоб скопіювати весь статус"
            setOnClickListener { copyCurrentText() }
        }
        counterView = Ui.textView(context, "", 13f, Ui.muted).apply {
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = "Натисни, щоб скопіювати весь статус"
            setOnClickListener { copyCurrentText() }
        }
        detailRow.addView(detailView)
        detailRow.addView(counterView)
        configureDetailLayout(terminal = false)
        addView(detailRow)

        progressView = SharedOperationProgressBar.create(context)
        addView(progressView, LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(context, SharedOperationProgressBar.HEIGHT_DP)).apply {
            topMargin = Ui.dp(context, 10)
        })
    }

    // Opt-in for ProjectActivity; MainActivity keeps its existing status.
    fun useProjectCompactLayout(initiallyExpanded: Boolean) {
        projectCompact = true
        projectDetailsExpanded = initiallyExpanded
        updateProjectCompactUi()
    }

    fun isProjectDetailsExpanded(): Boolean =
        projectCompact && projectDetailsExpanded

    private fun updateProjectCompactUi() {
        if (!projectCompact) return
        subjectView.visibility = if (projectDetailsExpanded) View.VISIBLE else View.GONE
        detailRow.visibility = if (projectDetailsExpanded) View.VISIBLE else View.GONE
        detailsToggleView.visibility = View.VISIBLE
        detailsToggleView.text = if (projectDetailsExpanded) "▴" else "▾"
        detailsToggleView.contentDescription =
            if (projectDetailsExpanded) "Згорнути деталі статусу" else "Розгорнути деталі статусу"
        copyButtonView.visibility = View.VISIBLE
        titleView.maxLines = 1
        titleView.ellipsize = TextUtils.TruncateAt.END
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
        titleView.setTextColor(Ui.text)
        subjectView.text =
            subject
                .trim()
                .ifBlank {
                    "Renault Docs"
                }
        configureDetailLayout(terminal = false)
        fullDetail = detail
        val parts = OperationStatusDetailFormatter.split(detail)
        detailView.text = parts.stage
        detailView.setTextColor(Ui.muted)
        counterView.setTextColor(Ui.success)
        counterView.text = parts.counter.orEmpty()
        updateProjectCompactUi()
        closeView.visibility = View.GONE
        cancelView.visibility = if (onCancel == null) View.GONE else View.VISIBLE
        cancelView.setOnClickListener(if (onCancel == null) null else View.OnClickListener { onCancel() })
        progressView.visibility = View.VISIBLE
        SharedOperationProgressBar.render(progressView, current, total)
    }

    fun showTerminal(
        title: String,
        detail: String,
        subject: String = "Renault Docs",
        outcome: OperationTerminalOutcome = OperationTerminalOutcome.fromTitle(title),
        onClose: () -> Unit,
    ) {
        visibility = View.VISIBLE
        val statusColor = when (outcome) {
            OperationTerminalOutcome.SUCCESS -> Ui.success
            OperationTerminalOutcome.FAILED -> Ui.danger
            OperationTerminalOutcome.CANCELLED -> Ui.warning
            OperationTerminalOutcome.NEUTRAL -> Ui.muted
        }
        titleView.text = title
        titleView.setTextColor(statusColor)
        subjectView.text =
            subject
                .trim()
                .ifBlank {
                    "Renault Docs"
                }
        configureDetailLayout(terminal = true)
        fullDetail = detail
        detailView.text = OperationStatusDisplayFormat.wrapHashesForDisplay(detail)
        detailView.setTextColor(statusColor)
        counterView.text = ""
        updateProjectCompactUi()
        progressView.visibility = View.VISIBLE
        SharedOperationProgressBar.render(
            progressView, null, null, completed = true, color = statusColor,
        )
        cancelView.visibility = View.GONE
        cancelView.setOnClickListener(null)
        closeView.visibility = View.VISIBLE
        closeView.setOnClickListener {
            visibility = View.GONE
            onClose()
        }
    }

    private fun configureDetailLayout(terminal: Boolean) {
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (terminal) {
            detailRow.orientation = VERTICAL
            counterView.visibility = View.GONE
            detailView.setSingleLine(false)
            detailView.minLines = 1
            detailView.maxLines = if (landscape) 10 else 12
            detailView.ellipsize = null
            detailView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
            return
        }

        // Reserve the count's row in portrait even before the total is known.
        // Landscape keeps the stage and count side by side on one fixed-height row.
        detailRow.orientation = if (landscape) HORIZONTAL else VERTICAL
        counterView.visibility = View.VISIBLE
        detailView.setSingleLine(true)
        detailView.ellipsize = TextUtils.TruncateAt.MIDDLE
        counterView.setSingleLine(true)
        counterView.ellipsize = TextUtils.TruncateAt.END
        counterView.gravity = Gravity.CENTER_VERTICAL
        detailView.gravity = Gravity.CENTER_VERTICAL

        val rowHeight = Ui.dp(context, 20)
        detailView.layoutParams = if (landscape) {
            LayoutParams(0, rowHeight, 1f)
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, rowHeight)
        }
        counterView.layoutParams = if (landscape) {
            LayoutParams(LayoutParams.WRAP_CONTENT, rowHeight).apply {
                marginStart = Ui.dp(context, 8)
            }
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, rowHeight)
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
                fullDetail.trim(),
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

}
