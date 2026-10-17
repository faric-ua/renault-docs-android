package com.saney.renaultdocs

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * v0.5.147 (owner 2026-10-17 05:02): delete a volume together with its files.
 *
 * Only on the owner's explicit choice in «Видалити томи разом з файлами» (list + «так»).
 * The volume's folder and archive are renamed into `rdpkg-trash` of the same place (one
 * rename each — the volume is gone at once), then the trash is erased in the background.
 *
 * v0.5.148 (owner 05:25: «як подивитися, чи вони досі стираються… скільки часу… чому немає
 * прогрес-бара»): the erasing runs in [VolumeTrashService] (shade notification with a bar,
 * keeps going with the screen off); the home screen shows a card from [progress]; the result
 * is remembered ([lastSummary]) until the owner closes the card. The home screen starts the
 * eraser again when something is left in the trash (app closed halfway).
 * Logcat tag `RenaultTrash` (Phone Diagnostics shows it).
 */
object VolumeTrash {
    const val TAG = "RenaultTrash"
    private const val PREFS = "volume_trash"
    private const val KEY_FINISHED_AT = "finished_at"
    private const val KEY_FILES = "files"
    private const val KEY_VOLUMES = "volumes"
    private const val KEY_MS = "ms"
    private const val KEY_LEFT = "left"
    private const val KEY_DISMISSED_AT = "dismissed_at"
    private val erasing = AtomicBoolean(false)

    /** What the eraser is doing now (null — not erasing). Read by the home card every 100 ms. */
    @Volatile var progress: VolumeTrashCore.EraseProgress? = null
        private set

    data class Summary(
        val finishedAt: Long,
        val files: Long,
        val volumes: Int,
        val ms: Long,
        val left: Int,
    )

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

    /**
     * Erases the trash in [VolumeTrashService] (with a bar in the shade). Called after
     * «Видалити назавжди» and when the home screen opens with something left in the trash.
     * If the service cannot start (Android does not allow it now), erases in a plain
     * background thread instead — the card on the home screen shows it either way.
     */
    fun startErase(context: Context) {
        val appContext = context.applicationContext
        if (erasing.get()) return
        if (pendingItems(appContext) == 0) return
        val started =
            runCatching {
                appContext.startForegroundService(Intent(appContext, VolumeTrashService::class.java))
                true
            }.getOrDefault(false)
        if (!started) {
            Thread({ runEraser(appContext) }, "volume-trash").apply {
                priority = Thread.MIN_PRIORITY
                start()
            }
        }
    }

    /**
     * Runs the eraser on the calling thread (one at a time); [onProgress] gets every step.
     * Returns the result, or null when another eraser is already running.
     */
    fun runEraser(
        context: Context,
        isCancelled: () -> Boolean = { false },
        onProgress: (VolumeTrashCore.EraseProgress) -> Unit = {},
    ): VolumeTrashCore.EraseResult? {
        val appContext = context.applicationContext
        if (!erasing.compareAndSet(false, true)) return null
        val started = System.currentTimeMillis()
        var volumes = 0
        try {
            val result =
                VolumeTrashCore.eraseAll(trashDirs(appContext), isCancelled = isCancelled) { step ->
                    progress = step
                    volumes = step.itemTotal
                    onProgress(step)
                }
            val ms = System.currentTimeMillis() - started
            runCatching {
                Log.i(TAG, "ERASE томів=$volumes файлів=${result.deletedFiles} лишилось=${result.leftItems} мс=$ms")
            }
            if (!result.cancelled && volumes > 0) {
                prefs(appContext).edit()
                    .putLong(KEY_FINISHED_AT, System.currentTimeMillis())
                    .putLong(KEY_FILES, result.deletedFiles)
                    .putInt(KEY_VOLUMES, volumes)
                    .putLong(KEY_MS, ms)
                    .putInt(KEY_LEFT, result.leftItems)
                    .apply()
            }
            return result
        } catch (error: Throwable) {
            runCatching { Log.w(TAG, "ERASE помилка: ${error.message}") }
            return VolumeTrashCore.EraseResult(0, pendingItems(appContext), cancelled = true)
        } finally {
            progress = null
            erasing.set(false)
        }
    }

    /** The last finished erase, until the owner closes its card. */
    fun lastSummary(context: Context): Summary? {
        val p = prefs(context)
        val finishedAt = p.getLong(KEY_FINISHED_AT, 0L)
        if (finishedAt <= 0L || p.getLong(KEY_DISMISSED_AT, 0L) >= finishedAt) return null
        return Summary(
            finishedAt = finishedAt,
            files = p.getLong(KEY_FILES, 0L),
            volumes = p.getInt(KEY_VOLUMES, 0),
            ms = p.getLong(KEY_MS, 0L),
            left = p.getInt(KEY_LEFT, 0),
        )
    }

    fun dismissSummary(
        context: Context,
        finishedAt: Long,
    ) {
        prefs(context).edit().putLong(KEY_DISMISSED_AT, finishedAt).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
