package com.saney.renaultdocs

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** What the volume menu shows about a volume's archive (archives plan v2, stage 1). */
enum class VolumeArchiveStatus {
    /** No archive yet — «Зробити архів тому». */
    NONE,

    /** The volume is read from its archive. */
    ACTIVE,

    /** The archive is there, but the owner chose to read from the folder. */
    OFF,

    /** The archive was made for another copy of the volume (installed again) — not used. */
    STALE,
}

/**
 * Archives plan v2, stage 1 (owner 2026-10-17 00:32): where a volume's archive lies and
 * whether the volume is read from it.
 *
 * The archive lies next to the volumes folder of the same place, in its own folder:
 * phone `noBackupFilesDir/rdpkg-archive/<id>.rdpkg`, card
 * `Android/data/<package>/files/rdpkg-archive/<id>.rdpkg` (with `.nomedia` first). Only the
 * app reads it; no permission is needed; it is not in backups.
 *
 * A volume is read from its archive only when all is true:
 * - the archive file exists (it gets its final name only after [VolumeArchiveVerifier]
 *   found every file equal to the folder — see [RdpkgExportService]);
 * - the owner has not chosen «Читати з папки»;
 * - while the unpacked folder is still there, its `rdpkg.json` equals the archive's — a volume
 *   installed again over an old archive is never read from the old archive.
 *
 * The answer is remembered for the whole app process and dropped together with everything
 * else about the volume ([VolumeFileIndex.forget]: install, replace, archive made, switch).
 * Nothing is ever deleted here.
 */
object VolumeArchiveAccess {
    const val DIR = "rdpkg-archive"
    const val SUFFIX = ".rdpkg"
    const val PART_SUFFIX = ".rdpkg.part"
    private const val MANIFEST = "rdpkg.json"
    private const val PREFS = "volume_archive"
    private const val KEY_OFF = "reading_off_ids"

    private object NoArchive

    private val remembered = ConcurrentHashMap<String, Any>()

    @Volatile private var fileCache: VolumeArchiveFileCache? = null

    init {
        VolumeFileIndex.onForget { packageId -> remembered.remove(packageId) }
    }

    /** The archive the volume is read from now, or null — read from the folder. */
    fun active(
        context: Context,
        packageId: String,
    ): VolumeArchive? {
        when (val known = remembered[packageId]) {
            is VolumeArchive -> return known
            NoArchive -> return null
        }
        val appContext = context.applicationContext
        val found =
            runCatching {
                if (isReadingOff(appContext, packageId)) return@runCatching null
                val folder = VolumeStorage.locate(appContext, packageId)
                val file = archiveFile(appContext, packageId, folder)?.takeIf { it.isFile } ?: return@runCatching null
                val started = SystemClock.elapsedRealtime()
                val archive = VolumeArchive.open(file)
                if (folder != null && !sameManifest(folder, archive)) {
                    archive.close()
                    return@runCatching null
                }
                val ms = SystemClock.elapsedRealtime() - started
                runCatching {
                    Log.i(
                        VolumeReadStats.TAG,
                        "ARCHIVE том=$packageId місце=" +
                            VolumeReadStats.placeLabel(placeOfArchive(appContext, file)) +
                            " файлів=${archive.fileCount} мс=$ms",
                    )
                }
                archive
            }.getOrNull()
        remembered[packageId] = found ?: NoArchive
        return found
    }

    /** For the volume menu. Opens the archive only when it is not already known (once). */
    fun status(
        context: Context,
        packageId: String,
    ): VolumeArchiveStatus {
        val appContext = context.applicationContext
        val folder = VolumeStorage.locate(appContext, packageId)
        val file = archiveFile(appContext, packageId, folder)?.takeIf { it.isFile } ?: return VolumeArchiveStatus.NONE
        if (isReadingOff(appContext, packageId)) return VolumeArchiveStatus.OFF
        if (active(appContext, packageId) != null) return VolumeArchiveStatus.ACTIVE
        // There is a file, the owner did not switch it off, but it is not used: made for
        // another copy of the volume (or unreadable) — offer to make it again.
        return if (file.isFile) VolumeArchiveStatus.STALE else VolumeArchiveStatus.NONE
    }

    /** Where the archive of [packageId] is (or would be made): beside the volume's folder. */
    fun archiveFile(
        context: Context,
        packageId: String,
        folder: File? = VolumeStorage.locate(context, packageId),
    ): File? {
        folder?.parentFile?.parentFile?.let { return File(File(it, DIR), packageId + SUFFIX) }
        // No unpacked folder (later stages): look on the phone, then on the card.
        val candidates =
            listOfNotNull(
                VolumeStorage.phoneRoot(context).parentFile,
                VolumeStorage.cardRoot(context)?.parentFile,
            ).map { File(File(it, DIR), packageId + SUFFIX) }
        return candidates.firstOrNull { it.isFile }
    }

    /** The unfinished file while an archive is being made (it never gets read). */
    fun partFile(
        context: Context,
        packageId: String,
    ): File? = archiveFile(context, packageId)?.let { File(it.parentFile, packageId + PART_SUFFIX) }

    fun isArchivePart(uriText: String?): Boolean =
        uriText != null && uriText.startsWith("file:") && uriText.endsWith(PART_SUFFIX)

    fun isReadingOff(
        context: Context,
        packageId: String,
    ): Boolean = prefs(context).getStringSet(KEY_OFF, emptySet()).orEmpty().contains(packageId)

    /** «Читати з папки» / «Читати з архіву». The archive file stays either way. */
    fun setReadingOff(
        context: Context,
        packageId: String,
        off: Boolean,
    ) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_OFF, emptySet()).orEmpty().toMutableSet()
        val changed = if (off) ids.add(packageId) else ids.remove(packageId)
        if (changed) p.edit().putStringSet(KEY_OFF, ids).commit()
        VolumeFileIndex.forget(packageId)
    }

    /** Copies of PDFs taken out of archives (phone cache, ≤300 MB). */
    fun fileCache(context: Context): VolumeArchiveFileCache =
        fileCache ?: synchronized(this) {
            fileCache ?: VolumeArchiveFileCache(
                File(context.applicationContext.cacheDir, VolumeArchiveFileCache.DIR_NAME),
            ).also { fileCache = it }
        }

    /** A real file for PdfRenderer; the copy-out time goes to the read log. */
    fun realFile(
        context: Context,
        archive: VolumeArchive,
        packageId: String,
        path: String,
    ): File? {
        val started = SystemClock.elapsedRealtime()
        val got = fileCache(context).get(archive, path) ?: return null
        if (got.extracted) {
            val ms = SystemClock.elapsedRealtime() - started
            runCatching {
                Log.i(
                    VolumeReadStats.TAG,
                    "ARCHIVE-PDF том=$packageId файл=«$path» байт=${got.file.length()} мс=$ms",
                )
            }
        }
        return got.file
    }

    private fun sameManifest(
        folder: File,
        archive: VolumeArchive,
    ): Boolean {
        val inFolder = runCatching { File(folder, MANIFEST).readBytes() }.getOrNull() ?: return false
        val inArchive = archive.readSmall(MANIFEST) ?: return false
        return inFolder.contentEquals(inArchive)
    }

    private fun placeOfArchive(
        context: Context,
        file: File,
    ): VolumePlace? {
        val phoneBase = VolumeStorage.phoneRoot(context).parentFile ?: return null
        return if (file.parentFile?.parentFile == phoneBase) VolumePlace.PHONE else VolumePlace.CARD
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
