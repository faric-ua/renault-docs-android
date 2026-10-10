package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class ProjectActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var settings: AppSettings
    private lateinit var nativeRunStore: NativeRdpkgRunStore
    private lateinit var rdpkgImportRunStore: RdpkgImportRunStore
    private lateinit var rdpkgExportRunStore: RdpkgExportRunStore
    private lateinit var rdpkgShareRunStore: RdpkgShareRunStore
    private lateinit var helpDialogs:
        LifecycleHelpDialogController
    private lateinit var project: RenaultProject
    private lateinit var volumeContainer: LinearLayout
    private lateinit var projectScroll: ScrollView
    private lateinit var statusText: TextView
    private lateinit var operationStatus: OperationStatusView
    private lateinit var nativeTerminalStatusRow: LinearLayout
    private lateinit var nativeTerminalStatusText: TextView
    private lateinit var countText: TextView
    private lateinit var addPanelBody: LinearLayout
    private lateinit var addPanelPinButton: TextView
    private lateinit var addPanelToggleButton: TextView
    private var restoredStatusDetailsExpanded = false
    private val monochromePinPaint by lazy {
        Paint().apply {
            colorFilter =
                ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        setSaturation(0f)
                    },
                )
        }
    }
    private var addPanelExpanded =
        false
    private var addPanelPinned =
        false
    // Ephemeral landscape UI only; never persisted as the portrait pin preference.
    private var addPanelLandscapeExpanded =
        false
    private var pendingManualImport =
        false
    private var restoredScrollY =
        0
    private var activeProjectDialogKind:
        String =
        ""
    private var activeProjectDialogVolumeId:
        String? =
        null
    private var activeProjectDialogTreeUri:
        String? =
        null
    private var activeProjectDialogAllowOverride:
        Boolean =
        false
    private var activeProjectDialogParentKind:
        String? =
        null
    private var pendingRdpkgExportVolumeId:
        String? =
        null
    private var pendingNativeSourceUri:
        String? =
        null
    private var pendingNativeSourceName:
        String? =
        null
    private var pendingNativeSourceKind:
        NativeRdpkgSourceKind =
        NativeRdpkgSourceKind.RAW_TREE
    private var pendingNativeRequestId:
        String? =
        null
    private var archiveSourcePreflightOpen = false
    private var archiveSourceDetailsExpanded = false
    private var archiveChooserDialog:
        AlertDialog? =
        null
    private var lastShownNativeFinishedAt =
        0L
    private val nativeRunHandler by lazy {
        Handler(
            Looper.getMainLooper(),
        )
    }
    private val nativeRunRefresh =
        object : Runnable {
            override fun run() {
                refreshNativeRunState()
                refreshRdpkgImportRunState()
                refreshRdpkgExportRunState()
                refreshRdpkgShareRunState()
                nativeRunHandler.postDelayed(
                    this,
                    NATIVE_RUN_REFRESH_MS,
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        pendingManualImport =
            savedInstanceState
                ?.getBoolean(
                    STATE_PENDING_MANUAL_IMPORT,
                    false,
                )
                ?: false

        restoredScrollY =
            savedInstanceState
                ?.getInt(
                    STATE_SCROLL_Y,
                    0,
                )
                ?: 0
        restoredStatusDetailsExpanded =
            savedInstanceState?.getBoolean(
                STATE_OPERATION_STATUS_DETAILS_EXPANDED,
                false,
            ) ?: false

        activeProjectDialogKind =
            savedInstanceState
                ?.getString(
                    STATE_ACTIVE_DIALOG_KIND,
                )
                .orEmpty()
        activeProjectDialogVolumeId =
            savedInstanceState
                ?.getString(
                    STATE_ACTIVE_DIALOG_VOLUME_ID,
                )
        activeProjectDialogTreeUri =
            savedInstanceState
                ?.getString(
                    STATE_ACTIVE_DIALOG_TREE_URI,
                )
        activeProjectDialogAllowOverride =
            savedInstanceState
                ?.getBoolean(
                    STATE_ACTIVE_DIALOG_ALLOW_OVERRIDE,
                    false,
                )
                ?: false
        activeProjectDialogParentKind =
            savedInstanceState
                ?.getString(
                    STATE_ACTIVE_DIALOG_PARENT_KIND,
                )

        pendingRdpkgExportVolumeId =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_RDPKG_EXPORT_VOLUME_ID,
                )
        pendingNativeSourceUri =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_NATIVE_SOURCE_URI,
                )
        pendingNativeSourceName =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_NATIVE_SOURCE_NAME,
                )
        pendingNativeSourceKind =
            runCatching {
                NativeRdpkgSourceKind.valueOf(
                    savedInstanceState
                        ?.getString(
                            STATE_PENDING_NATIVE_SOURCE_KIND,
                        )
                        ?: NativeRdpkgSourceKind.RAW_TREE.name,
                )
            }.getOrDefault(
                NativeRdpkgSourceKind.RAW_TREE,
            )
        pendingNativeRequestId =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_NATIVE_REQUEST_ID,
                )
        archiveSourcePreflightOpen =
            savedInstanceState?.getBoolean("archiveSourcePreflightOpen", false) ?: false
        archiveSourceDetailsExpanded =
            savedInstanceState?.getBoolean("archiveSourceDetailsExpanded", false) ?: false

        store =
            ProjectStore(
                this,
            )
        settings =
            AppSettings(
                this,
            )
        addPanelPinned =
            settings.projectAddPanelPinned
        addPanelExpanded =
            savedInstanceState
                ?.getBoolean(
                    STATE_ADD_PANEL_EXPANDED,
                    addPanelPinned,
                )
                ?: addPanelPinned
        if (
            addPanelPinned
        ) {
            addPanelExpanded =
                true
        }
        // Home -> New Volume -> project chooser is an options handoff,
        // not permission to launch an Android file picker automatically.
        // Respect restored expansion after rotation; preserve the pin setting.
        if (
            savedInstanceState == null &&
            intent.getBooleanExtra(EXTRA_SHOW_ADD_PANEL, false)
        ) {
            addPanelExpanded = true
            addPanelLandscapeExpanded = true
        }

        nativeRunStore =
            NativeRdpkgRunStore(
                this,
            )
        rdpkgImportRunStore =
            RdpkgImportRunStore(
                this,
            )
        rdpkgExportRunStore =
            RdpkgExportRunStore(
                this,
            )
        rdpkgShareRunStore =
            RdpkgShareRunStore(
                this,
            )

        val projectId =
            intent.getStringExtra(
                EXTRA_PROJECT_ID,
            )
                .orEmpty()

        project =
            store.project(
                projectId,
            )
                ?: run {
                    finish()
                    return
                }

        helpDialogs =
            LifecycleHelpDialogController(
                activity = this,
                resolve = ::helpSpec,
            )
        helpDialogs.restore(
            savedInstanceState,
        )

        setContentView(
            buildContent(),
        )
        helpDialogs.restoreOpen()
        render()
        restoreProjectScroll()
        repairSavedVolumeMetadata()

        if (archiveSourcePreflightOpen && !pendingNativeSourceUri.isNullOrBlank()) {
            volumeContainer.post {
                // Restores only the confirmation, not the conversion.
                showArchiveSourcePreflight()
            }
        }

        if (
            savedInstanceState !=
                null &&
            activeProjectDialogKind
                .isNotBlank()
        ) {
            volumeContainer.post {
                restoreProjectDialog()
            }
        }

        if (
            savedInstanceState ==
                null &&
            intent.getBooleanExtra(
                EXTRA_OPEN_PICKER,
                false,
            )
        ) {
            volumeContainer.post {
                openPackagePicker()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        nativeRunHandler.removeCallbacks(
            nativeRunRefresh,
        )
        nativeRunHandler.post(
            nativeRunRefresh,
        )
    }

    override fun onStop() {
        nativeRunHandler.removeCallbacks(
            nativeRunRefresh,
        )

        super.onStop()
    }

    override fun onDestroy() {
        archiveChooserDialog
            ?.dismiss()
        archiveChooserDialog =
            null

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::volumeContainer.isInitialized
        ) {
            render()
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        super.onSaveInstanceState(
            outState,
        )

        outState.putBoolean(
            STATE_PENDING_MANUAL_IMPORT,
            pendingManualImport,
        )
        outState.putBoolean(
            STATE_ADD_PANEL_EXPANDED,
            addPanelExpanded,
        )
        outState.putBoolean(
            STATE_OPERATION_STATUS_DETAILS_EXPANDED,
            if (::operationStatus.isInitialized) {
                operationStatus.isProjectDetailsExpanded()
            } else {
                false
            },
        )
        outState.putInt(
            STATE_SCROLL_Y,
            if (
                ::projectScroll.isInitialized
            ) {
                projectScroll.scrollY
            } else {
                restoredScrollY
            },
        )
        helpDialogs.save(
            outState,
        )

        outState.putString(
            STATE_ACTIVE_DIALOG_KIND,
            activeProjectDialogKind,
        )
        outState.putString(
            STATE_ACTIVE_DIALOG_VOLUME_ID,
            activeProjectDialogVolumeId,
        )
        outState.putString(
            STATE_ACTIVE_DIALOG_TREE_URI,
            activeProjectDialogTreeUri,
        )
        outState.putBoolean(
            STATE_ACTIVE_DIALOG_ALLOW_OVERRIDE,
            activeProjectDialogAllowOverride,
        )
        outState.putString(
            STATE_ACTIVE_DIALOG_PARENT_KIND,
            activeProjectDialogParentKind,
        )

        outState.putString(
            STATE_PENDING_RDPKG_EXPORT_VOLUME_ID,
            pendingRdpkgExportVolumeId,
        )
        outState.putString(
            STATE_PENDING_NATIVE_SOURCE_URI,
            pendingNativeSourceUri,
        )
        outState.putString(
            STATE_PENDING_NATIVE_SOURCE_NAME,
            pendingNativeSourceName,
        )
        outState.putString(
            STATE_PENDING_NATIVE_SOURCE_KIND,
            pendingNativeSourceKind.name,
        )
        outState.putString(
            STATE_PENDING_NATIVE_REQUEST_ID,
            pendingNativeRequestId,
        )
        outState.putBoolean("archiveSourcePreflightOpen", archiveSourcePreflightOpen)
        outState.putBoolean("archiveSourceDetailsExpanded", archiveSourceDetailsExpanded)
    }

    @Deprecated(
        "Uses platform SAF result for minSdk 26 compatibility.",
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data,
        )

        when (
            requestCode
        ) {
            REQUEST_RDPKG_FILE ->
                handleRdpkgResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )

            REQUEST_VOLUME_FOLDER ->
                handleFolderResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )

            REQUEST_RDPKG_EXPORT ->
                handleRdpkgExportResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )

            REQUEST_NATIVE_RDPKG_SOURCE ->
                handleNativeRdpkgSourceResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )

            REQUEST_NATIVE_RDPKG_ARCHIVE_SOURCE ->
                handleNativeRdpkgArchiveSourceResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )

            REQUEST_NATIVE_RDPKG_DESTINATION ->
                handleNativeRdpkgDestinationResult(
                    resultCode =
                        resultCode,
                    data =
                        data,
                )
        }
    }

    private fun handleRdpkgResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode != RESULT_OK) {
            statusText.text = "Імпорт .rdpkg скасовано."
            return
        }

        val uri = data?.data
        if (uri == null) {
            statusText.text = "Android не повернув адресу .rdpkg."
            return
        }

        persistReadPermission(
            uri = uri,
            returnedFlags = data.flags,
        )

        val existing = rdpkgImportRunStore.load()
        if (existing.isRunning) {
            statusText.text = "Інший .rdpkg вже імпортується."
            return
        }

        statusText.text = "Імпортую .rdpkg…"
        val started = RdpkgImportService.start(
            context = this,
            projectId = project.id,
            packageUri = uri,
        )
        if (!started) {
            statusText.text = "Не вдалося запустити імпорт .rdpkg."
        }
    }

    private fun refreshRdpkgImportRunState() {
        if (!::rdpkgImportRunStore.isInitialized || !::statusText.isInitialized || !::project.isInitialized) return

        val state = rdpkgImportRunStore.load()
        if (state.projectId != project.id || state.isConsumed) return

        if (state.isRunning) {
            val detail =
                state.message
                    .ifBlank {
                        "Імпортую…"
                    }
            statusText.text = DEFAULT_STATUS_TEXT
            if (::operationStatus.isInitialized) {
                operationStatus.showRunning(
                    title = "Імпорт тому",
                    detail = detail,
                    current =
                        state.progressCurrent
                            .takeIf {
                                state.progressTotal >
                                    0
                            },
                    total =
                        state.progressTotal
                            .takeIf {
                                it >
                                    0
                            },
                )
            }
            return
        }

        if (!state.isTerminal || state.finishedAtMs <= 0L) return

        if (state.phase == RdpkgImportRunPhase.FAILED) {
            val detail = "Не вдалося імпортувати .rdpkg: " + state.message
            statusText.text = detail
            if (::operationStatus.isInitialized) {
                operationStatus.showTerminal("Імпорт тому · помилка", detail) {
                    rdpkgImportRunStore.consume(state.finishedAtMs)
                }
            }
            return
        }

        val packageId = state.packageId
        if (packageId.isNullOrBlank()) {
            statusText.text = "Імпорт .rdpkg завершився без package id."
            return
        }

        val treeUri = LocalDatasetDocumentsProvider.treeUriFor(packageId)
        PreparedVolumeReader.read(
            context = applicationContext,
            treeUri = treeUri,
        ).onSuccess { imported ->
            importPreparedVolume(
                volume = imported,
                allowOverride = true,
            )
            rdpkgImportRunStore.consume(state.finishedAtMs)
        }.onFailure { error ->
            statusText.text =
                "Пакет встановлено, але не вдалося додати том у проєкт: " +
                    (error.message ?: "невідома помилка")
        }
    }

    private fun handleRdpkgExportResult(
        resultCode: Int,
        data: Intent?,
    ) {
        val volumeId =
            pendingRdpkgExportVolumeId

        pendingRdpkgExportVolumeId =
            null

        if (
            resultCode !=
            RESULT_OK
        ) {
            statusText.text =
                "Експорт .rdpkg скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу для .rdpkg."
            return
        }

        val volume =
            store.volumes(
                project.id,
            )
                .firstOrNull {
                    it.id ==
                        volumeId
                }

        if (
            volume ==
            null
        ) {
            statusText.text =
                "Не вдалося знайти том для експорту."
            return
        }

        statusText.text = DEFAULT_STATUS_TEXT
        val started =
            RdpkgExportService.start(
                context = this,
                projectId = project.id,
                volumeId = volume.id,
                destinationUri = uri,
            )
        if (!started) {
            statusText.text = "Не вдалося запустити експорт .rdpkg."
        }
    }

    private fun refreshRdpkgExportRunState() {
        if (!::rdpkgExportRunStore.isInitialized || !::project.isInitialized || !::operationStatus.isInitialized) return
        val state = rdpkgExportRunStore.load()
        if (state.projectId != project.id) return

        if (state.isRunning) {
            statusText.text = DEFAULT_STATUS_TEXT
            operationStatus.showRunning(
                title = "Експорт тому",
                detail =
                    state.message
                        .ifBlank {
                            "Експортую…"
                        },
                current =
                    state.progressCurrent
                        .takeIf {
                            state.progressTotal >
                                0
                        },
                total =
                    state.progressTotal
                        .takeIf {
                            it >
                                0
                        },
                subject =
                    state.destinationUri
                        ?.let {
                            uriText ->
                            DocumentFile
                                .fromSingleUri(
                                    this,
                                    Uri.parse(
                                        uriText,
                                    ),
                                )
                                ?.name
                        }
                        ?: volumeOperationSubject(
                            state.volumeId,
                        ),
            )
            return
        }

        if (!state.isTerminal || state.finishedAtMs <= 0L || state.isTerminalDismissed) return

        statusText.text = DEFAULT_STATUS_TEXT
        if (state.phase == RdpkgExportRunPhase.COMPLETE) {
            val detail =
                buildString {
                    append(state.message.ifBlank { "Експорт .rdpkg завершено." })
                    state.sha256?.takeIf { it.isNotBlank() }?.let {
                        append("\nSHA-256: ")
                        append(it)
                    }
                }
            operationStatus.showTerminal("Експорт тому завершено", detail) {
                rdpkgExportRunStore.dismissTerminal(state.finishedAtMs)
                operationStatus.hide()
            }
        } else {
            val detail = "Не вдалося експортувати .rdpkg: " + state.message
            operationStatus.showTerminal("Експорт тому · помилка", detail) {
                rdpkgExportRunStore.dismissTerminal(state.finishedAtMs)
                operationStatus.hide()
            }
        }
    }

    /**
     * Read persisted source provenance without starting, resuming or
     * clearing any conversion. The last run survives normal completion.
     */
    private fun showLastNativeSourceDiagnostics() {
        val last = nativeRunStore.load()
        val source = last.sourceUri
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val document = source?.let { uri ->
            runCatching {
                if (last.sourceKind == NativeRdpkgSourceKind.ARCHIVE_FILE) {
                    DocumentFile.fromSingleUri(this, uri)
                } else {
                    DocumentFile.fromTreeUri(this, uri)
                }
            }.getOrNull()
        }
        val currentName = document?.let { runCatching { it.name }.getOrNull() }
        val currentSize = document?.let {
            runCatching { if (it.exists()) it.length() else null }.getOrNull()
        }
        val documentId = source?.let { uri ->
            runCatching {
                if (DocumentsContract.isDocumentUri(this, uri)) {
                    DocumentsContract.getDocumentId(uri)
                } else {
                    null
                }
            }.getOrNull()
        }
        fun formattedTime(value: Long): String =
            if (value <= 0L) "немає"
            else DateFormat.getDateTimeInstance().format(Date(value))

        val report = buildString {
            appendLine("Renault Docs — останнє native .rdpkg (лише читання)")
            appendLine("Відкритий проєкт: ${project.title} (${project.id})")
            appendLine("Проєкт останньої операції: ${last.projectId ?: "немає"}")
            appendLine("Фаза: ${last.phase}")
            appendLine("Тип джерела: ${last.sourceKind}")
            appendLine("Назва джерела під час вибору: ${last.sourceName ?: "немає"}")
            appendLine("URI джерела: ${last.sourceUri ?: "немає"}")
            appendLine("Провайдер: ${source?.authority ?: "немає"}")
            appendLine("Document ID: ${documentId ?: "недоступний"}")
            appendLine("Назва зараз: ${currentName ?: "недоступна"}")
            appendLine("Розмір зараз: ${currentSize?.takeIf { it > 0 }?.toString() ?: "невідомий"} байт")
            appendLine("URI призначення: ${last.destinationUri ?: "немає"}")
            appendLine(NativeRdpkgBatchReportFormatter.reportLines(last))
            appendLine("Початок: ${formattedTime(last.startedAtMs)}")
            appendLine("Завершення: ${formattedTime(last.finishedAtMs)}")
            appendLine("Папка, відкрита в SAF-пікері, не зберігається окремо.")
            appendLine("Document ID іноді містить фізичний шлях джерела.")
            appendLine("Звіт не змінює файли й не запускає підготовку.")
        }
        val content = ScrollView(this).apply {
            addView(
                Ui.textView(this@ProjectActivity, report, 13f, Ui.text).apply {
                    setTextIsSelectable(true)
                    setPadding(
                        Ui.dp(this@ProjectActivity, 18),
                        Ui.dp(this@ProjectActivity, 12),
                        Ui.dp(this@ProjectActivity, 18),
                        Ui.dp(this@ProjectActivity, 12),
                    )
                },
            )
        }
        AlertDialog.Builder(this)
            .setTitle("Джерело останньої .rdpkg")
            .setView(content)
            .setPositiveButton("Копіювати звіт") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Renault Docs source", report))
                Toast.makeText(this, "Звіт скопійовано", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрити", null)
            .create()
            .also { dialog ->
                dialog.show()
                DialogUi.apply(dialog, DialogRole.CHOICE)
            }
    }

    private fun handleNativeRdpkgSourceResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (
            resultCode !=
            RESULT_OK
        ) {
            pendingNativeSourceUri =
                null
            pendingNativeSourceName =
                null
            pendingNativeSourceKind =
                NativeRdpkgSourceKind.RAW_TREE
            pendingNativeRequestId =
                null
            statusText.text =
                "Вибір raw Renault папки скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу raw Renault папки."
            return
        }

        persistReadPermission(
            uri =
                uri,
            returnedFlags =
                data.flags,
        )

        val sourceName =
            DocumentFile
                .fromTreeUri(
                    this,
                    uri,
                )
                ?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Renault"

        // Fail closed on explicit cross-model raw folders before asking where to save.
        ArchiveSourceGuard.conflictingModel(sourceName, project)?.let { problem ->
            showArchiveSourceRejected(problem)
            return
        }

        pendingNativeSourceUri =
            uri.toString()
        pendingNativeSourceName =
            sourceName
        pendingNativeSourceKind =
            NativeRdpkgSourceKind.RAW_TREE
        pendingNativeRequestId =
            UUID.randomUUID()
                .toString()

        openNativeRdpkgDestinationPicker(
            sourceName,
        )
    }

    private fun handleNativeRdpkgArchiveSourceResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (
            resultCode !=
            RESULT_OK
        ) {
            pendingNativeSourceUri =
                null
            pendingNativeSourceName =
                null
            pendingNativeSourceKind =
                NativeRdpkgSourceKind.RAW_TREE
            pendingNativeRequestId =
                null
            statusText.text =
                "Вибір Renault архіву скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу архіву."
            return
        }

        persistReadPermission(
            uri =
                uri,
            returnedFlags =
                data.flags,
        )

        val sourceName =
            DocumentFile
                .fromSingleUri(
                    this,
                    uri,
                )
                ?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Renault-archive"

        ArchiveSourceGuard.inputError(sourceName, project)?.let { problem ->
            showArchiveSourceRejected(problem)
            return
        }
        pendingNativeSourceUri = uri.toString()
        pendingNativeSourceName = sourceName
        pendingNativeSourceKind = NativeRdpkgSourceKind.ARCHIVE_FILE
        pendingNativeRequestId = UUID.randomUUID().toString()
        archiveSourceDetailsExpanded = false
        showArchiveSourcePreflight()
    }

    private fun showArchiveSourceRejected(message: String) {
        statusText.text = message
        val dialog = AlertDialog.Builder(this)
            .setTitle("Неправильне джерело")
            .setMessage(message)
            .setPositiveButton("Зрозуміло", null)
            .create()
        dialog.show()
        DialogUi.apply(dialog, DialogRole.HELP)
    }

    private fun clearPendingArchiveSource() {
        archiveSourcePreflightOpen = false
        archiveSourceDetailsExpanded = false
        pendingNativeSourceUri = null
        pendingNativeSourceName = null
        pendingNativeSourceKind = NativeRdpkgSourceKind.RAW_TREE
        pendingNativeRequestId = null
    }

    private fun showArchiveSourcePreflight() {
        val sourceName = pendingNativeSourceName ?: return
        val source = pendingNativeSourceUri?.let(Uri::parse) ?: return
        ArchiveSourceGuard.inputError(sourceName, project)?.let { problem ->
            clearPendingArchiveSource()
            showArchiveSourceRejected(problem)
            return
        }
        val documentId = runCatching {
            if (DocumentsContract.isDocumentUri(this, source)) {
                DocumentsContract.getDocumentId(source)
            } else {
                source.toString()
            }
        }.getOrDefault(source.toString())
        val matches = ArchiveSourceGuard.catalogMatches(
            store, RenaultVolumeIdentity.parse(sourceName),
        )
        val previewMessage = buildString {
            appendLine("Архів: $sourceName")
            appendLine("Проєкт: ${project.title}")
            appendLine()
            if (matches.isEmpty()) {
                appendLine("У каталозі встановлених томів збігів за NT немає.")
            } else {
                appendLine("Можливий дублікат — знайдено схожих томів: ${matches.size}")
                matches.take(2).forEach { match ->
                    val kind = if (match.matchingMetadata) {
                        "збіг метаданих"
                    } else {
                        "можливий дублікат"
                    }
                    appendLine("• ${match.projectTitle}: ${match.volumeTitle} — $kind")
                }
                if (matches.size > 2) {
                    appendLine("Ще ${matches.size - 2} — у деталях.")
                }
                appendLine("Вміст архівів не порівнювався.")
            }
            appendLine()
            append("Продовжити вибір папки збереження?")
        }
        val technicalMessage = buildString {
            appendLine("Провайдер: ${source.authority ?: "невідомий"}")
            appendLine("Фактичний Document ID: $documentId")
            appendLine()
            appendLine(ArchiveSourceGuard.summary(matches, project.id))
            appendLine()
            append(
                "Це пошук серед зареєстрованих томів усіх проєктів, " +
                    "а не серед усіх файлів телефона; номер NT — не хеш.",
            )
        }
        val detailsText = Ui.textView(
            this, technicalMessage, sizeSp = 13f, color = Ui.muted,
        ).apply {
            visibility = if (archiveSourceDetailsExpanded) View.VISIBLE else View.GONE
            setPadding(0, Ui.dp(this@ProjectActivity, 8), 0, 0)
        }
        val detailsToggle = Ui.textView(
            this,
            if (archiveSourceDetailsExpanded) "Сховати деталі ▲" else "Технічні деталі ▼",
            sizeSp = 14f,
            color = Ui.accent,
        ).apply {
            isClickable = true
            isFocusable = true
            setPadding(0, Ui.dp(this@ProjectActivity, 12), 0, Ui.dp(this@ProjectActivity, 4))
            setOnClickListener {
                archiveSourceDetailsExpanded = !archiveSourceDetailsExpanded
                detailsText.visibility =
                    if (archiveSourceDetailsExpanded) View.VISIBLE else View.GONE
                text = if (archiveSourceDetailsExpanded) "Сховати деталі ▲"
                    else "Технічні деталі ▼"
            }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Ui.textView(this@ProjectActivity, previewMessage, sizeSp = 16f))
            addView(detailsToggle)
            addView(detailsText)
        }
        val scroll = ScrollView(this).apply {
            setPadding(
                Ui.dp(this@ProjectActivity, 22),
                Ui.dp(this@ProjectActivity, 8),
                Ui.dp(this@ProjectActivity, 22),
                Ui.dp(this@ProjectActivity, 8),
            )
            clipToPadding = false
            addView(content)
        }
        archiveSourcePreflightOpen = true
        val dialog = AlertDialog.Builder(this)
            .setTitle("Підтвердь джерело архіву")
            .setView(scroll)
            .setPositiveButton("Продовжити") { _, _ ->
                archiveSourcePreflightOpen = false
                if (pendingNativeSourceUri == source.toString() &&
                    pendingNativeSourceName == sourceName
                ) {
                    openNativeRdpkgDestinationPicker(sourceName)
                }
            }
            .setNegativeButton("Скасувати") { _, _ ->
                clearPendingArchiveSource()
            }
            .create()
        dialog.setOnCancelListener { clearPendingArchiveSource() }
        dialog.show()
        DialogUi.apply(dialog, DialogRole.CONFIRM)
    }

    private fun handleNativeRdpkgDestinationResult(
        resultCode: Int,
        data: Intent?,
    ) {
        val sourceUri =
            pendingNativeSourceUri
        val sourceName =
            pendingNativeSourceName
                ?: "Renault"
        val requestId =
            pendingNativeRequestId
        val sourceKind =
            pendingNativeSourceKind

        pendingNativeSourceUri =
            null
        pendingNativeSourceName =
            null
        pendingNativeSourceKind =
            NativeRdpkgSourceKind.RAW_TREE
        pendingNativeRequestId =
            null

        if (
            resultCode !=
            RESULT_OK
        ) {
            statusText.text =
                "Створення .rdpkg скасовано до запуску."
            return
        }

        if (
            sourceUri.isNullOrBlank() ||
            requestId.isNullOrBlank()
        ) {
            statusText.text =
                "Втрачено source/session. Вибери джерело ще раз."
            return
        }

        val destination =
            data?.data

        if (
            destination ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу для нового .rdpkg."
            return
        }

        persistReadWritePermission(
            uri =
                destination,
            returnedFlags =
                data.flags,
        )

        if (sourceKind == NativeRdpkgSourceKind.ARCHIVE_FILE) {
            ArchiveSourceGuard.inputError(sourceName, project)?.let { problem ->
                showArchiveSourceRejected(problem)
                return
            }
        }
        val parsedSourceUri =
            Uri.parse(
                sourceUri,
            )

        if (
            sourceKind ==
            NativeRdpkgSourceKind.RAW_TREE &&
            destinationIsInsideSourceTree(
                sourceTreeUri =
                    parsedSourceUri,
                destinationUri =
                    destination,
            )
        ) {
            deleteCreatedDestination(
                destination,
            )

            statusText.text =
                "Не зберігай .rdpkg всередині raw source. " +
                    "Вибери іншу папку для готового пакета."
            return
        }

        val existingRun =
            nativeRunStore.load()

        if (
            existingRun.isActive
        ) {
            statusText.text =
                "Інша native .rdpkg підготовка вже виконується або очікує вибір томів."
            refreshNativeRunState()
            return
        }

        nativeRunStore.clearFinished()

        NativeRdpkgPreparationService.start(
            context =
                this,
            request =
                NativeRdpkgPreparationService
                    .StartRequest(
                        requestId =
                            requestId,
                        sourceTreeUri =
                            sourceUri,
                        sourceName =
                            sourceName,
                        sourceKind =
                            sourceKind,
                        destinationUri =
                            destination.toString(),
                        projectId =
                            project.id,
                        projectTitle =
                            project.title,
                        model =
                            project.model,
                    ),
        )

        statusText.text =
            if (
                sourceKind ==
                NativeRdpkgSourceKind.ARCHIVE_FILE
            ) {
                "Запускаю архів → raw → .rdpkg…"
            } else {
                "Запускаю Kotlin-native raw → .rdpkg…"
            }

        refreshNativeRunState()
    }

    private fun destinationIsInsideSourceTree(
        sourceTreeUri: Uri,
        destinationUri: Uri,
    ): Boolean {
        if (
            sourceTreeUri.authority !=
                destinationUri.authority
        ) {
            return false
        }

        return runCatching {
            val sourceDocumentId =
                DocumentsContract
                    .getTreeDocumentId(
                        sourceTreeUri,
                    )
                    .trimEnd(
                        '/',
                    )
            val destinationDocumentId =
                DocumentsContract
                    .getDocumentId(
                        destinationUri,
                    )

            destinationDocumentId ==
                sourceDocumentId ||
                destinationDocumentId.startsWith(
                    sourceDocumentId +
                        "/",
                )
        }.getOrDefault(
            false,
        )
    }

    private fun deleteCreatedDestination(
        uri: Uri,
    ) {
        val deleted =
            runCatching {
                contentResolver.delete(
                    uri,
                    null,
                    null,
                ) >
                    0
            }.getOrDefault(
                false,
            )

        if (
            deleted ||
            !DocumentsContract.isDocumentUri(
                this,
                uri,
            )
        ) {
            return
        }

        runCatching {
            DocumentsContract.deleteDocument(
                contentResolver,
                uri,
            )
        }
    }

    private fun startNativeRdpkgFlow() {
        val state =
            nativeRunStore.load()

        if (state.isRunning) {
            refreshNativeRunState()
            return
        }

        nativeRunStore.clearFinished()

        val picker =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE,
            ).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_NATIVE_RDPKG_SOURCE,
        )
    }

    private fun startNativeArchiveRdpkgFlow() {
        val state =
            nativeRunStore.load()

        if (
            state.isActive
        ) {
            refreshNativeRunState()
            return
        }

        val picker =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT,
            ).apply {
                addCategory(
                    Intent.CATEGORY_OPENABLE,
                )
                type =
                    "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "application/zip",
                        "application/x-7z-compressed",
                        "application/vnd.rar",
                        "application/x-rar-compressed",
                    ),
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_NATIVE_RDPKG_ARCHIVE_SOURCE,
        )
    }

    private fun openNativeRdpkgDestinationPicker(
        sourceName: String,
    ) {
        if (
            pendingNativeSourceKind ==
            NativeRdpkgSourceKind.ARCHIVE_FILE
        ) {
            val picker =
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT_TREE,
                ).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                    )
                    addFlags(
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                    )
                }

            @Suppress(
                "DEPRECATION",
            )
            startActivityForResult(
                picker,
                REQUEST_NATIVE_RDPKG_DESTINATION,
            )
            return
        }

        val defaultName =
            RenaultVolumeIdentity
                .canonicalFileName(
                    model =
                        project.model,
                    metadata =
                        RenaultVolumeIdentity.parse(
                            sourceName,
                        ),
                    fallbackId =
                        sourceName,
                )

        val picker =
            Intent(
                Intent.ACTION_CREATE_DOCUMENT,
            ).apply {
                addCategory(
                    Intent.CATEGORY_OPENABLE,
                )
                type =
                    "application/octet-stream"
                putExtra(
                    Intent.EXTRA_TITLE,
                    defaultName,
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_NATIVE_RDPKG_DESTINATION,
        )
    }

    private fun refreshNativeRunState() {
        if (
            !::nativeRunStore.isInitialized ||
            !::statusText.isInitialized ||
            !::project.isInitialized
        ) {
            return
        }

        val state =
            nativeRunStore.load()

        if (
            state.projectId !=
            project.id
        ) {
            hideNativeTerminalStatus()
            if (::operationStatus.isInitialized) operationStatus.hide()
            return
        }

        if (
            state.isWaitingForSelection
        ) {
            hideNativeTerminalStatus()

            if (
                ::operationStatus.isInitialized
            ) {
                operationStatus.showRunning(
                    title =
                        "Архів · вибір томів",
                    detail =
                        state.message.ifBlank {
                            "Вибери томи для створення .rdpkg."
                        },
                    subject =
                        state.sourceName
                            ?: "Renault архів",
                )
            }

            statusText.text =
                DEFAULT_STATUS_TEXT

            showArchiveVolumeChooser(
                state,
            )
            return
        }

        if (
            state.isRunning
        ) {
            hideNativeTerminalStatus()
            if (::operationStatus.isInitialized) {
                val operationSubject =
                    state.destinationUri
                        ?.let {
                            uriText ->
                            if (
                                state.sourceKind ==
                                NativeRdpkgSourceKind.ARCHIVE_FILE
                            ) {
                                DocumentFile
                                    .fromTreeUri(
                                        this,
                                        Uri.parse(
                                            uriText,
                                        ),
                                    )
                                    ?.name
                            } else {
                                DocumentFile
                                    .fromSingleUri(
                                        this,
                                        Uri.parse(
                                            uriText,
                                        ),
                                    )
                                    ?.name
                            }
                        }
                        ?: "Renault Docs"

                operationStatus.showRunning(
                    title = "Створення .rdpkg",
                    detail =
                        state.message
                            .ifBlank {
                                "Готую…"
                            },
                    current =
                        state.progressCurrent
                            .takeIf {
                                state.progressTotal >
                                    0
                            },
                    total =
                        state.progressTotal
                            .takeIf {
                                it >
                                    0
                            },
                    onCancel =
                        if (state.phase == NativeRdpkgRunPhase.PREPARING) {
                            {
                                NativeRdpkgPreparationService.requestCancel(this)
                                operationStatus.showRunning(
                                    "Створення .rdpkg",
                                    "Скасовую після поточного безпечного кроку…",
                                )
                            }
                        } else {
                            null
                        },
                    subject =
                        operationSubject,
                )
            }

            val serviceActive =
                NativeRdpkgPreparationService
                    .isActive()
            val withinStartupGrace =
                System.currentTimeMillis() -
                    state.startedAtMs <
                    NATIVE_RUN_STARTUP_GRACE_MS

            if (
                !serviceActive &&
                !withinStartupGrace
            ) {
                val recoveryStarted =
                    NativeRdpkgPreparationService
                        .resumePersisted(
                            context =
                                this,
                            state =
                                state,
                            project =
                                project,
                        )

                if (
                    recoveryStarted
                ) {
                    statusText.text =
                        "Відновлюю створення .rdpkg…"
                    return
                }

                nativeRunStore.fail(
                    "Попередню native .rdpkg підготовку було перервано. " +
                        "Автоматичне відновлення не вдалося. Source не змінено.",
                )
                refreshNativeRunState()
                return
            }

            statusText.text = DEFAULT_STATUS_TEXT
            return
        }

        if (
            !state.isTerminal ||
            state.finishedAtMs <=
                0L
        ) {
            hideNativeTerminalStatus()
            return
        }

        if (
            state.isTerminalDismissed
        ) {
            hideNativeTerminalStatus()
            if (::operationStatus.isInitialized) operationStatus.hide()
            return
        }

        if (
            state.finishedAtMs >
                lastShownNativeFinishedAt
        ) {
            lastShownNativeFinishedAt =
                state.finishedAtMs

            statusText.text =
                DEFAULT_STATUS_TEXT

            if (
                state.phase ==
                NativeRdpkgRunPhase.COMPLETE
            ) {
                render()
            }
        }

        showNativeTerminalStatus(
            state,
        )
    }

    private fun showArchiveVolumeChooser(
        state: NativeRdpkgRunState,
    ) {
        if (
            archiveChooserDialog
                ?.isShowing ==
            true
        ) {
            return
        }

        val candidates =
            state.archiveCandidates

        if (
            candidates.isEmpty()
        ) {
            nativeRunStore.fail(
                "Втрачено список томів архіву."
            )
            return
        }

        val content =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(
                        this@ProjectActivity,
                        18,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        10,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        18,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        8,
                    ),
                )
            }

        content.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "Знайдено томів: " +
                        candidates.size +
                        ". Встановлені томи пропускаються. " +
                        "Вибери один або кілька нових.",
                sizeSp =
                    15f,
                color =
                    Ui.text,
            ),
        )

        val list =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        candidates.forEach {
            candidate ->
            val suffix =
                when {
                    candidate.installed ->
                        "  ✓ Уже встановлено"

                    candidate.possibleDuplicate ->
                        "  ⚠ Схожий том уже є"

                    else ->
                        ""
                }

            list.addView(
                CheckBox(
                    this,
                ).apply {
                    text =
                        candidate.label +
                            suffix
                    isChecked =
                        candidate.selected &&
                            !candidate.installed
                    isEnabled =
                        !candidate.installed
                    setOnCheckedChangeListener {
                            _,
                            checked ->
                        nativeRunStore
                            .updateArchiveCandidateSelection(
                                relativePath =
                                    candidate.relativePath,
                                selected =
                                    checked,
                            )
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        content.addView(
            ScrollView(
                this,
            ).apply {
                addView(
                    list,
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val dialog =
            AlertDialog.Builder(
                this,
            )
                .setTitle(
                    "Томи в архіві",
                )
                .setView(
                    content,
                )
                .setPositiveButton(
                    "Створити вибрані",
                    null,
                )
                .setNegativeButton(
                    "Скасувати",
                ) {
                        _,
                        _ ->
                    NativeRdpkgPreparationService
                        .requestCancel(
                            this,
                        )
                }
                .setCancelable(
                    false,
                )
                .create()

        archiveChooserDialog =
            dialog

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE,
            ).setOnClickListener {
                val latest =
                    nativeRunStore
                        .load()
                val selected =
                    latest.archiveCandidates
                        .count {
                            it.selected &&
                                !it.installed
                        }

                if (
                    selected <=
                    0
                ) {
                    Toast.makeText(
                        this,
                        "Вибери хоча б один новий том.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }

                dialog.dismiss()
                archiveChooserDialog =
                    null

                val started =
                    NativeRdpkgPreparationService
                        .resumeArchiveSelection(
                            context =
                                this,
                        )

                if (
                    !started
                ) {
                    nativeRunStore.fail(
                        "Не вдалося продовжити вибрані томи архіву."
                    )
                }
            }
        }

        dialog.setOnDismissListener {
            if (
                archiveChooserDialog ==
                dialog
            ) {
                archiveChooserDialog =
                    null
            }
        }

        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun showNativeTerminalStatus(
        state: NativeRdpkgRunState,
    ) {
        if (!::operationStatus.isInitialized) {
            return
        }

        val detail =
            when (state.phase) {
                NativeRdpkgRunPhase.COMPLETE -> state.message
                NativeRdpkgRunPhase.ALREADY_PRESENT -> state.message
                NativeRdpkgRunPhase.CANCELLED ->
                    state.message.ifBlank { "Підготовку .rdpkg скасовано." }
                NativeRdpkgRunPhase.FAILED ->
                    "Не вдалося створити .rdpkg: " + state.message
                else -> state.message
            }

        nativeTerminalStatusRow.visibility = View.GONE
        if (::operationStatus.isInitialized) {
            val title =
                when (state.phase) {
                    NativeRdpkgRunPhase.COMPLETE -> "Створення .rdpkg завершено"
                    NativeRdpkgRunPhase.ALREADY_PRESENT -> "Том уже є"
                    NativeRdpkgRunPhase.CANCELLED -> "Створення .rdpkg скасовано"
                    NativeRdpkgRunPhase.FAILED -> "Створення .rdpkg · помилка"
                    else -> "Створення .rdpkg"
                }
            operationStatus.showTerminal(title, detail) {
                nativeRunStore.dismissTerminal(state.finishedAtMs)
                operationStatus.hide()
            }
        }
    }

    private fun hideNativeTerminalStatus() {
        if (
            ::nativeTerminalStatusRow.isInitialized
        ) {
            nativeTerminalStatusRow.visibility =
                View.GONE
        }
    }

    private fun handleFolderResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (
            resultCode !=
            RESULT_OK
        ) {
            statusText.text =
                "Додавання папки скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу вибраної папки."
            return
        }

        persistReadPermission(
            uri =
                uri,
            returnedFlags =
                data.flags,
        )

        val allowOverride =
            pendingManualImport

        pendingManualImport =
            false

        PreparedVolumeReader.readAll(
            context =
                this,
            treeUri =
                uri,
        )
            .onSuccess {
                volumes ->
                importPreparedVolumes(
                    volumes =
                        volumes,
                    allowOverride =
                        allowOverride,
                )
            }
            .onFailure {
                error ->
                statusText.text =
                    "Не вдалося додати том: " +
                        (
                            error.message
                                ?: "невідома помилка"
                        )
            }
    }

    private fun repairSavedVolumeMetadata() {
        Thread {
            val repaired =
                store
                    .repairIncompleteVolumeMetadata(
                        project.id,
                    )

            if (
                repaired <=
                0
            ) {
                return@Thread
            }

            runOnUiThread {
                if (
                    !isFinishing &&
                    !isDestroyed
                ) {
                    render()
                }
            }
        }.start()
    }

    private fun buildContent():
        View {
        window.statusBarColor =
            Ui.background
        window.navigationBarColor =
            Ui.background

        val root =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(
                    Ui.background,
                )
            }

        Ui.applySystemInsets(
            root,
        )

        val topBar =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        topBar.addView(
            ImageButton(
                this,
            ).apply {
                setImageResource(
                    R.drawable.ic_arrow_back,
                )
                contentDescription =
                    "Назад"
                setBackgroundColor(
                    android.graphics.Color.TRANSPARENT,
                )
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                Ui.dp(
                    this,
                    48,
                ),
                Ui.dp(
                    this,
                    48,
                ),
            ),
        )

        topBar.addView(
            Ui.textView(
                context =
                    this,
                value =
                    project.title,
                sizeSp =
                    26f,
                color =
                    Ui.entityTitle,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    Ui.dp(
                        this@ProjectActivity,
                        8,
                    ),
                    0,
                    0,
                    0,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        topBar.addView(
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_PROJECT,
                )
            },
            LinearLayout.LayoutParams(
                Ui.dp(
                    this,
                    44,
                ),
                Ui.dp(
                    this,
                    44,
                ),
            ),
        )

        root.addView(
            topBar,
        )

        countText =
            Ui.textView(
                context =
                    this,
                value =
                    "",
                sizeSp =
                    15f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ProjectActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@ProjectActivity,
                        12,
                    ),
                )
            }

        root.addView(
            countText,
        )

        root.addView(
            buildAddPanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Native preparation terminal state is rendered by operationStatus.
        nativeTerminalStatusText =
            Ui.textView(this, "", 14f, Ui.text)
        nativeTerminalStatusRow =
            LinearLayout(this).apply {
                visibility = View.GONE
            }

        projectScroll =
            ScrollView(
                this,
            ).apply {
                isFillViewport =
                    true
            }

        volumeContainer =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.TOP
            }

        projectScroll.addView(
            volumeContainer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            projectScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ProjectActivity,
                        10,
                    )
            },
        )

        return root
    }

    private fun buildAddPanel():
        View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@ProjectActivity,
                    fill =
                        Ui.surfaceAlt,
                    stroke =
                        Ui.accent,
                )
            setPadding(
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    8,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    8,
                ),
            )

            val titleRow =
                LinearLayout(
                    this@ProjectActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    isClickable =
                        true
                    isFocusable =
                        true
                    setOnClickListener {
                        toggleAddPanel()
                    }
                }

            titleRow.addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        "Додати",
                    sizeSp =
                        18f,
                    color =
                        Ui.accent,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            addPanelPinButton =
                addPanelHeaderButton(
                    label =
                        "📌",
                    contentDescription =
                        "Закріпити панель Додати",
                ) {
                    // Landscape does not overwrite portrait's saved pin choice.
                    if (!isLandscapeLayout()) {
                        addPanelPinned =
                            !addPanelPinned
                        settings.projectAddPanelPinned =
                            addPanelPinned
                        if (addPanelPinned) {
                            addPanelExpanded =
                                true
                        }
                        updateAddPanelUi()
                    }
                }

            titleRow.addView(
                addPanelPinButton,
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@ProjectActivity,
                            6,
                        )
                },
            )

            addPanelToggleButton =
                addPanelHeaderButton(
                    label =
                        "▼",
                    contentDescription =
                        "Розгорнути або згорнути панель Додати",
                ) {
                    toggleAddPanel()
                }

            titleRow.addView(
                addPanelToggleButton,
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@ProjectActivity,
                            6,
                        )
                },
            )

            titleRow.addView(
                Ui.helpButton(
                    context =
                        this@ProjectActivity,
                ) {
                    helpDialogs.show(
                        HELP_ADD,
                    )
                },
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                    Ui.dp(
                        this@ProjectActivity,
                        40,
                    ),
                ),
            )

            addView(
                titleRow,
            )

            addPanelBody =
                LinearLayout(
                    this@ProjectActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            val choices =
                LinearLayout(
                    this@ProjectActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    setPadding(
                        0,
                        Ui.dp(
                            this@ProjectActivity,
                            6,
                        ),
                        0,
                        0,
                    )
                }

            choices.addView(
                addChoiceButton(
                    label =
                        "Авто",
                    subtitle =
                        ".rdpkg · один том",
                    primary =
                        true,
                ) {
                    openPackagePicker()
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@ProjectActivity,
                            5,
                        )
                },
            )

            choices.addView(
                addChoiceButton(
                    label =
                        "Вручну",
                    subtitle =
                        "Папка / SAF",
                    primary =
                        false,
                ) {
                    openVolumePicker(
                        manual =
                            true,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@ProjectActivity,
                            5,
                        )
                },
            )

            addPanelBody.addView(
                choices,
            )

            addPanelBody.addView(
                buildProjectActionCard(
                    title =
                        "Створити .rdpkg з raw",
                    subtitle =
                        "Kotlin · без Python/Termux · без *_android",
                    primary =
                        true,
                    helpId =
                        HELP_RAW,
                ) {
                    startNativeRdpkgFlow()
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        Ui.dp(
                            this@ProjectActivity,
                            10,
                        )
                },
            )

            addPanelBody.addView(
                buildProjectActionCard(
                    title =
                        "Створити .rdpkg з архіву",
                    subtitle =
                        "ZIP · 7Z · RAR · без ручної розпаковки",
                    primary =
                        false,
                    helpId =
                        HELP_ARCHIVE,
                ) {
                    startNativeArchiveRdpkgFlow()
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        Ui.dp(
                            this@ProjectActivity,
                            8,
                        )
                },
            )

            // Read-only access to the last native source; no task or cleanup.
            addPanelBody.addView(
                Ui.textView(
                    context = this@ProjectActivity,
                    value = "Джерело останньої .rdpkg · діагностика",
                    sizeSp = 13f,
                    color = Ui.accent,
                ).apply {
                    gravity = Gravity.CENTER
                    minHeight = Ui.dp(this@ProjectActivity, 44)
                    background = Ui.roundedBackground(
                        context = this@ProjectActivity,
                        fill = Ui.surface,
                        stroke = Ui.border,
                        radiusDp = 12,
                    )
                    contentDescription = "Діагностика останнього джерела, тільки читання"
                    setOnClickListener { showLastNativeSourceDiagnostics() }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@ProjectActivity, 8) },
            )

            statusText =
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        DEFAULT_STATUS_TEXT,
                    sizeSp =
                        13f,
                    color =
                        Ui.statusWarmText,
                ).apply {
                    visibility =
                        View.GONE
                    background =
                        Ui.roundedBackground(
                            context =
                                this@ProjectActivity,
                            fill =
                                Ui.statusWarmFill,
                            stroke =
                                Ui.statusWarmBorder,
                            radiusDp =
                                10,
                        )
                    setPadding(
                        Ui.dp(
                            this@ProjectActivity,
                            12,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            8,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            12,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            8,
                        ),
                    )
                    addTextChangedListener(
                        object : TextWatcher {
                            override fun beforeTextChanged(
                                value: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int,
                            ) = Unit

                            override fun onTextChanged(
                                value: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int,
                            ) {
                                visibility =
                                    if (
                                        value
                                            ?.toString()
                                            .orEmpty()
                                            .isBlank()
                                    ) {
                                        View.GONE
                                    } else {
                                        View.VISIBLE
                                    }
                            }

                            override fun afterTextChanged(
                                value: Editable?,
                            ) = Unit
                        },
                    )
                }

            addPanelBody.addView(
                statusText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        Ui.dp(
                            this@ProjectActivity,
                            10,
                        )
                },
            )

            addView(
                addPanelBody,
            )

            // The status is inside Add but outside its collapsible actions:
            // current progress, cancel and terminal results remain reachable.
            operationStatus =
                OperationStatusView(this@ProjectActivity).apply {
                    useProjectCompactLayout(restoredStatusDetailsExpanded)
                }
            addView(
                operationStatus,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = Ui.dp(this@ProjectActivity, 8)
                },
            )

            post {
                updateAddPanelUi()
            }
        }

    private fun addPanelHeaderButton(
        label: String,
        contentDescription: String,
        onClick: () -> Unit,
    ): TextView =
        Ui.textView(
            context =
                this,
            value =
                label,
            sizeSp =
                17f,
            color =
                Ui.accent,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            gravity =
                Gravity.CENTER
            this.contentDescription =
                contentDescription
            isClickable =
                true
            isFocusable =
                true
            background =
                Ui.roundedBackground(
                    context =
                        this@ProjectActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp =
                        12,
                )
            setOnClickListener {
                onClick()
            }
        }

    private fun isLandscapeLayout(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun toggleAddPanel() {
        if (isLandscapeLayout()) {
            // A user can temporarily open Add in landscape without
            // changing the portrait pin or expanded state.
            addPanelLandscapeExpanded =
                !addPanelLandscapeExpanded
        } else if (!addPanelPinned) {
            addPanelExpanded =
                !addPanelExpanded
        }
        updateAddPanelUi()
    }

    private fun updateAddPanelUi() {
        if (!::addPanelBody.isInitialized) {
            return
        }

        val landscape = isLandscapeLayout()
        val pinnedHere = addPanelPinned && !landscape
        val expandedHere =
            if (landscape) {
                addPanelLandscapeExpanded
            } else {
                addPanelExpanded
            }

        addPanelBody.visibility =
            if (expandedHere) View.VISIBLE else View.GONE

        // Keep the exact same original Android emoji. The landscape pin
        // appears unpinned, while the saved portrait preference survives.
        addPanelPinButton.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            if (pinnedHere) {
                null
            } else {
                monochromePinPaint
            },
        )
        addPanelPinButton.alpha =
            1f
        addPanelPinButton.isEnabled =
            !landscape
        addPanelPinButton.isClickable =
            !landscape
        addPanelPinButton.isFocusable =
            !landscape
        addPanelPinButton.contentDescription =
            if (landscape) {
                "Закріплення тимчасово недоступне в альбомному режимі"
            } else if (pinnedHere) {
                "Відкріпити панель Додати"
            } else {
                "Закріпити панель Додати"
            }

        addPanelToggleButton.text =
            if (expandedHere) "▲" else "▼"
        addPanelToggleButton.isClickable =
            !pinnedHere
        addPanelToggleButton.isFocusable =
            !pinnedHere
        addPanelToggleButton.alpha =
            if (pinnedHere) 0.45f else 1f
        addPanelToggleButton.contentDescription =
            if (expandedHere) {
                "Згорнути панель Додати"
            } else {
                "Розгорнути панель Додати"
            }
    }

    private fun addChoiceButton(
        label: String,
        subtitle: String,
        primary: Boolean,
        onClick: () -> Unit,
    ): View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER
            isClickable =
                true
            isFocusable =
                true
            minimumHeight =
                Ui.dp(
                    this@ProjectActivity,
                    76,
                )
            background =
                Ui.roundedBackground(
                    context =
                        this@ProjectActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        if (
                            primary
                        ) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                    radiusDp =
                        12,
                )
            setPadding(
                Ui.dp(
                    this@ProjectActivity,
                    10,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    8,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    10,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    8,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        label,
                    sizeSp =
                        16f,
                    color =
                        if (
                            primary
                        ) {
                            Ui.accent
                        } else {
                            Ui.text
                        },
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                    gravity =
                        Gravity.CENTER
                },
            )

            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        subtitle,
                    sizeSp =
                        Ui.actionSubtitleSp,
                    color =
                        Ui.muted,
                ).apply {
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        0,
                        Ui.dp(
                            this@ProjectActivity,
                            2,
                        ),
                        0,
                        0,
                    )
                },
            )

            setOnClickListener {
                onClick()
            }
        }

    private fun buildProjectActionCard(
        title: String,
        subtitle: String,
        primary: Boolean,
        helpId: String? = null,
        onClick: () -> Unit,
    ): View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            isClickable =
                true
            isFocusable =
                true
            minimumHeight =
                Ui.dp(
                    this@ProjectActivity,
                    72,
                )
            background =
                Ui.roundedBackground(
                    context =
                        this@ProjectActivity,
                    fill =
                        if (
                            primary
                        ) {
                            Ui.surfaceAlt
                        } else {
                            Ui.surface
                        },
                    stroke =
                        if (
                            primary
                        ) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                )
            setPadding(
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    12,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    12,
                ),
            )

            val titleRow =
                LinearLayout(
                    this@ProjectActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            titleRow.addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        title,
                    sizeSp =
                        16f,
                    color =
                        if (
                            primary
                        ) {
                            Ui.accent
                        } else {
                            Ui.text
                        },
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            if (
                helpId !=
                null
            ) {
                titleRow.addView(
                    Ui.helpButton(
                        context =
                            this@ProjectActivity,
                    ) {
                        helpDialogs.show(
                            helpId,
                        )
                    },
                    LinearLayout.LayoutParams(
                        Ui.dp(
                            this@ProjectActivity,
                            40,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            40,
                        ),
                    ),
                )
            }

            addView(
                titleRow,
            )

            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        subtitle,
                    sizeSp =
                        Ui.actionSubtitleSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@ProjectActivity,
                            3,
                        ),
                        0,
                        0,
                    )
                },
            )

            setOnClickListener {
                onClick()
            }
        }

    private fun restoreProjectScroll() {
        if (
            restoredScrollY <=
            0 ||
            !::projectScroll.isInitialized
        ) {
            return
        }

        val scrollY =
            restoredScrollY
        restoredScrollY =
            0

        projectScroll.post {
            projectScroll.scrollTo(
                0,
                scrollY,
            )
        }
    }

    private fun render() {
        val volumes =
            store.volumes(
                project.id,
            )

        countText.text =
            project.model +
                " · томів: " +
                volumes.size

        volumeContainer.removeAllViews()

        if (
            volumes.isEmpty()
        ) {
            volumeContainer.addView(
                Ui.textView(
                    context =
                        this,
                    value =
                        "Порожній · додай том",
                    sizeSp =
                        15f,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        Ui.dp(
                            this@ProjectActivity,
                            16,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            18,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            16,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            18,
                        ),
                    )
                    background =
                        Ui.roundedBackground(
                            context =
                                this@ProjectActivity,
                            fill =
                                Ui.surface,
                        )
                },
            )
            return
        }

        volumes.forEach {
            volume ->
            volumeContainer.addView(
                buildVolumeCard(
                    volume,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@ProjectActivity,
                            10,
                        )
                },
            )
        }
    }

    private fun buildVolumeCard(
        volume: ProjectVolumeRecord,
    ): View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            isClickable =
                true
            isFocusable =
                true
            background =
                Ui.roundedBackground(
                    context =
                        this@ProjectActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                )
            setPadding(
                Ui.dp(
                    this@ProjectActivity,
                    16,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
                Ui.dp(
                    this@ProjectActivity,
                    14,
                ),
            )

            val textColumn =
                LinearLayout(
                    this@ProjectActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            val primaryTitle =
                listOfNotNull(
                    volume.documentCode
                        ?.takeIf {
                            it.isNotBlank()
                        },
                    volume.date
                        ?.takeIf {
                            it.isNotBlank()
                        },
                )
                    .joinToString(
                        " · ",
                    )
                    .ifBlank {
                        volume.title
                    }

            textColumn.addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        primaryTitle,
                    sizeSp =
                        19f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                },
            )

            val vehicleCodes =
                volume.vehicleCodes
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(
                        " · ",
                    )

            if (
                vehicleCodes.isNotBlank()
            ) {
                textColumn.addView(
                    Ui.textView(
                        context =
                            this@ProjectActivity,
                        value =
                            vehicleCodes,
                        sizeSp =
                            14f,
                        color =
                            Ui.muted,
                    ).apply {
                        setPadding(
                            0,
                            Ui.dp(
                                this@ProjectActivity,
                                5,
                            ),
                            0,
                            0,
                        )
                    },
                )
            }

            val documentDescriptor =
                RenaultVolumeIdentityMetadata(
                    documentType =
                        volume.documentType,
                    documentVersion =
                        volume.documentVersion,
                )
                    .documentDescriptor()

            val documentLine =
                listOfNotNull(
                    volume.region
                        ?.takeIf {
                            it.isNotBlank()
                        },
                    documentDescriptor,
                )
                    .joinToString(
                        " · ",
                    )

            if (
                documentLine.isNotBlank()
            ) {
                textColumn.addView(
                    Ui.textView(
                        context =
                            this@ProjectActivity,
                        value =
                            documentLine,
                        sizeSp =
                            14f,
                        color =
                            Ui.accent,
                    ).apply {
                        setPadding(
                            0,
                            Ui.dp(
                                this@ProjectActivity,
                                4,
                            ),
                            0,
                            0,
                        )
                    },
                )
            }

            addView(
                textColumn,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            if (PreparedShareStore.hasVolumeFile(this@ProjectActivity, project, volume)) {
                addView(
                    Ui.textView(
                        context = this@ProjectActivity,
                        value = "⇧",
                        sizeSp = 20f,
                        color = Ui.accent,
                    ).apply {
                        contentDescription = "Підготовлений .rdpkg готовий для передачі"
                        gravity = Gravity.CENTER
                        setPadding(
                            Ui.dp(this@ProjectActivity, 8),
                            0,
                            Ui.dp(this@ProjectActivity, 4),
                            0,
                        )
                    }
                )
            }



            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        "⋮",
                    sizeSp =
                        27f,
                    color =
                        Ui.muted,
                ).apply {
                    contentDescription =
                        "Дії тому"
                    isClickable =
                        true
                    isFocusable =
                        true
                    setPadding(
                        Ui.dp(
                            this@ProjectActivity,
                            12,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            6,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            12,
                        ),
                        Ui.dp(
                            this@ProjectActivity,
                            6,
                        ),
                    )
                    setOnClickListener {
                        showVolumeActions(
                            volume,
                        )
                    }
                },
            )

            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        "›",
                    sizeSp =
                        28f,
                    color =
                        Ui.muted,
                ),
            )

            setOnClickListener {
                openVolume(
                    volume,
                )
            }

            setOnLongClickListener {
                showVolumeActions(
                    volume,
                )
                true
            }
        }

    private fun showVolumeActions(
        volume: ProjectVolumeRecord,
    ) {
        val label =
            listOfNotNull(
                volume.documentCode,
                volume.date,
            ).joinToString(" · ").ifBlank {
                volume.title
            }

        setProjectDialogState(
            kind = DIALOG_VOLUME_ACTIONS,
            volume = volume,
        )

        val actions =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(this@ProjectActivity, 18),
                    Ui.dp(this@ProjectActivity, 8),
                    Ui.dp(this@ProjectActivity, 18),
                    Ui.dp(this@ProjectActivity, 8),
                )
            }

        lateinit var dialog: AlertDialog

        fun addAction(
            title: String,
            danger: Boolean = false,
            action: () -> Unit,
        ) {
            actions.addView(
                Ui.textView(
                    context = this,
                    value = "› " + title,
                    sizeSp = 18f,
                    color = if (danger) Ui.danger else Ui.text,
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    background =
                        Ui.roundedBackground(
                            context = this@ProjectActivity,
                            fill = Ui.surfaceAlt,
                            stroke = if (danger) Ui.danger else Ui.accent,
                            radiusDp = 11,
                        )
                    setPadding(
                        Ui.dp(this@ProjectActivity, 14),
                        Ui.dp(this@ProjectActivity, 12),
                        Ui.dp(this@ProjectActivity, 14),
                        Ui.dp(this@ProjectActivity, 12),
                    )
                    minHeight = Ui.dp(this@ProjectActivity, 52)
                    setOnClickListener {
                        dialog.dismiss()
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = Ui.dp(this@ProjectActivity, 10)
                },
            )
        }

        addAction("Експортувати .rdpkg") {
            clearProjectDialogState(DIALOG_VOLUME_ACTIONS)
            startRdpkgExport(volume)
        }
        addAction("Поділитися томом") {
            clearProjectDialogState(DIALOG_VOLUME_ACTIONS)
            shareRdpkg(volume)
        }
        val prepared =
            PreparedShareStore.existingVolumeFile(
                this,
                project,
                volume,
            )
        if (prepared != null) {
            addAction("Поділитися підготовленим .rdpkg") {
                clearProjectDialogState(DIALOG_VOLUME_ACTIONS)
                sharePreparedRdpkg(
                    volume,
                    prepared,
                )
            }
            addAction(
                title = "Видалити підготовлений .rdpkg",
                danger = true,
            ) {
                clearProjectDialogState(DIALOG_VOLUME_ACTIONS)
                activeProjectDialogParentKind = DIALOG_VOLUME_ACTIONS
                confirmDeletePreparedVolume(volume)
            }
        }
        addAction("Перемістити в інший проєкт") {
            showMoveVolumeDialog(volume)
        }
        addAction(
            title = "Видалити з проєкту",
            danger = true,
        ) {
            clearProjectDialogState(
                DIALOG_VOLUME_ACTIONS,
            )
            activeProjectDialogParentKind =
                DIALOG_VOLUME_ACTIONS
            confirmRemoveVolume(
                volume,
            )
        }
        addAction("Скасувати") {
            clearProjectDialogState(DIALOG_VOLUME_ACTIONS)
        }

        dialog =
            AlertDialog.Builder(this)
                .setTitle(label)
                .setView(actions)
                .create()

        trackProjectDialog(
            dialog = dialog,
            kind = DIALOG_VOLUME_ACTIONS,
            volumeId = volume.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.CHOICE,
        )
    }

    private fun showMoveVolumeDialog(
        volume: ProjectVolumeRecord,
    ) {
        val targets =
            store.projects()
                .filter { it.id != project.id }

        if (targets.isEmpty()) {
            clearProjectDialogState()
            statusText.text =
                "Немає іншого проєкту, куди можна перемістити том."
            return
        }

        setProjectDialogState(
            kind = DIALOG_MOVE_VOLUME,
            volume = volume,
        )

        val panel =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(this@ProjectActivity, 18),
                    Ui.dp(this@ProjectActivity, 8),
                    Ui.dp(this@ProjectActivity, 18),
                    Ui.dp(this@ProjectActivity, 8),
                )
            }

        lateinit var dialog: AlertDialog

        targets.forEach { target ->
            panel.addView(
                Ui.textView(
                    context = this,
                    value = "› " + target.title,
                    sizeSp = 18f,
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    background =
                        Ui.roundedBackground(
                            context = this@ProjectActivity,
                            fill = Ui.surfaceAlt,
                            stroke = Ui.accent,
                            radiusDp = 11,
                        )
                    setPadding(
                        Ui.dp(this@ProjectActivity, 14),
                        Ui.dp(this@ProjectActivity, 12),
                        Ui.dp(this@ProjectActivity, 14),
                        Ui.dp(this@ProjectActivity, 12),
                    )
                    minHeight = Ui.dp(this@ProjectActivity, 52)
                    setOnClickListener {
                        store.moveVolume(
                            fromProjectId = project.id,
                            toProjectId = target.id,
                            volumeId = volume.id,
                        )
                        clearProjectDialogState(DIALOG_MOVE_VOLUME)
                        dialog.dismiss()
                        statusText.text =
                            "Том переміщено до проєкту " +
                                target.title +
                                ". Файли не копіювалися."
                        render()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = Ui.dp(this@ProjectActivity, 10)
                },
            )
        }

        panel.addView(
            Ui.textView(
                context = this,
                value = "Скасувати",
                sizeSp = 18f,
            ).apply {
                Ui.applyActionStyle(this)
                setOnClickListener {
                    clearProjectDialogState(DIALOG_MOVE_VOLUME)
                    dialog.dismiss()
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        dialog =
            AlertDialog.Builder(this)
                .setTitle("Перемістити том")
                .setMessage(
                    "Вибери проєкт призначення. Файли тому залишаться на телефоні без копіювання.",
                )
                .setView(panel)
                .create()

        trackProjectDialog(
            dialog = dialog,
            kind = DIALOG_MOVE_VOLUME,
            volumeId = volume.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.CHOICE,
        )
    }

    private fun volumeOperationSubject(
        volumeId: String?,
    ): String =
        volumeId
            ?.let {
                id ->
                store.volumes(
                    project.id,
                )
                    .firstOrNull {
                        it.id ==
                            id
                    }
            }
            ?.let {
                volume ->
                RdpkgExporter.defaultFileName(
                    project =
                        project,
                    volume =
                        volume,
                )
            }
            ?: "Renault Docs"

    private fun shareRdpkg(
        volume: ProjectVolumeRecord,
    ) {
        if (!RdpkgExporter.canFastExport(volume)) {
            statusText.text =
                "Поділитися можна томом, встановленим з .rdpkg. " +
                    "Для SAF-папок спочатку створи .rdpkg."
            return
        }

        val preparedFile =
            PreparedShareStore.existingVolumeFile(
                this,
                project,
                volume,
            )
        if (preparedFile != null) {
            sharePreparedRdpkg(preparedFile)
            return
        }

        val existing = rdpkgShareRunStore.load()
        if (existing.isRunning && existing.projectId == project.id && existing.volumeId == volume.id) {
            refreshRdpkgShareRunState()
            return
        }

        statusText.text = DEFAULT_STATUS_TEXT
        if (!RdpkgShareService.start(this, project.id, volume.id)) {
            statusText.text = "Не вдалося запустити підготовку тому."
        }
    }

    private fun refreshRdpkgShareRunState() {
        if (!::rdpkgShareRunStore.isInitialized || !::project.isInitialized || !::operationStatus.isInitialized) return
        val state = rdpkgShareRunStore.load()
        if (state.projectId != project.id) return

        if (state.isRunning) {
            statusText.text = DEFAULT_STATUS_TEXT
            operationStatus.showRunning(
                title = "Підготовка тому",
                detail =
                    state.message
                        .ifBlank {
                            "Готую…"
                        },
                current =
                    state.progressCurrent
                        .takeIf {
                            state.progressTotal >
                                0
                        },
                total =
                    state.progressTotal
                        .takeIf {
                            it >
                                0
                        },
                subject =
                    volumeOperationSubject(
                        state.volumeId,
                    ),
            )
            return
        }

        if (!state.isTerminal || state.finishedAtMs <= 0L || state.isTerminalDismissed) return

        statusText.text = DEFAULT_STATUS_TEXT
        if (state.phase == RdpkgShareRunPhase.COMPLETE) {
            val path = state.preparedPath
            val file = path?.let(::File)
            if (file == null || !file.isFile) {
                operationStatus.showTerminal(
                    "Підготовка тому · помилка",
                    "Підготовлений .rdpkg не знайдено.",
                ) {
                    rdpkgShareRunStore.dismissTerminal(state.finishedAtMs)
                    operationStatus.hide()
                }
                return
            }
            operationStatus.showTerminal(
                "Підготовка тому завершена",
                state.message.ifBlank { "Том готовий для поширення." },
            ) {
                rdpkgShareRunStore.dismissTerminal(state.finishedAtMs)
                operationStatus.hide()
            }
            if (!state.isChooserLaunched && rdpkgShareRunStore.markChooserLaunched(state.finishedAtMs)) {
                sharePreparedRdpkg(file)
            }
        } else {
            val detail = "Не вдалося підготувати том: " + state.message
            operationStatus.showTerminal("Підготовка тому · помилка", detail) {
                rdpkgShareRunStore.dismissTerminal(state.finishedAtMs)
                operationStatus.hide()
            }
        }
    }

    private fun sharePreparedRdpkg(
        file: File,
    ) {
        val uri = FileProvider.getUriForFile(this, packageName + ".files", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        startActivity(Intent.createChooser(send, "Поділитися томом"))
    }

    private fun confirmDeletePreparedVolume(
        volume: ProjectVolumeRecord,
    ) {
        setProjectDialogState(
            kind = DIALOG_DELETE_PREPARED_VOLUME,
            volume = volume,
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Видалити підготовлений файл?")
                .setMessage(
                    "Буде видалено тільки підготовлений .rdpkg для поширення. " +
                        "Сам том, проєкт і вихідні Renault-файли залишаться.",
                )
                .setNegativeButton("Скасувати") { _, _ ->
                    val returnToActions =
                        activeProjectDialogParentKind ==
                            DIALOG_VOLUME_ACTIONS
                    clearProjectDialogState(DIALOG_DELETE_PREPARED_VOLUME)
                    if (returnToActions) {
                        showVolumeActions(volume)
                    }
                }
                .setPositiveButton("Видалити") { _, _ ->
                    clearProjectDialogState(DIALOG_DELETE_PREPARED_VOLUME)
                    val deleted =
                        PreparedShareStore.deleteVolume(
                            this,
                            project,
                            volume,
                        )
                    statusText.text =
                        if (deleted) {
                            render()
                            "Підготовлений .rdpkg видалено. Том і вихідні файли не змінено."
                        } else {
                            "Не вдалося видалити підготовлений .rdpkg."
                        }
                }
                .create()
        trackProjectDialog(
            dialog = dialog,
            kind = DIALOG_DELETE_PREPARED_VOLUME,
            volumeId = volume.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog = dialog,
            role = DialogRole.DANGER,
        )
    }

    private fun sharePreparedRdpkg(
        volume: ProjectVolumeRecord,
        file: File,
    ) {
        if (!file.exists()) {
            statusText.text =
                "Підготовлений .rdpkg уже відсутній."
            return
        }
        val uri =
            FileProvider.getUriForFile(
                this,
                packageName + ".files",
                file,
            )
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        statusText.text =
            "Використовую раніше підготовлений .rdpkg: " +
                listOfNotNull(
                    volume.documentCode,
                    volume.date,
                ).joinToString(" · ").ifBlank {
                    volume.title
                }
        startActivity(
            Intent.createChooser(
                send,
                "Поділитися підготовленим томом",
            ),
        )
    }

    private fun startRdpkgExport(
        volume: ProjectVolumeRecord,
    ) {
        if (
            !RdpkgExporter.canFastExport(
                volume,
            )
        ) {
            statusText.text =
                "Швидкий .rdpkg export доступний для томів, встановлених з .rdpkg. " +
                    "Конвертацію SAF-папок додамо окремим етапом."
            return
        }

        pendingRdpkgExportVolumeId =
            volume.id

        val picker =
            Intent(
                Intent.ACTION_CREATE_DOCUMENT,
            ).apply {
                addCategory(
                    Intent.CATEGORY_OPENABLE,
                )
                type =
                    "application/octet-stream"
                putExtra(
                    Intent.EXTRA_TITLE,
                    RdpkgExporter
                        .defaultFileName(
                            project =
                                project,
                            volume =
                                volume,
                        ),
                )
                addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_RDPKG_EXPORT,
        )
    }

    private fun confirmRemoveVolume(
        volume: ProjectVolumeRecord,
    ) {
        val label =
            listOfNotNull(
                volume.documentCode,
                volume.date,
            )
                .joinToString(
                    " · ",
                )
                .ifBlank {
                    volume.title
                }

        setProjectDialogState(
            kind =
                DIALOG_REMOVE_VOLUME,
            volume =
                volume,
        )

        val dialog =
            AlertDialog.Builder(
                this,
            )
                .setTitle(
                    "Видалити том з проєкту?",
                )
                .setMessage(
                    label +
                        "\n\nБуде видалено лише запис із проєкту " +
                        project.title +
                        ". Файли на телефоні залишаться без змін.",
                )
                .setNegativeButton(
                    "Скасувати",
                ) {
                    _,
                    _ ->
                    val returnToActions =
                        activeProjectDialogParentKind ==
                            DIALOG_VOLUME_ACTIONS

                    clearProjectDialogState(
                        DIALOG_REMOVE_VOLUME,
                    )

                    if (
                        returnToActions
                    ) {
                        showVolumeActions(
                            volume,
                        )
                    }
                }
                .setPositiveButton(
                    "Видалити з проєкту",
                ) {
                    _,
                    _ ->
                    clearProjectDialogState(
                        DIALOG_REMOVE_VOLUME,
                    )

                    store.removeVolume(
                        projectId =
                            project.id,
                        volumeId =
                            volume.id,
                    )

                    statusText.text =
                        "Том видалено з проєкту: " +
                            label +
                            ". Файли не видалено."

                    render()
                }
                .create()

        trackProjectDialog(
            dialog =
                dialog,
            kind =
                DIALOG_REMOVE_VOLUME,
            volumeId =
                volume.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.DANGER,
        )
    }

    private fun importPreparedVolumes(
        volumes: List<ProjectVolumeRecord>,
        allowOverride: Boolean,
    ) {
        if (
            volumes.size ==
            1
        ) {
            importPreparedVolume(
                volume =
                    volumes.single(),
                allowOverride =
                    allowOverride,
            )
            return
        }

        showPreparedVolumeChooser(
            volumes =
                volumes,
            allowOverride =
                allowOverride,
        )
    }

    private fun showPreparedVolumeChooser(
        volumes: List<ProjectVolumeRecord>,
        allowOverride: Boolean,
    ) {
        if (
            volumes.isEmpty()
        ) {
            clearProjectDialogState(
                DIALOG_VOLUME_CHOOSER,
            )
            return
        }

        val labels =
            volumes.map {
                volume ->
                listOfNotNull(
                    volume.documentCode,
                    volume.date,
                )
                    .joinToString(
                        " · ",
                    )
                    .ifBlank {
                        volume.title
                    }
            }
                .toTypedArray()

        activeProjectDialogKind =
            DIALOG_VOLUME_CHOOSER
        activeProjectDialogVolumeId =
            null
        activeProjectDialogTreeUri =
            volumes.first()
                .treeUri
        activeProjectDialogAllowOverride =
            allowOverride

        val dialog =
            AlertDialog.Builder(
                this,
            )
                .setTitle(
                    "Вибери том",
                )
                .setItems(
                    labels,
                ) {
                    _,
                    which ->
                    clearProjectDialogState(
                        DIALOG_VOLUME_CHOOSER,
                    )
                    importPreparedVolume(
                        volume =
                            volumes[which],
                        allowOverride =
                            allowOverride,
                    )
                }
                .setNegativeButton(
                    "Скасувати",
                    null,
                )
                .create()

        trackProjectDialog(
            dialog =
                dialog,
            kind =
                DIALOG_VOLUME_CHOOSER,
            volumeId =
                null,
        )
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun importPreparedVolume(
        volume: ProjectVolumeRecord,
        allowOverride: Boolean,
    ) {
        val detectedProject =
            volume.projectHint
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            detectedProject !=
                null &&
            detectedProject !=
                project.id
        ) {
            if (
                !allowOverride
            ) {
                statusText.text =
                    "Цей том підготовлено для проєкту: " +
                        detectedProject +
                        ". Вибери правильний проєкт або скористайся ручним додаванням."
                return
            }

            showProjectMismatchDialog(
                volume =
                    volume,
                detectedProject =
                    detectedProject,
            )

            return
        }

        saveVolume(
            volume,
        )
    }

    private fun showProjectMismatchDialog(
        volume: ProjectVolumeRecord,
        detectedProject: String =
            volume.projectHint
                .orEmpty(),
    ) {
        setProjectDialogState(
            kind =
                DIALOG_PROJECT_MISMATCH,
            volume =
                volume,
        )

        val dialog =
            AlertDialog.Builder(
                this,
            )
                .setTitle(
                    "Інший проєкт",
                )
                .setMessage(
                    "Том визначено для проєкту \"" +
                        detectedProject +
                        "\", а ти вибрав \"" +
                        project.title +
                        "\".\n\nДодати вручну саме сюди?",
                )
                .setNegativeButton(
                    "Скасувати",
                    null,
                )
                .setPositiveButton(
                    "Додати сюди",
                ) {
                    _,
                    _ ->
                    clearProjectDialogState(
                        DIALOG_PROJECT_MISMATCH,
                    )
                    saveVolume(
                        volume,
                    )
                }
                .create()

        trackProjectDialog(
            dialog =
                dialog,
            kind =
                DIALOG_PROJECT_MISMATCH,
            volumeId =
                volume.id,
        )
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CONFIRM,
        )
    }

    private fun setProjectDialogState(
        kind: String,
        volume: ProjectVolumeRecord? =
            null,
    ) {
        activeProjectDialogKind =
            kind
        activeProjectDialogVolumeId =
            volume?.id
        activeProjectDialogTreeUri =
            volume?.treeUri
        activeProjectDialogAllowOverride =
            false
    }

    private fun clearProjectDialogState(
        expectedKind: String? =
            null,
    ) {
        if (
            expectedKind !=
                null &&
            activeProjectDialogKind !=
                expectedKind
        ) {
            return
        }

        activeProjectDialogKind =
            ""
        activeProjectDialogVolumeId =
            null
        activeProjectDialogTreeUri =
            null
        activeProjectDialogAllowOverride =
            false
        activeProjectDialogParentKind =
            null
    }

    private fun trackProjectDialog(
        dialog: AlertDialog,
        kind: String,
        volumeId: String?,
    ) {
        dialog.setOnDismissListener {
            if (
                !isChangingConfigurations &&
                activeProjectDialogKind ==
                    kind &&
                activeProjectDialogVolumeId ==
                    volumeId
            ) {
                clearProjectDialogState(
                    kind,
                )
            }
        }
    }

    private fun restoreProjectDialog() {
        when (
            activeProjectDialogKind
        ) {
            DIALOG_VOLUME_ACTIONS,
            DIALOG_REMOVE_VOLUME,
            DIALOG_MOVE_VOLUME,
            DIALOG_DELETE_PREPARED_VOLUME -> {
                val volume =
                    store.volumes(
                        project.id,
                    )
                        .firstOrNull {
                            it.id ==
                                activeProjectDialogVolumeId
                        }

                if (
                    volume ==
                    null
                ) {
                    clearProjectDialogState()
                    return
                }

                when (activeProjectDialogKind) {
                    DIALOG_VOLUME_ACTIONS ->
                        showVolumeActions(volume)
                    DIALOG_MOVE_VOLUME ->
                        showMoveVolumeDialog(volume)
                    DIALOG_DELETE_PREPARED_VOLUME ->
                        confirmDeletePreparedVolume(volume)
                    else ->
                        confirmRemoveVolume(volume)
                }
            }

            DIALOG_VOLUME_CHOOSER ->
                restorePreparedVolumeChooser()

            DIALOG_PROJECT_MISMATCH ->
                restoreProjectMismatchDialog()

            else ->
                clearProjectDialogState()
        }
    }

    private fun restorePreparedVolumeChooser() {
        val treeUri =
            activeProjectDialogTreeUri
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: run {
                    clearProjectDialogState()
                    return
                }

        val allowOverride =
            activeProjectDialogAllowOverride

        PreparedVolumeReader.readAll(
            context =
                this,
            treeUri =
                Uri.parse(
                    treeUri,
                ),
        )
            .onSuccess {
                volumes ->
                if (
                    volumes.size <
                    2
                ) {
                    clearProjectDialogState()
                    statusText.text =
                        "Список томів змінився. Вибери папку ще раз."
                    return@onSuccess
                }

                showPreparedVolumeChooser(
                    volumes =
                        volumes,
                    allowOverride =
                        allowOverride,
                )
            }
            .onFailure {
                clearProjectDialogState()
                statusText.text =
                    "Не вдалося відновити вибір томів. Вибери папку ще раз."
            }
    }

    private fun restoreProjectMismatchDialog() {
        val treeUri =
            activeProjectDialogTreeUri
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: run {
                    clearProjectDialogState()
                    return
                }
        val volumeId =
            activeProjectDialogVolumeId
                ?: run {
                    clearProjectDialogState()
                    return
                }

        PreparedVolumeReader.readAll(
            context =
                this,
            treeUri =
                Uri.parse(
                    treeUri,
                ),
        )
            .onSuccess {
                volumes ->
                val volume =
                    volumes.firstOrNull {
                        it.id ==
                            volumeId
                    }

                if (
                    volume ==
                    null
                ) {
                    clearProjectDialogState()
                    statusText.text =
                        "Том для підтвердження більше не знайдено."
                    return@onSuccess
                }

                showProjectMismatchDialog(
                    volume =
                        volume,
                )
            }
            .onFailure {
                clearProjectDialogState()
                statusText.text =
                    "Не вдалося відновити підтвердження. Вибери том ще раз."
            }
    }

    private fun saveVolume(
        volume: ProjectVolumeRecord,
    ) {
        val existed =
            store.volumes(
                project.id,
            )
                .any {
                    it.id ==
                        volume.id
                }

        store.upsertVolume(
            projectId =
                project.id,
            volume =
                volume,
        )

        statusText.text =
            if (
                existed
            ) {
                "Том оновлено в проєкті " +
                    project.title +
                    ": " +
                    volume.title
            } else {
                "Том додано в проєкт " +
                    project.title +
                    ": " +
                    volume.title
            }

        render()
    }

    private fun openVolume(
        volume: ProjectVolumeRecord,
    ) {
        val openIntent =
            when (
                settings.defaultOpenMode
            ) {
                DatasetOpenMode.MODERN -> {
                    val displayTitle =
                        listOfNotNull(
                            volume.documentCode,
                            volume.date,
                        )
                            .joinToString(
                                " · ",
                            )
                            .ifBlank {
                                volume.title
                            }

                    ModernVolumeActivity
                        .intentForVolume(
                            context =
                                this,
                            datasetTitle =
                                project.title,
                            treeUri =
                                volume.treeUri,
                            classicEntrypoint =
                                volume.openEntrypoint,
                            volumeTitle =
                                displayTitle,
                            volumeEntrypoint =
                                volume.entrypoint,
                        )
                }

                DatasetOpenMode.CLASSIC ->
                    ViewerActivity
                        .intent(
                            context =
                                this,
                            record =
                                volume
                                    .asDatasetRecord(),
                        )
            }

        startActivity(
            openIntent,
        )
    }

    private fun helpSpec(
        helpId: String,
    ): HelpDialogSpec? =
        when (
            helpId
        ) {
            HELP_PROJECT ->
                HelpDialogSpec(
                    title =
                        "Проєкт і томи",
                    message =
                        "Проєкт — це одна модель Renault. Кожен випуск документації додається окремим томом.\n\n" +
                            "Том можна оновити повторним імпортом того самого .rdpkg. Видалення тому з проєкту не видаляє файли з телефона.",
                )

            HELP_ADD ->
                HelpDialogSpec(
                    title =
                        "Додати",
                    message =
                        "Розгорни панель «Додати», щоб побачити всі способи.\n\n" +
                            "Авто — вибрати один готовий .rdpkg.\n" +
                            "Вручну — вибрати вже підготовлену папку через SAF.\n" +
                            "Створити .rdpkg з raw — підготувати пакет з оригінальної Renault-папки.\n" +
                            "Створити .rdpkg з архіву — використати ZIP, 7Z або RAR без ручної розпаковки.\n\n" +
                            "📌 закріплює панель у розгорнутому стані. Закріплена шпилька — кольорова, відкріплена — чорно-біла. В альбомному режимі панель автоматично згортається, шпилька тимчасово неактивна, а томи доступні для прокручування. Панель можна розгорнути вручну. Після повернення у вертикальний режим попереднє закріплення відновлюється.",
                )

            HELP_RAW ->
                HelpDialogSpec(
                    title =
                        "Створити .rdpkg з raw",
                    message =
                        "Виберіть оригінальну папку Renault, потім місце збереження.\n\n" +
                            "Програма підготує переносний .rdpkg, який можна додати до проєкту або передати на інший пристрій.",
                )

            HELP_ARCHIVE ->
                HelpDialogSpec(
                    title =
                        "Створити .rdpkg з архіву",
                    message =
                        "Виберіть старий Renault архів ZIP, 7Z або RAR без попередньої розпаковки.\n\n" +
                            "Renault Docs скопіює архів у приватну тимчасову область, безпечно розпакує його, знайде raw-том і створить .rdpkg. Оригінальний архів не змінюється.",
                )

            else ->
                null
        }

    private fun openPackagePicker() {
        val picker =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT,
            ).apply {
                addCategory(
                    Intent.CATEGORY_OPENABLE,
                )
                type =
                    "*/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_RDPKG_FILE,
        )
    }

    private fun openVolumePicker(
        manual: Boolean,
    ) {
        pendingManualImport =
            manual

        val picker =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE,
            ).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
                addFlags(
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                )
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            picker,
            REQUEST_VOLUME_FOLDER,
        )
    }

    private fun persistReadPermission(
        uri: Uri,
        returnedFlags: Int,
    ) {
        val grantedRead =
            returnedFlags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (
            grantedRead ==
            0
        ) {
            return
        }

        runCatching {
            contentResolver
                .takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
        }
    }

    private fun persistReadWritePermission(
        uri: Uri,
        returnedFlags: Int,
    ) {
        val granted =
            returnedFlags and
                (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

        if (
            granted ==
            0
        ) {
            return
        }

        runCatching {
            contentResolver
                .takePersistableUriPermission(
                    uri,
                    granted,
                )
        }
    }

    companion object {
        private const val EXTRA_PROJECT_ID =
            "projectId"
        private const val EXTRA_OPEN_PICKER =
            "openPicker"
        private const val EXTRA_SHOW_ADD_PANEL =
            "showAddPanel"
        private const val REQUEST_VOLUME_FOLDER =
            4301
        private const val REQUEST_RDPKG_FILE =
            4302
        private const val REQUEST_RDPKG_EXPORT =
            4303
        private const val REQUEST_NATIVE_RDPKG_SOURCE =
            4304
        private const val REQUEST_NATIVE_RDPKG_DESTINATION =
            4305
        private const val REQUEST_NATIVE_RDPKG_ARCHIVE_SOURCE =
            4306
        private const val STATE_PENDING_MANUAL_IMPORT =
            "pendingManualImport"
        private const val STATE_SCROLL_Y =
            "projectScrollY"
        private const val STATE_ADD_PANEL_EXPANDED =
            "addPanelExpanded"
        private const val STATE_OPERATION_STATUS_DETAILS_EXPANDED =
            "operationStatusDetailsExpanded"
        private const val STATE_ACTIVE_DIALOG_KIND =
            "activeProjectDialogKind"
        private const val STATE_ACTIVE_DIALOG_VOLUME_ID =
            "activeProjectDialogVolumeId"
        private const val STATE_ACTIVE_DIALOG_TREE_URI =
            "activeProjectDialogTreeUri"
        private const val STATE_ACTIVE_DIALOG_ALLOW_OVERRIDE =
            "activeProjectDialogAllowOverride"
        private const val STATE_ACTIVE_DIALOG_PARENT_KIND =
            "activeProjectDialogParentKind"
        private const val STATE_PENDING_RDPKG_EXPORT_VOLUME_ID =
            "pendingRdpkgExportVolumeId"
        private const val STATE_PENDING_NATIVE_SOURCE_URI =
            "pendingNativeSourceUri"
        private const val STATE_PENDING_NATIVE_SOURCE_NAME =
            "pendingNativeSourceName"
        private const val STATE_PENDING_NATIVE_SOURCE_KIND =
            "pendingNativeSourceKind"
        private const val STATE_PENDING_NATIVE_REQUEST_ID =
            "pendingNativeRequestId"
        private const val DIALOG_VOLUME_ACTIONS =
            "volumeActions"
        private const val DIALOG_REMOVE_VOLUME =
            "removeVolume"
        private const val DIALOG_MOVE_VOLUME =
            "moveVolume"
        private const val DIALOG_DELETE_PREPARED_VOLUME =
            "deletePreparedVolume"
        private const val DIALOG_VOLUME_CHOOSER =
            "volumeChooser"
        private const val DIALOG_PROJECT_MISMATCH =
            "projectMismatch"

        private const val HELP_PROJECT =
            "project"
        private const val HELP_ADD =
            "add"
        private const val HELP_RAW =
            "raw"
        private const val HELP_ARCHIVE =
            "archive"

        private const val DEFAULT_STATUS_TEXT =
            ""

        private const val NATIVE_RUN_REFRESH_MS =
            100L
        private const val NATIVE_RUN_STARTUP_GRACE_MS =
            30_000L

        fun intent(
            context: Context,
            projectId: String,
            openPicker: Boolean = false,
            showAddPanel: Boolean = false,
        ): Intent =
            Intent(
                context,
                ProjectActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_PROJECT_ID,
                    projectId,
                )
                putExtra(
                    EXTRA_OPEN_PICKER,
                    openPicker,
                )
                putExtra(
                    EXTRA_SHOW_ADD_PANEL,
                    showAddPanel,
                )
            }
    }
}
