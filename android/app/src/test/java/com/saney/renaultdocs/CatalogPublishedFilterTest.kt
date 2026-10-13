package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPublishedFilterTest {
    private fun item(id: String, code: String, date: String) =
        ProjectVolumeExportPlan.Item(
            volumeId = id,
            fileName = "Megane-II_E84_${code}_Visu_$date.rdpkg",
            documentCode = code,
            date = date,
        )

    private fun published(code: String, date: String, name: String = "other.rdpkg") =
        DriveCatalogVolume(
            id = "megane-ii-" + code.lowercase() + "-" + date,
            documentCode = code,
            date = date,
            vehicleCodes = emptyList(),
            documentType = null,
            documentVersion = null,
            region = null,
            fileName = name,
            driveFileId = "D",
            sizeBytes = 1,
            sha256 = null,
        )

    @Test
    fun sameCodeAndDateIsPublishedButNeighbourNtIsNot() {
        val plan = listOf(
            item("a", "NT8340A", "2006-04-18"),
            item("b", "NT8342A", "2006-10-09"),
            item("c", "NT8340A", "2007-01-01"),
        )
        val result = CatalogPublishedFilter.publishedVolumeIds(plan, listOf(published("NT8340A", "2006-04-18")))
        assertEquals(setOf("a"), result)
    }

    @Test
    fun exactFileNameCountsAsPublished() {
        val plan = listOf(item("a", "NT8393", "2007-06-04"))
        val result = CatalogPublishedFilter.publishedVolumeIds(
            plan,
            listOf(published("XX", "", name = "megane-ii_e84_nt8393_visu_2007-06-04.rdpkg")),
        )
        assertEquals(setOf("a"), result)
    }

    @Test
    fun stateSelectsOnlyNewVolumes() {
        val state = ProjectExportWizardState(selectedVolumeIds = listOf("a"))
        state.selectOnly(listOf("a", "b", "c")) { it != "a" }
        assertEquals(listOf("b", "c"), state.selectedVolumeIds)
    }
}
