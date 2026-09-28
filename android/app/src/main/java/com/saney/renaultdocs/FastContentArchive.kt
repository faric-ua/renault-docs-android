package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.json.JSONObject

class FastContentArchive private constructor(
    private val zipFile: ZipFile,
    val descriptor: Descriptor,
) : AutoCloseable {
    data class Descriptor(
        val path: String,
        val sha256: String,
        val bytes: Long,
        val fileCount: Int,
    )

    data class PrepareResult(
        val archive: FastContentArchive?,
        val copiedToLocalCache: Boolean,
    )

    fun open(
        relativePath: String,
    ): InputStream? {
        val normalized =
            SafDatasetResolver.normalize(
                relativePath,
            )
                ?: return null

        val entry =
            zipFile.getEntry(
                normalized,
            )
                ?: return null

        if (entry.isDirectory) {
            return null
        }

        return zipFile.getInputStream(
            entry,
        )
    }

    override fun close() {
        zipFile.close()
    }

    companion object {
        private val preparationLock =
            Any()

        fun prepare(
            context: Context,
            treeUri: Uri,
        ): Result<PrepareResult> =
            runCatching {
                val resolver =
                    SafDatasetResolver(
                        context = context,
                        treeUri = treeUri,
                    )

                val descriptor =
                    readDescriptor(
                        resolver,
                    )
                        ?: return@runCatching PrepareResult(
                            archive = null,
                            copiedToLocalCache = false,
                        )

                synchronized(
                    preparationLock,
                ) {
                    val directory =
                        File(
                            context.cacheDir,
                            "renault-fast",
                        ).apply {
                            mkdirs()
                        }

                    val localFile =
                        File(
                            directory,
                            descriptor.sha256 +
                                ".zip",
                        )

                    var copied = false

                    if (
                        !localFile.isFile ||
                        localFile.length() !=
                        descriptor.bytes
                    ) {
                        copyAndVerify(
                            resolver = resolver,
                            descriptor =
                                descriptor,
                            destination =
                                localFile,
                        )
                        copied = true
                    }

                    PrepareResult(
                        archive =
                            FastContentArchive(
                                zipFile =
                                    ZipFile(
                                        localFile,
                                    ),
                                descriptor =
                                    descriptor,
                            ),
                        copiedToLocalCache =
                            copied,
                    )
                }
            }

        private fun readDescriptor(
            resolver: SafDatasetResolver,
        ): Descriptor? {
            val manifestText =
                resolver
                    .openInputStream(
                        "renault-dataset.json",
                    )
                    ?.bufferedReader(
                        Charsets.UTF_8,
                    )
                    ?.use {
                        it.readText()
                    }
                    ?: return null

            val fast =
                JSONObject(
                    manifestText,
                ).optJSONObject(
                    "fast_pack",
                )
                    ?: return null

            if (
                fast.optString(
                    "format",
                ) != "zip-web-v1"
            ) {
                return null
            }

            val path =
                fast.optString(
                    "path",
                ).trim()
            val sha256 =
                fast.optString(
                    "sha256",
                ).trim()
                    .lowercase()
            val bytes =
                fast.optLong(
                    "bytes",
                    -1L,
                )
            val fileCount =
                fast.optInt(
                    "file_count",
                    0,
                )

            if (
                path.isBlank() ||
                sha256.length != 64 ||
                bytes <= 0L
            ) {
                return null
            }

            return Descriptor(
                path = path,
                sha256 = sha256,
                bytes = bytes,
                fileCount = fileCount,
            )
        }

        private fun copyAndVerify(
            resolver: SafDatasetResolver,
            descriptor: Descriptor,
            destination: File,
        ) {
            val input =
                resolver.openInputStream(
                    descriptor.path,
                )
                    ?: error(
                        "Fast Pack не знайдено: " +
                            descriptor.path,
                    )

            val temp =
                File(
                    destination.parentFile,
                    destination.name +
                        ".tmp",
                )

            temp.delete()

            val digest =
                MessageDigest.getInstance(
                    "SHA-256",
                )

            try {
                input.use { source ->
                    FileOutputStream(
                        temp,
                    ).use { output ->
                        val buffer =
                            ByteArray(
                                DEFAULT_BUFFER_SIZE,
                            )

                        while (true) {
                            val read =
                                source.read(
                                    buffer,
                                )

                            if (read < 0) {
                                break
                            }

                            if (read == 0) {
                                continue
                            }

                            output.write(
                                buffer,
                                0,
                                read,
                            )
                            digest.update(
                                buffer,
                                0,
                                read,
                            )
                        }
                    }
                }

                require(
                    temp.length() ==
                        descriptor.bytes
                ) {
                    "Fast Pack має неправильний розмір."
                }

                val actualSha =
                    digest.digest()
                        .joinToString(
                            separator = "",
                        ) { byte ->
                            (
                                byte.toInt() and
                                    0xff
                            )
                                .toString(16)
                                .padStart(
                                    2,
                                    '0',
                                )
                        }

                require(
                    actualSha ==
                        descriptor.sha256
                ) {
                    "Fast Pack SHA-256 не збігається."
                }

                destination.delete()

                require(
                    temp.renameTo(
                        destination,
                    )
                ) {
                    "Не вдалося активувати локальний Fast Pack."
                }
            } finally {
                temp.delete()
            }
        }
    }
}
