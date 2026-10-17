package com.saney.renaultdocs

import java.io.File
import java.nio.file.Files

/**
 * v0.5.147 (owner 2026-10-17 05:02): «ці 18 томів ми просто видалимо» — the old unpacked
 * volumes on the memory card of the M30s are deleted and imported again as one file each.
 *
 * Until now the app never deleted volume files (only project records). Deleting is done in
 * two steps, so that the owner never waits on thousands of small files on the card:
 * 1. [moveIn] — the volume's folder / archive is renamed into the trash folder of the same
 *    place (`…/rdpkg-trash`, same disk → one rename). The volume is gone for the app at once.
 * 2. [eraseAll] — the trash is erased file by file in the background. If the app is closed
 *    halfway, the next start goes on from where it stopped (the trash is just a folder).
 *
 * Plain JVM (no Android): tested on the build machine. The eraser touches nothing outside
 * the trash folders it is given and never follows symbolic links.
 */
object VolumeTrashCore {
    const val TRASH_DIR = "rdpkg-trash"
    const val FOUND_REPORT_EVERY = 250
    private const val NO_MEDIA = ".nomedia"

    data class EraseResult(
        val deletedFiles: Long,
        val leftItems: Int,
        val cancelled: Boolean,
    )

    /** The trash folder next to a volumes folder (`…/rdpkg` → `…/rdpkg-trash`). */
    fun trashDirFor(volumesRoot: File): File = File(volumesRoot.parentFile ?: volumesRoot, TRASH_DIR)

    /**
     * Renames [item] (a volume folder or archive file) into [trashDir]. Returns the new place,
     * or null when it could not be moved (then nothing changed). `.nomedia` goes first.
     */
    fun moveIn(
        item: File,
        trashDir: File,
        stamp: Long,
    ): File? {
        if (!item.exists()) return null
        trashDir.mkdirs()
        if (!trashDir.isDirectory) return null
        val marker = File(trashDir, NO_MEDIA)
        if (!marker.exists()) runCatching { marker.createNewFile() }
        var target = File(trashDir, item.name + "-" + stamp)
        var n = 1
        while (target.exists()) {
            target = File(trashDir, item.name + "-" + stamp + "-" + n)
            n++
        }
        return if (item.renameTo(target)) target else null
    }

    /**
     * v0.5.149 (TODO №10): what broken-off operations left beside the volumes — only names
     * the app itself makes for unfinished work:
     * - `…/rdpkg/.import-<uuid>` — unpacking of an import (until v0.5.142, and replacements);
     * - `…/rdpkg-archive/.import-<uuid>.part` — copying of an import as one file (v0.5.143+);
     * - `…/rdpkg-archive/<id>.rdpkg.part` — making a volume archive (v0.5.141+).
     * Never `.backup-…` (may be the only old copy) and never a volume or a finished archive.
     * Looks one level deep only (fast on the card). Sorted by name.
     */
    fun findLeftovers(volumesRoots: List<File>): List<File> =
        volumesRoots.flatMap { root ->
            val imports =
                root.listFiles()?.filter { it.name.startsWith(".import-") }.orEmpty()
            val archiveDir = File(root.parentFile ?: root, "rdpkg-archive")
            val parts =
                archiveDir.listFiles()?.filter { file ->
                    file.isFile &&
                        ((file.name.startsWith(".import-") && file.name.endsWith(".part")) ||
                            (!file.name.startsWith(".") && file.name.endsWith(".rdpkg.part")))
                }.orEmpty()
            (imports + parts).sortedBy { it.name }
        }

    /** Volumes waiting to be erased (top-level items in the trash folders). */
    fun pendingItems(trashDirs: List<File>): Int =
        trashDirs.sumOf { dir ->
            dir.listFiles()?.count { it.name != NO_MEDIA } ?: 0
        }

    /**
     * v0.5.148 (owner 05:25: «як подивитися, чи вони досі стираються… і скільки це времені
     * займе… чому немає прогрес-бара?»): what the eraser is doing now.
     * [itemFiles] is -1 while the files of the current volume are being counted.
     */
    data class EraseProgress(
        val itemIndex: Int,
        val itemTotal: Int,
        val itemFiles: Int,
        // While [counting] (v0.5.151, TODO №24): files found so far in this volume.
        val itemDone: Int,
        val deletedFiles: Long,
        val elapsedMs: Long,
    ) {
        val counting: Boolean get() = itemFiles < 0

        /** Share of the whole run, 0..1: finished volumes plus the share of the current one. */
        val fraction: Double
            get() {
                if (itemTotal <= 0) return 1.0
                val inItem = if (itemFiles > 0) itemDone.toDouble() / itemFiles else 0.0
                return ((itemIndex - 1).coerceAtLeast(0) + inItem.coerceIn(0.0, 1.0)) / itemTotal
            }

        /** Time left, from the speed so far; null while there is too little to judge. */
        val remainingMs: Long?
            get() {
                val f = fraction
                if (f < 0.02 || elapsedMs < 5_000L || f >= 1.0) return null
                return (elapsedMs * (1.0 - f) / f).toLong()
            }
    }

    /**
     * Erases everything inside [trashDirs] (the folders themselves and `.nomedia` stay),
     * one volume at a time: first its files are listed (counted), then erased from that
     * list — so every file is looked at only once and the share done is known.
     * [progress] is called at the start of each volume, after counting, at most every
     * [reportEvery] files and at the end.
     */
    fun eraseAll(
        trashDirs: List<File>,
        isCancelled: () -> Boolean = { false },
        reportEvery: Int = 200,
        clock: () -> Long = { System.currentTimeMillis() },
        progress: (EraseProgress) -> Unit = {},
    ): EraseResult {
        val started = clock()
        val items =
            trashDirs.flatMap { dir ->
                dir.listFiles()?.filter { it.name != NO_MEDIA }.orEmpty().sortedBy { it.name }
            }
        var deleted = 0L
        var cancelled = false

        items@ for ((index, item) in items.withIndex()) {
            val number = index + 1
            progress(EraseProgress(number, items.size, -1, 0, deleted, clock() - started))
            // Post-order list: children before their folder; links are not followed.
            val order = ArrayList<Pair<File, Boolean>>()
            var found = 0
            val stack = ArrayDeque<Pair<File, Boolean>>()
            stack.addLast(item to false)
            while (stack.isNotEmpty()) {
                if (isCancelled()) {
                    cancelled = true
                    break@items
                }
                val (file, childrenDone) = stack.removeLast()
                val isLink = runCatching { Files.isSymbolicLink(file.toPath()) }.getOrDefault(false)
                val isFolder = !isLink && file.isDirectory
                if (isFolder && !childrenDone) {
                    stack.addLast(file to true)
                    file.listFiles()?.forEach { child -> stack.addLast(child to false) }
                    continue
                }
                order += file to isFolder
                // v0.5.151 (TODO №24, M30s 16:17: «рахую файли…» for minutes): files found so far.
                if (!isFolder) {
                    found++
                    if (found % FOUND_REPORT_EVERY == 0) {
                        progress(EraseProgress(number, items.size, -1, found, deleted, clock() - started))
                    }
                }
            }
            val files = order.count { !it.second }
            var done = 0
            progress(EraseProgress(number, items.size, files, 0, deleted, clock() - started))
            for ((file, isFolder) in order) {
                if (isCancelled()) {
                    cancelled = true
                    break@items
                }
                if (file.delete() && !isFolder) {
                    deleted++
                    done++
                    if (done % reportEvery == 0) {
                        progress(EraseProgress(number, items.size, files, done, deleted, clock() - started))
                    }
                } else if (!isFolder) {
                    done++ // could not erase it — counted as looked at; the next start tries again
                }
            }
            progress(EraseProgress(number, items.size, files, files, deleted, clock() - started))
        }
        val left = pendingItems(trashDirs)
        return EraseResult(deletedFiles = deleted, leftItems = left, cancelled = cancelled)
    }
}
