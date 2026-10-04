package com.saney.renaultdocs

import android.content.Context

enum class RdpkgShareRunPhase { IDLE, PREPARING, COMPLETE, FAILED }

data class RdpkgShareRunState(
    val phase: RdpkgShareRunPhase = RdpkgShareRunPhase.IDLE,
    val message: String = "",
    val projectId: String? = null,
    val volumeId: String? = null,
    val preparedPath: String? = null,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val chooserLaunchedFinishedAtMs: Long = 0L,
    val dismissedFinishedAtMs: Long = 0L,
) {
    val isRunning get() = phase == RdpkgShareRunPhase.PREPARING
    val isTerminal get() = phase == RdpkgShareRunPhase.COMPLETE || phase == RdpkgShareRunPhase.FAILED
    val isChooserLaunched get() = isTerminal && finishedAtMs > 0L && chooserLaunchedFinishedAtMs == finishedAtMs
    val isTerminalDismissed get() = isTerminal && finishedAtMs > 0L && dismissedFinishedAtMs == finishedAtMs
}

class RdpkgShareRunStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load() = RdpkgShareRunState(
        phase = runCatching { RdpkgShareRunPhase.valueOf(prefs.getString(KEY_PHASE, RdpkgShareRunPhase.IDLE.name)!!) }.getOrDefault(RdpkgShareRunPhase.IDLE),
        message = prefs.getString(KEY_MESSAGE, "").orEmpty(),
        projectId = prefs.getString(KEY_PROJECT_ID, null),
        volumeId = prefs.getString(KEY_VOLUME_ID, null),
        preparedPath = prefs.getString(KEY_PREPARED_PATH, null),
        startedAtMs = prefs.getLong(KEY_STARTED_AT, 0L),
        finishedAtMs = prefs.getLong(KEY_FINISHED_AT, 0L),
        chooserLaunchedFinishedAtMs = prefs.getLong(KEY_CHOOSER_LAUNCHED_FINISHED_AT, 0L),
        dismissedFinishedAtMs = prefs.getLong(KEY_DISMISSED_FINISHED_AT, 0L),
    )

    @Synchronized
    fun begin(projectId: String, volumeId: String): Boolean {
        if (load().isRunning) return false
        return prefs.edit().clear()
            .putString(KEY_PHASE, RdpkgShareRunPhase.PREPARING.name)
            .putString(KEY_MESSAGE, "Готую .rdpkg для поширення…")
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_VOLUME_ID, volumeId)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .commit()
    }

    fun update(message: String) { prefs.edit().putString(KEY_MESSAGE, message).apply() }

    fun complete(preparedPath: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgShareRunPhase.COMPLETE.name)
            .putString(KEY_MESSAGE, "Том готовий для поширення.")
            .putString(KEY_PREPARED_PATH, preparedPath)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    fun fail(message: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgShareRunPhase.FAILED.name)
            .putString(KEY_MESSAGE, message)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    @Synchronized
    fun markChooserLaunched(finishedAtMs: Long): Boolean {
        val state = load()
        if (!state.isTerminal || state.finishedAtMs != finishedAtMs || state.isChooserLaunched) return false
        return prefs.edit().putLong(KEY_CHOOSER_LAUNCHED_FINISHED_AT, finishedAtMs).commit()
    }

    @Synchronized
    fun dismissTerminal(finishedAtMs: Long): Boolean {
        val state = load()
        if (!state.isTerminal || state.finishedAtMs != finishedAtMs) return false
        return prefs.edit().putLong(KEY_DISMISSED_FINISHED_AT, finishedAtMs).commit()
    }

    companion object {
        private const val PREFS = "renault_docs_rdpkg_share_run"
        private const val KEY_PHASE = "phase"
        private const val KEY_MESSAGE = "message"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_VOLUME_ID = "volume_id"
        private const val KEY_PREPARED_PATH = "prepared_path"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_FINISHED_AT = "finished_at"
        private const val KEY_CHOOSER_LAUNCHED_FINISHED_AT = "chooser_launched_finished_at"
        private const val KEY_DISMISSED_FINISHED_AT = "dismissed_finished_at"
    }
}
