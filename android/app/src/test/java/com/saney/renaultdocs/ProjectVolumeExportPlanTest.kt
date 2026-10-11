package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectVolumeExportPlanTest {
    @Test
    fun differentRenaultNtCodesAreIndependentVolumeFiles() {
        val list = listOf(
            ProjectVolumeExportPlan.Item(
                "megane-nt8340a", "Megane-II_NT8340A.rdpkg", "NT8340A", "2006-04-18",
            ),
            ProjectVolumeExportPlan.Item(
                "megane-nt8342a", "Megane-II_NT8342A.rdpkg", "NT8342A", "2006-10-09",
            ),
        )
        ProjectVolumeExportPlan.validate(list)
        assertEquals(2, list.size)
        assertEquals(2, list.map { it.volumeId }.distinct().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sameFilenameCaseInsensitiveCannotOverwriteAnExistingPackage() {
        ProjectVolumeExportPlan.validate(listOf(
            ProjectVolumeExportPlan.Item("id-1", "Volume.rdpkg", "NT8340A", null),
            ProjectVolumeExportPlan.Item("id-2", "volume.rdpkg", "NT8342A", null),
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun distinctFilenameCannotAliasSameVolumeId() {
        ProjectVolumeExportPlan.validate(listOf(
            ProjectVolumeExportPlan.Item("duplicate", "Vol-1.rdpkg", "NT8340A", null),
            ProjectVolumeExportPlan.Item("duplicate", "Vol-2.rdpkg", "NT8342A", null),
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun folderTraversalNameIsRejected() {
        ProjectVolumeExportPlan.validate(listOf(
            ProjectVolumeExportPlan.Item("valid", "../escape.rdpkg", "NT8340A", null),
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPlanIsRejected() {
        ProjectVolumeExportPlan.validate(emptyList())
    }

    @Test
    fun normalFilenameWithUnicodeCanBeSeparatePackage() {
        val item = ProjectVolumeExportPlan.Item(
            "id-1", "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
            "NT8340A", "2006-04-18",
        )
        ProjectVolumeExportPlan.validate(listOf(item))
        assertTrue(item.fileName.endsWith(".rdpkg"))
    }

    private fun twoDistinctVolumes() = listOf(
        ProjectVolumeExportPlan.Item(
            "megane-nt8340a", "Megane-II_NT8340A.rdpkg", "NT8340A", "2006-04-18",
        ),
        ProjectVolumeExportPlan.Item(
            "megane-nt8342a", "Megane-II_NT8342A.rdpkg", "NT8342A", "2006-10-09",
        ),
    )

    @Test
    fun oneVolumeSelectsExactlyItsOpaqueId() {
        val selected = ProjectVolumeExportPlan.select(twoDistinctVolumes(), listOf("megane-nt8342a"))
        assertEquals(1, selected.size)
        assertEquals("megane-nt8342a", selected.single().volumeId)
        assertEquals("NT8342A", selected.single().documentCode)
    }

    @Test
    fun allVolumesRetainTheProjectsOriginalOrder() {
        val selected = ProjectVolumeExportPlan.select(
            twoDistinctVolumes(), listOf("megane-nt8342a", "megane-nt8340a"),
        )
        assertEquals(listOf("megane-nt8340a", "megane-nt8342a"), selected.map { it.volumeId })
    }

    @Test(expected = IllegalArgumentException::class)
    fun staleVolumeIdCannotExpandToFullProject() {
        ProjectVolumeExportPlan.select(twoDistinctVolumes(), listOf("no-longer-installed"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySelectionMustNotFallbackToExportAll() {
        ProjectVolumeExportPlan.select(twoDistinctVolumes(), emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateSelectedIdsAreRejected() {
        ProjectVolumeExportPlan.select(
            twoDistinctVolumes(), listOf("megane-nt8340a", "megane-nt8340a"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun mixedKnownAndUnknownSelectionIsRejectedRatherThanPartialExport() {
        ProjectVolumeExportPlan.select(
            twoDistinctVolumes(), listOf("megane-nt8340a", "unknown"),
        )
    }

}
