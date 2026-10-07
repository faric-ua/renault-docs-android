package com.saney.renaultdocs

import android.content.Context

enum class NativeRdpkgRunPhase {
    IDLE,
    PREPARING,
    IMPORTING,
    COMPLETE,
    ALREADY_PRESENT,
    FAILED,
    CANCELLED,
}

enum class NativeRdpkgSourceKind {
    RAW_TREE,
    ARCHIVE_FILE,
}

data class NativeRdpkgRunState(
    val phase: NativeRdpkgRunPhase = NativeRdpkgRunPhase.IDLE,
    val message: String = "",
    val projectId: String? = null,
    val sourceUri: String? = null,
    val sourceName: String? = null,
    val sourceKind: NativeRdpkgSourceKind = NativeRdpkgSourceKind.RAW_TREE,
    val destinationUri: String? = null,
    val packageId: String? = null,
    val volumeId: String? = null,
    val volumeTitle: String? = null,
    val sha256: String? = null,
    val filesTotal: Int = 0,
    val changedFiles: Int = 0,
    val changesTotal: Int = 0,
    val progressStage: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val cancelRequested: Boolean = false,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val dismissedFinishedAtMs: Long = 0L,
) {
    val isRunning: Boolean
        get() =
            phase in
                setOf(
                    NativeRdpkgRunPhase.PREPARING,
                    NativeRdpkgRunPhase.IMPORTING,
                )

    val isTerminal: Boolean
        get() =
            phase in
                setOf(
                    NativeRdpkgRunPhase.COMPLETE,
                    NativeRdpkgRunPhase.ALREADY_PRESENT,
                    NativeRdpkgRunPhase.FAILED,
                    NativeRdpkgRunPhase.CANCELLED,
                )

    val isTerminalDismissed: Boolean
        get() =
            isTerminal &&
                finishedAtMs > 0L &&
                dismissedFinishedAtMs == finishedAtMs
}

class NativeRdpkgRunStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )

    private val launchPrefs =
        context.applicationContext
            .getSharedPreferences(
                LAUNCH_GUARD_PREFS_NAME,
                Context.MODE_PRIVATE,
            )

    @Synchronized
    fun claimStartRequest(
        requestId: String,
    ): Boolean {
        if (
            requestId.isBlank()
        ) {
            return false
        }

        if (
            launchPrefs.getString(
                KEY_LAST_START_REQUEST_ID,
                null,
            ) ==
            requestId
        ) {
            return false
        }

        return launchPrefs.edit()
            .putString(
                KEY_LAST_START_REQUEST_ID,
                requestId,
            )
            .commit()
    }

    fun load(): NativeRdpkgRunState =
        NativeRdpkgRunState(
            phase =
                runCatching {
                    NativeRdpkgRunPhase.valueOf(
                        prefs.getString(
                            KEY_PHASE,
                            NativeRdpkgRunPhase.IDLE.name,
                        )
                            ?: NativeRdpkgRunPhase.IDLE.name,
                    )
                }.getOrDefault(
                    NativeRdpkgRunPhase.IDLE,
                ),
            message =
                prefs.getString(
                    KEY_MESSAGE,
                    "",
                ).orEmpty(),
            projectId =
                prefs.getString(
                    KEY_PROJECT_ID,
                    null,
                ),
            sourceUri =
                prefs.getString(
                    KEY_SOURCE_URI,
                    null,
                ),
            sourceName =
                prefs.getString(
                    KEY_SOURCE_NAME,
                    null,
                ),
            sourceKind =
                runCatching {
                    NativeRdpkgSourceKind.valueOf(
                        prefs.getString(
                            KEY_SOURCE_KIND,
                            NativeRdpkgSourceKind.RAW_TREE.name,
                        )
                            ?: NativeRdpkgSourceKind.RAW_TREE.name,
                    )
                }.getOrDefault(
                    NativeRdpkgSourceKind.RAW_TREE,
                ),
            destinationUri =
                prefs.getString(
                    KEY_DESTINATION_URI,
                    null,
                ),
            packageId =
                prefs.getString(
                    KEY_PACKAGE_ID,
                    null,
                ),
            volumeId =
                prefs.getString(
                    KEY_VOLUME_ID,
                    null,
                ),
            volumeTitle =
                prefs.getString(
                    KEY_VOLUME_TITLE,
                    null,
                ),
            sha256 =
                prefs.getString(
                    KEY_SHA256,
                    null,
                ),
            filesTotal =
                prefs.getInt(
                    KEY_FILES_TOTAL,
                    0,
                ),
            changedFiles =
                prefs.getInt(
                    KEY_CHANGED_FILES,
                    0,
                ),
            changesTotal =
                prefs.getInt(
                    KEY_CHANGES_TOTAL,
                    0,
                ),
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
            cancelRequested =
                prefs.getBoolean(
                    KEY_CANCEL_REQUESTED,
                    false,
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
            dismissedFinishedAtMs =
                prefs.getLong(
                    KEY_DISMISSED_FINISHED_AT,
                    0L,
                ),
        )

    fun begin(
        projectId: String,
        sourceUri: String,
        sourceName: String,
        sourceKind: NativeRdpkgSourceKind = NativeRdpkgSourceKind.RAW_TREE,
        destinationUri: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.PREPARING.name,
            )
            .putString(
                KEY_MESSAGE,
                "Починаю Kotlin-native підготовку…",
            )
            .putString(
                KEY_PROJECT_ID,
                projectId,
            )
            .putString(
                KEY_SOURCE_URI,
                sourceUri,
            )
            .putString(
                KEY_SOURCE_NAME,
                sourceName,
            )
            .putString(
                KEY_SOURCE_KIND,
                sourceKind.name,
            )
            .putString(
                KEY_DESTINATION_URI,
                destinationUri,
            )
            .remove(
                KEY_PACKAGE_ID,
            )
            .remove(
                KEY_VOLUME_ID,
            )
            .remove(
                KEY_VOLUME_TITLE,
            )
            .remove(
                KEY_SHA256,
            )
            .putInt(
                KEY_FILES_TOTAL,
                0,
            )
            .putInt(
                KEY_CHANGED_FILES,
                0,
            )
            .putInt(
                KEY_CHANGES_TOTAL,
                0,
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
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                false,
            )
            .putLong(
                KEY_STARTED_AT,
                System.currentTimeMillis(),
            )
            .putLong(
                KEY_FINISHED_AT,
                0L,
            )
            .remove(
                KEY_DISMISSED_FINISHED_AT,
            )
            .apply()
    }

    fun resumeAfterProcessRestart() {
        val state =
            load()

        if (
            !state.isRunning
        ) {
            return
        }

        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.PREPARING.name,
            )
            .putString(
                KEY_MESSAGE,
                "Відновлюю підготовку після перезапуску Android…",
            )
            .putString(
                KEY_PROGRESS_STAGE,
                "Відновлюю…",
            )
            .putInt(
                KEY_PROGRESS_CURRENT,
                0,
            )
            .putInt(
                KEY_PROGRESS_TOTAL,
                0,
            )
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                false,
            )
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

    fun updatePreparing(
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.PREPARING.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .apply()
    }

    fun updateImporting(
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.IMPORTING.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .apply()
    }

    fun complete(
        message: String,
        packageId: String,
        volumeId: String,
        volumeTitle: String,
        sha256: String,
        filesTotal: Int,
        changedFiles: Int,
        changesTotal: Int,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.COMPLETE.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putString(
                KEY_PACKAGE_ID,
                packageId,
            )
            .putString(
                KEY_VOLUME_ID,
                volumeId,
            )
            .putString(
                KEY_VOLUME_TITLE,
                volumeTitle,
            )
            .putString(
                KEY_SHA256,
                sha256,
            )
            .putInt(
                KEY_FILES_TOTAL,
                filesTotal,
            )
            .putInt(
                KEY_CHANGED_FILES,
                changedFiles,
            )
            .putInt(
                KEY_CHANGES_TOTAL,
                changesTotal,
            )
            .putString(
                KEY_PROGRESS_STAGE,
                "Готово",
            )
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                false,
            )
            .putLong(
                KEY_FINISHED_AT,
                System.currentTimeMillis(),
            )
            .apply()
    }

    fun markAlreadyPresent(
        message: String,
        volumeId: String,
        volumeTitle: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                NativeRdpkgRunPhase.ALREADY_PRESENT.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putString(
                KEY_VOLUME_ID,
                volumeId,
            )
            .putString(
                KEY_VOLUME_TITLE,
                volumeTitle,
            )
            .putString(
                KEY_PROGRESS_STAGE,
                "Вже є",
            )
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                false,
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
        finish(
            phase =
                NativeRdpkgRunPhase.FAILED,
            message =
                message,
        )
    }

    fun markCancelled(
        message: String,
    ) {
        finish(
            phase =
                NativeRdpkgRunPhase.CANCELLED,
            message =
                message,
        )
    }

    private fun finish(
        phase: NativeRdpkgRunPhase,
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                phase.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                false,
            )
            .putLong(
                KEY_FINISHED_AT,
                System.currentTimeMillis(),
            )
            .apply()
    }

    fun dismissTerminal(
        finishedAtMs: Long,
    ): Boolean {
        val state =
            load()

        if (
            !state.isTerminal ||
            state.finishedAtMs <= 0L ||
            state.finishedAtMs != finishedAtMs
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

    fun requestCancel() {
        prefs.edit()
            .putBoolean(
                KEY_CANCEL_REQUESTED,
                true,
            )
            .apply()
    }

    fun isCancelRequested(): Boolean =
        prefs.getBoolean(
            KEY_CANCEL_REQUESTED,
            false,
        )

    fun clearFinished() {
        if (
            !load().isRunning
        ) {
            prefs.edit()
                .clear()
                .apply()
        }
    }

    companion object {
        private const val PREFS_NAME =
            "renault_docs_native_rdpkg_run"

        private const val LAUNCH_GUARD_PREFS_NAME =
            "renault_docs_native_rdpkg_launch_guard"

        private const val KEY_LAST_START_REQUEST_ID =
            "last_start_request_id"

        private const val KEY_PHASE =
            "phase"
        private const val KEY_MESSAGE =
            "message"
        private const val KEY_PROJECT_ID =
            "project_id"
        private const val KEY_SOURCE_URI =
            "source_uri"
        private const val KEY_SOURCE_NAME =
            "source_name"
        private const val KEY_SOURCE_KIND =
            "source_kind"
        private const val KEY_DESTINATION_URI =
            "destination_uri"
        private const val KEY_PACKAGE_ID =
            "package_id"
        private const val KEY_VOLUME_ID =
            "volume_id"
        private const val KEY_VOLUME_TITLE =
            "volume_title"
        private const val KEY_SHA256 =
            "sha256"
        private const val KEY_FILES_TOTAL =
            "files_total"
        private const val KEY_CHANGED_FILES =
            "changed_files"
        private const val KEY_CHANGES_TOTAL =
            "changes_total"
        private const val KEY_PROGRESS_STAGE =
            "progress_stage"
        private const val KEY_PROGRESS_CURRENT =
            "progress_current"
        private const val KEY_PROGRESS_TOTAL =
            "progress_total"
        private const val KEY_CANCEL_REQUESTED =
            "cancel_requested"
        private const val KEY_STARTED_AT =
            "started_at"
        private const val KEY_FINISHED_AT =
            "finished_at"
        private const val KEY_DISMISSED_FINISHED_AT =
            "dismissed_finished_at"
    }
}
