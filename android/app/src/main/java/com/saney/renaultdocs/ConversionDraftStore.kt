package com.saney.renaultdocs

import android.content.Context

data class ConversionDraft(
    val sourceUri: String? = null,
    val sourceName: String? = null,
    val destinationUri: String? = null,
    val destinationName: String? = null,
)

class ConversionDraftStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): ConversionDraft = ConversionDraft(
        sourceUri = prefs.getString(KEY_SOURCE_URI, null),
        sourceName = prefs.getString(KEY_SOURCE_NAME, null),
        destinationUri = prefs.getString(KEY_DESTINATION_URI, null),
        destinationName = prefs.getString(KEY_DESTINATION_NAME, null),
    )

    fun saveSource(
        uri: String,
        name: String?,
    ) {
        prefs.edit()
            .putString(KEY_SOURCE_URI, uri)
            .putString(KEY_SOURCE_NAME, name)
            .apply()
    }

    fun saveDestination(
        uri: String,
        name: String?,
    ) {
        prefs.edit()
            .putString(KEY_DESTINATION_URI, uri)
            .putString(KEY_DESTINATION_NAME, name)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "renault_docs_conversion_draft"
        private const val KEY_SOURCE_URI = "source_uri"
        private const val KEY_SOURCE_NAME = "source_name"
        private const val KEY_DESTINATION_URI = "destination_uri"
        private const val KEY_DESTINATION_NAME = "destination_name"
    }
}
