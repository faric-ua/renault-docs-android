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

    fun defaultFileName(project: RenaultProject): String =
        project.id.ifBlank { "renault-project" } + ".rdproject"

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
                    val fileName =
                        RdpkgExporter.defaultFileName(project, volume)
                    val outFile = File(volumeDir, fileName)
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

                    manifestVolumes.put(
                        JSONObject()
                            .put("id", volume.id)
                            .put("document_code", volume.documentCode)
                            .put("date", volume.date)
                            .put("file", "volumes/" + fileName),
                    )
                }

                File(tempRoot, "rdproject.json")
                    .writeText(
                        JSONObject()
                            .put("format", FORMAT)
                            .put("schema_version", SCHEMA_VERSION)
                            .put(
                                "project",
                                JSONObject()
                                    .put("id", project.id)
                                    .put("title", project.title)
                                    .put("model", project.model),
                            )
                            .put("volumes", manifestVolumes)
                            .toString(2),
                        Charsets.UTF_8,
                    )

                context.contentResolver
                    .openOutputStream(destinationUri, "w")
                    ?.use { raw ->
                        ZipOutputStream(raw.buffered()).use { zip ->
                            tempRoot.walkTopDown()
                                .filter(File::isFile)
                                .sortedBy { it.relativeTo(tempRoot).invariantSeparatorsPath }
                                .forEach { file ->
                                    val relative =
                                        file.relativeTo(tempRoot).invariantSeparatorsPath
                                    zip.putNextEntry(ZipEntry(relative))
                                    file.inputStream().buffered().use {
                                        input -> input.copyTo(zip)
                                    }
                                    zip.closeEntry()
                                }
                        }
                    }
                    ?: error("Android не відкрив файл .rdproject для запису.")

                volumes.size
            } finally {
                tempRoot.deleteRecursively()
            }
        }
}
