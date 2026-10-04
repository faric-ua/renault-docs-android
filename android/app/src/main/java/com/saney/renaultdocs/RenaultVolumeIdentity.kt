package com.saney.renaultdocs

import java.text.Normalizer
import java.util.Locale

data class RenaultVolumeIdentityMetadata(
    val documentCode: String? = null,
    val date: String? = null,
    val vehicleCodes: List<String> = emptyList(),
    val documentType: String? = null,
    val documentVersion: String? = null,
    val region: String? = null,
) {
    fun documentDescriptor(): String? {
        val type =
            documentType
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null
        val version =
            documentVersion
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (
            version ==
            null
        ) {
            type
        } else {
            type +
                " v" +
                version.removePrefix(
                    "v",
                )
        }
    }
}

/**
 * Canonical parser/formatter for Renault volume identity carried by legacy
 * folder names and .rdpkg metadata.
 *
 * Renault codes such as E84/L84/K84/X74/X61 are opaque technical identifiers.
 * They are preserved verbatim (normalized to upper case) and are never
 * translated into body-style names by the runtime.
 */
object RenaultVolumeIdentity {
    private val ntRegex =
        Regex(
            """(?:^|[^A-Z0-9])NT(?<number>\d{4}[A-Z]?)(?=$|[^A-Z0-9])""",
            RegexOption.IGNORE_CASE,
        )

    private val dateRegex =
        Regex(
            """(?<year>20\d{2})[._-](?<month>\d{2})[._-](?<day>\d{2})"""
        )

    private val groupedVehicleCodeRegex =
        Regex(
            """(?<![A-Z0-9])(?<letters>[A-Z](?:\s*[,/]\s*[A-Z])+)[ ]*(?<series>\d{2})(?!\d)""",
            RegexOption.IGNORE_CASE,
        )

    private val directVehicleCodeRegex =
        Regex(
            """(?<![A-Z0-9])(?<prefix>[A-Z])(?<series>\d{2})(?![A-Z0-9])""",
            RegexOption.IGNORE_CASE,
        )

    private val visuRegex =
        Regex(
            """(?<![A-Z0-9])(?<type>VISU)\s*[- ]?\s*[Vv]?\s*(?<version>\d+(?:\.\d+)*)(?!\d)""",
            RegexOption.IGNORE_CASE,
        )

    private val europeRegex =
        Regex(
            """(?<![A-Z0-9])EUROPE(?![A-Z0-9])""",
            RegexOption.IGNORE_CASE,
        )

    fun parse(
        vararg values: String?,
    ): RenaultVolumeIdentityMetadata {
        val source =
            values
                .filterNotNull()
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    " ",
                )

        val documentCode =
            ntRegex.find(
                source,
            )
                ?.groups
                ?.get(
                    "number",
                )
                ?.value
                ?.let {
                    "NT" +
                        it.uppercase(
                            Locale.ROOT,
                        )
                }

        val date =
            dateRegex.find(
                source,
            )
                ?.let {
                    match ->
                    val year =
                        match.groups[
                            "year"
                        ]
                            ?.value
                    val month =
                        match.groups[
                            "month"
                        ]
                            ?.value
                    val day =
                        match.groups[
                            "day"
                        ]
                            ?.value

                    if (
                        year ==
                        null ||
                        month ==
                        null ||
                        day ==
                        null
                    ) {
                        null
                    } else {
                        year +
                            "-" +
                            month +
                            "-" +
                            day
                    }
                }

        val vehicleCodes =
            extractVehicleCodes(
                source,
            )

        val visu =
            visuRegex.find(
                source,
            )
        val documentType =
            visu
                ?.groups
                ?.get(
                    "type",
                )
                ?.value
                ?.let {
                    "Visu"
                }
        val documentVersion =
            visu
                ?.groups
                ?.get(
                    "version",
                )
                ?.value

        val region =
            if (
                europeRegex.containsMatchIn(
                    source,
                )
            ) {
                "Europe"
            } else {
                null
            }

        return RenaultVolumeIdentityMetadata(
            documentCode =
                documentCode,
            date =
                date,
            vehicleCodes =
                vehicleCodes,
            documentType =
                documentType,
            documentVersion =
                documentVersion,
            region =
                region,
        )
    }

    fun documentTypeFromHtml(
        html: String,
    ): String? {
        val title =
            Regex(
                """<title[^>]*>\s*Visu\s+Schema\b""",
                setOf(
                    RegexOption.IGNORE_CASE,
                    RegexOption.DOT_MATCHES_ALL,
                ),
            )

        return if (
            title.containsMatchIn(
                html,
            )
        ) {
            "Visu"
        } else {
            null
        }
    }

    fun canonicalFileName(
        model: String,
        metadata: RenaultVolumeIdentityMetadata,
        fallbackId: String? = null,
    ): String {
        val parts =
            buildList {
                add(
                    safeFilePart(
                        model,
                    ),
                )

                if (
                    metadata.vehicleCodes
                        .isNotEmpty()
                ) {
                    add(
                        metadata.vehicleCodes
                            .joinToString(
                                "-",
                            ) {
                                safeFilePart(
                                    it,
                                )
                            },
                    )
                }

                metadata.region
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            safeFilePart(
                                it,
                            ),
                        )
                    }

                add(
                    safeFilePart(
                        metadata.documentCode
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: fallbackId
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                            ?: "volume",
                    ),
                )

                metadata.documentType
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        type ->
                        val version =
                            metadata.documentVersion
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.removePrefix(
                                    "v",
                                )

                        add(
                            safeFilePart(
                                if (
                                    version ==
                                    null
                                ) {
                                    type
                                } else {
                                    type +
                                        "-v" +
                                        version
                                },
                            ),
                        )
                    }

                metadata.date
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            safeFilePart(
                                it,
                            ),
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
            ".rdpkg"
    }

    fun safeFilePart(
        value: String,
    ): String {
        val normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD,
            )
        val ascii =
            normalized
                .replace(
                    Regex(
                        "\\p{M}+",
                    ),
                    "",
                )
        val clean =
            ascii
                .replace(
                    Regex(
                        "[^A-Za-z0-9._-]+",
                    ),
                    "-",
                )
                .trim(
                    '-',
                    '.',
                    '_',
                )

        return clean.ifBlank {
            "Renault"
        }
    }

    private fun extractVehicleCodes(
        source: String,
    ): List<String> {
        val codes =
            mutableListOf<String>()

        groupedVehicleCodeRegex
            .findAll(
                source,
            )
            .forEach {
                match ->
                val letters =
                    match.groups[
                        "letters"
                    ]
                        ?.value
                        .orEmpty()
                val series =
                    match.groups[
                        "series"
                    ]
                        ?.value
                        .orEmpty()

                letters
                    .split(
                        Regex(
                            "\\s*[,/]\\s*",
                        ),
                    )
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.length ==
                            1 &&
                            it[0].isLetter()
                    }
                    .forEach {
                        prefix ->
                        codes +=
                            prefix.uppercase(
                                Locale.ROOT,
                            ) +
                                series
                    }
            }

        directVehicleCodeRegex
            .findAll(
                source,
            )
            .forEach {
                match ->
                val prefix =
                    match.groups[
                        "prefix"
                    ]
                        ?.value
                        ?.uppercase(
                            Locale.ROOT,
                        )
                        ?: return@forEach
                val series =
                    match.groups[
                        "series"
                    ]
                        ?.value
                        ?: return@forEach

                val code =
                    prefix +
                        series

                if (
                    code !in
                    codes
                ) {
                    codes +=
                        code
                }
            }

        return codes
            .distinct()
    }
}
