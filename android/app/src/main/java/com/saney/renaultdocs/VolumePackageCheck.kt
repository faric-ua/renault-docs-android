package com.saney.renaultdocs

import java.io.BufferedInputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import org.json.JSONObject

/**
 * Archives plan v2, stage 2 (owner 2026-10-17 03:10: «давай етап 2»): an imported `.rdpkg`
 * is kept as one file and the volume is read from it ([VolumeArchive]), instead of
 * unpacking ~6–8 thousand files (18 volumes on the M30s card took ~25 hours).
 *
 * Before the copied file gets its final name, it is checked here as strictly as unpacking
 * checked it — and more:
 * - `rdpkg.json`: schema, format, a safe `package_id`;
 * - `renault-dataset.json`: exactly one volume, its start page is in the package, the
 *   `project_id` agrees with `rdpkg.json`;
 * - every entry is read to the end: its bytes must match the stored size and CRC-32
 *   ([ZipInputStream] checks that; a damaged package — like the ones the M30s wrote with
 *   its compressor, TODO №19 — fails here, not later in the viewer);
 * - the table of contents ([ZipFile], the one [VolumeArchive] reads from) must list exactly
 *   the same files as the entries themselves;
 * - no unsafe path (`..`), no file twice, no file and folder with the same name.
 *
 * Nothing is written, moved or deleted. Plain Kotlin + java.util.zip + org.json — tested on
 * the JVM.
 */
object VolumePackageCheck {
    const val FORMAT = "renault-volume-package-v1"
    const val SCHEMA_VERSION = 1
    private const val PACKAGE_MANIFEST = "rdpkg.json"
    private const val DATASET_MANIFEST = "renault-dataset.json"
    private const val MAX_ENTRY_COUNT = 250_000
    private const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024

    data class Result(
        val packageId: String,
        val projectId: String?,
        val fileCount: Int,
        val bytes: Long,
    )

    /** Thrown with words for the owner; nothing was changed. */
    class BadPackageException(
        message: String,
    ) : IllegalStateException(message)

    fun isSafePackageId(packageId: String): Boolean =
        packageId.matches(Regex("[A-Za-z0-9._-]{1,160}")) &&
            packageId != "." &&
            packageId != ".." &&
            !packageId.startsWith(".")

    /**
     * [progress] gets bytes of [file] read so far and the file size.
     * [isCancelled] is asked between entries and buffers; it throws [OperationCancelledException].
     */
    fun check(
        file: File,
        progress: ((done: Long, total: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false },
    ): Result {
        val total = file.length().coerceAtLeast(1L)
        val files = LinkedHashMap<String, Long>()
        val folders = HashSet<String>()
        var packageJson: ByteArray? = null
        var datasetJson: ByteArray? = null
        var bytes = 0L
        val buffer = ByteArray(256 * 1024)
        val counting = CountingStream(BufferedInputStream(file.inputStream(), 1024 * 1024))

        ZipInputStream(counting).use { zip ->
            while (true) {
                if (isCancelled()) throw OperationCancelledException()
                val entry =
                    try {
                        zip.nextEntry
                    } catch (error: java.io.IOException) {
                        throw BadPackageException("Файл тому пошкоджений: ${error.message}")
                    } ?: break
                val path =
                    VolumeArchive.cleanEntryName(entry.name)
                        ?: throw BadPackageException("RDPKG містить небезпечний шлях: ${entry.name}")
                if (path.isEmpty()) continue
                val parts = path.split('/')
                for (i in 1 until parts.size) folders.add(parts.subList(0, i).joinToString("/"))
                if (entry.isDirectory) {
                    folders.add(path)
                    continue
                }
                if (files.containsKey(path)) throw BadPackageException("У томі файл двічі: $path")
                if (files.size + 1 > MAX_ENTRY_COUNT) throw BadPackageException("RDPKG містить забагато файлів.")
                val keep = path == PACKAGE_MANIFEST || path == DATASET_MANIFEST
                val kept = if (keep) java.io.ByteArrayOutputStream() else null
                val crc = CRC32()
                var size = 0L
                try {
                    while (true) {
                        if (isCancelled()) throw OperationCancelledException()
                        val read = zip.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        crc.update(buffer, 0, read)
                        size += read
                        if (kept != null) {
                            if (kept.size() + read > MAX_MANIFEST_BYTES) {
                                throw BadPackageException("Завеликий $path.")
                            }
                            kept.write(buffer, 0, read)
                        }
                        progress?.invoke(counting.count.coerceAtMost(total), total)
                    }
                } catch (error: java.io.IOException) {
                    // Which file and what zlib said — for the owner's card and the journal.
                    throw BadPackageException("Файл тому пошкоджений у $path: ${error.message}")
                }
                // ZipInputStream checks size and CRC only when they are known; check again.
                if (entry.crc != -1L && entry.crc != crc.value) {
                    throw BadPackageException("Файл тому пошкоджений у $path: CRC не збігся.")
                }
                if (entry.size != -1L && entry.size != size) {
                    throw BadPackageException("Файл тому пошкоджений у $path: розмір не збігся.")
                }
                files[path] = crc.value
                bytes += size
                if (path == PACKAGE_MANIFEST) packageJson = kept?.toByteArray()
                if (path == DATASET_MANIFEST) datasetJson = kept?.toByteArray()
            }
        }
        val clash = files.keys.firstOrNull { folders.contains(it) }
        if (clash != null) throw BadPackageException("У томі файл і папка з однаковою назвою: $clash")
        if (files.isEmpty()) throw BadPackageException("RDPKG порожній.")

        // The table of contents is what the volume will be read through: it must agree.
        try {
            ZipFile(file).use { zip ->
                val listed = HashMap<String, Long>()
                val iterator = zip.entries()
                while (iterator.hasMoreElements()) {
                    val entry = iterator.nextElement()
                    val path = VolumeArchive.cleanEntryName(entry.name) ?: continue
                    if (path.isEmpty() || entry.isDirectory) continue
                    listed[path] = entry.crc
                }
                if (listed.size != files.size || listed.any { (path, crc) -> files[path] != crc }) {
                    throw BadPackageException("Зміст файлу тому не збігається з його вмістом — файл пошкоджений.")
                }
            }
        } catch (error: ZipException) {
            throw BadPackageException("Зміст файлу тому не читається: ${error.message}")
        }

        val manifest = parse(packageJson, "У .rdpkg немає rdpkg.json.")
        if (manifest.optInt("schema_version", 0) != SCHEMA_VERSION) {
            throw BadPackageException("Непідтримувана версія rdpkg schema.")
        }
        if (manifest.optString("format") != FORMAT) throw BadPackageException("Це не Renault Docs .rdpkg.")
        val packageId = manifest.optString("package_id").trim()
        if (!isSafePackageId(packageId)) throw BadPackageException("RDPKG має некоректний package_id.")
        val projectId = manifest.optString("project_id").trim().takeIf { it.isNotBlank() }

        val dataset = parse(datasetJson, "У .rdpkg немає renault-dataset.json.")
        val volumes =
            dataset.optJSONArray("volumes")
                ?: throw BadPackageException("Dataset manifest у .rdpkg не містить volumes.")
        if (volumes.length() != 1) {
            throw BadPackageException("RDPKG має містити рівно один том, знайдено: " + volumes.length())
        }
        val entrypoint = volumes.getJSONObject(0).optString("entrypoint").trim()
        if (entrypoint.isBlank()) throw BadPackageException("Том у .rdpkg не містить entrypoint.")
        val start =
            VolumeArchive.cleanEntryName(entrypoint)
                ?: throw BadPackageException("Том у .rdpkg має некоректний entrypoint.")
        if (!files.containsKey(start)) {
            throw BadPackageException("У .rdpkg не знайдено стартову сторінку тому: $start")
        }
        val datasetProjectId = dataset.optString("project_id").trim().takeIf { it.isNotBlank() }
        if (projectId != null && datasetProjectId != null && projectId != datasetProjectId) {
            throw BadPackageException("project_id у rdpkg.json і renault-dataset.json не збігається.")
        }
        progress?.invoke(total, total)
        return Result(packageId = packageId, projectId = projectId, fileCount = files.size, bytes = bytes)
    }

    private fun parse(
        bytes: ByteArray?,
        missing: String,
    ): JSONObject {
        if (bytes == null) throw BadPackageException(missing)
        return try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (error: Exception) {
            throw BadPackageException(missing.replace("немає", "пошкоджений"))
        }
    }

    private class CountingStream(
        input: java.io.InputStream,
    ) : java.io.FilterInputStream(input) {
        var count = 0L
            private set

        override fun read(): Int = super.read().also { if (it >= 0) count++ }

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int = super.read(b, off, len).also { if (it > 0) count += it }

        override fun skip(n: Long): Long = super.skip(n).also { if (it > 0) count += it }
    }
}
