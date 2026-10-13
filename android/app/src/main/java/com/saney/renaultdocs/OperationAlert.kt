package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * v0.5.126 — sound in the shade when a long operation ends (owner 2026-10-13 21:27:
 * «щоб звук був, коли він доробив операцію в шторці, або якась помилка»).
 *
 * Progress stays silent (low channels, «only alert once»). Only the FINAL notification
 * of an operation moves to one of two sound channels:
 *  - «Готово» — default importance: the phone's normal notification sound;
 *  - «Помилки» — high importance: sound and a pop-up on top of the screen.
 * A cancel the owner pressed himself stays silent. Android lets the owner pick another
 * sound or switch either channel off: Settings → Apps → Renault Docs → Notifications.
 *
 * Channel ids are new: an existing channel's importance cannot be raised by the app.
 */
object OperationAlert {
    const val CHANNEL_DONE = "renault_alert_done_v1"
    const val CHANNEL_ERROR = "renault_alert_error_v1"

    enum class Kind { DONE, ERROR, SILENT }

    /** Pure: which channel a final text belongs to (JVM-tested). */
    fun kindOf(text: String): Kind = when (OperationTerminalOutcome.fromTitle(text)) {
        OperationTerminalOutcome.FAILED -> Kind.ERROR
        OperationTerminalOutcome.CANCELLED -> Kind.SILENT
        OperationTerminalOutcome.SUCCESS, OperationTerminalOutcome.NEUTRAL -> Kind.DONE
    }

    /** Moves a FINAL notification to a sound channel; returns the same builder. */
    fun terminal(context: Context, builder: Notification.Builder, text: String): Notification.Builder {
        val channel = when (kindOf(text)) {
            Kind.DONE -> CHANNEL_DONE
            Kind.ERROR -> CHANNEL_ERROR
            Kind.SILENT -> return builder
        }
        if (!ensureChannels(context)) return builder
        return builder
            .setChannelId(channel)
            // The final notification replaces the progress one under the same id;
            // «only alert once» would keep it silent.
            .setOnlyAlertOnce(false)
            .setCategory(if (channel == CHANNEL_ERROR) Notification.CATEGORY_ERROR else Notification.CATEGORY_STATUS)
    }

    /** For a service's own notification builder: progress unchanged, final → sound. */
    fun alertIfFinal(
        context: Context,
        builder: Notification.Builder,
        ongoing: Boolean,
        text: String,
    ): Notification.Builder = if (ongoing) builder else terminal(context, builder, text)

    fun ensureChannels(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Готово (зі звуком)", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Звук, коли підготовка, експорт, імпорт чи завантаження завершились" },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ERROR, "Помилки (зі звуком)", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Звук і спливне повідомлення, коли операція зупинилась з помилкою" },
        )
        true
    }.getOrDefault(false)
}
