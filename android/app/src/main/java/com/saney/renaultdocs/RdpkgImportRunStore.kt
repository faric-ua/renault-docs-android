package com.saney.renaultdocs

import android.content.Context

enum class RdpkgImportRunPhase { IDLE, IMPORTING, COMPLETE, FAILED }

data class RdpkgImportRunState(
    val phase: RdpkgImportRunPhase = RdpkgImportRunPhase.IDLE,
    val message: String = "",
    val projectId: String? = null,
    val packageUri: String? = null,
    val packageId: String? = null,
    val progressStage: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val consumedFinishedAtMs: Long = 0L,
) {
    val isRunning get() = phase == RdpkgImportRunPhase.IMPORTING
    val isTerminal get() = phase == RdpkgImportRunPhase.COMPLETE || phase == RdpkgImportRunPhase.FAILED
    val isConsumed get() = isTerminal && finishedAtMs > 0L && consumedFinishedAtMs == finishedAtMs
}

class RdpkgImportRunStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load() = RdpkgImportRunState(
        phase = runCatching { RdpkgImportRunPhase.valueOf(prefs.getString(KEY_PHASE, RdpkgImportRunPhase.IDLE.name)!!) }
            .getOrDefault(RdpkgImportRunPhase.IDLE),
        message = prefs.getString(KEY_MESSAGE, "").orEmpty(),
        projectId = prefs.getString(KEY_PROJECT_ID, null),
        packageUri = prefs.getString(KEY_PACKAGE_URI, null),
        packageId = prefs.getString(KEY_PACKAGE_ID, null),
        progressStage = prefs.getString(KEY_PROGRESS_STAGE, "").orEmpty(),
        progressCurrent = prefs.getInt(KEY_PROGRESS_CURRENT, 0),
        progressTotal = prefs.getInt(KEY_PROGRESS_TOTAL, 0),
        startedAtMs = prefs.getLong(KEY_STARTED_AT, 0L),
        finishedAtMs = prefs.getLong(KEY_FINISHED_AT, 0L),
        consumedFinishedAtMs = prefs.getLong(KEY_CONSUMED_FINISHED_AT, 0L),
    )

    @Synchronized
    fun begin(projectId: String, packageUri: String): Boolean {
        if (load().isRunning) return false
        prefs.edit().clear()
            .putString(KEY_PHASE, RdpkgImportRunPhase.IMPORTING.name)
            .putString(KEY_MESSAGE, "Імпортую .rdpkg…")
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_PACKAGE_URI, packageUri)
            .putString(KEY_PROGRESS_STAGE, "Читаю пакет…")
            .putInt(KEY_PROGRESS_CURRENT, 0)
            .putInt(KEY_PROGRESS_TOTAL, 0)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .commit()
        return true
    }

    fun update(message: String) {
        prefs.edit()
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun updateProgress(
        progress: OperationProgress,
    ) {
        prefs.edit().apply {
            putString(
                KEY_PROGRESS_STAGE,
                progress.compactStage(),
            )
            if (progress.isDeterminate) {
                putInt(
                    KEY_PROGRESS_CURRENT,
                    progress.normalizedCurrent ?: 0,
                )
                putInt(
                    KEY_PROGRESS_TOTAL,
                    progress.total ?: 0,
                )
            } else {
                putInt(
                    KEY_PROGRESS_CURRENT,
                    0,
                )
                putInt(
                    KEY_PROGRESS_TOTAL,
                    0,
                )
            }
        }.apply()
    }

    fun complete(packageId: String, message: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgImportRunPhase.COMPLETE.name)
            .putString(KEY_MESSAGE, message)
            .putString(KEY_PACKAGE_ID, packageId)
            .putString(KEY_PROGRESS_STAGE, "Готово")
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    fun fail(message: String) {
        prefs.edit()
            .putString(KEY_PHASE, RdpkgImportRunPhase.FAILED.name)
            .putString(KEY_MESSAGE, message)
            .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
            .apply()
    }

    @Synchronized
    fun consume(finishedAtMs: Long): Boolean {
        val state = load()
        if (!state.isTerminal || state.finishedAtMs != finishedAtMs || state.isConsumed) return false
        return prefs.edit().putLong(KEY_CONSUMED_FINISHED_AT, finishedAtMs).commit()
    }

    companion object {
        private const val PREFS = "renault_docs_rdpkg_import_run"
        private const val KEY_PHASE = "phase"
        private const val KEY_MESSAGE = "message"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_PACKAGE_URI = "package_uri"
        private const val KEY_PACKAGE_ID = "package_id"
        private const val KEY_PROGRESS_STAGE = "progress_stage"
        private const val KEY_PROGRESS_CURRENT = "progress_current"
        private const val KEY_PROGRESS_TOTAL = "progress_total"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_FINISHED_AT = "finished_at"
        private const val KEY_CONSUMED_FINISHED_AT = "consumed_finished_at"
    }
}
