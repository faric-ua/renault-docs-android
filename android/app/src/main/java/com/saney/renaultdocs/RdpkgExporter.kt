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

            if (!sourceRoot.isDirectory) {
                // Archives plan v2, stage 2: a volume imported as one file has no folder —
                // its archive *is* the package; it is copied as it is (and checked again).
                VolumeArchiveAccess.active(appContext, packageId)?.let { archive ->
                    return@runCatching copyArchive(
                        context = appContext,
                        archive = archive,
                        packageId = packageId,
                        destinationUri = destinationUri,
                        progressState = progressState,
                    )
                }
            }

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
                        listing = listingReporter(progressState),
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
                    listing = listingReporter(progressState),
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

    /**
     * Archives plan v2, stage 2: export of a volume kept as one `.rdpkg` file. The file was
     * checked entry by entry when it was installed; it is copied byte for byte and the copy
     * is unpacked again while it is written ([ZipStreamCheck], TODO №19).
     */
    private fun copyArchive(
        context: Context,
        archive: VolumeArchive,
        packageId: String,
        destinationUri: Uri,
        progressState: ((OperationProgress) -> Unit)?,
    ): ExportResult {
        val source = archive.file
        val total = source.length().coerceAtLeast(1L)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val raw =
            context.contentResolver.openOutputStream(destinationUri, "w")
                ?: error("Android could not open the .rdpkg destination for writing.")
        progressState?.invoke(OperationProgress.indeterminate("Копіюю том…"))
        ZipStreamCheck().use { check ->
            java.security.DigestOutputStream(TeeOutputStream(raw.buffered(), check.output), digest).use { out ->
                source.inputStream().buffered(1024 * 1024).use { input ->
                    val buffer = ByteArray(1024 * 1024)
                    var done = 0L
                    var lastShownAt = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        out.write(buffer, 0, read)
                        done += read
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastShownAt >= 100L || done >= total) {
                            lastShownAt = now
                            progressState?.invoke(
                                OperationProgress.measured(
                                    stage = "Копіюю том…",
                                    current = (done.coerceAtMost(total) * 10_000L / total).toInt(),
                                    total = 10_000,
                                ),
                            )
                        }
                    }
                }
            }
            check.finish(expectedFiles = archive.fileCount)
        }
        return ExportResult(
            packageId = packageId,
            fileCount = archive.fileCount,
            sourceBytes = source.length(),
            sha256 = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
    }

    /**
     * v0.5.149 (TODO №21 п. 2): while the volume's files are listed (no measure yet — 10+ s
     * on the M30s card) the card says «Шукаю файли тому… · Знайдено файлів: N».
     * At most every 100 ms.
     */
    private fun listingReporter(
        progressState: ((OperationProgress) -> Unit)?,
    ): ((Int) -> Unit)? {
        if (progressState == null) return null
        var lastAt = 0L
        return { found ->
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastAt >= 100L) {
                lastAt = now
                progressState(listingProgress(found))
            }
        }
    }

    fun listingProgress(found: Int): OperationProgress =
        OperationProgress(
            stage = LISTING_STAGE,
            itemCurrent = found,
            itemLabel = LISTING_LABEL,
        )

    const val LISTING_STAGE = "Шукаю файли тому…"
    const val LISTING_LABEL = "Знайдено файлів"

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
