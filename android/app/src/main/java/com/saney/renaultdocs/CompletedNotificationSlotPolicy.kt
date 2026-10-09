package com.saney.renaultdocs

/** A bounded ring of notification IDs; live foreground service IDs never enter this range. */
internal object CompletedNotificationSlotPolicy {
    const val MAX_RESULTS = 10
    const val SUMMARY_ID = 48_000
    private const val FIRST_RESULT_ID = 48_001

    fun slot(sequence: Long): Int =
        (sequence.coerceAtLeast(0L) % MAX_RESULTS).toInt()

    fun notificationId(sequence: Long): Int =
        FIRST_RESULT_ID + slot(sequence)

    fun nextSequence(sequence: Long): Long =
        if (sequence == Long.MAX_VALUE) 0L else sequence + 1L
}
