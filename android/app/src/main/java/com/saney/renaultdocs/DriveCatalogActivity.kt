package com.saney.renaultdocs

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private data class DriveCatalogRetainedState(
    val catalog: DriveCatalog?,
    val selectedIds: ArrayList<String>,
)

class DriveCatalogActivity : Activity() {
    private lateinit var confirmDialogs: RestorableDialogs
    private lateinit var projectStore:
        ProjectStore
    private lateinit var importStore:
        CatalogImportRunStore
    private lateinit var container:
        LinearLayout
    private lateinit var statusText:
        TextView
    private lateinit var importButton:
        TextView
    // TODO №36: тихий сірий рядок «Останній імпорт…» замість великої картки.
    private lateinit var lastImportRow: LinearLayout
    private lateinit var lastImportText: TextView
    private var compactLastImport = false

    private lateinit var operationStatus:
        OperationStatusView

    private var catalog:
        DriveCatalog? =
        null

    private val selectedIds =
        linkedSetOf<String>()

    // 0.6.6 (#133 closed catalogue): Google's «allow reading Drive» question — once per screen.
    private var driveAsked =
        false
    private var driveWindowOpen =
        false

    // 0.6.7 (TODO №33): why Google gave no permission — shown next to «відкритий доступ»
    // instead of silently falling back to the public link.
    private var driveRefusal: String? =
        null

    private val handler =
        Handler(
            Looper.getMainLooper(),
        )

    private val refreshRunnable =
        object : Runnable {
            override fun run() {
                refreshImportState()
                handler.postDelayed(
                    this,
                    REFRESH_MS,
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        projectStore =
            ProjectStore(
                this,
            )
        importStore =
            CatalogImportRunStore(
                this,
            )

        driveAsked =
            savedInstanceState?.getBoolean(
                STATE_DRIVE_ASKED,
            ) == true
        driveWindowOpen =
            savedInstanceState?.getBoolean(
                STATE_DRIVE_WINDOW,
            ) == true

        // TODO №36: картку вже бачили під час минулого відкриття — тепер лише рядок.
        // Після повороту екрана лишається той вигляд, що був.
        compactLastImport =
            if (savedInstanceState != null) {
                savedInstanceState.getBoolean(STATE_COMPACT_LAST_IMPORT)
            } else {
                importStore.load().phase == CatalogImportPhase.COMPLETE &&
                    importStore.isTerminalSeen()
            }

        savedInstanceState
            ?.getStringArrayList(
                STATE_SELECTED_IDS,
            )
            ?.let {
                selectedIds.addAll(
                    it,
                )
            }

        val retainedState =
            lastNonConfigurationInstance
                as? DriveCatalogRetainedState

        retainedState
            ?.selectedIds
            ?.let {
                selectedIds.addAll(
                    it,
                )
            }

        catalog =
            retainedState
                ?.catalog

        confirmDialogs =
            RestorableDialogs(
                activity = this,
                stateKey = "catalogConfirm",
            ) { key ->
                if (key == CONFIRM_CANCEL_IMPORT) buildCancelImportDialog() else null
            }
        confirmDialogs.restore(
            savedInstanceState,
        )

        setContentView(
            buildContent(),
        )
        // v0.5.106: an open question comes back after rotation.
        window.decorView.post {
            confirmDialogs.restoreOpen()
        }

        val retainedCatalog =
            catalog

        if (
            retainedCatalog != null
        ) {
            statusText.setTextColor(
                Ui.accent,
            )
            statusText.text =
                catalogStatus(
                    retainedCatalog,
                )
            renderCatalog()
        } else if (driveWindowOpen) {
            // Google's window is still open (screen turned): its answer loads the catalogue.
            statusText.setTextColor(
                Ui.muted,
            )
            statusText.text =
                ASKING_GOOGLE_TEXT
        } else {
            loadCatalog()
        }
    }

    override fun onStart() {
        super.onStart()

        handler.removeCallbacks(
            refreshRunnable,
        )
        handler.post(
            refreshRunnable,
        )
    }

    override fun onDestroy() {
        if (::confirmDialogs.isInitialized) {
            confirmDialogs.onDestroy()
        }
        super.onDestroy()
    }

    override fun onStop() {
        handler.removeCallbacks(
            refreshRunnable,
        )
        super.onStop()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putBoolean(
            STATE_DRIVE_ASKED,
            driveAsked,
        )
        outState.putBoolean(
            STATE_DRIVE_WINDOW,
            driveWindowOpen,
        )
        outState.putBoolean(
            STATE_COMPACT_LAST_IMPORT,
            compactLastImport,
        )
        outState.putStringArrayList(
            STATE_SELECTED_IDS,
            ArrayList(
                selectedIds,
            ),
        )
        confirmDialogs.save(
            outState,
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    override fun onRetainNonConfigurationInstance():
        Any =
        DriveCatalogRetainedState(
            catalog =
                catalog,
            selectedIds =
                ArrayList(
                    selectedIds,
                ),
        )

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
                background =
                    Ui.emblemScreenBackground(context)
            }

        Ui.applySystemInsets(
            root,
        )

        val top =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        top.addView(
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

        top.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "Каталог Renodocs",
                sizeSp =
                    24f,
                color =
                    Ui.entityTitle,
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

        root.addView(
            top,
        )

        statusText =
            Ui.textView(
                context =
                    this,
                value =
                    "Завантажую каталог…",
                sizeSp =
                    Ui.secondaryTextSp,
                color =
                    Ui.muted,
            )

        root.addView(
            statusText,
        )

        operationStatus =
            OperationStatusView(
                this,
            ).apply {
                visibility =
                    View.GONE
            }

        root.addView(
            operationStatus,
        )

        lastImportText =
            Ui.textView(
                context =
                    this,
                value =
                    "",
                sizeSp =
                    Ui.secondaryTextSp,
                color =
                    Ui.muted,
            ).apply {
                contentDescription =
                    "Останній імпорт. Натисни, щоб побачити подробиці"
                setOnClickListener {
                    // Подробиці — та сама велика картка, лише до наступного відкриття.
                    compactLastImport =
                        false
                    refreshImportState()
                }
            }
        val lastImportClose =
            Ui.textView(
                context =
                    this,
                value =
                    "×",
                sizeSp =
                    20f,
                color =
                    Ui.muted,
            ).apply {
                gravity =
                    Gravity.CENTER
                minWidth =
                    Ui.dp(this@DriveCatalogActivity, 44)
                minHeight =
                    Ui.dp(this@DriveCatalogActivity, 44)
                contentDescription =
                    "Прибрати рядок «Останній імпорт»"
                setOnClickListener {
                    closeFinishedImport(CatalogImportPhase.COMPLETE)
                }
            }
        lastImportRow =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                visibility =
                    View.GONE
                addView(
                    lastImportText,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
                addView(
                    lastImportClose,
                )
            }

        root.addView(
            lastImportRow,
        )

        val scroll =
            ScrollView(
                this,
            )

        container =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@DriveCatalogActivity,
                        12,
                    ),
                    0,
                    Ui.dp(
                        this@DriveCatalogActivity,
                        12,
                    ),
                )
            }

        scroll.addView(
            container,
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        importButton =
            Ui.actionButton(
                context =
                    this,
                label =
                    "Імпортувати вибране",
                primary =
                    true,
            ) {
                startImport()
            }.apply {
                isEnabled =
                    false
            }

        root.addView(
            importButton,
        )

        return root
    }

    /**
     * 0.6.6: signed in → first ask Google for «read files on Drive» (once per screen; Google
     * shows its window only the first time). Without sign-in, or if the person says no —
     * the catalogue still loads the old public way while the owner's folder is open.
     */
    private fun loadCatalog() {
        if (
            !driveAsked &&
            DriveAccess.isSignedIn(this) &&
            DriveAccess.cachedToken() == null
        ) {
            driveAsked =
                true
            statusText.setTextColor(
                Ui.muted,
            )
            statusText.text =
                ASKING_GOOGLE_TEXT
            DriveAccess.ask(
                activity =
                    this,
                onNeedsWindow = { sender ->
                    try {
                        driveWindowOpen =
                            true
                        startIntentSenderForResult(
                            sender,
                            REQUEST_DRIVE_ACCESS,
                            null,
                            0,
                            0,
                            0,
                        )
                    } catch (error: Throwable) {
                        driveWindowOpen =
                            false
                        fetchCatalog()
                    }
                },
                onDone = { token, reason ->
                    driveRefusal =
                        if (token == null) shortReason(reason) else null
                    fetchCatalog()
                },
            )
            return
        }
        fetchCatalog()
    }

    /** The answer from Google's permission window. */
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?,
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data,
        )
        if (requestCode != REQUEST_DRIVE_ACCESS) {
            return
        }
        driveWindowOpen =
            false
        val token =
            if (resultCode == RESULT_OK) {
                DriveAccess.tokenFromResult(
                    this,
                    data,
                )
            } else {
                null
            }
        driveRefusal =
            if (token == null) DRIVE_NOT_GRANTED_TEXT else null
        if (::statusText.isInitialized) {
            fetchCatalog()
        }
    }

    private fun catalogStatus(
        loaded: DriveCatalog,
    ): String =
        "Каталог v" +
            loaded.catalogVersion +
            when (DriveCatalogClient.lastVia) {
                DriveVia.ACCOUNT -> " · через акаунт Google"
                DriveVia.PUBLIC ->
                    " · відкритий доступ" +
                        (driveRefusal?.let { " (Google: $it)" } ?: "")
                null -> ""
            }

    private fun shortReason(
        reason: String?,
    ): String {
        val text =
            reason?.trim()?.takeIf { it.isNotEmpty() } ?: DRIVE_NOT_GRANTED_TEXT
        return if (text.length > 80) text.take(77) + "…" else text
    }

    private fun fetchCatalog() {
        if (isFinishing || isDestroyed) {
            return
        }
        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Завантажую каталог…"

        Thread {
            runCatching {
                DriveCatalogClient
                    .loadCatalog(
                        applicationContext,
                    )
            }
                .onSuccess {
                    loaded ->
                    runOnUiThread {
                        catalog =
                            loaded
                        statusText.setTextColor(
                            Ui.accent,
                        )
                        statusText.text =
                            catalogStatus(
                                loaded,
                            )
                        renderCatalog()
                    }
                }
                .onFailure {
                    error ->
                    runOnUiThread {
                        statusText.setTextColor(
                            Ui.danger,
                        )
                        statusText.text =
                            if (error is DriveAccessException) {
                                error.message.orEmpty()
                            } else {
                                "Не вдалося завантажити каталог: " +
                                    (
                                        error.message
                                            ?: "невідома помилка"
                                    )
                            }
                    }
                }
        }.start()
    }

    private fun renderCatalog() {
        container.removeAllViews()

        val loaded =
            catalog
                ?: return

        if (
            loaded.projects.isEmpty()
        ) {
            container.addView(
                Ui.textView(
                    context =
                        this,
                    value =
                        "У каталозі поки немає проєктів.",
                    sizeSp =
                        16f,
                    color =
                        Ui.muted,
                ),
            )
            return
        }

        loaded.projects.forEach {
            project ->
            container.addView(
                projectCard(
                    project,
                ),
            )
        }

        updateSelectionSummary()
    }

    private fun projectCard(
        project: DriveCatalogProject,
    ): View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@DriveCatalogActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                )
            setPadding(
                Ui.dp(
                    this@DriveCatalogActivity,
                    16,
                ),
                Ui.dp(
                    this@DriveCatalogActivity,
                    14,
                ),
                Ui.dp(
                    this@DriveCatalogActivity,
                    16,
                ),
                Ui.dp(
                    this@DriveCatalogActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@DriveCatalogActivity,
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
                },
            )

            val meta =
                buildList {
                    val years =
                        when {
                            project.documentYearFrom !=
                                null &&
                                project.documentYearTo !=
                                null &&
                                project.documentYearFrom !=
                                project.documentYearTo ->
                                project.documentYearFrom
                                    .toString() +
                                    "–" +
                                    project.documentYearTo

                            project.documentYearFrom !=
                                null ->
                                project.documentYearFrom
                                    .toString()

                            else ->
                                null
                        }

                    years?.let {
                        add(
                            "Документація: $it",
                        )
                    }

                    add(
                        project.volumes.size
                            .toString() +
                            " " +
                            volumeNoun(
                                project.volumes.size,
                            ),
                    )
                }
                    .joinToString(
                        " · ",
                    )

            addView(
                Ui.textView(
                    context =
                        this@DriveCatalogActivity,
                    value =
                        meta,
                    sizeSp =
                        Ui.secondaryTextSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@DriveCatalogActivity,
                            4,
                        ),
                        0,
                        Ui.dp(
                            this@DriveCatalogActivity,
                            8,
                        ),
                    )
                },
            )

            project.volumes.forEach {
                volume ->
                addView(
                    volumeRow(
                        project =
                            project,
                        volume =
                            volume,
                    ),
                )
            }
        }

    private fun volumeRow(
        project: DriveCatalogProject,
        volume: DriveCatalogVolume,
    ): View {
        val installed =
            isInstalled(
                project =
                    project,
                volume =
                    volume,
            )

        return LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            setPadding(
                0,
                Ui.dp(
                    this@DriveCatalogActivity,
                    6,
                ),
                0,
                Ui.dp(
                    this@DriveCatalogActivity,
                    6,
                ),
            )

            val check =
                CheckBox(
                    this@DriveCatalogActivity,
                ).apply {
                    isChecked =
                        selectedIds.contains(
                            volume.id,
                        )
                    isEnabled =
                        !installed
                    setOnCheckedChangeListener {
                        _,
                        checked ->
                        if (
                            checked
                        ) {
                            selectedIds.add(
                                volume.id,
                            )
                        } else {
                            selectedIds.remove(
                                volume.id,
                            )
                        }

                        updateSelectionSummary()
                    }
                }

            addView(
                check,
            )

            val text =
                LinearLayout(
                    this@DriveCatalogActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            text.addView(
                Ui.textView(
                    context =
                        this@DriveCatalogActivity,
                    value =
                        listOfNotNull(
                            volume.documentCode,
                            volume.date,
                        )
                            .joinToString(
                                " · ",
                            ),
                    sizeSp =
                        16f,
                    color =
                        if (
                            installed
                        ) {
                            Ui.muted
                        } else {
                            Ui.text
                        },
                ),
            )

            val detail =
                buildList {
                    if (
                        volume.vehicleCodes.isNotEmpty()
                    ) {
                        add(
                            volume.vehicleCodes
                                .joinToString(
                                    " · ",
                                ),
                        )
                    }

                    volume.documentType
                        ?.let {
                            type ->
                            add(
                                if (
                                    volume.documentVersion
                                        .isNullOrBlank()
                                ) {
                                    type
                                } else {
                                    type +
                                        " v" +
                                        volume.documentVersion
                                },
                            )
                        }

                    volume.region
                        ?.let(
                            ::add,
                        )

                    // v0.5.129: «✓ Встановлено» moved to its own green
                    // line below (owner 2026-10-14 04:50); the size stays
                    // here only for volumes that are not installed yet.
                    if (
                        !installed
                    ) {
                        add(
                            formatSize(
                                volume.sizeBytes,
                            ),
                        )
                    }
                }
                    .joinToString(
                        " · ",
                    )

            text.addView(
                Ui.textView(
                    context =
                        this@DriveCatalogActivity,
                    value =
                        detail,
                    sizeSp =
                        Ui.secondaryTextSp,
                    color =
                        if (
                            installed
                        ) {
                            Ui.accent
                        } else {
                            Ui.muted
                        },
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@DriveCatalogActivity,
                            3,
                        ),
                        0,
                        0,
                    )
                },
            )

            if (
                installed
            ) {
                text.addView(
                    Ui.textView(
                        context =
                            this@DriveCatalogActivity,
                        value =
                            "✓ Встановлено",
                        sizeSp =
                            Ui.secondaryTextSp,
                        color =
                            Ui.success,
                    ).apply {
                        setPadding(
                            0,
                            Ui.dp(
                                this@DriveCatalogActivity,
                                2,
                            ),
                            0,
                            0,
                        )
                    },
                )
            }

            addView(
                text,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )
        }
    }

    private fun isInstalled(
        project: DriveCatalogProject,
        volume: DriveCatalogVolume,
    ): Boolean =
        projectStore.volumes(
            project.id,
        )
            .any {
                local ->
                local.documentCode
                    ?.equals(
                        volume.documentCode,
                        ignoreCase =
                            true,
                    ) ==
                    true &&
                    (
                        volume.date ==
                            null ||
                            local.date ==
                                volume.date
                    )
            }

    private fun updateSelectionSummary() {
        val count =
            selectedIds.size

        importButton.isEnabled =
            count >
                0 &&
                !importStore
                    .load()
                    .isRunning

        statusText.text =
            if (
                count >
                0
            ) {
                "Вибрано: " +
                    count +
                    " " +
                    volumeNoun(
                        count,
                    )
            } else {
                // TODO №35: тим самим рядком, що й після завантаження каталогу,
                // щоб не губилось «· через акаунт Google» / «· відкритий доступ».
                catalog
                    ?.let {
                        catalogStatus(
                            it,
                        )
                    }
                    ?: "Каталог"
            }
    }

    private fun startImport() {
        val loaded =
            catalog
                ?: return

        val items =
            loaded.projects
                .flatMap {
                    project ->
                    project.volumes
                        .filter {
                            selectedIds.contains(
                                it.id,
                            )
                        }
                        .map {
                            volume ->
                            CatalogImportItem(
                                projectId =
                                    project.id,
                                projectTitle =
                                    project.title,
                                volumeId =
                                    volume.id,
                                documentCode =
                                    volume.documentCode,
                                fileName =
                                    volume.fileName,
                                driveFileId =
                                    volume.driveFileId,
                                sizeBytes =
                                    volume.sizeBytes,
                                sha256 =
                                    volume.sha256,
                            )
                        }
                }

        if (
            items.isEmpty()
        ) {
            return
        }

        val started =
            CatalogImportService
                .start(
                    context =
                        this,
                    items =
                        items,
                )

        if (
            !started
        ) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Не вдалося запустити імпорт."
            return
        }

        importButton.isEnabled =
            false
        refreshImportState()
    }

    private fun refreshImportState() {
        if (
            !::operationStatus.isInitialized ||
            !::lastImportRow.isInitialized
        ) {
            return
        }

        val state =
            importStore.load()

        if (
            state.isRunning
        ) {
            // Новий імпорт: його підсумок спершу знову великою карткою (TODO №36).
            compactLastImport =
                false
            lastImportRow.visibility =
                View.GONE

            val item =
                state.items
                    .getOrNull(
                        state.itemIndex,
                    )

            val stopping =
                importStore.isCancelRequested()

            operationStatus.showRunning(
                title =
                    when (
                        state.phase
                    ) {
                        CatalogImportPhase.DOWNLOADING ->
                            "Завантаження з каталогу"

                        else ->
                            "Імпорт із каталогу"
                    },
                detail =
                    if (stopping) {
                        "Зупиняю після поточного безпечного кроку…"
                    } else {
                        state.message
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
                    item?.fileName
                        ?: "Renodocs",
                // v0.5.105: same «Скасувати» as the Home card, with a question.
                onCancel =
                    if (stopping) {
                        null
                    } else {
                        {
                            confirmCancelImport()
                        }
                    },
            )

            importButton.isEnabled =
                false
            return
        }

        if (
            !state.isTerminal
        ) {
            lastImportRow.visibility =
                View.GONE
            return
        }

        if (
            state.phase ==
            CatalogImportPhase.COMPLETE &&
            compactLastImport
        ) {
            operationStatus.hide()
            lastImportText.text =
                LastImportLine.text(
                    state.items.map { it.documentCode },
                    state.finishedAtMs,
                )
            lastImportRow.visibility =
                View.VISIBLE
            return
        }

        lastImportRow.visibility =
            View.GONE
        // Помилки й скасування лишаються великою карткою — їх треба помітити.
        importStore.markTerminalSeen()

        operationStatus.showTerminal(
            title =
                when (
                    state.phase
                ) {
                    CatalogImportPhase.COMPLETE ->
                        "Імпорт із каталогу завершено"

                    CatalogImportPhase.CANCELLED ->
                        "Імпорт із каталогу · скасовано"

                    else ->
                        "Імпорт із каталогу · помилка"
                },
            detail =
                state.message,
            onClose = {
                closeFinishedImport(
                    state.phase,
                )
            },
        )
    }

    private fun closeFinishedImport(
        phase: CatalogImportPhase,
    ) {
        importStore
            .clearTerminal()
        operationStatus.hide()
        lastImportRow.visibility =
            View.GONE
        compactLastImport =
            false

        if (
            phase ==
            CatalogImportPhase.COMPLETE
        ) {
            selectedIds.clear()
            renderCatalog()
        } else {
            updateSelectionSummary()
        }
    }

    /** v0.5.105: stopping a catalog download/import asks once. */
    private fun confirmCancelImport() {
        confirmDialogs.show(CONFIRM_CANCEL_IMPORT)
    }

    /** v0.5.106: rebuilt after rotation while the import still runs. */
    private fun buildCancelImportDialog(): Dialog? {
        if (!importStore.load().isRunning) return null
        return ConfirmActions.confirmStop(
            activity = this,
            title = "Зупинити імпорт із каталогу?",
            message = "Поточний том не буде додано. Уже додані томи лишаться. Файли на Google Drive не змінюються.",
        ) {
            importStore.requestCancel()
        }
    }

    private fun formatSize(
        bytes: Long,
    ): String {
        if (
            bytes <=
            0L
        ) {
            return "Розмір невідомий"
        }

        val mb =
            bytes /
                1024.0 /
                1024.0

        return String.format(
            java.util.Locale.US,
            "%.1f MB",
            mb,
        )
    }

    private fun volumeNoun(
        count: Int,
    ): String {
        val mod100 =
            count %
                100
        val mod10 =
            count %
                10

        return when {
            mod100 in
                11..14 ->
                "томів"

            mod10 ==
                1 ->
                "том"

            mod10 in
                2..4 ->
                "томи"

            else ->
                "томів"
        }
    }

    companion object {
        private const val CONFIRM_CANCEL_IMPORT =
            "cancelCatalogImport"
        private const val REFRESH_MS =
            250L
        private const val STATE_SELECTED_IDS =
            "selectedIds"
        private const val STATE_DRIVE_ASKED =
            "driveAsked"
        private const val STATE_DRIVE_WINDOW =
            "driveWindowOpen"
        private const val STATE_COMPACT_LAST_IMPORT =
            "compactLastImport"
        private const val REQUEST_DRIVE_ACCESS =
            6606
        private const val ASKING_GOOGLE_TEXT =
            "Питаю Google дозвіл читати каталог…"
        private const val DRIVE_NOT_GRANTED_TEXT =
            "дозвіл не дано"
    }
}
