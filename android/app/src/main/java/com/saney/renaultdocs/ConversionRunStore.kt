package com.saney.renaultdocs

import android.content.Context

enum class ConversionRunPhase {
    IDLE,
    SCANNING,
    PREPARING,
    COPYING,
    PACKAGING,
    VALIDATING,
    FINALIZING,
    COMPLETE,
    FAILED,
    CANCELLED,
}

data class ConversionRunState(
    val phase: ConversionRunPhase = ConversionRunPhase.IDLE,
    val message: String = "",
    val filesTotal: Int = 0,
    val filesDone: Int = 0,
    val changedFiles: Int = 0,
    val changesTotal: Int = 0,
    val sourceUri: String? = null,
    val destinationUri: String? = null,
    val outputFolderName: String? = null,
    val outputTreeUri: String? = null,
    val mergeExisting: Boolean = false,
    val cancelRequested: Boolean = false,
    val startedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
) {
    val isRunning: Boolean
        get() =
            phase in setOf(
                ConversionRunPhase.SCANNING,
                ConversionRunPhase.PREPARING,
                ConversionRunPhase.COPYING,
                ConversionRunPhase.PACKAGING,
                ConversionRunPhase.VALIDATING,
                ConversionRunPhase.FINALIZING,
            )
}

class ConversionRunStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun load(): ConversionRunState =
        ConversionRunState(
            phase =
                runCatching {
                    ConversionRunPhase.valueOf(
                        prefs.getString(
                            KEY_PHASE,
                            ConversionRunPhase.IDLE.name,
                        ) ?: ConversionRunPhase.IDLE.name
                    )
                }.getOrDefault(
                    ConversionRunPhase.IDLE,
                ),
            message =
                prefs.getString(
                    KEY_MESSAGE,
                    "",
                ).orEmpty(),
            filesTotal =
                prefs.getInt(
                    KEY_FILES_TOTAL,
                    0,
                ),
            filesDone =
                prefs.getInt(
                    KEY_FILES_DONE,
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
            sourceUri =
                prefs.getString(
                    KEY_SOURCE_URI,
                    null,
                ),
            destinationUri =
                prefs.getString(
                    KEY_DESTINATION_URI,
                    null,
                ),
            outputFolderName =
                prefs.getString(
                    KEY_OUTPUT_FOLDER_NAME,
                    null,
                ),
            outputTreeUri =
                prefs.getString(
                    KEY_OUTPUT_TREE_URI,
                    null,
                ),
            mergeExisting =
                prefs.getBoolean(
                    KEY_MERGE_EXISTING,
                    false,
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
        sourceUri: String,
        destinationUri: String,
        outputFolderName: String,
        mergeExisting: Boolean = false,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                ConversionRunPhase.SCANNING.name,
            )
            .putString(
                KEY_MESSAGE,
                "Сканую source…",
            )
            .putInt(
                KEY_FILES_TOTAL,
                0,
            )
            .putInt(
                KEY_FILES_DONE,
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
                KEY_SOURCE_URI,
                sourceUri,
            )
            .putString(
                KEY_DESTINATION_URI,
                destinationUri,
            )
            .putString(
                KEY_OUTPUT_FOLDER_NAME,
                outputFolderName,
            )
            .putBoolean(
                KEY_MERGE_EXISTING,
                mergeExisting,
            )
            .remove(
                KEY_OUTPUT_TREE_URI,
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

    fun update(
        phase: ConversionRunPhase,
        message: String,
        filesTotal: Int? = null,
        filesDone: Int? = null,
        changedFiles: Int? = null,
        changesTotal: Int? = null,
    ) {
        prefs.edit().apply {
            putString(
                KEY_PHASE,
                phase.name,
            )
            putString(
                KEY_MESSAGE,
                message,
            )

            filesTotal?.let {
                putInt(
                    KEY_FILES_TOTAL,
                    it,
                )
            }
            filesDone?.let {
                putInt(
                    KEY_FILES_DONE,
                    it,
                )
            }
            changedFiles?.let {
                putInt(
                    KEY_CHANGED_FILES,
                    it,
                )
            }
            changesTotal?.let {
                putInt(
                    KEY_CHANGES_TOTAL,
                    it,
                )
            }
        }.apply()
    }

    fun complete(
        outputTreeUri: String,
        message: String,
        filesTotal: Int,
        changedFiles: Int,
        changesTotal: Int,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                ConversionRunPhase.COMPLETE.name,
            )
            .putString(
                KEY_MESSAGE,
                message,
            )
            .putInt(
                KEY_FILES_TOTAL,
                filesTotal,
            )
            .putInt(
                KEY_FILES_DONE,
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
                KEY_OUTPUT_TREE_URI,
                outputTreeUri,
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
        prefs.edit()
            .putString(
                KEY_PHASE,
                ConversionRunPhase.FAILED.name,
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

    fun markCancelled(
        message: String,
    ) {
        prefs.edit()
            .putString(
                KEY_PHASE,
                ConversionRunPhase.CANCELLED.name,
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
        if (!load().isRunning) {
            prefs.edit().clear().apply()
        }
    }

    companion object {
        private const val PREFS_NAME =
            "renault_docs_conversion_run"

        private const val KEY_PHASE =
            "phase"
        private const val KEY_MESSAGE =
            "message"
        private const val KEY_FILES_TOTAL =
            "files_total"
        private const val KEY_FILES_DONE =
            "files_done"
        private const val KEY_CHANGED_FILES =
            "changed_files"
        private const val KEY_CHANGES_TOTAL =
            "changes_total"
        private const val KEY_SOURCE_URI =
            "source_uri"
        private const val KEY_DESTINATION_URI =
            "destination_uri"
        private const val KEY_OUTPUT_FOLDER_NAME =
            "output_folder_name"
        private const val KEY_OUTPUT_TREE_URI =
            "output_tree_uri"
        private const val KEY_MERGE_EXISTING =
            "merge_existing"
        private const val KEY_CANCEL_REQUESTED =
            "cancel_requested"
        private const val KEY_STARTED_AT =
            "started_at"
        private const val KEY_FINISHED_AT =
            "finished_at"
    }
}
