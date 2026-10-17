package com.saney.renaultdocs

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

/**
 * TODO №21 (owner 2026-10-17 04:19, 04:42): the bar for «unknown how long» must not just
 * flicker at the left edge «like a flag in the wind». Like in YTM: a stripe «чух — і поїхала»
 * from the start to the end, at changing speed, then again.
 *
 * Two stripes run across the whole width every [CYCLE_MS]: a long one that speeds up and
 * slows down, and a shorter, quicker one after it (the usual Material «indeterminate» rhythm).
 * The phases are plain math ([SweepProgressMath]) — tested on the JVM.
 *
 * [android.widget.ProgressBar] starts and stops this drawable itself (it is [Animatable]).
 */
class SweepProgressDrawable(
    private var stripeColor: Int,
    private var trackColor: Int,
) : Drawable(), Animatable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var phase = 0f

    private val animator =
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CYCLE_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidateSelf()
            }
        }

    fun setColors(
        stripe: Int,
        track: Int,
    ) {
        stripeColor = stripe
        trackColor = track
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val w = b.width().toFloat()
        val radius = b.height() / 2f
        paint.color = trackColor
        rect.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.color = stripeColor
        for (stripe in SweepProgressMath.stripes(phase)) {
            val left = b.left + stripe.first * w
            val right = b.left + stripe.second * w
            if (right - left < 0.5f) continue
            rect.set(left, b.top.toFloat(), right, b.bottom.toFloat())
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    override fun start() {
        if (!animator.isStarted) animator.start()
    }

    override fun stop() {
        animator.cancel()
    }

    override fun isRunning(): Boolean = animator.isRunning

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        const val CYCLE_MS = 1_800L
    }
}
