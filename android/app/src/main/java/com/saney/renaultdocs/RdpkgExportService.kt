package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean

class RdpkgExportService : Service() {
    private lateinit var runStore: RdpkgExportRunStore
    private val workerRunning = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        runStore = RdpkgExportRunStore(this)
        val channel = NotificationChannel(CHANNEL_ID, "Renault .rdpkg export", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START || !workerRunning.compareAndSet(false, true)) return START_NOT_STICKY
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        val volumeId = intent.getStringExtra(EXTRA_VOLUME_ID)
        val destinationUri = intent.getStringExtra(EXTRA_DESTINATION_URI)
        if (projectId.isNullOrBlank() || volumeId.isNullOrBlank() || destinationUri.isNullOrBlank()) {
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }
        val state = runStore.load()
        if (!state.isRunning || state.projectId != projectId || state.volumeId != volumeId || state.destinationUri != destinationUri) {
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }
        val volume = ProjectStore(this).volumes(projectId).firstOrNull { it.id == volumeId }
        if (volume == null) {
            runStore.fail("Не вдалося знайти том для експорту.")
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notification(
                text = "Готую…",
                ongoing = true,
            ),
        )
        Thread {
            try {
                val result = RdpkgExporter.export(
                    context = applicationContext,
                    volume = volume,
                    destinationUri = Uri.parse(destinationUri),
                    progressState = { progress ->
                        val stage =
                            progress.compactStage()
                        runStore.update(
                            stage,
                        )
                        runStore.updateProgress(
                            progress,
                        )
                        getSystemService(
                            NotificationManager::class.java,
                        ).notify(
                            NOTIFICATION_ID,
                            notification(
                                text = stage,
                                ongoing = true,
                                current = progress.current,
                                total = progress.total,
                            ),
                        )
                    },
                ).getOrThrow()
                val label = listOfNotNull(volume.documentCode, volume.date).joinToString(" · ").ifBlank { volume.title }
                val message = "Пакет .rdpkg збережено: $label · ${result.fileCount} файлів"
                runStore.complete(result.sha256, result.fileCount, message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Експорт .rdpkg завершено.", false))
            } catch (error: Throwable) {
                val message = error.message ?: "Невідома помилка експорту."
                runStore.fail(message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Помилка експорту: $message", false))
            } finally {
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(
        text: String,
        ongoing: Boolean,
        current: Int? = null,
        total: Int? = null,
    ): Notification {
        val state = runStore.load()
        val openIntent = state.projectId?.takeIf { it.isNotBlank() }?.let { ProjectActivity.intent(this, it) } ?: Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Renault Docs · експорт .rdpkg")
            .setContentText(text)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .apply {
                if (ongoing) {
                    val resolvedTotal =
                        total ?: 0
                    val resolvedCurrent =
                        current ?: 0

                    if (
                        resolvedTotal >
                        0
                    ) {
                        setProgress(
                            resolvedTotal,
                            resolvedCurrent.coerceIn(
                                0,
                                resolvedTotal,
                            ),
                            false,
                        )
                    } else {
                        setProgress(
                            0,
                            0,
                            true,
                        )
                    }
                }
            }
            .build()
    }

    companion object {
        private const val ACTION_START = "com.saney.renaultdocs.action.RDPKG_EXPORT_START"
        private const val EXTRA_PROJECT_ID = "projectId"
        private const val EXTRA_VOLUME_ID = "volumeId"
        private const val EXTRA_DESTINATION_URI = "destinationUri"
        private const val CHANNEL_ID = "renault_rdpkg_export"
        private const val NOTIFICATION_ID = 3704

        fun start(context: Context, projectId: String, volumeId: String, destinationUri: Uri): Boolean {
            val runStore = RdpkgExportRunStore(context)
            if (!runStore.begin(projectId, volumeId, destinationUri.toString())) return false
            val intent = Intent(context, RdpkgExportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_VOLUME_ID, volumeId)
                putExtra(EXTRA_DESTINATION_URI, destinationUri.toString())
            }
            return try {
                context.startForegroundService(intent); true
            } catch (error: Throwable) {
                runStore.fail(error.message ?: "Не вдалося запустити експорт .rdpkg."); false
            }
        }
    }
}
