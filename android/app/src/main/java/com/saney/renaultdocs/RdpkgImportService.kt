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

class RdpkgImportService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private lateinit var runStore: RdpkgImportRunStore
    private val workerRunning = AtomicBoolean(false)
    private lateinit var workWakeLock: BackgroundWorkWakeLock

    override fun onCreate() {
        super.onCreate()
        workWakeLock =
            BackgroundWorkWakeLock(
                this,
                "RdpkgImport",
            )
        runStore = RdpkgImportRunStore(this)
        val channel = NotificationChannel(CHANNEL_ID, "Renault .rdpkg import", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (!workerRunning.compareAndSet(false, true)) return START_REDELIVER_INTENT
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        val packageUri = intent.getStringExtra(EXTRA_PACKAGE_URI)
        if (projectId.isNullOrBlank() || packageUri.isNullOrBlank()) {
            workerRunning.set(false)
            stopSelf()
            return START_NOT_STICKY
        }
        val state = runStore.load()
        if (!state.isRunning || state.projectId != projectId || state.packageUri != packageUri) {
            workerRunning.set(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notification(
                text = "Читаю пакет…",
                ongoing = true,
            ),
        )
        workWakeLock.acquire()

        Thread {
            try {
                val result = RdpkgImporter.install(
                    context = applicationContext,
                    packageUri = Uri.parse(packageUri),
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
                    isCancelled = { dataSyncTimeout.isExpired },
                ).getOrThrow()
                dataSyncTimeout.checkActive()
                runStore.complete(result.packageId, "Пакет .rdpkg імпортовано.")
                publishResult(
                    title = "Renault Docs · том імпортовано",
                    text = VolumeDuplicatePreflight.label(result.volume),
                )
            } catch (error: Throwable) {
                if (dataSyncTimeout.isExpired) return@Thread
                val message = error.message ?: "Невідома помилка імпорту."
                runStore.fail(message)
                publishResult(
                    title = "Renault Docs · помилка імпорту",
                    text = message,
                )
            } finally {
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                stopSelf()
            }
        }.start()
        return START_REDELIVER_INTENT
    }

    private fun publishResult(title: String, text: String) {
        val state = runStore.load()
        if (!state.isTerminal) return
        CompletedNotificationHistory.publish(
            context = this,
            eventKey = listOf(
                "import",
                state.projectId.orEmpty(),
                state.startedAtMs.toString(),
                state.finishedAtMs.toString(),
                state.phase.name,
            ).joinToString(":"),
            projectId = state.projectId,
            title = title,
            text = text,
        )
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
    ): Notification {
        val state = runStore.load()
        val openIntent = if (!state.projectId.isNullOrBlank()) {
            ProjectActivity.intent(this, state.projectId!!)
        } else {
            Intent(this, MainActivity::class.java)
        }
        val pending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · .rdpkg")
            .setContentText(text)
            .setContentIntent(pending)
            .setColor(Ui.success)
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
        private const val ACTION_START = "com.saney.renaultdocs.action.RDPKG_IMPORT_START"
        private const val EXTRA_PROJECT_ID = "projectId"
        private const val EXTRA_PACKAGE_URI = "packageUri"
        private const val CHANNEL_ID = "renault_rdpkg_import"
        private const val NOTIFICATION_ID = 3703

        fun start(context: Context, projectId: String, packageUri: Uri): Boolean {
            val runStore = RdpkgImportRunStore(context)
            if (!runStore.begin(projectId, packageUri.toString())) return false

            val intent = Intent(context, RdpkgImportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_PACKAGE_URI, packageUri.toString())
            }
            return try {
                context.startForegroundService(intent)
                true
            } catch (error: Throwable) {
                runStore.fail(error.message ?: "Не вдалося запустити імпорт .rdpkg.")
                false
            }
        }
    }
}
