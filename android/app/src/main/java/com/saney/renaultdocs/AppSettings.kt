package com.saney.renaultdocs

import android.content.Context

enum class DatasetOpenMode {
    MODERN,
    CLASSIC,
}

class AppSettings(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    var defaultOpenMode: DatasetOpenMode
        get() =
            runCatching {
                DatasetOpenMode.valueOf(
                    prefs.getString(
                        KEY_DEFAULT_OPEN_MODE,
                        DatasetOpenMode.MODERN.name,
                    ) ?: DatasetOpenMode.MODERN.name
                )
            }.getOrDefault(
                DatasetOpenMode.MODERN,
            )
        set(value) {
            prefs.edit()
                .putString(
                    KEY_DEFAULT_OPEN_MODE,
                    value.name,
                )
                .apply()
        }

    var pdfDefaultZoomPercent: Int
        get() =
            prefs.getInt(
                KEY_PDF_DEFAULT_ZOOM,
                100,
            ).coerceIn(
                50,
                200,
            )
        set(value) {
            prefs.edit()
                .putInt(
                    KEY_PDF_DEFAULT_ZOOM,
                    value.coerceIn(
                        50,
                        200,
                    ),
                )
                .apply()
        }

    var pdfZoomStepPercent: Int
        get() =
            prefs.getInt(
                KEY_PDF_ZOOM_STEP,
                10,
            ).coerceIn(
                5,
                50,
            )
        set(value) {
            prefs.edit()
                .putInt(
                    KEY_PDF_ZOOM_STEP,
                    value.coerceIn(
                        5,
                        50,
                    ),
                )
                .apply()
        }

    var projectAddPanelPinned: Boolean
        get() =
            prefs.getBoolean(
                KEY_PROJECT_ADD_PANEL_PINNED,
                false,
            )
        set(value) {
            prefs.edit()
                .putBoolean(
                    KEY_PROJECT_ADD_PANEL_PINNED,
                    value,
                )
                .apply()
        }

    val backupTreeUri: String?
        get() =
            prefs.getString(
                KEY_BACKUP_TREE_URI,
                null,
            )

    val backupTreeName: String?
        get() =
            prefs.getString(
                KEY_BACKUP_TREE_NAME,
                null,
            )

    fun setBackupFolder(
        uri: String,
        name: String?,
    ) {
        prefs.edit()
            .putString(
                KEY_BACKUP_TREE_URI,
                uri,
            )
            .putString(
                KEY_BACKUP_TREE_NAME,
                name,
            )
            .apply()
    }

    fun clearBackupFolder() {
        prefs.edit()
            .remove(
                KEY_BACKUP_TREE_URI,
            )
            .remove(
                KEY_BACKUP_TREE_NAME,
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "renault_docs_settings"

        private const val KEY_DEFAULT_OPEN_MODE =
            "default_open_mode"

        private const val KEY_PDF_DEFAULT_ZOOM =
            "pdf_default_zoom_percent"

        private const val KEY_PDF_ZOOM_STEP =
            "pdf_zoom_step_percent"

        private const val KEY_BACKUP_TREE_URI =
            "backup_tree_uri"

        private const val KEY_BACKUP_TREE_NAME =
            "backup_tree_name"
    }
}
