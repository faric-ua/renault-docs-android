package com.saney.renaultdocs

import org.json.JSONObject

object DatasetManifestParser {
    fun parse(text: String): DatasetMetadata {
        val json = JSONObject(text)

        val schemaVersion = json.optInt("schema_version", 0)
        require(schemaVersion >= 1) {
            "Непідтримуваний або відсутній schema_version"
        }

        val id = requiredString(json, "id")
        val title = requiredString(json, "title")
        val manufacturer = requiredString(json, "manufacturer")
        val model = requiredString(json, "model")
        val entrypoint = requiredString(json, "entrypoint")

        val catalogEntrypoint = json.optString("catalog_entrypoint")
            .takeIf { it.isNotBlank() }

        val years = json.optJSONObject("years")
        val yearFrom = years?.optInt("from", 0)?.takeIf { it > 0 }
        val yearTo = years?.optInt("to", 0)?.takeIf { it > 0 }

        val yearsLabel = when {
            yearFrom != null && yearTo != null -> yearFrom.toString() + "–" + yearTo.toString()
            yearFrom != null -> yearFrom.toString()
            yearTo != null -> yearTo.toString()
            else -> null
        }

        return DatasetMetadata(
            id = id,
            title = title,
            manufacturer = manufacturer,
            model = model,
            platform = json.optString("platform").takeIf { it.isNotBlank() },
            yearsLabel = yearsLabel,
            contentType = json.optString("content_type").takeIf { it.isNotBlank() },
            entrypoint = entrypoint,
            openEntrypoint = catalogEntrypoint ?: entrypoint,
            volumeCount = json.optJSONArray("volumes")?.length() ?: 0,
        )
    }

    private fun requiredString(
        json: JSONObject,
        key: String,
    ): String {
        val value = json.optString(key).trim()
        require(value.isNotEmpty()) {
            "У renault-dataset.json відсутнє поле: " + key
        }
        return value
    }
}
