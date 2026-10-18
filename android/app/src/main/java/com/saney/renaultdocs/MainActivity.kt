package com.saney.renaultdocs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.text.Editable
import android.text.TextWatcher
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {
    private lateinit var libraryContainer: LinearLayout
    private lateinit var homeAddPanelBody: BoundedAddActionsScrollView
    private lateinit var homeAddPinButton: TextView
    private lateinit var homeAddToggleButton: TextView
    private var homeAddPanelPinned = false
    private var homeAddPanelExpanded = false
    // Portrait pin preference is never modified by temporary landscape expansion.
    private var homeAddLandscapeExpanded = false
    private val monochromePinPaint by lazy {
        Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply { setSaturation(0f) },
            )
        }
    }
    private lateinit var statusText: TextView
    private lateinit var operationStatus: OperationStatusView
    // v0.5.148: its own card for erasing files of deleted volumes (can run next to an import).
    // v0.5.152 (TODO №25): the version line, outside the height-limited actions list.
    private lateinit var homeVersionLine: TextView
    private lateinit var trashStatus: OperationStatusView
    private lateinit var store: DatasetStore
    private lateinit var projectStore: ProjectStore
    private lateinit var settings: AppSettings
    private lateinit var helpDialogs:
        LifecycleHelpDialogController
    private lateinit var confirmDialogs: RestorableDialogs
    private lateinit var projectDialogs:
        HomeProjectDialogController
    private lateinit var folderExportRunStore: ProjectFolderExportRunStore
    private var pendingFolderExportProjectId: String? = null
    // #159: the Home operation status keeps the owner's expanded/collapsed choice on rotation.
    private var restoredStatusDetailsExpanded = false

    private lateinit var rdprojectShareRunStore:
        RdprojectShareRunStore
    private var lastRenderedRdprojectFinishedAt =
        0L
    private val rdprojectRunHandler by lazy {
        Handler(
            Looper.getMainLooper(),
        )
    }
    private val rdprojectRunRefresh =
        object : Runnable {
            override fun run() {
                refreshRdprojectShareRunState()
                refreshTrashCard()
                rdprojectRunHandler.postDelayed(
                    this,
                    RDPROJECT_RUN_REFRESH_MS,
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val restoredStatusText =
            savedInstanceState
                ?.getCharSequence(
                    STATE_STATUS_TEXT,
                )
                ?.toString()
        val restoredStatusColor =
            if (
                savedInstanceState
                    ?.containsKey(
                        STATE_STATUS_COLOR,
                    ) ==
                    true
            ) {
                savedInstanceState.getInt(
                    STATE_STATUS_COLOR,
                )
            } else {
                null
            }

        store = DatasetStore(this)
        projectStore = ProjectStore(this)
        rdprojectShareRunStore =
            RdprojectShareRunStore(
                this,
            )
        folderExportRunStore = ProjectFolderExportRunStore(this)
        pendingFolderExportProjectId =
            savedInstanceState?.getString(STATE_PENDING_FOLDER_EXPORT_PROJECT)
        restoredStatusDetailsExpanded =
            savedInstanceState?.getBoolean(STATE_STATUS_DETAILS_EXPANDED, false) ?: false
        settings = AppSettings(this)
        homeAddPanelPinned = settings.homeAddPanelPinned
        homeAddPanelExpanded = savedInstanceState
            ?.getBoolean(STATE_HOME_ADD_EXPANDED, homeAddPanelPinned)
            ?: homeAddPanelPinned
        if (homeAddPanelPinned) homeAddPanelExpanded = true
        projectDialogs =
            HomeProjectDialogController(
                activity = this,
                store = projectStore,
                onProjectRemoved = {
                    project ->
                    statusText.text =
                        "Проєкт видалено з бібліотеки: " +
                            project.title +
                            ". Файли на телефоні не видалено."
                    renderLibrary()
                },
                onPreparedProjectDeleted = {
                    project,
                    deleted ->
                    statusText.text =
                        if (
                            deleted
                        ) {
                            "Підготовлений .rdproject видалено: " +
                                project.title +
                                ". Проєкт і томи не змінено."
                        } else {
                            "Не вдалося видалити підготовлений .rdproject: " +
                                project.title
                        }
                    if (deleted) {
                        // v0.5.107: the owner deleted this file on purpose — the old
                        // «завершено» card for it is closed, not turned into an error.
                        val last = rdprojectShareRunStore.load()
                        if (
                            last.phase == RdprojectShareRunPhase.COMPLETE &&
                            last.projectId == project.id &&
                            rdprojectShareRunStore.dismissTerminal(last.finishedAtMs) &&
                            ::operationStatus.isInitialized
                        ) {
                            operationStatus.hide()
                        }
                    }
                    renderLibrary()
                },
                onShareProgress = { current, total, message ->
                    statusText.text = message
                    if (::operationStatus.isInitialized) {
                        operationStatus.showRunning(
                            "Підготовка проєкту",
                            message,
                            current,
                            total,
                        )
                    }
                },
                onExportFolderPickRequested = { projectId ->
                    openProjectFolderExportPicker(projectId)
                },
                onSeparateVolumeExportStart = { projectId, folderUri, volumeIds ->
                    startSeparateVolumeExport(projectId, folderUri, volumeIds)
                },
                onVolumesDeleted = { _, message ->
                    // v0.5.147: «Видалити томи разом з файлами» — the result and fresh counts.
                    statusText.text = message
                    renderLibrary()
                },
                onShareFinished = { message ->
                    statusText.text = message
                    if (::operationStatus.isInitialized) {
                        operationStatus.showTerminal(
                            "Підготовка проєкту · помилка",
                            message,
                        ) {
                            operationStatus.hide()
                        }
                    }
                },
            )
        projectDialogs.restore(
            savedInstanceState,
        )
        confirmDialogs =
            RestorableDialogs(
                activity = this,
                stateKey = "homeConfirm",
                build = ::buildConfirmDialog,
            )
        confirmDialogs.restore(
            savedInstanceState,
        )

        helpDialogs =
            LifecycleHelpDialogController(
                activity = this,
                resolve = ::helpSpec,
            )
        helpDialogs.restore(
            savedInstanceState,
        )

        projectStore.migrateLegacySingleVolumeDatasets(
            store.load(),
        )
        setContentView(buildContent())
        renderLibrary()
        requestNotificationPermissionIfNeeded()

        if (
            !restoredStatusText.isNullOrBlank()
        ) {
            statusText.text =
                restoredStatusText
            restoredStatusColor?.let {
                statusText.setTextColor(
                    it,
                )
            }
        }

        helpDialogs.restoreOpen()
        projectDialogs.restoreOpen()
        // v0.5.106: an open stop question comes back after rotation.
        window.decorView.post {
            confirmDialogs.restoreOpen()
        }
        repairSavedVolumeMetadata()
    }

    /**
     * #181 stop questions on the Home status card (v0.5.102), rebuilt after
     * rotation (v0.5.106) only while that operation is still running.
     */
    private fun buildConfirmDialog(key: String): Dialog? =
        when (key) {
            CONFIRM_STOP_FOLDER_EXPORT ->
                if (!folderExportRunStore.load().isRunning) {
                    null
                } else {
                    ConfirmActions.confirmStop(
                        activity = this,
                        title = "Зупинити експорт томів?",
                        message = "Поточний том не буде дописано. Уже готові файли в папці лишаться.",
                    ) {
                        if (folderExportRunStore.requestCancel()) {
                            statusText.text = "Зупиняю експорт після поточного кроку…"
                        }
                    }
                }

            CONFIRM_STOP_RDPROJECT ->
                if (!rdprojectShareRunStore.load().isRunning) {
                    null
                } else {
                    ConfirmActions.confirmStop(
                        activity = this,
                        title = "Зупинити підготовку проєкту?",
                        message = "Недороблений .rdproject буде видалено. Томи і попередні файли не змінюються.",
                    ) {
                        if (rdprojectShareRunStore.requestCancel()) {
                            statusText.text = "Зупиняю підготовку проєкту…"
                        }
                    }
                }

            else -> null
        }

    override fun onDestroy() {
        // v0.5.150: an update window of this screen is closed (rotation keeps its key).
        AppUpdateState.onHostDestroy(this)
        if (::confirmDialogs.isInitialized) {
            confirmDialogs.onDestroy()
        }
        if (::projectDialogs.isInitialized) {
            projectDialogs.onDestroy()
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        AppUpdateState.attach(this)
        rdprojectRunHandler.removeCallbacks(
            rdprojectRunRefresh,
        )
        rdprojectRunHandler.post(
            rdprojectRunRefresh,
        )
    }

    override fun onStop() {
        rdprojectRunHandler.removeCallbacks(
            rdprojectRunRefresh,
        )
        AppUpdateState.detach(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::libraryContainer.isInitialized) {
            renderLibrary()
        }
        refreshRdprojectShareRunState()
        // v0.5.148: something left in the trash (app closed while erasing) — go on erasing.
        VolumeTrash.startErase(this)
        refreshTrashCard()
        // v0.5.150: update from Google Drive — a window open before rotation comes back; after
        // «Дозволити з цього джерела» installing goes on; once a day a quiet check.
        if (AppUpdateSwitch.ENABLED) {
            val updates = AppUpdateUi(this)
            if (AppUpdateState.dialog == null) updates.restoreOpen()
            updates.resumePendingInstall()
            updates.autoCheck()
        }
    }

    /**
     * v0.5.148 (owner 05:25): the bar for erasing files of deleted volumes — «Том 5 з 23 ·
     * 34 % · залишилось ~12 хв»; after the end — how many files and how long, until closed.
     * Reads only memory and preferences (called every 100 ms).
     */
    private fun refreshTrashCard() {
        if (!::trashStatus.isInitialized) return
        val step = VolumeTrash.progress
        if (step != null) {
            trashStatus.showRunning(
                title = VolumeDeleteChoice.ERASE_TITLE,
                detail = VolumeDeleteChoice.eraseDetail(step),
                current = if (step.counting && step.itemIndex == 1) null else (step.fraction * 1000).toInt(),
                total = if (step.counting && step.itemIndex == 1) null else 1000,
                counterLabel = VolumeDeleteChoice.eraseCounter(step),
            )
            return
        }
        val summary = VolumeTrash.lastSummary(this)
        if (summary != null) {
            trashStatus.showTerminal(
                title = VolumeDeleteChoice.eraseDoneTitle(summary.left),
                detail = VolumeDeleteChoice.eraseDoneDetail(summary.files, summary.volumes, summary.ms, summary.left),
                outcome = if (summary.left == 0) OperationTerminalOutcome.SUCCESS else OperationTerminalOutcome.NEUTRAL,
            ) {
                VolumeTrash.dismissSummary(this, summary.finishedAt)
                trashStatus.hide()
            }
            return
        }
        if (trashStatus.visibility != View.GONE) trashStatus.hide()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        if (
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS,
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        requestPermissions(
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            REQUEST_POST_NOTIFICATIONS,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
        )

        if (
            requestCode !=
            REQUEST_POST_NOTIFICATIONS
        ) {
            return
        }

        val granted =
            grantResults.firstOrNull() ==
                PackageManager.PERMISSION_GRANTED

        if (
            !granted &&
            ::statusText.isInitialized
        ) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Сповіщення Renodocs вимкнені. " +
                    "Фонові процеси працюватимуть, але прогрес може не показуватися у шторці."
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        helpDialogs.save(
            outState,
        )
        projectDialogs.save(
            outState,
        )
        if (::confirmDialogs.isInitialized) {
            confirmDialogs.save(
                outState,
            )
        }
        outState.putString(
            STATE_PENDING_FOLDER_EXPORT_PROJECT,
            pendingFolderExportProjectId,
        )
        outState.putBoolean(STATE_HOME_ADD_EXPANDED, homeAddPanelExpanded)
        outState.putBoolean(
            STATE_STATUS_DETAILS_EXPANDED,
            ::operationStatus.isInitialized && operationStatus.isProjectDetailsExpanded(),
        )

        if (
            ::statusText.isInitialized
        ) {
            outState.putCharSequence(
                STATE_STATUS_TEXT,
                statusText.text,
            )
            outState.putInt(
                STATE_STATUS_COLOR,
                statusText.currentTextColor,
            )
        }

        super.onSaveInstanceState(
            outState,
        )
    }

    @Deprecated("Uses platform SAF result for minSdk 26 compatibility.")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FOLDER_EXPORT_DESTINATION) {
            val projectId = pendingFolderExportProjectId
            pendingFolderExportProjectId = null
            if (resultCode != RESULT_OK || projectId.isNullOrBlank()) {
                // Cancelled picker keeps the step-by-step window and its previous folder.
                projectDialogs.onExportFolderPicked(null)
                return
            }
            val folder = data?.data
            if (folder == null) {
                statusText.text = "Android не повернув папку для .rdpkg."
                projectDialogs.onExportFolderPicked(null)
                return
            }
            val flags = (data?.flags ?: 0) and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            if ((flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0 ||
                (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0
            ) {
                statusText.text = "Для експорту потрібні права читання та запису в папку."
                projectDialogs.onExportFolderPicked(null)
                return
            }
            val permission = runCatching {
                contentResolver.takePersistableUriPermission(folder, flags)
            }
            if (permission.isFailure) {
                statusText.text = "Не вдалося зберегти доступ до вибраної папки."
                projectDialogs.onExportFolderPicked(null)
                return
            }
            // Folder only goes back to the window (step 2). Export starts on explicit confirm.
            projectDialogs.onExportFolderPicked(folder.toString())
            return
        }
        // v0.5.112 (owner decision 2026-10-12): the «Legacy» folder picker
        // (REQUEST_DATASET_FOLDER) was removed together with the converter and
        // «Старі бібліотеки». Archive: branch archive/legacy-converter-v0.5.111.
    }

    private fun repairSavedVolumeMetadata() {
        Thread {
            val repaired =
                projectStore
                    .repairIncompleteVolumeMetadata()

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
                    renderLibrary()
                }
            }
        }.start()
    }

    private fun buildContent(): View {
        window.statusBarColor = Ui.background
        window.navigationBarColor = Ui.background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background =
                    Ui.emblemScreenBackground(context)
        }
        Ui.applySystemInsets(root)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topBar.addView(
            Ui.textView(
                context = this,
                value = "Renodocs",
                sizeSp = 30f,
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
            )
        )

        topBar.addView(
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_LIBRARY,
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
            ).apply {
                marginEnd =
                    Ui.dp(
                        this@MainActivity,
                        4,
                    )
            },
        )

        topBar.addView(
            ImageButton(this).apply {
                setImageResource(
                    android.R.drawable.ic_menu_preferences,
                )
                contentDescription =
                    "Налаштування"
                setBackgroundColor(
                    android.graphics.Color.TRANSPARENT,
                )
                setPadding(
                    Ui.dp(
                        this@MainActivity,
                        10,
                    ),
                    Ui.dp(
                        this@MainActivity,
                        10,
                    ),
                    Ui.dp(
                        this@MainActivity,
                        10,
                    ),
                    Ui.dp(
                        this@MainActivity,
                        10,
                    ),
                )
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            SettingsActivity::class.java,
                        )
                    )
                }
            },
            LinearLayout.LayoutParams(
                Ui.dp(this, 48),
                Ui.dp(this, 48),
            )
        )

        root.addView(topBar)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Keep the shared Add header and operation status visible.
        // Only the project library scrolls beneath it.
        root.addView(
            buildHomeAddPanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = Ui.dp(this@MainActivity, 8) },
        )

        libraryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(
                0,
                Ui.dp(this@MainActivity, 4),
                0,
                Ui.dp(this@MainActivity, 24),
            )
        }
        scrollContent.addView(
            libraryContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        )

        scroll.addView(
            scrollContent,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        )

        // TODO №27: the emblem is now the background of the whole screen
        // (Ui.emblemScreenBackground on root, Renodocs 0.6.2); in 0.6.1 it was an
        // ImageView behind this list only.
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )

        return root
    }

    private fun buildProjectCatalogCard():
        View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            isClickable =
                true
            isFocusable =
                true
            background =
                Ui.roundedBackground(
                    context =
                        this@MainActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                )
            setPadding(
                Ui.dp(
                    this@MainActivity,
                    16,
                ),
                Ui.dp(
                    this@MainActivity,
                    13,
                ),
                Ui.dp(
                    this@MainActivity,
                    16,
                ),
                Ui.dp(
                    this@MainActivity,
                    13,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        "Готові проєкти",
                    sizeSp =
                        18f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                },
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        "Завантажити з Google Drive",
                    sizeSp =
                        Ui.secondaryTextSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@MainActivity,
                            4,
                        ),
                        0,
                        0,
                    )
                },
            )

            setOnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        DriveCatalogActivity::class.java,
                    ),
                )
            }
        }

    /**
     * v0.5.132: Home → «Додати» → «Проєкт із файлу» — import of a whole project
     * from one .rdproject (made by ⋮ → «Поділитися проєктом одним файлом»).
     */
    private fun buildProjectFromFileCard(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            background =
                Ui.roundedBackground(
                    context = this@MainActivity,
                    fill = Ui.surface,
                    stroke = Ui.border,
                )
            setPadding(
                Ui.dp(this@MainActivity, 16),
                Ui.dp(this@MainActivity, 13),
                Ui.dp(this@MainActivity, 16),
                Ui.dp(this@MainActivity, 13),
            )
            addView(
                Ui.textView(this@MainActivity, "Проєкт із файлу", 18f, Ui.entityTitle).apply {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                },
            )
            addView(
                Ui.textView(
                    this@MainActivity,
                    "Відкрити .rdproject — додати томи, яких ще немає",
                    Ui.secondaryTextSp,
                    Ui.muted,
                ).apply {
                    setPadding(0, Ui.dp(this@MainActivity, 4), 0, 0)
                },
            )
            setOnClickListener {
                startActivity(RdprojectImportActivity.pickIntent(this@MainActivity))
            }
        }

    private fun buildHomeAddPanel(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.roundedBackground(
                context = this@MainActivity,
                fill = Ui.surfaceAlt,
                stroke = Ui.accent,
            )
            setPadding(
                Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 8),
                Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 8),
            )

            val header = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setOnClickListener { toggleHomeAddPanel() }
            }
            header.addView(
                Ui.textView(
                    this@MainActivity, "Додати", 18f, Ui.accent,
                ).apply {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                },
                LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
                ),
            )
            homeAddPinButton = homeAddHeaderButton(
                "📌", "Закріпити панель Додати",
            ) {
                if (!isHomeLandscape()) {
                    homeAddPanelPinned = !homeAddPanelPinned
                    settings.homeAddPanelPinned = homeAddPanelPinned
                    if (homeAddPanelPinned) homeAddPanelExpanded = true
                    updateHomeAddPanelUi()
                }
            }
            header.addView(
                homeAddPinButton,
                LinearLayout.LayoutParams(
                    Ui.dp(this@MainActivity, 40), Ui.dp(this@MainActivity, 40),
                ).apply { marginEnd = Ui.dp(this@MainActivity, 6) },
            )
            homeAddToggleButton = homeAddHeaderButton(
                "▼", "Розгорнути або згорнути панель Додати",
            ) { toggleHomeAddPanel() }
            header.addView(
                homeAddToggleButton,
                LinearLayout.LayoutParams(
                    Ui.dp(this@MainActivity, 40), Ui.dp(this@MainActivity, 40),
                ).apply { marginEnd = Ui.dp(this@MainActivity, 6) },
            )
            header.addView(
                Ui.helpButton(this@MainActivity) {
                    helpDialogs.show(HELP_LIBRARY)
                },
                LinearLayout.LayoutParams(
                    Ui.dp(this@MainActivity, 40), Ui.dp(this@MainActivity, 40),
                ),
            )
            addView(header)

            val actions = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            actions.addView(
                Ui.textView(
                    this@MainActivity,
                    "Проєкт = модель авто · томи додаються окремо.",
                    14f, Ui.muted,
                ).apply {
                    setPadding(0, Ui.dp(this@MainActivity, 10), 0, 0)
                },
            )
            val choices = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, Ui.dp(this@MainActivity, 8), 0, 0)
            }
            choices.addView(
                buildHomeChoiceCard("Новий том", "До проєкту", true) {
                    chooseProjectForVolume()
                },
                LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
                ).apply { marginEnd = Ui.dp(this@MainActivity, 5) },
            )
            choices.addView(
                buildHomeChoiceCard("Новий проєкт", "Створити модель", false) {
                    showCreateProjectDialog()
                },
                LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
                ).apply { marginStart = Ui.dp(this@MainActivity, 5) },
            )
            actions.addView(choices)
            actions.addView(
                buildProjectCatalogCard(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@MainActivity, 10) },
            )
            actions.addView(
                buildProjectFromFileCard(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@MainActivity, 10) },
            )
            homeAddPanelBody = BoundedAddActionsScrollView(this@MainActivity).apply {
                isFillViewport = false
                clipToPadding = false
                addView(actions)
            }
            addView(
                homeAddPanelBody,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            // v0.5.152 (TODO №25, M30s 19:41: «версія обрізана»): the version line was the last
            // row of the height-limited actions list, so on a smaller screen only its top showed.
            // Now it stands under the list, outside the scroll, and is always whole.
            homeVersionLine = Ui.textView(
                this@MainActivity,
                "v" + BuildConfig.VERSION_NAME + " · SAF reference mode",
                12f, Ui.muted,
            ).apply {
                setPadding(0, Ui.dp(this@MainActivity, 8), 0, 0)
            }
            addView(
                homeVersionLine,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            // Home status is inside Add but independent of the expandable actions.
            statusText = Ui.textView(
                this@MainActivity, "", 13f, Ui.statusWarmText,
            ).apply {
                visibility = View.GONE
                background = Ui.roundedBackground(
                    this@MainActivity, Ui.statusWarmFill,
                    Ui.statusWarmBorder, 10,
                )
                setPadding(
                    Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 8),
                    Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 8),
                )
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int,
                    ) = Unit
                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int,
                    ) {
                        visibility = if (s.isNullOrBlank() ||
                            s.toString() == DEFAULT_HOME_STATUS) View.GONE
                            else View.VISIBLE
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
            addView(
                statusText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@MainActivity, 8) },
            )

            operationStatus = OperationStatusView(this@MainActivity).apply {
                useProjectCompactLayout(restoredStatusDetailsExpanded)
            }
            addView(
                operationStatus,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@MainActivity, 8) },
            )
            trashStatus = OperationStatusView(this@MainActivity).apply {
                useProjectCompactLayout(true)
                visibility = View.GONE
            }
            addView(
                trashStatus,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@MainActivity, 8) },
            )
            post { updateHomeAddPanelUi() }
        }

    private fun homeAddHeaderButton(
        label: String,
        description: String,
        onClick: () -> Unit,
    ): TextView = Ui.textView(this, label, 17f, Ui.accent).apply {
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        contentDescription = description
        isClickable = true
        isFocusable = true
        background = Ui.roundedBackground(this@MainActivity, Ui.surface, Ui.border, 12)
        setOnClickListener { onClick() }
    }

    private fun isHomeLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun toggleHomeAddPanel() {
        if (isHomeLandscape()) {
            homeAddLandscapeExpanded = !homeAddLandscapeExpanded
        } else if (!homeAddPanelPinned) {
            homeAddPanelExpanded = !homeAddPanelExpanded
        }
        updateHomeAddPanelUi()
    }

    private fun updateHomeAddPanelUi() {
        if (!::homeAddPanelBody.isInitialized) return
        val landscape = isHomeLandscape()
        val pinnedHere = homeAddPanelPinned && !landscape
        val expandedHere = if (landscape) homeAddLandscapeExpanded else homeAddPanelExpanded
        homeAddPanelBody.visibility = if (expandedHere) View.VISIBLE else View.GONE
        if (::homeVersionLine.isInitialized) homeVersionLine.visibility = homeAddPanelBody.visibility
        homeAddPinButton.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            if (pinnedHere) null else monochromePinPaint,
        )
        homeAddPinButton.isEnabled = !landscape
        homeAddPinButton.isClickable = !landscape
        homeAddPinButton.isFocusable = !landscape
        homeAddPinButton.contentDescription = when {
            landscape -> "Закріплення недоступне в альбомному режимі"
            pinnedHere -> "Відкріпити панель Додати"
            else -> "Закріпити панель Додати"
        }
        homeAddToggleButton.text = if (expandedHere) "▲" else "▼"
        homeAddToggleButton.isClickable = !pinnedHere
        homeAddToggleButton.isFocusable = !pinnedHere
        homeAddToggleButton.alpha = if (pinnedHere) 0.45f else 1f
        homeAddToggleButton.contentDescription =
            if (expandedHere) "Згорнути панель Додати" else "Розгорнути панель Додати"
    }

    private fun buildHomeChoiceCard(
        title: String,
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
                    this@MainActivity,
                    86,
                )
            background =
                Ui.roundedBackground(
                    context =
                        this@MainActivity,
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
                    this@MainActivity,
                    10,
                ),
                Ui.dp(
                    this@MainActivity,
                    10,
                ),
                Ui.dp(
                    this@MainActivity,
                    10,
                ),
                Ui.dp(
                    this@MainActivity,
                    10,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
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
                    gravity =
                        Gravity.CENTER
                },
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
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
                            this@MainActivity,
                            4,
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

    private fun openProjectFolderExportPicker(projectId: String) {
        if (folderExportRunStore.load().isRunning ||
            rdprojectShareRunStore.load().isRunning
        ) {
            statusText.text = "Інший експорт проєкту вже виконується."
            return
        }
        pendingFolderExportProjectId = projectId
        val request = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(request, REQUEST_FOLDER_EXPORT_DESTINATION)
    }

    /** Called only by the confirm step of the export window with exact IDs and folder. */
    private fun startSeparateVolumeExport(
        projectId: String,
        folderUri: String,
        volumeIds: List<String>,
    ) {
        if (folderExportRunStore.load().isRunning ||
            rdprojectShareRunStore.load().isRunning
        ) {
            statusText.text = "Інший експорт проєкту вже виконується."
            return
        }
        val project = projectStore.project(projectId)
        val selected = runCatching {
            val current = requireNotNull(project) { "Проєкт не знайдено." }
            val plan = ProjectVolumeExportPlan.build(current, projectStore.volumes(projectId))
            ProjectVolumeExportPlan.select(plan, volumeIds)
        }.getOrElse {
            statusText.text = it.message ?: "Некоректний вибір томів."
            return
        }
        val folder = Uri.parse(folderUri)
        val granted = contentResolver.persistedUriPermissions.any {
            it.uri == folder && it.isReadPermission && it.isWritePermission
        }
        if (!granted) {
            statusText.text = "Доступ до папки втрачено. Відкрий експорт і вибери папку ще раз."
            return
        }
        if (ProjectFolderExportService.start(this, projectId, folder, selected.map { it.volumeId })) {
            statusText.text = "Почато експорт окремих .rdpkg. Каталог не змінюється."
        } else {
            statusText.text = "Не вдалося запустити окремий експорт. Перевірте стан томів."
        }
    }

    private fun refreshRdprojectShareRunState() {
        if (
            !::rdprojectShareRunStore.isInitialized ||
            !::operationStatus.isInitialized ||
            !::statusText.isInitialized
        ) {
            return
        }

        val folderState = folderExportRunStore.load()
        if (folderState.isRunning) {
            operationStatus.showRunning(
                title = "Експорт окремих томів",
                detail = folderState.message.ifBlank { "Експортую .rdpkg…" },
                current = (folderState.completed + folderState.skipped)
                    .takeIf { folderState.volumeIds.isNotEmpty() },
                total = folderState.volumeIds.size.takeIf { it > 0 },
                subject = folderState.projectId,
                onCancel = {
                    confirmDialogs.show(CONFIRM_STOP_FOLDER_EXPORT)
                },
            )
            return
        }
        if (folderState.isTerminal && !folderState.dismissed) {
            operationStatus.showTerminal(
                title = when (folderState.phase) {
                    ProjectFolderExportPhase.COMPLETE -> "Окремі томи · завершено"
                    ProjectFolderExportPhase.CANCELLED -> "Окремі томи · скасовано"
                    else -> "Окремі томи · помилка"
                },
                detail = folderState.message,
            ) {
                folderExportRunStore.dismiss(folderState.finishedAt)
                operationStatus.hide()
            }
            return
        }

        val state =
            rdprojectShareRunStore.load()

        if (
            state.isRunning
        ) {
            statusText.text =
                "Проєкт = модель авто · томи додаються окремо."

            val runningProject =
                state.projectId
                    ?.let {
                        projectStore.project(
                            it,
                        )
                    }
            val runningVolumes =
                runningProject
                    ?.let {
                        projectStore.volumes(
                            it.id,
                        )
                    }
                    .orEmpty()
            val operationSubject =
                if (
                    runningProject !=
                        null &&
                    runningVolumes.isNotEmpty()
                ) {
                    RdprojectExporter.defaultFileName(
                        project =
                            runningProject,
                        volumes =
                            runningVolumes,
                    )
                } else {
                    "Renodocs"
                }

            operationStatus.showRunning(
                title =
                    "Підготовка проєкту",
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
                    operationSubject,
                onCancel = {
                    confirmDialogs.show(CONFIRM_STOP_RDPROJECT)
                },
            )
            return
        }

        if (
            !state.isTerminal ||
            state.finishedAtMs <=
                0L ||
            state.isTerminalDismissed
        ) {
            return
        }

        if (
            state.finishedAtMs >
            lastRenderedRdprojectFinishedAt
        ) {
            lastRenderedRdprojectFinishedAt =
                state.finishedAtMs
            renderLibrary()
        }

        if (
            state.phase ==
            RdprojectShareRunPhase.COMPLETE
        ) {
            val file =
                state.preparedPath
                    ?.let(
                        ::File,
                    )

            if (
                file ==
                    null ||
                !file.isFile
            ) {
                // v0.5.107: a missing file is not a failed preparation. The
                // preparation finished; the file was removed afterwards.
                operationStatus.showTerminal(
                    title =
                        "Підготовка проєкту · файлу вже немає",
                    detail =
                        "Підготовлений .rdproject більше не знайдено — його видалено після підготовки. Проєкт і томи не змінено. Підготувати можна знову.",
                    outcome =
                        OperationTerminalOutcome.NEUTRAL,
                ) {
                    rdprojectShareRunStore
                        .dismissTerminal(
                            state.finishedAtMs,
                        )
                    operationStatus.hide()
                }
                return
            }

            operationStatus.showTerminal(
                title =
                    "Підготовка проєкту завершена",
                detail =
                    state.message
                        .ifBlank {
                            "Проєкт готовий для поширення."
                        },
            ) {
                rdprojectShareRunStore
                    .dismissTerminal(
                        state.finishedAtMs,
                    )
                operationStatus.hide()
            }

            if (
                !state.isChooserLaunched &&
                rdprojectShareRunStore
                    .markChooserLaunched(
                        state.finishedAtMs,
                    )
            ) {
                sharePreparedRdproject(
                    file,
                )
            }
        } else {
            operationStatus.showTerminal(
                title =
                    if (state.phase == RdprojectShareRunPhase.CANCELLED) {
                        "Підготовка проєкту · скасовано"
                    } else {
                        "Підготовка проєкту · помилка"
                    },
                detail =
                    state.message
                        .ifBlank {
                            "Не вдалося підготувати .rdproject."
                        },
            ) {
                rdprojectShareRunStore
                    .dismissTerminal(
                        state.finishedAtMs,
                    )
                operationStatus.hide()
            }
        }
    }

    private fun sharePreparedRdproject(
        file: File,
    ) {
        val uri =
            FileProvider.getUriForFile(
                this,
                packageName +
                    ".files",
                file,
            )

        val send =
            Intent(
                Intent.ACTION_SEND,
            ).apply {
                type =
                    "application/zip"
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri,
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

        startActivity(
            Intent.createChooser(
                send,
                "Поділитися проєктом",
            ),
        )
    }

    private fun renderLibrary() {
        libraryContainer.removeAllViews()

        val projects =
            projectStore.projects()

        libraryContainer.addView(
            Ui.textView(
                context = this,
                value = "Мої проєкти",
                sizeSp = 21f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    0,
                    Ui.dp(this@MainActivity, 8),
                    0,
                    Ui.dp(this@MainActivity, 10),
                )
            }
        )

        projects.forEach {
            project ->
            libraryContainer.addView(
                buildProjectTile(
                    project,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@MainActivity,
                            10,
                        )
                }
            )
        }

        // v0.5.112 (owner decision 2026-10-12): «Старі бібліотеки» (…_android
        // copies from «Legacy» / the converter) are no longer shown. Their
        // saved records stay untouched on the phone; nothing is deleted.
    }

    private fun buildProjectTile(
        project: RenaultProject,
    ): View {
        val volumes =
            projectStore
                .volumes(
                    project.id,
                )
        val volumeCount =
            volumes.size
        // #173: found by project id; may be a snapshot of older volumes.
        val preparedSnapshot =
            PreparedShareStore.projectSnapshot(
                this,
                project,
                volumes,
            )
        val hasPreparedShare =
            preparedSnapshot != null
        val preparedIsOlder =
            preparedSnapshot?.matches(volumes) == false

        return LinearLayout(this).apply {
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
                        this@MainActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        if (
                            volumeCount >
                            0
                        ) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                )
            setPadding(
                Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 14),
            )

            addView(
                View(
                    this@MainActivity,
                ).apply {
                    setBackgroundColor(
                        if (
                            volumeCount >
                            0
                        ) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@MainActivity,
                        4,
                    ),
                    Ui.dp(
                        this@MainActivity,
                        48,
                    ),
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@MainActivity,
                            14,
                        )
                },
            )

            val textColumn =
                LinearLayout(
                    this@MainActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            textColumn.addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        project.title,
                    sizeSp =
                        20f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            textColumn.addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        if (
                            volumeCount ==
                            0
                        ) {
                            "Порожній · додай том"
                        } else {
                            "Томів: " +
                                volumeCount
                        },
                    sizeSp =
                        14f,
                    color =
                        if (
                            volumeCount >
                            0
                        ) {
                            Ui.accent
                        } else {
                            Ui.muted
                        },
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(this@MainActivity, 5),
                        0,
                        0,
                    )
                }
            )

            addView(
                textColumn,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                )
            )

            if (hasPreparedShare) {
                addView(
                    Ui.textView(
                        context = this@MainActivity,
                        value = "⇧",
                        sizeSp = 20f,
                        color = if (preparedIsOlder) Ui.muted else Ui.accent,
                    ).apply {
                        contentDescription =
                            if (preparedIsOlder) {
                                "Підготовлений .rdproject — для попереднього набору томів"
                            } else {
                                "Підготовлений .rdproject готовий для передачі"
                            }
                        gravity = Gravity.CENTER
                        setPadding(
                            Ui.dp(this@MainActivity, 8),
                            0,
                            Ui.dp(this@MainActivity, 4),
                            0,
                        )
                    }
                )
            }

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        "⋮",
                    sizeSp =
                        26f,
                    color =
                        Ui.muted,
                ).apply {
                    contentDescription =
                        "Дії проєкту " +
                            project.title
                    gravity =
                        Gravity.CENTER
                    isClickable =
                        true
                    isFocusable =
                        true
                    setPadding(
                        Ui.dp(this@MainActivity, 10),
                        Ui.dp(this@MainActivity, 4),
                        Ui.dp(this@MainActivity, 10),
                        Ui.dp(this@MainActivity, 4),
                    )
                    setOnClickListener {
                        projectDialogs.showActions(
                            project.id,
                        )
                    }
                }
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        "›",
                    sizeSp =
                        28f,
                    color =
                        Ui.muted,
                )
            )

            setOnClickListener {
                startActivity(
                    ProjectActivity.intent(
                        context =
                            this@MainActivity,
                        projectId =
                            project.id,
                    )
                )
            }
        }
    }

    private fun helpSpec(
        helpId: String,
    ): HelpDialogSpec? =
        when (
            helpId
        ) {
            HELP_LIBRARY ->
                HelpDialogSpec(
                    title =
                        "Як влаштована бібліотека",
                    message =
                        "Проєкт — це модель автомобіля, наприклад Megane II або Laguna II.\n\n" +
                            "Том — окремий випуск документації для цієї моделі.\n\n" +
                            "«Додати том» веде до вибору проєкту. Рекомендований формат — .rdpkg. " +
                            "«Новий проєкт» створює нову модель у бібліотеці.\n\n" +
                            "Стару Renault-документацію відкрий у проєкті: «Створити .rdpkg з raw» (папка) або «… з архіву» — застосунок сам зробить з неї новий том.",
                )

            else ->
                null
        }

    private fun chooseProjectForVolume() {
        startActivity(
            Intent(
                this,
                ProjectChooserActivity::class.java,
            ),
        )
    }

    private fun showCreateProjectDialog() {
        startActivity(
            Intent(
                this,
                CreateProjectActivity::class.java,
            ),
        )
    }

    companion object {
        private const val CONFIRM_STOP_FOLDER_EXPORT =
            "stopFolderExport"
        private const val CONFIRM_STOP_RDPROJECT =
            "stopRdproject"
        private const val RDPROJECT_RUN_REFRESH_MS =
            100L
        private const val REQUEST_FOLDER_EXPORT_DESTINATION = 4103
        private const val STATE_PENDING_FOLDER_EXPORT_PROJECT =
            "main_pending_project_folder_export"
        private const val REQUEST_POST_NOTIFICATIONS = 4102
        private const val HELP_LIBRARY =
            "library"
        private const val DEFAULT_HOME_STATUS =
            "Проєкт = модель авто · томи додаються окремо."
        private const val STATE_HOME_ADD_EXPANDED = "main_home_add_expanded"
        private const val STATE_STATUS_DETAILS_EXPANDED = "main_status_details_expanded"
        private const val STATE_STATUS_TEXT =
            "main_status_text"
        private const val STATE_STATUS_COLOR =
            "main_status_color"
    }
}
