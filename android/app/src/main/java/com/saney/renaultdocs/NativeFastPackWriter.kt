package com.saney.renaultdocs

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Kotlin/JVM counterpart of core/fast_pack.py for the future raw-folder
 * preparation pipeline.
 *
 * The ZIP digest is calculated while the archive is written. There is no
 * second byte pass just to compute SHA-256.
 */
object NativeFastPackWriter {
    const val SCHEMA_VERSION =
        1

    const val FORMAT =
        "zip-web-v1"

    private const val PACKAGE_DIR =
        "_renault"

    private const val FAST_PACK_PREFIX =
        "fast-content-"

    private const val FAST_PACK_SUFFIX =
        ".zip"

    private const val TEMP_NAME =
        ".fast-content-building.zip"

    private const val BUFFER_SIZE =
        1024 * 1024

    private const val ZIP_EPOCH_MILLIS =
        315_532_800_000L

    private val extensions =
        setOf(
            "htm",
            "html",
            "js",
            "css",
            "gif",
            "ico",
            "png",
            "jpg",
            "jpeg",
            "svg",
            "json",
        )

    data class Result(
        val relativePath: String,
        val sha256: String,
        val bytes: Long,
        val fileCount: Int,
    )

    fun build(
        outputRoot: File,
        packageRoot: File =
            File(
                outputRoot,
                PACKAGE_DIR,
            ),
        progress: ((String) -> Unit)? = null,
    ): Result {
        val root =
            outputRoot.canonicalFile
        val metadataRoot =
            packageRoot.canonicalFile

        require(
            root.isDirectory,
        ) {
            "Fast Pack root is not a directory: " +
                root
        }

        require(
            metadataRoot.toPath().startsWith(
                root.toPath(),
            )
        ) {
            "Fast Pack package root must stay inside the dataset."
        }

        metadataRoot.mkdirs()

        progress?.invoke(
            "Fast Pack: сканую dataset…"
        )

        val sources =
            root
                .walkTopDown()
                .filter {
                    it.isFile
                }
                .mapNotNull {
                    file ->
                    val relative =
                        file.relativeTo(
                            root,
                        )
                            .invariantSeparatorsPath

                    if (
                        shouldPack(
                            relative,
                        )
                    ) {
                        file
                    } else {
                        null
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<File> {
                        it.relativeTo(
                            root,
                        )
                            .invariantSeparatorsPath
                            .lowercase(
                                Locale.ROOT,
                            )
                    }.thenBy {
                        it.relativeTo(
                            root,
                        )
                            .invariantSeparatorsPath
                    }
                )

        progress?.invoke(
            "Fast Pack: знайдено " +
                sources.size +
                " web-файлів."
        )

        val temp =
            File(
                metadataRoot,
                TEMP_NAME,
            )

        if (
            temp.exists()
        ) {
            require(
                temp.delete(),
            ) {
                "Не вдалося видалити старий Fast Pack staging."
            }
        }

        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        try {
            DigestOutputStream(
                BufferedOutputStream(
                    temp.outputStream(),
                    BUFFER_SIZE,
                ),
                digest,
            ).use {
                digestOutput ->
                ZipOutputStream(
                    digestOutput,
                ).use {
                    archive ->
                    archive.setLevel(
                        Deflater.BEST_SPEED,
                    )

                    val buffer =
                        ByteArray(
                            BUFFER_SIZE,
                        )

                    sources.forEachIndexed {
                        index,
                        source ->
                        val relative =
                            source.relativeTo(
                                root,
                            )
                                .invariantSeparatorsPath

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

                        if (
                            completed ==
                            1 ||
                            completed %
                                1000 ==
                            0 ||
                            completed ==
                            sources.size
                        ) {
                            progress?.invoke(
                                "Fast Pack: пакую " +
                                    completed +
                                    "/" +
                                    sources.size +
                                    "…"
                            )
                        }
                    }
                }
            }

            val sha256 =
                digest.digest()
                    .joinToString(
                        "",
                    ) {
                        byte ->
                        "%02x".format(
                            byte.toInt() and
                                0xff,
                        )
                    }

            val finalName =
                FAST_PACK_PREFIX +
                    sha256.take(
                        16,
                    ) +
                    FAST_PACK_SUFFIX

            val finalFile =
                File(
                    metadataRoot,
                    finalName,
                )

            if (
                finalFile.exists()
            ) {
                require(
                    finalFile.delete(),
                ) {
                    "Не вдалося замінити існуючий Fast Pack."
                }
            }

            require(
                temp.renameTo(
                    finalFile,
                )
            ) {
                "Не вдалося зафіксувати Fast Pack."
            }

            metadataRoot
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile &&
                        it.name.startsWith(
                            FAST_PACK_PREFIX,
                        ) &&
                        it.name.endsWith(
                            FAST_PACK_SUFFIX,
                        ) &&
                        it !=
                        finalFile
                }
                .forEach {
                    stale ->
                    stale.delete()
                }

            progress?.invoke(
                "Fast Pack: готово — " +
                    sources.size +
                    " файлів, " +
                    finalFile.length() +
                    " bytes."
            )

            return Result(
                relativePath =
                    finalFile.relativeTo(
                        root,
                    )
                        .invariantSeparatorsPath,
                sha256 =
                    sha256,
                bytes =
                    finalFile.length(),
                fileCount =
                    sources.size,
            )
        } finally {
            if (
                temp.exists()
            ) {
                temp.delete()
            }
        }
    }

    fun shouldPack(
        relativePath: String,
    ): Boolean {
        val normalized =
            relativePath
                .replace(
                    '\\',
                    '/',
                )
                .trimStart(
                    '/',
                )

        val name =
            normalized
                .substringAfterLast(
                    '/',
                )

        val extension =
            name
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                )

        if (
            extension !in
            extensions
        ) {
            return false
        }

        if (
            name.startsWith(
                FAST_PACK_PREFIX,
            )
        ) {
            return false
        }

        if (
            normalized.equals(
                "renault-dataset.json",
                ignoreCase =
                    true,
            )
        ) {
            return false
        }

        if (
            normalized.startsWith(
                PACKAGE_DIR +
                    "/",
                ignoreCase =
                    true,
            ) &&
            extension ==
            "json"
        ) {
            return false
        }

        return true
    }
}
