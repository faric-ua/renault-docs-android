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
    private const val INCOMING = ".incoming"
    private const val STALE_INCOMING_MS = 6L * 60L * 60L * 1000L

    fun projectFile(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): File =
        File(
            directory(
                context,
                PROJECTS,
            ),
            RdprojectExporter.defaultFileName(
                project,
                volumes,
            ),
        )

    fun existingProjectFile(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): File? {
        val canonical =
            projectFile(
                context,
                project,
                volumes,
            )

        if (
            canonical.isFile
        ) {
            return canonical
        }

        val legacy =
            File(
                directory(
                    context,
                    PROJECTS,
                ),
                legacyProjectFileName(
                    project,
                ),
            )

        if (
            !legacy.isFile
        ) {
            return null
        }

        if (
            legacy.absolutePath !=
                canonical.absolutePath &&
            legacy.renameTo(
                canonical,
            )
        ) {
            return canonical
        }

        return legacy
    }

    fun hasProjectFile(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): Boolean =
        existingProjectFile(
            context,
            project,
            volumes,
        ) !=
            null

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

    fun existingVolumeFile(
        context: Context,
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): File? {
        val canonical =
            volumeFile(
                context,
                project,
                volume,
            )

        if (
            canonical.isFile
        ) {
            return canonical
        }

        val legacy =
            File(
                directory(
                    context,
                    VOLUMES,
                ),
                legacyVolumeFileName(
                    project,
                    volume,
                ),
            )

        if (
            !legacy.isFile
        ) {
            return null
        }

        if (
            legacy.absolutePath !=
                canonical.absolutePath &&
            legacy.renameTo(
                canonical,
            )
        ) {
            return canonical
        }

        return legacy
    }

    fun hasVolumeFile(
        context: Context,
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): Boolean =
        existingVolumeFile(
            context,
            project,
            volume,
        ) !=
            null

    fun deleteProject(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): Boolean {
        val canonical =
            projectFile(
                context,
                project,
                volumes,
            )
        val legacy =
            File(
                directory(
                    context,
                    PROJECTS,
                ),
                legacyProjectFileName(
                    project,
                ),
            )

        return listOf(
            canonical,
            legacy,
        )
            .distinctBy {
                it.absolutePath
            }
            .all(
                ::deleteIfPresent,
            )
    }

    fun deleteVolume(
        context: Context,
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): Boolean {
        val canonical =
            volumeFile(
                context,
                project,
                volume,
            )
        val legacy =
            File(
                directory(
                    context,
                    VOLUMES,
                ),
                legacyVolumeFileName(
                    project,
                    volume,
                ),
            )

        return listOf(
            canonical,
            legacy,
        )
            .distinctBy {
                it.absolutePath
            }
            .all(
                ::deleteIfPresent,
            )
    }

    private fun legacyProjectFileName(
        project: RenaultProject,
    ): String =
        project.id.ifBlank {
            "renault-project"
        } +
            ".rdproject"

    private fun legacyVolumeFileName(
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): String {
        val parts =
            buildList {
                add(
                    RenaultVolumeIdentity.safeFilePart(
                        project.model,
                    ),
                )
                add(
                    RenaultVolumeIdentity.safeFilePart(
                        volume.documentCode
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: volume.id,
                    ),
                )

                volume.date
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            RenaultVolumeIdentity.safeFilePart(
                                it,
                            ),
                        )
                    }
            }

        return parts.joinToString(
            "_",
        ) +
            ".rdpkg"
    }

    /**
     * Temporary target for a file being prepared. It lives outside projects/ and volumes/,
     * so ⇧ never appears for a half-written file (interrupted, failed or cancelled run).
     */
    fun incomingFile(
        context: Context,
        finalFile: File,
    ): File =
        File(
            directory(context, INCOMING),
            finalFile.name + "." + System.currentTimeMillis() + ".partial",
        )

    /**
     * Moves a fully written and verified temporary file into place.
     * One prepared file per volume/project: an existing one is never replaced.
     */
    fun commitIncoming(
        temp: File,
        finalFile: File,
    ) {
        require(temp.isFile && temp.length() > 0L) { "Підготовлений файл порожній або відсутній." }
        require(!finalFile.exists()) {
            "Підготовлений файл уже є. Спершу видали його, щоб зробити новий."
        }
        finalFile.parentFile?.mkdirs()
        require(temp.renameTo(finalFile)) { "Не вдалося зберегти підготовлений файл." }
    }

    /** Removes only abandoned temporary files (killed process), never prepared results. */
    fun cleanupStaleIncoming(context: Context) {
        val now = System.currentTimeMillis()
        directory(context, INCOMING).listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".partial") && now - it.lastModified() > STALE_INCOMING_MS }
            .forEach { runCatching { it.delete() } }
    }

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
