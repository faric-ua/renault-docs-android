package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

object RdprojectExporter {
    const val FORMAT = "renault-docs-project"
    const val SCHEMA_VERSION = 1

    fun defaultFileName(
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): String {
        val model =
            project.model
                .takeIf {
                    it.isNotBlank()
                }
                ?: project.title
                    .takeIf {
                        it.isNotBlank()
                    }
                ?: project.id
                    .takeIf {
                        it.isNotBlank()
                    }
                ?: "Renault-project"

        val vehicleCodes =
            projectVehicleCodes(
                volumes,
            )

        val parts =
            buildList {
                add(
                    RenaultVolumeIdentity.safeFilePart(
                        model,
                    ),
                )

                if (
                    vehicleCodes.isNotEmpty()
                ) {
                    add(
                        vehicleCodes
                            .joinToString(
                                "-",
                            ) {
                                RenaultVolumeIdentity.safeFilePart(
                                    it,
                                )
                            },
                    )
                }
            }

        return parts
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                "_",
            ) +
            ".rdproject"
    }

    fun blockingVolumes(
        volumes: List<ProjectVolumeRecord>,
    ): List<ProjectVolumeRecord> =
        volumes.filterNot(RdpkgExporter::canFastExport)

    fun export(
        context: Context,
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
        destinationUri: Uri,
        progress: ((String) -> Unit)? = null,
        progressState: ((OperationProgress) -> Unit)? = null,
    ): Result<Int> =
        runCatching {
            require(volumes.isNotEmpty()) {
                "У проєкті немає томів для поширення."
            }
            val blocked = blockingVolumes(volumes)
            require(blocked.isEmpty()) {
                "Не всі томи можна запакувати в .rdproject."
            }

            val tempRoot =
                File(context.cacheDir, "rdproject-export").apply {
                    deleteRecursively()
                    mkdirs()
                }

            try {
                val volumeDir =
                    File(tempRoot, "volumes").apply {
                        mkdirs()
                    }
                val manifestVolumes = JSONArray()
                val progressUnitsPerPhase =
                    1_000
                val progressTotal =
                    (volumes.size + 1) *
                        progressUnitsPerPhase

                volumes.forEachIndexed { index, volume ->
                    val progressVolumeLabel =
                        progressVolumeLabel(
                            volume,
                        )

                    progress?.invoke(
                        "Готую том " +
                            progressVolumeLabel +
                            "…",
                    )
                    progressState?.invoke(
                        OperationProgress.measured(
                            stage =
                                "Готую том " +
                                    progressVolumeLabel +
                                    "…",
                            current =
                                index *
                                    progressUnitsPerPhase,
                            total =
                                progressTotal,
                        ),
                    )

                    val metadata =
                        RdpkgExporter.resolvedMetadata(
                            volume,
                        )
                    val fileName =
                        RdpkgExporter.defaultFileName(
                            project,
                            volume,
                        )
                    val outFile =
                        File(
                            volumeDir,
                            fileName,
                        )
                    val outUri =
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".files",
                            outFile,
                        )

                    RdpkgExporter.export(
                        context = context,
                        volume = volume,
                        destinationUri = outUri,
                        progressState = {
                            volumeProgress ->
                            val subCurrent =
                                if (
                                    volumeProgress.isDeterminate
                                ) {
                                    (
                                        volumeProgress.normalizedCurrent!!
                                            .toLong() *
                                            progressUnitsPerPhase /
                                            volumeProgress.total!!
                                    ).toInt()
                                } else {
                                    0
                                }

                            progressState?.invoke(
                                OperationProgress.measured(
                                    stage =
                                        "Пакую том " +
                                            progressVolumeLabel +
                                            "…",
                                    current =
                                        (
                                            index *
                                                progressUnitsPerPhase +
                                                subCurrent
                                        ).coerceAtMost(
                                            progressTotal,
                                        ),
                                    total =
                                        progressTotal,
                                    itemCurrent =
                                        volumeProgress.itemCurrent,
                                    itemTotal =
                                        volumeProgress.itemTotal,
                                    itemLabel =
                                        volumeProgress.itemLabel,
                                ),
                            )
                        },
                    ).getOrThrow()

                    val manifestVolume =
                        JSONObject()
                            .put(
                                "id",
                                volume.id,
                            )
                            .put(
                                "title",
                                volume.title,
                            )
                            .put(
                                "vehicle_codes",
                                JSONArray(
                                    metadata.vehicleCodes,
                                ),
                            )
                            .put(
                                "file",
                                "volumes/" + fileName,
                            )

                    metadata.documentCode
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            manifestVolume.put(
                                "document_code",
                                it,
                            )
                        }
                    metadata.date
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            manifestVolume.put(
                                "date",
                                it,
                            )
                        }
                    metadata.documentType
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            manifestVolume.put(
                                "document_type",
                                it,
                            )
                        }
                    metadata.documentVersion
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            manifestVolume.put(
                                "document_version",
                                it,
                            )
                        }
                    metadata.region
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            manifestVolume.put(
                                "region",
                                it,
                            )
                        }

                    manifestVolumes.put(
                        manifestVolume,
                    )
                }

                val projectJson =
                    JSONObject()
                        .put(
                            "id",
                            project.id,
                        )
                        .put(
                            "title",
                            project.title,
                        )
                        .put(
                            "model",
                            project.model,
                        )
                        .put(
                            "vehicle_codes",
                            JSONArray(
                                projectVehicleCodes(
                                    volumes,
                                ),
                            ),
                        )

                File(tempRoot, "rdproject.json")
                    .writeText(
                        JSONObject()
                            .put(
                                "format",
                                FORMAT,
                            )
                            .put(
                                "schema_version",
                                SCHEMA_VERSION,
                            )
                            .put(
                                "project",
                                projectJson,
                            )
                            .put(
                                "volumes",
                                manifestVolumes,
                            )
                            .toString(2),
                        Charsets.UTF_8,
                    )

                val projectFiles =
                    tempRoot.walkTopDown()
                        .filter(
                            File::isFile,
                        )
                        .sortedBy {
                            it.relativeTo(
                                tempRoot,
                            ).invariantSeparatorsPath
                        }
                        .toList()

                progress?.invoke(
                    "Пакую .rdproject…",
                )
                progressState?.invoke(
                    OperationProgress.measured(
                        stage =
                            "Пакую проєкт…",
                        current =
                            volumes.size *
                                progressUnitsPerPhase,
                        total =
                            progressTotal,
                    ),
                )

                val projectBytes =
                    projectFiles.sumOf {
                        it.length()
                    }
                var processedBytes =
                    0L
                var lastProgressAt =
                    0L

                context.contentResolver
                    .openOutputStream(
                        destinationUri,
                        "w",
                    )
                    ?.use { raw ->
                        ZipOutputStream(
                            raw.buffered(),
                        ).use { zip ->
                            val buffer =
                                ByteArray(
                                    1024 *
                                        1024,
                                )

                            projectFiles.forEachIndexed {
                                index,
                                file ->
                                val relative =
                                    file.relativeTo(
                                        tempRoot,
                                    ).invariantSeparatorsPath
                                zip.putNextEntry(
                                    ZipEntry(
                                        relative,
                                    ),
                                )

                                file.inputStream()
                                    .buffered()
                                    .use {
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

                                            zip.write(
                                                buffer,
                                                0,
                                                read,
                                            )
                                            processedBytes +=
                                                read

                                            val now =
                                                SystemClock.elapsedRealtime()

                                            if (
                                                now -
                                                    lastProgressAt >=
                                                    100L
                                            ) {
                                                val weighted =
                                                    OperationProgress
                                                        .weightedItemsAndBytes(
                                                            stage =
                                                                "Пакую проєкт…",
                                                            itemsDone =
                                                                index,
                                                            itemsTotal =
                                                                projectFiles.size,
                                                            bytesDone =
                                                                processedBytes,
                                                            bytesTotal =
                                                                projectBytes,
                                                            itemLabel =
                                                                "Файлів",
                                                        )
                                                val finalPhaseProgress =
                                                    (
                                                        weighted.normalizedCurrent!!
                                                            .toLong() *
                                                            progressUnitsPerPhase /
                                                            weighted.total!!
                                                    ).toInt()

                                                progressState?.invoke(
                                                    OperationProgress.measured(
                                                        stage =
                                                            "Пакую проєкт…",
                                                        current =
                                                            (
                                                                volumes.size *
                                                                    progressUnitsPerPhase +
                                                                    finalPhaseProgress
                                                            ).coerceAtMost(
                                                                progressTotal,
                                                            ),
                                                        total =
                                                            progressTotal,
                                                        itemCurrent =
                                                            index,
                                                        itemTotal =
                                                            projectFiles.size,
                                                        itemLabel =
                                                            "Файлів",
                                                    ),
                                                )
                                                lastProgressAt =
                                                    now
                                            }
                                        }
                                    }
                                zip.closeEntry()

                                val completed =
                                    index +
                                        1
                                val weighted =
                                    OperationProgress
                                        .weightedItemsAndBytes(
                                            stage =
                                                "Пакую проєкт…",
                                            itemsDone =
                                                completed,
                                            itemsTotal =
                                                projectFiles.size,
                                            bytesDone =
                                                processedBytes,
                                            bytesTotal =
                                                projectBytes,
                                            itemLabel =
                                                "Файлів",
                                        )
                                val finalPhaseProgress =
                                    (
                                        weighted.normalizedCurrent!!
                                            .toLong() *
                                            progressUnitsPerPhase /
                                            weighted.total!!
                                    ).toInt()

                                progressState?.invoke(
                                    OperationProgress.measured(
                                        stage =
                                            "Пакую проєкт…",
                                        current =
                                            (
                                                volumes.size *
                                                    progressUnitsPerPhase +
                                                    finalPhaseProgress
                                            ).coerceAtMost(
                                                progressTotal,
                                            ),
                                        total =
                                            progressTotal,
                                        itemCurrent =
                                            completed,
                                        itemTotal =
                                            projectFiles.size,
                                        itemLabel =
                                            "Файлів",
                                    ),
                                )
                            }
                        }
                    }
                    ?: error(
                        "Android не відкрив файл .rdproject для запису."
                    )

                progressState?.invoke(
                    OperationProgress.measured(
                        stage =
                            "Готово",
                        current =
                            progressTotal,
                        total =
                            progressTotal,
                    ),
                )

                volumes.size
            } finally {
                tempRoot.deleteRecursively()
            }
        }

    internal fun progressVolumeLabel(
        volume: ProjectVolumeRecord,
    ): String =
        listOfNotNull(
            volume.documentCode
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                },
            volume.date
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                },
        )
            .joinToString(
                " · ",
            )
            .ifBlank {
                volume.title
                    .trim()
                    .ifBlank {
                        volume.id
                    }
            }

    private fun projectVehicleCodes(
        volumes: List<ProjectVolumeRecord>,
    ): List<String> =
        volumes
            .flatMap {
                RdpkgExporter
                    .resolvedMetadata(
                        it,
                    )
                    .vehicleCodes
            }
            .distinct()
}
