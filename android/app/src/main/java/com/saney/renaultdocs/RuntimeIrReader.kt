package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

data class RuntimeIrSectionData(
    val schemaVersion: Int,
    val compilerPhase: String,
    val volumeTitle: String,
    val documentCode: String?,
    val date: String?,
    val section: JSONObject,
    val volumeDocumentationPath: String?,
)

data class RuntimeIrVolumeDocumentationData(
    val schemaVersion: Int,
    val compilerPhase: String,
    val volumeTitle: String,
    val documentCode: String?,
    val date: String?,
    val path: String,
    val documentation: JSONObject,
)

object RuntimeIrReader {
    private const val RUNTIME_INDEX =
        "_renault/runtime-ir-index.json"

    private const val MAX_INDEX_CHARS =
        4_000_000
    private const val MAX_SECTION_CHARS =
        16_000_000
    private const val MAX_DOCUMENTATION_CHARS =
        12_000_000

    fun readVolumeDocumentationForVolume(
        context: Context,
        treeUri: Uri,
        volumeEntrypoint: String,
    ): Result<RuntimeIrVolumeDocumentationData> =
        runCatching {
            val resolver =
                SafDatasetResolver(
                    context = context,
                    treeUri = treeUri,
                )

            val indexText =
                resolver
                    .openInputStream(
                        RUNTIME_INDEX,
                    )
                    ?.use {
                        readUtf8Limited(
                            input = it,
                            maxChars =
                                MAX_INDEX_CHARS,
                            label =
                                "Runtime IR index",
                        )
                    }
                    ?: error(
                        "Runtime IR index відсутній. Запусти Renault → 9 — Оновити Fast/Modern package."
                    )

            val lookup =
                findVolumeDocumentationShard(
                    indexText =
                        indexText,
                    volumeEntrypoint =
                        volumeEntrypoint,
                )

            val text =
                resolver
                    .openInputStream(
                        lookup.path,
                    )
                    ?.use {
                        readUtf8Limited(
                            input = it,
                            maxChars =
                                MAX_DOCUMENTATION_CHARS,
                            label =
                                "Volume documentation",
                        )
                    }
                    ?: error(
                        "Volume documentation shard ${lookup.path} не знайдено. Запусти Renault → 9."
                    )

            val documentation =
                parseVolumeDocumentationShard(
                    text = text,
                    expectedSchema =
                        lookup.schemaVersion,
                    expectedCompilerPhase =
                        lookup.compilerPhase,
                )

            RuntimeIrVolumeDocumentationData(
                schemaVersion =
                    lookup.schemaVersion,
                compilerPhase =
                    lookup.compilerPhase,
                volumeTitle =
                    lookup.volumeTitle,
                documentCode =
                    lookup.documentCode,
                date =
                    lookup.date,
                path =
                    lookup.path,
                documentation =
                    documentation,
            )
        }

    data class VolumeDocumentationLookup(
        val schemaVersion: Int,
        val compilerPhase: String,
        val path: String,
        val volumeTitle: String,
        val documentCode: String?,
        val date: String?,
    )

    fun findVolumeDocumentationShard(
        indexText: String,
        volumeEntrypoint: String,
    ): VolumeDocumentationLookup {
        val root =
            JSONObject(indexText)

        val schema =
            root.optInt(
                "schema_version",
                0,
            )

        require(schema >= 2) {
            "Для документації тому потрібен Runtime IR schema v2."
        }

        val compilerPhase =
            root.optString(
                "compiler_phase",
                "section-ir-v2",
            )

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
            volumeIndex in
            0 until volumes.length()
        ) {
            val volume =
                volumes.getJSONObject(
                    volumeIndex,
                )

            val classicEntrypoint =
                volume
                    .optString(
                        "classic_entrypoint",
                    )
                    .trim()

            val normalizedCurrent =
                SafDatasetResolver
                    .normalize(
                        classicEntrypoint,
                    )
                    ?: classicEntrypoint

            if (
                !normalizedCurrent.equals(
                    normalizedWanted,
                    ignoreCase = true,
                )
            ) {
                continue
            }

            val path =
                volume
                    .optString(
                        "documentation_path",
                    )
                    .trim()

            require(path.isNotBlank()) {
                "Для цього тому volume-level документація відсутня. Запусти Renault → 9 або відкрий Classic."
            }

            return VolumeDocumentationLookup(
                schemaVersion =
                    schema,
                compilerPhase =
                    compilerPhase,
                path =
                    path,
                volumeTitle =
                    volume.optString(
                        "title",
                        "Renault volume",
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
            )
        }

        error(
            "Для цього тому Runtime IR v2 index не знайдено."
        )
    }

    fun readSection(
        context: Context,
        treeUri: Uri,
        volumeEntrypoint: String,
        sectionCode: String,
        sectionEntrypoint: String,
    ): Result<RuntimeIrSectionData> =
        runCatching {
            val resolver =
                SafDatasetResolver(
                    context = context,
                    treeUri = treeUri,
                )

            val indexText =
                resolver
                    .openInputStream(
                        RUNTIME_INDEX,
                    )
                    ?.use {
                        readUtf8Limited(
                            input = it,
                            maxChars =
                                MAX_INDEX_CHARS,
                            label =
                                "Runtime IR index",
                        )
                    }
                    ?: error(
                        "Runtime IR index відсутній. Запусти Renault → 9 — Оновити Fast/Modern package."
                    )

            val lookup =
                findSectionShard(
                    indexText =
                        indexText,
                    volumeEntrypoint =
                        volumeEntrypoint,
                    sectionCode =
                        sectionCode,
                    sectionEntrypoint =
                        sectionEntrypoint,
                )

            val sectionText =
                resolver
                    .openInputStream(
                        lookup.path,
                    )
                    ?.use {
                        readUtf8Limited(
                            input = it,
                            maxChars =
                                MAX_SECTION_CHARS,
                            label =
                                "Runtime IR section",
                        )
                    }
                    ?: error(
                        "Runtime IR секцію ${lookup.path} не знайдено. Запусти Renault → 9."
                    )

            parseSectionShard(
                sectionText =
                    sectionText,
                expectedSchema =
                    lookup.schemaVersion,
                expectedCompilerPhase =
                    lookup.compilerPhase,
                expectedSectionCode =
                    sectionCode,
                volumeDocumentationPath =
                    lookup.documentationPath,
            )
        }

    data class SectionShardLookup(
        val schemaVersion: Int,
        val compilerPhase: String,
        val path: String,
        val documentationPath: String?,
    )

    fun findSectionShard(
        indexText: String,
        volumeEntrypoint: String,
        sectionCode: String,
        sectionEntrypoint: String = "",
    ): SectionShardLookup {
        val root =
            JSONObject(indexText)

        val schema =
            root.optInt(
                "schema_version",
                0,
            )

        require(schema >= 2) {
            "Для native preview потрібен Runtime IR schema v2."
        }

        val compilerPhase =
            root.optString(
                "compiler_phase",
                "section-ir-v2",
            )

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
            volumeIndex in
            0 until volumes.length()
        ) {
            val volume =
                volumes.getJSONObject(
                    volumeIndex,
                )

            val classicEntrypoint =
                volume
                    .optString(
                        "classic_entrypoint",
                    )
                    .trim()

            val normalizedCurrent =
                SafDatasetResolver
                    .normalize(
                        classicEntrypoint,
                    )
                    ?: classicEntrypoint

            if (
                !normalizedCurrent.equals(
                    normalizedWanted,
                    ignoreCase = true,
                )
            ) {
                continue
            }

            val normalizedSectionEntrypoint =
                SafDatasetResolver
                    .normalize(
                        sectionEntrypoint,
                    )
                    ?: sectionEntrypoint

            val entries =
                volume.optJSONArray(
                    "section_entries",
                )
                    ?: JSONArray()

            if (
                normalizedSectionEntrypoint
                    .isNotBlank()
            ) {
                for (
                    sectionIndex in
                    0 until entries.length()
                ) {
                    val item =
                        entries.getJSONObject(
                            sectionIndex,
                        )
                    val itemCode =
                        item.optString(
                            "code",
                        ).trim()

                    if (
                        !itemCode.equals(
                            sectionCode,
                            ignoreCase = true,
                        )
                    ) {
                        continue
                    }

                    val itemEntrypoint =
                        item.optString(
                            "entrypoint",
                        ).trim()
                    val normalizedItemEntrypoint =
                        SafDatasetResolver
                            .normalize(
                                itemEntrypoint,
                            )
                            ?: itemEntrypoint

                    if (
                        !normalizedItemEntrypoint
                            .equals(
                                normalizedSectionEntrypoint,
                                ignoreCase = true,
                            )
                    ) {
                        continue
                    }

                    val path =
                        item.optString(
                            "path",
                        ).trim()

                    require(
                        path.isNotBlank()
                    ) {
                        "Runtime IR entry для $sectionCode не містить path."
                    }

                    return SectionShardLookup(
                        schemaVersion =
                            schema,
                        compilerPhase =
                            compilerPhase,
                        path =
                            path,
                        documentationPath =
                            volume
                                .optString(
                                    "documentation_path",
                                )
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                },
                    )
                }
            }

            val sections =
                volume.optJSONObject(
                    "sections",
                )
                    ?: JSONObject()

            val path =
                sections
                    .optString(
                        sectionCode,
                    )
                    .trim()

            require(path.isNotBlank()) {
                "Секцію $sectionCode у Runtime IR index цього тому не знайдено."
            }

            return SectionShardLookup(
                schemaVersion =
                    schema,
                compilerPhase =
                    compilerPhase,
                path =
                    path,
                documentationPath =
                    volume
                        .optString(
                            "documentation_path",
                        )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        },
            )
        }

        error(
            "Для цього тому Runtime IR v2 index не знайдено."
        )
    }

    fun parseSectionShard(
        sectionText: String,
        expectedSchema: Int,
        expectedCompilerPhase: String,
        expectedSectionCode: String,
        volumeDocumentationPath: String? = null,
    ): RuntimeIrSectionData {
        val root =
            JSONObject(sectionText)

        val schema =
            root.optInt(
                "runtime_schema_version",
                0,
            )

        require(
            schema == expectedSchema
        ) {
            "Runtime IR index/section schema mismatch: $expectedSchema vs $schema."
        }

        val compilerPhase =
            root.optString(
                "compiler_phase",
                expectedCompilerPhase,
            )

        require(
            compilerPhase ==
                expectedCompilerPhase
        ) {
            "Runtime IR index/section compiler phase mismatch."
        }

        val volume =
            root.optJSONObject(
                "volume",
            )
                ?: error(
                    "Runtime IR section не містить volume metadata."
                )

        val section =
            root.optJSONObject(
                "section",
            )
                ?: error(
                    "Runtime IR section payload відсутній."
                )

        require(
            section
                .optString(
                    "code",
                )
                .trim()
                .equals(
                    expectedSectionCode,
                    ignoreCase = true,
                )
        ) {
            "Runtime IR section code mismatch."
        }

        require(
            section.optString(
                "compile_state",
            ) == "section-ir-v2"
        ) {
            "Секція ще не скомпільована у Runtime IR v2."
        }

        return RuntimeIrSectionData(
            schemaVersion =
                schema,
            compilerPhase =
                compilerPhase,
            volumeTitle =
                volume.optString(
                    "title",
                    "Renault volume",
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
            section =
                section,
            volumeDocumentationPath =
                volumeDocumentationPath,
        )
    }

    fun readVolumeDocumentation(
        context: Context,
        treeUri: Uri,
        path: String,
        expectedSchema: Int,
        expectedCompilerPhase: String,
    ): Result<JSONObject> =
        runCatching {
            require(path.isNotBlank()) {
                "Volume documentation path порожній."
            }

            val resolver =
                SafDatasetResolver(
                    context = context,
                    treeUri = treeUri,
                )

            val text =
                resolver
                    .openInputStream(
                        path,
                    )
                    ?.use {
                        readUtf8Limited(
                            input = it,
                            maxChars =
                                MAX_DOCUMENTATION_CHARS,
                            label =
                                "Volume documentation",
                        )
                    }
                    ?: error(
                        "Volume documentation shard $path не знайдено. Запусти Renault → 9."
                    )

            parseVolumeDocumentationShard(
                text = text,
                expectedSchema =
                    expectedSchema,
                expectedCompilerPhase =
                    expectedCompilerPhase,
            )
        }

    fun parseVolumeDocumentationShard(
        text: String,
        expectedSchema: Int,
        expectedCompilerPhase: String,
    ): JSONObject {
        val root =
            JSONObject(text)

        require(
            root.optInt(
                "runtime_schema_version",
                0,
            ) == expectedSchema
        ) {
            "Volume documentation schema mismatch."
        }

        require(
            root.optString(
                "compiler_phase",
                expectedCompilerPhase,
            ) == expectedCompilerPhase
        ) {
            "Volume documentation compiler phase mismatch."
        }

        return root.optJSONObject(
            "documentation",
        )
            ?: error(
                "Volume documentation payload відсутній."
            )
    }

    private fun readUtf8Limited(
        input: InputStream,
        maxChars: Int,
        label: String,
    ): String {
        val reader =
            InputStreamReader(
                input,
                Charsets.UTF_8,
            )

        val result =
            StringBuilder(
                minOf(
                    maxChars,
                    256 * 1024,
                )
            )

        val buffer =
            CharArray(
                16 * 1024
            )

        while (true) {
            val read =
                reader.read(buffer)

            if (read < 0) {
                break
            }

            require(
                result.length + read <=
                    maxChars
            ) {
                "$label завеликий для runtime. Потрібен sharded Runtime IR package."
            }

            result.append(
                buffer,
                0,
                read,
            )
        }

        return result.toString()
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
