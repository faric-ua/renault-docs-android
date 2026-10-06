package com.saney.renaultdocs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile

class ConversionActivity : Activity() {
    private lateinit var draftStore: ConversionDraftStore
    private lateinit var runStore: ConversionRunStore
    private lateinit var datasetStore: DatasetStore
    private lateinit var helpDialogs:
        LifecycleHelpDialogController

    private lateinit var sourceValue: TextView
    private lateinit var destinationValue: TextView
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var validateButton: Button
    private lateinit var startButton: Button
    private lateinit var cancelButton: Button
    private lateinit var registerButton: Button
    private lateinit var clearButton: Button
    private lateinit var sourceButton: Button
    private lateinit var destinationButton: Button

    private val uiHandler =
        Handler(
            Looper.getMainLooper(),
        )

    private val refreshRunnable =
        object : Runnable {
            override fun run() {
                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return
                }

                renderRunState()

                uiHandler.postDelayed(
                    this,
                    REFRESH_INTERVAL_MS,
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        draftStore =
            ConversionDraftStore(
                this,
            )
        runStore =
            ConversionRunStore(
                this,
            )
        datasetStore =
            DatasetStore(
                this,
            )

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
        renderDraft()
        renderRunState()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        helpDialogs.save(
            outState,
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    override fun onStart() {
        super.onStart()

        uiHandler.removeCallbacks(
            refreshRunnable,
        )
        uiHandler.post(
            refreshRunnable,
        )
        uiHandler.postDelayed(
            {
                reconcileInterruptedRun()
            },
            INTERRUPTED_RUN_RECHECK_MS,
        )
    }

    override fun onStop() {
        uiHandler.removeCallbacks(
            refreshRunnable,
        )

        super.onStop()
    }

    @Deprecated(
        "Uses platform SAF result for minSdk 26 compatibility."
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

        if (
            requestCode !=
            REQUEST_SOURCE_FOLDER &&
            requestCode !=
            REQUEST_DESTINATION_FOLDER
        ) {
            return
        }

        if (
            runStore.load()
                .isRunning
        ) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Конвертація вже виконується."
            return
        }

        if (
            resultCode !=
            RESULT_OK
        ) {
            statusText.setTextColor(
                Ui.muted,
            )
            statusText.text =
                "Вибір папки скасовано."
            return
        }

        val uri =
            data?.data

        if (
            uri == null
        ) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Android не повернув адресу вибраної папки."
            return
        }

        val returnedFlags =
            data?.flags ?: 0

        val name =
            DocumentFile
                .fromTreeUri(
                    this,
                    uri,
                )
                ?.name
                ?: uri.lastPathSegment

        // A newly selected source or destination starts a new conversion draft.
        // Do not keep the previous completed run visible underneath it.
        runStore.clearFinished()

        if (
            requestCode ==
            REQUEST_SOURCE_FOLDER
        ) {
            persistPermission(
                uri =
                    uri,
                returnedFlags =
                    returnedFlags,
                wantWrite =
                    false,
            )

            draftStore.saveSource(
                uri =
                    uri.toString(),
                name =
                    name,
            )

            statusText.setTextColor(
                Ui.muted,
            )
            statusText.text =
                "Source папку вибрано."
        } else {
            persistPermission(
                uri =
                    uri,
                returnedFlags =
                    returnedFlags,
                wantWrite =
                    true,
            )

            draftStore.saveDestination(
                uri =
                    uri.toString(),
                name =
                    name,
            )

            statusText.setTextColor(
                Ui.muted,
            )
            statusText.text =
                "Destination папку вибрано."
        }

        renderDraft()
        renderRunState()
    }

    private fun buildContent():
        View {
        window.statusBarColor =
            Ui.background
        window.navigationBarColor =
            Ui.background

        val root =
            LinearLayout(this).apply {
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
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        topBar.addView(
            ImageButton(this).apply {
                setImageResource(
                    R.drawable.ic_arrow_back,
                )
                contentDescription =
                    "Назад"
                setBackgroundColor(
                    android.graphics.Color.TRANSPARENT,
                )
                minimumWidth =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                minimumHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                setPadding(
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    ),
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
            )
        )

        topBar.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "Конвертація Renault",
                sizeSp =
                    24f,
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
            ).apply {
                marginStart =
                    Ui.dp(
                        this@ConversionActivity,
                        8,
                    )
            }
        )

        topBar.addView(
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_CONVERTER,
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

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
            }

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    ),
                    0,
                    Ui.dp(
                        this@ConversionActivity,
                        20,
                    ),
                )
            }

        body.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "Конвертер: source → staging/merge → нормалізація шляхів → package → validation → готовий *_android dataset. Якщо dataset цієї моделі вже існує, нові томи додаються в нього. Оригінал ніколи не видаляється.",
                sizeSp =
                    14f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    0,
                    0,
                    Ui.dp(
                        this@ConversionActivity,
                        16,
                    ),
                )
            }
        )

        body.addView(
            buildFolderCard(
                title =
                    "1. Стара Renault-папка",
                buttonText =
                    "Вибрати source",
                onClick = {
                    openTreePicker(
                        requestCode =
                            REQUEST_SOURCE_FOLDER,
                        wantWrite =
                            false,
                    )
                },
                valueHolder = {
                    sourceValue =
                        it
                },
                buttonHolder = {
                    sourceButton =
                        it
                },
            )
        )

        body.addView(
            buildFolderCard(
                title =
                    "2. Батьківська папка призначення",
                buttonText =
                    "Вибрати destination",
                onClick = {
                    openTreePicker(
                        requestCode =
                            REQUEST_DESTINATION_FOLDER,
                        wantWrite =
                            true,
                    )
                },
                valueHolder = {
                    destinationValue =
                        it
                },
                buttonHolder = {
                    destinationButton =
                        it
                },
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    )
            }
        )

        validateButton =
            Button(this).apply {
                text =
                    "Перевірити план"
                isAllCaps =
                    false
                minHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                Ui.applyActionStyle(
                    view =
                        this,
                    primary =
                        false,
                )
                setOnClickListener {
                    validatePlan()
                }
            }

        body.addView(
            validateButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        16,
                    )
            }
        )

        startButton =
            Button(this).apply {
                text =
                    "Почати конвертацію"
                isAllCaps =
                    false
                minHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        52,
                    )
                Ui.applyActionStyle(
                    view =
                        this,
                    primary =
                        true,
                )
                setOnClickListener {
                    startConversion()
                }
            }

        body.addView(
            startButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        8,
                    )
            }
        )

        cancelButton =
            Button(this).apply {
                text =
                    "Скасувати конвертацію"
                isAllCaps =
                    false
                minHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                visibility =
                    View.GONE
                Ui.applyActionStyle(
                    view =
                        this,
                    dangerAction =
                        true,
                )
                setOnClickListener {
                    ConversionService
                        .requestCancel(
                            this@ConversionActivity,
                        )

                    statusText.setTextColor(
                        Ui.muted,
                    )
                    statusText.text =
                        "Запит на скасування надіслано…"
                }
            }

        body.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        8,
                    )
            }
        )

        progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal,
            ).apply {
                max =
                    1000
                progress =
                    0
                isIndeterminate =
                    false
                visibility =
                    View.GONE
            }

        body.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(
                    this@ConversionActivity,
                    10,
                ),
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        16,
                    )
            }
        )

        progressText =
            Ui.textView(
                context =
                    this,
                value =
                    "",
                sizeSp =
                    13f,
                color =
                    Ui.muted,
            ).apply {
                visibility =
                    View.GONE
                setPadding(
                    0,
                    Ui.dp(
                        this@ConversionActivity,
                        8,
                    ),
                    0,
                    0,
                )
            }

        body.addView(
            progressText,
        )

        registerButton =
            Button(this).apply {
                text =
                    "Додати готову папку в бібліотеку"
                isAllCaps =
                    false
                minHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                visibility =
                    View.GONE
                Ui.applyActionStyle(
                    view =
                        this,
                    primary =
                        true,
                )
                setOnClickListener {
                    registerConvertedDataset()
                }
            }

        body.addView(
            registerButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    )
            }
        )

        clearButton =
            Button(this).apply {
                text =
                    "Очистити вибір"
                isAllCaps =
                    false
                minHeight =
                    Ui.dp(
                        this@ConversionActivity,
                        48,
                    )
                Ui.applyActionStyle(
                    view =
                        this,
                    primary =
                        false,
                )
                setOnClickListener {
                    if (
                        runStore.load()
                            .isRunning
                    ) {
                        return@setOnClickListener
                    }

                    draftStore.clear()
                    runStore.clearFinished()

                    statusText.setTextColor(
                        Ui.muted,
                    )
                    statusText.text =
                        "Вибір очищено."

                    renderDraft()
                    renderRunState()
                }
            }

        body.addView(
            clearButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        8,
                    )
            }
        )

        statusText =
            Ui.textView(
                context =
                    this,
                value =
                    "Вибери source і destination.",
                sizeSp =
                    14f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ConversionActivity,
                        16,
                    ),
                    0,
                    0,
                )
            }

        body.addView(
            statusText,
        )

        scroll.addView(
            body,
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

    private fun helpSpec(
        helpId: String,
    ): HelpDialogSpec? =
        when (
            helpId
        ) {
            HELP_CONVERTER ->
                HelpDialogSpec(
                    title =
                        "Як працює конвертер",
                    message =
                        "Виберіть оригінальну Renault-папку та папку, куди зберегти підготовлені дані. Оригінальні файли не змінюються.\n\n" +
                            "Перед початком програма перевіряє вибрані папки й показує план. Під час роботи цей екран можна закрити та відкрити знову.",
                )

            else ->
                null
        }

    private fun buildFolderCard(
        title: String,
        buttonText: String,
        onClick: () -> Unit,
        valueHolder:
            (
                TextView,
            ) -> Unit,
        buttonHolder:
            (
                Button,
            ) -> Unit,
    ): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@ConversionActivity,
                    fill =
                        Ui.surface,
                )
            setPadding(
                Ui.dp(
                    this@ConversionActivity,
                    16,
                ),
                Ui.dp(
                    this@ConversionActivity,
                    16,
                ),
                Ui.dp(
                    this@ConversionActivity,
                    16,
                ),
                Ui.dp(
                    this@ConversionActivity,
                    16,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@ConversionActivity,
                    value =
                        title,
                    sizeSp =
                        18f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            val value =
                Ui.textView(
                    context =
                        this@ConversionActivity,
                    value =
                        "Не вибрано",
                    sizeSp =
                        13f,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@ConversionActivity,
                            8,
                        ),
                        0,
                        Ui.dp(
                            this@ConversionActivity,
                            12,
                        ),
                    )
                }

            addView(
                value,
            )
            valueHolder(
                value,
            )

            val button =
                Button(
                    this@ConversionActivity,
                ).apply {
                    text =
                        buttonText
                    isAllCaps =
                        false
                    minHeight =
                        Ui.dp(
                            this@ConversionActivity,
                            48,
                        )
                    Ui.applyActionStyle(
                        view =
                            this,
                        primary =
                            false,
                    )
                    setOnClickListener {
                        onClick()
                    }
                }

            addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            )
            buttonHolder(
                button,
            )
        }

    private fun renderDraft() {
        val draft =
            draftStore.load()

        sourceValue.text =
            formatSelection(
                name =
                    draft.sourceName,
                uri =
                    draft.sourceUri,
            )

        destinationValue.text =
            formatSelection(
                name =
                    draft.destinationName,
                uri =
                    draft.destinationUri,
            )

        val hasPlan =
            !draft.sourceUri
                .isNullOrBlank() &&
                !draft.destinationUri
                    .isNullOrBlank()

        val running =
            runStore.load()
                .isRunning

        validateButton.isEnabled =
            hasPlan &&
                !running
        startButton.isEnabled =
            hasPlan &&
                !running
        sourceButton.isEnabled =
            !running
        destinationButton.isEnabled =
            !running
        clearButton.isEnabled =
            !running
    }

    private fun renderRunState() {
        val state =
            runStore.load()

        val running =
            state.isRunning

        val outputRegistered =
            state.outputTreeUri
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    outputUri ->
                    datasetStore
                        .load()
                        .any {
                            record ->
                            record.treeUri ==
                                outputUri
                        }
                }
                ?: false

        cancelButton.visibility =
            if (running) {
                View.VISIBLE
            } else {
                View.GONE
            }

        registerButton.visibility =
            if (
                state.phase ==
                ConversionRunPhase.COMPLETE &&
                !state.outputTreeUri
                    .isNullOrBlank() &&
                !outputRegistered
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        progressBar.visibility =
            if (
                running ||
                state.phase ==
                ConversionRunPhase.COMPLETE
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        progressText.visibility =
            if (
                progressBar.visibility ==
                View.VISIBLE
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val phaseLabel =
            when (
                state.phase
            ) {
                ConversionRunPhase.SCANNING ->
                    "Сканую…"

                ConversionRunPhase.PREPARING ->
                    "Готую…"

                ConversionRunPhase.COPYING ->
                    "Копіюю…"

                ConversionRunPhase.PACKAGING ->
                    "Пакую…"

                ConversionRunPhase.VALIDATING ->
                    "Перевіряю…"

                ConversionRunPhase.FINALIZING ->
                    "Завершую…"

                else ->
                    "Готую…"
            }

        if (
            state.phase ==
                ConversionRunPhase.SCANNING
        ) {
            progressBar.isIndeterminate =
                true
            progressText.text =
                if (
                    state.filesTotal >
                    0
                ) {
                    "Знайдено файлів: " +
                        state.filesTotal
                } else {
                    phaseLabel
                }
        } else if (
            state.filesTotal >
                0
        ) {
            progressBar.isIndeterminate =
                false

            val normalized =
                (
                    state.filesDone
                        .coerceIn(
                            0,
                            state.filesTotal,
                        )
                        .toLong() *
                        progressBar.max /
                        state.filesTotal
                            .coerceAtLeast(
                                1,
                            )
                ).toInt()

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.N
            ) {
                progressBar.setProgress(
                    normalized,
                    true,
                )
            } else {
                progressBar.progress =
                    normalized
            }

            progressText.text =
                phaseLabel +
                    " · Файлів: " +
                    state.filesDone
                        .coerceIn(
                            0,
                            state.filesTotal,
                        ) +
                    " / " +
                    state.filesTotal
        } else if (running) {
            progressBar.isIndeterminate =
                true
            progressText.text =
                phaseLabel
        } else {
            progressBar.isIndeterminate =
                false
            progressBar.progress =
                0
            progressText.text =
                ""
        }

        when (
            state.phase
        ) {
            ConversionRunPhase.IDLE ->
                Unit

            ConversionRunPhase.COMPLETE -> {
                statusText.setTextColor(
                    Ui.accent,
                )
                statusText.text =
                    state.message +
                        "\n\nOutput: " +
                        state.outputFolderName +
                        "\nОригінальна source-папка залишена без змін."
            }

            ConversionRunPhase.FAILED -> {
                statusText.setTextColor(
                    Ui.danger,
                )
                statusText.text =
                    state.message
            }

            ConversionRunPhase.CANCELLED -> {
                statusText.setTextColor(
                    Ui.muted,
                )
                statusText.text =
                    state.message
            }

            else -> {
                statusText.setTextColor(
                    Ui.muted,
                )
                statusText.text =
                    phaseLabel
            }
        }

        renderDraft()
    }

    private fun formatSelection(
        name: String?,
        uri: String?,
    ): String {
        if (
            uri.isNullOrBlank()
        ) {
            return "Не вибрано"
        }

        return SafDisplayPath
            .tree(
                uri,
            )
            ?: name
            ?: "Вибрана папка"
    }

    private fun openTreePicker(
        requestCode: Int,
        wantWrite: Boolean,
    ) {
        val intent =
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

                if (
                    wantWrite
                ) {
                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }

        @Suppress(
            "DEPRECATION",
        )
        startActivityForResult(
            intent,
            requestCode,
        )
    }

    private fun persistPermission(
        uri: Uri,
        returnedFlags: Int,
        wantWrite: Boolean,
    ) {
        var permissionFlags =
            returnedFlags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (
            wantWrite
        ) {
            permissionFlags =
                permissionFlags or
                    (
                        returnedFlags and
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
        }

        if (
            permissionFlags ==
            0
        ) {
            return
        }

        runCatching {
            contentResolver
                .takePersistableUriPermission(
                    uri,
                    permissionFlags,
                )
        }
    }

    private fun validatePlan() {
        runStore.clearFinished()
        renderRunState()

        validatedPlan()
            .onSuccess {
                plan ->
                val destination =
                    DocumentFile
                        .fromTreeUri(
                            this,
                            Uri.parse(
                                plan.destinationUri,
                            ),
                        )
                val existingOutput =
                    destination
                        ?.findFile(
                            plan.outputFolderName,
                        )
                        ?.takeIf {
                            it.isDirectory
                        }

                statusText.setTextColor(
                    Ui.accent,
                )

                statusText.text =
                    if (
                        existingOutput != null
                    ) {
                        "План готовий. Існуючий dataset буде доповнено новими томами:\n" +
                            plan.outputFolderName +
                            "\n\nSource не змінюється. Наявні томи не видаляються."
                    } else {
                        "План готовий. Output буде створено як:\n" +
                            plan.outputFolderName +
                            "\n\nSource не змінюється. Робота піде через staging."
                    }
            }
            .onFailure {
                error ->
                statusText.setTextColor(
                    Ui.danger,
                )
                statusText.text =
                    error.message
                        ?: "Не вдалося перевірити план."
            }
    }

    private fun startConversion() {
        if (
            runStore.load()
                .isRunning
        ) {
            return
        }

        validatedPlan()
            .onSuccess {
                plan ->
                runStore.clearFinished()

                ConversionService.start(
                    context =
                        this,
                    plan =
                        plan,
                )

                statusText.setTextColor(
                    Ui.muted,
                )
                statusText.text =
                    "Запускаю foreground-конвертацію…"

                renderRunState()
            }
            .onFailure {
                error ->
                statusText.setTextColor(
                    Ui.danger,
                )
                statusText.text =
                    error.message
                        ?: "Не вдалося запустити конвертацію."
            }
    }

    private fun validatedPlan():
        Result<ConversionPlan> {
        val draft =
            draftStore.load()

        return runCatching {
            val plan =
                ConversionPlanFactory.create(
                    sourceUri =
                        draft.sourceUri,
                    destinationUri =
                        draft.destinationUri,
                    sourceName =
                        draft.sourceName,
                )

            val source =
                DocumentFile
                    .fromTreeUri(
                        this,
                        Uri.parse(
                            plan.sourceUri,
                        ),
                    )
                    ?: error(
                        "Source папка більше недоступна."
                    )

            val destination =
                DocumentFile
                    .fromTreeUri(
                        this,
                        Uri.parse(
                            plan.destinationUri,
                        ),
                    )
                    ?: error(
                        "Destination папка більше недоступна."
                    )

            require(
                source.isDirectory,
            ) {
                "Source більше не є доступною папкою."
            }

            require(
                destination.isDirectory,
            ) {
                "Destination більше не є доступною папкою."
            }

            require(
                destination.canWrite(),
            ) {
                "Android не дав права запису в destination."
            }

            require(
                destination.findFile(
                    "renault-dataset.json",
                ) == null &&
                    !destination
                        .name
                        .orEmpty()
                        .endsWith(
                            "_android",
                            ignoreCase =
                                true,
                        )
            ) {
                "Destination має бути батьківською папкою, наприклад Documents/Renault. Не вибирай готову *_android папку."
            }

            destination
                .findFile(
                    plan.outputFolderName,
                )
                ?.let {
                    existingOutput ->
                    require(
                        existingOutput.isDirectory,
                    ) {
                        "Існуючий output не є папкою: " +
                            plan.outputFolderName
                    }

                    val existingTreeUri =
                        treeUriForDocument(
                            existingOutput.uri,
                        )

                    val existingRecord =
                        DatasetReader.read(
                            context =
                                this,
                            treeUri =
                                existingTreeUri,
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
                        "Output " +
                            plan.outputFolderName +
                            " належить іншому dataset: " +
                            existingRecord.title
                    }
                }

            plan
        }
    }

    private fun registerConvertedDataset() {
        val state =
            runStore.load()

        val outputTreeUri =
            state.outputTreeUri
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return

        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Перевіряю готовий dataset…"

        Thread {
            val outputUri =
                Uri.parse(
                    outputTreeUri,
                )

            val result =
                runCatching {
                    val outputRoot =
                        DocumentFile
                            .fromTreeUri(
                                this,
                                outputUri,
                            )
                            ?: error(
                                "Готова output-папка недоступна."
                            )

                    DatasetMediaIsolation.ensure(
                        context =
                            this,
                        root =
                            outputRoot,
                    )

                    DatasetReader.read(
                        context =
                            this,
                        treeUri =
                            outputUri,
                    ).getOrThrow()
                }

            runOnUiThread {
                result
                    .onSuccess {
                        record ->
                        datasetStore.upsert(
                            record,
                        )

                        statusText.setTextColor(
                            Ui.accent,
                        )
                        statusText.text =
                            "Додано в бібліотеку: " +
                                record.title +
                                " · томів: " +
                                record.volumeCount +
                                "\n\nDataset готовий до використання."
                    }
                    .onFailure {
                        error ->
                        statusText.setTextColor(
                            Ui.danger,
                        )
                        statusText.text =
                            "Output створено, але Library validation не пройшла: " +
                                (
                                    error.message
                                        ?: "невідома помилка"
                                )
                    }
            }
        }.start()
    }

    private fun reconcileInterruptedRun() {
        val state =
            runStore.load()

        if (
            !state.isRunning ||
            ConversionService.isActive()
        ) {
            return
        }

        if (
            state.mergeExisting
        ) {
            runStore.fail(
                "Попереднє додавання томів було перервано. Наявний dataset не видалено. Запусти конвертацію ще раз — merge staging буде очищено, а вже додані томи буде перевірено повторно."
            )
            renderRunState()
            return
        }

        val destinationUri =
            state.destinationUri
                ?.takeIf {
                    it.isNotBlank()
                }
        val outputFolderName =
            state.outputFolderName
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            destinationUri == null ||
            outputFolderName == null
        ) {
            runStore.fail(
                "Попередню конвертацію було перервано. Вибери папки та запусти її ще раз."
            )
            renderRunState()
            return
        }

        Thread {
            val recovered =
                runCatching {
                    val destination =
                        DocumentFile
                            .fromTreeUri(
                                this,
                                Uri.parse(
                                    destinationUri,
                                ),
                            )
                            ?: error(
                                "Destination недоступний."
                            )

                    val output =
                        destination
                            .findFile(
                                outputFolderName,
                            )
                            ?.takeIf {
                                it.isDirectory
                            }
                            ?: error(
                                "Готовий output ще не створений."
                            )

                    val outputTreeUri =
                        treeUriForDocument(
                            output.uri,
                        )

                    DatasetMediaIsolation.ensure(
                        context =
                            this,
                        root =
                            output,
                    )

                    val record =
                        DatasetReader.read(
                            context =
                                this,
                            treeUri =
                                outputTreeUri,
                        ).getOrThrow()

                    datasetStore.upsert(
                        record,
                    )

                    outputTreeUri
                }

            runOnUiThread {
                recovered
                    .onSuccess {
                        outputTreeUri ->
                        runStore.complete(
                            outputTreeUri =
                                outputTreeUri
                                    .toString(),
                            message =
                                "Попередній процес завершив файлову конвертацію до зупинки. Output перевірено та автоматично додано в бібліотеку.",
                            filesTotal =
                                state.filesTotal,
                            changedFiles =
                                state.changedFiles,
                            changesTotal =
                                state.changesTotal,
                        )
                    }
                    .onFailure {
                        runStore.fail(
                            "Попередню конвертацію було перервано. Source не змінено. Запусти конвертацію ще раз — стара staging-папка буде очищена автоматично."
                        )
                    }

                renderRunState()
            }
        }.start()
    }

    private fun treeUriForDocument(
        documentUri: Uri,
    ): Uri {
        val authority =
            documentUri.authority
                ?: error(
                    "Output URI не має authority."
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

    companion object {
        private const val HELP_CONVERTER =
            "converter"
        private const val REQUEST_SOURCE_FOLDER =
            4201
        private const val REQUEST_DESTINATION_FOLDER =
            4202

        private const val REFRESH_INTERVAL_MS =
            100L
        private const val INTERRUPTED_RUN_RECHECK_MS =
            1500L
    }
}
