package com.saney.renaultdocs

/** User-visible terminal results only; never represents ongoing foreground work. */
internal data class CompletedNotificationRecord(
    val sequence: Long,
    val eventKey: String,
    val projectId: String?,
    val title: String,
    val text: String,
    val dismissed: Boolean = false,
    /**
     * When the operation finished (0.6.4). After an APK update Android removes
     * the app's notifications and we post them again — without this time the
     * shade showed the moment of re-posting, so old results looked new
     * (owner 2026-10-18 05:36: «время одинакове… два томи додав, а записів 4»).
     * 0 = record from before 0.6.4, time unknown.
     */
    val finishedAtMs: Long = 0L,
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
