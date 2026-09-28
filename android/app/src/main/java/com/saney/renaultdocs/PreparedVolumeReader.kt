package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale
import org.json.JSONObject

object PreparedVolumeReader {
    fun read(
        context: Context,
        treeUri: Uri,
    ): Result<ProjectVolumeRecord> =
        readAll(
            context =
                context,
            treeUri =
                treeUri,
        )
            .mapCatching {
                volumes ->
                require(
                    volumes.size ==
                        1
                ) {
                    "Очікується один підготовлений том, а знайдено: " +
                        volumes.size
                }

                volumes.single()
            }

    fun readAll(
        context: Context,
        treeUri: Uri,
    ): Result<List<ProjectVolumeRecord>> =
        runCatching {
            val dataset =
                DatasetReader.read(
                    context =
                        context,
                    treeUri =
                        treeUri,
                ).getOrThrow()

            val root =
                DocumentFile
                    .fromTreeUri(
                        context,
                        treeUri,
                    )
                    ?: error(
                        "Android не зміг відкрити вибрану папку."
                    )

            val manifest =
                root.findFile(
                    "renault-dataset.json",
                )
                    ?: error(
                        "У корені немає renault-dataset.json."
                    )

            val text =
                context.contentResolver
                    .openInputStream(
                        manifest.uri,
                    )
                    ?.bufferedReader(
                        Charsets.UTF_8,
                    )
                    ?.use {
                        it.readText()
                    }
                    ?: error(
                        "Не вдалося прочитати renault-dataset.json."
                    )

            val json =
                JSONObject(
                    text,
                )

            val volumes =
                json.optJSONArray(
                    "volumes",
                )
                    ?: error(
                        "У підготовленому томі немає опису volumes."
                    )

            val projectHint =
                json.optString(
                    "project_id",
                )
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: ProjectStore.slugify(
                        dataset.model,
                    )

            val records =
                buildList {
                    for (
                        index in
                        0 until volumes.length()
                    ) {
                        val volume =
                            volumes.getJSONObject(
                                index,
                            )

                        val sourceFolder =
                            volume.optString(
                                "source_folder",
                            )
                                .trim()

                        val documentCode =
                            volume.optString(
                                "document_code",
                            )
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                }

                        val date =
                            volume.optString(
                                "date",
                            )
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                }

                        if (
                            shouldIgnoreSyntheticVolume(
                                sourceFolder =
                                    sourceFolder,
                                documentCode =
                                    documentCode,
                                date =
                                    date,
                            )
                        ) {
                            continue
                        }

                        val volumeEntrypoint =
                            volume.optString(
                                "entrypoint",
                            )
                                .trim()
                                .ifBlank {
                                    if (
                                        volumes.length() ==
                                        1
                                    ) {
                                        dataset.entrypoint
                                    } else {
                                        error(
                                            "У томі " +
                                                (
                                                    documentCode
                                                        ?: sourceFolder
                                                            .ifBlank {
                                                                (index + 1)
                                                                    .toString()
                                                            }
                                                ) +
                                                " немає entrypoint."
                                        )
                                    }
                                }

                        val volumeTitle =
                            volume.optString(
                                "title",
                            )
                                .trim()
                                .ifBlank {
                                    documentCode
                                        ?: sourceFolder
                                }
                                .ifBlank {
                                    dataset.title
                                }

                        val volumeId =
                            volume.optString(
                                "id",
                            )
                                .trim()
                                .ifBlank {
                                    ProjectStore.slugify(
                                        sourceFolder
                                            .ifBlank {
                                                volumeEntrypoint
                                            },
                                    )
                                }

                        add(
                            ProjectVolumeRecord(
                                id =
                                    volumeId,
                                title =
                                    volumeTitle,
                                documentCode =
                                    documentCode,
                                date =
                                    date,
                                projectHint =
                                    projectHint,
                                datasetId =
                                    dataset.id,
                                datasetTitle =
                                    dataset.title,
                                manufacturer =
                                    dataset.manufacturer,
                                model =
                                    dataset.model,
                                platform =
                                    dataset.platform,
                                yearsLabel =
                                    dataset.yearsLabel,
                                contentType =
                                    dataset.contentType,
                                entrypoint =
                                    volumeEntrypoint,
                                openEntrypoint =
                                    dataset.openEntrypoint,
                                treeUri =
                                    dataset.treeUri,
                            ),
                        )
                    }
                }

            require(
                records.isNotEmpty(),
            ) {
                "У підготовленій папці не знайдено жодного реального тому."
            }

            records
        }

    private fun shouldIgnoreSyntheticVolume(
        sourceFolder: String,
        documentCode: String?,
        date: String?,
    ): Boolean {
        if (
            !documentCode.isNullOrBlank() ||
            !date.isNullOrBlank()
        ) {
            return false
        }

        val normalized =
            sourceFolder
                .trim()
                .lowercase(
                    Locale.ROOT,
                )

        return normalized in
            setOf(
                "backup",
                "_renault",
                "packages",
            )
    }
}
