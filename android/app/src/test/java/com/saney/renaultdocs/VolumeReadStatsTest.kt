package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Stage 0 of the read-from-.rdpkg plan (TODO №16): counters for volume reads. */
class VolumeReadStatsTest {
    @Before
    fun clean() = VolumeReadStats.reset()

    @Test
    fun listingsAndOpensAreCountedPerVolume() {
        VolumeReadStats.recordListing("nt8494", 6_138, 4_200)
        VolumeReadStats.recordListing("nt8494", 12, 30)
        VolumeReadStats.recordOpen("nt8494", 250, found = true)
        VolumeReadStats.recordOpen("nt8494", 0, found = false)
        VolumeReadStats.recordOpen("nt8299a", 3, found = true)
        val card = VolumeReadStats.totals("nt8494")
        assertEquals(2, card.listings)
        assertEquals(6_150L, card.listedEntries)
        assertEquals(4_230L, card.listingMs)
        assertEquals(1, card.opens)
        assertEquals(250L, card.openMs)
        assertEquals(1, card.missing)
        assertEquals(1, VolumeReadStats.totals("nt8299a").opens)
        assertEquals(0, VolumeReadStats.totals("other").opens)
    }

    @Test
    fun negativeTimesDoNotCount() {
        VolumeReadStats.recordListing("v", -5, -10)
        val totals = VolumeReadStats.totals("v")
        assertEquals(0L, totals.listedEntries)
        assertEquals(0L, totals.listingMs)
    }

    @Test
    fun packageIdComesFromOurDocumentIds() {
        assertEquals("renault-megane-nt8494", VolumeReadStats.packageIdOf("pkg:renault-megane-nt8494"))
        assertEquals("abc", VolumeReadStats.packageIdOf("pkg:abc:payload/x"))
        assertEquals("abc", VolumeReadStats.packageIdOf("pkg:abc/payload"))
        assertNull(VolumeReadStats.packageIdOf("primary:Download"))
        assertNull(VolumeReadStats.packageIdOf("pkg:"))
    }

    @Test
    fun linesNameVolumePlaceAndTime() {
        assertEquals("карта", VolumeReadStats.placeLabel(VolumePlace.CARD))
        assertEquals("телефон", VolumeReadStats.placeLabel(VolumePlace.PHONE))
        assertEquals("інша папка", VolumeReadStats.placeLabel(null))
        val list = VolumeReadStats.listingLine("nt8494", "карта", "", 6_138, 4_200)
        assertTrue(list.startsWith("LIST "))
        assertTrue(list.contains("папка=«/»"))
        assertTrue(list.contains("записів=6138"))
        assertTrue(list.contains("мс=4200"))
        val totals = VolumeReadStats.recordOpen("nt8494", 20, found = true)
        val sum = VolumeReadStats.summaryLine("nt8494", "карта", totals)
        assertTrue(sum.startsWith("SUM "))
        assertTrue(sum.contains("відкрито=1"))
        assertTrue(VolumeReadStats.openLine("v", "телефон", "a/b.pdf", 300).contains("файл=«a/b.pdf»"))
    }

    @Test
    fun summaryEveryFortyOpens() {
        assertFalse(VolumeReadStats.shouldSummarize(VolumeReadStats.Totals(opens = 0)))
        assertFalse(VolumeReadStats.shouldSummarize(VolumeReadStats.Totals(opens = 39)))
        assertTrue(VolumeReadStats.shouldSummarize(VolumeReadStats.Totals(opens = 40)))
    }
}
