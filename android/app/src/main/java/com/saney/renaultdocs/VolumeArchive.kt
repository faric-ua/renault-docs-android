package com.saney.renaultdocs

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Archives plan v2, stage 1 (owner 2026-10-17 00:32: «починай перший етап»): a volume read
 * straight from its one `.rdpkg` file, without the thousands of unpacked files.
 *
 * A `.rdpkg` is the volume folder packed 1:1 ([RdpkgZipWriter]: files only, `rdpkg.json` and
 * `renault-dataset.json` at the top), so a path inside the volume is the same path inside the
 * archive. The table of contents (~6–8 thousand entries) is read once when the archive is
 * opened; folders are made up from the paths (the writer stores no folder entries).
 *
 * Entry names are cleaned exactly like [RdpkgImporter] does when it unpacks (backslashes,
 * leading `/`, empty and `.` parts); an entry with `..` is never offered (the importer refuses
 * such a package). When two entries clean to the same path, the later one wins — unpacking
 * would also leave the later one.
 *
 * Every file and folder reports the archive file's own time: a remade archive gets a new
 * time, so caches keyed by size and time (drawn PDF pages, extracted PDFs) never mix
 * an old archive with a new one.
 *
 * Read-only. Plain Kotlin + java.util.zip, no Android types — tested on the JVM.
 */
class VolumeArchive private constructor(
    val file: File,
    private val zip: ZipFile,
    private val entries: Map<String, ZipEntry>,
    private val folders: Map<String, List<String>>,
    val archiveTime: Long,
    val archiveSize: Long,
) : Closeable {
    data class Child(
        val name: String,
        val path: String,
        val attributes: VolumeFileIndex.Attributes,
    )

    val fileCount: Int get() = entries.size

    /** Null — no such file or folder. `""` is the volume itself (a folder). */
    fun attributes(path: String): VolumeFileIndex.Attributes? {
        entries[path]?.let { return VolumeFileIndex.Attributes(false, it.size.coerceAtLeast(0L), archiveTime) }
        if (folders.containsKey(path)) return VolumeFileIndex.Attributes(true, 0L, archiveTime)
        return null
    }

    /** Folders first, then by name ignoring case — the order of the folder listing. Null — not a folder. */
    fun children(path: String): List<Child>? {
        val names = folders[path] ?: return null
        return names
            .mapNotNull { name ->
                val childPath = if (path.isEmpty()) name else "$path/$name"
                attributes(childPath)?.let { Child(name, childPath, it) }
            }.sortedWith(
                compareBy<Child> { !it.attributes.isDirectory }
                    .thenBy { it.name.lowercase(Locale.ROOT) },
            )
    }

    /** The file's bytes as a stream, or null when there is no such file. */
    fun open(path: String): InputStream? {
        val entry = entries[path] ?: return null
        return zip.getInputStream(entry)
    }

    /** A small file read whole (e.g. `rdpkg.json`); null when missing or over [maxBytes]. */
    fun readSmall(
        path: String,
        maxBytes: Int = 1024 * 1024,
    ): ByteArray? {
        val entry = entries[path] ?: return null
        if (entry.size > maxBytes) return null
        return open(path)?.use { it.readBytes() }
    }

    override fun close() {
        runCatching { zip.close() }
    }

    companion object {
        /** Opens [file] and reads its table of contents. Throws when it is not a readable ZIP. */
        fun open(file: File): VolumeArchive {
            val zip = ZipFile(file)
            try {
                val entries = LinkedHashMap<String, ZipEntry>()
                val folders = HashMap<String, MutableSet<String>>()
                folders[""] = LinkedHashSet()
                val iterator = zip.entries()
                while (iterator.hasMoreElements()) {
                    val entry = iterator.nextElement()
                    val path = cleanEntryName(entry.name) ?: continue
                    if (path.isEmpty()) continue
                    val parts = path.split('/')
                    // Folders on the way to the entry.
                    var parent = ""
                    val folderParts = if (entry.isDirectory) parts.size else parts.size - 1
                    for (i in 0 until folderParts) {
                        val folder = if (parent.isEmpty()) parts[i] else parent + "/" + parts[i]
                        folders.getOrPut(parent) { LinkedHashSet() }.add(parts[i])
                        folders.getOrPut(folder) { LinkedHashSet() }
                        parent = folder
                    }
                    if (!entry.isDirectory) {
                        // A file and a folder with the same path cannot both be unpacked: keep the folder.
                        if (folders.containsKey(path)) continue
                        folders.getOrPut(parent) { LinkedHashSet() }.add(parts.last())
                        entries[path] = entry
                    }
                }
                // A name used both for a file and (later) for a folder: the folder wins.
                entries.keys.removeAll { folders.containsKey(it) }
                return VolumeArchive(
                    file = file,
                    zip = zip,
                    entries = entries,
                    folders = folders.mapValues { it.value.toList() },
                    archiveTime = file.lastModified(),
                    archiveSize = file.length(),
                )
            } catch (error: Throwable) {
                runCatching { zip.close() }
                throw error
            }
        }

        /** Same cleaning as [RdpkgImporter] when unpacking; null — unsafe (`..`), never offered. */
        fun cleanEntryName(name: String): String? {
            val parts =
                name.replace('\\', '/')
                    .trimStart('/')
                    .split('/')
                    .filter { it.isNotBlank() && it != "." }
            if (parts.any { it == ".." }) return null
            return parts.joinToString("/")
        }
    }
}
