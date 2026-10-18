package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    val success: Int = Color.parseColor("#62D296")
    val warning: Int = Color.parseColor("#F0B968")
    val statusWarmFill: Int = Color.parseColor("#2B251F")
    val statusWarmBorder: Int = Color.parseColor("#6E5A42")
    val statusWarmText: Int = Color.parseColor("#D8B58B")

    // v0.5.113 (owner request 2026-10-12): one «selected» look everywhere a
    // list lets you pick something — green frame, dark-green fill (✓ removed in v0.5.117).
    val selectedFill: Int = Color.parseColor("#16271E")
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
        opaque: Boolean = false,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(if (opaque) fill else tileFill(context, fill))
        cornerRadius = dp(context, radiusDp).toFloat()
        setStroke(dp(context, 1), stroke)
    }

    fun selectedBackground(
        context: Context,
        radiusDp: Int = 10,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(tileFill(context, selectedFill))
        cornerRadius = dp(context, radiusDp).toFloat()
        setStroke(dp(context, 2), success)
    }

    // ---- Emblem behind the screens (Renodocs 0.6.2, owner 2026-10-18 03:33) ----
    // «емблема на всіх сторінках… а плитки прозорі на 50 % — не текст, а самі
    // плитки, щоб через них трохи просвічувалась емблема». See HomeEmblem.

    /** Tile colours that become half transparent on emblem screens. */
    private val translucentTileFills: Set<Int> by lazy { setOf(surface, surfaceAlt, selectedFill) }

    /** 50 %: 0x80. Text and frames stay opaque — only the tile fill. */
    const val TILE_ALPHA: Int = 0x80

    fun tileFill(context: Context, fill: Int): Int =
        if (fill in translucentTileFills && isEmblemScreen(context)) {
            (fill and 0x00FFFFFF) or (TILE_ALPHA shl 24)
        } else {
            fill
        }

    /**
     * App's own screens (lists, menus, settings) — yes. The document viewer and
     * the guide (WebView pages) — no: panels over a document stay opaque.
     */
    fun isEmblemScreen(context: Context): Boolean {
        val activity = activityOf(context) ?: return false
        return activity !is ViewerActivity && activity !is GuideActivity
    }

    private fun activityOf(context: Context): Activity? {
        var current: Context? = context
        repeat(8) {
            if (current is Activity) return current as Activity
            current = (current as? android.content.ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    /** Screen background: the dark app colour with the emblem in the middle. */
    fun emblemScreenBackground(context: Context): android.graphics.drawable.Drawable =
        EmblemBackgroundDrawable(
            base = android.graphics.drawable.ColorDrawable(background),
            emblem = HomeEmblem.bitmap(context),
            fill = HomeEmblem.SCREEN_FILL,
        )

    /** Menu / dialog panel: the opaque panel with the emblem inside. */
    fun emblemDialogBackground(context: Context, panel: android.graphics.drawable.Drawable): android.graphics.drawable.Drawable =
        if (isEmblemScreen(context)) {
            EmblemBackgroundDrawable(
                base = panel,
                emblem = HomeEmblem.bitmap(context),
                fill = HomeEmblem.DIALOG_FILL,
            )
        } else {
            panel
        }

    /** Marks a list button as one choice of [scope]; see [refreshChoices]. */
    data class ChoiceTag(
        val scope: String,
        val key: String,
        val radiusDp: Int = 10,
    )

    /**
     * Normal list button (dark fill, blue frame) or the chosen one (green
     * frame). Text stays the same, so the layout does not jump.
     */
    fun styleChoice(
        button: Button,
        selected: Boolean,
        radiusDp: Int = 10,
    ) {
        val context = button.context
        button.background =
            if (selected) {
                selectedBackground(context, radiusDp)
            } else {
                roundedBackground(
                    context = context,
                    fill = surfaceAlt,
                    stroke = accent,
                    radiusDp = radiusDp,
                )
            }
        button.setTextColor(
            if (selected) Color.WHITE else text,
        )
        // v0.5.117 (owner 2026-10-13): no ✓ — it took width from the text and
        // the text jumped. The green frame alone marks the pick.
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            0,
            0,
        )
        // v0.5.133: the «›» icon of an openable tile is not a ✓ — put it back.
        (button.getTag(R.id.tag_open_chevron) as? Int)?.let {
            applyOpenChevron(button, it)
        }
        button.setTypeface(
            null,
            if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
        )
        val description =
            button.contentDescription?.toString().orEmpty()
                .removeSuffix(" · вибрано")
        button.contentDescription =
            if (selected) "$description · вибрано" else description
    }

    /**
     * v0.5.133 (owner, phone QA 2026-10-14 20:00, M30s): «›» used to be the first
     * character of the text. When a long label did not fit, «›» stayed alone on the
     * first line and the codes wrapped mid-word. Now «›» is its own icon at the start
     * of the tile, centred by the tile's height; the text wraps beside it.
     */
    fun applyOpenChevron(
        view: TextView,
        color: Int = accent,
    ) {
        val icon =
            ContextCompat.getDrawable(view.context, R.drawable.ic_chevron_open)
                ?.mutate()
                ?: return
        val size = dp(view.context, 18)
        icon.setBounds(0, 0, size, size)
        icon.setTint(color)
        view.setTag(R.id.tag_open_chevron, color)
        view.setCompoundDrawablesRelative(icon, null, null, null)
        view.compoundDrawablePadding = dp(view.context, 6)
    }

    /**
     * v0.5.133: Renault labels list codes without spaces («F9Q650,F9Q750,…»),
     * so Android broke them mid-code («G9 / T600»). An invisible break chance
     * after «,» and «/» lets a line end between codes instead.
     */
    fun breakableLabel(text: String): String =
        text.replace(",", ",\u200B").replace("/", "/\u200B")

    /** Re-styles every choice button of [scope] under [root]: only [selectedKey] is green. */
    fun refreshChoices(
        root: View,
        scope: String,
        selectedKey: String,
    ) {
        if (root is Button) {
            val tag = root.tag as? ChoiceTag
            if (tag != null && tag.scope == scope) {
                styleChoice(root, tag.key == selectedKey, tag.radiusDp)
            }
            return
        }
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                refreshChoices(root.getChildAt(index), scope, selectedKey)
            }
        }
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

    /**
     * App-owned Activity and dialog windows follow the same orientation rule.
     * Hide system status/navigation only: do not touch the IME or Android SAF.
     */
    fun applyOrientationSystemBars(activity: Activity) {
        applyOrientationSystemBars(
            activity.window,
            activity.resources.configuration.orientation,
        )
    }

    fun applyOrientationSystemBars(window: Window?, orientation: Int) {
        if (window == null) return
        val controller = WindowCompat.getInsetsController(
            window, window.decorView,
        )
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
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
