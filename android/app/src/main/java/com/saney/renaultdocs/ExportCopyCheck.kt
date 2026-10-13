package com.saney.renaultdocs

/**
 * v0.5.125: pure rules for checking a volume copied into a user-chosen folder.
 *
 * Phone QA 2026-10-13 (v0.5.124, Google Drive folder): 8 of 17 volumes were
 * copied and verified, the 9th failed «Контрольна сума скопійованого тому не
 * збігається». A cloud provider may still be finishing the write when the output
 * stream closes, so one immediate read-back is not proof of a broken copy.
 * The copy is re-read after growing pauses; only then it is reported as broken.
 */
object ExportCopyCheck {
    /** Pause before each read-back attempt: right away, then up to ~1 min in total. */
    val DELAYS_MS: List<Long> = listOf(0L, 2_000L, 5_000L, 10_000L, 15_000L, 30_000L)

    fun mismatchMessage(expectedBytes: Long, readBytes: Long, attempts: Int): String =
        if (readBytes != expectedBytes) {
            "Копія в папці не збіглася з томом після $attempts перевірок: " +
                "прочитано ${mb(readBytes)} із ${mb(expectedBytes)}. Недороблений файл прибрано."
        } else {
            "Копія в папці не збіглася з томом після $attempts перевірок " +
                "(розмір однаковий, вміст інший). Недороблений файл прибрано."
        }

    fun summary(
        completed: Int,
        total: Int,
        verifiedExisting: Int,
        different: Int,
        unreadable: Int = 0,
    ): String =
        buildString {
            append("Готово: $completed із $total")
            if (verifiedExisting > 0) append(" (уже були в папці й збіглися: $verifiedExisting)")
            append(".")
            if (different > 0) append(" Уже є, але інші — не замінено: $different.")
            if (unreadable > 0) append(" Уже є, але не прочитались — не замінено: $unreadable.")
            append(" Каталог не публікувався.")
        }

    fun mb(bytes: Long): String {
        val tenths = (bytes * 10 + 524_288) / 1_048_576
        return "${tenths / 10},${tenths % 10} МБ"
    }
}
