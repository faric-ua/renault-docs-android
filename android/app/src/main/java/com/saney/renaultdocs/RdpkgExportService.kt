package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean

class RdpkgExportService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private lateinit var runStore: RdpkgExportRunStore
    private val workerRunning = AtomicBoolean(false)
    private lateinit var workWakeLock: BackgroundWorkWakeLock

    override fun onCreate() {
        super.onCreate()
        workWakeLock =
            BackgroundWorkWakeLock(
                this,
                "RdpkgExport",
            )
        runStore = RdpkgExportRunStore(this)
        val channel = NotificationChannel(CHANNEL_ID, "Renault .rdpkg export", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (!workerRunning.compareAndSet(false, true)) return START_REDELIVER_INTENT
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

        val archiveMode = VolumeArchiveAccess.isArchivePart(destinationUri)

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
                var lastShownAt = 0L
                val onProgress: (OperationProgress) -> Unit = { progress ->
                    dataSyncTimeout.checkActive()
                    if (runStore.isCancelRequested()) throw OperationCancelledException()
                    val now = android.os.SystemClock.elapsedRealtime()
                    // The archive writer reports every file (~8 thousand): show ~3 times a second.
                    if (!archiveMode || now - lastShownAt >= 300L || progress.current == progress.total) {
                        lastShownAt = now
                        showExportProgress(progress)
                    }
                }
                val result =
                    if (archiveMode) {
                        // v0.5.142: STORED entries, no compressor (M30s wrote broken DEFLATE).
                        RdpkgExporter.exportArchive(
                            context = applicationContext,
                            volume = volume,
                            target = java.io.File(requireNotNull(Uri.parse(destinationUri).path)),
                            progressState = onProgress,
                        ).getOrThrow()
                    } else {
                        RdpkgExporter.export(
                            context = applicationContext,
                            volume = volume,
                            destinationUri = Uri.parse(destinationUri),
                            progressState = onProgress,
                        ).getOrThrow()
                    }
                dataSyncTimeout.checkActive()
                if (runStore.isCancelRequested()) throw OperationCancelledException()
                val label = listOfNotNull(volume.documentCode, volume.date).joinToString(" · ").ifBlank { volume.title }
                if (archiveMode) {
                    // Archives plan v2, stage 1: the archive is used only after every file
                    // was found equal to the folder; then it gets its final name.
                    val message = finishArchive(volume, result, label, Uri.parse(destinationUri))
                    runStore.complete(result.sha256, result.fileCount, message)
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Архів тому готовий.", false))
                    return@Thread
                }
                val message = "Пакет .rdpkg збережено: $label · ${result.fileCount} файлів"
                runStore.complete(result.sha256, result.fileCount, message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Експорт .rdpkg завершено.", false))
            } catch (error: Throwable) {
                if (dataSyncTimeout.isExpired) return@Thread
                if (archiveMode) {
                    // Only this run's own unfinished archive file is removed; the volume and
                    // any archive made before stay as they were.
                    val part = Uri.parse(destinationUri).path?.let { java.io.File(it) }
                    val removed = part?.let { !it.exists() || it.delete() } == true
                    val cancelled = error is OperationCancelledException || error.cause is OperationCancelledException
                    val tail = if (removed) "Недороблений архів видалено, сам том не змінено." else "Сам том не змінено."
                    if (cancelled) {
                        runStore.cancelled("Архів тому скасовано. $tail")
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Архів тому скасовано.", false))
                    } else {
                        val message = (error.message ?: "Невідома помилка.") + " " + tail
                        runStore.fail(message)
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Архів тому: помилка", false))
                    }
                    return@Thread
                }
                if (error is OperationCancelledException || error.cause is OperationCancelledException) {
                    // v0.5.105: the file was created by this export a moment ago and is
                    // unfinished; remove only it, so no broken .rdpkg is left behind.
                    val removed = runCatching {
                        DocumentsContract.deleteDocument(contentResolver, Uri.parse(destinationUri))
                    }.getOrDefault(false)
                    runStore.cancelled(
                        if (removed) {
                            "Експорт тому скасовано. Недороблений файл .rdpkg видалено, сам том не змінено."
                        } else {
                            "Експорт тому скасовано. Недороблений файл .rdpkg не вдалося видалити — видали його вручну. Сам том не змінено."
                        },
                    )
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Експорт .rdpkg скасовано.", false))
                    return@Thread
                }
                val message = error.message ?: "Невідома помилка експорту."
                runStore.fail(message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Помилка експорту: $message", false))
            } finally {
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
        return START_REDELIVER_INTENT
    }

    private fun showExportProgress(progress: OperationProgress) {
        val stage = progress.displayText()
        runStore.update(stage)
        runStore.updateProgress(progress)
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            notification(
                text = stage,
                ongoing = true,
                current = progress.current,
                total = progress.total,
            ),
        )
    }

    /**
     * Archives plan v2, stage 1 (owner 2026-10-17 00:32). The packed file is compared with the
     * folder file by file ([VolumeArchiveVerifier]); only when everything is equal it gets the
     * name `<id>.rdpkg` and the volume is read from it. An older archive of this volume (made
     * by this app for an earlier copy) is replaced. The unpacked folder is never touched.
     */
    private fun finishArchive(
        volume: ProjectVolumeRecord,
        result: RdpkgExporter.ExportResult,
        label: String,
        partUri: Uri,
    ): String {
        val part = java.io.File(requireNotNull(partUri.path))
        val folder =
            VolumeStorage.locate(applicationContext, result.packageId)
                ?: error("Папку тому не знайдено.")
        var lastShownAt = 0L
        val check =
            VolumeArchiveVerifier.verify(
                folder = folder,
                archive = part,
                progress = { done, total ->
                    dataSyncTimeout.checkActive()
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastShownAt >= 300L || done >= total) {
                        lastShownAt = now
                        val progress =
                            OperationProgress.measured(
                                stage = "Перевіряю архів…",
                                current = (done.coerceAtMost(total) * 1000L / total.coerceAtLeast(1L)).toInt(),
                                total = 1000,
                            )
                        runStore.update(progress.displayText())
                        runStore.updateProgress(progress)
                    }
                },
                isCancelled = { runStore.isCancelRequested() },
            )
        if (!check.ok) {
            error("Архів не збігся з томом — не використовую. ${check.problem.orEmpty()}")
        }
        val target = java.io.File(part.parentFile, result.packageId + VolumeArchiveAccess.SUFFIX)
        val replaced = target.exists()
        if (replaced && !target.delete()) error("Не вдалося замінити старий архів тому.")
        if (!part.renameTo(target)) error("Не вдалося дати архіву остаточну назву.")
        VolumeArchiveAccess.setReadingOff(applicationContext, result.packageId, off = false)
        VolumeFileIndex.forget(result.packageId)
        val size = RdprojectImportSupport.formatBytes(target.length())
        return buildString {
            append("Архів тому зроблено і перевірено: $label · ${check.fileCount} файлів, $size. ")
            if (replaced) append("Старий архів тому замінено. ")
            append("Том тепер читається з архіву; папка тому лишилась як була.")
        }
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
            // Even cleanup errors must not prevent Android's mandatory stop.
            runCatching { workWakeLock.release() }
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } finally {
                stopSelf()
            }
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
        val openIntent = state.projectId?.takeIf { it.isNotBlank() }?.let { ProjectActivity.intent(this, it) } ?: Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · експорт .rdpkg")
            .setContentText(text)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .let { OperationAlert.alertIfFinal(this, it, ongoing, text) }
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
