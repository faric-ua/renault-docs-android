package com.saney.renaultdocs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.net.Uri
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
    private lateinit var helpDialogs:
        LifecycleHelpDialogController
    private lateinit var confirmDialogs: RestorableDialogs

    private lateinit var sourceValue: TextView
    private lateinit var destinationValue: TextView
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var validateButton: Button
    private lateinit var startButton: Button
    private lateinit var cancelButton: Button
    private lateinit var clearButton: Button
    private lateinit var addToAppButton: Button
    private lateinit var datasetStore: DatasetStore
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

    // Recovery reconciliation is UI-owned, not a second worker. Cancel
    // its delayed attempt when the Activity is no longer visible.
    private var activityStarted = false
    private val interruptedRunCheck =
        Runnable {
            if (activityStarted) {
                reconcileInterruptedRun()
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
        confirmDialogs =
            RestorableDialogs(
                activity = this,
                stateKey = "conversionConfirm",
            ) { key ->
                when (key) {
                    CONFIRM_CANCEL_CONVERSION -> buildCancelConversionDialog()
                    CONFIRM_CLEAR_SELECTION -> buildClearSelectionDialog()
                    else -> null
                }
            }
        confirmDialogs.restore(
            savedInstanceState,
        )

        setContentView(
            buildContent(),
        )
        helpDialogs.restoreOpen()
        // v0.5.106: an open question comes back after rotation.
        window.decorView.post {
            confirmDialogs.restoreOpen()
        }
        renderDraft()
        renderRunState()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        helpDialogs.save(
            outState,
        )
        confirmDialogs.save(
            outState,
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    override fun onDestroy() {
        if (::confirmDialogs.isInitialized) {
            confirmDialogs.onDestroy()
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true

        uiHandler.removeCallbacks(
            refreshRunnable,
        )
        uiHandler.removeCallbacks(
            interruptedRunCheck,
        )
        uiHandler.post(
            refreshRunnable,
        )
        uiHandler.postDelayed(
            interruptedRunCheck,
            INTERRUPTED_RUN_RECHECK_MS,
        )
    }

    override fun onStop() {
        activityStarted = false
        uiHandler.removeCallbacks(
            refreshRunnable,
        )
        uiHandler.removeCallbacks(
            interruptedRunCheck,
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
                "Стару папку вибрано."
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
                "Папку призначення вибрано."
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
                    "Конвертер: стара папка → тимчасова папка (додавання томів) → нормалізація шляхів → пакет → перевірка → готова копія *_android. Якщо готова копія цієї моделі вже існує, нові томи додаються в неї. Оригінал ніколи не видаляється.",
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
                    "Вибрати стару папку",
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
                    "Вибрати папку призначення",
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
                    confirmCancelConversion()
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

        progressBar = SharedOperationProgressBar.create(this).apply {
            visibility = View.GONE
        }

        body.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(
                    this@ConversionActivity,
                    SharedOperationProgressBar.HEIGHT_DP,
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
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.MIDDLE
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

        // v0.5.110 (owner): the copy is for a browser first, but it can also
        // be opened from the app — only when the owner presses this button.
        addToAppButton =
            Button(this).apply {
                text =
                    "Додати копію в застосунок"
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
                    addCopyToApp()
                }
            }

        body.addView(
            addToAppButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@ConversionActivity,
                        12,
                    )
            },
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

                    confirmClearSelection()
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
                    "Вибери стару папку і папку призначення.",
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
                    // v0.5.108 (owner): explain what the converter is for, what to
                    // choose and what happens, in plain words.
                    message =
                        "НАВІЩО ЦЕ\n" +
                            "Документація Renault для Windows — це папки з HTML-сторінками (INDEX.HTM / ACCUEIL.HTM), які відкриваються в браузері. " +
                            "На телефоні ті самі папки часто відкриваються з помилками: Windows не розрізняє великі й малі літери в назвах файлів, а Android розрізняє. " +
                            "Через це посилання ламаються — частина сторінок і схем не відкривається.\n\n" +
                            "Конвертер робить з такої папки копію для Android. Усередині — ті самі оригінальні сторінки, без переробки «на новий лад», лише з виправленими посиланнями. " +
                            "Копія відкривається в браузері так само, як оригінал на Windows: стартова сторінка тому (INDEX.HTM) або загальний список томів _renault/START.html. " +
                            "Застосунок для цього не потрібен: папку можна запакувати й передати будь-кому. Сама в застосунок вона не додається; якщо хочеш відкривати її і тут — після конвертації натисни «Додати копію в застосунок», і вона з'явиться на головному в розділі «Старі бібліотеки».\n\n" +
                            "ЩО ВИБРАТИ\n" +
                            "1. Стара Renault-папка — папка, в якій лежать папки томів (у кожній є INDEX або ACCUEIL). Не сама папка тому! " +
                            "Наприклад, для Kangoo: «…(RUS)», а не внутрішня «…2009.08.31».\n" +
                            "2. Батьківська папка призначення — куди покласти копію, наприклад Documents/Renault. Там з'явиться нова папка з назвою …_android. " +
                            "Не вибирай саму готову …_android.\n\n" +
                            "ЯК ЦЕ ПРАЦЮЄ\n" +
                            "• Сканування — рахує файли й шукає томи.\n" +
                            "• Копіювання — у приховану тимчасову папку; там виправляються великі й малі літери в посиланнях.\n" +
                            "• Пакування — додається список томів (START.html), опис набору (renault-dataset.json) і звіт (conversion-report.json).\n" +
                            "• Перевірка — що скопійовано всі файли і відкривається стартова сторінка.\n" +
                            "• Готово — лише після перевірки тимчасова папка стає готовою копією …_android. Якщо щось пішло не так, недороблена копія не з'являється.\n\n" +
                            "Якщо копія цієї моделі вже є, нові томи додаються в неї, а наявні не видаляються.\n\n" +
                            "БЕЗПЕКА\n" +
                            "• Стара папка тільки читається: у ній нічого не змінюється і не видаляється.\n" +
                            "• «Перевірити план» показує, що буде зроблено, нічого не копіюючи.\n" +
                            "• «Скасувати конвертацію» спершу питає; видаляється лише тимчасова папка.\n" +
                            "• Цей екран можна закрити: робота йде у фоні, прогрес видно у шторці.",
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

        val copyInApp =
            state.phase == ConversionRunPhase.COMPLETE &&
                datasetStore.load().any { record ->
                    record.treeUri == state.outputTreeUri
                }
        addToAppButton.visibility =
            if (
                state.phase == ConversionRunPhase.COMPLETE &&
                !state.destinationUri.isNullOrBlank() &&
                !state.outputFolderName.isNullOrBlank() &&
                !copyInApp
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        cancelButton.visibility =
            if (running) {
                View.VISIBLE
            } else {
                View.GONE
            }

        progressBar.visibility =
            if (
                running ||
                state.phase in setOf(
                    ConversionRunPhase.COMPLETE,
                    ConversionRunPhase.FAILED,
                    ConversionRunPhase.CANCELLED,
                )
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

        progressText.setTextColor(Ui.muted)
        when {
            state.phase == ConversionRunPhase.SCANNING -> {
                SharedOperationProgressBar.render(progressBar, null, null)
                progressText.text = if (state.filesTotal > 0) {
                    OperationProgress(
                        stage = phaseLabel,
                        itemCurrent = state.filesTotal,
                        itemLabel = "Знайдено файлів",
                    ).displayText()
                } else {
                    phaseLabel
                }
            }

            state.phase == ConversionRunPhase.COMPLETE -> {
                SharedOperationProgressBar.render(progressBar, null, null, completed = true)
                progressText.text = "Завершено"
            }

            state.phase == ConversionRunPhase.FAILED -> {
                SharedOperationProgressBar.render(
                    progressBar, null, null, completed = true, color = Ui.danger,
                )
                progressText.setTextColor(Ui.danger)
                // v0.5.107: the full reason is shown once, in the status text
                // below; the bar line only says that the run failed.
                progressText.text = "Помилка конвертації"
            }

            state.phase == ConversionRunPhase.CANCELLED -> {
                SharedOperationProgressBar.render(
                    progressBar, null, null, completed = true, color = Ui.warning,
                )
                progressText.setTextColor(Ui.warning)
                progressText.text = "Операцію скасовано"
            }

            state.filesTotal > 0 -> {
                SharedOperationProgressBar.render(
                    progressBar,
                    state.filesDone,
                    state.filesTotal,
                )
                progressText.text = OperationProgress.measured(
                    stage = phaseLabel,
                    current = state.filesDone,
                    total = state.filesTotal,
                    itemCurrent = state.filesDone,
                    itemTotal = state.filesTotal,
                    itemLabel = "Файлів",
                ).displayText()
            }

            running -> {
                SharedOperationProgressBar.render(progressBar, null, null)
                progressText.text = phaseLabel
            }

            else -> {
                SharedOperationProgressBar.render(progressBar, null, null)
                progressText.text = ""
            }
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
                        "\n\nГотова копія: " +
                        state.outputFolderName +
                        "\nСтара папка залишена без змін."
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
                        "План готовий. Наявну готову копію буде доповнено новими томами:\n" +
                            plan.outputFolderName +
                            "\n\nСтара папка не змінюється. Наявні томи не видаляються."
                    } else {
                        "План готовий. Готову копію буде створено як:\n" +
                            plan.outputFolderName +
                            "\n\nСтара папка не змінюється. Робота піде через тимчасову папку."
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

    /**
     * v0.5.110: open the finished browser copy from the app too. The copy is
     * found inside the permitted destination (a separate sub-folder tree
     * address has no Android permission — v0.5.109 history) and appears on
     * Home in «Старі бібліотеки».
     */
    private fun addCopyToApp() {
        val state = runStore.load()
        val destinationUri = state.destinationUri
        val outputFolderName = state.outputFolderName
        if (state.phase != ConversionRunPhase.COMPLETE || destinationUri.isNullOrBlank() || outputFolderName.isNullOrBlank()) {
            return
        }
        addToAppButton.isEnabled = false
        statusText.setTextColor(Ui.muted)
        statusText.text = "Перевіряю копію…"

        Thread {
            val added =
                runCatching {
                    val destination =
                        DocumentFile.fromTreeUri(this, Uri.parse(destinationUri))
                            ?: error("Папка призначення недоступна.")
                    val output =
                        destination.findFile(outputFolderName)
                            ?.takeIf { it.isDirectory }
                            ?: error("Готову копію не знайдено: $outputFolderName")
                    DatasetMediaIsolation.ensure(
                        context = this,
                        root = output,
                    )
                    val record =
                        DatasetReader.readFolder(
                            context = this,
                            root = output,
                        ).getOrThrow()
                    datasetStore.upsert(record)
                    // Remember the in-tree address so the button hides.
                    runStore.complete(
                        outputTreeUri = record.treeUri,
                        message = state.message,
                        filesTotal = state.filesTotal,
                        changedFiles = state.changedFiles,
                        changesTotal = state.changesTotal,
                    )
                    record
                }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                addToAppButton.isEnabled = true
                added
                    .onSuccess { record ->
                        statusText.setTextColor(Ui.accent)
                        statusText.text =
                            "Додано в застосунок: " + record.title +
                                ". Відкривається на головному екрані в розділі «Старі бібліотеки»."
                    }
                    .onFailure { error ->
                        statusText.setTextColor(Ui.danger)
                        statusText.text =
                            "Не вдалося додати копію в застосунок: " +
                                (error.message ?: "невідома помилка")
                    }
                renderRunState()
            }
        }.start()
    }

    /** v0.5.104: owner asked for a confirmation before stopping the converter. */
    private fun confirmCancelConversion() {
        confirmDialogs.show(CONFIRM_CANCEL_CONVERSION)
    }

    /** v0.5.106: rebuilt after rotation from the current run state. */
    private fun buildCancelConversionDialog(): Dialog? {
        if (!runStore.load().isRunning) return null
        return ConfirmActions.confirmStop(
            activity = this,
            title = "Зупинити конвертацію?",
            message = "Недороблена тимчасова копія буде видалена. Стара Renault-папка не зміниться. Почати можна буде знову.",
        ) {
            if (!runStore.load().isRunning) {
                renderRunState()
                return@confirmStop
            }
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

    /** v0.5.104: clearing the chosen folders and the last result asks first. */
    private fun confirmClearSelection() {
        confirmDialogs.show(CONFIRM_CLEAR_SELECTION)
    }

    private fun buildClearSelectionDialog(): Dialog? {
        if (runStore.load().isRunning) return null
        return ConfirmActions.confirm(
            activity = this,
            title = "Очистити вибір?",
            message = "Вибрані папки і результат останньої конвертації зникнуть з цього екрана. Файли на телефоні не видаляються.",
            confirmLabel = "Очистити",
            keepLabel = "Залишити",
        ) {
            if (runStore.load().isRunning) {
                return@confirm
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
                    "Запускаю конвертацію…"

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
                        "Стара папка більше недоступна."
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
                        "Папка призначення більше недоступна."
                    )

            require(
                source.isDirectory,
            ) {
                "Стара папка більше не доступна як папка."
            }

            require(
                destination.isDirectory,
            ) {
                "Папка призначення більше не доступна як папка."
            }

            require(
                destination.canWrite(),
            ) {
                "Android не дав права запису в папку призначення."
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
                "Папка призначення має бути батьківською, наприклад Documents/Renault. Не вибирай готову *_android папку."
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
                        "Наявна готова копія не є папкою: " +
                            plan.outputFolderName
                    }

                    // v0.5.108: read inside the permitted destination folder.
                    val existingRecord =
                        DatasetReader.readFolder(
                            context =
                                this,
                            root =
                                existingOutput,
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
                }

            plan
        }
    }

    private fun reconcileInterruptedRun() {
        val state =
            runStore.load()

        if (
            !activityStarted ||
            !state.isRunning ||
            ConversionService.isActive()
        ) {
            return
        }

        if (
            state.mergeExisting
        ) {
            runStore.fail(
                "Попереднє додавання томів було перервано. Наявну готову копію не видалено. Запусти конвертацію ще раз — тимчасову папку буде очищено, а вже додані томи буде перевірено повторно."
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
                                "Папка призначення недоступна."
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
                                "Готову копію ще не створено."
                            )

                    DatasetMediaIsolation.ensure(
                        context =
                            this,
                        root =
                            output,
                    )

                    // v0.5.108: only check the copy (inside the permitted
                    // destination); it is a browser folder, not a library entry.
                    DatasetReader.readFolder(
                        context =
                            this,
                        root =
                            output,
                    ).getOrThrow()

                    output.uri
                }

            runOnUiThread {
                // A foreground service may have redelivered while filesystem
                // validation was running. Never overwrite its fresh state
                // (or a newer request) with a stale Activity-side result.
                val currentState = runStore.load()
                if (
                    !activityStarted ||
                    isFinishing ||
                    isDestroyed ||
                    ConversionService.isActive() ||
                    !currentState.isRunning ||
                    currentState.startedAtMs != state.startedAtMs ||
                    currentState.sourceUri != state.sourceUri ||
                    currentState.destinationUri != state.destinationUri ||
                    currentState.outputFolderName != state.outputFolderName
                ) {
                    return@runOnUiThread
                }
                recovered
                    .onSuccess {
                        outputTreeUri ->
                        runStore.complete(
                            outputTreeUri =
                                outputTreeUri
                                    .toString(),
                            message =
                                "Попередній процес завершив конвертацію до зупинки. Копію для браузера перевірено — вона готова.",
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
                            "Попередню конвертацію було перервано. Стару папку не змінено. Запусти конвертацію ще раз — стара тимчасова папка буде очищена автоматично."
                        )
                    }

                renderRunState()
            }
        }.start()
    }

    companion object {
        private const val CONFIRM_CANCEL_CONVERSION =
            "cancelConversion"
        private const val CONFIRM_CLEAR_SELECTION =
            "clearSelection"
        private const val HELP_CONVERTER =
            "converter"
        private const val REQUEST_SOURCE_FOLDER =
            4201
        private const val REQUEST_DESTINATION_FOLDER =
            4202

        private const val REFRESH_INTERVAL_MS =
            100L
        // Android redelivery may be delayed during process recreation;
        // 1.5s was not enough and could cause false "interrupted" status.
        private const val INTERRUPTED_RUN_RECHECK_MS =
            30_000L
    }
}
