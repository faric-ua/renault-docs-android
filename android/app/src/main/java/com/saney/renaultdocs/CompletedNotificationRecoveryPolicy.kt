package com.saney.renaultdocs

/** User-visible terminal results only; never represents ongoing foreground work. */
internal data class CompletedNotificationRecord(
    val sequence: Long,
    val eventKey: String,
    val projectId: String?,
    val title: String,
    val text: String,
    val dismissed: Boolean = false,
)

internal object CompletedNotificationRecoveryPolicy {
    fun missingUndismissed(
        saved: List<CompletedNotificationRecord>,
        activeNotificationIds: Set<Int>,
    ): List<CompletedNotificationRecord> =
        saved
            .filter { !it.dismissed && it.eventKey.isNotBlank() }
            .filter {
                CompletedNotificationSlotPolicy.notificationId(it.sequence) !in
                    activeNotificationIds
            }
            .sortedBy { it.sequence }
}
