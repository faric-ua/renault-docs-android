package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

/** TODO №1 (2026-10-15): volumes in the phone or on the memory card. */
class VolumeStoragePolicyTest {
    @Test
    fun phoneWinsThenCard() {
        assertEquals(VolumeState.PHONE, VolumeStoragePolicy.state(true, true, true, true))
        assertEquals(VolumeState.CARD, VolumeStoragePolicy.state(false, true, false, true))
    }

    @Test
    fun cardOutIsNotBroken() {
        assertEquals(VolumeState.CARD_MISSING, VolumeStoragePolicy.state(false, false, true, false))
        // Remembered on the card, card is in, but the folder is gone — really not installed.
        assertEquals(VolumeState.NOT_INSTALLED, VolumeStoragePolicy.state(false, false, true, true))
        assertEquals(VolumeState.NOT_INSTALLED, VolumeStoragePolicy.state(false, false, false, false))
    }

    @Test
    fun unknownSettingMeansPhone() {
        assertEquals(VolumePlace.PHONE, VolumeStoragePolicy.parsePlace(null))
        assertEquals(VolumePlace.PHONE, VolumeStoragePolicy.parsePlace("SD"))
        assertEquals(VolumePlace.CARD, VolumeStoragePolicy.parsePlace("CARD"))
    }

    @Test
    fun linesAreShortAndHonest() {
        assertEquals("9,0 ГБ", VolumeStoragePolicy.sizeLabel(9L * 1024 * 1024 * 1024))
        assertEquals("850 МБ", VolumeStoragePolicy.sizeLabel(850L * 1024 * 1024))
        assertEquals(
            "Карта пам'яті · вільно 1,5 ГБ",
            VolumeStoragePolicy.choiceLine(VolumePlace.CARD, true, 1536L * 1024 * 1024),
        )
        assertEquals(
            "Карта пам'яті — зараз не вставлена",
            VolumeStoragePolicy.choiceLine(VolumePlace.CARD, false, null),
        )
        assertEquals("Телефон", VolumeStoragePolicy.choiceLine(VolumePlace.PHONE, true, null))
    }

    @Test
    fun choiceItemsStayShort() {
        assertEquals("Карта пам'яті · 333,8 ГБ", VolumeStoragePolicy.choiceItem(VolumePlace.CARD, true, 3338L * 1024 * 1024 * 1024 / 10))
        assertEquals("Телефон · 8,2 ГБ", VolumeStoragePolicy.choiceItem(VolumePlace.PHONE, true, 82L * 1024 * 1024 * 1024 / 10))
        assertEquals("Карта пам'яті — не вставлена", VolumeStoragePolicy.choiceItem(VolumePlace.CARD, false, null))
    }
}
