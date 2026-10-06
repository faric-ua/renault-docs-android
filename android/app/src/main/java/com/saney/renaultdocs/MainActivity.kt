package com.saney.renaultdocs

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {
    private lateinit var libraryContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var operationProgress: ProgressBar
    private lateinit var operationStatus: OperationStatusView
    private lateinit var store: DatasetStore
    private lateinit var projectStore: ProjectStore
    private lateinit var settings: AppSettings
    private lateinit var helpDialogs:
        LifecycleHelpDialogController
    private lateinit var projectDialogs:
        HomeProjectDialogController
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
        settings = AppSettings(this)
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
                    renderLibrary()
                },
                onShareProgress = { current, total, message ->
                    operationProgress.visibility = View.GONE
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
                onShareFinished = { message ->
                    operationProgress.visibility = View.GONE
                    operationProgress.progress = 0
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
        repairSavedVolumeMetadata()
    }

    override fun onStart() {
        super.onStart()
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
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::libraryContainer.isInitialized) {
            renderLibrary()
        }
        refreshRdprojectShareRunState()
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

        if (requestCode != REQUEST_DATASET_FOLDER) {
            return
        }

        if (resultCode != RESULT_OK) {
            statusText.text = "Вибір папки скасовано."
            return
        }

        val uri = data?.data
        if (uri == null) {
            statusText.text = "Android не повернув адресу вибраної папки."
            return
        }

        persistReadPermission(uri, data.flags)

        DatasetReader.read(this, uri)
            .onSuccess { record ->
                store.upsert(record)
                statusText.text =
                    "Додано: " + record.title + " · томів: " + record.volumeCount
                renderLibrary()
            }
            .onFailure { error ->
                statusText.text =
                    "Це не готовий Renault dataset: " +
                        (error.message ?: "невідома помилка")
            }
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
            setBackgroundColor(Ui.background)
        }
        Ui.applySystemInsets(root)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topBar.addView(
            Ui.textView(
                context = this,
                value = "Renault Docs",
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

        scrollContent.addView(
            Ui.textView(
                context = this,
                value = "Бібліотека технічної документації",
                sizeSp = 15f,
                color = Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(this@MainActivity, 4),
                    0,
                    Ui.dp(this@MainActivity, 16),
                )
            }
        )

        scrollContent.addView(
            buildHomeAddPanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        scrollContent.addView(
            buildProjectCatalogCard(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@MainActivity,
                        10,
                    )
            },
        )

        statusText = Ui.textView(
            context = this,
            value = "Проєкт = модель Renault · томи додаються окремо.",
            sizeSp = 14f,
            color = Ui.muted,
        ).apply {
            setPadding(
                0,
                Ui.dp(this@MainActivity, 12),
                0,
                Ui.dp(this@MainActivity, 10),
            )
        }
        scrollContent.addView(statusText)

        operationStatus = OperationStatusView(this)
        scrollContent.addView(
            operationStatus,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = Ui.dp(this@MainActivity, 10)
            },
        )

        operationProgress =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal,
            ).apply {
                max = 100
                progress = 0
                visibility = View.GONE
            }
        scrollContent.addView(
            operationProgress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 4),
            ),
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

        scrollContent.addView(
            Ui.textView(
                context = this,
                value = "v" + BuildConfig.VERSION_NAME + " · SAF reference mode",
                sizeSp = 12f,
                color = Ui.muted,
            )
        )

        scroll.addView(
            scrollContent,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        )

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

    private fun buildHomeAddPanel():
        View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@MainActivity,
                    fill =
                        Ui.surfaceAlt,
                    stroke =
                        Ui.accent,
                )
            setPadding(
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
                Ui.dp(
                    this@MainActivity,
                    12,
                ),
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
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
            )

            val row =
                LinearLayout(
                    this@MainActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    setPadding(
                        0,
                        Ui.dp(
                            this@MainActivity,
                            8,
                        ),
                        0,
                        0,
                    )
                }

            row.addView(
                buildHomeChoiceCard(
                    title =
                        "Новий том",
                    subtitle =
                        "До проєкту",
                    primary =
                        true,
                ) {
                    chooseProjectForVolume()
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@MainActivity,
                            5,
                        )
                },
            )

            row.addView(
                buildHomeChoiceCard(
                    title =
                        "Новий проєкт",
                    subtitle =
                        "Створити модель",
                    primary =
                        false,
                ) {
                    showCreateProjectDialog()
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@MainActivity,
                            5,
                        )
                },
            )

            addView(
                row,
            )
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

    private fun buildToolsPanel():
        View =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@MainActivity,
                    fill =
                        Ui.surfaceAlt,
                    stroke =
                        Ui.border,
                )
            setPadding(
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
                Ui.dp(
                    this@MainActivity,
                    12,
                ),
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
                Ui.dp(
                    this@MainActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@MainActivity,
                    value =
                        "Інструменти",
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

            val row =
                LinearLayout(
                    this@MainActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    setPadding(
                        0,
                        Ui.dp(
                            this@MainActivity,
                            8,
                        ),
                        0,
                        0,
                    )
                }

            row.addView(
                buildToolCard(
                    title =
                        "Конвертер",
                    subtitle =
                        "+ стару Renault",
                ) {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            ConversionActivity::class.java,
                        )
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@MainActivity,
                            5,
                        )
                },
            )

            row.addView(
                buildToolCard(
                    title =
                        "Legacy",
                    subtitle =
                        "+ готова",
                ) {
                    openDatasetPicker()
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@MainActivity,
                            5,
                        )
                },
            )

            addView(
                row,
            )
        }

    private fun buildToolCard(
        title: String,
        subtitle: String,
        onClick: () -> Unit,
    ): View =
        FrameLayout(
            this,
        ).apply {
            isClickable =
                true
            isFocusable =
                true
            minimumHeight =
                Ui.dp(
                    this@MainActivity,
                    94,
                )
            background =
                Ui.roundedBackground(
                    context =
                        this@MainActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp =
                        12,
                )
            setPadding(
                Ui.dp(
                    this@MainActivity,
                    12,
                ),
                Ui.dp(
                    this@MainActivity,
                    10,
                ),
                Ui.dp(
                    this@MainActivity,
                    12,
                ),
                Ui.dp(
                    this@MainActivity,
                    10,
                ),
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
                        title,
                    sizeSp =
                        15f,
                    color =
                        Ui.text,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                    maxLines =
                        1
                },
            )

            textColumn.addView(
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
                    setPadding(
                        0,
                        Ui.dp(
                            this@MainActivity,
                            4,
                        ),
                        Ui.dp(
                            this@MainActivity,
                            46,
                        ),
                        0,
                    )
                },
            )

            addView(
                textColumn,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            addView(
                ImageView(
                    this@MainActivity,
                ).apply {
                    setImageResource(
                        R.drawable.ic_folder,
                    )
                    setColorFilter(
                        Ui.accent,
                    )
                    contentDescription =
                        "Папка"
                },
                FrameLayout.LayoutParams(
                    Ui.dp(
                        this@MainActivity,
                        38,
                    ),
                    Ui.dp(
                        this@MainActivity,
                        38,
                    ),
                    Gravity.END or Gravity.BOTTOM,
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@MainActivity,
                            2,
                        )
                    bottomMargin =
                        Ui.dp(
                            this@MainActivity,
                            2,
                        )
                },
            )

            setOnClickListener {
                onClick()
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

        val state =
            rdprojectShareRunStore.load()

        if (
            state.isRunning
        ) {
            operationProgress.visibility =
                View.GONE
            statusText.text =
                "Проєкт = модель Renault · томи додаються окремо."

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
                    "Renault Docs"
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
                operationStatus.showTerminal(
                    title =
                        "Підготовка проєкту · помилка",
                    detail =
                        "Підготовлений .rdproject не знайдено.",
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
                    "Підготовка проєкту · помилка",
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
                value = "Мої Renault",
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

        libraryContainer.addView(
            buildToolsPanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@MainActivity,
                        14,
                    )
            },
        )

        val records =
            store.load()

        val visibleLegacyRecords =
            records.filter {
                record ->
                val currentProject =
                    projectStore
                        .findProjectForModel(
                            record.model,
                        )

                currentProject ==
                    null ||
                    projectStore
                        .volumes(
                            currentProject.id,
                        )
                        .isEmpty()
            }

        if (
            visibleLegacyRecords.isNotEmpty()
        ) {
            libraryContainer.addView(
                Ui.textView(
                    context = this,
                    value = "Старі бібліотеки",
                    sizeSp = 15f,
                    color = Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(this@MainActivity, 18),
                        0,
                        Ui.dp(this@MainActivity, 8),
                    )
                }
            )

            visibleLegacyRecords.forEach {
                record ->
                libraryContainer.addView(
                    buildDatasetTile(record),
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
        }
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
        val hasPreparedShare =
            PreparedShareStore.hasProjectFile(
                this,
                project,
                volumes,
            )

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
                        color = Ui.accent,
                    ).apply {
                        contentDescription = "Підготовлений .rdproject готовий для передачі"
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

    private fun buildDatasetTile(
        record: DatasetRecord,
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            background = Ui.roundedBackground(
                context = this@MainActivity,
                fill = Ui.surface,
            )
            setPadding(
                Ui.dp(this@MainActivity, 18),
                Ui.dp(this@MainActivity, 16),
                Ui.dp(this@MainActivity, 18),
                Ui.dp(this@MainActivity, 16),
            )

            addView(
                Ui.textView(
                    context = this@MainActivity,
                    value = record.title,
                    sizeSp = 20f,
                    color = Ui.entityTitle,
                ).apply {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
            )

            val metadata = listOfNotNull(
                record.model,
                record.yearsLabel,
                record.platform,
            ).joinToString(" · ")

            addView(
                Ui.textView(
                    context = this@MainActivity,
                    value = metadata,
                    sizeSp = 15f,
                    color = Ui.muted,
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
                Ui.textView(
                    context = this@MainActivity,
                    value =
                        "Томів: " +
                            record.volumeCount +
                            " · відкриття: " +
                            when (
                                settings.defaultOpenMode
                            ) {
                                DatasetOpenMode.MODERN ->
                                    "Modern"

                                DatasetOpenMode.CLASSIC ->
                                    "Classic"
                            },
                    sizeSp = 13f,
                    color = Ui.accent,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(this@MainActivity, 10),
                        0,
                        0,
                    )
                }
            )

            setOnClickListener {
                openLegacyDataset(
                    record,
                )
            }
        }
    }

    private fun openLegacyDataset(
        record: DatasetRecord,
    ) {
        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Перевіряю стару бібліотеку: " +
                record.title +
                "…"

        Thread {
            val result =
                DatasetReader.read(
                    context =
                        this,
                    treeUri =
                        Uri.parse(
                            record.treeUri,
                        ),
                )

            runOnUiThread {
                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@runOnUiThread
                }

                result
                    .onSuccess {
                        fresh ->
                        store.upsert(
                            fresh,
                        )

                        statusText.setTextColor(
                            Ui.muted,
                        )
                        statusText.text =
                            "Відкриваю: " +
                                fresh.title

                        val intent =
                            when (
                                settings.defaultOpenMode
                            ) {
                                DatasetOpenMode.MODERN ->
                                    ModernDatasetActivity.intent(
                                        context =
                                            this,
                                        record =
                                            fresh,
                                    )

                                DatasetOpenMode.CLASSIC ->
                                    ViewerActivity.intent(
                                        context =
                                            this,
                                        record =
                                            fresh,
                                    )
                            }

                        startActivity(
                            intent,
                        )
                    }
                    .onFailure {
                        error ->
                        val migratedProject =
                            projectStore
                                .findProjectForModel(
                                    record.model,
                                )
                                ?.takeIf {
                                    projectStore
                                        .volumes(
                                            it.id,
                                        )
                                        .isNotEmpty()
                                }

                        if (migratedProject != null) {
                            statusText.setTextColor(
                                Ui.muted,
                            )
                            statusText.text =
                                "Старий запис «" +
                                    record.title +
                                    "» більше не має окремої папки. Відкриваю актуальний проєкт."
                            startActivity(
                                ProjectActivity.intent(
                                    context =
                                        this,
                                    projectId =
                                        migratedProject.id,
                                ),
                            )
                            return@onFailure
                        }

                        statusText.setTextColor(
                            Ui.danger,
                        )
                        statusText.text =
                            "Стара бібліотека «" +
                                record.title +
                                "» більше недоступна за збереженим шляхом. " +
                                "Якщо папку перенесено або видалено — додай її знову через Legacy. " +
                                "(" +
                                (
                                    error.message
                                        ?: "невідома помилка"
                                ) +
                                ")"
                    }
            }
        }.start()
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
                            "«Конвертер» і «Legacy» — сервісні інструменти для старих Renault-папок.",
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

    private fun openDatasetPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }

        @Suppress("DEPRECATION")
        startActivityForResult(
            intent,
            REQUEST_DATASET_FOLDER,
        )
    }

    private fun persistReadPermission(
        uri: Uri,
        returnedFlags: Int,
    ) {
        val grantedRead = returnedFlags and
            Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (grantedRead == 0) {
            return
        }

        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    companion object {
        private const val RDPROJECT_RUN_REFRESH_MS =
            100L
        private const val REQUEST_DATASET_FOLDER = 4101
        private const val HELP_LIBRARY =
            "library"
        private const val STATE_STATUS_TEXT =
            "main_status_text"
        private const val STATE_STATUS_COLOR =
            "main_status_color"
    }
}
