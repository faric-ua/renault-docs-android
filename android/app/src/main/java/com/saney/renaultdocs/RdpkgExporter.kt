package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.File
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

    fun resolvedMetadata(
        volume: ProjectVolumeRecord,
    ): RenaultVolumeIdentityMetadata {
        val inferred =
            RenaultVolumeIdentity.parse(
                volume.entrypoint,
                volume.openEntrypoint,
                volume.title,
                volume.datasetTitle,
                volume.platform,
            )

        return RenaultVolumeIdentityMetadata(
            documentCode =
                volume.documentCode
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: inferred.documentCode,
            date =
                volume.date
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: inferred.date,
            vehicleCodes =
                volume.vehicleCodes
                    .takeIf {
                        it.isNotEmpty()
                    }
                    ?: inferred.vehicleCodes,
            documentType =
                volume.documentType
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: inferred.documentType,
            documentVersion =
                volume.documentVersion
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: inferred.documentVersion,
            region =
                volume.region
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: inferred.region,
        )
    }

    fun defaultFileName(
        project: RenaultProject,
        volume: ProjectVolumeRecord,
    ): String =
        RenaultVolumeIdentity
            .canonicalFileName(
                model =
                    project.model,
                metadata =
                    resolvedMetadata(
                        volume,
                    ),
                fallbackId =
                    volume.id,
            )

    fun export(
        context: Context,
        volume: ProjectVolumeRecord,
        destinationUri: Uri,
        progress: ((String) -> Unit)? = null,
        progressState: ((OperationProgress) -> Unit)? = null,
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

            progressState?.invoke(
                OperationProgress.indeterminate(
                    "Перевіряю…",
                ),
            )

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
                        byteProgress = {
                            completedFiles,
                            totalFiles,
                            processedBytes,
                            totalBytes ->
                            progressState?.invoke(
                                OperationProgress.weightedItemsAndBytes(
                                    stage =
                                        "Пакую том…",
                                    itemsDone =
                                        completedFiles,
                                    itemsTotal =
                                        totalFiles,
                                    bytesDone =
                                        processedBytes,
                                    bytesTotal =
                                        totalBytes,
                                    itemLabel =
                                        "Файлів",
                                ),
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

    /**
     * v0.5.142: the volume archive (archives plan v2, stage 1) — same checks as [export], but
     * written by [VolumeArchiveWriter] (every entry STORED, no compressor; see there why) into
     * a plain file of the app's own folder.
     */
    fun exportArchive(
        context: Context,
        volume: ProjectVolumeRecord,
        target: File,
        progressState: ((OperationProgress) -> Unit)? = null,
    ): Result<ExportResult> =
        runCatching {
            val appContext = context.applicationContext
            val packageId =
                LocalDatasetDocumentsProvider.packageIdFromTreeUri(Uri.parse(volume.treeUri))
                    ?: error("Архів доступний лише для тому, встановленого з .rdpkg.")
            val sourceRoot =
                LocalDatasetDocumentsProvider.packageDirectory(appContext, packageId).canonicalFile
            require(sourceRoot.isDirectory) { "Встановлений пакет не знайдено: " + packageId }
            progressState?.invoke(OperationProgress.indeterminate("Перевіряю…"))
            validateManagedPackage(sourceRoot = sourceRoot, packageId = packageId)
            val written =
                VolumeArchiveWriter.write(
                    sourceRoot = sourceRoot,
                    target = target,
                    progress = { filesDone, filesTotal, bytesDone, bytesTotal ->
                        progressState?.invoke(
                            OperationProgress.weightedItemsAndBytes(
                                stage = "Пакую том…",
                                itemsDone = filesDone,
                                itemsTotal = filesTotal,
                                bytesDone = bytesDone,
                                bytesTotal = bytesTotal,
                                itemLabel = "Файлів",
                            ),
                        )
                    },
                )
            ExportResult(
                packageId = packageId,
                fileCount = written.fileCount,
                sourceBytes = written.sourceBytes,
                sha256 = written.sha256,
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

}
