package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object ModernCatalogReader {
    private const val MODERN_INDEX =
        "_renault/modern-index.json"
    private const val DATASET_MANIFEST =
        "renault-dataset.json"

    fun read(
        context: Context,
        treeUri: Uri,
    ): Result<ModernCatalog> = runCatching {
        val resolver =
            SafDatasetResolver(
                context = context,
                treeUri = treeUri,
            )

        val modernText =
            resolver
                .openInputStream(
                    MODERN_INDEX,
                )
                ?.bufferedReader(
                    Charsets.UTF_8,
                )
                ?.use {
                    it.readText()
                }

        if (!modernText.isNullOrBlank()) {
            return@runCatching parseModernIndex(
                modernText,
            )
        }

        val manifestText =
            resolver
                .openInputStream(
                    DATASET_MANIFEST,
                )
                ?.bufferedReader(
                    Charsets.UTF_8,
                )
                ?.use {
                    it.readText()
                }
                ?: error(
                    "Не вдалося прочитати renault-dataset.json"
                )

        parseManifestFallback(
            manifestText,
        )
    }

    fun parseModernIndex(
        text: String,
    ): ModernCatalog {
        val root = JSONObject(text)

        require(
            root.optInt(
                "schema_version",
                0,
            ) >= 1
        ) {
            "Непідтримуваний modern-index schema"
        }

        val dataset =
            root.optJSONObject("dataset")
                ?: error(
                    "modern-index не містить dataset"
                )

        val navigation =
            root.optJSONObject("navigation")
                ?: error(
                    "modern-index не містить navigation"
                )

        val volumes =
            navigation.optJSONArray("volumes")
                ?: JSONArray()

        return ModernCatalog(
            title = requiredString(
                dataset,
                "title",
            ),
            manufacturer =
                optionalString(
                    dataset,
                    "manufacturer",
                ),
            model =
                optionalString(
                    dataset,
                    "model",
                ),
            platform =
                optionalString(
                    dataset,
                    "platform",
                ),
            yearsLabel =
                yearsLabel(
                    dataset
                        .optJSONObject("years"),
                ),
            contentType =
                optionalString(
                    dataset,
                    "content_type",
                ),
            volumes =
                parseVolumes(volumes),
            source = "modern-index",
        )
    }

    fun parseManifestFallback(
        text: String,
    ): ModernCatalog {
        val root = JSONObject(text)

        val volumes =
            root.optJSONArray("volumes")
                ?: JSONArray()

        return ModernCatalog(
            title = requiredString(
                root,
                "title",
            ),
            manufacturer =
                optionalString(
                    root,
                    "manufacturer",
                ),
            model =
                optionalString(
                    root,
                    "model",
                ),
            platform =
                optionalString(
                    root,
                    "platform",
                ),
            yearsLabel =
                yearsLabel(
                    root.optJSONObject(
                        "years",
                    ),
                ),
            contentType =
                optionalString(
                    root,
                    "content_type",
                ),
            volumes =
                parseVolumes(volumes),
            source = "manifest-fallback",
        )
    }

    private fun parseVolumes(
        array: JSONArray,
    ): List<ModernVolume> =
        buildList {
            for (
                index in
                0 until array.length()
            ) {
                val item =
                    array.getJSONObject(index)

                val sourceFolder =
                    optionalString(
                        item,
                        "source_folder",
                    )

                val documentCode =
                    optionalString(
                        item,
                        "document_code",
                    )

                val date =
                    optionalString(
                        item,
                        "date",
                    )

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

                val entrypoint =
                    requiredString(
                        item,
                        "entrypoint",
                    )

                add(
                    ModernVolume(
                        id =
                            optionalString(
                                item,
                                "id",
                            )
                                ?: entrypoint,
                        title =
                            optionalString(
                                item,
                                "title",
                            )
                                ?: optionalString(
                                    item,
                                    "document_code",
                                )
                                ?: optionalString(
                                    item,
                                    "source_folder",
                                )
                                ?: entrypoint,
                        documentCode =
                            documentCode,
                        date =
                            date,
                        kind =
                            optionalString(
                                item,
                                "kind",
                            ),
                        sourceFolder =
                            sourceFolder,
                        entrypoint =
                            entrypoint,
                    )
                )
            }
        }.sortedWith(
            compareBy<ModernVolume> {
                it.date
                    ?: "9999-99-99"
            }.thenBy {
                it.documentCode
                    ?: it.title
            }
        )

    private fun shouldIgnoreSyntheticVolume(
        sourceFolder: String?,
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
                ?.trim()
                ?.lowercase(
                    Locale.ROOT,
                )
                ?: return false

        return normalized in
            setOf(
                "backup",
                "_renault",
                "packages",
            )
    }

    private fun requiredString(
        json: JSONObject,
        key: String,
    ): String {
        val value =
            json.optString(key).trim()

        require(value.isNotEmpty()) {
            "Відсутнє поле: $key"
        }

        return value
    }

    private fun optionalString(
        json: JSONObject,
        key: String,
    ): String? =
        json.optString(key)
            .trim()
            .takeIf {
                it.isNotEmpty()
            }

    private fun yearsLabel(
        years: JSONObject?,
    ): String? {
        if (years == null) {
            return null
        }

        val from =
            years.optInt(
                "from",
                0,
            ).takeIf {
                it > 0
            }

        val to =
            years.optInt(
                "to",
                0,
            ).takeIf {
                it > 0
            }

        return when {
            from != null &&
                to != null ->
                "$from–$to"

            from != null ->
                from.toString()

            to != null ->
                to.toString()

            else ->
                null
        }
    }
}
