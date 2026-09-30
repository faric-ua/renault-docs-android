package com.saney.renaultdocs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import kotlin.math.roundToInt

object Ui {
    val background: Int = Color.parseColor("#101318")
    val surface: Int = Color.parseColor("#181D25")
    val surfaceAlt: Int = Color.parseColor("#222936")
    val border: Int = Color.parseColor("#384352")
    val text: Int = Color.parseColor("#F3F6F8")
    val muted: Int = Color.parseColor("#AAB5C2")
    val accent: Int = Color.parseColor("#76BDFF")
    val danger: Int = Color.parseColor("#FF7A88")
    const val actionSubtitleSp: Float = 11f
    const val secondaryTextSp: Float = 12f
    const val valueTextSp: Float = 13f
    const val compactButtonSp: Float = 12f

    fun dp(
        context: Context,
        value: Int,
    ): Int = (
        value * context.resources.displayMetrics.density
    ).roundToInt()

    fun roundedBackground(
        context: Context,
        fill: Int,
        stroke: Int = border,
        radiusDp: Int = 14,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(context, radiusDp).toFloat()
        setStroke(dp(context, 1), stroke)
    }

    fun textView(
        context: Context,
        value: String,
        sizeSp: Float = 16f,
        color: Int = text,
    ): TextView = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
    }

    fun helpButton(
        context: Context,
        onClick: () -> Unit,
    ): TextView =
        textView(
            context = context,
            value = "?",
            sizeSp = 18f,
            color = accent,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            gravity =
                Gravity.CENTER
            contentDescription =
                "Довідка"
            isClickable =
                true
            isFocusable =
                true
            minWidth =
                dp(
                    context,
                    40,
                )
            minimumWidth =
                dp(
                    context,
                    40,
                )
            minHeight =
                dp(
                    context,
                    40,
                )
            minimumHeight =
                dp(
                    context,
                    40,
                )
            background =
                roundedBackground(
                    context =
                        context,
                    fill =
                        surface,
                    stroke =
                        border,
                    radiusDp =
                        12,
                )
            setOnClickListener {
                onClick()
            }
        }

    fun applySystemInsets(
        view: View,
        horizontalDp: Int = 16,
        topDp: Int = 16,
        bottomDp: Int = 16,
    ) {
        @Suppress("DEPRECATION")
        view.setOnApplyWindowInsetsListener { target, insets ->
            target.setPadding(
                dp(target.context, horizontalDp),
                insets.systemWindowInsetTop + dp(target.context, topDp),
                dp(target.context, horizontalDp),
                insets.systemWindowInsetBottom + dp(target.context, bottomDp),
            )
            insets
        }
        view.requestApplyInsets()
    }
}
