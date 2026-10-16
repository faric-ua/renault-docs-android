package com.saney.renaultdocs

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

/**
 * v0.5.138 — the quick step of TODO №16 (Phone Diagnostics 33, log 110, M30s 2026-10-16):
 * on the memory card ~3/4 of the wait was not reading documents but *finding* them.
 * To find one file the app listed its whole folder (330 entries — 11–12.5 s), our
 * provider asked the file system 4–5 times per entry (`isDirectory`, type, size, time),
 * resolved every path to its canonical form (one more call per path part), and found
 * the volume folder again on every request. On Android 11 every such call on the card
 * goes through FUSE (~30 ms). The same folder was listed again and again, because the
 * cache lived only inside one short-lived [SafDatasetResolver].
 *
 * This index is shared by the whole app process:
 * - the volume folder is found once per volume ([root]);
 * - a file is checked directly by its known path, one call ([attributes]), no listing;
 * - what was found is remembered ([nodeCached] / [rememberNode]);
 * - [forget] drops everything about a volume (install / replace — [RdpkgImporter]).
 *
 * Safety: paths come only through [SafDatasetResolver.normalize] (no `..`, no leading
 * `/`), and installed volumes are extracted by our own importer, which never creates
 * symbolic links — so a path cannot leave the volume folder. Nothing is ever written
 * or deleted here.
 *
 * Plain Kotlin + java.nio, no Android types — tested on the JVM.
 */
object VolumeFileIndex {
    /** One file-system call: exists, folder or file, size, time. Null — no such file. */
    data class Attributes(
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
    )

    /** Upper bound of remembered files per volume (a volume has ~6–8 thousand). */
    const val MAX_NODES_PER_VOLUME = 20_000

    private val roots = ConcurrentHashMap<String, File>()
    private val nodes = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()
    private val forgetListeners = mutableListOf<(String) -> Unit>()

    fun attributes(file: File): Attributes? =
        try {
            val attrs =
                Files.readAttributes(
                    file.toPath(),
                    BasicFileAttributes::class.java,
                    LinkOption.NOFOLLOW_LINKS,
                )
            if (attrs.isSymbolicLink || attrs.isOther) {
                null
            } else {
                Attributes(
                    isDirectory = attrs.isDirectory,
                    size = if (attrs.isDirectory) 0L else attrs.size(),
                    lastModified = attrs.lastModifiedTime().toMillis(),
                )
            }
        } catch (_: Exception) {
            null
        }

    /**
     * The volume folder, found once. [locate] is the slow lookup (phone, then card); its
     * result is made canonical once and remembered. Null — the volume is not installed
     * (or its card is out); nothing is remembered then, so the next call looks again.
     */
    fun root(
        packageId: String,
        locate: () -> File?,
    ): File? {
        roots[packageId]?.let { return it }
        val found = locate()?.let { runCatching { it.canonicalFile }.getOrNull() } ?: return null
        if (!found.isDirectory) return null
        roots[packageId] = found
        return found
    }

    /** `root/normalized` — [normalized] must come from [SafDatasetResolver.normalize]. */
    fun fileIn(
        root: File,
        normalized: String,
    ): File = if (normalized.isBlank()) root else File(root, normalized)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> nodeCached(
        packageId: String,
        normalized: String,
    ): T? = nodes[packageId]?.get(normalized) as T?

    fun rememberNode(
        packageId: String,
        normalized: String,
        node: Any,
    ) {
        val map = nodes.computeIfAbsent(packageId) { ConcurrentHashMap() }
        if (map.size >= MAX_NODES_PER_VOLUME) map.clear()
        map[normalized] = node
    }

    /** Called by others that keep per-volume memory (e.g. the Fast Pack descriptor). */
    @Synchronized
    fun onForget(listener: (String) -> Unit) {
        forgetListeners += listener
    }

    /** A volume was installed, replaced or moved: everything remembered about it goes. */
    fun forget(packageId: String) {
        roots.remove(packageId)
        nodes.remove(packageId)
        val listeners = synchronized(this) { forgetListeners.toList() }
        listeners.forEach { runCatching { it(packageId) } }
    }

    /** Tests only. */
    fun clearAll() {
        roots.clear()
        nodes.clear()
    }
}
