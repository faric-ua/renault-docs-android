package com.saney.renaultdocs

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * v0.5.147 (owner 2026-10-17 05:02): delete a volume together with its files.
 *
 * Only on the owner's explicit choice in «Видалити томи разом з файлами» (list + «так»).
 * The volume's folder and archive are renamed into `rdpkg-trash` of the same place (one
 * rename each — the volume is gone at once), then [startErase] erases the trash in the
 * background. The app start calls [startErase] too, so a half-erased trash is finished later.
 * Logcat tag `RenaultTrash` (Phone Diagnostics shows it).
 */
object VolumeTrash {
    const val TAG = "RenaultTrash"
    private val erasing = AtomicBoolean(false)

    @Volatile var lastProgress: String = ""
        private set

    fun trashDirs(context: Context): List<File> {
        val appContext = context.applicationContext
        return listOfNotNull(
            VolumeTrashCore.trashDirFor(VolumeStorage.phoneRoot(appContext)),
            VolumeStorage.cardRoot(appContext)?.let(VolumeTrashCore::trashDirFor),
        )
    }

    fun pendingItems(context: Context): Int = runCatching { VolumeTrashCore.pendingItems(trashDirs(context)) }.getOrDefault(0)

    fun isErasing(): Boolean = erasing.get()

    /**
     * Moves every copy of the volume (unpacked folder and/or archive, phone and card) into
     * the trash of its own place. Returns how many items were moved; throws when something
     * that exists could not be moved (what was moved stays in the trash).
     */
    fun moveVolumeToTrash(
        context: Context,
        packageId: String,
    ): Int {
        require(packageId.isNotBlank() && !packageId.contains('/') && !packageId.startsWith(".")) {
            "Некоректний id тому."
        }
        val appContext = context.applicationContext
        val roots = listOfNotNull(VolumeStorage.phoneRoot(appContext), VolumeStorage.cardRoot(appContext))
        val stamp = System.currentTimeMillis()
        var moved = 0
        for (root in roots) {
            val trash = VolumeTrashCore.trashDirFor(root)
            val items =
                listOf(
                    File(root, packageId),
                    VolumeStorage.archiveIn(root, packageId),
                    File(VolumeStorage.archiveIn(root, packageId).parentFile, packageId + VolumeArchiveAccess.PART_SUFFIX),
                ).filter { it.exists() }
            for (item in items) {
                VolumeTrashCore.moveIn(item, trash, stamp)
                    ?: error("Не вдалося прибрати файли тому $packageId: ${item.name}")
                moved++
            }
        }
        VolumeFileIndex.forget(packageId)
        // The «Читати з папки» choice means nothing for a volume that is gone.
        runCatching { VolumeArchiveAccess.setReadingOff(appContext, packageId, off = false) }
        runCatching { Log.i(TAG, "TRASH том=$packageId предметів=$moved") }
        return moved
    }

    /**
     * «Видалити томи разом з файлами» after the owner's «Видалити назавжди». Files first
     * (one rename each into the trash), then the record: when the files cannot be moved, the
     * volume stays in the project as it was. Off the main thread — on Android 11 the card can
     * be slow even for a rename. [onDone] is called on the background thread.
     */
    fun deleteVolumesInBackground(
        context: Context,
        store: ProjectStore,
        projectId: String,
        selected: List<VolumeDeleteItem>,
        onDone: (VolumeDeleteOutcome) -> Unit,
    ) {
        val appContext = context.applicationContext
        Thread({
            val withFiles = mutableListOf<String>()
            val recordOnly = mutableListOf<String>()
            val failed = mutableListOf<String>()
            for (item in selected) {
                try {
                    if (item.erasesFiles) {
                        moveVolumeToTrash(appContext, item.packageId!!)
                        store.removeVolume(projectId = projectId, volumeId = item.volumeId)
                        withFiles += item.label
                    } else {
                        store.removeVolume(projectId = projectId, volumeId = item.volumeId)
                        recordOnly += item.label
                    }
                } catch (error: Throwable) {
                    failed += item.label + (error.message?.let { " ($it)" } ?: "")
                }
            }
            runCatching {
                Log.i(TAG, "DELETE з файлами=${withFiles.size} лише запис=${recordOnly.size} помилки=${failed.size}")
            }
            startErase(appContext)
            onDone(VolumeDeleteOutcome(withFiles, recordOnly, failed))
        }, "delete-volumes").start()
    }

    /** Erases the trash in the background; one eraser at a time. */
    fun startErase(context: Context) {
        val appContext = context.applicationContext
        if (pendingItems(appContext) == 0) return
        if (!erasing.compareAndSet(false, true)) return
        Thread({
            val started = System.currentTimeMillis()
            try {
                val result =
                    VolumeTrashCore.eraseAll(trashDirs(appContext)) { deleted, left ->
                        lastProgress = "стерто файлів: $deleted, лишилось томів: $left"
                    }
                runCatching {
                    Log.i(
                        TAG,
                        "ERASE файлів=${result.deletedFiles} лишилось=${result.leftItems} " +
                            "мс=${System.currentTimeMillis() - started}",
                    )
                }
            } catch (error: Throwable) {
                runCatching { Log.w(TAG, "ERASE помилка: ${error.message}") }
            } finally {
                erasing.set(false)
            }
        }, "volume-trash").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }
}
