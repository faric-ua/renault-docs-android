package com.saney.renaultdocs

import android.content.Context

/** Saved between screen rotations and app restarts. */
class RdpkgImportQueue(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rdpkg_import_queue", Context.MODE_PRIVATE)

    fun load(): ImportQueueState = ImportQueueState.decode(prefs.getString(KEY, null))

    fun save(state: ImportQueueState) {
        prefs.edit().putString(KEY, state.encode()).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY).commit()
    }

    private companion object {
        const val KEY = "state"
    }
}
