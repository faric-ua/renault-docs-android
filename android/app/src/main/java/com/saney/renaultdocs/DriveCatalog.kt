package com.saney.renaultdocs

import org.json.JSONArray
import org.json.JSONObject

data class DriveCatalog(
    val schemaVersion: Int,
    val catalogVersion: Int,
    val projects: List<DriveCatalogProject>,
)

data class DriveCatalogProject(
    val id: String,
    val title: String,
    val vehicleCodes: List<String>,
    val documentYearFrom: Int?,
    val documentYearTo: Int?,
    val volumes: List<DriveCatalogVolume>,
)

data class DriveCatalogVolume(
    val id: String,
    val documentCode: String,
    val date: String?,
    val documentType: String?,
    val documentVersion: String?,
    val region: String?,
    val fileName: String,
    val driveFileId: String,
    val sizeBytes: Long,
    val sha256: String?,
)

object DriveCatalogParser {
    const val SCHEMA_VERSION = 1

    fun parse(raw: String): DriveCatalog {
        val json = JSONObject(raw)
        val schemaVersion = json.getInt("schema_version")
        require(schemaVersion == SCHEMA_VERSION) {
            "Непідтримувана версія каталогу: $schemaVersion"
        }

        val projectsArray = json.getJSONArray("projects")
        val projects = buildList {
            for (index in 0 until projectsArray.length()) {
                add(parseProject(projectsArray.getJSONObject(index)))
            }
        }

        return DriveCatalog(
            schemaVersion = schemaVersion,
            catalogVersion = json.optInt("catalog_version", 0),
            projects = projects,
        )
    }

    private fun parseProject(json: JSONObject): DriveCatalogProject {
        val id = json.getString("id").trim()
        val title = json.getString("title").trim()
        require(id.isNotBlank() && title.isNotBlank()) {
            "Каталог містить проєкт без id/title."
        }

        val volumesArray = json.getJSONArray("volumes")
        val volumes = buildList {
            for (index in 0 until volumesArray.length()) {
                add(parseVolume(volumesArray.getJSONObject(index)))
            }
        }

        return DriveCatalogProject(
            id = id,
            title = title,
            vehicleCodes = json.optJSONArray("vehicle_codes").toStrings(),
            documentYearFrom = json.optInt("document_year_from", 0).takeIf { it > 0 },
            documentYearTo = json.optInt("document_year_to", 0).takeIf { it > 0 },
            volumes = volumes,
        )
    }

    private fun parseVolume(json: JSONObject): DriveCatalogVolume {
        val id = json.getString("id").trim()
        val documentCode = json.getString("document_code").trim()
        val fileName = json.getString("file_name").trim()
        val driveFileId = json.getString("drive_file_id").trim()

        require(
            id.isNotBlank() &&
                documentCode.isNotBlank() &&
                fileName.endsWith(".rdpkg", ignoreCase = true) &&
                driveFileId.matches(Regex("[A-Za-z0-9_-]{10,}"))
        ) {
            "Каталог містить некоректний запис тому."
        }

        return DriveCatalogVolume(
            id = id,
            documentCode = documentCode,
            date = json.optNullableString("date"),
            documentType = json.optNullableString("document_type"),
            documentVersion = json.optNullableString("document_version"),
            region = json.optNullableString("region"),
            fileName = fileName,
            driveFileId = driveFileId,
            sizeBytes = json.optLong("size_bytes", 0L).coerceAtLeast(0L),
            sha256 = json.optNullableString("sha256"),
        )
    }

    private fun JSONObject.optNullableString(
        key: String,
    ): String? {
        if (
            !has(
                key,
            ) ||
            isNull(
                key,
            )
        ) {
            return null
        }

        return optString(
            key,
        )
            .trim()
            .takeIf {
                it.isNotBlank() &&
                    !it.equals(
                        "null",
                        ignoreCase = true,
                    )
            }
    }

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
