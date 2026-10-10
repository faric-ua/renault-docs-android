package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Bounded record of the last ten completed operations, independent of the
 * Android shade. The OS may remove its notifications when the APK is replaced.
 *
 * Do not restore ongoing progress, nor re-post notifications the user dismissed.
 * Old (pre-0.5.93) slot keys have no saved title/body and cannot be reconstructed.
 */
internal object CompletedNotificationHistory {
    private const val PREFS = "renault_completed_notifications_v1"
    private const val KEY_SEQUENCE = "next_sequence"
    private const val KEY_SLOT_PREFIX = "slot_key_"
    private const val KEY_RECORD_PREFIX = "result_record_"
    private const val KEY_RECORD_VERSION = "last_recorded_app_version"
    private const val CHANNEL_ID = "renault_completed_operations"
    private const val GROUP_KEY = "com.saney.renaultdocs.completed"

    internal const val ACTION_DISMISS_RESULT =
        "com.saney.renaultdocs.COMPLETED_RESULT_DISMISSED"
    internal const val ACTION_DISMISS_GROUP =
        "com.saney.renaultdocs.COMPLETED_GROUP_DISMISSED"
    internal const val EXTRA_EVENT_KEY = "event_key"
    internal const val EXTRA_MAX_SEQUENCE = "max_sequence"

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
            // Notification permission/channels must never fail an installed tome.
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

        if ((0 until CompletedNotificationSlotPolicy.MAX_RESULTS).any { slot ->
                prefs.getString(KEY_SLOT_PREFIX + slot, null) == eventKey
            }) return false

        val seq = prefs.getLong(KEY_SEQUENCE, 0L)
        val slot = CompletedNotificationSlotPolicy.slot(seq)
        val record = CompletedNotificationRecord(
            sequence = seq,
            eventKey = eventKey,
            projectId = projectId,
            title = title,
            text = text,
        )

        // Store the *real* displayed content before posting. Pre-v0.5.93
        // entries only contained event keys and cannot be backfilled safely.
        if (!prefs.edit()
                .putLong(KEY_SEQUENCE, CompletedNotificationSlotPolicy.nextSequence(seq))
                .putString(KEY_SLOT_PREFIX + slot, eventKey)
                .putString(KEY_RECORD_PREFIX + slot, encode(record))
                .putInt(KEY_RECORD_VERSION, BuildConfig.VERSION_CODE)
                .commit()
        ) return false

        val manager = app.getSystemService(NotificationManager::class.java)
        ensureChannel(manager)
        manager.notify(notificationId(record), buildChild(app, record))
        manager.notify(
            CompletedNotificationSlotPolicy.SUMMARY_ID,
            buildSummary(app, seq),
        )
        return true
    }

    /**
     * Run when any Activity comes to foreground after an in-place APK update.
     * Only previously persisted, not-user-dismissed results are recreated;
     * restoring must never restart an archive job or touch project files.
     */
    @Synchronized
    fun restoreAfterPackageUpdate(context: Context) {
        try {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val previousVersion = prefs.getInt(KEY_RECORD_VERSION, -1)
            val currentVersion = BuildConfig.VERSION_CODE
            if (previousVersion == currentVersion) return

            // Upgrade from legacy key-only history: no titles/messages to
            // recover. Record a baseline; do not invent "completed" results.
            if (previousVersion < 0) {
                prefs.edit().putInt(KEY_RECORD_VERSION, currentVersion).commit()
                return
            }

            val manager = app.getSystemService(NotificationManager::class.java)
            if (!manager.areNotificationsEnabled()) return
            ensureChannel(manager)
            val activeIds = manager.activeNotifications.map { it.id }.toSet()
            val saved = readRecords(prefs)
            val toRestore = CompletedNotificationRecoveryPolicy.missingUndismissed(
                saved, activeIds,
            )
            toRestore.forEach { record ->
                manager.notify(notificationId(record), buildChild(app, record))
            }
            if (toRestore.isNotEmpty()) {
                manager.notify(
                    CompletedNotificationSlotPolicy.SUMMARY_ID,
                    buildSummary(app, saved.maxOf { it.sequence }),
                )
            }
            // Mark migration only after posting, to allow safe retry on errors.
            prefs.edit().putInt(KEY_RECORD_VERSION, currentVersion).commit()
        } catch (error: Exception) {
            Log.w("RenaultDocs", "Cannot restore completed notification history", error)
        }
    }

    /** Invoked only by explicit notification deleteIntent, never by an update. */
    @Synchronized
    fun markDismissed(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val eventKey = intent?.getStringExtra(EXTRA_EVENT_KEY)
        val maxSequence = intent?.getLongExtra(EXTRA_MAX_SEQUENCE, -1L) ?: -1L
        val summary = intent?.action == ACTION_DISMISS_GROUP

        val editor = prefs.edit()
        readRecords(prefs).forEach { record ->
            val matching = if (summary) {
                maxSequence >= 0L && record.sequence <= maxSequence
            } else {
                eventKey != null && record.eventKey == eventKey
            }
            if (matching && !record.dismissed) {
                editor.putString(
                    KEY_RECORD_PREFIX + CompletedNotificationSlotPolicy.slot(record.sequence),
                    encode(record.copy(dismissed = true)),
                )
            }
        }
        editor.commit()
    }

    private fun readRecords(
        prefs: android.content.SharedPreferences,
    ): List<CompletedNotificationRecord> =
        (0 until CompletedNotificationSlotPolicy.MAX_RESULTS).mapNotNull { slot ->
            val raw = prefs.getString(KEY_RECORD_PREFIX + slot, null) ?: return@mapNotNull null
            val record = decode(raw) ?: return@mapNotNull null
            // Do not allow a stale record to overwrite a newer slot owner.
            record.takeIf {
                CompletedNotificationSlotPolicy.slot(it.sequence) == slot &&
                    prefs.getString(KEY_SLOT_PREFIX + slot, null) == it.eventKey
            }
        }

    private fun encode(record: CompletedNotificationRecord): String =
        JSONObject()
            .put("sequence", record.sequence)
            .put("event_key", record.eventKey)
            .put("project_id", record.projectId)
            .put("title", record.title)
            .put("text", record.text)
            .put("dismissed", record.dismissed)
            .toString()

    private fun decode(raw: String): CompletedNotificationRecord? =
        runCatching {
            val json = JSONObject(raw)
            CompletedNotificationRecord(
                sequence = json.getLong("sequence"),
                eventKey = json.getString("event_key"),
                projectId = if (json.isNull("project_id")) null else
                    json.getString("project_id").takeIf { it.isNotBlank() },
                title = json.getString("title"),
                text = json.getString("text"),
                dismissed = json.optBoolean("dismissed", false),
            )
        }.getOrNull()

    private fun ensureChannel(manager: NotificationManager) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Renault Docs · результати",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Останні завершені операції з томами Renault"
            },
        )
    }

    private fun notificationId(record: CompletedNotificationRecord): Int =
        CompletedNotificationSlotPolicy.notificationId(record.sequence)

    private fun buildChild(context: Context, record: CompletedNotificationRecord): Notification {
        val id = notificationId(record)
        val openIntent = if (!record.projectId.isNullOrBlank()) {
            ProjectActivity.intent(context, record.projectId)
        } else {
            Intent(context, MainActivity::class.java)
        }
        val open = PendingIntent.getActivity(
            context, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val delete = PendingIntent.getBroadcast(
            context, id,
            Intent(context, CompletedNotificationDismissReceiver::class.java).apply {
                action = ACTION_DISMISS_RESULT
                putExtra(EXTRA_EVENT_KEY, record.eventKey)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle(record.title)
            .setContentText(record.text)
            .setColor(
                when (OperationTerminalOutcome.fromTitle(record.title)) {
                    OperationTerminalOutcome.SUCCESS -> Ui.success
                    OperationTerminalOutcome.FAILED -> Ui.danger
                    OperationTerminalOutcome.CANCELLED -> Ui.warning
                    OperationTerminalOutcome.NEUTRAL -> Ui.muted
                },
            )
            .setStyle(Notification.BigTextStyle().bigText(record.text))
            .setContentIntent(open)
            .setDeleteIntent(delete)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildSummary(context: Context, latestSequence: Long): Notification {
        val open = PendingIntent.getActivity(
            context, CompletedNotificationSlotPolicy.SUMMARY_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val delete = PendingIntent.getBroadcast(
            context, CompletedNotificationSlotPolicy.SUMMARY_ID,
            Intent(context, CompletedNotificationDismissReceiver::class.java).apply {
                action = ACTION_DISMISS_GROUP
                putExtra(EXTRA_MAX_SEQUENCE, latestSequence)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · завершені операції")
            .setContentText("Розгорни групу, щоб переглянути результати томів")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setContentIntent(open)
            .setDeleteIntent(delete)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()
    }
}
