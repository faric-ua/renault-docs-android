package com.saney.renaultdocs

import java.io.File
import java.security.MessageDigest

/**
 * v0.5.139: pages of PDFs already drawn are kept as small PNG files in the app's own cache
 * folder (phone memory, `cacheDir` — never the memory card, never in backups, Android may
 * clear it when space runs out). A diagram opened before shows at once, also after the
 * app was closed (before: only in RAM, lost on every restart).
 *
 * The key includes the PDF's size and time, so a volume installed again gets new pictures.
 * Only this folder is touched: when it grows over [limitBytes] the oldest pictures are
 * removed — pictures of this cache only, never documents or volumes.
 *
 * Plain Kotlin + java.io — tested on the JVM.
 */
class PdfPageDiskCache(
    private val dir: File,
    private val limitBytes: Long = DEFAULT_LIMIT_BYTES,
) {
    @Volatile private var writesSinceTrim = 0

    fun get(key: String): ByteArray? {
        val file = fileFor(key)
        return runCatching {
            if (!file.isFile) return null
            val bytes = file.readBytes()
            // «Last used» for trimming: the oldest not-opened pictures go first.
            file.setLastModified(System.currentTimeMillis())
            bytes.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    fun put(
        key: String,
        bytes: ByteArray,
    ) {
        if (bytes.isEmpty() || bytes.size > limitBytes) return
        runCatching {
            dir.mkdirs()
            val target = fileFor(key)
            val part = File(dir, target.name + ".part")
            part.writeBytes(bytes)
            if (!part.renameTo(target)) part.delete()
            if (++writesSinceTrim >= TRIM_EVERY_WRITES) {
                writesSinceTrim = 0
                trim()
            }
        }
    }

    /** Removes the least recently used pictures of this folder until it fits [limitBytes]. */
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

    fun fileFor(key: String): File = File(dir, sha256(key) + SUFFIX)

    companion object {
        const val DIR_NAME = "renault-pdf-pages"
        const val SUFFIX = ".png"
        const val DEFAULT_LIMIT_BYTES = 200L * 1024 * 1024
        const val TRIM_EVERY_WRITES = 20

        fun key(
            namespace: String,
            path: String,
            fileSize: Long,
            fileTime: Long,
            page: Int,
            width: Int,
        ): String = "v1|$namespace|$path|$fileSize|$fileTime|$page|$width"

        private fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
