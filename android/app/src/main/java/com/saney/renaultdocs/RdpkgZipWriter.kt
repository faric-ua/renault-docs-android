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
 */
object RdpkgZipWriter {
    private const val BUFFER_SIZE =
        1024 * 1024

    private const val ZIP_EPOCH_MILLIS =
        315_532_800_000L

    private const val PROGRESS_THROTTLE_MS =
        200L

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
    ): WriteResult {
        val root =
            sourceRoot.canonicalFile

        require(
            root.isDirectory,
        ) {
            "Package source is not a directory: " +
                root
        }

        val files =
            root
                .walkTopDown()
                .filter {
                    it.isFile
                }
                .toList()
                .sortedWith(
                    compareBy<File> {
                        relativePath(
                            root,
                            it,
                        ).lowercase(
                            Locale.ROOT,
                        )
                    }.thenBy {
                        relativePath(
                            root,
                            it,
                        )
                    }
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

        DigestOutputStream(
            rawOutput.buffered(),
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
                        lastProgressAt =
                            now
                    }
                }
            }
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
