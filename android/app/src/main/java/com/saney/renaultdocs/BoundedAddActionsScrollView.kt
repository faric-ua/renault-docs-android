package com.saney.renaultdocs

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.ScrollView

/**
 * Keeps the Home Add actions scrollable without hiding the My Renault list.
 * Unlike setting a fixed height, short content never leaves an empty panel.
 */
class BoundedAddActionsScrollView(context: Context) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val landscape = resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        val fraction = if (landscape) 0.44f else 0.46f
        val viewportLimit = (resources.displayMetrics.heightPixels * fraction)
            .toInt().coerceAtLeast(Ui.dp(context, 120))
        val parentLimit = if (View.MeasureSpec.getMode(heightMeasureSpec) ==
            View.MeasureSpec.UNSPECIFIED) {
            viewportLimit
        } else {
            minOf(viewportLimit, View.MeasureSpec.getSize(heightMeasureSpec))
        }
        super.onMeasure(
            widthMeasureSpec,
            View.MeasureSpec.makeMeasureSpec(parentLimit, View.MeasureSpec.AT_MOST),
        )
    }
}
