package com.saney.renaultdocs

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class OperationStatusView(context: Context) : LinearLayout(context) {
    private val titleView: TextView
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

        addView(Ui.textView(context, "Renault Docs", 12f, Ui.muted).apply {
            setPadding(0, 0, 0, Ui.dp(context, 6))
        })

        detailView = Ui.textView(context, "", 13f, Ui.muted)
        addView(detailView)

        progressView = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
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
    ) {
        visibility = View.VISIBLE
        titleView.text = title
        detailView.text = detail
        closeView.visibility = View.GONE
        cancelView.visibility = if (onCancel == null) View.GONE else View.VISIBLE
        cancelView.setOnClickListener(if (onCancel == null) null else View.OnClickListener { onCancel() })
        progressView.visibility = View.VISIBLE
        val determinate = current != null && total != null && total > 0
        progressView.isIndeterminate = !determinate
        if (determinate) {
            progressView.max = total!!
            progressView.progress = current!!.coerceIn(0, total)
        }
    }

    fun showTerminal(title: String, detail: String, onClose: () -> Unit) {
        visibility = View.VISIBLE
        titleView.text = title
        detailView.text = detail
        progressView.isIndeterminate = false
        progressView.max = 1
        progressView.progress = 1
        cancelView.visibility = View.GONE
        cancelView.setOnClickListener(null)
        closeView.visibility = View.VISIBLE
        closeView.setOnClickListener {
            visibility = View.GONE
            onClose()
        }
    }

    fun hide() {
        visibility = View.GONE
        closeView.setOnClickListener(null)
        cancelView.setOnClickListener(null)
    }
}
