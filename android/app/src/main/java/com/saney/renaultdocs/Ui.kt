package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

object Ui {
    val background: Int = Color.parseColor("#101318")
    val surface: Int = Color.parseColor("#181D25")
    val surfaceAlt: Int = Color.parseColor("#222936")
    val border: Int = Color.parseColor("#384352")
    val text: Int = Color.parseColor("#F3F6F8")
    val muted: Int = Color.parseColor("#AAB5C2")
    val accent: Int = Color.parseColor("#76BDFF")
    val entityTitle: Int = Color.parseColor("#C8E5FF")
    val danger: Int = Color.parseColor("#FF7A88")
    const val actionSubtitleSp: Float = 11f
    const val secondaryTextSp: Float = 12f
    const val valueTextSp: Float = 13f
    const val compactButtonSp: Float = 12f
    const val secondaryButtonTextSp: Float = 14f

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

    fun applyActionStyle(
        view: TextView,
        primary: Boolean = false,
        dangerAction: Boolean = false,
    ) {
        view.isAllCaps =
            false
        view.textSize =
            14f
        view.gravity =
            Gravity.CENTER
        view.minHeight =
            dp(
                view.context,
                44,
            )
        view.minimumHeight =
            dp(
                view.context,
                44,
            )
        view.setTextColor(
            when {
                dangerAction ->
                    danger

                primary ->
                    accent

                else ->
                    text
            }
        )
        view.background =
            roundedBackground(
                context =
                    view.context,
                fill =
                    if (
                        primary
                    ) {
                        surfaceAlt
                    } else {
                        surface
                    },
                stroke =
                    when {
                        dangerAction ->
                            danger

                        primary ->
                            accent

                        else ->
                            border
                    },
                radiusDp =
                    11,
            )
        view.setPadding(
            dp(
                view.context,
                12,
            ),
            dp(
                view.context,
                8,
            ),
            dp(
                view.context,
                12,
            ),
            dp(
                view.context,
                8,
            ),
        )
    }

    fun modeButton(
        context: Context,
        label: String,
        active: Boolean,
        onClick: () -> Unit,
    ): TextView =
        textView(
            context =
                context,
            value =
                label,
            sizeSp =
                13f,
            color =
                if (
                    active
                ) {
                    background
                } else {
                    text
                },
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            gravity =
                android.view.Gravity.CENTER
            isClickable =
                !active
            isFocusable =
                !active
            minHeight =
                dp(
                    context,
                    36,
                )
            minimumHeight =
                dp(
                    context,
                    36,
                )
            background =
                roundedBackground(
                    context =
                        context,
                    fill =
                        if (
                            active
                        ) {
                            accent
                        } else {
                            surfaceAlt
                        },
                    stroke =
                        if (
                            active
                        ) {
                            accent
                        } else {
                            border
                        },
                    radiusDp =
                        10,
                )
            setPadding(
                dp(
                    context,
                    10,
                ),
                dp(
                    context,
                    6,
                ),
                dp(
                    context,
                    10,
                ),
                dp(
                    context,
                    6,
                ),
            )
            alpha =
                if (
                    active
                ) {
                    1f
                } else {
                    0.96f
                }

            if (
                !active
            ) {
                setOnClickListener {
                    onClick()
                }
            }
        }

    fun actionButton(
        context: Context,
        label: String,
        primary: Boolean = false,
        onClick: () -> Unit,
    ): TextView =
        textView(
            context =
                context,
            value =
                label,
            sizeSp =
                14f,
            color =
                if (
                    primary
                ) {
                    accent
                } else {
                    text
                },
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            gravity =
                android.view.Gravity.CENTER
            isClickable =
                true
            isFocusable =
                true
            minHeight =
                dp(
                    context,
                    44,
                )
            minimumHeight =
                dp(
                    context,
                    44,
                )
            background =
                roundedBackground(
                    context =
                        context,
                    fill =
                        if (
                            primary
                        ) {
                            surfaceAlt
                        } else {
                            surface
                        },
                    stroke =
                        if (
                            primary
                        ) {
                            accent
                        } else {
                            border
                        },
                    radiusDp =
                        11,
                )
            setPadding(
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    8,
                ),
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    8,
                ),
            )
            setOnClickListener {
                onClick()
            }
        }

    fun applyOrientationSystemBars(
        activity: Activity,
    ) {
        val landscape =
            activity.resources
                .configuration
                .orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        val window =
            activity.window
        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView,
            )

        if (
            landscape
        ) {
            WindowCompat.setDecorFitsSystemWindows(
                window,
                false,
            )
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(
                WindowInsetsCompat.Type.systemBars(),
            )
        } else {
            controller.show(
                WindowInsetsCompat.Type.systemBars(),
            )
            WindowCompat.setDecorFitsSystemWindows(
                window,
                true,
            )
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
