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
import android.os.SystemClock
import androidx.core.content.FileProvider
import java.io.EOFException
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipException

/**
 * Imports a whole project from one `.rdproject` file (v0.5.132).
 *
 * One pass through the file: volumes that are already on the phone are read past,
 * each missing one is copied to the app cache, installed exactly like a single
 * `.rdpkg`, added to the project, and its copy is deleted. Installed packages are
 * never replaced ([RdpkgImporter.install] with refuseExistingPackage). If the run
 * stops halfway, the volumes already added stay; a new run skips them.
 */
class RdprojectImportService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private lateinit var runStore: RdprojectImportRunStore
    private lateinit var workWakeLock: BackgroundWorkWakeLock
    private val workerRunning = AtomicBoolean(false)

    @Volatile private var importedCount = 0

    @Volatile private var skippedCount = 0
    private var lastNotifyAt = 0L

    override fun onCreate() {
        super.onCreate()
        workWakeLock = BackgroundWorkWakeLock(this, "RdprojectImport")
        runStore = RdprojectImportRunStore(this)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Renault Docs project import",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (!workerRunning.compareAndSet(false, true)) return START_REDELIVER_INTENT
        val state = runStore.load()
        if (!state.isRunning || state.wantedFiles.isEmpty()) {
            workerRunning.set(false)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification("Готую імпорт проєкту…", true))
        workWakeLock.acquire()
        Thread {
            val workDir = File(cacheDir, WORK_DIR)
            try {
                importProject(state, workDir)
            } catch (error: Throwable) {
                if (dataSyncTimeout.isExpired) return@Thread
                val cancelled =
                    runStore.isCancelRequested() &&
                        (
                            error is OperationCancelledException ||
                                error.cause is OperationCancelledException ||
                                error is ConversionCancelledException ||
                                error.cause is ConversionCancelledException
                        )
                if (cancelled) {
                    runStore.cancelled(
                        "Імпорт проєкту скасовано. Уже додано: $importedCount. Недороблений том не додано.",
                    )
                    notify("Імпорт проєкту скасовано.", false)
                    return@Thread
                }
                val message = friendlyError(error)
                runStore.fail(
                    if (importedCount > 0) {
                        "$message Уже додано томів: $importedCount — вони лишаються."
                    } else {
                        message
                    },
                )
                notify("Помилка імпорту проєкту.", false)
            } finally {
                // Only this service's own temporary copy; nothing else is touched.
                runCatching { workDir.deleteRecursively() }
                workWakeLock.release()
                workerRunning.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
        // Like the catalog import: if Android kills the process, the run starts again
        // and skips the volumes that were already added.
        return START_REDELIVER_INTENT
    }

    /** Android 15+: the shared dataSync budget ran out — record it and stop at once. */
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        dataSyncTimeout.expire()
        try {
            if (runStore.load().isRunning) runStore.fail(DataSyncTimeoutUi.MESSAGE)
        } finally {
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

    private fun importProject(
        state: RdprojectImportRunState,
        workDir: File,
    ) {
        val uri = Uri.parse(state.uri)
        val store = ProjectStore(applicationContext)
        workDir.deleteRecursively()
        workDir.mkdirs()
        importedCount = 0
        skippedCount = 0
        metWanted.clear()
        val manifest = RdprojectImportSupport.readManifest(applicationContext, uri)
        val wanted = state.wantedFiles.toSet().intersect(manifest.volumes.map { it.file }.toSet())
        // v0.5.134 (M30s 2026-10-14 20:37): a volume left unticked was announced as
        // «уже є на телефоні». Say which of the two it really is.
        val existingNow = RdprojectImportSupport.existingVolumes(store)
        val presentFiles =
            manifest.volumes
                .filter { volume -> existingNow.any { RdprojectImportPlanner.isSameVolume(volume, it) } }
                .map { it.file }
                .toSet()
        require(wanted.isNotEmpty()) { "Файл змінився: томів, вибраних для імпорту, у ньому немає." }
        val wantedOrder = manifest.volumes.filter { wanted.contains(it.file) }
        val fileSize = state.fileSizeBytes.takeIf { it > 0L } ?: RdprojectImportSupport.fileSize(this, uri)
        val input =
            contentResolver.openInputStream(uri)
                ?: throw IllegalStateException(RdprojectImportSupport.NO_ACCESS)
        val missing =
            RdprojectArchiveWalker.walk(
                input = input,
                manifest = manifest,
                wanted = wanted,
                workDir = workDir,
                freeBytes = { workDir.usableSpace },
                isCancelled = { dataSyncTimeout.isExpired || runStore.isCancelRequested() },
                onProgress = { progress ->
                    dataSyncTimeout.checkActive()
                    val volume = manifest.volumes[progress.volumeIndex]
                    val position = stepLabel(wantedOrder, volume, manifest)
                    val text =
                        if (progress.wanted) {
                            "$position · ${volume.label} — копіюю з файлу"
                        } else {
                            if (presentFiles.contains(volume.file)) {
                                "Пропускаю ${volume.label} — уже є на телефоні"
                            } else {
                                "Пропускаю ${volume.label} — не вибрано"
                            }
                        }
                    publish(text, progress.bytesRead, fileSize)
                },
                onVolume = { volume, file ->
                    installVolume(store, manifest, wantedOrder, volume, file)
                },
            )
        dataSyncTimeout.checkActive()
        val missingWanted = missing.filter { wanted.contains(it) }
        val project = runStore.load().projectTitle.ifBlank { manifest.displayTitle }
        val summary =
            buildString {
                if (importedCount > 0) {
                    append("Додано $importedCount ${RdprojectImportSupport.volumeNoun(importedCount)} до проєкту «$project».")
                } else {
                    append("Нових томів не додано — усі вже є на телефоні.")
                }
                if (skippedCount > 0) append(" Пропущено (уже були): $skippedCount.")
                if (missingWanted.isNotEmpty()) {
                    append(
                        " У файлі не знайдено ${missingWanted.size} ${RdprojectImportSupport.volumeNoun(missingWanted.size)} — " +
                            "файл, схоже, неповний.",
                    )
                }
                append(" Наявні томи не змінено.")
            }
        if (missingWanted.isNotEmpty() && importedCount == 0) {
            runStore.fail(summary)
            notify("Імпорт проєкту: файл неповний.", false)
        } else {
            runStore.complete(summary)
            notify("Імпорт проєкту завершено.", false)
        }
    }

    private fun installVolume(
        store: ProjectStore,
        manifest: RdprojectManifest,
        wantedOrder: List<RdprojectManifestVolume>,
        volume: RdprojectManifestVolume,
        file: File,
    ) {
        dataSyncTimeout.checkActive()
        if (runStore.isCancelRequested()) throw OperationCancelledException()
        val position = stepLabel(wantedOrder, volume, manifest)
        // Checked again right before installing: a repeated run (or a volume added
        // meanwhile) must not add the same volume twice.
        val alreadyThere =
            RdprojectImportSupport.existingVolumes(store).any {
                RdprojectImportPlanner.isSameVolume(volume, it)
            }
        if (alreadyThere) {
            skippedCount++
            runStore.counts(importedCount, skippedCount)
            return
        }
        val text = "$position · ${volume.label} — встановлюю"
        runStore.update(text)
        notifyThrottled(text)
        val uri = FileProvider.getUriForFile(applicationContext, "$packageName.files", file)
        val result =
            RdpkgImporter.install(
                context = applicationContext,
                packageUri = uri,
                progressState = { progress ->
                    dataSyncTimeout.checkActive()
                    runStore.update(
                        message = text,
                        current = progress.normalizedCurrent ?: 0,
                        total = progress.total ?: 0,
                    )
                },
                // Stops only at the importer's safe checkpoints, never mid-activation.
                isCancelled = { dataSyncTimeout.isExpired || runStore.isCancelRequested() },
                refuseExistingPackage = true,
            )
        val imported =
            result.getOrElse { error ->
                if (error is PackageAlreadyInstalledException) {
                    skippedCount++
                    runStore.counts(importedCount, skippedCount)
                    return
                }
                throw error
            }
        dataSyncTimeout.checkActive()
        val project = ensureProject(store, manifest)
        // Once installed, the volume is always added to the project — no hidden package.
        store.upsertVolume(
            projectId = project.id,
            volume = imported.volume.copy(projectHint = project.id),
        )
        importedCount++
        runStore.counts(importedCount, skippedCount)
    }

    /** The project is created only when the first volume is really installed. */
    private fun ensureProject(
        store: ProjectStore,
        manifest: RdprojectManifest,
    ): RenaultProject {
        val saved = runStore.load().projectId
        val project =
            saved.takeIf { it.isNotBlank() }?.let { store.project(it) }
                ?: RdprojectImportSupport.targetProject(store, manifest)
                ?: store.createProject(
                    title = manifest.displayTitle,
                    model = manifest.projectModel.ifBlank { manifest.displayTitle },
                )
        runStore.project(project.id, project.title)
        return project
    }

    /**
     * v0.5.133: «Том N/M» counts wanted volumes in the order they are met in the
     * file. The ZIP is sorted by file name, not in the project's order, so v0.5.132
     * started at «Том 10/23» (M30s 2026-10-14 20:16) although it was the first one.
     */
    private val metWanted = LinkedHashSet<String>()

    private fun stepLabel(
        wantedOrder: List<RdprojectManifestVolume>,
        volume: RdprojectManifestVolume,
        manifest: RdprojectManifest,
    ): String {
        if (!wantedOrder.contains(volume)) {
            return "Том ${manifest.volumes.indexOf(volume) + 1}/${manifest.volumes.size}"
        }
        metWanted.add(volume.file)
        return "Том ${metWanted.indexOf(volume.file) + 1}/${wantedOrder.size}"
    }

    private fun publish(
        text: String,
        bytesRead: Long,
        fileSize: Long,
    ) {
        if (fileSize > 0L) {
            val current = (bytesRead.coerceIn(0L, fileSize) * SCALE / fileSize).toInt()
            runStore.update(text, current, SCALE)
        } else {
            runStore.update(text)
        }
        notifyThrottled(text)
    }

    private fun notifyThrottled(text: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotifyAt < NOTIFY_EVERY_MS) return
        lastNotifyAt = now
        notify(text, true)
    }

    private fun friendlyError(error: Throwable): String =
        when {
            error is RdprojectArchiveWalker.LowSpaceException -> error.message.orEmpty()
            error is RdprojectFormatException -> error.message.orEmpty()
            error is ZipException || error is EOFException || error.cause is EOFException ->
                "Файл проєкту пошкоджений або завантажений не до кінця. Завантаж його ще раз."
            error is SecurityException -> RdprojectImportSupport.NO_ACCESS
            else -> error.message?.takeIf { it.isNotBlank() } ?: "Невідома помилка імпорту проєкту."
        }

    private fun notify(
        text: String,
        ongoing: Boolean,
    ) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text, ongoing))
    }

    private fun notification(
        text: String,
        ongoing: Boolean,
    ): Notification {
        val state = runStore.load()
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, RdprojectImportActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · імпорт проєкту")
            .setContentText(text)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .let { OperationAlert.alertIfFinal(this, it, ongoing, text) }
            .apply {
                if (ongoing) {
                    if (state.progressTotal > 0) {
                        setProgress(
                            state.progressTotal,
                            state.progressCurrent.coerceIn(0, state.progressTotal),
                            false,
                        )
                    } else {
                        setProgress(0, 0, true)
                    }
                }
            }
            .build()
    }

    companion object {
        private const val ACTION_START = "com.saney.renaultdocs.action.RDPROJECT_IMPORT_START"
        private const val CHANNEL_ID = "renault_rdproject_import"
        private const val NOTIFICATION_ID = 3713
        private const val WORK_DIR = "rdproject-import"
        private const val SCALE = 10_000
        private const val NOTIFY_EVERY_MS = 1_000L

        fun start(
            context: Context,
            preview: RdprojectImportSupport.Preview,
            // v0.5.133: only the volumes ticked on the preview screen.
            wantedFiles: List<String> = preview.plan.toImport.map { it.volume.file },
        ): Boolean {
            val store = RdprojectImportRunStore(context)
            val target = preview.plan.targetProject
            if (
                !store.begin(
                    uri = preview.uri.toString(),
                    fileName = preview.fileName,
                    fileSizeBytes = preview.fileSizeBytes,
                    projectId = target?.id.orEmpty(),
                    projectTitle = preview.plan.targetTitle,
                    wantedFiles = wantedFiles,
                )
            ) {
                return false
            }
            return try {
                context.startForegroundService(
                    Intent(context, RdprojectImportService::class.java).apply {
                        action = ACTION_START
                        // Keeps Android's read access to a file opened from Telegram,
                        // Drive or a file manager for as long as the import runs.
                        setDataAndType(preview.uri, "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
                true
            } catch (error: Throwable) {
                store.fail(error.message ?: "Не вдалося запустити імпорт проєкту.")
                false
            }
        }
    }
}
