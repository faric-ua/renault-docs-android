package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConversionPlanFactoryTest {
    @Test
    fun outputFolderUsesSourceName() {
        val plan = ConversionPlanFactory.create(
            sourceUri = "content://source",
            destinationUri = "content://destination",
            sourceName = "Laguna 2 2001-2006",
        )

        assertEquals(
            "Laguna 2 2001-2006_android",
            plan.outputFolderName,
        )
    }

    @Test
    fun sameSourceAndDestinationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionPlanFactory.create(
                sourceUri = "content://same",
                destinationUri = "content://same",
                sourceName = "Laguna",
            )
        }
    }

    @Test
    fun unsafeFilenameCharactersAreNormalized() {
        assertEquals(
            "Renault_Test_android",
            ConversionPlanFactory.outputName("Renault/Test"),
        )
    }
}
