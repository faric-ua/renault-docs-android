package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.Normalizer
import org.json.JSONObject

object RdpkgExporter {
    private const val PACKAGE_MANIFEST =
        "rdpkg.json"
    private const val DATASET_MANIFEST =
        "renault-dataset.json"

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

            val written =
                RdpkgZipWriter
                    .writeDirectory(
                        context =
                            appContext,
                        sourceRoot =
                            sourceRoot,
                        destinationUri =
                            destinationUri,
                        progress = {
                            completed,
                            total ->
                            progress?.invoke(
                                "Експортую .rdpkg… " +
                                    completed +
                                    "/" +
                                    total
                            )
                        },
                    )

            ExportResult(
                packageId =
                    packageId,
                fileCount =
                    written.fileCount,
                sourceBytes =
                    written.sourceBytes,
                sha256 =
                    written.sha256,
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
