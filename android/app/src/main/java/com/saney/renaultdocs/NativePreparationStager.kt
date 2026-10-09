package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * First stage of the Kotlin-native raw-folder pipeline.
 *
 * The raw SAF tree is scanned once for metadata, then every source file is read
 * once and copied into app-private local staging. HTM/HTML/JS path fixes are
 * applied during that copy so later compiler passes never need to revisit SAF.
 */
class NativePreparationStager(
    private val context: Context,
    private val onProgress: (Progress) -> Unit = {},
    private val isCancelled: () -> Boolean = { false },
) {
    enum class Phase {
        SCANNING,
        PREPARING,
        COPYING,
        READY,
    }

    data class Progress(
        val phase: Phase,
        val message: String,
        val filesFound: Int = 0,
        val filesDone: Int = 0,
        val filesTotal: Int = 0,
        val bytesDone: Long = 0L,
        val bytesTotal: Long = 0L,
    )

    data class Result(
        val stagingRoot: File,
        val filesTotal: Int,
        val directoriesTotal: Int,
        val changedFiles: Int,
        val changesTotal: Int,
        val relativeFiles: Set<String>,
        val relativeDirectories: Set<String>,
        val singleVolumeSourceFolder: String?,
    )

    private data class SourceFile(
        val relativePath: String,
        val uri: Uri? = null,
        val localFile: File? = null,
        val mimeType: String?,
        val size: Long,
    )

    private data class SourceScan(
        val files: List<SourceFile>,
        val directories: Set<String>,
    )

    fun prepare(
        sourceTreeUri: Uri,
        stagingToken: String,
    ): Result {
        checkCancelled()

        val root =
            DocumentFile.fromTreeUri(
                context,
                sourceTreeUri,
            )
                ?: error(
                    "Source папка недоступна."
                )

        require(
            root.isDirectory,
        ) {
            "Source більше не є папкою."
        }

        emit(
            Phase.SCANNING,
            "Сканую raw Renault source…",
        )

        val scan =
            scanSource(
                treeUri =
                    sourceTreeUri,
                root =
                    root,
            )

        return stageScan(
            scan = scan,
            sourceName =
                root.name
                    ?: stagingToken,
            stagingToken =
                stagingToken,
        )
    }

    fun prepareLocal(
        sourceRoot: File,
        stagingToken: String,
        sourceName: String = sourceRoot.name,
        excludedNestedRawRoots: Set<File> = emptySet(),
    ): Result {
        checkCancelled()

        require(
            sourceRoot.isDirectory,
        ) {
            "Archive raw root більше не є папкою."
        }

        emit(
            Phase.SCANNING,
            "Сканую розпакований Renault raw…",
        )

        // ZIP/7Z/RAR may wrap the actual raw data in ordinary folders.
        // Resolve only a unique nested entrypoint; never choose arbitrarily
        // among several Renault volumes or override a batch chooser.
        val actualRoot =
            if (excludedNestedRawRoots.isEmpty()) {
                ArchiveNativeRawRoot.resolve(sourceRoot)
            } else {
                require(ArchiveNativeRawRoot.hasDirectEntrypoint(sourceRoot)) {
                    "Вибраний archive batch том втратив кореневий INDEX. " +
                        "Вкладені томи не об'єднано автоматично."
                }
                sourceRoot.canonicalFile
            }

        val scan =
            scanLocalSource(
                actualRoot,
                excludedNestedRawRoots,
            )

        return stageScan(
            scan = scan,
            sourceName = sourceName,
            stagingToken = stagingToken,
            sourceRoot = actualRoot,
        )
    }

    private fun stageScan(
        scan: SourceScan,
        sourceName: String,
        stagingToken: String,
        sourceRoot: File? = null,
    ): Result {
        checkCancelled()

        require(
            scan.files.isNotEmpty(),
        ) {
            "Source папка порожня."
        }

        require(
            scan.files.none {
                it.relativePath.equals(
                    "renault-dataset.json",
                    ignoreCase =
                        true,
                )
            },
        ) {
            "Ця папка вже схожа на підготовлений Renault dataset."
        }

        val exactFiles =
            scan.files
                .map {
                    it.relativePath
                }
                .toSet()

        require(
            hasRootEntrypoint(
                exactFiles,
            )
        ) {
            if (sourceRoot != null) {
                ArchiveNativeRawRoot.describeMismatch(sourceRoot, exactFiles)
            } else {
                "Вибрано батьківську або змішану папку. " +
                    "Для .rdpkg вибери raw-папку одного Renault тому, " +
                    "де INDEX.HTM / INDEX.HTML / ACCUEIL.HTM лежить у корені."
            }
        }

        val lowerMap =
            ConverterPathNormalizer
                .buildLowerMap(
                    exactFiles,
                )

        val singleVolumeSourceFolder =
            safeLeafName(
                sourceName
                    .ifBlank {
                        stagingToken
                    },
            )

        val stagingBase =
            File(
                context.noBackupFilesDir,
                STAGING_DIR,
            )

        require(
            stagingBase.exists() ||
                stagingBase.mkdirs(),
        ) {
            "Не вдалося створити private preparation staging."
        }

        val staging =
            File(
                stagingBase,
                safeToken(
                    stagingToken,
                ) +
                    STAGING_SUFFIX,
            )

        if (
            staging.exists()
        ) {
            require(
                staging.deleteRecursively(),
            ) {
                "Не вдалося очистити старий private staging."
            }
        }

        require(
            staging.mkdirs(),
        ) {
            "Не вдалося створити private staging."
        }

        val payloadRoot =
            singleVolumeSourceFolder
                ?.let {
                    name ->
                    File(
                        staging,
                        name,
                    ).also {
                        directory ->
                        require(
                            directory.mkdirs(),
                        ) {
                            "Не вдалося створити single-volume staging root."
                        }
                    }
                }
                ?: staging

        emit(
            phase =
                Phase.PREPARING,
            message =
                "Готую private staging…",
            filesFound =
                scan.files.size,
            filesTotal =
                scan.files.size,
        )

        try {
            scan.directories
                .sortedWith(
                    compareBy<String> {
                        it.count {
                                char ->
                            char ==
                                '/'
                        }
                    }.thenBy {
                        it.lowercase(
                            Locale.ROOT,
                        )
                    }
                )
                .forEach {
                    relative ->
                    checkCancelled()

                    val directory =
                        File(
                            payloadRoot,
                            relative,
                        )

                    require(
                        directory.isDirectory ||
                            directory.mkdirs(),
                    ) {
                        "Не вдалося створити staging папку: " +
                            relative
                    }
                }

            var changedFiles =
                0
            var changesTotal =
                0
            var bytesDone =
                0L
            val bytesTotal =
                scan.files.sumOf {
                    it.size.coerceAtLeast(
                        0L,
                    )
                }
            var lastProgressAt =
                0L

            scan.files
                .forEachIndexed {
                    index,
                    source ->
                    checkCancelled()

                    val changes =
                        copyAndPatch(
                            source =
                                source,
                            staging =
                                payloadRoot,
                            exactFiles =
                                exactFiles,
                            lowerMap =
                                lowerMap,
                            onBytesCopied = {
                                delta ->
                                bytesDone +=
                                    delta

                                val now =
                                    SystemClock.elapsedRealtime()

                                if (
                                    now -
                                        lastProgressAt >=
                                        COPY_PROGRESS_THROTTLE_MS
                                ) {
                                    emit(
                                        phase =
                                            Phase.COPYING,
                                        message =
                                            "Копіюю і нормалізую raw source…",
                                        filesFound =
                                            scan.files.size,
                                        filesDone =
                                            index,
                                        filesTotal =
                                            scan.files.size,
                                        bytesDone =
                                            bytesDone,
                                        bytesTotal =
                                            bytesTotal,
                                    )
                                    lastProgressAt =
                                        now
                                }
                            },
                        )

                    if (
                        changes >
                        0
                    ) {
                        changedFiles +=
                            1
                        changesTotal +=
                            changes
                    }

                    val completed =
                        index +
                            1

                    if (
                        completed ==
                        1 ||
                        completed %
                            COPY_PROGRESS_EVERY ==
                        0 ||
                        completed ==
                        scan.files.size
                    ) {
                        emit(
                            phase =
                                Phase.COPYING,
                            message =
                                "Копіюю і нормалізую raw source… " +
                                    completed +
                                    "/" +
                                    scan.files.size,
                            filesFound =
                                scan.files.size,
                            filesDone =
                                completed,
                            filesTotal =
                                scan.files.size,
                            bytesDone =
                                bytesDone,
                            bytesTotal =
                                bytesTotal,
                        )
                    }
                }

            checkCancelled()

            emit(
                phase =
                    Phase.READY,
                message =
                    "Private staging готовий · " +
                        scan.files.size +
                        " файлів.",
                filesFound =
                    scan.files.size,
                filesDone =
                    scan.files.size,
                filesTotal =
                    scan.files.size,
                bytesDone =
                    bytesDone,
                bytesTotal =
                    bytesTotal,
            )

            return Result(
                stagingRoot =
                    staging,
                filesTotal =
                    scan.files.size,
                directoriesTotal =
                    scan.directories.size,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
                relativeFiles =
                    exactFiles,
                relativeDirectories =
                    scan.directories,
                singleVolumeSourceFolder =
                    singleVolumeSourceFolder,
            )
        } catch (
            error:
                Throwable,
        ) {
            staging.deleteRecursively()
            throw error
        }

    }

    private fun scanLocalSource(
        sourceRoot: File,
        excludedNestedRawRoots: Set<File>,
    ): SourceScan {
        val files =
            mutableListOf<SourceFile>()
        val directories =
            linkedSetOf<String>()

        ArchiveRawVolumeIsolation
            .walkSelectedRoot(
                sourceRoot,
                excludedNestedRawRoots,
            )
            .forEach {
                entry ->
                checkCancelled()

                if (
                    entry ==
                    sourceRoot
                ) {
                    return@forEach
                }

                val relative =
                    sourceRoot
                        .toPath()
                        .relativize(
                            entry.toPath(),
                        )
                        .toString()
                        .replace(
                            File.separatorChar,
                            '/',
                        )

                when {
                    entry.isDirectory -> {
                        directories +=
                            relative
                    }

                    entry.isFile -> {
                        files +=
                            SourceFile(
                                relativePath =
                                    relative,
                                localFile =
                                    entry,
                                mimeType =
                                    null,
                                size =
                                    entry.length(),
                            )

                        if (
                            files.size %
                                FALLBACK_SCAN_PROGRESS_EVERY ==
                            0
                        ) {
                            emit(
                                phase =
                                    Phase.SCANNING,
                                message =
                                    "Сканую розпакований raw · файлів: " +
                                        files.size +
                                        " · папок: " +
                                        directories.size,
                                filesFound =
                                    files.size,
                            )
                        }
                    }
                }
            }

        return SourceScan(
            files =
                files,
            directories =
                directories,
        )
    }

    private fun scanSource(
        treeUri: Uri,
        root: DocumentFile,
    ): SourceScan =
        try {
            scanSourceFast(
                treeUri,
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            throw cancelled
        } catch (
            error:
                Throwable,
        ) {
            emit(
                Phase.SCANNING,
                "Швидкий scanner недоступний · compatibility scan…",
            )

            scanSourceFallback(
                root,
            )
        }

    private fun scanSourceFast(
        treeUri: Uri,
    ): SourceScan {
        val files =
            mutableListOf<SourceFile>()
        val directories =
            linkedSetOf<String>()

        val rootDocumentId =
            DocumentsContract
                .getTreeDocumentId(
                    treeUri,
                )

        fun visit(
            documentId: String,
            prefix: String,
        ) {
            checkCancelled()

            val childrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        documentId,
                    )

            context.contentResolver
                .query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document
                            .COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document
                            .COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document
                            .COLUMN_MIME_TYPE,
                        DocumentsContract.Document
                            .COLUMN_SIZE,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use {
                    cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document
                                .COLUMN_DOCUMENT_ID,
                        )
                    val nameIndex =
                        cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document
                                .COLUMN_DISPLAY_NAME,
                        )
                    val mimeIndex =
                        cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document
                                .COLUMN_MIME_TYPE,
                        )
                    val sizeIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document
                                .COLUMN_SIZE,
                        )

                    while (
                        cursor.moveToNext()
                    ) {
                        checkCancelled()

                        val childId =
                            cursor.getString(
                                idIndex,
                            )
                        val name =
                            cursor.getString(
                                nameIndex,
                            )
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: continue
                        val mime =
                            cursor.getString(
                                mimeIndex,
                            )
                        val relative =
                            if (
                                prefix.isBlank()
                            ) {
                                name
                            } else {
                                prefix +
                                    "/" +
                                    name
                            }

                        if (
                            mime ==
                            DocumentsContract.Document
                                .MIME_TYPE_DIR
                        ) {
                            directories +=
                                relative

                            visit(
                                childId,
                                relative,
                            )
                            continue
                        }

                        val childUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    childId,
                                )

                        val size =
                            if (
                                sizeIndex >=
                                0 &&
                                !cursor.isNull(
                                    sizeIndex,
                                )
                            ) {
                                cursor.getLong(
                                    sizeIndex,
                                )
                            } else {
                                0L
                            }

                        files +=
                            SourceFile(
                                relativePath =
                                    relative,
                                uri =
                                    childUri,
                                mimeType =
                                    mime,
                                size =
                                    size,
                            )

                        if (
                            files.size %
                                SCAN_PROGRESS_EVERY ==
                            0
                        ) {
                            emit(
                                phase =
                                    Phase.SCANNING,
                                message =
                                    "Сканую raw source · файлів: " +
                                        files.size +
                                        " · папок: " +
                                        directories.size,
                                filesFound =
                                    files.size,
                            )
                        }
                    }
                }
                ?: error(
                    "DocumentsProvider не повернув список файлів."
                )
        }

        visit(
            rootDocumentId,
            "",
        )

        emit(
            phase =
                Phase.SCANNING,
            message =
                "Сканування завершено · файлів: " +
                    files.size +
                    " · папок: " +
                    directories.size,
            filesFound =
                files.size,
        )

        return SourceScan(
            files =
                files,
            directories =
                directories,
        )
    }

    private fun scanSourceFallback(
        root: DocumentFile,
    ): SourceScan {
        val files =
            mutableListOf<SourceFile>()
        val directories =
            linkedSetOf<String>()

        fun visit(
            directory: DocumentFile,
            prefix: String,
        ) {
            checkCancelled()

            directory
                .listFiles()
                .forEach {
                    child ->
                    checkCancelled()

                    val name =
                        child.name
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@forEach

                    val relative =
                        if (
                            prefix.isBlank()
                        ) {
                            name
                        } else {
                            prefix +
                                "/" +
                                name
                        }

                    when {
                        child.isDirectory -> {
                            directories +=
                                relative

                            visit(
                                child,
                                relative,
                            )
                        }

                        child.isFile -> {
                            files +=
                                SourceFile(
                                    relativePath =
                                        relative,
                                    uri =
                                        child.uri,
                                    mimeType =
                                        child.type,
                                    size =
                                        child.length(),
                                )

                            if (
                                files.size %
                                    FALLBACK_SCAN_PROGRESS_EVERY ==
                                0
                            ) {
                                emit(
                                    phase =
                                        Phase.SCANNING,
                                    message =
                                        "Compatibility scan · файлів: " +
                                            files.size +
                                            " · папок: " +
                                            directories.size,
                                    filesFound =
                                        files.size,
                                )
                            }
                        }
                    }
                }
        }

        visit(
            root,
            "",
        )

        return SourceScan(
            files =
                files,
            directories =
                directories,
        )
    }

    private fun openSource(
        source: SourceFile,
    ): java.io.InputStream {
        source.localFile
            ?.let {
                file ->
                return file.inputStream()
            }

        source.uri
            ?.let {
                uri ->
                return context.contentResolver
                    .openInputStream(
                        uri,
                    )
                    ?: error(
                        "Не вдалося прочитати: " +
                            source.relativePath
                    )
            }

        error(
            "Source не має доступного backing file: " +
                source.relativePath
        )
    }

    private fun copyAndPatch(
        source: SourceFile,
        staging: File,
        exactFiles: Set<String>,
        lowerMap: Map<String, List<String>>,
        onBytesCopied: (Long) -> Unit = {},
    ): Int {
        val target =
            File(
                staging,
                source.relativePath,
            )

        target.parentFile
            ?.let {
                parent ->
                require(
                    parent.isDirectory ||
                        parent.mkdirs(),
                ) {
                    "Не вдалося створити staging parent: " +
                        source.relativePath
                }
            }

        if (
            isPatchableText(
                source.relativePath,
            )
        ) {
            val raw =
                openSource(
                    source,
                ).use {
                    it.readBytes()
                }

            val patched =
                ConverterPathNormalizer
                    .patchText(
                        sourceRelativePath =
                            source.relativePath,
                        input =
                            raw.toString(
                                Charsets.ISO_8859_1,
                            ),
                        exactFiles =
                            exactFiles,
                        lowerMap =
                            lowerMap,
                    )

            val outputBytes =
                patched.text
                    .toByteArray(
                        Charsets.ISO_8859_1,
                    )

            target.writeBytes(
                outputBytes,
            )
            onBytesCopied(
                source.size
                    .takeIf {
                        it >
                            0L
                    }
                    ?: outputBytes.size.toLong(),
            )

            return patched
                .changes
                .size
        }

        val input =
            openSource(
                source,
            )

        BufferedInputStream(
            input,
            BUFFER_SIZE,
        ).use {
            sourceStream ->
            BufferedOutputStream(
                target.outputStream(),
                BUFFER_SIZE,
            ).use {
                targetStream ->
                val buffer =
                    ByteArray(
                        BUFFER_SIZE,
                    )

                while (
                    true
                ) {
                    checkCancelled()

                    val read =
                        sourceStream.read(
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

                    targetStream.write(
                        buffer,
                        0,
                        read,
                    )
                    onBytesCopied(
                        read.toLong(),
                    )
                }
            }
        }

        return 0
    }

    private fun emit(
        phase: Phase,
        message: String,
        filesFound: Int = 0,
        filesDone: Int = 0,
        filesTotal: Int = 0,
        bytesDone: Long = 0L,
        bytesTotal: Long = 0L,
    ) {
        onProgress(
            Progress(
                phase =
                    phase,
                message =
                    message,
                filesFound =
                    filesFound,
                filesDone =
                    filesDone,
                filesTotal =
                    filesTotal,
                bytesDone =
                    bytesDone,
                bytesTotal =
                    bytesTotal,
            )
        )
    }

    private fun checkCancelled() {
        if (
            isCancelled()
        ) {
            throw ConversionCancelledException()
        }
    }

    companion object {
        private const val STAGING_DIR =
            "native-preparation"

        private const val STAGING_SUFFIX =
            ".staging"

        private const val BUFFER_SIZE =
            1024 * 1024

        private const val SCAN_PROGRESS_EVERY =
            500

        private const val FALLBACK_SCAN_PROGRESS_EVERY =
            100

        private const val COPY_PROGRESS_EVERY =
            100

        private const val COPY_PROGRESS_THROTTLE_MS =
            100L

        fun isPatchableText(
            relativePath: String,
        ): Boolean =
            relativePath
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                ) in
                setOf(
                    "htm",
                    "html",
                    "js",
                )

        fun hasRootEntrypoint(
            relativeFiles: Set<String>,
        ): Boolean =
            relativeFiles.any {
                path ->
                '/' !in
                    path &&
                    path.lowercase(
                        Locale.ROOT,
                    ) in
                    setOf(
                        "index.htm",
                        "index.html",
                        "accueil.htm",
                    )
            }

        fun safeLeafName(
            value: String,
        ): String =
            value
                .replace(
                    Regex(
                        """[\\/\u0000-\u001F]+"""
                    ),
                    "_",
                )
                .trim()
                .trim(
                    '.',
                )
                .ifBlank {
                    "volume"
                }
                .take(
                    120,
                )

        fun safeToken(
            value: String,
        ): String {
            val normalized =
                Normalizer.normalize(
                    value,
                    Normalizer.Form.NFKD,
                )
                    .replace(
                        Regex(
                            "\\p{M}+",
                        ),
                        "",
                    )

            return normalized
                .replace(
                    Regex(
                        "[^A-Za-z0-9._-]+",
                    ),
                    "-",
                )
                .trim(
                    '-',
                    '.',
                    '_',
                )
                .ifBlank {
                    "renault"
                }
                .take(
                    80,
                )
        }
    }
}
