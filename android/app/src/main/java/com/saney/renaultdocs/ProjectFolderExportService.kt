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
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject

/**
 * Explicit user-selected SAF folder export, one .rdpkg at a time.
 * NEVER wraps files into .rdproject and NEVER publishes a public Drive catalog.
 * All installed package folders are immutable input.
 */
class ProjectFolderExportService : Service() {
    private val dataSyncTimeout = DataSyncTimeoutGate()
    private val active = AtomicBoolean(false)
    private lateinit var runStore: ProjectFolderExportRunStore
    private lateinit var wakeLock: BackgroundWorkWakeLock
    @Volatile private var doneCounts: Pair<Int, Int> = 0 to 0

    override fun onCreate() {
        super.onCreate()
        runStore = ProjectFolderExportRunStore(this)
        wakeLock = BackgroundWorkWakeLock(this, "ProjectVolumeFolderExport")
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Renault окремі томи", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (!active.compareAndSet(false, true)) return START_NOT_STICKY
        val state = runStore.load()
        if (!state.isRunning || state.projectId != intent.getStringExtra(EXTRA_PROJECT)) {
            active.set(false)
            stopSelf()
            return START_NOT_STICKY
        }
        val project = ProjectStore(this).project(state.projectId)
        if (project == null) {
            runStore.fail("Проєкт більше не знайдено.")
            active.set(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, notification("Готую окремі томи…", true))
        wakeLock.acquire()
        Thread {
            try {
                export(state, project)
            } catch (error: Throwable) {
                if (error is OperationCancelledException && !dataSyncTimeout.isExpired) {
                    val (done, skip) = doneCounts
                    runStore.cancelled(done, skip,
                        "Скасовано. Уже експортовано: $done. Готові файли в папці не видалялись.")
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, notification("Експорт окремих томів скасовано.", false),
                    )
                } else if (!dataSyncTimeout.isExpired) {
                    val message = error.message ?: "Не вдалося експортувати окремі томи."
                    runStore.fail(message)
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, notification("Помилка експорту окремих томів.", false),
                    )
                }
            } finally {
                wakeLock.release()
                active.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
        // Do not automatically start another sequence after a process restart.
        return START_NOT_STICKY
    }

    private fun export(state: ProjectFolderExportState, project: RenaultProject) {
        val tree = DocumentFile.fromTreeUri(this, Uri.parse(state.treeUri))
            ?: error("Не вдалося відкрити папку призначення.")
        require(tree.isDirectory && tree.canWrite()) {
            "Немає прав на запис у вибрану папку."
        }
        val all = ProjectStore(this).volumes(project.id)
        val byId = all.associateBy { it.id }
        val allPlan = ProjectVolumeExportPlan.build(project, all)
        val plan = ProjectVolumeExportPlan.select(allPlan, state.volumeIds)
        val selected = plan.map { item ->
            byId[item.volumeId] ?: error("Склад проєкту змінився, том відсутній: ${item.volumeId}")
        }
        require(plan.size <= 250) { "Надто багато томів для однієї операції." }

        val records = JSONArray()
        var completed = 0
        var skipped = 0
        doneCounts = 0 to 0
        val existingNames = tree.listFiles()
            .mapNotNull { it.name?.lowercase(java.util.Locale.ROOT) }
            .toMutableSet()

        plan.forEachIndexed { index, item ->
            doneCounts = completed to skipped
            checkpoint()
            val targetName = item.fileName
            val key = targetName.lowercase(java.util.Locale.ROOT)
            val record = JSONObject()
                .put("volume_id", item.volumeId)
                .put("file_name", targetName)
                .put("document_code", item.documentCode)
                .put("date", item.date)
            if (key in existingNames) {
                // Same filename is NOT content equivalence. Never overwrite it.
                skipped++
                record.put("status", "EXISTING_UNVERIFIED")
                runStore.progress(completed, skipped,
                    "Том ${index + 1}/${plan.size}: файл уже є, не замінюю.")
                records.put(record)
                notifyProgress(completed + skipped, plan.size, "Файл уже є · потрібна звірка")
                return@forEachIndexed
            }

            checkpoint()
            // #182: the folder copy comes from the one prepared file inside the app (⇧).
            // An existing prepared volume is reused; a missing one is packed once.
            val volume = selected[index]
            val existingPrepared = PreparedShareStore.existingVolumeFile(applicationContext, project, volume)
            val prepared: java.io.File =
                if (existingPrepared != null) {
                    record.put("prepared", "REUSED")
                    existingPrepared
                } else {
                    runStore.progress(completed, skipped,
                        "Том ${index + 1}/${plan.size}: пакую ${item.documentCode ?: targetName}")
                    notifyProgress(completed + skipped, plan.size, "Пакую том ${index + 1}/${plan.size}")
                    val created = prepareVolume(project, volume, index, plan.size, completed, skipped, record)
                    record.put("prepared", "CREATED")
                    created
                }
            val localSha = sha256OfFile(prepared)

            checkpoint()
            runStore.progress(completed, skipped,
                "Том ${index + 1}/${plan.size}: копіюю в папку ${item.documentCode ?: targetName}")
            notifyProgress(completed + skipped, plan.size, "Копіюю том ${index + 1}/${plan.size}")
            // Unique .part: no old or user-provided canonical .rdpkg is touched.
            val partName = "$targetName.${System.currentTimeMillis()}.$index.part"
            require(partName.lowercase(java.util.Locale.ROOT) !in existingNames) {
                "Тимчасовий файл уже існує."
            }
            val created = tree.createFile("application/octet-stream", partName)
                ?: error("Не вдалося створити тимчасовий .rdpkg.")
            var finalized = false
            try {
                copyToUri(prepared, created.uri)
                checkpoint()
                val sha = sha256OfUri(created.uri)
                require(sha.equals(localSha, ignoreCase = true)) {
                    "Контрольна сума скопійованого тому не збігається."
                }
                require(tree.findFile(targetName) == null) {
                    "Під час експорту з'явився однойменний файл; заміну заборонено."
                }
                require(created.renameTo(targetName) && created.name == targetName) {
                    "Не вдалося завершити створення файлу з точною назвою."
                }
                finalized = true
                existingNames.add(key)
                record.put("status", "EXPORTED_SHA256_VERIFIED")
                    .put("sha256", sha)
                    .put("size_bytes", created.length())
                // #162 stage 2 (v0.5.123): everything a public catalog entry
                // needs, so the catalog is built from this report + the exact
                // Drive file ids, without downloading the packages again.
                putCatalogIdentity(record, volume, prepared)
                completed++
                doneCounts = completed to skipped
                records.put(record)
                runStore.progress(completed, skipped, "Готово: $targetName")
                notifyProgress(completed + skipped, plan.size, "Том ${index + 1}/${plan.size} готовий")
            } finally {
                if (!finalized) {
                    // Delete only the new incomplete .part created by this run.
                    runCatching { created.delete() }
                }
            }
        }
        dataSyncTimeout.checkActive()
        // This is NOT the public catalog: no Google Drive file IDs are invented.
        val manifest = JSONObject()
            .put("format", "renault-docs-folder-export-v1")
            // v0.5.123: schema 2 adds package_id / vehicle_codes / document_type /
            // document_version / region per exported volume (#162 stage 2).
            .put("schema_version", 2)
            .put("project", JSONObject()
                .put("id", project.id)
                .put("title", project.title)
                .put("model", project.model))
            .put("expected_volumes", plan.size)
            .put("exported_volumes", completed)
            .put("existing_unverified", skipped)
            .put("ready_for_public_catalog", false)
            .put("volumes", records)
        val summaryName = "RenaultDocs-${project.id}-${System.currentTimeMillis()}-export.json"
        require(tree.findFile(summaryName) == null) { "Ім'я звіту експорту зайняте." }
        val report = tree.createFile("application/json", summaryName)
            ?: error("Не вдалося записати JSON-звіт про окремі томи.")
        var committedReport = false
        try {
            contentResolver.openOutputStream(report.uri, "w").use { output ->
                requireNotNull(output) { "Немає доступу до JSON-звіту." }
                output.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            }
            committedReport = true
        } finally {
            if (!committedReport) runCatching { report.delete() }
        }
        runStore.complete(completed, skipped,
            "Експортовано: $completed із ${plan.size}. Уже існували (не перевірені): $skipped. Каталог не публікувався.")
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID, notification("Окремі .rdpkg готові: $completed / ${plan.size}", false),
        )
    }

    /** Between files and inside long loops: timeout first, then the owner's «Скасувати». */
    private fun checkpoint() {
        dataSyncTimeout.checkActive()
        if (runStore.isCancelRequested()) throw OperationCancelledException()
    }

    /** Packs one volume into the app's single prepared slot via a temporary file. */
    private fun prepareVolume(
        project: RenaultProject,
        volume: ProjectVolumeRecord,
        index: Int,
        total: Int,
        completed: Int,
        skipped: Int,
        record: JSONObject,
    ): java.io.File {
        PreparedShareStore.cleanupStaleIncoming(applicationContext)
        val file = PreparedShareStore.volumeFile(applicationContext, project, volume)
        val partial = PreparedShareStore.incomingFile(applicationContext, file)
        try {
            val uri = FileProvider.getUriForFile(applicationContext, "$packageName.files", partial)
            val write = RdpkgExporter.export(
                context = applicationContext,
                volume = volume,
                destinationUri = uri,
                progressState = { progress ->
                    checkpoint()
                    runStore.progress(completed, skipped,
                        "Том ${index + 1}/$total: ${progress.displayText()}")
                },
            ).getOrThrow()
            checkpoint()
            require(sha256OfFile(partial).equals(write.sha256, ignoreCase = true)) {
                "Контрольна сума підготовленого тому не збігається."
            }
            record.put("file_count", write.fileCount)
            PreparedShareStore.commitIncoming(partial, file)
            return file
        } finally {
            if (partial.exists()) runCatching { partial.delete() }
        }
    }

    private fun copyToUri(source: java.io.File, target: Uri) {
        val buffer = ByteArray(1024 * 1024)
        source.inputStream().use { input ->
            contentResolver.openOutputStream(target, "w").use { output ->
                requireNotNull(output) { "Немає доступу для запису в папку." }
                while (true) {
                    checkpoint()
                    val size = input.read(buffer)
                    if (size < 0) break
                    if (size > 0) output.write(buffer, 0, size)
                }
            }
        }
    }

    /** #162 stage 2: package id from the package itself + identity metadata. */
    private fun putCatalogIdentity(
        record: JSONObject,
        volume: ProjectVolumeRecord,
        prepared: java.io.File,
    ) {
        val packageId =
            runCatching {
                java.util.zip.ZipFile(prepared).use { zip ->
                    val entry = zip.getEntry("rdpkg.json") ?: return@use null
                    if (entry.size > 1024 * 1024) return@use null
                    JSONObject(
                        zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) },
                    ).optString("package_id").trim().takeIf { it.isNotBlank() }
                }
            }.getOrNull()
        val metadata = RdpkgExporter.resolvedMetadata(volume)
        record.put("package_id", packageId ?: JSONObject.NULL)
            .put("vehicle_codes", JSONArray(metadata.vehicleCodes))
            .put("document_type", metadata.documentType ?: JSONObject.NULL)
            .put("document_version", metadata.documentVersion ?: JSONObject.NULL)
            .put("region", metadata.region ?: JSONObject.NULL)
    }

    private fun sha256OfFile(file: java.io.File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024)
        file.inputStream().use { input ->
            while (true) {
                dataSyncTimeout.checkActive()
                val size = input.read(buffer)
                if (size < 0) break
                if (size > 0) digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256OfUri(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024)
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Не вдалося прочитати створений .rdpkg для перевірки." }
            while (true) {
                dataSyncTimeout.checkActive()
                val size = input.read(buffer)
                if (size < 0) break
                if (size > 0) digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun notifyProgress(current: Int, total: Int, message: String) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID, notification(message, true, current, total),
        )
    }

    private fun notification(
        message: String, ongoing: Boolean, current: Int = 0, total: Int = 0,
    ): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_document)
            .setContentTitle("Renault Docs · окремі томи")
            .setContentText(message)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .apply {
                if (ongoing) setProgress(total, current.coerceAtMost(total), total == 0)
            }
            .build()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        dataSyncTimeout.expire()
        try {
            if (runStore.load().isRunning) runStore.fail(DataSyncTimeoutUi.MESSAGE)
        } finally {
            runCatching { wakeLock.release() }
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } finally {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_START = "com.saney.renaultdocs.PROJECT_FOLDER_EXPORT"
        private const val EXTRA_PROJECT = "projectId"
        private const val CHANNEL = "renault_project_folder_export"
        private const val NOTIFICATION_ID = 3712

        fun start(
            context: Context,
            projectId: String,
            tree: Uri,
            selectedVolumeIds: List<String>,
        ): Boolean {
            val store = ProjectStore(context)
            val project = store.project(projectId) ?: return false
            val volumes = store.volumes(projectId)
            val plan = runCatching {
                val allPlan = ProjectVolumeExportPlan.build(project, volumes)
                ProjectVolumeExportPlan.select(allPlan, selectedVolumeIds)
            }.getOrElse { return false }
            if (RdprojectShareRunStore(context).load().isRunning) return false
            val runStore = ProjectFolderExportRunStore(context)
            if (!runStore.begin(projectId, tree.toString(), plan.map { it.volumeId })) {
                return false
            }
            return try {
                context.startForegroundService(Intent(context, ProjectFolderExportService::class.java)
                    .apply {
                        action = ACTION_START
                        putExtra(EXTRA_PROJECT, projectId)
                    })
                true
            } catch (error: Throwable) {
                runStore.fail(error.message ?: "Не вдалося запустити експорт томів.")
                false
            }
        }
    }
}
