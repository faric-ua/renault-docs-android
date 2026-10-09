package com.saney.renaultdocs

import android.content.Context
import android.os.Build
import android.widget.ProgressBar

/**
 * Single thin, measured-progress contract for operation cards and the converter.
 * Unknown totals never use the distracting bouncing horizontal animation.
 */
object SharedOperationProgressBar {
    const val SCALE = 1_000
    const val HEIGHT_DP = 5

    fun create(context: Context): ProgressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = SCALE
            isIndeterminate = false
            progress = 0
        }

    fun normalized(current: Int?, total: Int?): Int? {
        if (current == null || total == null || total <= 0) return null
        return (current.coerceIn(0, total).toLong() * SCALE / total)
            .toInt()
            .coerceIn(0, SCALE)
    }

    fun render(
        bar: ProgressBar,
        current: Int?,
        total: Int?,
        completed: Boolean = false,
    ) {
        val value = if (completed) SCALE else normalized(current, total) ?: 0
        bar.isIndeterminate = false
        bar.max = SCALE
        if (bar.progress == value) return

        // Animate measured changes only; unknown progress and terminal reset are immediate.
        if (!completed && normalized(current, total) != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        ) {
            bar.setProgress(value, true)
        } else {
            bar.progress = value
        }
    }
}
