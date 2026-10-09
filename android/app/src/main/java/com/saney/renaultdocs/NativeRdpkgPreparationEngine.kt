package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * End-to-end native preparation core.
 *
 * UI wiring is intentionally separate. This engine already owns the final
 * no-Python data path from a raw SAF source tree to one portable .rdpkg.
 */
class NativeRdpkgPreparationEngine(
    private val context: Context,
    private val onProgress: (String) -> Unit = {},
    private val onProgressState: (OperationProgress) -> Unit = {},
    private val isCancelled: () -> Boolean = { false },
) {
    data class Request(
        val sourceTreeUri: Uri?,
        val sourceName: String,
        val projectId: String,
        val model: String,
        val destinationUri: Uri,
        val title: String = model,
        val platform: String? = null,
        val yearsFrom: String? = null,
        val yearsTo: String? = null,
    )

    data class Result(
        val packageId: String,
        val volume: JSONObject,
        val sectionCount: Int,
        val sourceFiles: Int,
        val changedFiles: Int,
        val changesTotal: Int,
        val archiveFiles: Int,
        val archiveSourceBytes: Long,
        val sha256: String,
    )

    fun prepare(
        request: Request,
    ): Result {
        checkCancelled()

        val sourceTreeUri =
            requireNotNull(
                request.sourceTreeUri,
            ) {
                "Raw SAF source URI відсутній."
            }

        val staged =
            createStager()
                .prepare(
                    sourceTreeUri =
                        sourceTreeUri,
                    stagingToken =
                        stagingToken(
                            request,
                        ),
                )

        return prepareStaged(
            request =
                request,
            staged =
                staged,
        )
    }

    fun prepareLocal(
        request: Request,
        sourceRoot: File,
        excludedNestedRawRoots: Set<File> = emptySet(),
    ): Result {
        checkCancelled()

        val staged =
            createStager()
                .prepareLocal(
                    sourceRoot =
                        sourceRoot,
                    stagingToken =
                        stagingToken(
                            request,
                        ),
                    sourceName =
                        request.sourceName,
                    excludedNestedRawRoots =
                        excludedNestedRawRoots,
                )

        return prepareStaged(
            request =
                request,
            staged =
                staged,
        )
    }

    private fun createStager():
        NativePreparationStager =
                    NativePreparationStager(
                        context =
                            context,
                        onProgress = {
                            progress ->

                            val stage =
                                when (
                                    progress.phase
                                ) {
                                    NativePreparationStager.Phase.SCANNING ->
                                        "Сканую…"
                                    NativePreparationStager.Phase.PREPARING ->
                                        "Готую…"
                                    NativePreparationStager.Phase.COPYING ->
                                        "Копіюю…"
                                    NativePreparationStager.Phase.READY ->
                                        "Готую…"
                                }
        
                            val state =
                                when {
                                    progress.bytesTotal >
                                        0L -> {
                                        OperationProgress.weightedItemsAndBytes(
                                            stage =
                                                stage,
                                            itemsDone =
                                                progress.filesDone,
                                            itemsTotal =
                                                progress.filesTotal
                                                    .coerceAtLeast(
                                                        1,
                                                    ),
                                            bytesDone =
                                                progress.bytesDone,
                                            bytesTotal =
                                                progress.bytesTotal,
                                            itemLabel =
                                                "Файлів",
                                        )
                                    }
        
                                    progress.filesTotal >
                                        0 ->
                                        OperationProgress.measured(
                                            stage =
                                                stage,
                                            current =
                                                progress.filesDone,
                                            total =
                                                progress.filesTotal,
                                            itemCurrent =
                                                progress.filesDone,
                                            itemTotal =
                                                progress.filesTotal,
                                            itemLabel =
                                                "Файлів",
                                        )
        
                                    progress.filesFound >
                                        0 ->
                                        OperationProgress(
                                            stage =
                                                stage,
                                            itemCurrent =
                                                progress.filesFound,
                                            itemLabel =
                                                "Файлів",
                                        )
        
                                    else ->
                                        OperationProgress.indeterminate(
                                            stage,
                                        )
                                }
        
                            onProgressState(
                                state,
                            )
                        },
                        isCancelled =
                            isCancelled,
                    )

    private fun stagingToken(
        request: Request,
    ): String =
        listOf(
            request.projectId,
            request.sourceName,
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                "-",
            )

    private fun prepareStaged(
        request: Request,
        staged: NativePreparationStager.Result,
    ): Result {
        val root =
            staged.stagingRoot

        try {
            checkCancelled()

            val discovered =
                NativeVolumeCompiler
                    .discoverVolumes(
                        root,
                    )

            require(
                discovered.size ==
                1
            ) {
                "Для .rdpkg потрібно вибрати одну Renault volume-папку. " +
                    "Знайдено томів: " +
                    discovered.size
            }

            val sourceVolume =
                discovered.single()

            val projectId =
                request.projectId
                    .trim()
                    .ifBlank {
                        NativeVolumeCompiler
                            .slugify(
                                request.model,
                            )
                    }

            val datasetId =
                NativeVolumeCompiler
                    .slugify(
                        projectId +
                            "-" +
                            sourceVolume
                                .optString(
                                    "id",
                                    "volume",
                                ),
                    )

            val dataset =
                buildDatasetMetadata(
                    request =
                        request,
                    datasetId =
                        datasetId,
                    projectId =
                        projectId,
                    volume =
                        sourceVolume,
                )

            onProgress(
                "Компілюю volume metadata / Modern index…",
            )
            onProgressState(
                OperationProgress.indeterminate(
                    "Готую структуру…",
                ),
            )

            val volumeResult =
                NativeVolumeCompiler
                    .compile(
                        outputRoot =
                            root,
                        dataset =
                            dataset,
                        discoveredVolumes =
                            discovered,
                    )

            checkCancelled()

            val volume =
                volumeResult
                    .volumes
                    .single()

            onProgress(
                "Компілюю native section index…",
            )
            onProgressState(
                OperationProgress.indeterminate(
                    "Готую індекс…",
                ),
            )

            val sections =
                NativeSectionCompiler
                    .compile(
                        outputRoot =
                            root,
                        volumes =
                            volumeResult.volumes,
                        progress =
                            onProgress,
                    )

            checkCancelled()

            onProgress(
                "Компілюю Runtime IR…",
            )
            onProgressState(
                OperationProgress.indeterminate(
                    "Готую дані…",
                ),
            )

            val runtime =
                NativeRuntimeIrCompiler
                    .compile(
                        outputRoot =
                            root,
                        volumes =
                            volumeResult.volumes,
                        sectionsIndex =
                            sections.data,
                        progress =
                            onProgress,
                        sectionProgress = { done, total ->
                            onProgressState(
                                OperationProgress.measured(
                                    stage = "Готую дані…",
                                    current = done,
                                    total = total,
                                    itemCurrent = done,
                                    itemTotal = total,
                                    itemLabel = "Розділів",
                                ),
                            )
                        },
                        phaseProgress = { stage ->
                            onProgressState(OperationProgress.indeterminate(stage))
                        },
                    )

            checkCancelled()

            val packageRoot =
                File(
                    root,
                    NativeVolumeCompiler.PACKAGE_DIR,
                )

            writeCatalogFiles(
                outputRoot =
                    root,
                packageRoot =
                    packageRoot,
                dataset =
                    dataset,
                volume =
                    volume,
            )

            checkCancelled()

            onProgress(
                "Будую Fast Pack…",
            )
            onProgressState(
                OperationProgress.indeterminate(
                    "Пакую дані…",
                ),
            )

            val fastPack =
                NativeFastPackWriter
                    .build(
                        outputRoot =
                            root,
                        packageRoot =
                            packageRoot,
                        progress = onProgress,
                        fileProgress = { done, total ->
                            onProgressState(
                                OperationProgress.measured(
                                    stage = "Пакую дані…",
                                    current = done,
                                    total = total,
                                    itemCurrent = done,
                                    itemTotal = total,
                                    itemLabel = "Файлів",
                                ),
                            )
                        },
                    )

            checkCancelled()

            val manifest =
                buildDatasetManifest(
                    dataset =
                        dataset,
                    volume =
                        volume,
                    fastPack =
                        fastPack,
                    staged =
                        staged,
                )

            File(
                root,
                DATASET_MANIFEST,
            ).writeText(
                manifest.toString(
                    2,
                ) +
                    "\n",
                Charsets.UTF_8,
            )

            val packageId =
                packageId(
                    projectId =
                        projectId,
                    volume =
                        volume,
                )

            val payloadFilesBeforeManifest =
                root
                    .walkTopDown()
                    .filter {
                        it.isFile &&
                            it.name !=
                            RDPKG_MANIFEST
                    }
                    .toList()

            val payloadBytes =
                payloadFilesBeforeManifest
                    .sumOf {
                        it.length()
                    }

            val packageManifest =
                JSONObject()
                    .put(
                        "schema_version",
                        RdpkgImporter.SCHEMA_VERSION,
                    )
                    .put(
                        "format",
                        RdpkgImporter.FORMAT,
                    )
                    .put(
                        "package_id",
                        packageId,
                    )
                    .put(
                        "project_id",
                        projectId,
                    )
                    .put(
                        "dataset_id",
                        datasetId,
                    )
                    .put(
                        "dataset_title",
                        dataset.optString(
                            "title",
                        ),
                    )
                    .put(
                        "volume",
                        copyVolumeForManifest(
                            volume,
                        ),
                    )
                    .put(
                        "dataset_manifest",
                        DATASET_MANIFEST,
                    )
                    .put(
                        "payload_file_count",
                        payloadFilesBeforeManifest.size,
                    )
                    .put(
                        "payload_bytes",
                        payloadBytes,
                    )

            File(
                root,
                RDPKG_MANIFEST,
            ).writeText(
                packageManifest.toString(
                    2,
                ) +
                    "\n",
                Charsets.UTF_8,
            )

            checkCancelled()

            onProgress(
                "Пакую .rdpkg…",
            )
            onProgressState(
                OperationProgress.indeterminate(
                    "Пакую…",
                ),
            )

            val archive =
                RdpkgZipWriter
                    .writeDirectory(
                        context =
                            context,
                        sourceRoot =
                            root,
                        destinationUri =
                            request.destinationUri,
                        progress = { _, _ ->
                            checkCancelled()
                        },
                        byteProgress = {
                            completedFiles,
                            totalFiles,
                            processedBytes,
                            totalBytes ->
                            checkCancelled()

                            onProgressState(
                                OperationProgress.weightedItemsAndBytes(
                                    stage =
                                        "Пакую…",
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

            onProgress(
                "Готово · " +
                    volume
                        .optString(
                            "document_code",
                            volume.optString(
                                "title",
                            ),
                        ) +
                    " · " +
                    runtime.sectionCount +
                    " native sections",
            )

            return Result(
                packageId =
                    packageId,
                volume =
                    JSONObject(
                        volume.toString(),
                    ),
                sectionCount =
                    runtime.sectionCount,
                sourceFiles =
                    staged.filesTotal,
                changedFiles =
                    staged.changedFiles,
                changesTotal =
                    staged.changesTotal,
                archiveFiles =
                    archive.fileCount,
                archiveSourceBytes =
                    archive.sourceBytes,
                sha256 =
                    archive.sha256,
            )
        } finally {
            root.deleteRecursively()
        }

    }

    private fun buildDatasetMetadata(
        request: Request,
        datasetId: String,
        projectId: String,
        volume: JSONObject,
    ): JSONObject {
        val dataset =
            JSONObject()
                .put(
                    "id",
                    datasetId,
                )
                .put(
                    "title",
                    request.title
                        .ifBlank {
                            request.model
                        },
                )
                .put(
                    "manufacturer",
                    "Renault",
                )
                .put(
                    "model",
                    request.model,
                )
                .put(
                    "project_id",
                    projectId,
                )
                .put(
                    "content_type",
                    "technical-documentation",
                )
                .put(
                    "entrypoint",
                    volume.optString(
                        "entrypoint",
                    ),
                )
                .put(
                    "viewer_profile",
                    "renault-legacy-web-v1",
                )

        request.platform
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                dataset.put(
                    "platform",
                    it,
                )
            }

        if (
            !request.yearsFrom.isNullOrBlank() ||
            !request.yearsTo.isNullOrBlank()
        ) {
            val years =
                JSONObject()

            request.yearsFrom
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    years.put(
                        "from",
                        it,
                    )
                }

            request.yearsTo
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    years.put(
                        "to",
                        it,
                    )
                }

            dataset.put(
                "years",
                years,
            )
        }

        return dataset
    }

    private fun buildDatasetManifest(
        dataset: JSONObject,
        volume: JSONObject,
        fastPack: NativeFastPackWriter.Result,
        staged: NativePreparationStager.Result,
    ): JSONObject {
        val manifest =
            JSONObject(
                dataset.toString(),
            )

        manifest
            .put(
                "schema_version",
                1,
            )
            .put(
                "legacy_entrypoint",
                dataset.optString(
                    "entrypoint",
                ),
            )
            .put(
                "catalog_entrypoint",
                "_renault/START.html",
            )
            .put(
                "modern_index",
                "_renault/" +
                    NativeVolumeCompiler.MODERN_INDEX_FILENAME,
            )
            .put(
                "modern_sections",
                "_renault/" +
                    NativeSectionCompiler.FILENAME,
            )
            .put(
                "runtime_tree",
                "_renault/" +
                    NativeRuntimeIrCompiler.RUNTIME_TREE_FILENAME,
            )
            .put(
                "runtime_ir_index",
                "_renault/" +
                    NativeRuntimeIrCompiler.RUNTIME_INDEX_FILENAME,
            )
            .put(
                "runtime_ir_coverage",
                "_renault/" +
                    NativeRuntimeIrCompiler.COVERAGE_FILENAME,
            )
            .put(
                "volumes",
                JSONArray()
                    .put(
                        JSONObject(
                            volume.toString(),
                        )
                    ),
            )
            .put(
                "fast_pack",
                JSONObject()
                    .put(
                        "schema_version",
                        NativeFastPackWriter.SCHEMA_VERSION,
                    )
                    .put(
                        "format",
                        NativeFastPackWriter.FORMAT,
                    )
                    .put(
                        "path",
                        fastPack.relativePath,
                    )
                    .put(
                        "sha256",
                        fastPack.sha256,
                    )
                    .put(
                        "bytes",
                        fastPack.bytes,
                    )
                    .put(
                        "file_count",
                        fastPack.fileCount,
                    ),
            )
            .put(
                "capabilities",
                JSONObject()
                    .put(
                        "html_frames",
                        true,
                    )
                    .put(
                        "javascript",
                        true,
                    )
                    .put(
                        "pdf",
                        true,
                    )
                    .put(
                        "pdf_fragments",
                        true,
                    ),
            )
            .put(
                "conversion",
                JSONObject()
                    .put(
                        "normalized",
                        true,
                    )
                    .put(
                        "tool",
                        "renault-docs-android",
                    )
                    .put(
                        "writer",
                        "android-native-rdpkg-v1",
                    )
                    .put(
                        "files_total",
                        staged.filesTotal,
                    )
                    .put(
                        "changed_files",
                        staged.changedFiles,
                    )
                    .put(
                        "changes_total",
                        staged.changesTotal,
                    )
                    .put(
                        "modern_runtime_compiled",
                        true,
                    ),
            )

        return manifest
    }

    private fun writeCatalogFiles(
        outputRoot: File,
        packageRoot: File,
        dataset: JSONObject,
        volume: JSONObject,
    ) {
        require(
            packageRoot.isDirectory ||
                packageRoot.mkdirs(),
        ) {
            "Не вдалося створити _renault."
        }

        val title =
            escapeHtml(
                dataset.optString(
                    "title",
                    dataset.optString(
                        "model",
                        "Renault",
                    ),
                ),
            )
        val volumeTitle =
            escapeHtml(
                volume.optString(
                    "title",
                    "Renault volume",
                ),
            )
        val entrypoint =
            escapeAttribute(
                volume.optString(
                    "entrypoint",
                ),
            )

        File(
            packageRoot,
            "START.html",
        ).writeText(
            """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>$title</title>
            </head>
            <body>
              <h1>$title</h1>
              <a href="../$entrypoint">$volumeTitle</a>
            </body>
            </html>
            """.trimIndent() +
                "\n",
            Charsets.UTF_8,
        )

        File(
            packageRoot,
            "README_UA.html",
        ).writeText(
            """
            <!doctype html>
            <html lang="uk">
            <head><meta charset="utf-8"><title>Renault Docs</title></head>
            <body>
              <h1>$title</h1>
              <p>Один підготовлений Renault том у portable .rdpkg.</p>
              <p>Modern navigation, Runtime IR та Fast Pack згенеровано нативно на Android.</p>
            </body>
            </html>
            """.trimIndent() +
                "\n",
            Charsets.UTF_8,
        )
    }

    private fun packageId(
        projectId: String,
        volume: JSONObject,
    ): String =
        NativeVolumeCompiler
            .slugify(
                listOfNotNull(
                    projectId,
                    volume
                        .optString(
                            "document_code",
                            volume.optString(
                                "id",
                                "volume",
                            ),
                        ),
                    volume
                        .optString(
                            "date",
                        )
                        .takeIf {
                            it.isNotBlank()
                        },
                ).joinToString(
                    "-",
                ),
            )
            .take(
                160,
            )

    private fun copyVolumeForManifest(
        volume: JSONObject,
    ): JSONObject {
        val result =
            JSONObject()

        listOf(
            "id",
            "title",
            "document_code",
            "date",
            "vehicle_codes",
            "document_type",
            "document_version",
            "region",
            "kind",
            "source_folder",
            "entrypoint",
        ).forEach {
            key ->
            if (
                volume.has(
                    key,
                ) &&
                !volume.isNull(
                    key,
                )
            ) {
                result.put(
                    key,
                    volume.get(
                        key,
                    ),
                )
            }
        }

        return result
    }

    private fun checkCancelled() {
        if (
            isCancelled()
        ) {
            throw ConversionCancelledException()
        }
    }

    private fun escapeHtml(
        value: String,
    ): String =
        value
            .replace(
                "&",
                "&amp;",
            )
            .replace(
                "<",
                "&lt;",
            )
            .replace(
                ">",
                "&gt;",
            )
            .replace(
                "\"",
                "&quot;",
            )

    private fun escapeAttribute(
        value: String,
    ): String =
        escapeHtml(
            value,
        )

    companion object {
        private const val DATASET_MANIFEST =
            "renault-dataset.json"

        private const val RDPKG_MANIFEST =
            "rdpkg.json"
    }
}
