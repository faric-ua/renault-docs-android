package com.saney.renaultdocs

import android.content.Context
import org.json.JSONArray

enum class RdprojectImportPhase {
    IDLE,
    RUNNING,
    COMPLETE,
    FAILED,
    CANCELLED,
}

data class RdprojectImportRunState(
    val phase: RdprojectImportPhase = RdprojectImportPhase.IDLE,
    val uri: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val projectId: String = "",
    val projectTitle: String = "",
    /** `volumes/<name>.rdpkg` paths chosen on the preview screen. */
    val wantedFiles: List<String> = emptyList(),
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val message: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
) {
    val isRunning: Boolean get() = phase == RdprojectImportPhase.RUNNING
    val isTerminal: Boolean
        get() =
            phase == RdprojectImportPhase.COMPLETE ||
                phase == RdprojectImportPhase.FAILED ||
                phase == RdprojectImportPhase.CANCELLED
}

/**
 * State of a `.rdproject` import (v0.5.132), kept in SharedPreferences so the screen,
 * rotation and the notification all see the same run. Same shape as the catalog import.
 */
class RdprojectImportRunStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): RdprojectImportRunState {
        val phase =
            runCatching {
                RdprojectImportPhase.valueOf(
                    prefs.getString(KEY_PHASE, RdprojectImportPhase.IDLE.name)
                        ?: RdprojectImportPhase.IDLE.name,
                )
            }.getOrDefault(RdprojectImportPhase.IDLE)
        return RdprojectImportRunState(
            phase = phase,
            uri = prefs.getString(KEY_URI, "").orEmpty(),
            fileName = prefs.getString(KEY_FILE_NAME, "").orEmpty(),
            fileSizeBytes = prefs.getLong(KEY_FILE_SIZE, 0L),
            projectId = prefs.getString(KEY_PROJECT_ID, "").orEmpty(),
            projectTitle = prefs.getString(KEY_PROJECT_TITLE, "").orEmpty(),
            wantedFiles = decode(prefs.getString(KEY_WANTED, "[]").orEmpty()),
            importedCount = prefs.getInt(KEY_IMPORTED, 0),
            skippedCount = prefs.getInt(KEY_SKIPPED, 0),
            message = prefs.getString(KEY_MESSAGE, "").orEmpty(),
            progressCurrent = prefs.getInt(KEY_PROGRESS_CURRENT, 0),
            progressTotal = prefs.getInt(KEY_PROGRESS_TOTAL, 0),
            startedAtMs = prefs.getLong(KEY_STARTED_AT, 0L),
            finishedAtMs = prefs.getLong(KEY_FINISHED_AT, 0L),
        )
    }

    @Synchronized
    fun begin(
        uri: String,
        fileName: String,
        fileSizeBytes: Long,
        projectId: String,
        projectTitle: String,
        wantedFiles: List<String>,
    ): Boolean {
        if (wantedFiles.isEmpty() || load().isRunning) return false
        return prefs.edit()
            .clear()
            .putString(KEY_PHASE, RdprojectImportPhase.RUNNING.name)
            .putString(KEY_URI, uri)
            .putString(KEY_FILE_NAME, fileName)
            .putLong(KEY_FILE_SIZE, fileSizeBytes)
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_PROJECT_TITLE, projectTitle)
            .putString(KEY_WANTED, JSONArray(wantedFiles).toString())
            .putString(KEY_MESSAGE, "Відкриваю файл проєкту…")
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .putLong(KEY_BEAT_AT, System.currentTimeMillis())
            .commit()
    }

    fun update(
        message: String,
        current: Int = 0,
        total: Int = 0,
    ) {
        prefs.edit()
            .putString(KEY_MESSAGE, message)
            .putInt(KEY_PROGRESS_CURRENT, current)
            .putInt(KEY_PROGRESS_TOTAL, total)
            .putLong(KEY_BEAT_AT, System.currentTimeMillis())
            .apply()
    }

    fun counts(
        imported: Int,
        skipped: Int,
    ) {
        prefs.edit()
            .putInt(KEY_IMPORTED, imported)
            .putInt(KEY_SKIPPED, skipped)
            .putLong(KEY_BEAT_AT, System.currentTimeMillis())
            .apply()
    }

    /** The project id can change when the project is created during the import. */
    fun project(
        projectId: String,
        projectTitle: String,
    ) {
        prefs.edit()
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_PROJECT_TITLE, projectTitle)
            .putLong(KEY_BEAT_AT, System.currentTimeMillis())
            .apply()
    }

    fun complete(message: String) = finish(RdprojectImportPhase.COMPLETE, message)

    fun fail(message: String) = finish(RdprojectImportPhase.FAILED, message)

    fun cancelled(message: String) = finish(RdprojectImportPhase.CANCELLED, message)

    private fun finish(
        phase: RdprojectImportPhase,
        message: String,
    ) {
        prefs.edit()
            .putString(KEY_PHASE, phase.name)
            .putString(KEY_MESSAGE, message)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .commit()
    }

    /**
     * Phone QA 2026-10-15 15:00 (M30s): the screen still said «копіюю» many hours
     * later — the phone restarts by itself at 6:00 and the import died with it, but the
     * saved state stayed RUNNING forever. When nothing in this process is doing the
     * import and nothing was reported for a while, the run is recorded as broken off.
     * The volumes already added stay; opening the file again adds only what is missing.
     */
    @Synchronized
    fun markInterruptedIfOrphaned(
        workerAlive: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val state = load()
        val lastBeat = maxOf(prefs.getLong(KEY_BEAT_AT, 0L), state.startedAtMs)
        if (!RdprojectImportProgress.isOrphaned(state.isRunning, workerAlive, nowMs, lastBeat)) return false
        finish(
            RdprojectImportPhase.FAILED,
            RdprojectImportProgress.interruptedMessage(state.importedCount),
        )
        return true
    }

    fun clearTerminal() {
        if (load().isTerminal) prefs.edit().clear().apply()
    }

    @Synchronized
    fun requestCancel(): Boolean {
        if (!load().isRunning) return false
        return prefs.edit().putBoolean(KEY_CANCEL_REQUESTED, true).commit()
    }

    fun isCancelRequested(): Boolean = prefs.getBoolean(KEY_CANCEL_REQUESTED, false)

    private fun decode(raw: String): List<String> =
        runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

    companion object {
        private const val PREFS = "renault_docs_rdproject_import"
        private const val KEY_PHASE = "phase"
        private const val KEY_URI = "uri"
        private const val KEY_FILE_NAME = "file_name"
        private const val KEY_FILE_SIZE = "file_size"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_PROJECT_TITLE = "project_title"
        private const val KEY_WANTED = "wanted"
        private const val KEY_IMPORTED = "imported"
        private const val KEY_SKIPPED = "skipped"
        private const val KEY_MESSAGE = "message"
        private const val KEY_PROGRESS_CURRENT = "progress_current"
        private const val KEY_PROGRESS_TOTAL = "progress_total"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_FINISHED_AT = "finished_at"
        private const val KEY_CANCEL_REQUESTED = "cancel_requested"
        private const val KEY_BEAT_AT = "beat_at"
    }
}
