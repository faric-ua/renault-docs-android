package com.saney.renaultdocs

/** A result is recorded only AFTER the package was installed and registered. */
data class NativeRdpkgBatchVolumeResult(
    val label: String,
    val packageId: String,
    val volumeId: String,
    val sha256: String,
    val outputUri: String?,
    val sections: Int,
)

/**
 * Stored independently of the legacy single-package fields, with a versioned
 * private JSON representation. Each entry represents a completed commit.
 * Bound stored lists to avoid oversized SharedPreferences and diagnostic UI.
 */
data class NativeRdpkgBatchReport(
    val completed: List<NativeRdpkgBatchVolumeResult> = emptyList(),
    val skipped: List<String> = emptyList(),
    val omittedCompleted: Int = 0,
    val omittedSkipped: Int = 0,
) {
    val completedCount: Int get() = completed.size + omittedCompleted
    val skippedCount: Int get() = skipped.size + omittedSkipped

    fun afterCompleted(entry: NativeRdpkgBatchVolumeResult): NativeRdpkgBatchReport {
        if (completed.any { it.packageId == entry.packageId }) return this
        return if (completed.size < MAX_RECORDED) {
            copy(completed = completed + entry)
        } else {
            copy(omittedCompleted = omittedCompleted + 1)
        }
    }

    fun afterSkipped(label: String): NativeRdpkgBatchReport =
        if (skipped.size < MAX_RECORDED) copy(skipped = skipped + label)
        else copy(omittedSkipped = omittedSkipped + 1)

    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_RECORDED = 128
    }
}

/** Read-only detail text: never imply that a batch has one Package ID/SHA. */
object NativeRdpkgBatchReportFormatter {
    fun reportLines(state: NativeRdpkgRunState): String {
        val batch = state.batchReport
        if (
            batch != null &&
            state.sourceKind == NativeRdpkgSourceKind.ARCHIVE_FILE &&
            (batch.completedCount > 1 || batch.skippedCount > 0 ||
                state.phase != NativeRdpkgRunPhase.COMPLETE)
        ) {
            return buildString {
                appendLine("Результат: пакетна обробка")
                appendLine("Томів створено: ${batch.completedCount}")
                appendLine("Томів пропущено: ${batch.skippedCount}")
                batch.completed.forEachIndexed { index, volume ->
                    appendLine("")
                    appendLine("Том ${index + 1}: ${volume.label}")
                    appendLine("Статус: створено")
                    appendLine("Package ID: ${volume.packageId}")
                    appendLine("Volume ID: ${volume.volumeId}")
                    appendLine("SHA-256: ${volume.sha256.ifBlank { "недоступний" }}")
                    appendLine("Native розділів: ${volume.sections}")
                    volume.outputUri?.takeIf { it.isNotBlank() }?.let {
                        appendLine("URI пакета: $it")
                    }
                }
                if (batch.omittedCompleted > 0) {
                    appendLine("Не показано ще ${batch.omittedCompleted} завершених томів (обмеження звіту).")
                }
                if (batch.skipped.isNotEmpty()) {
                    appendLine("")
                    appendLine("Пропущені томи:")
                    batch.skipped.forEach { appendLine("• Уже є: $it") }
                }
                if (batch.omittedSkipped > 0) {
                    appendLine("Не показано пропущених томів: ${batch.omittedSkipped}.")
                }
                if (state.phase != NativeRdpkgRunPhase.COMPLETE) {
                    appendLine("Пакетна операція завершилася зі станом: ${state.phase}")
                    appendLine("У списку зазначені лише раніше успішно завершені томи.")
                }
            }.trimEnd()
        }

        val legacyCount = state.volumeTitle
            ?.takeIf { state.sourceKind == NativeRdpkgSourceKind.ARCHIVE_FILE }
            ?.let { Regex("""^(\d+) томів$""").matchEntire(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
        if (legacyCount != null && legacyCount > 1) {
            return "Результат: пакетна обробка\n" +
                "Томів створено: $legacyCount\n" +
                "Дані про всі пакети в цьому старому звіті недоступні. " +
                "Окремі Package ID та SHA-256 не були збережені."
        }

        return buildString {
            appendLine("Том результату: ${state.volumeTitle ?: "немає"}")
            appendLine("Package ID: ${state.packageId?.takeIf { it.isNotBlank() } ?: "немає"}")
            append("SHA-256 результату: ${state.sha256?.takeIf { it.isNotBlank() } ?: "немає"}")
        }
    }
}
