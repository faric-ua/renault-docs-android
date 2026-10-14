package com.saney.renaultdocs

import java.util.Locale

/**
 * What a `.rdproject` import will do — decided BEFORE anything is written (v0.5.132).
 * Owner rule: show the project and the volumes that are already on the phone, then,
 * after «так», add only the missing volumes. Nothing existing is replaced.
 *
 * Plain JVM: the caller maps ProjectStore records into these small types.
 */
data class RdprojectExistingProject(
    val id: String,
    val title: String,
)

data class RdprojectExistingVolume(
    val projectId: String,
    val projectTitle: String,
    val id: String,
    val documentCode: String?,
    val date: String?,
    val vehicleCodes: List<String>,
)

enum class RdprojectVolumeStatus {
    /** Not on the phone — will be added. */
    NEW,

    /** Already in the project it is imported into — skipped. */
    IN_TARGET_PROJECT,

    /** Already on the phone in another project — skipped, nothing is moved. */
    IN_OTHER_PROJECT,
}

data class RdprojectVolumePlan(
    val volume: RdprojectManifestVolume,
    val status: RdprojectVolumeStatus,
    val existingProjectTitle: String? = null,
)

data class RdprojectImportPlan(
    val manifest: RdprojectManifest,
    /** Existing project the volumes go into; null = a new project is created. */
    val targetProject: RdprojectExistingProject?,
    val volumes: List<RdprojectVolumePlan>,
) {
    val createsProject: Boolean get() = targetProject == null
    val targetTitle: String get() = targetProject?.title ?: manifest.displayTitle
    val toImport: List<RdprojectVolumePlan> get() = volumes.filter { it.status == RdprojectVolumeStatus.NEW }
    val alreadyPresent: List<RdprojectVolumePlan> get() = volumes.filterNot { it.status == RdprojectVolumeStatus.NEW }
}

object RdprojectImportPlanner {
    fun plan(
        manifest: RdprojectManifest,
        targetProject: RdprojectExistingProject?,
        existing: List<RdprojectExistingVolume>,
    ): RdprojectImportPlan =
        RdprojectImportPlan(
            manifest = manifest,
            targetProject = targetProject,
            volumes =
                manifest.volumes.map { volume ->
                    val matches = existing.filter { isSameVolume(volume, it) }
                    val inTarget =
                        targetProject != null && matches.any { it.projectId == targetProject.id }
                    when {
                        inTarget ->
                            RdprojectVolumePlan(volume, RdprojectVolumeStatus.IN_TARGET_PROJECT, targetProject?.title)
                        matches.isNotEmpty() ->
                            RdprojectVolumePlan(
                                volume,
                                RdprojectVolumeStatus.IN_OTHER_PROJECT,
                                matches.first().projectTitle,
                            )
                        else -> RdprojectVolumePlan(volume, RdprojectVolumeStatus.NEW)
                    }
                },
        )

    /**
     * Same rules as [VolumeDuplicatePreflight]: same volume id, or the same
     * document code (NT8340A ≠ NT8342A — exact code only) and the same date;
     * without dates on both sides — the same non-empty set of vehicle codes.
     */
    fun isSameVolume(
        volume: RdprojectManifestVolume,
        existing: RdprojectExistingVolume,
    ): Boolean {
        if (volume.id.isNotBlank() && volume.id.equals(existing.id, ignoreCase = true)) {
            return true
        }
        val code = volume.documentCode?.trim()?.takeIf { it.isNotBlank() } ?: return false
        val existingCode = existing.documentCode?.trim()?.takeIf { it.isNotBlank() } ?: return false
        if (!code.equals(existingCode, ignoreCase = true)) return false
        val date = volume.date?.trim()?.takeIf { it.isNotBlank() }
        val existingDate = existing.date?.trim()?.takeIf { it.isNotBlank() }
        if (date != null && existingDate != null) return date == existingDate
        if (date == null && existingDate == null) {
            val a = volume.vehicleCodes.map { it.uppercase(Locale.ROOT) }.toSet()
            val b = existing.vehicleCodes.map { it.uppercase(Locale.ROOT) }.toSet()
            return a.isNotEmpty() && a == b
        }
        return false
    }

    /**
     * Rough disk need for the volumes that will be added: their share of the file,
     * plus a margin (the copy of one volume and unpacking). Only for the warning line.
     */
    fun estimatedNeedBytes(
        plan: RdprojectImportPlan,
        fileSizeBytes: Long,
    ): Long? {
        if (fileSizeBytes <= 0L || plan.volumes.isEmpty()) return null
        if (plan.toImport.isEmpty()) return 0L
        val share = fileSizeBytes.toDouble() * plan.toImport.size / plan.volumes.size
        return (share * 1.25).toLong() + RdprojectArchiveWalker.MIN_FREE_BYTES
    }
}
