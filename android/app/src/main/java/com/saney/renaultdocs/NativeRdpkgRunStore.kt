package com.saney.renaultdocs

import android.content.Context

enum class NativeRdpkgRunPhase {
    IDLE,
    PREPARING,
    IMPORTING,
    COMPLETE,
    FAILED,
    CANCELLED,
}

data class NativeRdpkgRunState(
    val phase: NativeRdpkgRunPhase = NativeRdpkgRunPhase.IDLE,
    val message: String = "",
    val projectId: String? = null,
    val sourceUri: String? = null,
    val sourceName: String? = null,
    val destinationUri: String? = null,
    val packageId: String? = null,
    val volumeId: String? = null,
    val volumeTitle: String? = null,
    val sha256: String? = null,
    val filesTotal: Int = 0,
    val changedFiles: Int = 0,
    val changesTotal: Int = 0,
    val cancelRequested: Boolean = false,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
) {
    val isRunning: Boolean
        get() =
            phase in
                setOf(
                    NativeRdpkgRunPhase.PREPARING,
                    NativeRdpkgRunPhase.IMPORTING,
                )
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
        )

    fun begin(
        projectId: String,
        sourceUri: String,
        sourceName: String,
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
            .apply()
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
        private const val KEY_CANCEL_REQUESTED =
            "cancel_requested"
        private const val KEY_STARTED_AT =
            "started_at"
        private const val KEY_FINISHED_AT =
            "finished_at"
    }
}
