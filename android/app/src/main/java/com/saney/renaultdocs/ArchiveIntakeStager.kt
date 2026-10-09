package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File

/**
 * Android/SAF boundary for archive intake.
 *
 * The user-selected archive is copied read-only into app-private staging.
 * ArchiveIntake then extracts only inside that staging area.
 */
class ArchiveIntakeStager(
    context: Context,
    private val onMessage: (String) -> Unit = {},
    private val onProgress: (OperationProgress) -> Unit = {},
    private val isCancelled: () -> Boolean = { false },
) {
    data class Result(
        val workRoot: File,
        val sourceCopy: File,
        val extractionRoot: File,
        val format: ArchiveIntake.Format,
        val rawRoots: List<File>,
        val files: Int,
        val directories: Int,
        val extractedBytes: Long,
    ) {
        fun cleanup() {
            workRoot.deleteRecursively()
        }
    }

    data class StagedSource(
        val workRoot: File,
        val sourceCopy: File,
        val sourceName: String,
    ) {
        fun cleanup() {
            workRoot.deleteRecursively()
        }
    }

    private val appContext =
        context.applicationContext

    fun stageSource(
        sourceUri: Uri,
        sourceName: String,
        stagingToken: String,
    ): StagedSource {
        checkCancelled()

        val base =
            File(
                appContext.noBackupFilesDir,
                STAGING_DIR,
            )

        require(
            base.isDirectory ||
                base.mkdirs(),
        ) {
            "Не вдалося створити archive intake staging."
        }

        val workRoot =
            File(
                base,
                NativePreparationStager.safeToken(
                    stagingToken,
                ) +
                    STAGING_SUFFIX,
            )

        if (
            workRoot.exists()
        ) {
            require(
                workRoot.deleteRecursively(),
            ) {
                "Не вдалося очистити старий archive intake staging."
            }
        }

        require(
            workRoot.mkdirs(),
        ) {
            "Не вдалося створити archive intake staging."
        }

        try {
            val extension =
                sourceName
                    .substringAfterLast(
                        '.',
                        "",
                    )
                    .lowercase()
                    .takeIf {
                        it in
                            setOf(
                                "zip",
                                "7z",
                                "rar",
                            )
                    }
                    ?.let {
                        "." +
                            it
                    }
                    .orEmpty()

            val sourceCopy =
                File(
                    workRoot,
                    "source" +
                        extension,
                )

            val totalBytes =
                querySize(
                    sourceUri,
                )
                    .takeIf {
                        it >
                            0L
                    }

            totalBytes
                ?.let {
                    bytes ->
                    require(
                        workRoot.usableSpace -
                            bytes >=
                            MIN_FREE_SPACE_BYTES
                    ) {
                        "Недостатньо вільного місця, щоб підготувати архів."
                    }
                }

            onMessage(
                "Копіюю архів у private staging…",
            )
            onProgress(
                totalBytes
                    ?.let {
                        OperationProgress.measured(
                            stage =
                                "Копіюю архів…",
                            current =
                                0,
                            total =
                                PROGRESS_SCALE,
                        )
                    }
                    ?: OperationProgress.indeterminate(
                        "Копіюю архів…",
                    ),
            )

            copySource(
                sourceUri =
                    sourceUri,
                destination =
                    sourceCopy,
                totalBytes =
                    totalBytes,
            )

            checkCancelled()

            return StagedSource(
                workRoot =
                    workRoot,
                sourceCopy =
                    sourceCopy,
                sourceName =
                    sourceName,
            )
        } catch (
            error:
                Throwable,
        ) {
            workRoot.deleteRecursively()
            throw error
        }
    }

    fun extract(
        staged: StagedSource,
    ): Result {
        checkCancelled()

        try {
            onMessage(
                "Перевіряю та розпаковую архів…",
            )
            onProgress(
                OperationProgress.indeterminate(
                    "Розпаковую…",
                ),
            )

            val extractionRoot =
                File(
                    staged.workRoot,
                    "extracted",
                )

            val extracted =
                ArchiveIntake.extract(
                    source =
                        staged.sourceCopy,
                    extractionRoot =
                        extractionRoot,
                    onProgress = {
                        progress ->
                        val state =
                            if (
                                progress.entriesTotal !=
                                null &&
                                progress.entriesTotal >
                                    0
                            ) {
                                OperationProgress.measured(
                                    stage =
                                        progress.stage,
                                    current =
                                        progress.entriesDone,
                                    total =
                                        progress.entriesTotal,
                                    itemCurrent =
                                        progress.entriesDone,
                                    itemTotal =
                                        progress.entriesTotal,
                                    itemLabel =
                                        "Файлів",
                                )
                            } else {
                                OperationProgress(
                                    stage =
                                        progress.stage,
                                    itemCurrent =
                                        progress.entriesDone,
                                    itemLabel =
                                        "Файлів",
                                )
                            }

                        onProgress(
                            state,
                        )
                    },
                    isCancelled =
                        isCancelled,
                )

            checkCancelled()

            return Result(
                workRoot =
                    staged.workRoot,
                sourceCopy =
                    staged.sourceCopy,
                extractionRoot =
                    extractionRoot,
                format =
                    extracted.format,
                rawRoots =
                    extracted.rawRoots,
                files =
                    extracted.files,
                directories =
                    extracted.directories,
                extractedBytes =
                    extracted.extractedBytes,
            )
        } catch (
            error:
                Throwable,
        ) {
            staged.cleanup()
            throw error
        }
    }

    fun prepare(
        sourceUri: Uri,
        sourceName: String,
        stagingToken: String,
    ): Result =
        extract(
            stageSource(
                sourceUri =
                    sourceUri,
                sourceName =
                    sourceName,
                stagingToken =
                    stagingToken,
            ),
        )

    private fun copySource(
        sourceUri: Uri,
        destination: File,
        totalBytes: Long?,
    ) {
        val input =
            appContext
                .contentResolver
                .openInputStream(
                    sourceUri,
                )
                ?: error(
                    "Android не зміг відкрити архів для читання."
                )

        var copied =
            0L
        var lastPercent =
            -1

        BufferedInputStream(
            input,
            BUFFER_SIZE,
        ).use {
            source ->
            BufferedOutputStream(
                destination.outputStream(),
                BUFFER_SIZE,
            ).use {
                target ->
                val buffer =
                    ByteArray(
                        BUFFER_SIZE,
                    )

                while (
                    true
                ) {
                    checkCancelled()

                    val read =
                        source.read(
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

                    target.write(
                        buffer,
                        0,
                        read,
                    )

                    copied +=
                        read

                    totalBytes
                        ?.takeIf {
                            it >
                                0L
                        }
                        ?.let {
                            total ->
                            val percent =
                                (
                                    copied *
                                        100L /
                                        total
                                )
                                    .toInt()
                                    .coerceIn(
                                        0,
                                        100,
                                    )

                            if (
                                percent !=
                                lastPercent
                            ) {
                                onProgress(
                                    OperationProgress.measured(
                                        stage =
                                            "Копіюю архів…",
                                        current =
                                            percent,
                                        total =
                                            100,
                                    ),
                                )
                                lastPercent =
                                    percent
                            }
                        }
                }
            }
        }
    }

    private fun querySize(
        uri: Uri,
    ): Long =
        runCatching {
            appContext
                .contentResolver
                .query(
                    uri,
                    arrayOf(
                        OpenableColumns.SIZE,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use {
                    cursor ->
                    if (
                        !cursor.moveToFirst()
                    ) {
                        return@use -1L
                    }

                    val index =
                        cursor.getColumnIndex(
                            OpenableColumns.SIZE,
                        )

                    if (
                        index <
                        0 ||
                        cursor.isNull(
                            index,
                        )
                    ) {
                        -1L
                    } else {
                        cursor.getLong(
                            index,
                        )
                    }
                }
                ?: -1L
        }.getOrDefault(
            -1L,
        )

    private fun checkCancelled() {
        if (
            isCancelled()
        ) {
            throw ConversionCancelledException()
        }
    }

    companion object {
        private const val STAGING_DIR =
            "archive-intake"

        private const val STAGING_SUFFIX =
            ".staging"

        private const val BUFFER_SIZE =
            1024 *
                1024

        private const val PROGRESS_SCALE =
            100

        private const val MIN_FREE_SPACE_BYTES =
            128L *
                1024L *
                1024L
    }
}
