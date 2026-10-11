package com.saney.renaultdocs

/**
 * Presentation state of the step-by-step "export separate volumes" window (#178).
 *
 * Pure Kotlin, no Android types: it survives rotation through a Bundle and is unit-tested.
 * Nothing here starts SAF or an export. The window only collects:
 *   1) exact opaque volume IDs, 2) a destination folder URI, 3) an explicit start.
 * The export itself is still revalidated by MainActivity and ProjectFolderExportService.
 */
class ProjectExportWizardState(
    step: Step = Step.VOLUMES,
    selectedVolumeIds: Collection<String> = emptyList(),
    folderUri: String? = null,
    var scrollY: Int = 0,
) {
    enum class Step(val number: Int) {
        VOLUMES(1),
        FOLDER(2),
        CONFIRM(3),
    }

    var step: Step = step
        private set

    var folderUri: String? = folderUri?.takeIf { it.isNotBlank() }
        private set

    private val selected = linkedSetOf<String>().apply { addAll(selectedVolumeIds) }

    val selectedVolumeIds: List<String>
        get() = selected.toList()

    val selectedCount: Int
        get() = selected.size

    fun isSelected(volumeId: String): Boolean = volumeId in selected

    fun setSelected(volumeId: String, checked: Boolean) {
        if (checked) selected.add(volumeId) else selected.remove(volumeId)
    }

    /** Select every planned volume, or clear when everything is already selected. */
    fun toggleAll(planVolumeIds: List<String>) {
        val selectAll = !allSelected(planVolumeIds)
        selected.clear()
        if (selectAll) selected.addAll(planVolumeIds)
    }

    fun allSelected(planVolumeIds: List<String>): Boolean =
        planVolumeIds.isNotEmpty() && selected.containsAll(planVolumeIds) &&
            selected.size == planVolumeIds.toSet().size

    /** False when the project changed under the window: stale IDs must never be exported. */
    fun isConsistentWith(planVolumeIds: List<String>): Boolean =
        planVolumeIds.toSet().containsAll(selected)

    fun onFolderPicked(uri: String?) {
        val value = uri?.takeIf { it.isNotBlank() } ?: return
        folderUri = value
    }

    fun canGoNext(): Boolean =
        when (step) {
            Step.VOLUMES -> selected.isNotEmpty()
            Step.FOLDER -> selected.isNotEmpty() && folderUri != null
            Step.CONFIRM -> false
        }

    fun canStart(): Boolean =
        step == Step.CONFIRM && selected.isNotEmpty() && folderUri != null

    fun next(): Boolean {
        if (!canGoNext()) return false
        step = if (step == Step.VOLUMES) Step.FOLDER else Step.CONFIRM
        return true
    }

    /** Returns false on the first step: the caller closes the window instead. */
    fun back(): Boolean {
        step =
            when (step) {
                Step.VOLUMES -> return false
                Step.FOLDER -> Step.VOLUMES
                Step.CONFIRM -> Step.FOLDER
            }
        return true
    }

    companion object {
        fun restore(
            stepName: String?,
            selectedVolumeIds: List<String>?,
            folderUri: String?,
            scrollY: Int,
        ): ProjectExportWizardState {
            val restoredStep = Step.values().firstOrNull { it.name == stepName } ?: Step.VOLUMES
            val state = ProjectExportWizardState(
                step = Step.VOLUMES,
                selectedVolumeIds = selectedVolumeIds.orEmpty(),
                folderUri = folderUri,
                scrollY = scrollY.coerceAtLeast(0),
            )
            // Never restore into a step whose preconditions are gone.
            while (state.step != restoredStep && state.next()) Unit
            return state
        }
    }
}
