package com.saney.renaultdocs

/**
 * Where the two stripes are at [phase] 0..1 of one cycle, as fractions of the width
 * (left, right), already clipped to 0..1. Plain Kotlin — tested on the JVM.
 */
object SweepProgressMath {
    /** Long stripe: head runs over 0..0.75 of the cycle, tail over 0.2..1.0. */
    private val longHead = 0f to 0.75f
    private val longTail = 0.2f to 1f

    /** Short stripe: head 0.55..1.0, tail 0.75..1.0 — quicker, right after the long one. */
    private val shortHead = 0.55f to 1f
    private val shortTail = 0.75f to 1f

    fun stripes(phase: Float): List<Pair<Float, Float>> {
        val p = phase.coerceIn(0f, 1f)
        return listOfNotNull(
            stripe(p, longHead, longTail),
            stripe(p, shortHead, shortTail),
        )
    }

    private fun stripe(
        p: Float,
        head: Pair<Float, Float>,
        tail: Pair<Float, Float>,
    ): Pair<Float, Float>? {
        val right = ease(progressIn(p, head))
        val left = ease(progressIn(p, tail))
        if (right <= 0f || left >= 1f || right <= left) return null
        return left.coerceIn(0f, 1f) to right.coerceIn(0f, 1f)
    }

    private fun progressIn(
        p: Float,
        window: Pair<Float, Float>,
    ): Float =
        when {
            p <= window.first -> 0f
            p >= window.second -> 1f
            else -> (p - window.first) / (window.second - window.first)
        }

    /** Speeds up, then slows down («чух — і поїхала»). */
    fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}
