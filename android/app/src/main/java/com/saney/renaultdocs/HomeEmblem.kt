package com.saney.renaultdocs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The owner's emblem behind the app's own screens (TODO №27, owner 2026-10-18).
 *
 * History:
 *  * 00:56 — idea: one of the owner's four emblems behind the project tiles on
 *    the home screen, a different one on every app launch.
 *  * 0.6.1 (03:20) — home screen only, an ImageView behind the project list, 14 %.
 *  * 0.6.2 (owner 03:33) — on **every** app screen and in the menus (dialogs),
 *    and the tiles themselves are half transparent (Ui.roundedBackground) so the
 *    emblem shows through them; text stays fully opaque. Emblem 25 % — through a
 *    50 % tile it is still visible, text stays easy to read (checked on a mock).
 *    Not behind the document viewer and the guide (WebView pages) — the documents
 *    themselves must stay clean.
 *
 * Picked once per process — so it stays the same while the app is open
 * (rotation, moving between screens) and changes on the next launch.
 * The pictures are the owner's `docs/brand/emblems/emblem-1..4.png`, scaled to
 * 720 px WebP (the originals are 1254 px and 1.5–2 MB each — too big for the APK).
 */
object HomeEmblem {
    val drawables: IntArray = intArrayOf(
        R.drawable.home_emblem_1,
        R.drawable.home_emblem_2,
        R.drawable.home_emblem_3,
        R.drawable.home_emblem_4,
    )

    /** How visible the emblem is on the dark background. */
    const val ALPHA: Float = 0.25f

    /** Part of the shorter side of the screen / dialog the emblem takes. */
    const val SCREEN_FILL: Float = 0.9f
    const val DIALOG_FILL: Float = 0.8f

    val current: Int by lazy { drawables[pick(Random.Default, drawables.size)] }

    fun pick(random: Random, count: Int): Int = random.nextInt(count)

    @Volatile
    private var cached: Bitmap? = null

    /** Decoded once per process (720×720 ≈ 2 MB), shared by all screens. */
    fun bitmap(context: Context): Bitmap? {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                BitmapFactory.decodeResource(context.applicationContext.resources, current)
            }.getOrNull().also { cached = it }
        }
    }
}

/**
 * [base] (screen colour or the dialog panel) with the emblem drawn on top,
 * fit-centred and muted. Behaves like [base] for size and padding.
 */
class EmblemBackgroundDrawable(
    private val base: Drawable,
    private val emblem: Bitmap?,
    private val fill: Float,
    emblemAlpha: Float = HomeEmblem.ALPHA,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = (emblemAlpha * 255).roundToInt().coerceIn(0, 255)
    }
    private val target = Rect()

    override fun onBoundsChange(bounds: Rect) {
        base.bounds = bounds
        val side = (min(bounds.width(), bounds.height()) * fill).roundToInt()
        val left = bounds.left + (bounds.width() - side) / 2
        val top = bounds.top + (bounds.height() - side) / 2
        target.set(left, top, left + side, top + side)
    }

    override fun draw(canvas: Canvas) {
        base.draw(canvas)
        val bitmap = emblem ?: return
        if (target.width() > 0) canvas.drawBitmap(bitmap, null, target, paint)
    }

    override fun setAlpha(alpha: Int) {
        base.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        base.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getPadding(padding: Rect): Boolean = base.getPadding(padding)
}
