package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import org.json.JSONObject

/** The volume package is already installed; nothing was changed (v0.5.132). */
class PackageAlreadyInstalledException(
    val packageId: String,
) : IllegalStateException(
        "Том уже встановлено на телефоні — не замінюю: " + packageId,
    )

object RdpkgImporter {
    /**
     * TODO №13 (2026-10-16): the package id of a `.rdpkg` without unpacking it — only
     * `rdpkg.json` is read. Lets an import see «this volume already lies on the phone /
     * card» before spending minutes (hours on the card) unpacking it again. Null when
     * the file has no valid manifest; the normal install then reports the real error.
     */
    fun peekPackageId(
        file: File,
    ): String? =
        runCatching {
            java.util.zip.ZipFile(file).use { zip ->
                val entry = zip.getEntry(PACKAGE_MANIFEST) ?: return@use null
                val json =
                    JSONObject(
                        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() },
                    )
                if (json.optString("format") != FORMAT) return@use null
                json.optString("package_id").trim().takeIf { isSafePackageId(it) }
            }
        }.getOrNull()

    fun isSafePackageId(
        packageId: String,
    ): Boolean =
        packageId.matches(Regex("[A-Za-z0-9._-]{1,160}")) &&
            packageId != "." &&
            packageId != ".." &&
            !packageId.startsWith(".")

    const val FORMAT =
        "renault-volume-package-v1"
    const val SCHEMA_VERSION =
        1
    private const val PACKAGE_MANIFEST =
        "rdpkg.json"
    private const val DATASET_MANIFEST =
        "renault-dataset.json"
    private const val MAX_ENTRY_COUNT =
        250_000
    private const val PROGRESS_THROTTLE_MS =
        100L

    /**
     * Archives plan v2, stage 2 (v0.5.143): imports keep the package as one file. `false`
     * would bring back unpacking every file (the way until v0.5.142).
     */
    const val IMPORT_AS_ARCHIVE =
        true
    private const val WHOLE = 10_000
    private const val HALF = 5_000L

    data class ImportResult(
        val packageId: String,
        val treeUri: Uri,
        val volume: ProjectVolumeRecord,
        val payloadFiles: Int,
        val payloadBytes: Long,
    )

    fun install(
        context: Context,
        packageUri: Uri,
        progress: ((String) -> Unit)? = null,
        progressState: ((OperationProgress) -> Unit)? = null,
        isCancelled: () -> Boolean = { false },
        // v0.5.132 (.rdproject import): never replace a package that is already
        // installed — the import adds only missing volumes and overwrites nothing.
        refuseExistingPackage: Boolean = false,
    ): Result<ImportResult> =
        runCatching {
            if (isCancelled()) throw ConversionCancelledException()
            val appContext =
                context.applicationContext
            // TODO №1 (2026-10-15): new volumes go where Settings → «Де зберігати нові
            // томи» says (phone or memory card). Staging is made in the same place, so
            // activation stays one rename on one disk.
            val chosenPlace =
                VolumeStorage.chosenPlace(appContext)
            val packagesDirectory =
                (
                    VolumeStorage.root(appContext, chosenPlace)
                        ?: throw IllegalStateException(VolumeStoragePolicy.CARD_NOT_AVAILABLE_FOR_INSTALL)
                )
                    .apply {
                        mkdirs()
                    }
            // TODO №12: on the card the volumes folder is marked `.nomedia` before the
            // first volume file is written (owner's rule 2026-10-15).
            if (chosenPlace == VolumePlace.CARD) {
                VolumeStorage.ensureNoMedia(packagesDirectory)
            }

            // Archives plan v2, stage 2 (owner 2026-10-17 03:10): the package is kept as one
            // checked file and the volume is read from it — no thousands of files to write.
            // Null only when this volume already lies unpacked and may be replaced: that
            // replacement keeps the old way below.
            if (IMPORT_AS_ARCHIVE) {
                installAsArchive(
                    context = appContext,
                    packageUri = packageUri,
                    chosenPlace = chosenPlace,
                    packagesDirectory = packagesDirectory,
                    progressState = progressState,
                    isCancelled = isCancelled,
                    refuseExistingPackage = refuseExistingPackage,
                )?.let { return@runCatching it }
            }

            val staging =
                File(
                    packagesDirectory,
                    ".import-" +
                        UUID.randomUUID()
                            .toString(),
                )

            staging.deleteRecursively()

            require(
                staging.mkdirs(),
            ) {
                "Не вдалося створити тимчасову папку для імпорту."
            }

            try {
                progress?.invoke(
                    "Імпортую .rdpkg…",
                )
                progressState?.invoke(
                    OperationProgress.indeterminate(
                        "Читаю пакет…",
                    ),
                )

                val extraction =
                    extract(
                        context =
                            appContext,
                        packageUri =
                            packageUri,
                        staging =
                            staging,
                        progress =
                            progress,
                        progressState =
                            progressState,
                        isCancelled =
                            isCancelled,
                    )

                if (isCancelled()) throw ConversionCancelledException()
                progressState?.invoke(
                    OperationProgress(
                        stage = "Перевіряю пакет…",
                        itemCurrent = extraction.fileCount,
                        itemTotal = extraction.fileCount,
                        itemLabel = "Розпаковано файлів",
                    ),
                )

                val packageMetadata =
                    readPackageMetadata(
                        staging,
                    )

                validateDatasetPayload(
                    staging =
                        staging,
                    packageMetadata =
                        packageMetadata,
                )

                if (isCancelled()) throw ConversionCancelledException()
                progressState?.invoke(
                    OperationProgress(
                        stage = "Встановлюю том…",
                        itemCurrent = extraction.fileCount,
                        itemTotal = extraction.fileCount,
                        itemLabel = "Розпаковано файлів",
                    ),
                )

                // Where this volume is installed now (phone or card), if anywhere.
                val previousDirectory =
                    VolumeStorage.locate(
                        appContext,
                        packageMetadata.packageId,
                    )
                val finalDirectory =
                    File(
                        packagesDirectory,
                        packageMetadata.packageId,
                    )
                // The old copy is set aside in its own place (rename never crosses disks).
                val backupDirectory =
                    File(
                        previousDirectory?.parentFile ?: packagesDirectory,
                        ".backup-" +
                            packageMetadata.packageId,
                    )

                if (
                    refuseExistingPackage &&
                    previousDirectory != null
                ) {
                    throw PackageAlreadyInstalledException(
                        packageMetadata.packageId,
                    )
                }
                // Last cancellation checkpoint before atomic rename/rollback.
                // Never interrupt the activation transaction halfway through.
                if (isCancelled()) throw ConversionCancelledException()
                backupDirectory
                    .deleteRecursively()

                if (
                    previousDirectory != null
                ) {
                    require(
                        previousDirectory.renameTo(
                            backupDirectory,
                        ),
                    ) {
                        "Не вдалося підготувати оновлення встановленого пакета."
                    }
                    VolumeFileIndex.forget(
                        packageMetadata.packageId,
                    )
                }

                var activated =
                    false

                try {
                    require(
                        staging.renameTo(
                            finalDirectory,
                        ),
                    ) {
                        "Не вдалося активувати імпортований пакет."
                    }

                    activated =
                        true

                    // v0.5.138: forget the remembered folder and files of this volume.
                    VolumeFileIndex.forget(
                        packageMetadata.packageId,
                    )

                    VolumeStorage.rememberPlace(
                        appContext,
                        packageMetadata.packageId,
                        chosenPlace,
                    )

                    val treeUri =
                        LocalDatasetDocumentsProvider
                            .treeUriFor(
                                packageMetadata.packageId,
                            )

                    val volume =
                        PreparedVolumeReader
                            .read(
                                context =
                                    appContext,
                                treeUri =
                                    treeUri,
                            )
                            .getOrThrow()

                    if (
                        packageMetadata.projectId !=
                        null &&
                        volume.projectHint !=
                        null
                    ) {
                        require(
                            packageMetadata.projectId ==
                                volume.projectHint
                        ) {
                            "Project id у rdpkg не збігається з dataset manifest."
                        }
                    }

                    backupDirectory
                        .deleteRecursively()

                    ImportResult(
                        packageId =
                            packageMetadata.packageId,
                        treeUri =
                            treeUri,
                        volume =
                            volume,
                        payloadFiles =
                            extraction.fileCount,
                        payloadBytes =
                            extraction.bytes,
                    )
                } catch (
                    error: Throwable,
                ) {
                    if (
                        activated
                    ) {
                        finalDirectory
                            .deleteRecursively()
                    }

                    VolumeFileIndex.forget(
                        packageMetadata.packageId,
                    )

                    if (
                        backupDirectory.exists()
                    ) {
                        backupDirectory.renameTo(
                            previousDirectory ?: finalDirectory,
                        )
                        previousDirectory?.let { previous ->
                            VolumeStorage.rememberPlace(
                                appContext,
                                packageMetadata.packageId,
                                if (previous.parentFile == VolumeStorage.phoneRoot(appContext)) {
                                    VolumePlace.PHONE
                                } else {
                                    VolumePlace.CARD
                                },
                            )
                        }
                    }

                    throw error
                }
            } finally {
                staging.deleteRecursively()
            }
        }

    /**
     * Archives plan v2, stage 2. Copies the package into `rdpkg-archive/.import-<uuid>.part`
     * beside the volumes folder of the chosen place, checks it entry by entry
     * ([VolumePackageCheck]) and only then names it `<package id>.rdpkg`; the volume is then
     * read from it ([VolumeArchiveAccess]). An older archive of the same volume is set aside
     * and put back if anything fails. Removes only its own unfinished file.
     *
     * Returns null — this volume lies unpacked and replacing is allowed: the caller unpacks
     * as before (a folder and a different archive of one volume must never mix).
     */
    private fun installAsArchive(
        context: Context,
        packageUri: Uri,
        chosenPlace: VolumePlace,
        packagesDirectory: File,
        progressState: ((OperationProgress) -> Unit)?,
        isCancelled: () -> Boolean,
        refuseExistingPackage: Boolean,
    ): ImportResult? {
        val archiveDirectory = File(packagesDirectory.parentFile ?: packagesDirectory, VolumeStorage.ARCHIVE_DIR)
        archiveDirectory.mkdirs()
        // Owner's rule 2026-10-15: `.nomedia` first, before any volume file.
        require(VolumeStorage.ensureNoMedia(archiveDirectory)) {
            "Не вдалося підготувати папку для тому."
        }
        val part = File(archiveDirectory, ".import-" + UUID.randomUUID().toString() + ".part")
        try {
            copyPackage(context, packageUri, part, progressState, isCancelled)
            if (isCancelled()) throw ConversionCancelledException()
            // v0.5.144 (M30s 2026-10-17 03:48: «до 50 % швидко, а потім довго»): the check
            // reports after every entry (~8 thousand small files); each report wrote the run
            // state and posted a notification. Now at most ~10 times a second, like unpacking.
            var lastCheckProgressAt = 0L
            val check =
                try {
                    VolumePackageCheck.check(
                        file = part,
                        progress = { done, total ->
                            val now = SystemClock.elapsedRealtime()
                            if (done < total && now - lastCheckProgressAt < PROGRESS_THROTTLE_MS) return@check
                            lastCheckProgressAt = now
                            progressState?.invoke(
                                OperationProgress.measured(
                                    stage = "Перевіряю том…",
                                    current = (HALF + done.coerceAtMost(total) * HALF / total.coerceAtLeast(1L)).toInt(),
                                    total = WHOLE,
                                ),
                            )
                        },
                        isCancelled = isCancelled,
                    )
                } catch (cancelled: OperationCancelledException) {
                    throw ConversionCancelledException()
                }
            val packageId = check.packageId
            val previousFolder = VolumeStorage.locate(context, packageId)
            val previousArchive = VolumeStorage.locateArchive(context, packageId)
            if (refuseExistingPackage && (previousFolder != null || previousArchive != null)) {
                throw PackageAlreadyInstalledException(packageId)
            }
            if (previousFolder != null) return null
            // Last cancellation checkpoint before the rename; never stop halfway through it.
            if (isCancelled()) throw ConversionCancelledException()
            progressState?.invoke(OperationProgress.measured("Встановлюю том…", WHOLE, WHOLE))
            val target = File(archiveDirectory, packageId + VolumeStorage.ARCHIVE_SUFFIX)
            val backup = previousArchive?.let { File(it.parentFile, ".backup-" + packageId + VolumeStorage.ARCHIVE_SUFFIX) }
            backup?.delete()
            if (previousArchive != null && backup != null) {
                require(previousArchive.renameTo(backup)) {
                    "Не вдалося підготувати оновлення встановленого пакета."
                }
                VolumeFileIndex.forget(packageId)
            }
            var activated = false
            try {
                require(part.renameTo(target)) { "Не вдалося активувати імпортований пакет." }
                activated = true
                // A volume kept as one file is always read from it (also forgets old answers).
                VolumeArchiveAccess.setReadingOff(context, packageId, off = false)
                VolumeFileIndex.forget(packageId)
                VolumeStorage.rememberPlace(context, packageId, chosenPlace)
                val treeUri = LocalDatasetDocumentsProvider.treeUriFor(packageId)
                val volume = PreparedVolumeReader.read(context = context, treeUri = treeUri).getOrThrow()
                if (check.projectId != null && volume.projectHint != null) {
                    require(check.projectId == volume.projectHint) {
                        "Project id у rdpkg не збігається з dataset manifest."
                    }
                }
                backup?.delete()
                return ImportResult(
                    packageId = packageId,
                    treeUri = treeUri,
                    volume = volume,
                    payloadFiles = check.fileCount,
                    payloadBytes = check.bytes,
                )
            } catch (error: Throwable) {
                // Only this run's own new file is removed; the older archive goes back.
                if (activated) target.delete()
                VolumeFileIndex.forget(packageId)
                if (previousArchive != null && backup != null && backup.exists()) {
                    backup.renameTo(previousArchive)
                    VolumeStorage.rememberPlace(
                        context,
                        packageId,
                        if (previousArchive == VolumeStorage.archiveIn(VolumeStorage.phoneRoot(context), packageId)) {
                            VolumePlace.PHONE
                        } else {
                            VolumePlace.CARD
                        },
                    )
                }
                throw error
            }
        } finally {
            if (part.exists()) part.delete()
        }
    }

    /** One stream copy of the chosen package; progress 0–50 % (the check is the other half). */
    private fun copyPackage(
        context: Context,
        packageUri: Uri,
        target: File,
        progressState: ((OperationProgress) -> Unit)?,
        isCancelled: () -> Boolean,
    ) {
        val total =
            runCatching {
                context.contentResolver.openAssetFileDescriptor(packageUri, "r")?.use { it.length }
            }.getOrNull()?.takeIf { it > 0L }
        progressState?.invoke(
            if (total != null) {
                OperationProgress.measured("Копіюю том…", 0, WHOLE)
            } else {
                OperationProgress.indeterminate("Копіюю том…")
            },
        )
        val input =
            context.contentResolver.openInputStream(packageUri)
                ?: error("Android не зміг відкрити вибраний .rdpkg.")
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(1024 * 1024)
                var done = 0L
                var lastProgressAt = 0L
                while (true) {
                    if (isCancelled()) throw ConversionCancelledException()
                    val read = source.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    done += read
                    val now = SystemClock.elapsedRealtime()
                    if (total != null && now - lastProgressAt >= PROGRESS_THROTTLE_MS) {
                        lastProgressAt = now
                        progressState?.invoke(
                            OperationProgress.measured(
                                stage = "Копіюю том…",
                                current = (done.coerceAtMost(total) * HALF / total).toInt(),
                                total = WHOLE,
                            ),
                        )
                    }
                }
                output.fd.sync()
            }
        }
        require(target.length() > 0L) { "RDPKG порожній." }
    }

    private fun extract(
        context: Context,
        packageUri: Uri,
        staging: File,
        progress: ((String) -> Unit)?,
        progressState: ((OperationProgress) -> Unit)?,
        isCancelled: () -> Boolean,
    ): ExtractionResult {
        val input =
            context.contentResolver
                .openInputStream(
                    packageUri,
                )
                ?: error(
                    "Android не зміг відкрити вибраний .rdpkg."
                )

        val archiveBytesTotal =
            runCatching {
                context.contentResolver
                    .openAssetFileDescriptor(
                        packageUri,
                        "r",
                    )
                    ?.use {
                        descriptor ->
                        descriptor.length
                    }
            }.getOrNull()
                ?.takeIf {
                    it >
                        0L
                }

        val countingInput =
            CountingInputStream(
                input.buffered(),
            )

        var fileCount =
            0
        var bytes =
            0L
        var expectedFiles:
            Int? =
            null
        var lastProgressAt =
            0L

        ZipInputStream(
            countingInput,
        ).use {
            archive ->
            while (
                true
            ) {
                if (isCancelled()) throw ConversionCancelledException()
                val entry =
                    archive.nextEntry
                        ?: break

                val normalized =
                    normalizedEntryPath(
                        entry.name,
                    )

                if (
                    normalized.isBlank()
                ) {
                    archive.closeEntry()
                    continue
                }

                val destination =
                    File(
                        staging,
                        normalized,
                    ).canonicalFile

                val stagingPath =
                    staging.canonicalPath
                val destinationPath =
                    destination.canonicalPath

                require(
                    destinationPath.startsWith(
                        stagingPath +
                            File.separator,
                    ),
                ) {
                    "RDPKG містить небезпечний шлях: " +
                        entry.name
                }

                if (
                    entry.isDirectory
                ) {
                    require(
                        destination.mkdirs() ||
                            destination.isDirectory,
                    ) {
                        "Не вдалося створити папку: " +
                            normalized
                    }
                    archive.closeEntry()
                    continue
                }

                fileCount +=
                    1

                require(
                    fileCount <=
                        MAX_ENTRY_COUNT,
                ) {
                    "RDPKG містить забагато файлів."
                }

                destination.parentFile
                    ?.let {
                        parent ->
                        require(
                            parent.mkdirs() ||
                                parent.isDirectory,
                        ) {
                            "Не вдалося створити папку для: " +
                                normalized
                        }
                    }

                FileOutputStream(
                    destination,
                ).use {
                    output ->
                    val buffer =
                        ByteArray(
                            DEFAULT_BUFFER_SIZE,
                        )

                    while (
                        true
                    ) {
                        if (isCancelled()) throw ConversionCancelledException()
                        val read =
                            archive.read(
                                buffer,
                            )

                        if (
                            read <
                            0
                        ) {
                            break
                        }

                        if (
                            read ==
                            0
                        ) {
                            continue
                        }

                        output.write(
                            buffer,
                            0,
                            read,
                        )
                        bytes +=
                            read

                        val now =
                            SystemClock.elapsedRealtime()

                        if (
                            now -
                                lastProgressAt >=
                                PROGRESS_THROTTLE_MS
                        ) {
                            publishImportProgress(
                                fileCount =
                                    fileCount,
                                expectedFiles =
                                    expectedFiles,
                                archiveBytes =
                                    countingInput.bytesRead,
                                archiveBytesTotal =
                                    archiveBytesTotal,
                                progressState =
                                    progressState,
                            )
                            lastProgressAt =
                                now
                        }
                    }
                }

                if (
                    normalized ==
                    PACKAGE_MANIFEST
                ) {
                    expectedFiles =
                        runCatching {
                            JSONObject(
                                destination.readText(
                                    Charsets.UTF_8,
                                ),
                            )
                                .optInt(
                                    "payload_file_count",
                                    0,
                                )
                                .takeIf {
                                    it >
                                        0
                                }
                                ?.plus(
                                    1,
                                )
                        }.getOrNull()
                }

                val now =
                    SystemClock.elapsedRealtime()
                val shouldPublish =
                    fileCount ==
                        1 ||
                        (
                            expectedFiles !=
                                null &&
                                fileCount >=
                                expectedFiles!!
                        ) ||
                        now -
                            lastProgressAt >=
                            PROGRESS_THROTTLE_MS

                if (
                    shouldPublish
                ) {
                    progress?.invoke(
                        "Імпортую .rdpkg… " +
                            ukrainianFileCount(
                                fileCount,
                            ),
                    )

                    publishImportProgress(
                        fileCount =
                            fileCount,
                        expectedFiles =
                            expectedFiles,
                        archiveBytes =
                            countingInput.bytesRead,
                        archiveBytesTotal =
                            archiveBytesTotal,
                        progressState =
                            progressState,
                    )

                    lastProgressAt =
                        now
                }

                archive.closeEntry()
            }
        }

        if (isCancelled()) throw ConversionCancelledException()
        require(
            fileCount >
                0,
        ) {
            "RDPKG порожній."
        }

        // We reached ZIP EOF without error: every actual entry was extracted.
        // ZIP central-directory bytes may not be read by ZipInputStream and
        // throttled progress may miss the final entry, leaving the bar at <100%.
        // Complete only this extraction phase; package validation/install follow.
        progressState?.invoke(
            OperationProgress.measured(
                stage = "Розпаковано .rdpkg…",
                current = fileCount,
                total = fileCount,
                itemCurrent = fileCount,
                itemTotal = fileCount,
                itemLabel = "Файлів",
            ),
        )

        return ExtractionResult(
            fileCount =
                fileCount,
            bytes =
                bytes,
        )
    }

    private fun publishImportProgress(
        fileCount: Int,
        expectedFiles: Int?,
        archiveBytes: Long,
        archiveBytesTotal: Long?,
        progressState: ((OperationProgress) -> Unit)?,
    ) {
        val totalBytes =
            archiveBytesTotal

        if (
            totalBytes !=
            null &&
            totalBytes >
            0L
        ) {
            val knownFiles =
                expectedFiles

            progressState?.invoke(
                if (
                    knownFiles !=
                    null &&
                    knownFiles >
                    0
                ) {
                    OperationProgress.weightedItemsAndBytes(
                        stage =
                            "Імпортую…",
                        itemsDone =
                            fileCount,
                        itemsTotal =
                            knownFiles,
                        bytesDone =
                            archiveBytes,
                        bytesTotal =
                            totalBytes,
                        itemLabel =
                            "Файлів",
                    )
                } else {
                    // Without a known file denominator, show *real* byte
                    // progress over the whole compressed source, not only the
                    // 25% byte share reserved by the mixed weighting formula.
                    val byteCurrent =
                        (archiveBytes.coerceIn(0L, totalBytes) * 10_000L /
                            totalBytes).toInt()

                    OperationProgress.measured(
                        stage =
                            "Імпортую…",
                        current =
                            byteCurrent,
                        total =
                            10_000,
                        itemCurrent =
                            fileCount,
                        itemLabel =
                            "Файлів",
                    )
                },
            )
            return
        }

        if (
            expectedFiles !=
            null &&
            expectedFiles >
            0
        ) {
            progressState?.invoke(
                OperationProgress.measured(
                    stage =
                        "Імпортую…",
                    current =
                        fileCount,
                    total =
                        expectedFiles,
                    itemCurrent =
                        fileCount,
                    itemTotal =
                        expectedFiles,
                    itemLabel =
                        "Файлів",
                ),
            )
            return
        }

        progressState?.invoke(
            OperationProgress(
                stage =
                    "Імпортую…",
                itemCurrent =
                    fileCount,
                itemLabel =
                    "Файлів",
            ),
        )
    }

    internal fun ukrainianFileCount(
        count: Int,
    ): String {
        val lastTwo =
            count %
                100

        val noun =
            when {
                lastTwo in
                    11..14 ->
                    "файлів"

                count %
                    10 ==
                    1 ->
                    "файл"

                count %
                    10 in
                    2..4 ->
                    "файли"

                else ->
                    "файлів"
            }

        return count
            .toString() +
            " " +
            noun
    }

    private fun readPackageMetadata(
        staging: File,
    ): PackageMetadata {
        val manifest =
            File(
                staging,
                PACKAGE_MANIFEST,
            )

        require(
            manifest.isFile,
        ) {
            "У .rdpkg немає rdpkg.json."
        }

        val json =
            JSONObject(
                manifest.readText(
                    Charsets.UTF_8,
                ),
            )

        require(
            json.optInt(
                "schema_version",
                0,
            ) ==
                SCHEMA_VERSION,
        ) {
            "Непідтримувана версія rdpkg schema."
        }

        require(
            json.optString(
                "format",
            ) ==
                FORMAT,
        ) {
            "Це не Renodocs .rdpkg."
        }

        val packageId =
            json.optString(
                "package_id",
            )
                .trim()

        require(
            packageId.matches(
                Regex(
                    "[A-Za-z0-9._-]{1,160}",
                ),
            ),
        ) {
            "RDPKG має некоректний package_id."
        }

        return PackageMetadata(
            packageId =
                packageId,
            projectId =
                json.optString(
                    "project_id",
                )
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    },
        )
    }

    private fun validateDatasetPayload(
        staging: File,
        packageMetadata: PackageMetadata,
    ) {
        val manifest =
            File(
                staging,
                DATASET_MANIFEST,
            )

        require(
            manifest.isFile,
        ) {
            "У .rdpkg немає renault-dataset.json."
        }

        val json =
            JSONObject(
                manifest.readText(
                    Charsets.UTF_8,
                ),
            )

        val volumes =
            json.optJSONArray(
                "volumes",
            )
                ?: error(
                    "Dataset manifest у .rdpkg не містить volumes."
                )

        require(
            volumes.length() ==
                1,
        ) {
            "RDPKG має містити рівно один том, знайдено: " +
                volumes.length()
        }

        val entrypoint =
            volumes
                .getJSONObject(
                    0,
                )
                .optString(
                    "entrypoint",
                )
                .trim()

        require(
            entrypoint.isNotBlank(),
        ) {
            "Том у .rdpkg не містить entrypoint."
        }

        val normalized =
            SafDatasetResolver
                .normalize(
                    entrypoint,
                )
                ?: error(
                    "Том у .rdpkg має некоректний entrypoint."
                )

        val entrypointFile =
            File(
                staging,
                normalized,
            )
                .canonicalFile
        val stagingPath =
            staging.canonicalPath

        require(
            entrypointFile.canonicalPath
                .startsWith(
                    stagingPath +
                        File.separator,
                ) &&
                entrypointFile.isFile,
        ) {
            "У .rdpkg не знайдено стартову сторінку тому: " +
                normalized
        }

        val datasetProjectId =
            json.optString(
                "project_id",
            )
                .trim()
                .takeIf {
                    it.isNotBlank()
                }

        if (
            packageMetadata.projectId !=
            null &&
            datasetProjectId !=
            null
        ) {
            require(
                packageMetadata.projectId ==
                    datasetProjectId,
            ) {
                "project_id у rdpkg.json і renault-dataset.json не збігається."
            }
        }
    }

    private fun normalizedEntryPath(
        value: String,
    ): String {
        val normalized =
            value.replace(
                '\\',
                '/',
            )
                .trimStart(
                    '/',
                )

        val parts =
            normalized.split(
                '/',
            )
                .filter {
                    it.isNotBlank() &&
                        it !=
                        "."
                }

        require(
            parts.none {
                it ==
                    ".."
            },
        ) {
            "RDPKG містить небезпечний шлях."
        }

        return parts.joinToString(
            "/",
        )
    }

    private class CountingInputStream(
        input: InputStream,
    ) : FilterInputStream(
        input,
    ) {
        var bytesRead: Long =
            0L
            private set

        override fun read(): Int {
            val value =
                super.read()

            if (
                value >=
                0
            ) {
                bytesRead +=
                    1L
            }

            return value
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            val read =
                super.read(
                    buffer,
                    offset,
                    length,
                )

            if (
                read >
                0
            ) {
                bytesRead +=
                    read
            }

            return read
        }
    }

    private data class PackageMetadata(
        val packageId: String,
        val projectId: String?,
    )

    private data class ExtractionResult(
        val fileCount: Int,
        val bytes: Long,
    )
}
