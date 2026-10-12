package com.saney.renaultdocs

import android.content.Context
import org.json.JSONArray

enum class ProjectFolderExportPhase { IDLE, EXPORTING, COMPLETE, FAILED, CANCELLED }

data class ProjectFolderExportState(
    val phase: ProjectFolderExportPhase = ProjectFolderExportPhase.IDLE,
    val projectId: String = "",
    val treeUri: String = "",
    val volumeIds: List<String> = emptyList(),
    val completed: Int = 0,
    val skipped: Int = 0,
    val message: String = "",
    val finishedAt: Long = 0L,
    val dismissedAt: Long = 0L,
) {
    val isRunning: Boolean get() = phase == ProjectFolderExportPhase.EXPORTING
    val isTerminal: Boolean get() =
        phase == ProjectFolderExportPhase.COMPLETE ||
            phase == ProjectFolderExportPhase.FAILED ||
            phase == ProjectFolderExportPhase.CANCELLED
    val dismissed: Boolean get() = isTerminal && finishedAt != 0L && finishedAt == dismissedAt
}

class ProjectFolderExportRunStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "renault_docs_project_folder_export", Context.MODE_PRIVATE,
    )

    @Synchronized
    fun load(): ProjectFolderExportState = ProjectFolderExportState(
        phase = runCatching {
            ProjectFolderExportPhase.valueOf(
                prefs.getString("phase", "IDLE") ?: "IDLE",
            )
        }.getOrDefault(ProjectFolderExportPhase.IDLE),
        projectId = prefs.getString("project", "").orEmpty(),
        treeUri = prefs.getString("tree", "").orEmpty(),
        volumeIds = runCatching {
            val array = JSONArray(prefs.getString("volumes", "[]"))
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList()),
        completed = prefs.getInt("completed", 0),
        skipped = prefs.getInt("skipped", 0),
        message = prefs.getString("message", "").orEmpty(),
        finishedAt = prefs.getLong("finished", 0L),
        dismissedAt = prefs.getLong("dismissed", 0L),
    )

    @Synchronized
    fun begin(projectId: String, treeUri: String, volumeIds: List<String>): Boolean {
        if (load().isRunning || volumeIds.isEmpty() || volumeIds.distinct().size != volumeIds.size) {
            return false
        }
        val array = JSONArray()
        volumeIds.forEach { array.put(it) }
        return prefs.edit().clear()
            .putString("phase", ProjectFolderExportPhase.EXPORTING.name)
            .putString("project", projectId)
            .putString("tree", treeUri)
            .putString("volumes", array.toString())
            .putString("message", "Готую окремі томи…")
            .commit()
    }

    fun progress(completed: Int, skipped: Int, message: String) {
        if (!load().isRunning) return
        prefs.edit()
            .putInt("completed", completed)
            .putInt("skipped", skipped)
            .putString("message", message)
            .apply()
    }

    fun complete(completed: Int, skipped: Int, message: String) {
        if (!load().isRunning) return
        prefs.edit()
            .putString("phase", ProjectFolderExportPhase.COMPLETE.name)
            .putInt("completed", completed)
            .putInt("skipped", skipped)
            .putString("message", message)
            .putLong("finished", System.currentTimeMillis())
            .apply()
    }

    fun fail(message: String) {
        if (!load().isRunning) return
        prefs.edit()
            .putString("phase", ProjectFolderExportPhase.FAILED.name)
            .putString("message", message)
            .putLong("finished", System.currentTimeMillis())
            .apply()
    }

    /** #181: the worker observes this between files and inside long copy loops. */
    fun requestCancel(): Boolean {
        if (!load().isRunning) return false
        return prefs.edit().putBoolean("cancel_requested", true).commit()
    }

    fun isCancelRequested(): Boolean = prefs.getBoolean("cancel_requested", false)

    fun cancelled(completed: Int, skipped: Int, message: String) {
        if (!load().isRunning) return
        prefs.edit()
            .putString("phase", ProjectFolderExportPhase.CANCELLED.name)
            .putInt("completed", completed)
            .putInt("skipped", skipped)
            .putString("message", message)
            .putLong("finished", System.currentTimeMillis())
            .apply()
    }

    @Synchronized
    fun dismiss(finishedAt: Long): Boolean {
        val current = load()
        if (!current.isTerminal || current.finishedAt != finishedAt) return false
        return prefs.edit().putLong("dismissed", finishedAt).commit()
    }
}
