package com.saney.renaultdocs

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Import of a whole project from one `.rdproject` file (v0.5.132).
 *
 * `.rdproject` (made by [RdprojectExporter]) is a ZIP:
 *   rdproject.json          — project + list of volumes (always the FIRST entry:
 *                             the exporter writes files sorted by path)
 *   volumes/<name>.rdpkg    — one ordinary volume package per volume
 *
 * Everything in this file is plain JVM (no Android) so it is covered by JVM tests.
 */
data class RdprojectManifestVolume(
    val id: String,
    val title: String,
    val file: String,
    val documentCode: String?,
    val date: String?,
    val vehicleCodes: List<String>,
) {
    /** Short name for the screen: «NT8299A · 2005-03», or the title. */
    val label: String
        get() =
            listOfNotNull(
                documentCode?.takeIf { it.isNotBlank() },
                date?.takeIf { it.isNotBlank() },
            )
                .joinToString(" · ")
                .ifBlank { title.ifBlank { id } }
}

data class RdprojectManifest(
    val projectId: String,
    val projectTitle: String,
    val projectModel: String,
    val vehicleCodes: List<String>,
    val volumes: List<RdprojectManifestVolume>,
) {
    val displayTitle: String
        get() = projectTitle.ifBlank { projectModel }.ifBlank { projectId }
}

/** Not a Renault Docs project file, or a damaged one. The message is for the owner. */
class RdprojectFormatException(message: String) : IllegalArgumentException(message)

object RdprojectManifestParser {
    const val FORMAT = "renault-docs-project"
    const val SCHEMA_VERSION = 1
    const val MANIFEST_NAME = "rdproject.json"
    const val VOLUMES_PREFIX = "volumes/"
    const val MAX_MANIFEST_BYTES = 2 * 1024 * 1024

    private const val NOT_PROJECT_FILE =
        "Це не файл проєкту Renault Docs (.rdproject)."

    fun parse(text: String): RdprojectManifest {
        val json =
            runCatching { JSONObject(text) }
                .getOrElse { throw RdprojectFormatException(NOT_PROJECT_FILE) }
        if (json.optString("format") != FORMAT) {
            throw RdprojectFormatException(NOT_PROJECT_FILE)
        }
        val schema = json.optInt("schema_version", 0)
        if (schema != SCHEMA_VERSION) {
            throw RdprojectFormatException(
                "Файл проєкту зроблено новішою версією Renault Docs (формат $schema). Оновіть застосунок.",
            )
        }
        val project =
            json.optJSONObject("project")
                ?: throw RdprojectFormatException("У файлі проєкту немає опису проєкту.")
        val projectId = project.optString("id").trim()
        val projectTitle = project.optString("title").trim()
        val projectModel = project.optString("model").trim()
        if (projectId.isBlank() && projectTitle.isBlank() && projectModel.isBlank()) {
            throw RdprojectFormatException("У файлі проєкту немає назви проєкту.")
        }
        val volumesJson = json.optJSONArray("volumes") ?: JSONArray()
        val volumes = mutableListOf<RdprojectManifestVolume>()
        val seenFiles = mutableSetOf<String>()
        for (index in 0 until volumesJson.length()) {
            val item =
                volumesJson.optJSONObject(index)
                    ?: throw RdprojectFormatException("Пошкоджений список томів у файлі проєкту.")
            val file =
                safeVolumePath(item.optString("file"))
                    ?: throw RdprojectFormatException(
                        "Некоректний шлях тому у файлі проєкту: " + item.optString("file"),
                    )
            if (!seenFiles.add(file)) {
                throw RdprojectFormatException("Той самий том записано у файлі двічі: $file")
            }
            volumes +=
                RdprojectManifestVolume(
                    id = item.optString("id").trim(),
                    title = item.optString("title").trim(),
                    file = file,
                    documentCode = item.optString("document_code").trim().takeIf { it.isNotBlank() },
                    date = item.optString("date").trim().takeIf { it.isNotBlank() },
                    vehicleCodes = strings(item.optJSONArray("vehicle_codes")),
                )
        }
        if (volumes.isEmpty()) {
            throw RdprojectFormatException("У файлі проєкту немає томів.")
        }
        return RdprojectManifest(
            projectId = projectId,
            projectTitle = projectTitle,
            projectModel = projectModel,
            vehicleCodes = strings(project.optJSONArray("vehicle_codes")),
            volumes = volumes,
        )
    }

    /**
     * Only `volumes/<one file>.rdpkg`. No sub-folders, no `..`, no absolute paths:
     * the path from the file never decides where anything is written.
     */
    fun safeVolumePath(raw: String?): String? {
        val path = raw?.trim().orEmpty()
        if (!path.startsWith(VOLUMES_PREFIX)) return null
        val name = path.removePrefix(VOLUMES_PREFIX)
        if (name.isBlank() || name.length > 200) return null
        if (name.contains('/') || name.contains('\\') || name.contains('\u0000')) return null
        if (name == "." || name == ".." || name.startsWith(".")) return null
        if (!name.endsWith(".rdpkg", ignoreCase = true) || name.length <= ".rdpkg".length) return null
        return path
    }

    /**
     * Reads only the beginning of the file: the first ZIP entry must be
     * rdproject.json. Cheap even for a 3 GB file — the volumes are not touched.
     */
    fun readFromStream(input: InputStream): RdprojectManifest {
        val zip = ZipInputStream(input.buffered())
        val entry =
            runCatching { zip.nextEntry }.getOrNull()
                ?: throw RdprojectFormatException(NOT_PROJECT_FILE)
        if (entry.isDirectory || entry.name != MANIFEST_NAME) {
            throw RdprojectFormatException(NOT_PROJECT_FILE)
        }
        val bytes = readLimited(zip, MAX_MANIFEST_BYTES)
        return parse(bytes.toString(Charsets.UTF_8))
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) {
                throw RdprojectFormatException("Опис проєкту у файлі завеликий — файл пошкоджений.")
            }
        }
        return out.toByteArray()
    }

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length())
            .mapNotNull { array.optString(it).trim().takeIf { value -> value.isNotBlank() } }
    }
}

/**
 * One pass through the whole `.rdproject`: every wanted `volumes/<name>.rdpkg` is copied
 * to [workDir] and handed to [onVolume] (which installs it); the copy is deleted
 * right after, so at most one volume is on disk twice. Volumes that are not wanted
 * (already on the phone) are read past without being written anywhere.
 */
object RdprojectArchiveWalker {
    data class Progress(
        val bytesRead: Long,
        val volumeIndex: Int,
        val volumeTotal: Int,
        val file: String,
        val wanted: Boolean,
    )

    class LowSpaceException(message: String) : IllegalStateException(message)

    /** Stop copying when less than this stays free (the install needs room too). */
    const val MIN_FREE_BYTES = 300L * 1024L * 1024L

    /**
     * @return files from the manifest that were not found in the archive.
     */
    fun walk(
        input: InputStream,
        manifest: RdprojectManifest,
        wanted: Set<String>,
        workDir: File,
        freeBytes: () -> Long,
        isCancelled: () -> Boolean,
        onProgress: (Progress) -> Unit,
        onVolume: (RdprojectManifestVolume, File) -> Unit,
    ): List<String> {
        val byFile = manifest.volumes.associateBy { it.file }
        val counting = CountingStream(input)
        val zip = ZipInputStream(counting.buffered(1024 * 1024))
        val found = mutableSetOf<String>()
        val buffer = ByteArray(1024 * 1024)
        workDir.mkdirs()
        zip.use {
            while (true) {
                if (isCancelled()) throw OperationCancelledException()
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val volume = byFile[entry.name] ?: continue
                if (!found.add(volume.file)) continue
                val index = manifest.volumes.indexOf(volume)
                val isWanted = wanted.contains(volume.file)
                if (!isWanted) {
                    // Read past it: the bytes go nowhere.
                    drain(zip, buffer, isCancelled) {
                        onProgress(Progress(counting.count, index, manifest.volumes.size, volume.file, false))
                    }
                    continue
                }
                val target = File(workDir, "volume-" + (index + 1) + ".rdpkg")
                target.delete()
                try {
                    target.outputStream().use { out ->
                        copy(zip, out, buffer, isCancelled, freeBytes) {
                            onProgress(Progress(counting.count, index, manifest.volumes.size, volume.file, true))
                        }
                    }
                    onVolume(volume, target)
                } finally {
                    target.delete()
                }
            }
        }
        return manifest.volumes.map { it.file }.filterNot { found.contains(it) }
    }

    private fun drain(
        input: InputStream,
        buffer: ByteArray,
        isCancelled: () -> Boolean,
        tick: () -> Unit,
    ) {
        var sinceTick = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            sinceTick += read
            if (sinceTick >= TICK_BYTES) {
                sinceTick = 0
                if (isCancelled()) throw OperationCancelledException()
                tick()
            }
        }
        tick()
    }

    private fun copy(
        input: InputStream,
        out: OutputStream,
        buffer: ByteArray,
        isCancelled: () -> Boolean,
        freeBytes: () -> Long,
        tick: () -> Unit,
    ) {
        var sinceTick = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            sinceTick += read
            if (sinceTick >= TICK_BYTES) {
                sinceTick = 0
                if (isCancelled()) throw OperationCancelledException()
                if (freeBytes() < MIN_FREE_BYTES) {
                    throw LowSpaceException(
                        "На телефоні закінчується місце — імпорт зупинено. Уже додані томи лишились.",
                    )
                }
                tick()
            }
        }
        tick()
    }

    private const val TICK_BYTES = 4L * 1024L * 1024L

    private class CountingStream(private val source: InputStream) : InputStream() {
        var count = 0L
            private set

        override fun read(): Int {
            val value = source.read()
            if (value >= 0) count++
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = source.read(b, off, len)
            if (read > 0) count += read
            return read
        }

        override fun close() = source.close()
    }
}
