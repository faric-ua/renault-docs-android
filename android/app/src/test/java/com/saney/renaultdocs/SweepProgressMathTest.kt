package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TODO №21 (v0.5.146): the «unknown how long» stripe runs across the whole bar. */
class SweepProgressMathTest {
    @Test
    fun stripesStayInsideTheBar() {
        for (i in 0..1000) {
            SweepProgressMath.stripes(i / 1000f).forEach { (left, right) ->
                assertTrue(left in 0f..1f && right in 0f..1f && left < right)
            }
        }
    }

    @Test
    fun longStripeTravelsFromStartToEnd() {
        var lastRight = -1f
        var reachedEnd = false
        for (i in 1..80) {
            val first = SweepProgressMath.stripes(i / 100f).first()
            assertTrue("head never goes back", first.second >= lastRight)
            lastRight = first.second
            if (first.second > 0.99f) reachedEnd = true
        }
        assertTrue(reachedEnd)
        assertTrue("starts near the left edge", SweepProgressMath.stripes(0.05f).first().first < 0.05f)
    }

    @Test
    fun secondQuickerStripeFollows() {
        assertEquals(2, SweepProgressMath.stripes(0.7f).size)
    }

    @Test
    fun speedChanges() {
        // «чух — і поїхала»: slow at the start, fast in the middle, slow at the end.
        val start = SweepProgressMath.ease(0.1f) - SweepProgressMath.ease(0f)
        val middle = SweepProgressMath.ease(0.55f) - SweepProgressMath.ease(0.45f)
        assertTrue(middle > start * 2)
    }
}
