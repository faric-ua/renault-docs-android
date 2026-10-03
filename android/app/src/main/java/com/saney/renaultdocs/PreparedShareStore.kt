package com.saney.renaultdocs

import android.content.Context
import java.io.File

/**
 * Persistent app-private storage for files prepared for sharing.
 *
 * These copies survive closing/cancelling the Android Share Sheet and are
 * removed only when explicitly replaced/deleted or when app data is cleared.
 * Source Renault files and ProjectStore associations are never touched here.
 */
object PreparedShareStore {
    private const val ROOT = "prepared-share"
    private const val PROJECTS = "projects"
    private const val VOLUMES = "volumes"

    fun projectFile(
        context: Context,
        project: RenaultProject,
    ): File =
        File(
            directory(context, PROJECTS),
            RdprojectExporter.defaultFileName(project),
        )

    fun volumeFile(
        context: Context,
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): File =
        File(
            directory(context, VOLUMES),
            RdpkgExporter.defaultFileName(
                project = project,
                volume = volume,
            ),
        )

    fun deleteProject(
        context: Context,
        project: RenaultProject,
    ): Boolean =
        deleteIfPresent(
            projectFile(context, project),
        )

    fun deleteVolume(
        context: Context,
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): Boolean =
        deleteIfPresent(
            volumeFile(context, project, volume),
        )

    private fun directory(
        context: Context,
        child: String,
    ): File =
        File(
            File(context.filesDir, ROOT),
            child,
        ).apply {
            mkdirs()
        }

    private fun deleteIfPresent(
        file: File,
    ): Boolean =
        !file.exists() || file.delete()
}
