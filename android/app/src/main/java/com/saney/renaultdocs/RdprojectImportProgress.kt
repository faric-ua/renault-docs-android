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

    /**
     * TODO №15 (M30s 2026-10-16 01:42, «Етап: 100%» while «Том 18/18 · NT8494 —
     * встановлюю 31 %» went on for 2 more hours): reading the file is fast, installing a
     * volume on the card is slow, so the share of the file is a bad measure. The main
     * bar is now the share of the chosen volumes done: finished volumes plus the
     * installed part of the current one ([currentPart] in [scale] units). Never goes back.
     */
    fun overallBarValue(
        done: Int,
        total: Int,
        currentPart: Int,
        previous: Int,
        scale: Int,
    ): Int {
        if (total <= 0) return maxOf(previous, 0)
        val finished = done.coerceIn(0, total).toLong()
        val part = if (finished >= total) 0L else currentPart.coerceIn(0, scale).toLong()
        val now = ((finished * scale + part) / total).toInt().coerceIn(0, scale)
        return maxOf(now, previous)
    }

    /** Line above the main bar: «Загалом: томів 17 з 18 · 96 %». */
    fun overallLabel(
        done: Int,
        total: Int,
        barValue: Int,
        scale: Int,
    ): String {
        val percent = if (scale > 0) barValue.coerceIn(0, scale).toLong() * 100 / scale else 0L
        return "Загалом: томів ${done.coerceIn(0, maxOf(total, 0))} з $total · $percent %"
    }

    /** Line above the second bar, for the current volume only (resets on each volume). */
    fun volumeLabel(
        label: String,
        percent: Int?,
    ): String =
        if (percent == null) {
            "Цей том: $label — копіюю з файлу"
        } else {
            "Цей том: $label — ${percent.coerceIn(0, 100)} %"
        }

    /**
     * TODO №13: volumes that already lay unpacked on the phone / card but were missing
     * from the project after a broken-off import; this run added them back.
     */
    fun recoveredMessage(
        count: Int,
        project: String,
    ): String = "Повернуто до проєкту «$project» томів, що вже лежали на телефоні / карті: $count."

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
