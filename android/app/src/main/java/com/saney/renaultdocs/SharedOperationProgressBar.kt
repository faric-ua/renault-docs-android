package com.saney.renaultdocs

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.widget.ProgressBar

/**
 * Shared progress contract. Known totals use measured 0..1000 progress.
 * Unknown totals show real activity instead of a misleading, frozen zero.
 */
object SharedOperationProgressBar {
    const val SCALE = 1_000
    const val HEIGHT_DP = 5

    fun create(context: Context): ProgressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = SCALE
            isIndeterminate = false
            progress = 0
            tint(this, Ui.success)
        }

    fun normalized(current: Int?, total: Int?): Int? {
        if (current == null || total == null || total <= 0) return null
        return (current.coerceIn(0, total).toLong() * SCALE / total)
            .toInt()
            .coerceIn(0, SCALE)
    }

    fun tint(bar: ProgressBar, color: Int) {
        val colors = ColorStateList.valueOf(color)
        bar.progressTintList = colors
        bar.indeterminateTintList = colors
        bar.progressBackgroundTintList = ColorStateList.valueOf(Ui.border)
    }

    fun render(
        bar: ProgressBar,
        current: Int?,
        total: Int?,
        completed: Boolean = false,
        color: Int = Ui.success,
    ) {
        tint(bar, color)
        val measured = normalized(current, total)
        if (!completed && measured == null) {
            // The worker is alive but has not supplied a denominator yet.
            // Do not claim 0% or show a fake file count.
            bar.isIndeterminate = true
            return
        }
        bar.isIndeterminate = false
        bar.max = SCALE
        val value = if (completed) SCALE else measured ?: 0
        if (bar.progress == value) return
        if (!completed && value >= bar.progress &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        ) {
            bar.setProgress(value, true)
        } else {
            bar.progress = value
        }
    }
}
