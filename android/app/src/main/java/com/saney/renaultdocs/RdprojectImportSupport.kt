package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Android side of the `.rdproject` import (v0.5.132): reading the file's name/size,
 * its manifest, and the phone's projects and volumes for [RdprojectImportPlanner].
 */
object RdprojectImportSupport {
    data class Preview(
        val uri: Uri,
        val fileName: String,
        val fileSizeBytes: Long,
        val freeBytes: Long,
        val plan: RdprojectImportPlan,
    ) {
        val needBytes: Long?
            get() = RdprojectImportPlanner.estimatedNeedBytes(plan, fileSizeBytes)
        val lowSpace: Boolean
            get() = needBytes?.let { it > freeBytes } == true

        fun needBytes(count: Int): Long? = RdprojectImportPlanner.estimatedNeedBytes(plan, fileSizeBytes, count)

        fun lowSpace(count: Int): Boolean = needBytes(count)?.let { it > freeBytes } == true
    }

    fun readManifest(
        context: Context,
        uri: Uri,
    ): RdprojectManifest =
        context.contentResolver.openInputStream(uri)?.use {
            RdprojectManifestParser.readFromStream(it)
        } ?: throw IllegalStateException(NO_ACCESS)

    fun preview(
        context: Context,
        uri: Uri,
    ): Preview {
        val manifest = readManifest(context, uri)
        return Preview(
            uri = uri,
            fileName = displayName(context, uri),
            fileSizeBytes = fileSize(context, uri),
            freeBytes = freeBytes(context),
            plan = plan(ProjectStore(context), manifest),
        )
    }

    fun targetProject(
        store: ProjectStore,
        manifest: RdprojectManifest,
    ): RenaultProject? =
        manifest.projectId.takeIf { it.isNotBlank() }?.let { store.project(it) }
            ?: manifest.projectModel.takeIf { it.isNotBlank() }?.let { store.findProjectForModel(it) }
            ?: manifest.projectTitle.takeIf { it.isNotBlank() }?.let { store.findProjectForModel(it) }

    fun existingVolumes(store: ProjectStore): List<RdprojectExistingVolume> =
        store.projects().flatMap { project ->
            store.volumes(project.id).map { volume ->
                RdprojectExistingVolume(
                    projectId = project.id,
                    projectTitle = project.title,
                    id = volume.id,
                    documentCode = volume.documentCode,
                    date = volume.date,
                    vehicleCodes = volume.vehicleCodes,
                )
            }
        }

    fun plan(
        store: ProjectStore,
        manifest: RdprojectManifest,
    ): RdprojectImportPlan =
        RdprojectImportPlanner.plan(
            manifest = manifest,
            targetProject = targetProject(store, manifest)?.let { RdprojectExistingProject(it.id, it.title) },
            existing = existingVolumes(store),
        )

    /** Free space where volumes are installed (the import copy lives on the same storage). */
    fun freeBytes(context: Context): Long =
        // TODO №1 (2026-10-15): free space where new volumes go (phone or card).
        VolumeStorage.freeBytes(context, VolumeStorage.chosenPlace(context)) ?: 0L

    fun displayName(
        context: Context,
        uri: Uri,
    ): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "проєкт.rdproject"

    fun fileSize(
        context: Context,
        uri: Uri,
    ): Long =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
                }
        }.getOrNull()
            ?.takeIf { it > 0L }
            ?: runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull()
                ?.takeIf { it > 0L }
            ?: 0L

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "розмір невідомий"
        val gb = bytes / 1024.0 / 1024.0 / 1024.0
        return if (gb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f ГБ", gb)
        } else {
            String.format(java.util.Locale.US, "%.0f МБ", bytes / 1024.0 / 1024.0)
        }
    }

    fun volumeNoun(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        return when {
            mod100 in 11..14 -> "томів"
            mod10 == 1 -> "том"
            mod10 in 2..4 -> "томи"
            else -> "томів"
        }
    }

    const val FILE_PATH_NO_ACCESS =
        "Android не дає прочитати цей файл напряму. " +
            "Натисни «Вибрати цей файл ще раз» і вибери його у списку — далі все як звичайно."

    const val NO_ACCESS =
        "Немає доступу до файлу проєкту. Відкрий файл ще раз (з Telegram, Диска чи «Мої файли»)."
}
