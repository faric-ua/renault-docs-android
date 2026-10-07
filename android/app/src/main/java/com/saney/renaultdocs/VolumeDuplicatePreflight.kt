package com.saney.renaultdocs

import java.io.File
import java.util.Locale

object VolumeDuplicatePreflight {
    data class Result(
        val exact: ProjectVolumeRecord?,
        val possible: List<ProjectVolumeRecord>,
        val sourceId: String,
        val sourceIdentity: RenaultVolumeIdentityMetadata,
    )

    fun check(
        existing: List<ProjectVolumeRecord>,
        rawRoot: File,
    ): Result {
        val sourceId =
            NativeVolumeCompiler.slugify(
                rawRoot.name,
            )
        val identity =
            RenaultVolumeIdentity.parse(
                rawRoot.name,
            )

        val exact =
            existing.firstOrNull {
                volume ->
                isExact(
                    volume =
                        volume,
                    sourceId =
                        sourceId,
                    identity =
                        identity,
                )
            }

        val possible =
            if (
                exact !=
                null ||
                identity.documentCode
                    .isNullOrBlank()
            ) {
                emptyList()
            } else {
                existing
                    .filter {
                        volume ->
                        volume.documentCode
                            ?.equals(
                                identity.documentCode,
                                ignoreCase =
                                    true,
                            ) ==
                            true
                    }
                    .filterNot {
                        volume ->
                        isExact(
                            volume =
                                volume,
                            sourceId =
                                sourceId,
                            identity =
                                identity,
                        )
                    }
            }

        return Result(
            exact =
                exact,
            possible =
                possible,
            sourceId =
                sourceId,
            sourceIdentity =
                identity,
        )
    }

    private fun isExact(
        volume: ProjectVolumeRecord,
        sourceId: String,
        identity: RenaultVolumeIdentityMetadata,
    ): Boolean {
        if (
            volume.id
                .equals(
                    sourceId,
                    ignoreCase =
                        true,
                )
        ) {
            return true
        }

        val sourceCode =
            identity.documentCode
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false
        val existingCode =
            volume.documentCode
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false

        if (
            !existingCode.equals(
                sourceCode,
                ignoreCase =
                    true,
            )
        ) {
            return false
        }

        val sourceDate =
            identity.date
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
        val existingDate =
            volume.date
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            sourceDate !=
                null &&
            existingDate !=
                null
        ) {
            return sourceDate ==
                existingDate
        }

        if (
            sourceDate ==
                null &&
            existingDate ==
                null
        ) {
            val sourceCodes =
                identity.vehicleCodes
                    .map {
                        it.uppercase(
                            Locale.ROOT,
                        )
                    }
                    .toSet()
            val existingCodes =
                volume.vehicleCodes
                    .map {
                        it.uppercase(
                            Locale.ROOT,
                        )
                    }
                    .toSet()

            return sourceCodes.isNotEmpty() &&
                sourceCodes ==
                    existingCodes
        }

        return false
    }

    fun label(
        volume: ProjectVolumeRecord,
    ): String =
        listOfNotNull(
            volume.documentCode
                ?.takeIf {
                    it.isNotBlank()
                },
            volume.date
                ?.takeIf {
                    it.isNotBlank()
                },
        )
            .joinToString(
                " · ",
            )
            .ifBlank {
                volume.title
            }
}
