package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CompletedNotificationRecoveryPolicyTest {
    private fun record(sequence: Long, dismissed: Boolean = false) =
        CompletedNotificationRecord(
            sequence = sequence,
            eventKey = "native:megane-ii:$sequence",
            projectId = "megane-ii",
            title = "Renault Docs · .rdpkg готовий",
            text = "NT8227A · 303 native",
            dismissed = dismissed,
        )

    @Test
    fun restoresOnlyMissingNotDismissedResultsInChronologicalOrder() {
        val records = listOf(
            record(3),
            record(0, dismissed = true),
            record(2),
            record(1),
        )
        val result = CompletedNotificationRecoveryPolicy.missingUndismissed(
            saved = records,
            activeNotificationIds = setOf(CompletedNotificationSlotPolicy.notificationId(2)),
        )
        assertEquals(listOf(1L, 3L), result.map { it.sequence })
    }

    @Test
    fun doesNotRepostAnythingWhenShadeAlreadyContainsCurrentResults() {
        val entries = listOf(record(0), record(1))
        val active = entries.map {
            CompletedNotificationSlotPolicy.notificationId(it.sequence)
        }.toSet()
        assertEquals(
            emptyList<CompletedNotificationRecord>(),
            CompletedNotificationRecoveryPolicy.missingUndismissed(entries, active),
        )
    }

    @Test
    fun neverRestoresManuallyDismissedOrInvalidRecords() {
        val invalid = record(0).copy(eventKey = "")
        val dismissed = record(1, dismissed = true)
        val selected = CompletedNotificationRecoveryPolicy.missingUndismissed(
            listOf(invalid, dismissed), emptySet(),
        )
        assertFalse(selected.isNotEmpty())
    }
}
