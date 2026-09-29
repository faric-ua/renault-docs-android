package com.saney.renaultdocs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeFastPackWriterTest {
    @Test
    fun mirrorsPythonFastPackSelectionRules() {
        assertTrue(
            NativeFastPackWriter.shouldPack(
                "RUS/HTM/MENU/101.HTM",
            )
        )
        assertTrue(
            NativeFastPackWriter.shouldPack(
                "COMMUN/GIF/icon.GIF",
            )
        )
        assertTrue(
            NativeFastPackWriter.shouldPack(
                "runtime/config.json",
            )
        )

        assertFalse(
            NativeFastPackWriter.shouldPack(
                "COMMUN/PDF/PC/S7.PDF",
            )
        )
        assertFalse(
            NativeFastPackWriter.shouldPack(
                "renault-dataset.json",
            )
        )
        assertFalse(
            NativeFastPackWriter.shouldPack(
                "_renault/runtime-ir-index.json",
            )
        )
        assertFalse(
            NativeFastPackWriter.shouldPack(
                "_renault/fast-content-deadbeef.zip",
            )
        )
    }
}
