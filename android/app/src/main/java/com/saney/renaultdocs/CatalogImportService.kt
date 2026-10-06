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
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CatalogImportService : Service() {
    private lateinit var runStore: CatalogImportRunStore
    private lateinit var workWakeLock: BackgroundWorkWakeLock
    private val workerRunning = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        workWakeLock =
            BackgroundWorkWakeLock(
                this,
                "CatalogImport",
            )
        runStore = CatalogImportRunStore(this)

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Renault Docs catalog import",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (
            intent?.action != ACTION_START
        ) {
            return START_NOT_STICKY
        }

        if (
            !workerRunning.compareAndSet(false, true)
        ) {
            return START_REDELIVER_INTENT
        }

        val state = runStore.load()

        if (!state.isRunning || state.items.isEmpty()) {
            workerRunning.set(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notification(
                "Готую імпорт із каталогу…",
                true,
            ),
        )

        workWakeLock.acquire()

        Thread {
            try {
                importItems(state.items)
            } catch (error: Throwable) {
                val message =
                    error.message
                        ?: "Невідома помилка імпорту з каталогу."

                runStore.fail(message)
                notify(
                    "Помилка: $message",
                    false,
                )
            } finally {
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? =
        null

    override fun onDestroy() {
        workWakeLock.release()
        super.onDestroy()
    }

    private fun importItems(
        items: List<CatalogImportItem>,
    ) {
        val store =
            ProjectStore(
                applicationContext,
            )

        val downloadDirectory =
            File(
                cacheDir,
                "catalog-downloads",
            ).apply {
                mkdirs()
            }

        items.forEachIndexed {
            index,
            item ->
            val targetProject =
                store.project(
                    item.projectId,
                )
                    ?: store.createProject(
                        title =
                            item.projectTitle,
                        model =
                            item.projectTitle,
                    )

            val destination =
                File(
                    downloadDirectory,
                    item.fileName,
                )

            val downloadText =
                "Завантажую " +
                    item.documentCode +
                    " · " +
                    (index + 1) +
                    "/" +
                    items.size

            runStore.update(
                phase =
                    CatalogImportPhase.DOWNLOADING,
                itemIndex =
                    index,
                message =
                    downloadText,
            )

            notify(
                downloadText,
                true,
            )

            DriveCatalogClient
                .downloadPackage(
                    driveFileId =
                        item.driveFileId,
                    destination =
                        destination,
                    expectedBytes =
                        item.sizeBytes,
                ) {
                    done,
                    total ->
                    val scale =
                        10_000
                    val progress =
                        if (
                            total >
                            0L
                        ) {
                            (
                                done.coerceIn(
                                    0L,
                                    total,
                                ) *
                                    scale /
                                    total
                            ).toInt()
                        } else {
                            0
                        }

                    runStore.update(
                        phase =
                            CatalogImportPhase.DOWNLOADING,
                        itemIndex =
                            index,
                        message =
                            downloadText,
                        current =
                            progress,
                        total =
                            if (
                                total >
                                0L
                            ) {
                                scale
                            } else {
                                0
                            },
                    )
                }

            val uri =
                FileProvider
                    .getUriForFile(
                        applicationContext,
                        packageName +
                            ".files",
                        destination,
                    )

            val importText =
                "Імпортую " +
                    item.documentCode +
                    " · " +
                    (index + 1) +
                    "/" +
                    items.size

            runStore.update(
                phase =
                    CatalogImportPhase.IMPORTING,
                itemIndex =
                    index,
                message =
                    importText,
            )

            val imported =
                RdpkgImporter
                    .install(
                        context =
                            applicationContext,
                        packageUri =
                            uri,
                        progressState = {
                            progress ->
                            runStore.update(
                                phase =
                                    CatalogImportPhase.IMPORTING,
                                itemIndex =
                                    index,
                                message =
                                    importText,
                                current =
                                    progress.normalizedCurrent
                                        ?: 0,
                                total =
                                    progress.total
                                        ?: 0,
                            )
                        },
                    )
                    .getOrThrow()

            store.upsertVolume(
                projectId =
                    targetProject.id,
                volume =
                    imported.volume.copy(
                        projectHint =
                            targetProject.id,
                    ),
            )

            destination.delete()
        }

        runStore.complete(
            "Імпортовано: " +
                items.size +
                " " +
                volumeCountNoun(
                    items.size,
                ),
        )

        notify(
            "Імпорт із каталогу завершено.",
            false,
        )
    }

    private fun volumeCountNoun(
        count: Int,
    ): String {
        val mod100 =
            count %
                100
        val mod10 =
            count %
                10

        return when {
            mod100 in
                11..14 ->
                "томів"

            mod10 ==
                1 ->
                "том"

            mod10 in
                2..4 ->
                "томи"

            else ->
                "томів"
        }
    }

    private fun notify(
        text: String,
        ongoing: Boolean,
    ) {
        getSystemService(NotificationManager::class.java)
            .notify(
                NOTIFICATION_ID,
                notification(
                    text,
                    ongoing,
                ),
            )
    }

    private fun notification(
        text: String,
        ongoing: Boolean,
    ): Notification {
        val state =
            runStore.load()

        val pending =
            PendingIntent.getActivity(
                this,
                0,
                Intent(
                    this,
                    DriveCatalogActivity::class.java,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        return Notification.Builder(
            this,
            CHANNEL_ID,
        )
            .setSmallIcon(
                android.R.drawable.stat_sys_download,
            )
            .setContentTitle(
                "Renault Docs · каталог",
            )
            .setContentText(
                text,
            )
            .setContentIntent(
                pending,
            )
            .setOnlyAlertOnce(
                true,
            )
            .setOngoing(
                ongoing,
            )
            .apply {
                if (
                    ongoing
                ) {
                    if (
                        state.progressTotal >
                        0
                    ) {
                        setProgress(
                            state.progressTotal,
                            state.progressCurrent
                                .coerceIn(
                                    0,
                                    state.progressTotal,
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
        private const val ACTION_START =
            "com.saney.renaultdocs.action.CATALOG_IMPORT_START"
        private const val CHANNEL_ID =
            "renault_catalog_import"
        private const val NOTIFICATION_ID =
            3707

        fun start(
            context: Context,
            items: List<CatalogImportItem>,
        ): Boolean {
            val store =
                CatalogImportRunStore(
                    context,
                )

            if (
                !store.begin(
                    items,
                )
            ) {
                return false
            }

            return try {
                context.startForegroundService(
                    Intent(
                        context,
                        CatalogImportService::class.java,
                    ).apply {
                        action =
                            ACTION_START
                    },
                )
                true
            } catch (
                error:
                    Throwable,
            ) {
                store.fail(
                    error.message
                        ?: "Не вдалося запустити імпорт із каталогу.",
                )
                false
            }
        }
    }
}
