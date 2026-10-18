package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class LastImportLineTest {
    private val kyiv = TimeZone.getTimeZone("Europe/Kiev")

    // 2026-10-18 21:22 у Києві (UTC+3) = 18:22 UTC.
    private val at = 1792347720000L

    @Test
    fun oneVolumeWithTime() {
        assertEquals(
            "Останній імпорт: NT8218A · 18.10, 21:22",
            LastImportLine.text(listOf("NT8218A"), at, kyiv),
        )
    }

    @Test
    fun twoVolumes() {
        assertEquals(
            "Останній імпорт: NT8218A, NT8236A · 18.10, 21:22",
            LastImportLine.text(listOf("NT8218A", "NT8236A"), at, kyiv),
        )
    }

    @Test
    fun manyVolumesShort() {
        assertEquals(
            "Останній імпорт: NT8183A і ще 3 · 18.10, 21:22",
            LastImportLine.text(listOf("NT8183A", "NT8218A", "NT8236A", "NT8240A"), at, kyiv),
        )
    }

    @Test
    fun noTimeNoCodes() {
        assertEquals("Останній імпорт: томи з каталогу", LastImportLine.text(emptyList(), 0L, kyiv))
    }

    @Test
    fun similarCodesStayDifferent() {
        // NT8340A ≠ NT8342A — правило проєкту: не зливати за схожістю.
        assertEquals(
            "Останній імпорт: NT8340A, NT8342A",
            LastImportLine.text(listOf("NT8340A", "NT8342A"), 0L, kyiv),
        )
    }
}
