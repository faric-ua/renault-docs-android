package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.Locale

data class SafConversionResult(
    val outputTreeUri: String,
    val filesTotal: Int,
    val changedFiles: Int,
    val changesTotal: Int,
    val volumeCount: Int,
)

class ConversionCancelledException :
    RuntimeException("Конвертацію скасовано.")

class SafConversionEngine(
    private val context: Context,
    private val runStore: ConversionRunStore,
    private val onProgress:
        (
            phase: ConversionRunPhase,
            message: String,
            filesTotal: Int,
            filesDone: Int,
            changedFiles: Int,
            changesTotal: Int,
        ) -> Unit,
) {
    private data class SourceFile(
        val relativePath: String,
        val uri: Uri,
        val mimeType: String?,
        val size: Long,
    )

    private data class SourceScan(
        val files: List<SourceFile>,
        val directories: Set<String>,
    )

    fun execute(
        plan: ConversionPlan,
    ): SafConversionResult {
        val sourceUri =
            Uri.parse(
                plan.sourceUri,
            )
        val destinationUri =
            Uri.parse(
                plan.destinationUri,
            )

        require(
            !isTreeNestedInside(
                parentTree = sourceUri,
                childTree = destinationUri,
            )
        ) {
            "Папка призначення не може бути всередині старої папки."
        }

        val source =
            DocumentFile.fromTreeUri(
                context,
                sourceUri,
            )
                ?: error(
                    "Стара папка недоступна."
                )
        val destination =
            DocumentFile.fromTreeUri(
                context,
                destinationUri,
            )
                ?: error(
                    "Папка призначення недоступна."
                )

        require(
            source.isDirectory,
        ) {
            "Стара папка більше не є папкою."
        }
        require(
            destination.isDirectory,
        ) {
            "Папка призначення більше не є папкою."
        }
        require(
            destination.canWrite(),
        ) {
            "Немає права запису в папку призначення."
        }

        progress(
            ConversionRunPhase.SCANNING,
            "Сканую стару папку…",
            0,
            0,
            0,
            0,
        )

        val scan =
            scanSource(
                treeUri =
                    sourceUri,
                root =
                    source,
            )

        checkCancelled()

        require(
            scan.files.isNotEmpty(),
        ) {
            "Стара папка порожня."
        }

        require(
            scan.files.none {
                it.relativePath.equals(
                    "renault-dataset.json",
                    ignoreCase = true,
                )
            },
        ) {
            "Ця папка вже схожа на готову копію Renault."
        }

        val exactFiles =
            scan.files
                .map {
                    it.relativePath
                }
                .toSet()

        // v0.5.107 (phone QA, Kangoo NT8486): stop before copying anything when
        // the chosen folder has no volume sub-folders. Previously this produced
        // «Готово · томів 0» and a copy the library could not add.
        val volumeRoots =
            AndroidDatasetPackageWriter
                .discoverVolumeRoots(
                    filePaths =
                        exactFiles,
                    directoryPaths =
                        scan.directories,
                )

        if (volumeRoots.isEmpty()) {
            error(
                if (AndroidDatasetPackageWriter.isVolumeFolderItself(exactFiles)) {
                    "Вибрана стара папка сама є томом: INDEX/ACCUEIL лежить прямо в ній. " +
                        "Вибери папку на рівень вище — ту, в якій лежить папка цього тому. " +
                        "Нічого не скопійовано, стару папку не змінено."
                } else {
                    "У старій папці не знайдено папок томів з INDEX/ACCUEIL. " +
                        "Вибери папку, в якій лежать папки томів. Нічого не скопійовано, стару папку не змінено."
                },
            )
        }

        val lowerMap =
            ConverterPathNormalizer
                .buildLowerMap(
                    exactFiles,
                )

        val existingOutput =
            destination.findFile(
                plan.outputFolderName,
            )

        if (existingOutput != null) {
            require(
                existingOutput.isDirectory,
            ) {
                "Наявна готова копія не є папкою: " +
                    plan.outputFolderName
            }

            return mergeIntoExistingOutput(
                plan =
                    plan,
                scan =
                    scan,
                existingOutput =
                    existingOutput,
                sourceExactFiles =
                    exactFiles,
            )
        }

        val stagingName =
            "." +
                plan.outputFolderName +
                ".renault-staging"

        require(
            destination.findFile(
                plan.outputFolderName,
            ) == null
        ) {
            "Готова копія вже існує: " +
                plan.outputFolderName
        }

        destination.findFile(
            stagingName,
        )?.let {
            stale ->
            require(
                stale.delete(),
            ) {
                "Не вдалося видалити стару тимчасову папку: " +
                    stagingName
            }
        }

        progress(
            ConversionRunPhase.PREPARING,
            "Створюю тимчасову папку…",
            scan.files.size,
            0,
            0,
            0,
        )

        val staging =
            destination.createDirectory(
                stagingName,
            )
                ?: error(
                    "Не вдалося створити тимчасову папку."
                )

        DatasetMediaIsolation.ensure(
            context =
                context,
            root =
                staging,
        )

        var changedFiles =
            0
        var changesTotal =
            0
        var finalized =
            false

        try {
            val directoryMap =
                createDirectories(
                    staging =
                        staging,
                    relativeDirectories =
                        scan.directories,
                )

            scan.files.forEachIndexed {
                    index,
                    sourceFile ->
                checkCancelled()

                val fileResult =
                    copyFile(
                        sourceFile =
                            sourceFile,
                        staging =
                            staging,
                        directoryMap =
                            directoryMap,
                        exactFiles =
                            exactFiles,
                        lowerMap =
                            lowerMap,
                    )

                if (fileResult > 0) {
                    changedFiles += 1
                    changesTotal +=
                        fileResult
                }

                progress(
                    ConversionRunPhase.COPYING,
                    "Копіюю: " +
                        sourceFile.relativePath,
                    scan.files.size,
                    index + 1,
                    changedFiles,
                    changesTotal,
                )
            }

            checkCancelled()

            writeConversionReport(
                outputRoot =
                    staging,
                plan =
                    plan,
                filesTotal =
                    scan.files.size,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
            )

            progress(
                ConversionRunPhase.PACKAGING,
                "Створюю базовий пакет готової копії…",
                scan.files.size,
                scan.files.size,
                changedFiles,
                changesTotal,
            )

            val packageResult =
                AndroidDatasetPackageWriter.write(
                    context = context,
                    outputRoot =
                        staging,
                    sourceName =
                        plan.sourceName,
                    sourceFilePaths =
                        exactFiles,
                    sourceDirectoryPaths =
                        scan.directories,
                    filesTotal =
                        scan.files.size,
                    changedFiles =
                        changedFiles,
                    changesTotal =
                        changesTotal,
                )

            checkCancelled()

            progress(
                ConversionRunPhase.VALIDATING,
                "Перевіряю готову копію…",
                scan.files.size,
                scan.files.size,
                changedFiles,
                changesTotal,
            )

            validateOutput(
                outputRoot =
                    staging,
                expectedSourceFiles =
                    scan.files.size,
                expectedEntrypoint =
                    packageResult.entrypoint,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
            )

            checkCancelled()

            progress(
                ConversionRunPhase.FINALIZING,
                "Фіксую готову копію…",
                scan.files.size,
                scan.files.size,
                changedFiles,
                changesTotal,
            )

            require(
                staging.renameTo(
                    plan.outputFolderName,
                )
            ) {
                "Не вдалося перетворити тимчасову папку на готову копію."
            }

            finalized =
                true

            val outputTreeUri =
                treeUriForDocument(
                    staging.uri,
                ).toString()

            return SafConversionResult(
                outputTreeUri =
                    outputTreeUri,
                filesTotal =
                    scan.files.size,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
                volumeCount =
                    packageResult.volumeCount,
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            if (!finalized) {
                staging.delete()
            }
            throw cancelled
        } catch (
            error:
                Throwable,
        ) {
            if (!finalized) {
                staging.delete()
            }
            throw error
        }
    }

    private fun scanSource(
        treeUri: Uri,
        root: DocumentFile,
        label: String = "стара папка",
    ): SourceScan {
        return try {
            scanSourceFast(
                treeUri,
                label,
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
            progress(
                ConversionRunPhase.SCANNING,
                "Швидке сканування недоступне · сканую повільнішим способом…",
                0,
                0,
                0,
                0,
            )

            scanSourceFallback(
                root,
                label,
            )
        }
    }

    private fun scanSourceFast(
        treeUri: Uri,
        label: String,
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
                                documentId =
                                    childId,
                                prefix =
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
                                sizeIndex >= 0 &&
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
                            progressDue()
                        ) {
                            progress(
                                ConversionRunPhase.SCANNING,
                                "Сканую (" +
                                    label +
                                    ") · файлів: " +
                                    files.size +
                                    " · папок: " +
                                    directories.size,
                                files.size,
                                0,
                                0,
                                0,
                            )
                        }
                    }
                }
                ?: error(
                    "Android не повернув список файлів папки."
                )
        }

        visit(
            documentId =
                rootDocumentId,
            prefix =
                "",
        )

        progress(
            ConversionRunPhase.SCANNING,
            "Сканування (" +
                label +
                ") завершено · файлів: " +
                files.size +
                " · папок: " +
                directories.size,
            files.size,
            0,
            0,
            0,
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
        label: String,
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
                                progressDue()
                            ) {
                                progress(
                                    ConversionRunPhase.SCANNING,
                                    "Повільне сканування (" +
                                        label +
                                        ") · файлів: " +
                                        files.size +
                                        " · папок: " +
                                        directories.size,
                                    files.size,
                                    0,
                                    0,
                                    0,
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

    private fun mergeIntoExistingOutput(
        plan: ConversionPlan,
        scan: SourceScan,
        existingOutput: DocumentFile,
        sourceExactFiles: Set<String>,
    ): SafConversionResult {
        val outputTreeUri =
            treeUriForDocument(
                existingOutput.uri,
            )

        val existingRecord =
            DatasetReader.read(
                context =
                    context,
                treeUri =
                    outputTreeUri,
            ).getOrThrow()

        val expectedDatasetId =
            AndroidDatasetPackageWriter
                .datasetIdFor(
                    plan.sourceName,
                )

        require(
            existingRecord.id ==
                expectedDatasetId
        ) {
            "Папка " +
                plan.outputFolderName +
                " уже належить іншій готовій копії: " +
                existingRecord.title
        }

        existingOutput
            .listFiles()
            .filter {
                it.name
                    ?.startsWith(
                        ".renault-merge-",
                    ) ==
                    true
            }
            .forEach {
                staleMerge ->
                staleMerge.delete()
            }

        progress(
            ConversionRunPhase.PREPARING,
            "Наявну готову копію знайдено · сканую її томи…",
            scan.files.size,
            0,
            0,
            0,
        )

        val existingScan =
            scanSource(
                treeUri =
                    outputTreeUri,
                root =
                    existingOutput,
                label =
                    "наявна готова копія",
            )

        val existingFiles =
            existingScan.files
                .filterNot {
                    isGeneratedDatasetPath(
                        it.relativePath,
                    )
                }

        val existingDirectories =
            existingScan.directories
                .filterNot {
                    isGeneratedDatasetPath(
                        it,
                    )
                }
                .toSet()

        val volumeRoots =
            AndroidDatasetPackageWriter
                .discoverVolumeRoots(
                    filePaths =
                        sourceExactFiles,
                    directoryPaths =
                        scan.directories,
                )

        require(
            volumeRoots.isNotEmpty(),
        ) {
            "У старій папці не знайдено окремих томів з INDEX/ACCUEIL для додавання."
        }

        val sourceVolumeFiles =
            scan.files.filter {
                sourceFile ->
                volumeRoots.any {
                    root ->
                    sourceFile.relativePath
                        .startsWith(
                            root + "/",
                        )
                }
            }

        val sourceVolumeDirectories =
            scan.directories.filter {
                relative ->
                volumeRoots.any {
                    root ->
                    relative ==
                        root ||
                        relative.startsWith(
                            root + "/",
                        )
                }
            }.toSet()

        val exactFilesForPatching =
            (
                existingFiles
                    .map {
                        it.relativePath
                    } +
                    sourceExactFiles
            ).toSet()

        val lowerMapForPatching =
            ConverterPathNormalizer
                .buildLowerMap(
                    exactFilesForPatching,
                )

        var changedFiles =
            0
        var changesTotal =
            0
        var filesDone =
            0

        volumeRoots.forEach {
                root ->
            checkCancelled()

            val existingVolume =
                existingOutput.findFile(
                    root,
                )

            if (
                existingVolume != null
            ) {
                require(
                    existingVolume.isDirectory,
                ) {
                    "Конфлікт імені тому: " +
                        root
                }

                filesDone +=
                    sourceVolumeFiles
                        .count {
                            it.relativePath
                                .startsWith(
                                    root + "/",
                                )
                        }

                progress(
                    ConversionRunPhase.COPYING,
                    "Том уже є · перевіряю пакет: " +
                        root,
                    sourceVolumeFiles.size,
                    filesDone,
                    changedFiles,
                    changesTotal,
                )

                return@forEach
            }

            val tempName =
                ".renault-merge-" +
                    AndroidDatasetPackageWriter
                        .datasetIdFor(
                            root,
                        )

            existingOutput
                .findFile(
                    tempName,
                )
                ?.let {
                    staleTemp ->
                    require(
                        staleTemp.delete(),
                    ) {
                        "Не вдалося очистити стару тимчасову папку додавання: " +
                            tempName
                    }
                }

            val tempRoot =
                existingOutput
                    .createDirectory(
                        tempName,
                    )
                    ?: error(
                        "Не вдалося створити тимчасову папку додавання для тому: " +
                            root
                    )

            try {
                val relativeDirectories =
                    sourceVolumeDirectories
                        .asSequence()
                        .filter {
                            it.startsWith(
                                root + "/",
                            )
                        }
                        .map {
                            it.removePrefix(
                                root + "/",
                            )
                        }
                        .filter {
                            it.isNotBlank()
                        }
                        .toSet()

                val directoryMap =
                    createDirectories(
                        staging =
                            tempRoot,
                        relativeDirectories =
                            relativeDirectories,
                    )

                val filesForVolume =
                    sourceVolumeFiles
                        .filter {
                            it.relativePath
                                .startsWith(
                                    root + "/",
                                )
                        }

                filesForVolume.forEach {
                        sourceFile ->
                    checkCancelled()

                    val targetRelativePath =
                        sourceFile.relativePath
                            .removePrefix(
                                root + "/",
                            )

                    val fileResult =
                        copyFileToRelativePath(
                            sourceFile =
                                sourceFile,
                            targetRoot =
                                tempRoot,
                            targetRelativePath =
                                targetRelativePath,
                            directoryMap =
                                directoryMap,
                            exactFiles =
                                exactFilesForPatching,
                            lowerMap =
                                lowerMapForPatching,
                        )

                    if (
                        fileResult > 0
                    ) {
                        changedFiles +=
                            1
                        changesTotal +=
                            fileResult
                    }

                    filesDone +=
                        1

                    progress(
                        ConversionRunPhase.COPYING,
                        "Додаю том: " +
                            root,
                        sourceVolumeFiles.size,
                        filesDone,
                        changedFiles,
                        changesTotal,
                    )
                }

                require(
                    tempRoot.renameTo(
                        root,
                    )
                ) {
                    "Не вдалося зафіксувати доданий том: " +
                        root
                }
            } catch (
                error:
                    Throwable,
            ) {
                tempRoot.delete()
                throw error
            }
        }

        checkCancelled()

        DatasetMediaIsolation.ensure(
            context =
                context,
            root =
                existingOutput,
        )

        val combinedFilePaths =
            (
                existingFiles
                    .map {
                        it.relativePath
                    } +
                    sourceVolumeFiles
                        .map {
                            it.relativePath
                        }
            ).toSet()

        val combinedDirectoryPaths =
            (
                existingDirectories +
                    sourceVolumeDirectories
            ).toSet()

        writeConversionReport(
            outputRoot =
                existingOutput,
            plan =
                plan,
            filesTotal =
                combinedFilePaths.size,
            changedFiles =
                changedFiles,
            changesTotal =
                changesTotal,
        )

        progress(
            ConversionRunPhase.PACKAGING,
            "Оновлюю пакет для всіх томів…",
            combinedFilePaths.size,
            combinedFilePaths.size,
            changedFiles,
            changesTotal,
        )

        val packageResult =
            AndroidDatasetPackageWriter.write(
                context =
                    context,
                outputRoot =
                    existingOutput,
                sourceName =
                    plan.sourceName,
                sourceFilePaths =
                    combinedFilePaths,
                sourceDirectoryPaths =
                    combinedDirectoryPaths,
                filesTotal =
                    combinedFilePaths.size,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
            )

        checkCancelled()

        validateOutput(
            outputRoot =
                existingOutput,
            expectedSourceFiles =
                combinedFilePaths.size,
            expectedEntrypoint =
                packageResult.entrypoint,
            changedFiles =
                changedFiles,
            changesTotal =
                changesTotal,
        )

        progress(
            ConversionRunPhase.FINALIZING,
            "Готово · томів: " +
                packageResult.volumeCount,
            combinedFilePaths.size,
            combinedFilePaths.size,
            changedFiles,
            changesTotal,
        )

        return SafConversionResult(
            outputTreeUri =
                outputTreeUri
                    .toString(),
            filesTotal =
                combinedFilePaths.size,
            changedFiles =
                changedFiles,
            changesTotal =
                changesTotal,
            volumeCount =
                packageResult.volumeCount,
        )
    }

    private fun isGeneratedDatasetPath(
        relativePath: String,
    ): Boolean {
        val normalized =
            relativePath
                .replace(
                    '\\',
                    '/',
                )

        if (
            normalized ==
                "renault-dataset.json" ||
            normalized ==
                "conversion-report.json" ||
            normalized ==
                ".nomedia"
        ) {
            return true
        }

        return normalized ==
            "_renault" ||
            normalized.startsWith(
                "_renault/",
            ) ||
            normalized.startsWith(
                ".renault-merge-",
            )
    }

    private fun copyFileToRelativePath(
        sourceFile: SourceFile,
        targetRoot: DocumentFile,
        targetRelativePath: String,
        directoryMap: Map<String, DocumentFile>,
        exactFiles: Set<String>,
        lowerMap: Map<String, List<String>>,
    ): Int {
        val parentPath =
            targetRelativePath
                .substringBeforeLast(
                    '/',
                    "",
                )
        val name =
            targetRelativePath
                .substringAfterLast(
                    '/',
                )
        val parent =
            directoryMap[parentPath]
                ?: targetRoot

        parent.findFile(
            name,
        )?.delete()

        val target =
            parent.createFile(
                mimeFor(
                    sourceFile,
                ),
                name,
            )
                ?: error(
                    "Не вдалося створити: " +
                        targetRelativePath
                )

        val extension =
            name
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                )

        if (
            extension in
                setOf(
                    "htm",
                    "html",
                    "js",
                )
        ) {
            val raw =
                context.contentResolver
                    .openInputStream(
                        sourceFile.uri,
                    )
                    ?.use {
                        it.readBytes()
                    }
                    ?: error(
                        "Не вдалося прочитати: " +
                            sourceFile.relativePath
                    )

            val text =
                raw.toString(
                    Charsets.ISO_8859_1,
                )

            val patched =
                ConverterPathNormalizer
                    .patchText(
                        sourceRelativePath =
                            sourceFile.relativePath,
                        input =
                            text,
                        exactFiles =
                            exactFiles,
                        lowerMap =
                            lowerMap,
                    )

            context.contentResolver
                .openOutputStream(
                    target.uri,
                    "wt",
                )
                ?.use {
                    it.write(
                        patched.text
                            .toByteArray(
                                Charsets.ISO_8859_1,
                            )
                    )
                }
                ?: error(
                    "Не вдалося записати: " +
                        sourceFile.relativePath
                )

            return patched
                .changes
                .size
        }

        val input =
            context.contentResolver
                .openInputStream(
                    sourceFile.uri,
                )
                ?: error(
                    "Не вдалося прочитати: " +
                        sourceFile.relativePath
                )

        val output =
            context.contentResolver
                .openOutputStream(
                    target.uri,
                    "wt",
                )
                ?: error(
                    "Не вдалося записати: " +
                        targetRelativePath
                )

        BufferedInputStream(
            input,
            BUFFER_SIZE,
        ).use {
                sourceStream ->
            BufferedOutputStream(
                output,
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
                        read < 0
                    ) {
                        break
                    }

                    targetStream.write(
                        buffer,
                        0,
                        read,
                    )
                }
            }
        }

        return 0
    }

    private fun createDirectories(
        staging: DocumentFile,
        relativeDirectories: Set<String>,
    ): MutableMap<String, DocumentFile> {
        val result =
            mutableMapOf(
                "" to staging,
            )

        relativeDirectories
            .sortedWith(
                compareBy<String> {
                    it.count {
                            char ->
                        char == '/'
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

                val parentPath =
                    relative
                        .substringBeforeLast(
                            '/',
                            "",
                        )
                val name =
                    relative
                        .substringAfterLast(
                            '/',
                        )
                val parent =
                    result[parentPath]
                        ?: error(
                            "Не знайдено папку для тимчасових файлів: " +
                                parentPath
                        )

                val created =
                    parent.createDirectory(
                        name,
                    )
                        ?: error(
                            "Не вдалося створити папку: " +
                                relative
                        )

                result[relative] =
                    created
            }

        return result
    }

    private fun copyFile(
        sourceFile: SourceFile,
        staging: DocumentFile,
        directoryMap: Map<String, DocumentFile>,
        exactFiles: Set<String>,
        lowerMap: Map<String, List<String>>,
    ): Int {
        val parentPath =
            sourceFile.relativePath
                .substringBeforeLast(
                    '/',
                    "",
                )
        val name =
            sourceFile.relativePath
                .substringAfterLast(
                    '/',
                )
        val parent =
            directoryMap[parentPath]
                ?: staging

        val target =
            parent.createFile(
                mimeFor(
                    sourceFile,
                ),
                name,
            )
                ?: error(
                    "Не вдалося створити: " +
                        sourceFile.relativePath
                )

        val extension =
            name
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                )

        if (
            extension in
            setOf(
                "htm",
                "html",
                "js",
            )
        ) {
            val raw =
                context.contentResolver
                    .openInputStream(
                        sourceFile.uri,
                    )
                    ?.use {
                        it.readBytes()
                    }
                    ?: error(
                        "Не вдалося прочитати: " +
                            sourceFile.relativePath
                    )

            val text =
                raw.toString(
                    Charsets.ISO_8859_1,
                )

            val patched =
                ConverterPathNormalizer
                    .patchText(
                        sourceRelativePath =
                            sourceFile.relativePath,
                        input =
                            text,
                        exactFiles =
                            exactFiles,
                        lowerMap =
                            lowerMap,
                    )

            context.contentResolver
                .openOutputStream(
                    target.uri,
                    "wt",
                )
                ?.use {
                    it.write(
                        patched.text
                            .toByteArray(
                                Charsets.ISO_8859_1,
                            )
                    )
                }
                ?: error(
                    "Не вдалося записати: " +
                        sourceFile.relativePath
                )

            return patched
                .changes
                .size
        }

        val input =
            context.contentResolver
                .openInputStream(
                    sourceFile.uri,
                )
                ?: error(
                    "Не вдалося прочитати: " +
                        sourceFile.relativePath
                )

        val output =
            context.contentResolver
                .openOutputStream(
                    target.uri,
                    "wt",
                )
                ?: error(
                    "Не вдалося записати: " +
                        sourceFile.relativePath
                )

        BufferedInputStream(
            input,
            BUFFER_SIZE,
        ).use {
                sourceStream ->
            BufferedOutputStream(
                output,
                BUFFER_SIZE,
            ).use {
                    targetStream ->
                val buffer =
                    ByteArray(
                        BUFFER_SIZE,
                    )

                while (true) {
                    checkCancelled()

                    val read =
                        sourceStream.read(
                            buffer,
                        )

                    if (read < 0) {
                        break
                    }

                    targetStream.write(
                        buffer,
                        0,
                        read,
                    )
                }
            }
        }

        return 0
    }

    private fun validateOutput(
        outputRoot: DocumentFile,
        expectedSourceFiles: Int,
        expectedEntrypoint: String,
        changedFiles: Int,
        changesTotal: Int,
    ) {
        val manifest =
            outputRoot.findFile(
                "renault-dataset.json",
            )
                ?: error(
                    "Перевірка: немає файлу renault-dataset.json."
                )

        require(
            manifest.isFile,
        ) {
            "Перевірка: опис набору (renault-dataset.json) не є файлом."
        }

        require(
            findRelative(
                outputRoot,
                expectedEntrypoint,
            )?.isFile ==
                true
        ) {
            "Перевірка: немає стартового файлу: " +
                expectedEntrypoint
        }

        progress(
            ConversionRunPhase.VALIDATING,
            "Перевіряю готову копію…",
            expectedSourceFiles,
            0,
            changedFiles,
            changesTotal,
        )

        val copiedSourceFiles =
            countSourceFiles(
                root =
                    outputRoot,
                expectedSourceFiles =
                    expectedSourceFiles,
                changedFiles =
                    changedFiles,
                changesTotal =
                    changesTotal,
            )

        require(
            copiedSourceFiles >=
                expectedSourceFiles
        ) {
            "Перевірка: зі старої папки скопійовано не всі файли: " +
                copiedSourceFiles +
                "/" +
                expectedSourceFiles
        }
    }

    private fun countSourceFiles(
        root: DocumentFile,
        expectedSourceFiles: Int,
        changedFiles: Int,
        changesTotal: Int,
    ): Int {
        var count =
            0

        fun visit(
            directory: DocumentFile,
        ) {
            checkCancelled()

            directory
                .listFiles()
                .forEach {
                    child ->
                    if (
                        child.name ==
                            "_renault" ||
                        child.name
                            ?.startsWith(
                                ".renault-merge-",
                            ) ==
                            true
                    ) {
                        return@forEach
                    }

                    if (
                        child.isDirectory
                    ) {
                        visit(
                            child,
                        )
                    } else if (
                        child.isFile &&
                        !child.name.equals(
                            "renault-dataset.json",
                            ignoreCase = true,
                        ) &&
                        !child.name.equals(
                            "conversion-report.json",
                            ignoreCase = true,
                        ) &&
                        !child.name.equals(
                            ".nomedia",
                            ignoreCase = true,
                        )
                    ) {
                        count +=
                            1

                        if (
                            progressDue() ||
                            count ==
                                expectedSourceFiles
                        ) {
                            progress(
                                ConversionRunPhase.VALIDATING,
                                "Перевіряю готову копію…",
                                expectedSourceFiles,
                                count.coerceAtMost(
                                    expectedSourceFiles,
                                ),
                                changedFiles,
                                changesTotal,
                            )
                        }
                    }
                }
        }

        visit(
            root,
        )

        return count
    }

    private fun writeConversionReport(
        outputRoot: DocumentFile,
        plan: ConversionPlan,
        filesTotal: Int,
        changedFiles: Int,
        changesTotal: Int,
    ) {
        val report =
            JSONObject()
                .put(
                    "source_uri",
                    plan.sourceUri,
                )
                .put(
                    "destination_uri",
                    plan.destinationUri,
                )
                .put(
                    "output_folder",
                    plan.outputFolderName,
                )
                .put(
                    "files_total",
                    filesTotal,
                )
                .put(
                    "changed_files",
                    changedFiles,
                )
                .put(
                    "changes_total",
                    changesTotal,
                )
                .put(
                    "changes_by_kind",
                    JSONArray(),
                )

        outputRoot
            .findFile(
                "conversion-report.json",
            )
            ?.delete()

        val file =
            outputRoot.createFile(
                "application/json",
                "conversion-report.json",
            )
                ?: error(
                    "Не вдалося створити звіт conversion-report.json."
                )

        context.contentResolver
            .openOutputStream(
                file.uri,
                "wt",
            )
            ?.bufferedWriter(
                Charsets.UTF_8,
            )
            ?.use {
                it.write(
                    report.toString(
                        2,
                    ) +
                        "\n",
                )
            }
            ?: error(
                "Не вдалося записати звіт conversion-report.json."
            )
    }

    private fun findRelative(
        root: DocumentFile,
        relativePath: String,
    ): DocumentFile? {
        var current =
            root

        relativePath
            .replace(
                '\\',
                '/',
            )
            .split(
                '/',
            )
            .filter {
                it.isNotBlank() &&
                    it != "."
            }
            .forEach {
                part ->
                if (part == "..") {
                    return null
                }

                current =
                    current.findFile(
                        part,
                    )
                        ?: return null
            }

        return current
    }

    private fun mimeFor(
        sourceFile: SourceFile,
    ): String {
        sourceFile.mimeType
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                return it
            }

        val extension =
            sourceFile.relativePath
                .substringAfterLast(
                    '.',
                    "",
                )
                .lowercase(
                    Locale.ROOT,
                )

        return MimeTypeMap
            .getSingleton()
            .getMimeTypeFromExtension(
                extension,
            )
            ?: "application/octet-stream"
    }

    private fun progress(
        phase: ConversionRunPhase,
        message: String,
        filesTotal: Int,
        filesDone: Int,
        changedFiles: Int,
        changesTotal: Int,
    ) {
        onProgress(
            phase,
            message,
            filesTotal,
            filesDone,
            changedFiles,
            changesTotal,
        )
    }

    /**
     * Live counter: scan/validation report by time, not every N files, so the
     * owner sees the bar move continuously (phone QA v0.5.102). The service
     * still throttles persisted writes; this only avoids building a message per file.
     */
    private var lastProgressAtMs = 0L

    private fun progressDue(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastProgressAtMs < PROGRESS_INTERVAL_MS) return false
        lastProgressAtMs = now
        return true
    }

    private fun checkCancelled() {
        if (
            runStore
                .isCancelRequested()
        ) {
            throw ConversionCancelledException()
        }
    }

    private fun treeUriForDocument(
        documentUri: Uri,
    ): Uri {
        val authority =
            documentUri.authority
                ?: error(
                    "Адреса готової копії неповна (немає постачальника файлів)."
                )

        val documentId =
            DocumentsContract
                .getDocumentId(
                    documentUri,
                )

        return DocumentsContract
            .buildTreeDocumentUri(
                authority,
                documentId,
            )
    }

    private fun isTreeNestedInside(
        parentTree: Uri,
        childTree: Uri,
    ): Boolean {
        if (
            parentTree.authority !=
            childTree.authority
        ) {
            return false
        }

        val parentId =
            runCatching {
                DocumentsContract
                    .getTreeDocumentId(
                        parentTree,
                    )
            }.getOrNull()
                ?: return false

        val childId =
            runCatching {
                DocumentsContract
                    .getTreeDocumentId(
                        childTree,
                    )
            }.getOrNull()
                ?: return false

        if (
            parentId ==
            childId
        ) {
            return true
        }

        val normalizedParent =
            parentId.trimEnd(
                '/',
            )

        return childId.startsWith(
            normalizedParent +
                "/",
        )
    }

    companion object {
        private const val BUFFER_SIZE =
            128 * 1024

        private const val PROGRESS_INTERVAL_MS =
            80L
    }
}
