package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectExportWizardStateTest {
    private val plan = listOf("megane-nt8445", "megane-nt8446", "megane-nt8206a", "megane-nt8220a")

    @Test
    fun cannotLeaveVolumesStepWithoutSelection() {
        val state = ProjectExportWizardState()
        assertFalse(state.canGoNext())
        assertFalse(state.next())
        assertEquals(ProjectExportWizardState.Step.VOLUMES, state.step)
    }

    @Test
    fun folderIsRequiredBeforeConfirmAndStart() {
        val state = ProjectExportWizardState()
        state.setSelected("megane-nt8445", true)
        assertTrue(state.next())
        assertEquals(ProjectExportWizardState.Step.FOLDER, state.step)
        assertFalse(state.next())
        state.onFolderPicked(null)
        state.onFolderPicked("")
        assertNull(state.folderUri)
        state.onFolderPicked("content://tree/primary%3ADocuments%2Fexport")
        assertTrue(state.next())
        assertEquals(ProjectExportWizardState.Step.CONFIRM, state.step)
        assertTrue(state.canStart())
    }

    @Test
    fun cancelledPickerKeepsPreviousFolder() {
        val state = ProjectExportWizardState(folderUri = "content://tree/a")
        state.onFolderPicked(null)
        assertEquals("content://tree/a", state.folderUri)
    }

    @Test
    fun backFromFirstStepMeansClose() {
        val state = ProjectExportWizardState(selectedVolumeIds = listOf("megane-nt8445"))
        assertFalse(state.back())
        state.next()
        assertTrue(state.back())
        assertEquals(ProjectExportWizardState.Step.VOLUMES, state.step)
    }

    @Test
    fun toggleAllSelectsEveryVolumeThenClears() {
        val state = ProjectExportWizardState(selectedVolumeIds = listOf("megane-nt8445"))
        state.toggleAll(plan)
        assertEquals(plan.toSet(), state.selectedVolumeIds.toSet())
        assertTrue(state.allSelected(plan))
        state.toggleAll(plan)
        assertEquals(0, state.selectedCount)
    }

    @Test
    fun distinctNtCodesStayDistinctSelections() {
        val state = ProjectExportWizardState()
        state.setSelected("megane-nt8340a", true)
        state.setSelected("megane-nt8342a", true)
        state.setSelected("megane-nt8340a", false)
        assertEquals(listOf("megane-nt8342a"), state.selectedVolumeIds)
    }

    @Test
    fun staleIdsAreDetectedWhenProjectChanged() {
        val state = ProjectExportWizardState(selectedVolumeIds = listOf("megane-nt8445", "gone"))
        assertFalse(state.isConsistentWith(plan))
        assertTrue(ProjectExportWizardState(selectedVolumeIds = listOf("megane-nt8446")).isConsistentWith(plan))
    }

    @Test
    fun restoreNeverSkipsMissingPreconditions() {
        val withoutFolder = ProjectExportWizardState.restore("CONFIRM", listOf("megane-nt8445"), null, 120)
        assertEquals(ProjectExportWizardState.Step.FOLDER, withoutFolder.step)
        assertEquals(120, withoutFolder.scrollY)

        val withoutVolumes = ProjectExportWizardState.restore("CONFIRM", emptyList(), "content://tree/a", 0)
        assertEquals(ProjectExportWizardState.Step.VOLUMES, withoutVolumes.step)

        val full = ProjectExportWizardState.restore(
            "CONFIRM", listOf("megane-nt8445", "megane-nt8206a"), "content://tree/a", -5,
        )
        assertEquals(ProjectExportWizardState.Step.CONFIRM, full.step)
        assertEquals(listOf("megane-nt8445", "megane-nt8206a"), full.selectedVolumeIds)
        assertEquals(0, full.scrollY)

        val unknown = ProjectExportWizardState.restore("BOGUS", listOf("megane-nt8445"), null, 0)
        assertEquals(ProjectExportWizardState.Step.VOLUMES, unknown.step)
    }
}
