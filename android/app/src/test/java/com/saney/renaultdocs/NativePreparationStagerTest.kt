package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativePreparationStagerTest {
    @Test
    fun patchableTextMatchesConverterContract() {
        assertTrue(
            NativePreparationStager.isPatchableText(
                "RUS/HTM/MENU/101.HTM",
            )
        )
        assertTrue(
            NativePreparationStager.isPatchableText(
                "COMMUN/JS/VISU.JS",
            )
        )
        assertFalse(
            NativePreparationStager.isPatchableText(
                "COMMUN/PDF/PC/S7.PDF",
            )
        )
    }

    @Test
    fun rootEntrypointDistinguishesExactVolumeFromParentFolder() {
        assertTrue(
            NativePreparationStager.hasRootEntrypoint(
                setOf(
                    "INDEX.HTM",
                    "RUS/HTM/MENU/101.HTM",
                ),
            )
        )

        assertFalse(
            NativePreparationStager.hasRootEntrypoint(
                setOf(
                    "NT8340A/INDEX.HTM",
                    "NT8393/INDEX.HTM",
                ),
            )
        )
    }


    @Test
    fun stagingTokenIsFilesystemSafeAndStable() {
        assertEquals(
            "Megane-II-NT8340A-2006-04-18",
            NativePreparationStager.safeToken(
                "Megane II / NT8340A / 2006-04-18",
            ),
        )

        assertEquals(
            "renault",
            NativePreparationStager.safeToken(
                "   ",
            ),
        )
    }
}
