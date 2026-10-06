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
import android.provider.DocumentsContract
import java.util.concurrent.atomic.AtomicBoolean

class NativeRdpkgPreparationService : Service() {
    data class StartRequest(
        val requestId: String,
        val sourceTreeUri: String,
        val sourceName: String,
        val destinationUri: String,
        val projectId: String,
        val projectTitle: String,
        val model: String,
    )

    private lateinit var runStore:
        NativeRdpkgRunStore

    private val workerRunning =
        AtomicBoolean(
            false,
        )

    private var wakeLock:
        PowerManager.WakeLock? =
        null

    private var lastNotificationAt =
        0L

    override fun onCreate() {
        super.onCreate()

        runStore =
            NativeRdpkgRunStore(
                this,
            )

        ensureNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (
            intent?.action
        ) {
            ACTION_CANCEL -> {
                val state =
                    runStore.load()

                if (
                    state.phase ==
                    NativeRdpkgRunPhase.PREPARING
                ) {
                    runStore.requestCancel()

                    updateNotification(
                        title =
                            "Renault Docs · скасування",
                        text =
                            "Зупиняю підготовку після поточної операції…",
                        projectId =
                            state.projectId,
                        cancellable =
                            false,
                    )
                } else if (
                    !state.isRunning
                ) {
                    stopSelf()
                }

                return START_NOT_STICKY
            }

            ACTION_START -> Unit

            else ->
                return START_NOT_STICKY
        }

        if (
            !workerRunning.compareAndSet(
                false,
                true,
            )
        ) {
            return START_REDELIVER_INTENT
        }

        val request =
            parseRequest(
                intent,
            )
                ?: return stopForMissingRequest()

        val persistedState =
            runStore.load()

        val resumingAfterProcessRestart =
            persistedState.isRunning &&
                persistedState.projectId ==
                    request.projectId &&
                persistedState.sourceUri ==
                    request.sourceTreeUri &&
                persistedState.destinationUri ==
                    request.destinationUri

        if (
            persistedState.isRunning &&
            !resumingAfterProcessRestart
        ) {
            startInFlight.set(
                false,
            )
            workerRunning.set(
                false,
            )
            return START_NOT_STICKY
        }

        if (
            resumingAfterProcessRestart
        ) {
            runStore.resumeAfterProcessRestart()
        } else {
            runStore.begin(
                projectId =
                    request.projectId,
                sourceUri =
                    request.sourceTreeUri,
                sourceName =
                    request.sourceName,
                destinationUri =
                    request.destinationUri,
            )
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title =
                    "Renault Docs · створення .rdpkg",
                text =
                    "Починаю Kotlin-native підготовку…",
                projectId =
                    request.projectId,
                cancellable =
                    true,
            ),
        )

        active =
            true

        acquireWakeLock()

        Thread {
            runPreparation(
                request,
            )
        }.start()

        startInFlight.set(
            false,
        )

        return START_REDELIVER_INTENT
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? =
        null

    private fun runPreparation(
        request: StartRequest,
    ) {
        val destination =
            Uri.parse(
                request.destinationUri,
            )

        var validatedDestination =
            false

        try {
            val engine =
                NativeRdpkgPreparationEngine(
                    context =
                        this,
                    onProgress = {
                        message ->
                        runStore.updatePreparing(
                            message,
                        )
                    },
                    onProgressState = {
                        progress ->
                        runStore.updateProgress(
                            progress,
                        )
                        runStore.updatePreparing(
                            progress.displayText(),
                        )

                        updateNotificationThrottled(
                            title =
                                "Renault Docs · створення .rdpkg",
                            text =
                                progress.displayText(),
                            projectId =
                                request.projectId,
                            cancellable =
                                true,
                        )
                    },
                    isCancelled = {
                        runStore.isCancelRequested()
                    },
                )

            val prepared =
                engine.prepare(
                    NativeRdpkgPreparationEngine
                        .Request(
                            sourceTreeUri =
                                Uri.parse(
                                    request.sourceTreeUri,
                                ),
                            sourceName =
                                request.sourceName,
                            projectId =
                                request.projectId,
                            model =
                                request.model,
                            destinationUri =
                                destination,
                            title =
                                request.projectTitle,
                        ),
                )

            if (
                runStore.isCancelRequested()
            ) {
                throw ConversionCancelledException()
            }

            runStore.updateImporting(
                "Перевіряю та встановлюю створений .rdpkg…",
            )
            runStore.updateProgress(
                OperationProgress.indeterminate(
                    "Перевіряю…",
                ),
            )

            updateNotification(
                title =
                    "Renault Docs · перевірка .rdpkg",
                text =
                    "Перевіряю та встановлюю створений пакет…",
                projectId =
                    request.projectId,
                cancellable =
                    false,
            )

            val imported =
                RdpkgImporter
                    .install(
                        context =
                            this,
                        packageUri =
                            destination,
                        progress = {
                            message ->
                            runStore.updateImporting(
                                message,
                            )
                        },
                        progressState = {
                            progress ->
                            runStore.updateProgress(
                                progress,
                            )

                            updateNotificationThrottled(
                                title =
                                    "Renault Docs · перевірка .rdpkg",
                                text =
                                    progress.displayText(),
                                projectId =
                                    request.projectId,
                                cancellable =
                                    false,
                            )
                        },
                    )
                    .getOrThrow()

            require(
                imported.packageId ==
                    prepared.packageId
            ) {
                "Generated package_id не збігається після validation/import."
            }

            validatedDestination =
                true

            val projectStore =
                ProjectStore(
                    this,
                )

            require(
                projectStore.project(
                    request.projectId,
                ) !=
                    null
            ) {
                "Проєкт більше не існує: " +
                    request.projectId
            }

            projectStore.upsertVolume(
                projectId =
                    request.projectId,
                volume =
                    imported.volume,
            )

            val label =
                listOfNotNull(
                    imported.volume.documentCode,
                    imported.volume.date,
                )
                    .joinToString(
                        " · ",
                    )
                    .ifBlank {
                        imported.volume.title
                    }

            val message =
                "Готово · " +
                    label +
                    " · " +
                    prepared.sectionCount +
                    " native" +
                    "\nSHA-256: " +
                    prepared.sha256

            runStore.complete(
                message =
                    message,
                packageId =
                    imported.packageId,
                volumeId =
                    imported.volume.id,
                volumeTitle =
                    label,
                sha256 =
                    prepared.sha256,
                filesTotal =
                    prepared.sourceFiles,
                changedFiles =
                    prepared.changedFiles,
                changesTotal =
                    prepared.changesTotal,
            )

            notifyFinal(
                title =
                    "Renault Docs · .rdpkg готовий",
                text =
                    label +
                    " · " +
                    prepared.sectionCount +
                    " native",
                projectId =
                    request.projectId,
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            val destinationCleaned =
                validatedDestination ||
                    deleteDestination(
                        destination,
                    )

            runStore.markCancelled(
                "Підготовку .rdpkg скасовано. Source не змінено, private staging очищено." +
                    if (
                        destinationCleaned
                    ) {
                        ""
                    } else {
                        "\nУвага: неповний .rdpkg не вдалося видалити автоматично."
                    },
            )

            notifyFinal(
                title =
                    "Створення .rdpkg скасовано",
                text =
                    "Source не змінено.",
                projectId =
                    request.projectId,
            )
        } catch (
            error:
                Throwable,
        ) {
            val destinationCleaned =
                validatedDestination ||
                    deleteDestination(
                        destination,
                    )

            val message =
                (
                    error.message
                        ?: "Невідома помилка Kotlin-native підготовки."
                ) +
                    if (
                        destinationCleaned
                    ) {
                        ""
                    } else {
                        "\nУвага: неповний .rdpkg не вдалося видалити автоматично."
                    }

            runStore.fail(
                message,
            )

            notifyFinal(
                title =
                    "Не вдалося створити .rdpkg",
                text =
                    message,
                projectId =
                    request.projectId,
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

    private fun parseRequest(
        intent: Intent,
    ): StartRequest? {
        val requestId =
            intent.getStringExtra(
                EXTRA_REQUEST_ID,
            )
                ?: return null
        val sourceTreeUri =
            intent.getStringExtra(
                EXTRA_SOURCE_URI,
            )
                ?: return null
        val sourceName =
            intent.getStringExtra(
                EXTRA_SOURCE_NAME,
            )
                ?: "Renault"
        val destinationUri =
            intent.getStringExtra(
                EXTRA_DESTINATION_URI,
            )
                ?: return null
        val projectId =
            intent.getStringExtra(
                EXTRA_PROJECT_ID,
            )
                ?: return null
        val projectTitle =
            intent.getStringExtra(
                EXTRA_PROJECT_TITLE,
            )
                ?: return null
        val model =
            intent.getStringExtra(
                EXTRA_MODEL,
            )
                ?: return null

        return StartRequest(
            requestId =
                requestId,
            sourceTreeUri =
                sourceTreeUri,
            sourceName =
                sourceName,
            destinationUri =
                destinationUri,
            projectId =
                projectId,
            projectTitle =
                projectTitle,
            model =
                model,
        )
    }

    private fun updateNotificationThrottled(
        title: String,
        text: String,
        projectId: String,
        cancellable: Boolean,
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

        updateNotification(
            title =
                title,
            text =
                text,
            projectId =
                projectId,
            cancellable =
                cancellable,
        )
    }

    private fun updateNotification(
        title: String,
        text: String,
        projectId: String? = null,
        cancellable: Boolean,
    ) {
        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        title,
                    text =
                        text,
                    projectId =
                        projectId,
                    cancellable =
                        cancellable,
                ),
            )
    }

    private fun notifyFinal(
        title: String,
        text: String,
        projectId: String,
    ) {
        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        title,
                    text =
                        text,
                    projectId =
                        projectId,
                    cancellable =
                        false,
                ),
            )
    }

    private fun buildNotification(
        title: String,
        text: String,
        projectId: String?,
        cancellable: Boolean,
    ): Notification {
        val openIntent =
            if (
                projectId !=
                null
            ) {
                ProjectActivity.intent(
                    context =
                        this,
                    projectId =
                        projectId,
                )
            } else {
                Intent(
                    this,
                    MainActivity::class.java,
                )
            }

        val contentIntent =
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                openIntent,
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
                val state =
                    runStore.load()

                if (
                    state.isRunning
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

                if (
                    cancellable
                ) {
                    val cancelIntent =
                        Intent(
                            this@NativeRdpkgPreparationService,
                            NativeRdpkgPreparationService::class.java,
                        ).apply {
                            action =
                                ACTION_CANCEL
                        }

                    val cancelPending =
                        PendingIntent.getService(
                            this@NativeRdpkgPreparationService,
                            NOTIFICATION_ID +
                                1,
                            cancelIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE,
                        )

                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Скасувати",
                        cancelPending,
                    )
                }
            }
            .build()
    }

    private fun ensureNotificationChannel() {
        notificationManager()
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Renault native .rdpkg",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description =
                        "Kotlin-native підготовка Renault raw folder у .rdpkg"
                },
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
                        ":NativeRdpkgPreparation",
                )
                .apply {
                    setReferenceCounted(
                        false,
                    )
                    acquire(
                        WAKE_LOCK_TIMEOUT_MS,
                    )
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

    private fun deleteDestination(
        uri: Uri,
    ): Boolean {
        val resolver =
            contentResolver

        val deletedByResolver =
            runCatching {
                resolver.delete(
                    uri,
                    null,
                    null,
                ) >
                    0
            }.getOrDefault(
                false,
            )

        if (
            deletedByResolver
        ) {
            return true
        }

        if (
            !DocumentsContract.isDocumentUri(
                this,
                uri,
            )
        ) {
            return false
        }

        return runCatching {
            DocumentsContract.deleteDocument(
                resolver,
                uri,
            )
        }.getOrDefault(
            false,
        )
    }

    override fun onDestroy() {
        releaseWakeLock()

        active =
            false

        super.onDestroy()
    }

    private fun stopForMissingRequest():
        Int {
        active =
            false

        workerRunning.set(
            false,
        )
        startInFlight.set(
            false,
        )

        runStore.fail(
            "Не вдалося відновити параметри native .rdpkg підготовки.",
        )

        stopSelf()

        return START_NOT_STICKY
    }

    companion object {
        @Volatile
        private var active =
            false

        private val startInFlight =
            AtomicBoolean(
                false,
            )

        private const val ACTION_START =
            "com.saney.renaultdocs.action.NATIVE_RDPKG_START"

        private const val ACTION_CANCEL =
            "com.saney.renaultdocs.action.NATIVE_RDPKG_CANCEL"

        private const val EXTRA_REQUEST_ID =
            "requestId"

        private const val EXTRA_SOURCE_URI =
            "sourceUri"

        private const val EXTRA_SOURCE_NAME =
            "sourceName"

        private const val EXTRA_DESTINATION_URI =
            "destinationUri"

        private const val EXTRA_PROJECT_ID =
            "projectId"

        private const val EXTRA_PROJECT_TITLE =
            "projectTitle"

        private const val EXTRA_MODEL =
            "model"

        private const val CHANNEL_ID =
            "renault_native_rdpkg"

        private const val NOTIFICATION_ID =
            3702

        private const val NOTIFICATION_THROTTLE_MS =
            700L

        private const val WAKE_LOCK_TIMEOUT_MS =
            6L *
                60L *
                60L *
                1000L

        fun isActive():
            Boolean =
            active

        fun start(
            context: Context,
            request: StartRequest,
        ) {
            if (
                !startInFlight.compareAndSet(
                    false,
                    true,
                )
            ) {
                return
            }

            val claimed =
                NativeRdpkgRunStore(
                    context,
                ).claimStartRequest(
                    request.requestId,
                )

            if (
                !claimed
            ) {
                startInFlight.set(
                    false,
                )
                return
            }

            val intent =
                Intent(
                    context,
                    NativeRdpkgPreparationService::class.java,
                ).apply {
                    action =
                        ACTION_START
                    putExtra(
                        EXTRA_REQUEST_ID,
                        request.requestId,
                    )
                    putExtra(
                        EXTRA_SOURCE_URI,
                        request.sourceTreeUri,
                    )
                    putExtra(
                        EXTRA_SOURCE_NAME,
                        request.sourceName,
                    )
                    putExtra(
                        EXTRA_DESTINATION_URI,
                        request.destinationUri,
                    )
                    putExtra(
                        EXTRA_PROJECT_ID,
                        request.projectId,
                    )
                    putExtra(
                        EXTRA_PROJECT_TITLE,
                        request.projectTitle,
                    )
                    putExtra(
                        EXTRA_MODEL,
                        request.model,
                    )
                }

            try {
                context.startForegroundService(
                    intent,
                )
            } catch (
                error:
                    Throwable,
            ) {
                startInFlight.set(
                    false,
                )
                throw error
            }
        }

        fun requestCancel(
            context: Context,
        ) {
            val intent =
                Intent(
                    context,
                    NativeRdpkgPreparationService::class.java,
                ).apply {
                    action =
                        ACTION_CANCEL
                }

            context.startService(
                intent,
            )
        }
    }
}
