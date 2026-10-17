package com.saney.renaultdocs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * v0.5.148 (owner 2026-10-17 05:25): «як подивитися, чи вони досі стираються… скільки
 * времені… чому немає прогрес-бара?» — the files of deleted volumes are erased here, with a
 * bar in the shade («Том 5 з 23 · 34 % · залишилось ~12 хв»), also with the screen off.
 * Erases only inside the trash folders ([VolumeTrash.runEraser]). Nothing to cancel: the
 * volumes are already gone; when Android stops the service, the rest is erased next time.
 */
class VolumeTrashService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private val workerRunning = AtomicBoolean(false)
    private lateinit var workWakeLock: BackgroundWorkWakeLock

    override fun onCreate() {
        super.onCreate()
        workWakeLock = BackgroundWorkWakeLock(this, "VolumeTrash")
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Renodocs · стирання видалених томів", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification("Готую…", ongoing = true))
        if (!workerRunning.compareAndSet(false, true)) return START_NOT_STICKY
        workWakeLock.acquire()
        Thread({
            var lastNotifiedAt = 0L
            try {
                val result =
                    VolumeTrash.runEraser(
                        context = applicationContext,
                        isCancelled = { dataSyncTimeout.isExpired },
                    ) { step ->
                        val now = SystemClock.elapsedRealtime()
                        // The shade at most twice a second (a notify per file would slow everything).
                        if (now - lastNotifiedAt < NOTIFY_EVERY_MS && !step.counting) return@runEraser
                        lastNotifiedAt = now
                        notify(
                            notification(
                                text = VolumeDeleteChoice.eraseCounter(step),
                                ongoing = true,
                                permille = if (step.counting && step.itemIndex == 1) null else (step.fraction * 1000).toInt(),
                            ),
                        )
                    }
                if (dataSyncTimeout.isExpired) return@Thread
                if (result == null) {
                    // Another eraser (plain thread) is already at work — its card is on the home screen.
                    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                    return@Thread
                }
                val summary = VolumeTrash.lastSummary(applicationContext)
                if (summary != null && !result.cancelled) {
                    notify(
                        notification(
                            text =
                                VolumeDeleteChoice.eraseDoneTitle(summary.left) + ". " +
                                    VolumeDeleteChoice.eraseDoneDetail(summary.files, summary.volumes, summary.ms, summary.left),
                            ongoing = false,
                        ),
                    )
                } else {
                    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                }
            } finally {
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }, "volume-trash").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
        return START_NOT_STICKY
    }

    /** Android 15+: the dataSync time budget is used up — stop now; the rest is erased next time. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        dataSyncTimeout.expire()
        runCatching { workWakeLock.release() }
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } finally {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        workWakeLock.release()
        super.onDestroy()
    }

    private fun notify(notification: android.app.Notification) {
        if (dataSyncTimeout.isExpired) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun notification(
        text: String,
        ongoing: Boolean,
        permille: Int? = null,
    ) =
        android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle(if (ongoing) VolumeDeleteChoice.ERASE_TITLE else "Renodocs")
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .let { OperationAlert.alertIfFinal(this, it, ongoing, text) }
            .apply {
                if (ongoing) {
                    if (permille != null) setProgress(1000, permille.coerceIn(0, 1000), false) else setProgress(0, 0, true)
                }
            }
            .build()

    companion object {
        private const val CHANNEL_ID = "renault_volume_trash"
        private const val NOTIFICATION_ID = 3711
        private const val NOTIFY_EVERY_MS = 500L
    }
}
