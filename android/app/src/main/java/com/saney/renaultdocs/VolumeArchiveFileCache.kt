package com.saney.renaultdocs

import java.io.File
import java.security.MessageDigest

/**
 * Archives plan v2, stage 1: a PDF inside a volume archive cannot be handed to Android's
 * PdfRenderer as is — it needs a real file it can jump around in. So a PDF is copied out of
 * the archive once into the app's own cache folder (phone memory, `cacheDir` — never the
 * memory card, never in backups) and opened from there; the next time it is already there.
 *
 * The key holds the archive's path, size and time and the path inside it: a remade or
 * replaced archive never reuses an old copy. Only this folder is touched: when it grows over
 * [limitBytes] the least recently used copies are removed — copies of this cache only, never
 * documents or volumes.
 *
 * Plain Kotlin + java.io — tested on the JVM.
 */
class VolumeArchiveFileCache(
    private val dir: File,
    private val limitBytes: Long = DEFAULT_LIMIT_BYTES,
) {
    data class Got(
        val file: File,
        /** True when it was copied out of the archive just now (for the timing log). */
        val extracted: Boolean,
    )

    /** The file [path] of [archive] as a real file, or null when the archive has no such file. */
    fun get(
        archive: VolumeArchive,
        path: String,
    ): Got? {
        val target = fileFor(archive, path)
        val expectedSize = archive.attributes(path)?.takeIf { !it.isDirectory }?.size ?: return null
        synchronized(lockFor(target.name)) {
            if (target.isFile && target.length() == expectedSize) {
                target.setLastModified(System.currentTimeMillis())
                return Got(target, extracted = false)
            }
            dir.mkdirs()
            val part = File(dir, target.name + ".part")
            val input = archive.open(path) ?: return null
            input.use { source -> part.outputStream().use { source.copyTo(it, 256 * 1024) } }
            if (part.length() != expectedSize) {
                part.delete()
                error("Файл з архіву скопіювався не повністю: $path")
            }
            if (!part.renameTo(target)) {
                part.delete()
                error("Не вдалося зберегти файл з архіву в кеш: $path")
            }
        }
        if (++writesSinceTrim >= TRIM_EVERY_WRITES) {
            writesSinceTrim = 0
            trim()
        }
        return Got(target, extracted = true)
    }

    /** Removes the least recently used copies of this folder until it fits [limitBytes]. */
    fun trim() {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(SUFFIX) }?.toList().orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= limitBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= limitBytes * 9 / 10) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    fun fileFor(
        archive: VolumeArchive,
        path: String,
    ): File {
        val key = "v1|${archive.file.absolutePath}|${archive.archiveSize}|${archive.archiveTime}|$path"
        return File(dir, sha256(key) + SUFFIX)
    }

    @Volatile private var writesSinceTrim = 0

    private fun lockFor(name: String): Any = locks.computeIfAbsent(name) { Any() }

    private val locks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    companion object {
        const val DIR_NAME = "renault-archive-files"
        const val SUFFIX = ".bin"
        const val DEFAULT_LIMIT_BYTES = 300L * 1024 * 1024
        const val TRIM_EVERY_WRITES = 10

        private fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
