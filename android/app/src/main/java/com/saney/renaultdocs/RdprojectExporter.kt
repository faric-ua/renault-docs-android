package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
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

                volumes.forEachIndexed { index, volume ->
                    progress?.invoke(
                        "Готую том " + (index + 1) + "/" + volumes.size + "…",
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

                context.contentResolver
                    .openOutputStream(
                        destinationUri,
                        "w",
                    )
                    ?.use { raw ->
                        ZipOutputStream(
                            raw.buffered(),
                        ).use { zip ->
                            tempRoot.walkTopDown()
                                .filter(
                                    File::isFile,
                                )
                                .sortedBy {
                                    it.relativeTo(
                                        tempRoot,
                                    ).invariantSeparatorsPath
                                }
                                .forEach { file ->
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
                                            input.copyTo(
                                                zip,
                                            )
                                        }
                                    zip.closeEntry()
                                }
                        }
                    }
                    ?: error(
                        "Android не відкрив файл .rdproject для запису."
                    )

                volumes.size
            } finally {
                tempRoot.deleteRecursively()
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
