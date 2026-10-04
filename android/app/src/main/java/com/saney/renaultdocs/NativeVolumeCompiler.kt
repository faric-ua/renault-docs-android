package com.saney.renaultdocs

import java.io.File
import java.text.Normalizer
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * File-based Kotlin parity layer for core/volumes.py + core/modern_index.py.
 *
 * It runs only after NativePreparationStager has moved source I/O away from SAF.
 */
object NativeVolumeCompiler {
    const val MODERN_INDEX_SCHEMA_VERSION =
        1

    const val MODERN_INDEX_FILENAME =
        "modern-index.json"

    const val VOLUMES_FILENAME =
        "volumes.json"

    const val PACKAGE_DIR =
        "_renault"

    private val entryNames =
        listOf(
            "INDEX.HTM",
            "index.htm",
            "INDEX.HTML",
            "index.html",
            "ACCUEIL.HTM",
            "accueil.htm",
        )

    private val dateRegex =
        Regex(
            """(?<year>20\d{2})[._-](?<month>\d{2})[._-](?<day>\d{2})"""
        )

    private val ntRegex =
        Regex(
            """(?:^|[^A-Z0-9])NT(?<number>\d{4}[A-Z]?)(?=$|[^A-Z0-9])""",
            RegexOption.IGNORE_CASE,
        )

    data class CompileResult(
        val volumes: List<JSONObject>,
        val volumesPath: File,
        val modernIndexPath: File,
    )

    fun compile(
        outputRoot: File,
        dataset: JSONObject,
        discoveredVolumes: List<JSONObject>? = null,
    ): CompileResult {
        val root =
            outputRoot.canonicalFile

        require(
            root.isDirectory,
        ) {
            "Prepared staging root is not a directory: " +
                root
        }

        val packageRoot =
            File(
                root,
                PACKAGE_DIR,
            )

        require(
            packageRoot.isDirectory ||
                packageRoot.mkdirs(),
        ) {
            "Не вдалося створити _renault."
        }

        val volumes =
            discoveredVolumes
                ?.map {
                    JSONObject(
                        it.toString(),
                    )
                }
                ?: discoverVolumes(
                    root,
                )

        require(
            volumes.isNotEmpty(),
        ) {
            "Не знайдено жодного Renault тому з INDEX/ACCUEIL."
        }

        val volumesPath =
            File(
                packageRoot,
                VOLUMES_FILENAME,
            )

        volumesPath.writeText(
            JSONArray(
                volumes,
            ).toString(
                2,
            ) +
                "\n",
            Charsets.UTF_8,
        )

        val modernIndex =
            buildModernIndex(
                dataset =
                    dataset,
                volumes =
                    volumes,
            )

        val modernIndexPath =
            File(
                packageRoot,
                MODERN_INDEX_FILENAME,
            )

        modernIndexPath.writeText(
            modernIndex.toString(
                2,
            ) +
                "\n",
            Charsets.UTF_8,
        )

        return CompileResult(
            volumes =
                volumes,
            volumesPath =
                volumesPath,
            modernIndexPath =
                modernIndexPath,
        )
    }

    fun discoverVolumes(
        root: File,
    ): List<JSONObject> {
        if (
            !root.isDirectory
        ) {
            return emptyList()
        }

        val volumes =
            root
                .listFiles()
                .orEmpty()
                .filter {
                    it.isDirectory
                }
                .sortedWith(
                    compareBy<File> {
                        it.name.lowercase(
                            Locale.ROOT,
                        )
                    }.thenBy {
                        it.name
                    }
                )
                .mapNotNull {
                    child ->
                    val entrypoint =
                        discoverEntrypoint(
                            child,
                        )
                            ?: return@mapNotNull null

                    val folderName =
                        child.name
                    val identity =
                        RenaultVolumeIdentity.parse(
                            folderName,
                        )
                    val date =
                        identity.date
                    val documentCode =
                        identity.documentCode
                    val documentType =
                        identity.documentType
                            ?: inferDocumentTypeFromEntrypoint(
                                child =
                                    child,
                                entrypoint =
                                    entrypoint,
                            )
                    val isVisu =
                        documentType
                            ?.equals(
                                "Visu",
                                ignoreCase = true,
                            )
                            ?: (
                                "visu" in
                                    folderName.lowercase(
                                        Locale.ROOT,
                                    )
                            )

                    val title =
                        buildList {
                            add(
                                documentCode
                                    ?: if (
                                        isVisu
                                    ) {
                                        "VISU"
                                    } else {
                                        folderName
                                    },
                            )

                            if (
                                date !=
                                null
                            ) {
                                add(
                                    date,
                                )
                            }
                        }
                            .joinToString(
                                " · ",
                            )

                    JSONObject()
                        .put(
                            "id",
                            slugify(
                                folderName,
                            ),
                        )
                        .put(
                            "title",
                            title,
                        )
                        .put(
                            "source_folder",
                            folderName,
                        )
                        .put(
                            "entrypoint",
                            folderName +
                                "/" +
                                entrypoint,
                        )
                        .put(
                            "kind",
                            if (
                                isVisu
                            ) {
                                "wiring-diagrams"
                            } else {
                                "technical-documentation"
                            },
                        )
                        .apply {
                            if (
                                documentCode !=
                                null
                            ) {
                                put(
                                    "document_code",
                                    documentCode,
                                )
                            }

                            if (
                                date !=
                                null
                            ) {
                                put(
                                    "date",
                                    date,
                                )
                            }

                            if (
                                identity.vehicleCodes
                                    .isNotEmpty()
                            ) {
                                put(
                                    "vehicle_codes",
                                    JSONArray(
                                        identity.vehicleCodes,
                                    ),
                                )
                            }

                            documentType
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    put(
                                        "document_type",
                                        it,
                                    )
                                }

                            identity.documentVersion
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    put(
                                        "document_version",
                                        it,
                                    )
                                }

                            identity.region
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    put(
                                        "region",
                                        it,
                                    )
                                }
                        }
                }
                .toMutableList()

        volumes.sortWith {
            left,
            right ->
            val leftDate =
                left.optString(
                    "date",
                )
                    .takeIf {
                        it.isNotBlank()
                    }
            val rightDate =
                right.optString(
                    "date",
                )
                    .takeIf {
                        it.isNotBlank()
                    }

            when {
                leftDate == null &&
                    rightDate != null ->
                    1

                leftDate != null &&
                    rightDate == null ->
                    -1

                leftDate != null &&
                    rightDate != null &&
                    leftDate !=
                    rightDate ->
                    leftDate.compareTo(
                        rightDate,
                    )

                else -> {
                    val leftLabel =
                        left.optString(
                            "document_code",
                            left.optString(
                                "title",
                            ),
                        )
                            .lowercase(
                                Locale.ROOT,
                            )
                    val rightLabel =
                        right.optString(
                            "document_code",
                            right.optString(
                                "title",
                            ),
                        )
                            .lowercase(
                                Locale.ROOT,
                            )

                    leftLabel.compareTo(
                        rightLabel,
                    )
                }
            }
        }

        return volumes
    }

    fun buildModernIndex(
        dataset: JSONObject,
        volumes: List<JSONObject>,
    ): JSONObject {
        val normalizedVolumes =
            JSONArray()

        volumes.forEach {
            volume ->
            val normalized =
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
                    normalized.put(
                        key,
                        volume.get(
                            key,
                        ),
                    )
                }
            }

            normalizedVolumes.put(
                normalized,
            )
        }

        val datasetJson =
            JSONObject()

        listOf(
            "id",
            "title",
            "model",
            "platform",
            "years",
        ).forEach {
            key ->
            if (
                dataset.has(
                    key,
                ) &&
                !dataset.isNull(
                    key,
                )
            ) {
                datasetJson.put(
                    key,
                    dataset.get(
                        key,
                    ),
                )
            }
        }

        datasetJson.put(
            "manufacturer",
            dataset.optString(
                "manufacturer",
                "Renault",
            )
                .ifBlank {
                    "Renault"
                },
        )

        datasetJson.put(
            "content_type",
            dataset.optString(
                "content_type",
                "technical-documentation",
            )
                .ifBlank {
                    "technical-documentation"
                },
        )

        return JSONObject()
            .put(
                "schema_version",
                MODERN_INDEX_SCHEMA_VERSION,
            )
            .put(
                "dataset",
                datasetJson,
            )
            .put(
                "navigation",
                JSONObject()
                    .put(
                        "level",
                        "volumes",
                    )
                    .put(
                        "volumes",
                        normalizedVolumes,
                    ),
            )
    }

    fun discoverEntrypoint(
        root: File,
    ): String? {
        entryNames.forEach {
            name ->
            val direct =
                File(
                    root,
                    name,
                )

            if (
                direct.isFile
            ) {
                return name
            }
        }

        val matches =
            mutableListOf<File>()

        entryNames.forEach {
            name ->
            root.walkTopDown()
                .filter {
                    file ->
                    file.isFile &&
                        file.name ==
                        name
                }
                .forEach {
                    matches +=
                        it
                }
        }

        return matches
            .distinctBy {
                it.canonicalPath
            }
            .sortedWith(
                compareBy<File> {
                    it.relativeTo(
                        root,
                    )
                        .invariantSeparatorsPath
                }
            )
            .firstOrNull()
            ?.relativeTo(
                root,
            )
            ?.invariantSeparatorsPath
    }

    fun extractDate(
        name: String,
    ): String? {
        val match =
            dateRegex.find(
                name,
            )
                ?: return null

        val year =
            match.groups[
                "year"
            ]
                ?.value
                ?: return null
        val month =
            match.groups[
                "month"
            ]
                ?.value
                ?: return null
        val day =
            match.groups[
                "day"
            ]
                ?.value
                ?: return null

        return year +
            "-" +
            month +
            "-" +
            day
    }

    fun extractNtCode(
        name: String,
    ): String? {
        val match =
            ntRegex.find(
                name,
            )
                ?: return null

        return "NT" +
            (
                match.groups[
                    "number"
                ]
                    ?.value
                    ?: return null
            )
                .uppercase(
                    Locale.ROOT,
                )
    }

    fun slugify(
        value: String,
    ): String {
        val normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD,
            )

        val ascii =
            normalized
                .filter {
                    it.code in
                        0..127
                }

        return ascii
            .replace(
                Regex(
                    "[^A-Za-z0-9]+",
                ),
                "-",
            )
            .trim(
                '-',
            )
            .lowercase(
                Locale.ROOT,
            )
            .ifBlank {
                "volume"
            }
    }
    private fun inferDocumentTypeFromEntrypoint(
        child: File,
        entrypoint: String,
    ): String? =
        runCatching {
            val file =
                File(
                    child,
                    entrypoint,
                )

            if (
                !file.isFile
            ) {
                return@runCatching null
            }

            val bytes =
                file.inputStream()
                    .buffered()
                    .use {
                        input ->
                        val buffer =
                            ByteArray(
                                64 * 1024,
                            )
                        val read =
                            input.read(
                                buffer,
                            )

                        if (
                            read <=
                            0
                        ) {
                            ByteArray(
                                0,
                            )
                        } else {
                            buffer.copyOf(
                                read,
                            )
                        }
                    }

            RenaultVolumeIdentity
                .documentTypeFromHtml(
                    bytes.toString(
                        Charsets.ISO_8859_1,
                    ),
                )
        }
            .getOrNull()


}
