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
import android.os.PowerManager
import androidx.documentfile.provider.DocumentFile
import java.util.concurrent.atomic.AtomicBoolean

class ConversionService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private lateinit var runStore: ConversionRunStore
    private val workerRunning =
        AtomicBoolean(
            false,
        )

    private var wakeLock:
        PowerManager.WakeLock? =
        null

    private var lastNotificationAt =
        0L

    private var lastProgressStoreAt =
        0L

    private var lastProgressPhase:
        ConversionRunPhase? =
        null

    override fun onCreate() {
        super.onCreate()

        runStore =
            ConversionRunStore(
                this,
            )

        ensureNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (
            intent?.action !=
            ACTION_START
        ) {
            return START_NOT_STICKY
        }

        if (
            !workerRunning
                .compareAndSet(
                    false,
                    true,
                )
        ) {
            return START_REDELIVER_INTENT
        }

        val sourceUri =
            intent.getStringExtra(
                EXTRA_SOURCE_URI,
            )
                ?: return stopForMissingPlan()
        val destinationUri =
            intent.getStringExtra(
                EXTRA_DESTINATION_URI,
            )
                ?: return stopForMissingPlan()
        val sourceName =
            intent.getStringExtra(
                EXTRA_SOURCE_NAME,
            )
                ?: "Renault"
        val outputFolderName =
            intent.getStringExtra(
                EXTRA_OUTPUT_FOLDER_NAME,
            )
                ?: return stopForMissingPlan()

        val plan =
            ConversionPlan(
                sourceUri =
                    sourceUri,
                destinationUri =
                    destinationUri,
                sourceName =
                    sourceName,
                outputFolderName =
                    outputFolderName,
            )

        val mergeExisting =
            DocumentFile
                .fromTreeUri(
                    this,
                    Uri.parse(
                        destinationUri,
                    ),
                )
                ?.findFile(
                    outputFolderName,
                )
                ?.isDirectory ==
                true

        val persistedState =
            runStore.load()

        // START_REDELIVER_INTENT may arrive AFTER the previous run completed.
        // It must never create a second conversion or overwrite terminal state.
        val isFrameworkRedelivery =
            flags and START_FLAG_REDELIVERY != 0
        if (
            isFrameworkRedelivery &&
            !persistedState.isRunning
        ) {
            workerRunning.set(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (persistedState.isRunning) {
            // No request ID exists in the old conversion contract, so use
            // the persisted immutable plan as the recovery identity.
            if (
                persistedState.sourceUri != sourceUri ||
                persistedState.destinationUri != destinationUri ||
                persistedState.outputFolderName != outputFolderName
            ) {
                workerRunning.set(false)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            runStore.resumeAfterProcessRestart()
        } else {
            runStore.begin(
                sourceUri =
                    sourceUri,
                destinationUri =
                    destinationUri,
                outputFolderName =
                    outputFolderName,
                mergeExisting =
                    mergeExisting,
            )
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title =
                    "Конвертація Renault",
                text =
                    "Починаю сканування…",
                progress =
                    null,
            ),
        )

        active =
            true
        acquireWakeLock()

        Thread {
            runConversion(
                plan,
            )
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
            // Even cleanup errors must not prevent Android's mandatory stop.
            runCatching { releaseWakeLock() }
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } finally {
                stopSelf()
            }
        }
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? =
        null

    private fun runConversion(
        plan: ConversionPlan,
    ) {
        try {
            val engine =
                SafConversionEngine(
                    context =
                        this,
                    runStore =
                        runStore,
                    onProgress = {
                            phase,
                            message,
                            filesTotal,
                            filesDone,
                            changedFiles,
                            changesTotal ->
                        dataSyncTimeout.checkActive()
                        val now =
                            System.currentTimeMillis()

                        if (
                            phase !=
                                lastProgressPhase ||
                            filesDone ==
                                filesTotal ||
                            now -
                                lastProgressStoreAt >=
                                PROGRESS_STORE_THROTTLE_MS
                        ) {
                            runStore.update(
                                phase =
                                    phase,
                                message =
                                    message,
                                filesTotal =
                                    filesTotal,
                                filesDone =
                                    filesDone,
                                changedFiles =
                                    changedFiles,
                                changesTotal =
                                    changesTotal,
                            )

                            lastProgressPhase =
                                phase
                            lastProgressStoreAt =
                                now
                        }

                        updateNotification(
                            phase =
                                phase,
                            message =
                                message,
                            filesTotal =
                                filesTotal,
                            filesDone =
                                filesDone,
                        )
                    },
                )

            val result =
                engine.execute(
                    plan,
                )

            dataSyncTimeout.checkActive()
            val registration =
                runCatching {
                    val record =
                        DatasetReader.read(
                            context =
                                this,
                            treeUri =
                                android.net.Uri.parse(
                                    result.outputTreeUri,
                                ),
                        ).getOrThrow()

                    DatasetStore(
                        this,
                    ).upsert(
                        record,
                    )

                    record
                }

            val completionMessage =
                buildString {
                    append(
                        "Готово · файлів: "
                    )
                    append(
                        result.filesTotal,
                    )
                    append(
                        " · змінено: "
                    )
                    append(
                        result.changedFiles,
                    )
                    append(
                        " · томів: "
                    )
                    append(
                        result.volumeCount,
                    )

                    if (
                        registration.isSuccess
                    ) {
                        append(
                            "\nДодано в бібліотеку автоматично."
                        )
                    } else {
                        append(
                            "\nOutput готовий. Автододавання в бібліотеку не вдалося."
                        )
                    }
                }

            dataSyncTimeout.checkActive()
            runStore.complete(
                outputTreeUri =
                    result.outputTreeUri,
                message =
                    completionMessage,
                filesTotal =
                    result.filesTotal,
                changedFiles =
                    result.changedFiles,
                changesTotal =
                    result.changesTotal,
            )

            notifyFinal(
                title =
                    "Renault Docs · готово",
                text =
                    if (
                        registration.isSuccess
                    ) {
                        plan.outputFolderName +
                            " готовий і доданий у бібліотеку."
                    } else {
                        plan.outputFolderName +
                            " створено. Відкрий конвертер для додавання в бібліотеку."
                    },
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            if (dataSyncTimeout.isExpired) return
            runStore.markCancelled(
                "Конвертацію скасовано. Staging видалено, source не змінено."
            )

            notifyFinal(
                title =
                    "Конвертацію скасовано",
                text =
                    "Source не змінено.",
            )
        } catch (
            error:
                Throwable,
        ) {
            if (dataSyncTimeout.isExpired) return
            runStore.fail(
                error.message
                    ?: "Помилка конвертації."
            )

            notifyFinal(
                title =
                    "Помилка конвертації",
                text =
                    error.message
                        ?: "Невідома помилка.",
            )
        } finally {
            releaseWakeLock()
            active =
                false
            workerRunning.set(
                false,
            )
            stopForeground(
                STOP_FOREGROUND_DETACH,
            )
            stopSelf()
        }
    }

    private fun updateNotification(
        phase: ConversionRunPhase,
        message: String,
        filesTotal: Int,
        filesDone: Int,
    ) {
        val now =
            System.currentTimeMillis()

        if (
            now -
                lastNotificationAt <
            NOTIFICATION_THROTTLE_MS
        ) {
            return
        }

        lastNotificationAt =
            now

        val progress =
            if (
                filesTotal > 0 &&
                phase in
                    setOf(
                        ConversionRunPhase.COPYING,
                        ConversionRunPhase.VALIDATING,
                    )
            ) {
                filesDone to
                    filesTotal
            } else {
                null
            }

        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        "Конвертація Renault",
                    text =
                        message,
                    progress =
                        progress,
                ),
            )
    }

    private fun notifyFinal(
        title: String,
        text: String,
    ) {
        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        title,
                    text =
                        text,
                    progress =
                        null,
                ),
            )
    }

    private fun buildNotification(
        title: String,
        text: String,
        progress: Pair<Int, Int>?,
    ): Notification {
        val openIntent =
            Intent(
                this,
                ConversionActivity::class.java,
            )

        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        return Notification.Builder(
            this,
            CHANNEL_ID,
        )
            .setSmallIcon(
                R.drawable.ic_notification_document,
            )
            .setContentTitle(
                title,
            )
            .setContentText(
                text,
            )
            .setContentIntent(
                contentIntent,
            )
            .setOnlyAlertOnce(
                true,
            )
            .setOngoing(
                runStore.load()
                    .isRunning,
            )
            .apply {
                if (progress != null) {
                    setProgress(
                        progress.second,
                        progress.first,
                        false,
                    )
                } else if (
                    runStore.load()
                        .isRunning
                ) {
                    setProgress(
                        0,
                        0,
                        true,
                    )
                }
            }
            .build()
    }

    private fun ensureNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Renault conversion",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description =
                    "Прогрес довгої конвертації Renault dataset"
            }

        notificationManager()
            .createNotificationChannel(
                channel,
            )
    }

    private fun notificationManager():
        NotificationManager =
        getSystemService(
            NotificationManager::class.java,
        )

    private fun acquireWakeLock() {
        if (
            wakeLock?.isHeld ==
            true
        ) {
            return
        }

        val powerManager =
            getSystemService(
                PowerManager::class.java,
            )

        wakeLock =
            powerManager
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    packageName +
                        ":RenaultConversion",
                )
                .apply {
                    setReferenceCounted(
                        false,
                    )
                    acquire()
                }
    }

    private fun releaseWakeLock() {
        wakeLock
            ?.takeIf {
                it.isHeld
            }
            ?.release()

        wakeLock =
            null
    }

    override fun onDestroy() {
        releaseWakeLock()
        active =
            false

        super.onDestroy()
    }

    private fun stopForMissingPlan():
        Int {
        active =
            false
        workerRunning.set(
            false,
        )
        runStore.fail(
            "Не вдалося відновити план конвертації."
        )
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        @Volatile
        private var active =
            false

        fun isActive():
            Boolean =
            active

        private const val ACTION_START =
            "com.saney.renaultdocs.action.CONVERT_START"

        private const val EXTRA_SOURCE_URI =
            "sourceUri"
        private const val EXTRA_DESTINATION_URI =
            "destinationUri"
        private const val EXTRA_SOURCE_NAME =
            "sourceName"
        private const val EXTRA_OUTPUT_FOLDER_NAME =
            "outputFolderName"

        private const val CHANNEL_ID =
            "renault_conversion"
        private const val NOTIFICATION_ID =
            3701
        private const val NOTIFICATION_THROTTLE_MS =
            700L
        private const val PROGRESS_STORE_THROTTLE_MS =
            250L

        fun start(
            context: Context,
            plan: ConversionPlan,
        ) {
            val intent =
                Intent(
                    context,
                    ConversionService::class.java,
                ).apply {
                    action =
                        ACTION_START
                    putExtra(
                        EXTRA_SOURCE_URI,
                        plan.sourceUri,
                    )
                    putExtra(
                        EXTRA_DESTINATION_URI,
                        plan.destinationUri,
                    )
                    putExtra(
                        EXTRA_SOURCE_NAME,
                        plan.sourceName,
                    )
                    putExtra(
                        EXTRA_OUTPUT_FOLDER_NAME,
                        plan.outputFolderName,
                    )
                }

            context.startForegroundService(
                intent,
            )
        }

        fun requestCancel(
            context: Context,
        ) {
            ConversionRunStore(
                context,
            ).requestCancel()
        }
    }
}
