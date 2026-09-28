package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object ModernSectionsReader {
    private const val MODERN_SECTIONS =
        "_renault/modern-sections.json"

    fun read(
        context: Context,
        treeUri: Uri,
        volumeEntrypoint: String,
    ): Result<ModernVolumeSections> =
        runCatching {
            val resolver =
                SafDatasetResolver(
                    context = context,
                    treeUri = treeUri,
                )

            val text =
                resolver
                    .openInputStream(
                        MODERN_SECTIONS,
                    )
                    ?.bufferedReader(
                        Charsets.UTF_8,
                    )
                    ?.use {
                        it.readText()
                    }
                    ?: error(
                        "Native index розділів відсутній. Том буде відкрито в сумісному Classic режимі."
                    )

            parse(
                text = text,
                volumeEntrypoint =
                    volumeEntrypoint,
            )
        }

    fun parse(
        text: String,
        volumeEntrypoint: String,
    ): ModernVolumeSections {
        val root = JSONObject(text)

        require(
            root.optInt(
                "schema_version",
                0,
            ) >= 1
        ) {
            "Непідтримуваний modern-sections schema"
        }

        val normalizedWanted =
            SafDatasetResolver
                .normalize(
                    volumeEntrypoint,
                )
                ?: volumeEntrypoint

        val volumes =
            root.optJSONArray(
                "volumes",
            )
                ?: JSONArray()

        for (
            index in
            0 until volumes.length()
        ) {
            val volume =
                volumes.getJSONObject(
                    index,
                )

            val entrypoint =
                optionalString(
                    volume,
                    "entrypoint",
                )
                    ?: continue

            val normalizedCurrent =
                SafDatasetResolver
                    .normalize(
                        entrypoint,
                    )
                    ?: entrypoint

            if (
                !normalizedCurrent.equals(
                    normalizedWanted,
                    ignoreCase = true,
                )
            ) {
                continue
            }

            return ModernVolumeSections(
                volumeId =
                    optionalString(
                        volume,
                        "id",
                    ),
                volumeTitle =
                    optionalString(
                        volume,
                        "title",
                    ),
                documentCode =
                    optionalString(
                        volume,
                        "document_code",
                    ),
                date =
                    optionalString(
                        volume,
                        "date",
                    ),
                volumeEntrypoint =
                    entrypoint,
                sourceFile =
                    optionalString(
                        volume,
                        "source_file",
                    ),
                sections =
                    parseSections(
                        volume.optJSONArray(
                            "sections",
                        )
                            ?: JSONArray(),
                    ),
            )
        }

        error(
            "Для цього тому native index розділів не знайдено."
        )
    }

    private fun parseSections(
        array: JSONArray,
    ): List<ModernSection> =
        buildList {
            for (
                index in
                0 until array.length()
            ) {
                val item =
                    array.getJSONObject(
                        index,
                    )

                val code =
                    requiredString(
                        item,
                        "code",
                    )
                val entrypoint =
                    requiredString(
                        item,
                        "entrypoint",
                    )

                add(
                    ModernSection(
                        code = code,
                        title =
                            optionalString(
                                item,
                                "title",
                            )
                                ?: "Розділ $code",
                        entrypoint =
                            entrypoint,
                    )
                )
            }
        }

    private fun requiredString(
        json: JSONObject,
        key: String,
    ): String {
        val value =
            json.optString(
                key,
            ).trim()

        require(
            value.isNotBlank()
        ) {
            "Відсутнє поле: $key"
        }

        return value
    }

    private fun optionalString(
        json: JSONObject,
        key: String,
    ): String? =
        json.optString(
            key,
        ).trim()
            .takeIf {
                it.isNotBlank()
            }
}
