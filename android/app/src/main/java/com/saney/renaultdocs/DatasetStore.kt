package com.saney.renaultdocs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DatasetStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): List<DatasetRecord> {
        val raw = prefs.getString(KEY_DATASETS, null)
            ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        DatasetRecord(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            manufacturer = item.getString("manufacturer"),
                            model = item.getString("model"),
                            platform = item.optString("platform")
                                .takeIf { it.isNotBlank() },
                            yearsLabel = item.optString("yearsLabel")
                                .takeIf { it.isNotBlank() },
                            contentType = item.optString("contentType")
                                .takeIf { it.isNotBlank() },
                            entrypoint = item.getString("entrypoint"),
                            openEntrypoint = item.getString("openEntrypoint"),
                            volumeCount = item.optInt("volumeCount", 0),
                            treeUri = item.getString("treeUri"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun upsert(record: DatasetRecord) {
        val updated = load()
            .filterNot { it.id == record.id }
            .plus(record)
            .sortedBy { it.title.lowercase() }

        save(updated)
    }

    fun remove(id: String) {
        save(
            load()
                .filterNot {
                    it.id == id
                },
        )
    }

    private fun save(records: List<DatasetRecord>) {
        val array = JSONArray()

        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("title", record.title)
                    .put("manufacturer", record.manufacturer)
                    .put("model", record.model)
                    .put("platform", record.platform ?: "")
                    .put("yearsLabel", record.yearsLabel ?: "")
                    .put("contentType", record.contentType ?: "")
                    .put("entrypoint", record.entrypoint)
                    .put("openEntrypoint", record.openEntrypoint)
                    .put("volumeCount", record.volumeCount)
                    .put("treeUri", record.treeUri)
            )
        }

        prefs.edit()
            .putString(KEY_DATASETS, array.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "renault_docs_library"
        private const val KEY_DATASETS = "datasets"
    }
}
