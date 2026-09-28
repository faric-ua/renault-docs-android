package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import org.json.JSONObject

object RdpkgImporter {
    const val FORMAT =
        "renault-volume-package-v1"
    const val SCHEMA_VERSION =
        1
    private const val PACKAGE_MANIFEST =
        "rdpkg.json"
    private const val DATASET_MANIFEST =
        "renault-dataset.json"
    private const val MAX_ENTRY_COUNT =
        250_000

    data class ImportResult(
        val packageId: String,
        val treeUri: Uri,
        val volume: ProjectVolumeRecord,
        val payloadFiles: Int,
        val payloadBytes: Long,
    )

    fun install(
        context: Context,
        packageUri: Uri,
        progress: ((String) -> Unit)? = null,
    ): Result<ImportResult> =
        runCatching {
            val appContext =
                context.applicationContext
            val packagesDirectory =
                LocalDatasetDocumentsProvider
                    .packagesDirectory(
                        appContext,
                    )
                    .apply {
                        mkdirs()
                    }

            val staging =
                File(
                    packagesDirectory,
                    ".import-" +
                        UUID.randomUUID()
                            .toString(),
                )

            staging.deleteRecursively()

            require(
                staging.mkdirs(),
            ) {
                "Не вдалося створити тимчасову папку для імпорту."
            }

            try {
                progress?.invoke(
                    "Імпортую .rdpkg…",
                )

                val extraction =
                    extract(
                        context =
                            appContext,
                        packageUri =
                            packageUri,
                        staging =
                            staging,
                        progress =
                            progress,
                    )

                val packageMetadata =
                    readPackageMetadata(
                        staging,
                    )

                validateDatasetPayload(
                    staging =
                        staging,
                    packageMetadata =
                        packageMetadata,
                )

                val finalDirectory =
                    LocalDatasetDocumentsProvider
                        .packageDirectory(
                            appContext,
                            packageMetadata.packageId,
                        )
                val backupDirectory =
                    File(
                        packagesDirectory,
                        ".backup-" +
                            packageMetadata.packageId,
                    )

                backupDirectory
                    .deleteRecursively()

                if (
                    finalDirectory.exists()
                ) {
                    require(
                        finalDirectory.renameTo(
                            backupDirectory,
                        ),
                    ) {
                        "Не вдалося підготувати оновлення встановленого пакета."
                    }
                }

                var activated =
                    false

                try {
                    require(
                        staging.renameTo(
                            finalDirectory,
                        ),
                    ) {
                        "Не вдалося активувати імпортований пакет."
                    }

                    activated =
                        true

                    val treeUri =
                        LocalDatasetDocumentsProvider
                            .treeUriFor(
                                packageMetadata.packageId,
                            )

                    val volume =
                        PreparedVolumeReader
                            .read(
                                context =
                                    appContext,
                                treeUri =
                                    treeUri,
                            )
                            .getOrThrow()

                    if (
                        packageMetadata.projectId !=
                        null &&
                        volume.projectHint !=
                        null
                    ) {
                        require(
                            packageMetadata.projectId ==
                                volume.projectHint
                        ) {
                            "Project id у rdpkg не збігається з dataset manifest."
                        }
                    }

                    backupDirectory
                        .deleteRecursively()

                    ImportResult(
                        packageId =
                            packageMetadata.packageId,
                        treeUri =
                            treeUri,
                        volume =
                            volume,
                        payloadFiles =
                            extraction.fileCount,
                        payloadBytes =
                            extraction.bytes,
                    )
                } catch (
                    error: Throwable,
                ) {
                    if (
                        activated
                    ) {
                        finalDirectory
                            .deleteRecursively()
                    }

                    if (
                        backupDirectory.exists()
                    ) {
                        backupDirectory.renameTo(
                            finalDirectory,
                        )
                    }

                    throw error
                }
            } finally {
                staging.deleteRecursively()
            }
        }

    private fun extract(
        context: Context,
        packageUri: Uri,
        staging: File,
        progress: ((String) -> Unit)?,
    ): ExtractionResult {
        val input =
            context.contentResolver
                .openInputStream(
                    packageUri,
                )
                ?: error(
                    "Android не зміг відкрити вибраний .rdpkg."
                )

        var fileCount =
            0
        var bytes =
            0L

        ZipInputStream(
            input.buffered(),
        ).use {
            archive ->
            while (
                true
            ) {
                val entry =
                    archive.nextEntry
                        ?: break

                val normalized =
                    normalizedEntryPath(
                        entry.name,
                    )

                if (
                    normalized.isBlank()
                ) {
                    archive.closeEntry()
                    continue
                }

                val destination =
                    File(
                        staging,
                        normalized,
                    ).canonicalFile

                val stagingPath =
                    staging.canonicalPath
                val destinationPath =
                    destination.canonicalPath

                require(
                    destinationPath.startsWith(
                        stagingPath +
                            File.separator,
                    ),
                ) {
                    "RDPKG містить небезпечний шлях: " +
                        entry.name
                }

                if (
                    entry.isDirectory
                ) {
                    require(
                        destination.mkdirs() ||
                            destination.isDirectory,
                    ) {
                        "Не вдалося створити папку: " +
                            normalized
                    }
                    archive.closeEntry()
                    continue
                }

                fileCount +=
                    1

                require(
                    fileCount <=
                        MAX_ENTRY_COUNT,
                ) {
                    "RDPKG містить забагато файлів."
                }

                destination.parentFile
                    ?.let {
                        parent ->
                        require(
                            parent.mkdirs() ||
                                parent.isDirectory,
                        ) {
                            "Не вдалося створити папку для: " +
                                normalized
                        }
                    }

                FileOutputStream(
                    destination,
                ).use {
                    output ->
                    val buffer =
                        ByteArray(
                            DEFAULT_BUFFER_SIZE,
                        )

                    while (
                        true
                    ) {
                        val read =
                            archive.read(
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

                        output.write(
                            buffer,
                            0,
                            read,
                        )
                        bytes +=
                            read
                    }
                }

                if (
                    fileCount ==
                    1 ||
                    fileCount %
                    500 ==
                    0
                ) {
                    progress?.invoke(
                        "Імпортую .rdpkg… " +
                            fileCount +
                            " файлів",
                    )
                }

                archive.closeEntry()
            }
        }

        require(
            fileCount >
                0,
        ) {
            "RDPKG порожній."
        }

        return ExtractionResult(
            fileCount =
                fileCount,
            bytes =
                bytes,
        )
    }

    private fun readPackageMetadata(
        staging: File,
    ): PackageMetadata {
        val manifest =
            File(
                staging,
                PACKAGE_MANIFEST,
            )

        require(
            manifest.isFile,
        ) {
            "У .rdpkg немає rdpkg.json."
        }

        val json =
            JSONObject(
                manifest.readText(
                    Charsets.UTF_8,
                ),
            )

        require(
            json.optInt(
                "schema_version",
                0,
            ) ==
                SCHEMA_VERSION,
        ) {
            "Непідтримувана версія rdpkg schema."
        }

        require(
            json.optString(
                "format",
            ) ==
                FORMAT,
        ) {
            "Це не Renault Docs .rdpkg."
        }

        val packageId =
            json.optString(
                "package_id",
            )
                .trim()

        require(
            packageId.matches(
                Regex(
                    "[A-Za-z0-9._-]{1,160}",
                ),
            ),
        ) {
            "RDPKG має некоректний package_id."
        }

        return PackageMetadata(
            packageId =
                packageId,
            projectId =
                json.optString(
                    "project_id",
                )
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    },
        )
    }

    private fun validateDatasetPayload(
        staging: File,
        packageMetadata: PackageMetadata,
    ) {
        val manifest =
            File(
                staging,
                DATASET_MANIFEST,
            )

        require(
            manifest.isFile,
        ) {
            "У .rdpkg немає renault-dataset.json."
        }

        val json =
            JSONObject(
                manifest.readText(
                    Charsets.UTF_8,
                ),
            )

        val volumes =
            json.optJSONArray(
                "volumes",
            )
                ?: error(
                    "Dataset manifest у .rdpkg не містить volumes."
                )

        require(
            volumes.length() ==
                1,
        ) {
            "RDPKG має містити рівно один том, знайдено: " +
                volumes.length()
        }

        val entrypoint =
            volumes
                .getJSONObject(
                    0,
                )
                .optString(
                    "entrypoint",
                )
                .trim()

        require(
            entrypoint.isNotBlank(),
        ) {
            "Том у .rdpkg не містить entrypoint."
        }

        val normalized =
            SafDatasetResolver
                .normalize(
                    entrypoint,
                )
                ?: error(
                    "Том у .rdpkg має некоректний entrypoint."
                )

        val entrypointFile =
            File(
                staging,
                normalized,
            )
                .canonicalFile
        val stagingPath =
            staging.canonicalPath

        require(
            entrypointFile.canonicalPath
                .startsWith(
                    stagingPath +
                        File.separator,
                ) &&
                entrypointFile.isFile,
        ) {
            "У .rdpkg не знайдено стартову сторінку тому: " +
                normalized
        }

        val datasetProjectId =
            json.optString(
                "project_id",
            )
                .trim()
                .takeIf {
                    it.isNotBlank()
                }

        if (
            packageMetadata.projectId !=
            null &&
            datasetProjectId !=
            null
        ) {
            require(
                packageMetadata.projectId ==
                    datasetProjectId,
            ) {
                "project_id у rdpkg.json і renault-dataset.json не збігається."
            }
        }
    }

    private fun normalizedEntryPath(
        value: String,
    ): String {
        val normalized =
            value.replace(
                '\\',
                '/',
            )
                .trimStart(
                    '/',
                )

        val parts =
            normalized.split(
                '/',
            )
                .filter {
                    it.isNotBlank() &&
                        it !=
                        "."
                }

        require(
            parts.none {
                it ==
                    ".."
            },
        ) {
            "RDPKG містить небезпечний шлях."
        }

        return parts.joinToString(
            "/",
        )
    }

    private data class PackageMetadata(
        val packageId: String,
        val projectId: String?,
    )

    private data class ExtractionResult(
        val fileCount: Int,
        val bytes: Long,
    )
}
