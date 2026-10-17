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

    /** Volumes waiting to be erased (top-level items in the trash folders). */
    fun pendingItems(trashDirs: List<File>): Int =
        trashDirs.sumOf { dir ->
            dir.listFiles()?.count { it.name != NO_MEDIA } ?: 0
        }

    /**
     * Erases everything inside [trashDirs] (the folders themselves and `.nomedia` stay).
     * [progress] gets the number of files erased so far and the items left, at most every
     * [reportEvery] files and once at the end.
     */
    fun eraseAll(
        trashDirs: List<File>,
        isCancelled: () -> Boolean = { false },
        reportEvery: Int = 500,
        progress: (deletedFiles: Long, leftItems: Int) -> Unit = { _, _ -> },
    ): EraseResult {
        var deleted = 0L
        var sinceReport = 0
        var cancelled = false

        fun counted() {
            deleted++
            sinceReport++
            if (sinceReport >= reportEvery) {
                sinceReport = 0
                progress(deleted, pendingItems(trashDirs))
            }
        }

        // Depth first, without recursion limits on deep trees and without following links.
        fun erase(top: File) {
            val stack = ArrayDeque<Pair<File, Boolean>>()
            stack.addLast(top to false)
            while (stack.isNotEmpty()) {
                if (isCancelled()) {
                    cancelled = true
                    return
                }
                val (file, childrenDone) = stack.removeLast()
                val isLink = runCatching { Files.isSymbolicLink(file.toPath()) }.getOrDefault(false)
                val isFolder = !isLink && file.isDirectory
                if (isFolder && !childrenDone) {
                    stack.addLast(file to true)
                    file.listFiles()?.forEach { child -> stack.addLast(child to false) }
                    continue
                }
                if (file.delete() && !isFolder) counted()
            }
        }

        for (dir in trashDirs) {
            val items = dir.listFiles()?.filter { it.name != NO_MEDIA }.orEmpty()
            for (item in items) {
                erase(item)
                if (cancelled) break
            }
            if (cancelled) break
        }
        val left = pendingItems(trashDirs)
        progress(deleted, left)
        return EraseResult(deletedFiles = deleted, leftItems = left, cancelled = cancelled)
    }
}
