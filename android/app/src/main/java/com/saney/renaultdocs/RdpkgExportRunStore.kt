package com.saney.renaultdocs

import android.content.Context

enum class RdpkgExportRunPhase { IDLE, EXPORTING, COMPLETE, FAILED }

data class RdpkgExportRunState(
    val phase: RdpkgExportRunPhase = RdpkgExportRunPhase.IDLE,
    val message: String = "",
    val projectId: String? = null,
    val volumeId: String? = null,
    val destinationUri: String? = null,
    val sha256: String? = null,
    val fileCount: Int = 0,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val dismissedFinishedAtMs: Long = 0L,
) {
    val isRunning get() = phase == RdpkgExportRunPhase.EXPORTING
    val isTerminal get() = phase == RdpkgExportRunPhase.COMPLETE || phase == RdpkgExportRunPhase.FAILED
    val isTerminalDismissed get() = isTerminal && finishedAtMs > 0L && dismissedFinishedAtMs == finishedAtMs
}

class RdpkgExportRunStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load() = RdpkgExportRunState(
        phase = runCatching { RdpkgExportRunPhase.valueOf(prefs.getString(KEY_PHASE, RdpkgExportRunPhase.IDLE.name)!!) }.getOrDefault(RdpkgExportRunPhase.IDLE),
        message = prefs.getString(KEY_MESSAGE, "").orEmpty(),
        projectId = prefs.getString(KEY_PROJECT_ID, null),
        volumeId = prefs.getString(KEY_VOLUME_ID, null),
        destinationUri = prefs.getString(KEY_DESTINATION_URI, null),
        sha256 = prefs.getString(KEY_SHA256, null),
        fileCount = prefs.getInt(KEY_FILE_COUNT, 0),
        startedAtMs = prefs.getLong(KEY_STARTED_AT, 0L),
        finishedAtMs = prefs.getLong(KEY_FINISHED_AT, 0L),
        dismissedFinishedAtMs = prefs.getLong(KEY_DISMISSED_FINISHED_AT, 0L),
    )

    @Synchronized
    fun begin(projectId: String, volumeId: String, destinationUri: String): Boolean {
        if (load().isRunning) return false
        return prefs.edit().clear()
            .putString(KEY_PHASE, RdpkgExportRunPhase.EXPORTING.name)
            .putString(KEY_MESSAGE, "Експортую .rdpkg…")
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_VOLUME_ID, volumeId)
            .putString(KEY_DESTINATION_URI, destinationUri)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .commit()
    }

    fun update(message: String) { prefs.edit().putString(KEY_MESSAGE, message).apply() }

    fun complete(sha256: String, fileCount: Int, message: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgExportRunPhase.COMPLETE.name)
            .putString(KEY_MESSAGE, message)
            .putString(KEY_SHA256, sha256)
            .putInt(KEY_FILE_COUNT, fileCount)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    fun fail(message: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgExportRunPhase.FAILED.name)
            .putString(KEY_MESSAGE, message)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    @Synchronized
    fun dismissTerminal(finishedAtMs: Long): Boolean {
        val state = load()
        if (!state.isTerminal || state.finishedAtMs != finishedAtMs) return false
        return prefs.edit().putLong(KEY_DISMISSED_FINISHED_AT, finishedAtMs).commit()
    }

    companion object {
        private const val PREFS = "renault_docs_rdpkg_export_run"
        private const val KEY_PHASE = "phase"
        private const val KEY_MESSAGE = "message"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_VOLUME_ID = "volume_id"
        private const val KEY_DESTINATION_URI = "destination_uri"
        private const val KEY_SHA256 = "sha256"
        private const val KEY_FILE_COUNT = "file_count"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_FINISHED_AT = "finished_at"
        private const val KEY_DISMISSED_FINISHED_AT = "dismissed_finished_at"
    }
}
