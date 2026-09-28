package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

object RdpkgExporter {
    private const val PACKAGE_MANIFEST =
        "rdpkg.json"
    private const val DATASET_MANIFEST =
        "renault-dataset.json"
    private const val BUFFER_SIZE =
        1024 * 1024
    private const val ZIP_EPOCH_MILLIS =
        315_532_800_000L

    data class ExportResult(
        val packageId: String,
        val fileCount: Int,
        val sourceBytes: Long,
        val sha256: String,
    )

    fun canFastExport(
        volume: ProjectVolumeRecord,
    ): Boolean =
        LocalDatasetDocumentsProvider
            .packageIdFromTreeUri(
                Uri.parse(
                    volume.treeUri,
                ),
            ) !=
            null

    fun defaultFileName(
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): String {
        val parts =
            buildList {
                add(
                    safeFilePart(
                        project.model,
                    ),
                )
                add(
                    safeFilePart(
                        volume.documentCode
                            ?: volume.id,
                    ),
                )

                volume.date
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            safeFilePart(
                                it,
                            ),
                        )
                    }
            }

        return parts.joinToString(
            "_",
        ) +
            ".rdpkg"
    }

    fun export(
        context: Context,
        volume: ProjectVolumeRecord,
        destinationUri: Uri,
        progress: ((String) -> Unit)? = null,
    ): Result<ExportResult> =
        runCatching {
            val appContext =
                context.applicationContext
            val packageId =
                LocalDatasetDocumentsProvider
                    .packageIdFromTreeUri(
                        Uri.parse(
                            volume.treeUri,
                        ),
                    )
                    ?: error(
                        "Швидкий експорт доступний лише для тому, встановленого з .rdpkg."
                    )

            val sourceRoot =
                LocalDatasetDocumentsProvider
                    .packageDirectory(
                        appContext,
                        packageId,
                    )
                    .canonicalFile

            require(
                sourceRoot.isDirectory,
            ) {
                "Встановлений пакет не знайдено: " +
                    packageId
            }

            validateManagedPackage(
                sourceRoot =
                    sourceRoot,
                packageId =
                    packageId,
            )

            val files =
                sourceRoot
                    .walkTopDown()
                    .filter {
                        it.isFile
                    }
                    .toList()
                    .sortedBy {
                        it.relativeTo(
                            sourceRoot,
                        )
                            .invariantSeparatorsPath
                            .lowercase(
                                Locale.ROOT,
                            )
                    }

            require(
                files.isNotEmpty(),
            ) {
                "Встановлений пакет порожній."
            }

            val sourceBytes =
                files.sumOf {
                    it.length()
                }
            val digest =
                MessageDigest.getInstance(
                    "SHA-256",
                )

            progress?.invoke(
                "Експортую .rdpkg… 0/" +
                    files.size
            )

            val rawOutput =
                appContext
                    .contentResolver
                    .openOutputStream(
                        destinationUri,
                        "w",
                    )
                    ?: error(
                        "Android не зміг відкрити файл для запису."
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
                    archive.setLevel(
                        6,
                    )

                    val buffer =
                        ByteArray(
                            BUFFER_SIZE,
                        )

                    files.forEachIndexed {
                        index,
                        source ->
                        val relative =
                            source.relativeTo(
                                sourceRoot,
                            )
                                .invariantSeparatorsPath

                        val entry =
                            ZipEntry(
                                relative,
                            ).apply {
                                time =
                                    ZIP_EPOCH_MILLIS
                            }

                        archive.putNextEntry(
                            entry,
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
                                500 ==
                            0 ||
                            completed ==
                            files.size
                        ) {
                            progress?.invoke(
                                "Експортую .rdpkg… " +
                                    completed +
                                    "/" +
                                    files.size
                            )
                        }
                    }
                }
            }

            ExportResult(
                packageId =
                    packageId,
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

    private fun validateManagedPackage(
        sourceRoot: File,
        packageId: String,
    ) {
        val packageManifest =
            File(
                sourceRoot,
                PACKAGE_MANIFEST,
            )

        require(
            packageManifest.isFile,
        ) {
            "У встановленому пакеті немає rdpkg.json."
        }

        val packageJson =
            JSONObject(
                packageManifest.readText(
                    Charsets.UTF_8,
                ),
            )

        require(
            packageJson.optInt(
                "schema_version",
                0,
            ) ==
                RdpkgImporter.SCHEMA_VERSION,
        ) {
            "Непідтримувана версія rdpkg schema."
        }

        require(
            packageJson.optString(
                "format",
            ) ==
                RdpkgImporter.FORMAT,
        ) {
            "Встановлений пакет має невідомий формат."
        }

        require(
            packageJson.optString(
                "package_id",
            )
                .trim() ==
                packageId,
        ) {
            "package_id встановленого пакета не збігається з його storage id."
        }

        val datasetManifest =
            File(
                sourceRoot,
                DATASET_MANIFEST,
            )

        require(
            datasetManifest.isFile,
        ) {
            "У встановленому пакеті немає renault-dataset.json."
        }

        val datasetJson =
            JSONObject(
                datasetManifest.readText(
                    Charsets.UTF_8,
                ),
            )
        val volumes =
            datasetJson.optJSONArray(
                "volumes",
            )
                ?: error(
                    "Dataset manifest не містить volumes."
                )

        require(
            volumes.length() ==
                1,
        ) {
            "Швидкий .rdpkg export очікує рівно один том."
        }
    }

    private fun safeFilePart(
        value: String,
    ): String {
        val normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD,
            )
        val ascii =
            normalized
                .replace(
                    Regex(
                        "\\p{M}+",
                    ),
                    "",
                )
        val clean =
            ascii
                .replace(
                    Regex(
                        "[^A-Za-z0-9._-]+",
                    ),
                    "-",
                )
                .trim(
                    '-',
                    '.',
                    '_',
                )

        return clean.ifBlank {
            "Renault"
        }
    }
}
