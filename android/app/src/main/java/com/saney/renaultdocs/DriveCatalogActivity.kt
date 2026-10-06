package com.saney.renaultdocs

import android.app.Activity
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
    private lateinit var operationStatus:
        OperationStatusView

    private var catalog:
        DriveCatalog? =
        null

    private val selectedIds =
        linkedSetOf<String>()

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

        setContentView(
            buildContent(),
        )

        val retainedCatalog =
            catalog

        if (
            retainedCatalog != null
        ) {
            statusText.setTextColor(
                Ui.accent,
            )
            statusText.text =
                "Каталог v" +
                    retainedCatalog.catalogVersion
            renderCatalog()
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

    override fun onStop() {
        handler.removeCallbacks(
            refreshRunnable,
        )
        super.onStop()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putStringArrayList(
            STATE_SELECTED_IDS,
            ArrayList(
                selectedIds,
            ),
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
                setBackgroundColor(
                    Ui.background,
                )
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
                    "Каталог Renault Docs",
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

    private fun loadCatalog() {
        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Завантажую каталог…"

        Thread {
            runCatching {
                DriveCatalogClient
                    .loadCatalog()
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
                            "Каталог v" +
                                loaded.catalogVersion
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
                            "Не вдалося завантажити каталог: " +
                                (
                                    error.message
                                        ?: "невідома помилка"
                                )
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
                    if (
                        project.vehicleCodes.isNotEmpty()
                    ) {
                        add(
                            project.vehicleCodes
                                .joinToString(
                                    " · ",
                                ),
                        )
                    }

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

                    add(
                        if (
                            installed
                        ) {
                            "✓ Встановлено"
                        } else {
                            formatSize(
                                volume.sizeBytes,
                            )
                        },
                    )
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
                catalog
                    ?.let {
                        "Каталог v" +
                            it.catalogVersion
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
            !::operationStatus.isInitialized
        ) {
            return
        }

        val state =
            importStore.load()

        if (
            state.isRunning
        ) {
            val item =
                state.items
                    .getOrNull(
                        state.itemIndex,
                    )

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
                    state.message,
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
                        ?: "Renault Docs",
            )

            importButton.isEnabled =
                false
            return
        }

        if (
            !state.isTerminal
        ) {
            return
        }

        operationStatus.showTerminal(
            title =
                if (
                    state.phase ==
                    CatalogImportPhase.COMPLETE
                ) {
                    "Імпорт із каталогу завершено"
                } else {
                    "Імпорт із каталогу · помилка"
                },
            detail =
                state.message,
            onClose = {
                importStore
                    .clearTerminal()
                operationStatus.hide()

                if (
                    state.phase ==
                    CatalogImportPhase.COMPLETE
                ) {
                    selectedIds.clear()
                    renderCatalog()
                } else {
                    updateSelectionSummary()
                }
            },
        )
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
        private const val REFRESH_MS =
            250L
        private const val STATE_SELECTED_IDS =
            "selectedIds"
    }
}
