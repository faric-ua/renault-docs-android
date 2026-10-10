package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.FileProvider
import java.util.concurrent.atomic.AtomicBoolean

class RdpkgShareService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private lateinit var runStore: RdpkgShareRunStore
    private val workerRunning = AtomicBoolean(false)
    private lateinit var workWakeLock: BackgroundWorkWakeLock

    override fun onCreate() {
        super.onCreate()
        workWakeLock =
            BackgroundWorkWakeLock(
                this,
                "RdpkgShare",
            )
        runStore = RdpkgShareRunStore(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Renault .rdpkg share preparation", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (!workerRunning.compareAndSet(false, true)) return START_REDELIVER_INTENT
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        val volumeId = intent.getStringExtra(EXTRA_VOLUME_ID)
        if (projectId.isNullOrBlank() || volumeId.isNullOrBlank()) {
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }
        val state = runStore.load()
        if (!state.isRunning || state.projectId != projectId || state.volumeId != volumeId) {
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }
        val store = ProjectStore(this)
        val project = store.project(projectId)
        val volume = store.volumes(projectId).firstOrNull { it.id == volumeId }
        if (project == null || volume == null) {
            runStore.fail("Не вдалося знайти проєкт або том для поширення.")
            workerRunning.set(false); stopSelf(); return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notification(
                text = "Готую…",
                ongoing = true,
            ),
        )
        workWakeLock.acquire()

        Thread {
            try {
                val file = PreparedShareStore.volumeFile(applicationContext, project, volume)
                val uri = FileProvider.getUriForFile(applicationContext, packageName + ".files", file)
                RdpkgExporter.export(
                    context = applicationContext,
                    volume = volume,
                    destinationUri = uri,
                    progressState = { progress ->
                        dataSyncTimeout.checkActive()
                        val stage =
                            progress.displayText()
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
                dataSyncTimeout.checkActive()
                runStore.complete(file.absolutePath)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Том готовий для поширення.", false))
            } catch (error: Throwable) {
                if (dataSyncTimeout.isExpired) return@Thread
                val message = error.message ?: "Невідома помилка підготовки тому."
                runStore.fail(message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Помилка підготовки: $message", false))
            } finally {
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
        return START_REDELIVER_INTENT
    }

    /**
     * Android 15+ calls this when the app's shared dataSync FGS budget expires.
     * Persist the interruption; stopSelf is mandatory within a few seconds.
     * Worker callbacks observe dataSyncTimeout and cannot publish success.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        dataSyncTimeout.expire()
        try {
            if (runStore.load().isRunning) {
                runStore.fail(DataSyncTimeoutUi.MESSAGE)
            }
        } finally {
        workWakeLock.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        workWakeLock.release()
        super.onDestroy()
    }

    private fun notification(
        text: String,
        ongoing: Boolean,
        current: Int? = null,
        total: Int? = null,
    ) =
        android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · підготовка тому")
            .setContentText(text)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    runStore.load().projectId?.let { ProjectActivity.intent(this, it) } ?: Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
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

    companion object {
        private const val ACTION_START = "com.saney.renaultdocs.action.RDPKG_SHARE_START"
        private const val EXTRA_PROJECT_ID = "projectId"
        private const val EXTRA_VOLUME_ID = "volumeId"
        private const val CHANNEL_ID = "renault_rdpkg_share"
        private const val NOTIFICATION_ID = 3705

        fun start(context: Context, projectId: String, volumeId: String): Boolean {
            val store = RdpkgShareRunStore(context)
            if (!store.begin(projectId, volumeId)) return false
            return try {
                context.startForegroundService(
                    Intent(context, RdpkgShareService::class.java).apply {
                        action = ACTION_START
                        putExtra(EXTRA_PROJECT_ID, projectId)
                        putExtra(EXTRA_VOLUME_ID, volumeId)
                    },
                )
                true
            } catch (error: Throwable) {
                store.fail(error.message ?: "Не вдалося запустити підготовку тому.")
                false
            }
        }
    }
}
