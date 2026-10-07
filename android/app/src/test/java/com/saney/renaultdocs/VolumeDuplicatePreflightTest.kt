package com.saney.renaultdocs

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeDuplicatePreflightTest {
    @Test
    fun exactDocumentCodeAndDateSkipsDuplicate() {
        val existing =
            volume(
                id = "old-folder-name",
                documentCode = "NT8344",
                date = "2007-05-02",
            )

        val result =
            VolumeDuplicatePreflight.check(
                existing = listOf(existing),
                rawRoot =
                    File(
                        "Megane-II_E84-L84-K84_Europe_NT8344_Visu-v4.0_2007-05-02",
                    ),
            )

        assertEquals(existing, result.exact)
        assertTrue(result.possible.isEmpty())
    }

    @Test
    fun sameDocumentCodeWithDifferentDateIsNotAutoSkipped() {
        val existing =
            volume(
                id = "nt9999-old",
                documentCode = "NT9999",
                date = "2007-01-01",
            )

        val result =
            VolumeDuplicatePreflight.check(
                existing = listOf(existing),
                rawRoot =
                    File(
                        "Megane-II_NT9999_2007-02-01",
                    ),
            )

        assertNull(result.exact)
        assertEquals(
            listOf(existing),
            result.possible,
        )
    }

    @Test
    fun sameStableVolumeIdIsExactEvenWithoutParsedMetadata() {
        val existing =
            volume(
                id = "legacy-volume",
                documentCode = null,
                date = null,
            )

        val result =
            VolumeDuplicatePreflight.check(
                existing = listOf(existing),
                rawRoot =
                    File(
                        "legacy volume",
                    ),
            )

        assertEquals(existing, result.exact)
    }

    @Test
    fun codeWithoutDatesNeedsMatchingVehicleCodesBeforeAutoSkip() {
        val existing =
            volume(
                id = "other-name",
                documentCode = "NT8888",
                date = null,
                vehicleCodes =
                    listOf(
                        "E84",
                        "L84",
                        "K84",
                    ),
            )

        val exact =
            VolumeDuplicatePreflight.check(
                existing = listOf(existing),
                rawRoot =
                    File(
                        "E84-L84-K84_NT8888",
                    ),
            )

        assertEquals(existing, exact.exact)

        val uncertain =
            VolumeDuplicatePreflight.check(
                existing =
                    listOf(
                        existing.copy(
                            vehicleCodes =
                                listOf(
                                    "B84",
                                ),
                        ),
                    ),
                rawRoot =
                    File(
                        "E84-L84-K84_NT8888",
                    ),
            )

        assertNull(uncertain.exact)
        assertEquals(1, uncertain.possible.size)
    }

    private fun volume(
        id: String,
        documentCode: String?,
        date: String?,
        vehicleCodes: List<String> = emptyList(),
    ): ProjectVolumeRecord =
        ProjectVolumeRecord(
            id = id,
            title =
                documentCode
                    ?: id,
            documentCode =
                documentCode,
            date =
                date,
            projectHint =
                null,
            datasetId =
                "dataset-" +
                    id,
            datasetTitle =
                "Megane II",
            manufacturer =
                "Renault",
            model =
                "Megane II",
            platform =
                null,
            yearsLabel =
                null,
            contentType =
                "technical-documentation",
            entrypoint =
                id +
                    "/INDEX.HTM",
            openEntrypoint =
                "INDEX.HTM",
            treeUri =
                "content://test/" +
                    id,
            vehicleCodes =
                vehicleCodes,
        )
}
