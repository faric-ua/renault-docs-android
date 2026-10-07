package com.saney.renaultdocs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class NativeRdpkgPreparationService : Service() {
    data class StartRequest(
        val requestId: String,
        val sourceTreeUri: String,
        val sourceName: String,
        val sourceKind: NativeRdpkgSourceKind = NativeRdpkgSourceKind.RAW_TREE,
        val destinationUri: String,
        val projectId: String,
        val projectTitle: String,
        val model: String,
    )

    private lateinit var runStore:
        NativeRdpkgRunStore

    private val workerRunning =
        AtomicBoolean(
            false,
        )

    private var wakeLock:
        PowerManager.WakeLock? =
        null

    private var lastNotificationAt =
        0L

    override fun onCreate() {
        super.onCreate()

        runStore =
            NativeRdpkgRunStore(
                this,
            )

        ensureNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (
            intent?.action
        ) {
            ACTION_CANCEL -> {
                val state =
                    runStore.load()

                if (
                    state.isWaitingForSelection
                ) {
                    cleanupPersistedArchiveWorkspace(
                        state.archiveExtractionRoot,
                    )
                    runStore.clearArchiveSelectionData()
                    runStore.markCancelled(
                        "Вибір томів скасовано. Оригінальний архів не змінено, private staging очищено.",
                    )

                    notifyFinal(
                        title =
                            "Archive intake скасовано",
                        text =
                            "Оригінальний архів не змінено.",
                        projectId =
                            state.projectId
                                ?: "",
                    )

                    stopSelf()
                } else if (
                    state.phase ==
                    NativeRdpkgRunPhase.PREPARING
                ) {
                    runStore.requestCancel()

                    updateNotification(
                        title =
                            "Renault Docs · скасування",
                        text =
                            "Зупиняю підготовку після поточної операції…",
                        projectId =
                            state.projectId,
                        cancellable =
                            false,
                    )
                } else if (
                    !state.isRunning
                ) {
                    stopSelf()
                }

                return START_NOT_STICKY
            }

            ACTION_RESUME_ARCHIVE ->
                return startArchiveResumeCommand()

            ACTION_START -> Unit

            else ->
                return START_NOT_STICKY
        }

        if (
            !workerRunning.compareAndSet(
                false,
                true,
            )
        ) {
            return START_REDELIVER_INTENT
        }

        val request =
            parseRequest(
                intent,
            )
                ?: return stopForMissingRequest()

        val persistedState =
            runStore.load()

        val resumingAfterProcessRestart =
            persistedState.isRunning &&
                persistedState.projectId ==
                    request.projectId &&
                persistedState.sourceUri ==
                    request.sourceTreeUri &&
                persistedState.sourceKind ==
                    request.sourceKind &&
                persistedState.destinationUri ==
                    request.destinationUri

        if (
            persistedState.isRunning &&
            !resumingAfterProcessRestart
        ) {
            startInFlight.set(
                false,
            )
            workerRunning.set(
                false,
            )
            return START_NOT_STICKY
        }

        if (
            resumingAfterProcessRestart
        ) {
            runStore.resumeAfterProcessRestart()
        } else {
            runStore.begin(
                projectId =
                    request.projectId,
                sourceUri =
                    request.sourceTreeUri,
                sourceName =
                    request.sourceName,
                sourceKind =
                    request.sourceKind,
                destinationUri =
                    request.destinationUri,
            )
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title =
                    "Renault Docs · створення .rdpkg",
                text =
                    if (
                        request.sourceKind ==
                        NativeRdpkgSourceKind.ARCHIVE_FILE
                    ) {
                        "Починаю підготовку архіву…"
                    } else {
                        "Починаю Kotlin-native підготовку…"
                    },
                projectId =
                    request.projectId,
                cancellable =
                    true,
            ),
        )

        active =
            true

        acquireWakeLock()

        Thread {
            runPreparation(
                request,
            )
        }.start()

        startInFlight.set(
            false,
        )

        return START_REDELIVER_INTENT
    }

    private fun startArchiveResumeCommand():
        Int {
        if (
            !workerRunning.compareAndSet(
                false,
                true,
            )
        ) {
            return START_REDELIVER_INTENT
        }

        val state =
            runStore.load()

        if (
            state.sourceKind !=
                NativeRdpkgSourceKind.ARCHIVE_FILE ||
            !state.isRunning ||
            state.archiveExtractionRoot
                .isNullOrBlank() ||
            state.archiveCandidates
                .isEmpty()
        ) {
            workerRunning.set(
                false,
            )
            runStore.fail(
                "Не вдалося відновити вибрані томи архіву."
            )
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title =
                    "Renault Docs · archive batch",
                text =
                    "Продовжую вибрані томи…",
                projectId =
                    state.projectId,
                cancellable =
                    true,
            ),
        )

        active =
            true

        acquireWakeLock()

        Thread {
            runArchiveSelection()
        }.start()

        return START_REDELIVER_INTENT
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? =
        null

    private data class ProcessedVolume(
        val prepared: NativeRdpkgPreparationEngine.Result,
        val imported: RdpkgImporter.ImportResult,
        val label: String,
        val destination: Uri,
    )

    private fun runPreparation(
        request: StartRequest,
    ) {
        when (
            request.sourceKind
        ) {
            NativeRdpkgSourceKind.RAW_TREE ->
                runRawPreparation(
                    request,
                )

            NativeRdpkgSourceKind.ARCHIVE_FILE ->
                runArchiveInitial(
                    request,
                )
        }
    }

    private fun runRawPreparation(
        request: StartRequest,
    ) {
        try {
            val processed =
                processPreparedSource(
                    request =
                        request,
                    sourceTreeUri =
                        Uri.parse(
                            request.sourceTreeUri,
                        ),
                    sourceRoot =
                        null,
                    sourceName =
                        request.sourceName,
                    destination =
                        Uri.parse(
                            request.destinationUri,
                        ),
                    progressPrefix =
                        "",
                )

            completeSingleVolume(
                request =
                    request,
                processed =
                    processed,
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            runStore.markCancelled(
                "Підготовку .rdpkg скасовано. Source не змінено, private staging очищено.",
            )

            notifyFinal(
                title =
                    "Створення .rdpkg скасовано",
                text =
                    "Source не змінено.",
                projectId =
                    request.projectId,
            )
        } catch (
            error:
                Throwable,
        ) {
            val message =
                error.message
                    ?: "Невідома помилка Kotlin-native підготовки."

            runStore.fail(
                message,
            )

            notifyFinal(
                title =
                    "Не вдалося створити .rdpkg",
                text =
                    message,
                projectId =
                    request.projectId,
            )
        } finally {
            finishWorker()
        }
    }

    private fun runArchiveInitial(
        request: StartRequest,
    ) {
        var archiveSourceStage:
            ArchiveIntakeStager.StagedSource? =
            null
        var archiveStage:
            ArchiveIntakeStager.Result? =
            null
        var preserveArchiveStage =
            false

        try {
            val stager =
                ArchiveIntakeStager(
                    context =
                        this,
                    onMessage = {
                        message ->
                        runStore.updatePreparing(
                            message,
                        )
                    },
                    onProgress = {
                        progress ->
                        runStore.updateProgress(
                            progress,
                        )
                        runStore.updatePreparing(
                            progress.displayText(),
                        )

                        updateNotificationThrottled(
                            title =
                                "Renault Docs · архів → .rdpkg",
                            text =
                                progress.displayText(),
                            projectId =
                                request.projectId,
                            cancellable =
                                true,
                        )
                    },
                    isCancelled = {
                        runStore.isCancelRequested()
                    },
                )

            val stagedSource =
                stager.stageSource(
                    sourceUri =
                        Uri.parse(
                            request.sourceTreeUri,
                        ),
                    sourceName =
                        request.sourceName,
                    stagingToken =
                        listOf(
                            request.projectId,
                            request.sourceName,
                        )
                            .joinToString(
                                "-",
                            ),
                )

            archiveSourceStage =
                stagedSource

            runStore.updatePreparing(
                "Перевіряю склад архіву без розпакування…",
            )
            updateNotification(
                title =
                    "Renault Docs · перевірка архіву",
                text =
                    "Шукаю Renault томи без розпакування…",
                projectId =
                    request.projectId,
                cancellable =
                    true,
            )

            val inspection =
                ArchiveIntake.inspectRawRoots(
                    stagedSource.sourceCopy,
                )
            val installedFromHints =
                installedVolumesForArchiveHints(
                    projectId =
                        request.projectId,
                    hints =
                        inspection.rawRoots,
                )

            if (
                inspection.rawRoots.isNotEmpty() &&
                installedFromHints !=
                    null
            ) {
                markArchiveHintsAlreadyInstalled(
                    request =
                        request,
                    installed =
                        installedFromHints,
                )
                return
            }

            val staged =
                stager.extract(
                    stagedSource,
                )

            archiveStage =
                staged

            val candidates =
                buildArchiveCandidates(
                    projectId =
                        request.projectId,
                    extractionRoot =
                        staged.extractionRoot,
                    rawRoots =
                        staged.rawRoots,
                )

            if (
                candidates.all {
                    it.installed
                }
            ) {
                markArchiveAllInstalled(
                    request =
                        request,
                    candidates =
                        candidates,
                )
                return
            }

            if (
                candidates.size >
                1
            ) {
                runStore.markWaitingForArchiveSelection(
                    extractionRoot =
                        staged.extractionRoot
                            .canonicalPath,
                    candidates =
                        candidates,
                )

                preserveArchiveStage =
                    true

                notifyFinal(
                    title =
                        "Renault Docs · вибери томи",
                    text =
                        "Знайдено томів: " +
                            candidates.size,
                    projectId =
                        request.projectId,
                )

                return
            }

            val rawRoot =
                staged.rawRoots.single()
            val duplicate =
                VolumeDuplicatePreflight
                    .check(
                        existing =
                            ProjectStore(
                                this,
                            ).volumes(
                                request.projectId,
                            ),
                        rawRoot =
                            rawRoot,
                    )

            duplicate.exact
                ?.let {
                    existing ->
                    val label =
                        VolumeDuplicatePreflight
                            .label(
                                existing,
                            )

                    runStore.markAlreadyPresent(
                        message =
                            "Том уже є в проєкті: " +
                                label +
                                ". Конвертацію пропущено.",
                        volumeId =
                            existing.id,
                        volumeTitle =
                            label,
                    )

                    notifyFinal(
                        title =
                            "Renault Docs · том уже є",
                        text =
                            label +
                                " · конвертацію пропущено",
                        projectId =
                            request.projectId,
                    )

                    return
                }

            val destination =
                createArchiveDestination(
                    destinationTreeUri =
                        Uri.parse(
                            request.destinationUri,
                        ),
                    request =
                        request,
                    rawRoot =
                        rawRoot,
                )

            val processed =
                processPreparedSource(
                    request =
                        request,
                    sourceTreeUri =
                        null,
                    sourceRoot =
                        rawRoot,
                    sourceName =
                        rawRoot.name,
                    destination =
                        destination,
                    progressPrefix =
                        "",
                )

            completeSingleVolume(
                request =
                    request,
                processed =
                    processed,
            )
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            runStore.markCancelled(
                "Підготовку архіву скасовано. Оригінальний архів не змінено, private staging очищено.",
            )

            notifyFinal(
                title =
                    "Архів → .rdpkg скасовано",
                text =
                    "Оригінальний архів не змінено.",
                projectId =
                    request.projectId,
            )
        } catch (
            error:
                Throwable,
        ) {
            val message =
                error.message
                    ?: "Невідома помилка archive intake."

            runStore.fail(
                message,
            )

            notifyFinal(
                title =
                    "Не вдалося обробити архів",
                text =
                    message,
                projectId =
                    request.projectId,
            )
        } finally {
            if (
                !preserveArchiveStage
            ) {
                if (
                    archiveStage !=
                    null
                ) {
                    archiveStage
                        ?.cleanup()
                } else {
                    archiveSourceStage
                        ?.cleanup()
                }
                runStore.clearArchiveSelectionData()
            }

            finishWorker()
        }
    }

    private fun runArchiveSelection() {
        val initialState =
            runStore.load()
        val projectId =
            initialState.projectId
                ?: run {
                    runStore.fail(
                        "Втрачено project id для archive batch."
                    )
                    finishWorker()
                    return
                }
        val project =
            ProjectStore(
                this,
            ).project(
                projectId,
            )
                ?: run {
                    runStore.fail(
                        "Проєкт більше не існує: " +
                            projectId,
                    )
                    cleanupPersistedArchiveWorkspace(
                        initialState.archiveExtractionRoot,
                    )
                    runStore.clearArchiveSelectionData()
                    finishWorker()
                    return
                }

        val request =
            StartRequest(
                requestId =
                    "archive-resume-" +
                        UUID.randomUUID()
                            .toString(),
                sourceTreeUri =
                    initialState.sourceUri
                        .orEmpty(),
                sourceName =
                    initialState.sourceName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: project.title,
                sourceKind =
                    NativeRdpkgSourceKind.ARCHIVE_FILE,
                destinationUri =
                    initialState.destinationUri
                        .orEmpty(),
                projectId =
                    project.id,
                projectTitle =
                    project.title,
                model =
                    project.model,
            )

        val processed =
            mutableListOf<ProcessedVolume>()
        val skipped =
            mutableListOf<String>()

        try {
            val extractionRoot =
                resolvePersistedArchiveRoot(
                    initialState.archiveExtractionRoot,
                )
            val selected =
                initialState.archiveCandidates
                    .filter {
                        it.selected &&
                            !it.installed
                    }

            require(
                selected.isNotEmpty(),
            ) {
                "Не вибрано жодного нового тому."
            }

            selected.forEachIndexed {
                index,
                candidate ->
                if (
                    runStore.isCancelRequested()
                ) {
                    throw ConversionCancelledException()
                }

                val rawRoot =
                    resolveArchiveCandidateRoot(
                        extractionRoot =
                            extractionRoot,
                        relativePath =
                            candidate.relativePath,
                    )

                val duplicate =
                    VolumeDuplicatePreflight
                        .check(
                            existing =
                                ProjectStore(
                                    this,
                                ).volumes(
                                    request.projectId,
                                ),
                            rawRoot =
                                rawRoot,
                        )

                duplicate.exact
                    ?.let {
                        existing ->
                        skipped +=
                            VolumeDuplicatePreflight
                                .label(
                                    existing,
                                )
                        return@forEachIndexed
                    }

                val destination =
                    createArchiveDestination(
                        destinationTreeUri =
                            Uri.parse(
                                request.destinationUri,
                            ),
                        request =
                            request,
                        rawRoot =
                            rawRoot,
                    )

                processed +=
                    processPreparedSource(
                        request =
                            request,
                        sourceTreeUri =
                            null,
                        sourceRoot =
                            rawRoot,
                        sourceName =
                            rawRoot.name,
                        destination =
                            destination,
                        progressPrefix =
                            "Том " +
                                (index + 1) +
                                "/" +
                                selected.size +
                                " · ",
                    )
            }

            if (
                runStore.isCancelRequested()
            ) {
                throw ConversionCancelledException()
            }

            if (
                processed.isEmpty()
            ) {
                runStore.markAlreadyPresent(
                    message =
                        "Усі вибрані томи вже є в проєкті. Конвертацію пропущено.",
                    volumeId =
                        initialState.archiveCandidates
                            .firstNotNullOfOrNull {
                                it.existingVolumeId
                            }
                            ?: "archive-existing",
                    volumeTitle =
                        "Усі вибрані томи вже встановлено",
                )

                notifyFinal(
                    title =
                        "Renault Docs · томи вже є",
                    text =
                        "Конвертацію пропущено.",
                    projectId =
                        request.projectId,
                )
            } else {
                completeArchiveBatch(
                    request =
                        request,
                    processed =
                        processed,
                    skipped =
                        skipped,
                )
            }
        } catch (
            cancelled:
                ConversionCancelledException,
        ) {
            runStore.markCancelled(
                "Archive batch скасовано. Уже завершені томи залишено встановленими; незавершений пакет очищено.",
            )

            notifyFinal(
                title =
                    "Archive batch скасовано",
                text =
                    "Завершені томи збережено.",
                projectId =
                    request.projectId,
            )
        } catch (
            error:
                Throwable,
        ) {
            val prefix =
                if (
                    processed.isNotEmpty()
                ) {
                    "Завершено томів: " +
                        processed.size +
                        ". "
                } else {
                    ""
                }
            val message =
                prefix +
                    (
                        error.message
                            ?: "Невідома помилка archive batch."
                    )

            runStore.fail(
                message,
            )

            notifyFinal(
                title =
                    "Archive batch · помилка",
                text =
                    message,
                projectId =
                    request.projectId,
            )
        } finally {
            cleanupPersistedArchiveWorkspace(
                initialState.archiveExtractionRoot,
            )
            runStore.clearArchiveSelectionData()
            finishWorker()
        }
    }

    private fun processPreparedSource(
        request: StartRequest,
        sourceTreeUri: Uri?,
        sourceRoot: File?,
        sourceName: String,
        destination: Uri,
        progressPrefix: String,
    ): ProcessedVolume {
        var packageValidated =
            false

        try {
            val engine =
                NativeRdpkgPreparationEngine(
                    context =
                        this,
                    onProgress = {
                        message ->
                        val display =
                            progressPrefix +
                                message
                        runStore.updatePreparing(
                            display,
                        )
                    },
                    onProgressState = {
                        progress ->
                        val display =
                            progressPrefix +
                                progress.displayText()

                        runStore.updateProgress(
                            progress,
                        )
                        runStore.updatePreparing(
                            display,
                        )

                        updateNotificationThrottled(
                            title =
                                "Renault Docs · створення .rdpkg",
                            text =
                                display,
                            projectId =
                                request.projectId,
                            cancellable =
                                true,
                        )
                    },
                    isCancelled = {
                        runStore.isCancelRequested()
                    },
                )

            val engineRequest =
                NativeRdpkgPreparationEngine
                    .Request(
                        sourceTreeUri =
                            sourceTreeUri,
                        sourceName =
                            sourceName,
                        projectId =
                            request.projectId,
                        model =
                            request.model,
                        destinationUri =
                            destination,
                        title =
                            request.projectTitle,
                    )

            val prepared =
                if (
                    sourceRoot !=
                    null
                ) {
                    engine.prepareLocal(
                        request =
                            engineRequest,
                        sourceRoot =
                            sourceRoot,
                    )
                } else {
                    engine.prepare(
                        engineRequest,
                    )
                }

            if (
                runStore.isCancelRequested()
            ) {
                throw ConversionCancelledException()
            }

            runStore.updateImporting(
                progressPrefix +
                    "Перевіряю та встановлюю створений .rdpkg…",
            )
            runStore.updateProgress(
                OperationProgress.indeterminate(
                    progressPrefix +
                        "Перевіряю…",
                ),
            )

            updateNotification(
                title =
                    "Renault Docs · перевірка .rdpkg",
                text =
                    progressPrefix +
                        "Перевіряю та встановлюю пакет…",
                projectId =
                    request.projectId,
                cancellable =
                    false,
            )

            val imported =
                RdpkgImporter
                    .install(
                        context =
                            this,
                        packageUri =
                            destination,
                        progress = {
                            message ->
                            runStore.updateImporting(
                                progressPrefix +
                                    message,
                            )
                        },
                        progressState = {
                            progress ->
                            runStore.updateProgress(
                                progress,
                            )

                            updateNotificationThrottled(
                                title =
                                    "Renault Docs · перевірка .rdpkg",
                                text =
                                    progressPrefix +
                                        progress.displayText(),
                                projectId =
                                    request.projectId,
                                cancellable =
                                    false,
                            )
                        },
                    )
                    .getOrThrow()

            require(
                imported.packageId ==
                    prepared.packageId
            ) {
                "Generated package_id не збігається після validation/import."
            }

            packageValidated =
                true

            val projectStore =
                ProjectStore(
                    this,
                )

            require(
                projectStore.project(
                    request.projectId,
                ) !=
                    null
            ) {
                "Проєкт більше не існує: " +
                    request.projectId
            }

            projectStore.upsertVolume(
                projectId =
                    request.projectId,
                volume =
                    imported.volume,
            )

            val label =
                listOfNotNull(
                    imported.volume.documentCode,
                    imported.volume.date,
                )
                    .joinToString(
                        " · ",
                    )
                    .ifBlank {
                        imported.volume.title
                    }

            return ProcessedVolume(
                prepared =
                    prepared,
                imported =
                    imported,
                label =
                    label,
                destination =
                    destination,
            )
        } catch (
            error:
                Throwable,
        ) {
            if (
                !packageValidated
            ) {
                val destinationCleaned =
                    deleteDestination(
                        destination,
                    )

                if (
                    !destinationCleaned
                ) {
                    throw IllegalStateException(
                        (
                            error.message
                                ?: "Не вдалося створити .rdpkg."
                        ) +
                            "\nУвага: неповний .rdpkg не вдалося видалити автоматично.",
                        error,
                    )
                }
            }

            throw error
        }
    }

    private fun completeSingleVolume(
        request: StartRequest,
        processed: ProcessedVolume,
    ) {
        val message =
            "Готово · " +
                processed.label +
                " · " +
                processed.prepared.sectionCount +
                " native" +
                "\nSHA-256: " +
                processed.prepared.sha256

        runStore.complete(
            message =
                message,
            packageId =
                processed.imported.packageId,
            volumeId =
                processed.imported.volume.id,
            volumeTitle =
                processed.label,
            sha256 =
                processed.prepared.sha256,
            filesTotal =
                processed.prepared.sourceFiles,
            changedFiles =
                processed.prepared.changedFiles,
            changesTotal =
                processed.prepared.changesTotal,
        )

        notifyFinal(
            title =
                "Renault Docs · .rdpkg готовий",
            text =
                processed.label +
                    " · " +
                    processed.prepared.sectionCount +
                    " native",
            projectId =
                request.projectId,
        )
    }

    private fun completeArchiveBatch(
        request: StartRequest,
        processed: List<ProcessedVolume>,
        skipped: List<String>,
    ) {
        val last =
            processed.last()

        val message =
            buildString {
                append(
                    "Готово · створено томів: ",
                )
                append(
                    processed.size,
                )

                if (
                    skipped.isNotEmpty()
                ) {
                    append(
                        " · уже було: ",
                    )
                    append(
                        skipped.size,
                    )
                }

                processed.forEach {
                    item ->
                    append(
                        "\n✓ ",
                    )
                    append(
                        item.label,
                    )
                }

                skipped.forEach {
                    label ->
                    append(
                        "\n• Уже є: ",
                    )
                    append(
                        label,
                    )
                }
            }

        runStore.complete(
            message =
                message,
            packageId =
                last.imported.packageId,
            volumeId =
                last.imported.volume.id,
            volumeTitle =
                if (
                    processed.size ==
                    1
                ) {
                    last.label
                } else {
                    processed.size
                        .toString() +
                        " томів"
                },
            sha256 =
                if (
                    processed.size ==
                    1
                ) {
                    last.prepared.sha256
                } else {
                    ""
                },
            filesTotal =
                processed.sumOf {
                    it.prepared.sourceFiles
                },
            changedFiles =
                processed.sumOf {
                    it.prepared.changedFiles
                },
            changesTotal =
                processed.sumOf {
                    it.prepared.changesTotal
                },
        )

        notifyFinal(
            title =
                "Renault Docs · archive batch готовий",
            text =
                "Створено: " +
                    processed.size +
                    if (
                        skipped.isNotEmpty()
                    ) {
                        " · уже було: " +
                            skipped.size
                    } else {
                        ""
                    },
            projectId =
                request.projectId,
        )
    }

    private fun buildArchiveCandidates(
        projectId: String,
        extractionRoot: File,
        rawRoots: List<File>,
    ): List<ArchiveVolumeCandidate> {
        val existing =
            ProjectStore(
                this,
            ).volumes(
                projectId,
            )

        return rawRoots
            .map {
                rawRoot ->
                val preflight =
                    VolumeDuplicatePreflight
                        .check(
                            existing =
                                existing,
                            rawRoot =
                                rawRoot,
                        )
                val relativePath =
                    extractionRoot
                        .canonicalFile
                        .toPath()
                        .relativize(
                            rawRoot.canonicalFile
                                .toPath(),
                        )
                        .toString()
                        .replace(
                            File.separatorChar,
                            '/',
                        )

                require(
                    relativePath.isNotBlank() &&
                        !relativePath.startsWith(
                            "../",
                        )
                ) {
                    "Некоректний raw-root path в archive staging."
                }

                ArchiveVolumeCandidate(
                    relativePath =
                        relativePath,
                    label =
                        archiveCandidateLabel(
                            rawRoot,
                        ),
                    installed =
                        preflight.exact !=
                            null,
                    possibleDuplicate =
                        preflight.exact ==
                            null &&
                            preflight.possible
                                .isNotEmpty(),
                    selected =
                        preflight.exact ==
                            null,
                    existingVolumeId =
                        preflight.exact
                            ?.id,
                )
            }
    }

    private fun archiveCandidateLabel(
        rawRoot: File,
    ): String {
        val identity =
            RenaultVolumeIdentity
                .parse(
                    rawRoot.name,
                )

        return listOfNotNull(
            identity.documentCode,
            identity.date,
        )
            .joinToString(
                " · ",
            )
            .ifBlank {
                rawRoot.name
            }
    }

    private fun createArchiveDestination(
        destinationTreeUri: Uri,
        request: StartRequest,
        rawRoot: File,
    ): Uri {
        val destinationTree =
            DocumentFile.fromTreeUri(
                this,
                destinationTreeUri,
            )
                ?: error(
                    "Папка для готових .rdpkg недоступна."
                )

        require(
            destinationTree.isDirectory &&
                destinationTree.canWrite(),
        ) {
            "Папка для готових .rdpkg недоступна для запису."
        }

        val fileName =
            RenaultVolumeIdentity
                .canonicalFileName(
                    model =
                        request.model,
                    metadata =
                        RenaultVolumeIdentity
                            .parse(
                                rawRoot.name,
                            ),
                    fallbackId =
                        rawRoot.name,
                )

        return destinationTree
            .createFile(
                "application/octet-stream",
                fileName,
            )
            ?.uri
            ?: error(
                "Не вдалося створити " +
                    fileName,
            )
    }

    private fun resolvePersistedArchiveRoot(
        path: String?,
    ): File {
        val rawPath =
            path
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: error(
                    "Втрачено archive extraction staging."
                )

        val base =
            File(
                noBackupFilesDir,
                "archive-intake",
            )
                .canonicalFile
        val root =
            File(
                rawPath,
            )
                .canonicalFile
        val prefix =
            base.path +
                File.separator

        require(
            root.isDirectory &&
                root.path.startsWith(
                    prefix,
                )
        ) {
            "Archive staging більше не доступний."
        }

        return root
    }

    private fun resolveArchiveCandidateRoot(
        extractionRoot: File,
        relativePath: String,
    ): File {
        val target =
            File(
                extractionRoot,
                relativePath,
            )
                .canonicalFile
        val prefix =
            extractionRoot
                .canonicalFile
                .path +
                File.separator

        require(
            target.isDirectory &&
                target.path.startsWith(
                    prefix,
                )
        ) {
            "Втрачено вибраний raw-том архіву."
        }

        return target
    }

    private fun cleanupPersistedArchiveWorkspace(
        extractionRootPath: String?,
    ) {
        val extractionRoot =
            runCatching {
                resolvePersistedArchiveRoot(
                    extractionRootPath,
                )
            }.getOrNull()
                ?: return

        val workRoot =
            extractionRoot.parentFile
                ?: return
        val base =
            File(
                noBackupFilesDir,
                "archive-intake",
            )
                .canonicalFile
        val safeWorkRoot =
            runCatching {
                workRoot.canonicalFile
            }.getOrNull()
                ?: return
        val prefix =
            base.path +
                File.separator

        if (
            safeWorkRoot.path.startsWith(
                prefix,
            )
        ) {
            safeWorkRoot.deleteRecursively()
        }
    }

    private fun installedVolumesForArchiveHints(
        projectId: String,
        hints: List<ArchiveIntake.RawRootHint>,
    ): List<ProjectVolumeRecord>? {
        if (
            hints.isEmpty()
        ) {
            return null
        }

        val existing =
            ProjectStore(
                this,
            ).volumes(
                projectId,
            )

        val matches =
            mutableListOf<ProjectVolumeRecord>()

        hints.forEach {
            hint ->
            val exact =
                VolumeDuplicatePreflight
                    .check(
                        existing =
                            existing,
                        rawRoot =
                            File(
                                hint.leafName,
                            ),
                    )
                    .exact
                    ?: return null

            matches +=
                exact
        }

        return matches
    }

    private fun markArchiveHintsAlreadyInstalled(
        request: StartRequest,
        installed: List<ProjectVolumeRecord>,
    ) {
        val labels =
            installed
                .map {
                    VolumeDuplicatePreflight
                        .label(
                            it,
                        )
                }
                .distinct()

        val message =
            if (
                labels.size ==
                1
            ) {
                "Том уже є в проєкті: " +
                    labels.single() +
                    ". Розпакування і конвертацію пропущено."
            } else {
                "Усі " +
                    labels.size +
                    " томів з архіву вже є в проєкті. " +
                    "Розпакування і конвертацію пропущено."
            }

        runStore.markAlreadyPresent(
            message =
                message,
            volumeId =
                installed.firstOrNull()
                    ?.id
                    ?: "archive-existing",
            volumeTitle =
                if (
                    labels.size ==
                    1
                ) {
                    labels.single()
                } else {
                    labels.size
                        .toString() +
                        " томів"
                },
        )

        notifyFinal(
            title =
                "Renault Docs · томи вже є",
            text =
                "Розпакування не потрібне.",
            projectId =
                request.projectId,
        )
    }

    private fun markArchiveAllInstalled(
        request: StartRequest,
        candidates: List<ArchiveVolumeCandidate>,
    ) {
        val label =
            if (
                candidates.size ==
                1
            ) {
                candidates.single()
                    .label
            } else {
                candidates.size
                    .toString() +
                    " томів"
            }

        runStore.markAlreadyPresent(
            message =
                if (
                    candidates.size ==
                    1
                ) {
                    "Том уже є в проєкті: " +
                        candidates.single()
                            .label +
                        ". Конвертацію пропущено."
                } else {
                    "Усі " +
                        candidates.size +
                        " томів з архіву вже є в проєкті. Конвертацію пропущено."
                },
            volumeId =
                candidates
                    .firstNotNullOfOrNull {
                        it.existingVolumeId
                    }
                    ?: "archive-existing",
            volumeTitle =
                label,
        )

        notifyFinal(
            title =
                "Renault Docs · томи вже є",
            text =
                label +
                    " · конвертацію пропущено",
            projectId =
                request.projectId,
        )
    }

    private fun finishWorker() {
        releaseWakeLock()

        active =
            false

        workerRunning.set(
            false,
        )

        stopForeground(
            STOP_FOREGROUND_DETACH,
        )

        stopSelf()
    }

    private fun parseRequest(
        intent: Intent,
    ): StartRequest? {
        val requestId =
            intent.getStringExtra(
                EXTRA_REQUEST_ID,
            )
                ?: return null
        val sourceTreeUri =
            intent.getStringExtra(
                EXTRA_SOURCE_URI,
            )
                ?: return null
        val sourceName =
            intent.getStringExtra(
                EXTRA_SOURCE_NAME,
            )
                ?: "Renault"
        val sourceKind =
            runCatching {
                NativeRdpkgSourceKind.valueOf(
                    intent.getStringExtra(
                        EXTRA_SOURCE_KIND,
                    )
                        ?: NativeRdpkgSourceKind.RAW_TREE.name,
                )
            }.getOrDefault(
                NativeRdpkgSourceKind.RAW_TREE,
            )
        val destinationUri =
            intent.getStringExtra(
                EXTRA_DESTINATION_URI,
            )
                ?: return null
        val projectId =
            intent.getStringExtra(
                EXTRA_PROJECT_ID,
            )
                ?: return null
        val projectTitle =
            intent.getStringExtra(
                EXTRA_PROJECT_TITLE,
            )
                ?: return null
        val model =
            intent.getStringExtra(
                EXTRA_MODEL,
            )
                ?: return null

        return StartRequest(
            requestId =
                requestId,
            sourceTreeUri =
                sourceTreeUri,
            sourceName =
                sourceName,
            sourceKind =
                sourceKind,
            destinationUri =
                destinationUri,
            projectId =
                projectId,
            projectTitle =
                projectTitle,
            model =
                model,
        )
    }

    private fun updateNotificationThrottled(
        title: String,
        text: String,
        projectId: String,
        cancellable: Boolean,
    ) {
        val now =
            System.currentTimeMillis()

        if (
            now -
                lastNotificationAt <
            NOTIFICATION_THROTTLE_MS
        ) {
            return
        }

        lastNotificationAt =
            now

        updateNotification(
            title =
                title,
            text =
                text,
            projectId =
                projectId,
            cancellable =
                cancellable,
        )
    }

    private fun updateNotification(
        title: String,
        text: String,
        projectId: String? = null,
        cancellable: Boolean,
    ) {
        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        title,
                    text =
                        text,
                    projectId =
                        projectId,
                    cancellable =
                        cancellable,
                ),
            )
    }

    private fun notifyFinal(
        title: String,
        text: String,
        projectId: String,
    ) {
        notificationManager()
            .notify(
                NOTIFICATION_ID,
                buildNotification(
                    title =
                        title,
                    text =
                        text,
                    projectId =
                        projectId,
                    cancellable =
                        false,
                ),
            )
    }

    private fun buildNotification(
        title: String,
        text: String,
        projectId: String?,
        cancellable: Boolean,
    ): Notification {
        val openIntent =
            if (
                projectId !=
                null
            ) {
                ProjectActivity.intent(
                    context =
                        this,
                    projectId =
                        projectId,
                )
            } else {
                Intent(
                    this,
                    MainActivity::class.java,
                )
            }

        val contentIntent =
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        return Notification.Builder(
            this,
            CHANNEL_ID,
        )
            .setSmallIcon(
                R.drawable.ic_notification_document,
            )
            .setContentTitle(
                title,
            )
            .setContentText(
                text,
            )
            .setContentIntent(
                contentIntent,
            )
            .setOnlyAlertOnce(
                true,
            )
            .setOngoing(
                runStore.load()
                    .isRunning,
            )
            .apply {
                val state =
                    runStore.load()

                if (
                    state.isRunning
                ) {
                    if (
                        state.progressTotal >
                        0
                    ) {
                        setProgress(
                            state.progressTotal,
                            state.progressCurrent
                                .coerceIn(
                                    0,
                                    state.progressTotal,
                                ),
                            false,
                        )
                    } else {
                        setProgress(
                            0,
                            0,
                            true,
                        )
                    }
                }

                if (
                    cancellable
                ) {
                    val cancelIntent =
                        Intent(
                            this@NativeRdpkgPreparationService,
                            NativeRdpkgPreparationService::class.java,
                        ).apply {
                            action =
                                ACTION_CANCEL
                        }

                    val cancelPending =
                        PendingIntent.getService(
                            this@NativeRdpkgPreparationService,
                            NOTIFICATION_ID +
                                1,
                            cancelIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE,
                        )

                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Скасувати",
                        cancelPending,
                    )
                }
            }
            .build()
    }

    private fun ensureNotificationChannel() {
        notificationManager()
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Renault native .rdpkg",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description =
                        "Підготовка Renault raw folder або архіву у .rdpkg"
                },
            )
    }

    private fun notificationManager():
        NotificationManager =
        getSystemService(
            NotificationManager::class.java,
        )

    private fun acquireWakeLock() {
        if (
            wakeLock?.isHeld ==
            true
        ) {
            return
        }

        val powerManager =
            getSystemService(
                PowerManager::class.java,
            )

        wakeLock =
            powerManager
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    packageName +
                        ":NativeRdpkgPreparation",
                )
                .apply {
                    setReferenceCounted(
                        false,
                    )
                    acquire(
                        WAKE_LOCK_TIMEOUT_MS,
                    )
                }
    }

    private fun releaseWakeLock() {
        wakeLock
            ?.takeIf {
                it.isHeld
            }
            ?.release()

        wakeLock =
            null
    }

    private fun deleteDestination(
        uri: Uri,
    ): Boolean {
        val resolver =
            contentResolver

        val deletedByResolver =
            runCatching {
                resolver.delete(
                    uri,
                    null,
                    null,
                ) >
                    0
            }.getOrDefault(
                false,
            )

        if (
            deletedByResolver
        ) {
            return true
        }

        if (
            !DocumentsContract.isDocumentUri(
                this,
                uri,
            )
        ) {
            return false
        }

        return runCatching {
            DocumentsContract.deleteDocument(
                resolver,
                uri,
            )
        }.getOrDefault(
            false,
        )
    }

    override fun onDestroy() {
        releaseWakeLock()

        active =
            false

        super.onDestroy()
    }

    private fun stopForMissingRequest():
        Int {
        active =
            false

        workerRunning.set(
            false,
        )
        startInFlight.set(
            false,
        )

        runStore.fail(
            "Не вдалося відновити параметри native .rdpkg підготовки.",
        )

        stopSelf()

        return START_NOT_STICKY
    }

    companion object {
        @Volatile
        private var active =
            false

        private val startInFlight =
            AtomicBoolean(
                false,
            )

        private const val ACTION_START =
            "com.saney.renaultdocs.action.NATIVE_RDPKG_START"

        private const val ACTION_CANCEL =
            "com.saney.renaultdocs.action.NATIVE_RDPKG_CANCEL"

        private const val ACTION_RESUME_ARCHIVE =
            "com.saney.renaultdocs.action.NATIVE_RDPKG_RESUME_ARCHIVE"

        private const val EXTRA_REQUEST_ID =
            "requestId"

        private const val EXTRA_SOURCE_URI =
            "sourceUri"

        private const val EXTRA_SOURCE_NAME =
            "sourceName"

        private const val EXTRA_SOURCE_KIND =
            "sourceKind"

        private const val EXTRA_DESTINATION_URI =
            "destinationUri"

        private const val EXTRA_PROJECT_ID =
            "projectId"

        private const val EXTRA_PROJECT_TITLE =
            "projectTitle"

        private const val EXTRA_MODEL =
            "model"

        private const val CHANNEL_ID =
            "renault_native_rdpkg"

        private const val NOTIFICATION_ID =
            3702

        private const val NOTIFICATION_THROTTLE_MS =
            700L

        private const val WAKE_LOCK_TIMEOUT_MS =
            6L *
                60L *
                60L *
                1000L

        fun isActive():
            Boolean =
            active

        fun start(
            context: Context,
            request: StartRequest,
        ) {
            if (
                !startInFlight.compareAndSet(
                    false,
                    true,
                )
            ) {
                return
            }

            val claimed =
                NativeRdpkgRunStore(
                    context,
                ).claimStartRequest(
                    request.requestId,
                )

            if (
                !claimed
            ) {
                startInFlight.set(
                    false,
                )
                return
            }

            val intent =
                Intent(
                    context,
                    NativeRdpkgPreparationService::class.java,
                ).apply {
                    action =
                        ACTION_START
                    putExtra(
                        EXTRA_REQUEST_ID,
                        request.requestId,
                    )
                    putExtra(
                        EXTRA_SOURCE_URI,
                        request.sourceTreeUri,
                    )
                    putExtra(
                        EXTRA_SOURCE_NAME,
                        request.sourceName,
                    )
                    putExtra(
                        EXTRA_SOURCE_KIND,
                        request.sourceKind.name,
                    )
                    putExtra(
                        EXTRA_DESTINATION_URI,
                        request.destinationUri,
                    )
                    putExtra(
                        EXTRA_PROJECT_ID,
                        request.projectId,
                    )
                    putExtra(
                        EXTRA_PROJECT_TITLE,
                        request.projectTitle,
                    )
                    putExtra(
                        EXTRA_MODEL,
                        request.model,
                    )
                }

            try {
                context.startForegroundService(
                    intent,
                )
            } catch (
                error:
                    Throwable,
            ) {
                startInFlight.set(
                    false,
                )
                throw error
            }
        }

        fun resumePersisted(
            context: Context,
            state: NativeRdpkgRunState,
            project: RenaultProject,
        ): Boolean {
            val sourceUri =
                state.sourceUri
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: return false
            val destinationUri =
                state.destinationUri
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: return false

            if (
                !state.isRunning ||
                state.projectId !=
                    project.id
            ) {
                return false
            }

            if (
                state.sourceKind ==
                    NativeRdpkgSourceKind.ARCHIVE_FILE &&
                !state.archiveExtractionRoot
                    .isNullOrBlank() &&
                state.archiveCandidates
                    .isNotEmpty()
            ) {
                return startArchiveResumeService(
                    context,
                )
            }

            return runCatching {
                start(
                    context = context,
                    request =
                        StartRequest(
                            requestId =
                                "resume-" +
                                    UUID.randomUUID()
                                        .toString(),
                            sourceTreeUri =
                                sourceUri,
                            sourceName =
                                state.sourceName
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: project.title,
                            sourceKind =
                                state.sourceKind,
                            destinationUri =
                                destinationUri,
                            projectId =
                                project.id,
                            projectTitle =
                                project.title,
                            model =
                                project.model,
                        ),
                )
                true
            }.getOrDefault(
                false,
            )
        }

        fun resumeArchiveSelection(
            context: Context,
        ): Boolean {
            val store =
                NativeRdpkgRunStore(
                    context,
                )

            val selected =
                runCatching {
                    store.beginArchiveSelectionProcessing()
                }.getOrNull()
                    ?: return false

            if (
                selected.isEmpty()
            ) {
                store.restoreArchiveWaitingSelection()
                return false
            }

            val started =
                startArchiveResumeService(
                    context,
                )

            if (
                !started
            ) {
                store.restoreArchiveWaitingSelection()
            }

            return started
        }

        private fun startArchiveResumeService(
            context: Context,
        ): Boolean =
            runCatching {
                val intent =
                    Intent(
                        context,
                        NativeRdpkgPreparationService::class.java,
                    ).apply {
                        action =
                            ACTION_RESUME_ARCHIVE
                    }

                context.startForegroundService(
                    intent,
                )
                true
            }.getOrDefault(
                false,
            )

        fun requestCancel(
            context: Context,
        ) {
            val store =
                NativeRdpkgRunStore(
                    context,
                )
            val state =
                store.load()

            if (
                state.isWaitingForSelection
            ) {
                cleanupWaitingArchiveWorkspace(
                    context =
                        context,
                    extractionRootPath =
                        state.archiveExtractionRoot,
                )
                store.clearArchiveSelectionData()
                store.markCancelled(
                    "Вибір томів скасовано. Оригінальний архів не змінено, private staging очищено.",
                )

                context.getSystemService(
                    NotificationManager::class.java,
                )
                    ?.cancel(
                        NOTIFICATION_ID,
                    )
                return
            }

            val intent =
                Intent(
                    context,
                    NativeRdpkgPreparationService::class.java,
                ).apply {
                    action =
                        ACTION_CANCEL
                }

            context.startService(
                intent,
            )
        }

        private fun cleanupWaitingArchiveWorkspace(
            context: Context,
            extractionRootPath: String?,
        ) {
            val path =
                extractionRootPath
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: return
            val base =
                File(
                    context.noBackupFilesDir,
                    "archive-intake",
                )
                    .canonicalFile
            val extraction =
                runCatching {
                    File(
                        path,
                    ).canonicalFile
                }.getOrNull()
                    ?: return
            val prefix =
                base.path +
                    File.separator

            if (
                !extraction.path
                    .startsWith(
                        prefix,
                    )
            ) {
                return
            }

            val workRoot =
                extraction.parentFile
                    ?.canonicalFile
                    ?: return

            if (
                workRoot.path
                    .startsWith(
                        prefix,
                    )
            ) {
                workRoot.deleteRecursively()
            }
        }
    }
}
