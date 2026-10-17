package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import java.io.BufferedInputStream
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Shared streaming writer for the outer .rdpkg archive.
 *
 * The prepared package directory is treated as immutable input. The writer
 * performs one ordered pass over it, computes SHA-256 while bytes are written,
 * and chooses a speed-oriented DEFLATE level per entry.
 *
 * TODO №19 (2026-10-17): every written byte is also unpacked by [ZipStreamCheck]; a ZIP that
 * would not open (the M30s compressor, v0.5.141 history) fails the write with
 * [DamagedOutputException] instead of reaching another phone.
 */
object RdpkgZipWriter {
    private const val BUFFER_SIZE =
        1024 * 1024

    private const val ZIP_EPOCH_MILLIS =
        315_532_800_000L

    private const val PROGRESS_THROTTLE_MS =
        100L

    private val textExtensions =
        setOf(
            "htm",
            "html",
            "js",
            "css",
            "json",
            "txt",
            "xml",
            "svg",
            "csv",
        )

    data class WriteResult(
        val fileCount: Int,
        val sourceBytes: Long,
        val sha256: String,
    )

    fun writeDirectory(
        context: Context,
        sourceRoot: File,
        destinationUri: Uri,
        progress: ((completed: Int, total: Int) -> Unit)? = null,
        byteProgress: ((
            completedFiles: Int,
            totalFiles: Int,
            processedBytes: Long,
            totalBytes: Long,
        ) -> Unit)? = null,
        // v0.5.149 (TODO №21 п. 2): files found so far while the folder is listed.
        listing: ((found: Int) -> Unit)? = null,
    ): WriteResult {
        val root =
            sourceRoot.canonicalFile

        require(
            root.isDirectory,
        ) {
            "Package source is not a directory: " +
                root
        }

        // Same order as before (path, lower case first) — one listing shared with the
        // STORED path, which used to list the whole folder a second time (v0.5.145–148).
        val files =
            VolumeArchiveWriter.listFiles(
                root,
                listing,
            )

        require(
            files.isNotEmpty(),
        ) {
            "Package source directory is empty."
        }

        val sourceBytes =
            files.sumOf {
                it.length()
            }

        // v0.5.145: on a phone whose compressor breaks (M30s, Android 11) — no compressor.
        if (ZipCompressionPolicy.storedOnly(context)) {
            return writeStored(
                context = context,
                root = root,
                files = files,
                destinationUri = destinationUri,
                progress = progress,
                byteProgress = byteProgress,
            )
        }

        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        val rawOutput =
            context
                .applicationContext
                .contentResolver
                .openOutputStream(
                    destinationUri,
                    "w",
                )
                ?: error(
                    "Android could not open the .rdpkg destination for writing."
                )

        progress?.invoke(
            0,
            files.size,
        )
        byteProgress?.invoke(
            0,
            files.size,
            0L,
            sourceBytes,
        )

        // TODO №19 (2026-10-17): the written bytes are unpacked again while they are
        // written ([ZipStreamCheck]) — on the M30s the compressor sometimes made a broken ZIP.
        val check =
            ZipStreamCheck()

        try {
            DigestOutputStream(
                TeeOutputStream(
                    rawOutput.buffered(),
                    check.output,
                ),
                digest,
            ).use {
                digestOutput ->
                ZipOutputStream(
                    digestOutput,
                ).use {
                    archive ->
                    val buffer =
                        ByteArray(
                            BUFFER_SIZE,
                        )
                    var lastProgressAt =
                        0L
                    var processedBytes =
                        0L

                    files.forEachIndexed {
                        index,
                        source ->
                        val relative =
                            relativePath(
                                root,
                                source,
                            )

                        archive.setLevel(
                            compressionLevelFor(
                                relativePath =
                                    relative,
                                size =
                                    source.length(),
                            ),
                        )

                        archive.putNextEntry(
                            ZipEntry(
                                relative,
                            ).apply {
                                time =
                                    ZIP_EPOCH_MILLIS
                            },
                        )

                        BufferedInputStream(
                            source.inputStream(),
                            BUFFER_SIZE,
                        ).use {
                            input ->
                            while (
                                true
                            ) {
                                val read =
                                    input.read(
                                        buffer,
                                    )

                                if (
                                    read <
                                    0
                                ) {
                                    break
                                }

                                if (
                                    read ==
                                    0
                                ) {
                                    continue
                                }

                                archive.write(
                                    buffer,
                                    0,
                                    read,
                                )

                                processedBytes +=
                                    read

                                val byteNow =
                                    SystemClock.elapsedRealtime()

                                if (
                                    byteNow -
                                        lastProgressAt >=
                                        PROGRESS_THROTTLE_MS
                                ) {
                                    byteProgress?.invoke(
                                        index,
                                        files.size,
                                        processedBytes,
                                        sourceBytes,
                                    )
                                    lastProgressAt =
                                        byteNow
                                }
                            }
                        }

                        archive.closeEntry()

                        val completed =
                            index +
                                1

                        val now =
                            SystemClock.elapsedRealtime()

                        if (
                            completed ==
                            1 ||
                            completed ==
                            files.size ||
                            now -
                                lastProgressAt >=
                                PROGRESS_THROTTLE_MS
                        ) {
                            progress?.invoke(
                                completed,
                                files.size,
                            )
                            byteProgress?.invoke(
                                completed,
                                files.size,
                                processedBytes,
                                sourceBytes,
                            )
                            lastProgressAt =
                                now
                        }
                    }
                }
            }

            try {
                check.finish(
                    expectedFiles =
                        files.size,
                )
            } catch (damaged: DamagedOutputException) {
                // v0.5.145: this phone's compressor broke a file — from now on it writes
                // without one ([ZipCompressionPolicy]); the owner repeats the operation.
                ZipCompressionPolicy.rememberBroken(context)
                throw damaged
            }
        } finally {
            check.close()
        }

        return WriteResult(
            fileCount =
                files.size,
            sourceBytes =
                sourceBytes,
            sha256 =
                digest.digest()
                    .joinToString(
                        "",
                    ) {
                        byte ->
                        "%02x".format(
                            byte.toInt() and
                                0xff,
                        )
                    },
        )
    }

    /**
     * v0.5.145: the package with every entry STORED ([VolumeArchiveWriter] — same order and
     * names), still checked while written ([ZipStreamCheck]). Progress at most every 100 ms.
     */
    private fun writeStored(
        context: Context,
        root: File,
        files: List<File>,
        destinationUri: Uri,
        progress: ((completed: Int, total: Int) -> Unit)?,
        byteProgress: ((Int, Int, Long, Long) -> Unit)?,
    ): WriteResult {
        val rawOutput =
            context.applicationContext.contentResolver.openOutputStream(destinationUri, "w")
                ?: error("Android could not open the .rdpkg destination for writing.")
        val check = ZipStreamCheck()
        try {
            var lastProgressAt = 0L
            val written =
                VolumeArchiveWriter.writeTo(
                    sourceRoot = root,
                    output = TeeOutputStream(rawOutput, check.output),
                    files = files,
                    progress = { filesDone, filesTotal, bytesDone, bytesTotal ->
                        val now = SystemClock.elapsedRealtime()
                        if (filesDone <= 1 || filesDone >= filesTotal || now - lastProgressAt >= PROGRESS_THROTTLE_MS) {
                            lastProgressAt = now
                            progress?.invoke(filesDone, filesTotal)
                            byteProgress?.invoke(filesDone, filesTotal, bytesDone, bytesTotal)
                        }
                    },
                )
            check.finish(expectedFiles = written.fileCount)
            return WriteResult(
                fileCount = written.fileCount,
                sourceBytes = written.sourceBytes,
                sha256 = written.sha256,
            )
        } finally {
            check.close()
        }
    }

    /**
     * Text/web metadata gets cheap level-1 compression. Everything else uses
     * DEFLATE level 0 so already-compressed PDFs/images/archives are streamed
     * without spending CPU on recompression.
     *
     * ZipOutputStream still emits standard DEFLATED entries at level 0, so we
     * do not need a separate CRC/size pre-pass required by STORED entries.
     */
    fun compressionLevelFor(
        relativePath: String,
        size: Long,
    ): Int {
        val extension =
            relativePath
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                )

        if (
            extension in
            textExtensions
        ) {
            return Deflater.BEST_SPEED
        }

        return Deflater.NO_COMPRESSION
    }

    private fun relativePath(
        root: File,
        file: File,
    ): String =
        file.relativeTo(
            root,
        )
            .invariantSeparatorsPath
}
