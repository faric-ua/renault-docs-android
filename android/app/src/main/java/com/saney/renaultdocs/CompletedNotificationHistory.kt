package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Last 10 user-visible completed results. Never use this for live progress updates.
 *
 * IDs are shared across native .rdpkg creation and .rdpkg import. A persisted ring
 * prevents new results from replacing the previous run and limits notification noise.
 * The operating system still owns user dismissal and notification permissions.
 */
internal object CompletedNotificationHistory {
    private const val PREFS = "renault_completed_notifications_v1"
    private const val KEY_SEQUENCE = "next_sequence"
    private const val KEY_SLOT_PREFIX = "slot_key_"
    private const val CHANNEL_ID = "renault_completed_operations"
    private const val GROUP_KEY = "com.saney.renaultdocs.completed"

    @Synchronized
    fun publish(
        context: Context,
        eventKey: String,
        projectId: String?,
        title: String,
        text: String,
    ): Boolean {
        if (eventKey.isBlank()) return false
        return try {
            publishSafely(context, eventKey, projectId, title, text)
        } catch (error: Exception) {
            // A disabled/blocked Android notification must never fail the
            // already-installed tome or change the operation's result.
            Log.w("RenaultDocs", "Completion notification unavailable", error)
            false
        }
    }

    private fun publishSafely(
        context: Context,
        eventKey: String,
        projectId: String?,
        title: String,
        text: String,
    ): Boolean {

        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Do not re-post on Android redelivery or when finishing the same batch twice.
        if ((0 until CompletedNotificationSlotPolicy.MAX_RESULTS).any { slot ->
                prefs.getString(KEY_SLOT_PREFIX + slot, null) == eventKey
            }) {
            return false
        }

        val seq = prefs.getLong(KEY_SEQUENCE, 0L)
        val slot = CompletedNotificationSlotPolicy.slot(seq)
        val notificationId = CompletedNotificationSlotPolicy.notificationId(seq)
        if (!prefs.edit()
                .putLong(KEY_SEQUENCE, CompletedNotificationSlotPolicy.nextSequence(seq))
                .putString(KEY_SLOT_PREFIX + slot, eventKey)
                .commit()
        ) {
            return false
        }

        val manager = app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Renault Docs · результати",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Останні завершені операції з томами Renault"
            },
        )

        val openIntent = if (!projectId.isNullOrBlank()) {
            ProjectActivity.intent(app, projectId)
        } else {
            Intent(app, MainActivity::class.java)
        }
        val pending = PendingIntent.getActivity(
            app,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.notify(
            notificationId,
            Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_document)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(
                    when (OperationTerminalOutcome.fromTitle(title)) {
                        OperationTerminalOutcome.SUCCESS -> Ui.success
                        OperationTerminalOutcome.FAILED -> Ui.danger
                        OperationTerminalOutcome.CANCELLED -> Ui.warning
                        OperationTerminalOutcome.NEUTRAL -> Ui.muted
                    },
                )
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .build(),
        )

        val summaryIntent = PendingIntent.getActivity(
            app,
            CompletedNotificationSlotPolicy.SUMMARY_ID,
            Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            CompletedNotificationSlotPolicy.SUMMARY_ID,
            Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_document)
                .setContentTitle("Renault Docs · завершені операції")
                .setContentText("Розгорни групу, щоб переглянути результати томів")
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(summaryIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .build(),
        )
        return true
    }
}
