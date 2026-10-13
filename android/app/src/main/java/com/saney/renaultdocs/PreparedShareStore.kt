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

    /**
     * #173 (v0.5.122): a prepared .rdproject is an unchanging snapshot of the
     * project at the moment it was made. It is found by WHO it belongs to
     * (project id inside its rdproject.json), not by the file name — the name
     * is built from the current volumes and drifts when a volume is added or
     * its metadata is repaired, which hid (and before v0.5.122 deleted) the
     * owner's 21-volume Megane II file after the 22nd volume was added.
     *
     * Looking up never renames, rewrites or deletes anything.
     */
    data class ProjectSnapshot(
        val file: File,
        /** Volume ids recorded inside the snapshot; null if it could not be read. */
        val volumeIds: Set<String>?,
    ) {
        val volumeCount: Int?
            get() = volumeIds?.size

        /** false = made for an older set of volumes (one was added, removed or moved since). */
        fun matches(
            volumes: List<ProjectVolumeRecord>,
        ): Boolean =
            volumeIds == null ||
                volumeIds ==
                volumes.map { it.id }.toSet()
    }

    private data class ManifestInfo(
        val projectId: String?,
        val volumeIds: Set<String>?,
    )

    private val manifestCache =
        java.util.concurrent.ConcurrentHashMap<String, Pair<String, ManifestInfo>>()

    private const val MAX_MANIFEST_BYTES =
        4 * 1024 * 1024

    fun projectSnapshots(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): List<ProjectSnapshot> =
        projectSnapshotsIn(
            directory(
                context,
                PROJECTS,
            ),
            project,
            volumes,
        )

    internal fun projectSnapshotsIn(
        projectsDirectory: File,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): List<ProjectSnapshot> {
        val canonicalName =
            RdprojectExporter.defaultFileName(
                project,
                volumes,
            )
        val legacyName =
            legacyProjectFileName(
                project,
            )

        return projectsDirectory
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile &&
                    it.name.endsWith(
                        ".rdproject",
                        ignoreCase = true,
                    )
            }
            .mapNotNull { file ->
                val info =
                    manifestInfo(
                        file,
                    )
                val belongs =
                    if (info.projectId != null) {
                        info.projectId == project.id
                    } else {
                        // unreadable manifest: fall back to the old name rules
                        file.name == canonicalName ||
                            file.name == legacyName
                    }
                if (belongs) {
                    ProjectSnapshot(
                        file = file,
                        volumeIds = info.volumeIds,
                    )
                } else {
                    null
                }
            }
            .sortedByDescending {
                it.file.lastModified()
            }
    }

    fun projectSnapshot(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): ProjectSnapshot? =
        projectSnapshots(
            context,
            project,
            volumes,
        ).firstOrNull()

    fun existingProjectFile(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): File? =
        projectSnapshot(
            context,
            project,
            volumes,
        )?.file

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

    /** Read-only: the small rdproject.json inside the ZIP (central directory + one entry). */
    private fun manifestInfo(
        file: File,
    ): ManifestInfo {
        val key =
            file.length().toString() +
                ":" +
                file.lastModified()
        manifestCache[file.absolutePath]
            ?.takeIf {
                it.first == key
            }
            ?.let {
                return it.second
            }

        val info =
            runCatching {
                java.util.zip.ZipFile(file).use { zip ->
                    val entry =
                        zip.getEntry(
                            "rdproject.json",
                        )
                            ?: return@use null
                    if (
                        entry.size > MAX_MANIFEST_BYTES
                    ) {
                        return@use null
                    }
                    val text =
                        zip.getInputStream(entry).use {
                            it.readBytes().toString(Charsets.UTF_8)
                        }
                    parseManifest(text)
                }
            }.getOrNull()
                ?: ManifestInfo(
                    projectId = null,
                    volumeIds = null,
                )

        manifestCache[file.absolutePath] =
            key to info
        return info
    }

    internal fun parseManifestForTest(
        text: String,
    ): Pair<String?, Set<String>?> =
        parseManifest(text).let {
            it.projectId to it.volumeIds
        }

    private fun parseManifest(
        text: String,
    ): ManifestInfo {
        val root =
            org.json.JSONObject(text)
        val projectId =
            root.optJSONObject("project")
                ?.optString("id")
                ?.takeIf { it.isNotBlank() }
        val volumes =
            root.optJSONArray("volumes")
        val ids =
            volumes?.let { array ->
                (0 until array.length())
                    .mapNotNull {
                        array.optJSONObject(it)
                            ?.optString("id")
                            ?.takeIf { id -> id.isNotBlank() }
                    }
                    .toSet()
            }
        return ManifestInfo(
            projectId = projectId,
            volumeIds = ids,
        )
    }

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

    /**
     * Explicit user action only («Видалити підготовлений .rdproject» after a
     * confirmation). Deletes exactly the snapshot shown to the user.
     * #173: never called when volumes are added, removed or moved.
     */
    fun deleteProjectSnapshot(
        file: File,
    ): Boolean {
        manifestCache.remove(
            file.absolutePath,
        )
        return deleteIfPresent(
            file,
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
