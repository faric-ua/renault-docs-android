package com.saney.renaultdocs

import java.util.Locale

/**
 * v0.5.124 (owner 2026-10-13 19:37): «щоб я не шукав імена і не переплутав» —
 * the export window can tick only the volumes that are NOT yet in the public
 * catalog. A volume counts as published when the catalog has the same exact
 * file name, or the same NT code AND the same date. Similar NT codes are never
 * merged (NT8340A ≠ NT8342A). Pure Kotlin, unit-tested.
 */
object CatalogPublishedFilter {
    fun publishedVolumeIds(
        plan: List<ProjectVolumeExportPlan.Item>,
        catalogVolumes: List<DriveCatalogVolume>,
    ): Set<String> {
        val names =
            catalogVolumes
                .map { it.fileName.trim().lowercase(Locale.ROOT) }
                .toSet()
        val codesAndDates =
            catalogVolumes
                .mapNotNull { volume ->
                    val date = volume.date?.trim().orEmpty()
                    val code = volume.documentCode.trim().uppercase(Locale.ROOT)
                    if (code.isBlank() || date.isBlank()) null else code to date
                }
                .toSet()

        return plan
            .filter { item ->
                item.fileName.trim().lowercase(Locale.ROOT) in names ||
                    run {
                        val code = item.documentCode?.trim()?.uppercase(Locale.ROOT).orEmpty()
                        val date = item.date?.trim().orEmpty()
                        code.isNotBlank() && date.isNotBlank() && (code to date) in codesAndDates
                    }
            }
            .map { it.volumeId }
            .toSet()
    }

    /** Volumes of this project in the catalog (by project id). */
    fun catalogVolumesFor(
        catalog: DriveCatalog,
        projectId: String,
    ): List<DriveCatalogVolume> =
        catalog.projects
            .filter { it.id == projectId }
            .flatMap { it.volumes }
}
