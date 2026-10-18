package com.saney.renaultdocs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * TODO №36 (faric 2026-10-18 21:40, варіант А): після імпорту з каталогу велика зелена
 * картка — лише перший раз; при наступному відкритті каталогу вона згортається в тихий
 * сірий рядок «Останній імпорт: NT8218A · 18.10, 21:22». × прибирає й рядок.
 */
object LastImportLine {
    fun text(
        documentCodes: List<String>,
        finishedAtMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val codes =
            documentCodes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val what =
            when {
                codes.isEmpty() -> "томи з каталогу"
                codes.size <= 2 -> codes.joinToString(", ")
                else -> codes.first() + " і ще " + (codes.size - 1)
            }
        val time =
            if (finishedAtMs > 0L) {
                " · " +
                    SimpleDateFormat("dd.MM, HH:mm", Locale.ROOT)
                        .apply { this.timeZone = timeZone }
                        .format(Date(finishedAtMs))
            } else {
                ""
            }
        return "Останній імпорт: $what$time"
    }
}
