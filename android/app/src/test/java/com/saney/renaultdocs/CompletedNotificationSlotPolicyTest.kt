package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class CompletedNotificationSlotPolicyTest {
    @Test
    fun keepsExactlyTenIndependentResultIdentifiers() {
        val ids = (0L until 10L).map(CompletedNotificationSlotPolicy::notificationId)
        assertEquals(10, ids.toSet().size)
        assertEquals(48_001, ids.first())
        assertEquals(48_010, ids.last())
    }

    @Test
    fun eleventhResultReplacesOnlyOldestSlot() {
        assertEquals(
            CompletedNotificationSlotPolicy.notificationId(0L),
            CompletedNotificationSlotPolicy.notificationId(10L),
        )
        assertEquals(
            CompletedNotificationSlotPolicy.notificationId(4L),
            CompletedNotificationSlotPolicy.notificationId(14L),
        )
        assertEquals(48_000, CompletedNotificationSlotPolicy.SUMMARY_ID)
    }

    @Test
    fun legacyForegroundIdsNeverOverlapHistorySlots() {
        val values = (0L until 100L).map(CompletedNotificationSlotPolicy::notificationId)
        for (foregroundId in 3702..3708) {
            org.junit.Assert.assertFalse(foregroundId in values)
        }
    }

    @Test
    fun nextSequenceIsMonotonicUntilRareRollover() {
        assertEquals(11L, CompletedNotificationSlotPolicy.nextSequence(10L))
        assertEquals(0L, CompletedNotificationSlotPolicy.nextSequence(Long.MAX_VALUE))
    }
}
