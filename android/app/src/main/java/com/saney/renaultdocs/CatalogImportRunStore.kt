package com.saney.renaultdocs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class CatalogImportPhase {
    IDLE,
    DOWNLOADING,
    IMPORTING,
    COMPLETE,
    FAILED,
}

data class CatalogImportItem(
    val projectId: String,
    val projectTitle: String,
    val volumeId: String,
    val documentCode: String,
    val fileName: String,
    val driveFileId: String,
    val sizeBytes: Long,
)

data class CatalogImportRunState(
    val phase: CatalogImportPhase = CatalogImportPhase.IDLE,
    val items: List<CatalogImportItem> = emptyList(),
    val itemIndex: Int = 0,
    val message: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
) {
    val isRunning: Boolean
        get() =
            phase ==
                CatalogImportPhase.DOWNLOADING ||
                phase ==
                    CatalogImportPhase.IMPORTING

    val isTerminal: Boolean
        get() =
            phase ==
                CatalogImportPhase.COMPLETE ||
                phase ==
                    CatalogImportPhase.FAILED
}

class CatalogImportRunStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE,
            )

    fun load(): CatalogImportRunState {
        val phase =
            runCatching {
                CatalogImportPhase.valueOf(
                    prefs.getString(
                        KEY_PHASE,
                        CatalogImportPhase.IDLE.name,
                    ) ?: CatalogImportPhase.IDLE.name,
                )
            }.getOrDefault(
                CatalogImportPhase.IDLE,
            )

        return CatalogImportRunState(
            phase = phase,
            items = decodeItems(
                prefs.getString(
                    KEY_ITEMS,
                    "[]",
                ).orEmpty(),
            ),
            itemIndex = prefs.getInt(
                KEY_ITEM_INDEX,
                0,
            ),
            message = prefs.getString(
                KEY_MESSAGE,
                "",
            ).orEmpty(),
            progressCurrent = prefs.getInt(
                KEY_PROGRESS_CURRENT,
                0,
            ),
            progressTotal = prefs.getInt(
                KEY_PROGRESS_TOTAL,
                0,
            ),
            startedAtMs = prefs.getLong(
                KEY_STARTED_AT,
                0L,
            ),
            finishedAtMs = prefs.getLong(
                KEY_FINISHED_AT,
                0L,
            ),
        )
    }

    @Synchronized
    fun begin(
        items: List<CatalogImportItem>,
    ): Boolean {
        if (
            items.isEmpty() ||
            load().isRunning
        ) {
            return false
        }

        return prefs.edit()
            .clear()
            .putString(
                KEY_PHASE,
                CatalogImportPhase.DOWNLOADING.name,
            )
            .putString(
                KEY_ITEMS,
                encodeItems(
                    items,
                ),
            )
            .putInt(
                KEY_ITEM_INDEX,
                0,
            )
            .putString(
                KEY_MESSAGE,
                "Готую завантаження…",
            )
            .putLong(
                KEY_STARTED_AT,
                System.currentTimeMillis(),
            )
            .commit()
    }

    fun update(
        phase: CatalogImportPhase,
        itemIndex: Int,
        message: String,
        current: Int = 0,
        total: Int = 0,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                phase.name,
            )
            .putInt(
                KEY_ITEM_INDEX,
                itemIndex,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putInt(
                KEY_PROGRESS_CURRENT,
                current,
            )
            .putInt(
                KEY_PROGRESS_TOTAL,
                total,
            )
            .apply()
    }

    fun complete(
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                CatalogImportPhase.COMPLETE.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putLong(
                KEY_FINISHED_AT,
                System.currentTimeMillis(),
            )
            .apply()
    }

    fun fail(
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                CatalogImportPhase.FAILED.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putLong(
                KEY_FINISHED_AT,
                System.currentTimeMillis(),
            )
            .apply()
    }

    fun clearTerminal() {
        if (
            load().isTerminal
        ) {
            prefs.edit()
                .clear()
                .apply()
        }
    }

    private fun encodeItems(
        items: List<CatalogImportItem>,
    ): String {
        val array =
            JSONArray()

        items.forEach {
            item ->
            array.put(
                JSONObject()
                    .put(
                        "projectId",
                        item.projectId,
                    )
                    .put(
                        "projectTitle",
                        item.projectTitle,
                    )
                    .put(
                        "volumeId",
                        item.volumeId,
                    )
                    .put(
                        "documentCode",
                        item.documentCode,
                    )
                    .put(
                        "fileName",
                        item.fileName,
                    )
                    .put(
                        "driveFileId",
                        item.driveFileId,
                    )
                    .put(
                        "sizeBytes",
                        item.sizeBytes,
                    )
            )
        }

        return array.toString()
    }

    private fun decodeItems(
        raw: String,
    ): List<CatalogImportItem> =
        runCatching {
            val array =
                JSONArray(
                    raw,
                )
            buildList {
                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(
                            index,
                        )
                    add(
                        CatalogImportItem(
                            projectId =
                                item.getString(
                                    "projectId",
                                ),
                            projectTitle =
                                item.getString(
                                    "projectTitle",
                                ),
                            volumeId =
                                item.getString(
                                    "volumeId",
                                ),
                            documentCode =
                                item.getString(
                                    "documentCode",
                                ),
                            fileName =
                                item.getString(
                                    "fileName",
                                ),
                            driveFileId =
                                item.getString(
                                    "driveFileId",
                                ),
                            sizeBytes =
                                item.optLong(
                                    "sizeBytes",
                                    0L,
                                ),
                        ),
                    )
                }
            }
        }.getOrDefault(
            emptyList(),
        )

    companion object {
        private const val PREFS =
            "renault_docs_catalog_import"
        private const val KEY_PHASE =
            "phase"
        private const val KEY_ITEMS =
            "items"
        private const val KEY_ITEM_INDEX =
            "item_index"
        private const val KEY_MESSAGE =
            "message"
        private const val KEY_PROGRESS_CURRENT =
            "progress_current"
        private const val KEY_PROGRESS_TOTAL =
            "progress_total"
        private const val KEY_STARTED_AT =
            "started_at"
        private const val KEY_FINISHED_AT =
            "finished_at"
    }
}
