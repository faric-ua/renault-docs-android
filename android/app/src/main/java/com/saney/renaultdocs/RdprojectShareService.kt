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
import androidx.core.content.FileProvider
import java.util.concurrent.atomic.AtomicBoolean

class RdprojectShareService : Service() {
    private lateinit var runStore:
        RdprojectShareRunStore

    private val workerRunning =
        AtomicBoolean(
            false,
        )

    private var lastNotificationAt =
        0L

    override fun onCreate() {
        super.onCreate()

        runStore =
            RdprojectShareRunStore(
                this,
            )

        getSystemService(
            NotificationManager::class.java,
        ).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Renault .rdproject share preparation",
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
            intent?.action !=
                ACTION_START ||
            !workerRunning.compareAndSet(
                false,
                true,
            )
        ) {
            return START_NOT_STICKY
        }

        val projectId =
            intent.getStringExtra(
                EXTRA_PROJECT_ID,
            )

        if (
            projectId.isNullOrBlank()
        ) {
            workerRunning.set(
                false,
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val state =
            runStore.load()

        if (
            !state.isRunning ||
            state.projectId !=
                projectId
        ) {
            workerRunning.set(
                false,
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val store =
            ProjectStore(
                this,
            )
        val project =
            store.project(
                projectId,
            )
        val volumes =
            store.volumes(
                projectId,
            )

        if (
            project ==
                null ||
            volumes.isEmpty()
        ) {
            runStore.fail(
                "Не вдалося знайти проєкт або його томи.",
            )
            workerRunning.set(
                false,
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val blocked =
            RdprojectExporter
                .blockingVolumes(
                    volumes,
                )

        if (
            blocked.isNotEmpty()
        ) {
            runStore.fail(
                "Не всі томи готові для .rdproject.",
            )
            workerRunning.set(
                false,
            )
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notification(
                text =
                    "Готую…",
                ongoing =
                    true,
            ),
        )

        Thread {
            try {
                val file =
                    PreparedShareStore
                        .projectFile(
                            applicationContext,
                            project,
                            volumes,
                        )

                val uri =
                    FileProvider
                        .getUriForFile(
                            applicationContext,
                            packageName +
                                ".files",
                            file,
                        )

                RdprojectExporter
                    .export(
                        context =
                            applicationContext,
                        project =
                            project,
                        volumes =
                            volumes,
                        destinationUri =
                            uri,
                        progressState = {
                            progress ->
                            runStore
                                .updateProgress(
                                    progress,
                                )

                            notifyProgress(
                                progress,
                            )
                        },
                    )
                    .getOrThrow()

                runStore.complete(
                    preparedPath =
                        file.absolutePath,
                    message =
                        "Проєкт " +
                            project.title +
                            " підготовлено · " +
                            volumeCountLabel(
                                volumes.size,
                            ) +
                            ".",
                )

                notificationManager()
                    .notify(
                        NOTIFICATION_ID,
                        notification(
                            text =
                                "Проєкт готовий для поширення.",
                            ongoing =
                                false,
                        ),
                    )
            } catch (
                error:
                    Throwable,
            ) {
                val message =
                    error.message
                        ?: "Невідома помилка підготовки проєкту."

                runStore.fail(
                    message,
                )

                notificationManager()
                    .notify(
                        NOTIFICATION_ID,
                        notification(
                            text =
                                "Помилка підготовки: " +
                                    message,
                            ongoing =
                                false,
                        ),
                    )
            } finally {
                workerRunning.set(
                    false,
                )
                stopForeground(
                    STOP_FOREGROUND_DETACH,
                )
                stopSelf()
            }
        }.start()

        return START_REDELIVER_INTENT
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? =
        null

    private fun notifyProgress(
        progress: OperationProgress,
    ) {
        val now =
            System.currentTimeMillis()

        if (
            now -
                lastNotificationAt <
            NOTIFICATION_THROTTLE_MS &&
            progress.normalizedCurrent !=
                progress.total
        ) {
            return
        }

        lastNotificationAt =
            now

        notificationManager()
            .notify(
                NOTIFICATION_ID,
                notification(
                    text =
                        progress.compactStage(),
                    ongoing =
                        true,
                    current =
                        progress.current,
                    total =
                        progress.total,
                ),
            )
    }

    private fun notification(
        text: String,
        ongoing: Boolean,
        current: Int? = null,
        total: Int? = null,
    ): Notification {
        val state =
            runStore.load()

        val openIntent =
            Intent(
                this,
                MainActivity::class.java,
            )

        val pending =
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
                android.R.drawable.stat_sys_upload,
            )
            .setContentTitle(
                "Renault Docs · підготовка проєкту",
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
                    val resolvedCurrent =
                        current
                            ?: state.progressCurrent
                    val resolvedTotal =
                        total
                            ?: state.progressTotal

                    if (
                        resolvedTotal >
                        0
                    ) {
                        setProgress(
                            resolvedTotal,
                            resolvedCurrent
                                .coerceIn(
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

    private fun volumeCountLabel(
        count: Int,
    ): String {
        val mod100 =
            count %
                100
        val mod10 =
            count %
                10

        val noun =
            when {
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

        return count
            .toString() +
            " " +
            noun
    }

    private fun notificationManager():
        NotificationManager =
        getSystemService(
            NotificationManager::class.java,
        )

    companion object {
        private const val ACTION_START =
            "com.saney.renaultdocs.action.RDPROJECT_SHARE_START"
        private const val EXTRA_PROJECT_ID =
            "projectId"
        private const val CHANNEL_ID =
            "renault_rdproject_share"
        private const val NOTIFICATION_ID =
            3706
        private const val NOTIFICATION_THROTTLE_MS =
            300L

        fun start(
            context: Context,
            projectId: String,
        ): Boolean {
            val store =
                RdprojectShareRunStore(
                    context,
                )

            if (
                !store.begin(
                    projectId,
                )
            ) {
                return false
            }

            val intent =
                Intent(
                    context,
                    RdprojectShareService::class.java,
                ).apply {
                    action =
                        ACTION_START
                    putExtra(
                        EXTRA_PROJECT_ID,
                        projectId,
                    )
                }

            return try {
                context.startForegroundService(
                    intent,
                )
                true
            } catch (
                error:
                    Throwable,
            ) {
                store.fail(
                    error.message
                        ?: "Не вдалося запустити підготовку проєкту.",
                )
                false
            }
        }
    }
}
