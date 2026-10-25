package com.saney.renaultdocs

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableColumnPlanTest {
    @Test
    fun fourColumnPinTableFitsScreenAsBefore() {
        // № | мм² | Код | Опис — перші три вузькі, опис забирає решту, прокрутки нема.
        val plan = TableColumnPlan.plan(intArrayOf(40, 50, 60, 900), available = 1000, minLast = 140, maxMiddle = 600)
        assertArrayEquals(intArrayOf(40, 50, 60, 850), plan.widths)
        assertEquals(1000, plan.total)
        assertTrue(plan.wrap[3])
    }

    @Test
    fun fiveColumnsDescriptionNoLongerOverflowsAndLastColumnIsNotZero() {
        // Скрін 25.10: опис — 4-та колонка (2000 px тексту), 5-та — майже без місця.
        val plan = TableColumnPlan.plan(intArrayOf(40, 50, 60, 2000, 80), available = 1000, minLast = 140, maxMiddle = 600)
        assertEquals(600, plan.widths[3])
        assertTrue(plan.wrap[3])
        assertEquals(250, plan.widths[4])
        assertEquals(1000, plan.total)
    }

    @Test
    fun tooManyWideColumnsScrollSideways() {
        val plan = TableColumnPlan.plan(intArrayOf(700, 700, 700), available = 1000, minLast = 140, maxMiddle = 600)
        assertArrayEquals(intArrayOf(600, 600, 140), plan.widths)
        assertTrue(plan.total > 1000) // ширше за екран → прокрутка вбік
    }

    @Test
    fun singleColumnFillsScreen() {
        val plan = TableColumnPlan.plan(intArrayOf(300), available = 1000, minLast = 140, maxMiddle = 600)
        assertArrayEquals(intArrayOf(1000), plan.widths)
    }
}
