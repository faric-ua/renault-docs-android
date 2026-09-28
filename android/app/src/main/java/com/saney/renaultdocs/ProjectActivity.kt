package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ProjectActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var settings: AppSettings
    private lateinit var project: RenaultProject
    private lateinit var volumeContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var countText: TextView
    private var pendingManualImport =
        false
    private var pendingRdpkgExportVolumeId:
        String? =
        null

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        pendingRdpkgExportVolumeId =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_RDPKG_EXPORT_VOLUME_ID,
                )

        store =
            ProjectStore(
                this,
            )
        settings =
            AppSettings(
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

        setContentView(
            buildContent(),
        )
        render()
        repairSavedVolumeMetadata()

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

        outState.putString(
            STATE_PENDING_RDPKG_EXPORT_VOLUME_ID,
            pendingRdpkgExportVolumeId,
        )
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
        }
    }

    private fun handleRdpkgResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (
            resultCode !=
            RESULT_OK
        ) {
            statusText.text =
                "Імпорт .rdpkg скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri ==
            null
        ) {
            statusText.text =
                "Android не повернув адресу .rdpkg."
            return
        }

        persistReadPermission(
            uri =
                uri,
            returnedFlags =
                data.flags,
        )

        statusText.text =
            "Імпортую .rdpkg…"

        Thread {
            val result =
                RdpkgImporter.install(
                    context =
                        this,
                    packageUri =
                        uri,
                    progress = {
                        message ->
                        runOnUiThread {
                            if (
                                !isFinishing &&
                                !isDestroyed
                            ) {
                                statusText.text =
                                    message
                            }
                        }
                    },
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
                        imported ->
                        importPreparedVolume(
                            volume =
                                imported.volume,
                            allowOverride =
                                true,
                        )
                    }
                    .onFailure {
                        error ->
                        statusText.text =
                            "Не вдалося імпортувати .rdpkg: " +
                                (
                                    error.message
                                        ?: "невідома помилка"
                                )
                    }
            }
        }.start()
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

        statusText.text =
            "Експортую .rdpkg…"

        val appContext =
            applicationContext

        Thread {
            val result =
                RdpkgExporter.export(
                    context =
                        appContext,
                    volume =
                        volume,
                    destinationUri =
                        uri,
                    progress = {
                        message ->
                        runOnUiThread {
                            if (
                                !isFinishing &&
                                !isDestroyed
                            ) {
                                statusText.text =
                                    message
                            }
                        }
                    },
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
                        exported ->
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

                        statusText.text =
                            "Пакет .rdpkg збережено: " +
                                label +
                                " · " +
                                exported.fileCount +
                                " файлів\nSHA-256: " +
                                exported.sha256
                    }
                    .onFailure {
                        error ->
                        statusText.text =
                            "Не вдалося експортувати .rdpkg: " +
                                (
                                    error.message
                                        ?: "невідома помилка"
                                )
                    }
            }
        }.start()
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
                        pendingManualImport,
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

        val actionsRow =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        actionsRow.addView(
            buildProjectActionCard(
                title =
                    "Додати том",
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
                        6,
                    )
            },
        )

        actionsRow.addView(
            buildProjectActionCard(
                title =
                    "Ручне додавання",
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
                        6,
                    )
            },
        )

        root.addView(
            actionsRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        statusText =
            Ui.textView(
                context =
                    this,
                value =
                    "Натисни на том, щоб відкрити. Утримуй том — щоб видалити його з проєкту без видалення файлів.",
                sizeSp =
                    14f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ProjectActivity,
                        12,
                    ),
                    0,
                    Ui.dp(
                        this@ProjectActivity,
                        12,
                    ),
                )
            }

        root.addView(
            statusText,
        )

        val scroll =
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

        scroll.addView(
            volumeContainer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        return root
    }

    private fun buildProjectActionCard(
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

            addView(
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
            )

            addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        subtitle,
                    sizeSp =
                        12f,
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
                        "Проєкт порожній. Додай потрібний том.",
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
                volume.documentCode
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: volume.title

            textColumn.addView(
                Ui.textView(
                    context =
                        this@ProjectActivity,
                    value =
                        primaryTitle,
                    sizeSp =
                        19f,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                },
            )

            val details =
                buildList {
                    volume.date
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            add(
                                it,
                            )
                        }

                    if (
                        volume.documentCode
                            .isNullOrBlank() &&
                        volume.title !=
                            primaryTitle
                    ) {
                        add(
                            volume.title,
                        )
                    }
                }
                    .joinToString(
                        " · ",
                    )

            if (
                details.isNotBlank()
            ) {
                textColumn.addView(
                    Ui.textView(
                        context =
                            this@ProjectActivity,
                        value =
                            details,
                        sizeSp =
                            14f,
                        color =
                            Ui.accent,
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

            addView(
                textColumn,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

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
                confirmRemoveVolume(
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
            )
                .joinToString(
                    " · ",
                )
                .ifBlank {
                    volume.title
                }

        AlertDialog.Builder(
            this,
        )
            .setTitle(
                label,
            )
            .setItems(
                arrayOf(
                    "Експортувати .rdpkg",
                    "Видалити з проєкту",
                ),
            ) {
                _,
                which ->
                when (
                    which
                ) {
                    0 ->
                        startRdpkgExport(
                            volume,
                        )

                    1 ->
                        confirmRemoveVolume(
                            volume,
                        )
                }
            }
            .setNegativeButton(
                "Скасувати",
                null,
            )
            .show()
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
                null,
            )
            .setPositiveButton(
                "Видалити з проєкту",
            ) {
                _,
                _ ->
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
            .show()
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
            .show()
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
                    saveVolume(
                        volume,
                    )
                }
                .show()

            return
        }

        saveVolume(
            volume,
        )
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

    companion object {
        private const val EXTRA_PROJECT_ID =
            "projectId"
        private const val EXTRA_OPEN_PICKER =
            "openPicker"
        private const val REQUEST_VOLUME_FOLDER =
            4301
        private const val REQUEST_RDPKG_FILE =
            4302
        private const val REQUEST_RDPKG_EXPORT =
            4303
        private const val STATE_PENDING_RDPKG_EXPORT_VOLUME_ID =
            "pendingRdpkgExportVolumeId"

        fun intent(
            context: Context,
            projectId: String,
            openPicker: Boolean = false,
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
            }
    }
}
