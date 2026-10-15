package com.saney.renaultdocs

/** Plain helpers for the .rdproject import screen and service (no Android types). */
object RdprojectImportProgress {
    /**
     * TODO №3 (M30s 2026-10-14 20:36–20:39): the «Етап» bar jumped 1 % → 44 % → 40 % →
     * 70 % — it showed the share of the file while reading and one volume's install
     * while installing. The bar is now only the share of the whole file already passed,
     * in [scale] units, and never goes back.
     */
    fun fileBarValue(
        bytesRead: Long,
        fileSize: Long,
        previous: Int,
        scale: Int,
    ): Int {
        if (fileSize <= 0L) return maxOf(previous, 0)
        val now = (bytesRead.coerceIn(0L, fileSize) * scale / fileSize).toInt()
        return maxOf(now, previous)
    }

    /** No report for this long and no worker in this process — the run is gone. */
    const val ORPHAN_AFTER_MS = 2L * 60L * 1000L

    /**
     * Phone QA 2026-10-15 15:00 (M30s, daily restart at 6:00): a run left RUNNING by a
     * killed process. A fresh run is never treated so: right after the start there is a
     * short pause before the service reports, so the last report must be old too.
     */
    fun isOrphaned(
        running: Boolean,
        workerAlive: Boolean,
        nowMs: Long,
        lastBeatMs: Long,
    ): Boolean =
        running && !workerAlive && (lastBeatMs <= 0L || nowMs - lastBeatMs >= ORPHAN_AFTER_MS)

    fun interruptedMessage(importedCount: Int): String =
        "Імпорт обірвався: телефон перезавантажився або Android зупинив застосунок. " +
            (if (importedCount > 0) "Уже додано томів: $importedCount — вони лишаються. " else "") +
            "Відкрий файл ще раз — додадуться лише томи, яких бракує."

    /** Install progress of the current volume as a whole percent — shown in text. */
    fun installPercent(progress: OperationProgress): Int? {
        val total = progress.total ?: return null
        val current = progress.normalizedCurrent ?: return null
        if (total <= 0) return null
        return (current.toLong() * 100 / total).toInt()
    }

    /**
     * Phone QA 2026-10-14 23:01 (M30s «Мої файли»): what the other app sent — the
     * address kind and the file type — so a screenshot shows why a file did not open.
     * Never the path.
     */
    fun describeIncoming(
        scheme: String?,
        type: String?,
    ): String {
        val kind = scheme?.trim()?.lowercase()?.ifBlank { null } ?: "?"
        val mime = type?.trim()?.ifBlank { null } ?: "не вказано"
        return "Отримано: $kind:// · тип: $mime"
    }
}
