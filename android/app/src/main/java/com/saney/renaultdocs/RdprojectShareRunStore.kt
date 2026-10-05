package com.saney.renaultdocs

import android.content.Context

enum class RdprojectShareRunPhase {
    IDLE,
    PREPARING,
    COMPLETE,
    FAILED,
}

data class RdprojectShareRunState(
    val phase: RdprojectShareRunPhase = RdprojectShareRunPhase.IDLE,
    val projectId: String? = null,
    val message: String = "",
    val progressStage: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val preparedPath: String? = null,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val chooserLaunchedFinishedAtMs: Long = 0L,
    val dismissedFinishedAtMs: Long = 0L,
) {
    val isRunning: Boolean
        get() =
            phase ==
                RdprojectShareRunPhase.PREPARING

    val isTerminal: Boolean
        get() =
            phase in
                setOf(
                    RdprojectShareRunPhase.COMPLETE,
                    RdprojectShareRunPhase.FAILED,
                )

    val isChooserLaunched: Boolean
        get() =
            isTerminal &&
                finishedAtMs >
                    0L &&
                chooserLaunchedFinishedAtMs ==
                    finishedAtMs

    val isTerminalDismissed: Boolean
        get() =
            isTerminal &&
                finishedAtMs >
                    0L &&
                dismissedFinishedAtMs ==
                    finishedAtMs
}

class RdprojectShareRunStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE,
            )

    fun load(): RdprojectShareRunState =
        RdprojectShareRunState(
            phase =
                runCatching {
                    RdprojectShareRunPhase.valueOf(
                        prefs.getString(
                            KEY_PHASE,
                            RdprojectShareRunPhase.IDLE.name,
                        ) ?: RdprojectShareRunPhase.IDLE.name,
                    )
                }.getOrDefault(
                    RdprojectShareRunPhase.IDLE,
                ),
            projectId =
                prefs.getString(
                    KEY_PROJECT_ID,
                    null,
                ),
            message =
                prefs.getString(
                    KEY_MESSAGE,
                    "",
                ).orEmpty(),
            progressStage =
                prefs.getString(
                    KEY_PROGRESS_STAGE,
                    "",
                ).orEmpty(),
            progressCurrent =
                prefs.getInt(
                    KEY_PROGRESS_CURRENT,
                    0,
                ),
            progressTotal =
                prefs.getInt(
                    KEY_PROGRESS_TOTAL,
                    0,
                ),
            preparedPath =
                prefs.getString(
                    KEY_PREPARED_PATH,
                    null,
                ),
            startedAtMs =
                prefs.getLong(
                    KEY_STARTED_AT,
                    0L,
                ),
            finishedAtMs =
                prefs.getLong(
                    KEY_FINISHED_AT,
                    0L,
                ),
            chooserLaunchedFinishedAtMs =
                prefs.getLong(
                    KEY_CHOOSER_LAUNCHED_FINISHED_AT,
                    0L,
                ),
            dismissedFinishedAtMs =
                prefs.getLong(
                    KEY_DISMISSED_FINISHED_AT,
                    0L,
                ),
        )

    @Synchronized
    fun begin(
        projectId: String,
    ): Boolean {
        if (
            load().isRunning
        ) {
            return false
        }

        return prefs.edit()
            .clear()
            .putString(
                KEY_PHASE,
                RdprojectShareRunPhase.PREPARING.name,
            )
            .putString(
                KEY_PROJECT_ID,
                projectId,
            )
            .putString(
                KEY_MESSAGE,
                "Готую проєкт…",
            )
            .putString(
                KEY_PROGRESS_STAGE,
                "Готую…",
            )
            .putInt(
                KEY_PROGRESS_CURRENT,
                0,
            )
            .putInt(
                KEY_PROGRESS_TOTAL,
                0,
            )
            .putLong(
                KEY_STARTED_AT,
                System.currentTimeMillis(),
            )
            .commit()
    }

    fun updateProgress(
        progress: OperationProgress,
    ) {
        prefs.edit().apply {
            putString(
                KEY_PROGRESS_STAGE,
                progress.compactStage(),
            )
            putString(
                KEY_MESSAGE,
                progress.displayText(),
            )

            if (
                progress.isDeterminate
            ) {
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

    fun complete(
        preparedPath: String,
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                RdprojectShareRunPhase.COMPLETE.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putString(
                KEY_PROGRESS_STAGE,
                "Готово",
            )
            .putString(
                KEY_PREPARED_PATH,
                preparedPath,
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
                RdprojectShareRunPhase.FAILED.name,
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

    @Synchronized
    fun markChooserLaunched(
        finishedAtMs: Long,
    ): Boolean {
        val state =
            load()

        if (
            !state.isTerminal ||
            state.finishedAtMs !=
                finishedAtMs ||
            state.isChooserLaunched
        ) {
            return false
        }

        return prefs.edit()
            .putLong(
                KEY_CHOOSER_LAUNCHED_FINISHED_AT,
                finishedAtMs,
            )
            .commit()
    }

    @Synchronized
    fun dismissTerminal(
        finishedAtMs: Long,
    ): Boolean {
        val state =
            load()

        if (
            !state.isTerminal ||
            state.finishedAtMs !=
                finishedAtMs
        ) {
            return false
        }

        return prefs.edit()
            .putLong(
                KEY_DISMISSED_FINISHED_AT,
                finishedAtMs,
            )
            .commit()
    }

    companion object {
        private const val PREFS =
            "renault_docs_rdproject_share_run"

        private const val KEY_PHASE =
            "phase"
        private const val KEY_PROJECT_ID =
            "project_id"
        private const val KEY_MESSAGE =
            "message"
        private const val KEY_PROGRESS_STAGE =
            "progress_stage"
        private const val KEY_PROGRESS_CURRENT =
            "progress_current"
        private const val KEY_PROGRESS_TOTAL =
            "progress_total"
        private const val KEY_PREPARED_PATH =
            "prepared_path"
        private const val KEY_STARTED_AT =
            "started_at"
        private const val KEY_FINISHED_AT =
            "finished_at"
        private const val KEY_CHOOSER_LAUNCHED_FINISHED_AT =
            "chooser_launched_finished_at"
        private const val KEY_DISMISSED_FINISHED_AT =
            "dismissed_finished_at"
    }
}
