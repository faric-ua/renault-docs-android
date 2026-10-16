package com.saney.renaultdocs

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

/**
 * Archives plan v2, stage 1: before a volume is read from its archive, the archive is
 * checked against the unpacked folder — every file of the folder must be in the archive with
 * the same size and the same bytes (CRC-32), and the archive must hold nothing else.
 *
 * The archive is read once from start to end with [ZipInputStream], which itself also checks
 * each entry's stored size and CRC against the data — so a damaged archive fails too.
 * Nothing is written, moved or deleted here.
 *
 * Plain Kotlin + java.util.zip — tested on the JVM.
 */
object VolumeArchiveVerifier {
    data class Result(
        val ok: Boolean,
        val fileCount: Int,
        val bytes: Long,
        /** Human words for the owner when [ok] is false. */
        val problem: String? = null,
    )

    private class Expected(
        val file: File,
        val size: Long,
    )

    fun verify(
        folder: File,
        archive: File,
        progress: ((done: Long, total: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false },
    ): Result {
        val root = folder.canonicalFile
        val expected = HashMap<String, Expected>()
        root.walkTopDown()
            .onEnter { !java.nio.file.Files.isSymbolicLink(it.toPath()) }
            .filter { it.isFile }
            .forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                expected[relative] = Expected(file, file.length())
            }
        val totalBytes = expected.values.sumOf { it.size } * 2
        var doneBytes = 0L
        val seen = HashSet<String>()
        val buffer = ByteArray(256 * 1024)

        fun crcOf(input: InputStream, counted: Boolean): Pair<Long, Long> {
            val crc = CRC32()
            var size = 0L
            while (true) {
                if (isCancelled()) throw OperationCancelledException()
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                crc.update(buffer, 0, read)
                size += read
                if (counted) {
                    doneBytes += read
                    progress?.invoke(doneBytes, totalBytes)
                }
            }
            return crc.value to size
        }

        ZipInputStream(BufferedInputStream(archive.inputStream(), 1024 * 1024)).use { zip ->
            while (true) {
                if (isCancelled()) throw OperationCancelledException()
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val path = VolumeArchive.cleanEntryName(entry.name)
                if (path.isNullOrEmpty()) {
                    return Result(false, seen.size, doneBytes, "В архіві є небезпечний шлях: ${entry.name}")
                }
                val want = expected[path]
                    ?: return Result(false, seen.size, doneBytes, "В архіві є зайвий файл: $path")
                if (!seen.add(path)) {
                    return Result(false, seen.size, doneBytes, "В архіві файл двічі: $path")
                }
                val (archiveCrc, archiveSize) =
                    try {
                        crcOf(zip, counted = true)
                    } catch (error: java.util.zip.ZipException) {
                        // Which file and what zlib said — for the owner's card and the journal.
                        return Result(false, seen.size, doneBytes, "Архів пошкоджений у файлі $path: ${error.message}")
                    }
                if (archiveSize != want.size) {
                    return Result(false, seen.size, doneBytes, "Розмір не збігся: $path")
                }
                val (folderCrc, folderSize) = want.file.inputStream().use { crcOf(it, counted = true) }
                if (folderSize != want.size || folderCrc != archiveCrc) {
                    return Result(false, seen.size, doneBytes, "Вміст не збігся: $path")
                }
            }
        }
        val missing = expected.keys - seen
        if (missing.isNotEmpty()) {
            return Result(false, seen.size, doneBytes, "В архіві бракує ${missing.size} файл(ів), напр. ${missing.sorted().first()}")
        }
        if (seen.isEmpty()) return Result(false, 0, 0L, "Том порожній.")
        return Result(true, seen.size, expected.values.sumOf { it.size })
    }
}
