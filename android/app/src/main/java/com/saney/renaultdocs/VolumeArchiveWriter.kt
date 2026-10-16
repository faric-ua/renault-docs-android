package com.saney.renaultdocs

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * v0.5.142 (Phone QA M30s 2026-10-17 01:22–01:39): the volume archive is written with every
 * entry STORED — the bytes as they are, no compression at all.
 *
 * Why. On the M30s (Android 11) archives packed by [RdpkgZipWriter] — DEFLATE with the level
 * switched per file (1 for text, 0 for the rest) — failed the check right away, each time with
 * another zlib error («invalid code lengths set», «invalid distance too far back»), for a volume
 * on the card and for one in phone memory alike. The same volumes imported on the M30s from
 * archives made on the A26 unpack fine, so reading is not the problem: what the M30s *writes*
 * with its compressor comes out broken. The same code made a correct archive on the A26
 * (Android 16), and the same steps with upstream zlib 1.2.11 / 1.2.13 on a PC never broke.
 * Storing needs no compressor, so this path no longer depends on it; [VolumeArchiveVerifier]
 * still checks every byte afterwards.
 *
 * Bonus: a stored entry is read by plain copying (no unpacking) — fastest for the old phone.
 * Costs: text files are not compressed (the archive is a little bigger), and each file's CRC
 * must be known before its bytes are written — small files are read once into memory, big ones
 * are read twice.
 *
 * Same file order and names as [RdpkgZipWriter] (sorted by path, files only). Plain Kotlin +
 * java.util.zip — tested on the JVM.
 */
object VolumeArchiveWriter {
    data class Result(
        val fileCount: Int,
        val sourceBytes: Long,
        val sha256: String,
    )

    /** Files up to this size are read once into memory; bigger ones twice (CRC, then copy). */
    const val IN_MEMORY_MAX = 4 * 1024 * 1024

    private const val ZIP_EPOCH_MILLIS = 315_532_800_000L

    fun write(
        sourceRoot: File,
        target: File,
        progress: ((filesDone: Int, filesTotal: Int, bytesDone: Long, bytesTotal: Long) -> Unit)? = null,
    ): Result {
        val root = sourceRoot.canonicalFile
        require(root.isDirectory) { "Package source is not a directory: $root" }
        val files =
            root.walkTopDown()
                .filter { it.isFile }
                .toList()
                .sortedWith(
                    compareBy<File> { relativePath(root, it).lowercase(Locale.ROOT) }
                        .thenBy { relativePath(root, it) },
                )
        require(files.isNotEmpty()) { "Package source directory is empty." }
        val totalBytes = files.sumOf { it.length() }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(256 * 1024)
        var doneBytes = 0L
        progress?.invoke(0, files.size, 0L, totalBytes)
        DigestOutputStream(BufferedOutputStream(target.outputStream(), 1024 * 1024), digest).use { out ->
            ZipOutputStream(out).use { zip ->
                zip.setMethod(ZipOutputStream.STORED)
                files.forEachIndexed { index, file ->
                    val relative = relativePath(root, file)
                    val size = file.length()
                    val crc = CRC32()
                    val inMemory: ByteArray? =
                        if (size <= IN_MEMORY_MAX) {
                            file.readBytes().also { crc.update(it) }
                        } else {
                            BufferedInputStream(file.inputStream(), buffer.size).use { input ->
                                updateCrc(input, crc, buffer)
                            }
                            null
                        }
                    val actualSize = inMemory?.size?.toLong() ?: size
                    zip.putNextEntry(
                        ZipEntry(relative).apply {
                            method = ZipEntry.STORED
                            this.size = actualSize
                            compressedSize = actualSize
                            this.crc = crc.value
                            time = ZIP_EPOCH_MILLIS
                        },
                    )
                    if (inMemory != null) {
                        zip.write(inMemory)
                    } else {
                        // ZipOutputStream checks size and CRC at closeEntry: a file changed
                        // between the two reads fails loudly instead of writing a wrong entry.
                        BufferedInputStream(file.inputStream(), buffer.size).use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                if (read > 0) zip.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                    doneBytes += actualSize
                    progress?.invoke(index + 1, files.size, doneBytes, totalBytes)
                }
            }
        }
        return Result(
            fileCount = files.size,
            sourceBytes = totalBytes,
            sha256 = digest.digest().joinToString("") { "%02x".format(it) },
        )
    }

    private fun updateCrc(
        input: InputStream,
        crc: CRC32,
        buffer: ByteArray,
    ) {
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) crc.update(buffer, 0, read)
        }
    }

    private fun relativePath(
        root: File,
        file: File,
    ): String = file.relativeTo(root).invariantSeparatorsPath
}
