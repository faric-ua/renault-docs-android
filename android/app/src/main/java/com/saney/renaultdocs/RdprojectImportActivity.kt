package com.saney.renaultdocs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
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

/**
 * «Імпорт проєкту» — a whole project from one `.rdproject` file (v0.5.132).
 *
 * Opens from: Telegram / Google Drive / «Мої файли» («Відкрити за допомогою» or
 * «Поділитися» → Renault Docs), or Home → «Додати» → «Проєкт із файлу».
 * 1) reads only the beginning of the file and shows the project and its volumes,
 *    marking those already on the phone;
 * 2) after «Імпортувати» the service adds only the missing volumes;
 * 3) progress and the result stay on this screen (rotation and the notification
 *    reopen the same run).
 */
class RdprojectImportActivity : Activity() {
    private lateinit var runStore: RdprojectImportRunStore
    private lateinit var confirmDialogs: RestorableDialogs
    private lateinit var statusText: TextView
    private lateinit var operationStatus: OperationStatusView
    private lateinit var container: LinearLayout
    private lateinit var bottomButton: TextView

    private var uri: Uri? = null
    private var preview: RdprojectImportSupport.Preview? = null
    private var previewError: String? = null

    /** How another app handed the file over: shown only when it could not be read. */
    private var incomingInfo: String? = null
    private var loading = false
    private var pickerOpen = false
    private var shownTerminalAt = -1L
    private var renderedPhase: RdprojectImportPhase? = null

    /**
     * v0.5.133 (owner, M30s 2026-10-14 20:15): «мені з нього тільки два файли».
     * Ticked new volumes; null = not set yet → all new volumes ticked.
     */
    private var selectedFiles: LinkedHashSet<String>? = null
    private var summaryLine: TextView? = null
    private var spaceLine: TextView? = null
    private var toggleAllButton: TextView? = null
    private var buttonOpensProject = false

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable =
        object : Runnable {
            override fun run() {
                refreshRunState()
                handler.postDelayed(this, REFRESH_MS)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runStore = RdprojectImportRunStore(this)
        confirmDialogs =
            RestorableDialogs(activity = this, stateKey = "rdprojectImportConfirm") { key ->
                if (key == CONFIRM_STOP) buildStopDialog() else null
            }
        confirmDialogs.restore(savedInstanceState)
        setContentView(buildContent())

        val retained = lastNonConfigurationInstance as? Retained
        pickerOpen = savedInstanceState?.getBoolean(STATE_PICKER_OPEN, false) ?: false
        uri = retained?.uri ?: savedInstanceState?.getString(STATE_URI)?.let(Uri::parse)
        preview = retained?.preview
        previewError = retained?.error
        selectedFiles =
            retained?.selected
                ?: savedInstanceState?.getStringArrayList(STATE_SELECTED)?.let { LinkedHashSet(it) }

        if (savedInstanceState == null && retained == null) {
            handleIntent(intent)
        } else {
            render()
            if (preview == null && previewError == null && uri != null) loadPreview(uri!!)
        }
        window.decorView.post { confirmDialogs.restoreOpen() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val incoming = incomingUri(intent)
        val state = runStore.load()
        when {
            incoming != null && state.isRunning -> {
                // One import at a time: the running one is shown, the new file waits.
                uri = null
                preview = null
                previewError =
                    "Зараз уже йде імпорт проєкту. Дочекайся кінця і відкрий новий файл ще раз."
                render()
            }
            incoming != null -> {
                if (state.isTerminal) runStore.clearTerminal()
                incomingInfo = RdprojectImportProgress.describeIncoming(incoming.scheme, intent?.type)
                uri = incoming
                selectedFiles = null
                preview = null
                previewError = null
                render()
                loadPreview(incoming)
            }
            intent?.getBooleanExtra(EXTRA_PICK, false) == true && !state.isRunning -> {
                if (state.isTerminal) runStore.clearTerminal()
                render()
                openPicker()
            }
            else -> render()
        }
    }

    @Suppress("DEPRECATION")
    private fun incomingUri(intent: Intent?): Uri? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
            else -> null
        }
    }

    private fun openPicker() {
        if (pickerOpen) return
        pickerOpen = true
        val picker =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        runCatching { startActivityForResult(picker, REQUEST_PICK) }
            .onFailure {
                pickerOpen = false
                previewError = "Не вдалося відкрити вибір файлу."
                render()
            }
    }

    @Deprecated(
        "Uses platform SAF result for minSdk 26 compatibility.",
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK) return
        pickerOpen = false
        val picked = data?.data
        if (resultCode != RESULT_OK || picked == null) {
            if (uri == null && preview == null && !runStore.load().isRunning && !runStore.load().isTerminal) {
                finish()
            }
            return
        }
        runCatching { contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        incomingInfo = null
        uri = picked
        selectedFiles = null
        preview = null
        previewError = null
        render()
        loadPreview(picked)
    }

    private fun loadPreview(target: Uri) {
        if (loading) return
        loading = true
        render()
        Thread {
            val result = runCatching { RdprojectImportSupport.preview(applicationContext, target) }
            runOnUiThread {
                loading = false
                if (isFinishing || isDestroyed || uri != target) return@runOnUiThread
                result
                    .onSuccess { preview = it }
                    .onFailure { error ->
                        previewError =
                            when (error) {
                                is RdprojectFormatException -> error.message
                                is SecurityException -> RdprojectImportSupport.NO_ACCESS
                                // Phone QA 2026-10-14 23:01: «Мої файли» may hand over a
                                // raw file:// path that Android does not let us read.
                                else ->
                                    if (target.scheme == "file") {
                                        RdprojectImportSupport.FILE_PATH_NO_ACCESS
                                    } else {
                                        error.message
                                    }
                            } ?: "Не вдалося прочитати файл проєкту."
                    }
                render()
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onStop() {
        handler.removeCallbacks(refreshRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        confirmDialogs.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        uri?.let { outState.putString(STATE_URI, it.toString()) }
        selectedFiles?.let { outState.putStringArrayList(STATE_SELECTED, ArrayList(it)) }
        outState.putBoolean(STATE_PICKER_OPEN, pickerOpen)
        confirmDialogs.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRetainNonConfigurationInstance(): Any = Retained(uri, preview, previewError, selectedFiles)

    private data class Retained(
        val uri: Uri?,
        val preview: RdprojectImportSupport.Preview?,
        val error: String?,
        val selected: LinkedHashSet<String>?,
    )

    // ---------------------------------------------------------------- layout

    private fun buildContent(): View {
        window.statusBarColor = Ui.background
        window.navigationBarColor = Ui.background
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Ui.background)
            }
        Ui.applySystemInsets(root)

        val top =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        top.addView(
            ImageButton(this).apply {
                setImageResource(R.drawable.ic_arrow_back)
                contentDescription = "Назад"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)),
        )
        top.addView(
            Ui.textView(this, "Імпорт проєкту", 24f, Ui.entityTitle).apply {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        root.addView(top)

        statusText =
            Ui.textView(this, "", Ui.secondaryTextSp, Ui.muted).apply {
                setPadding(Ui.dp(this@RdprojectImportActivity, 12), 0, Ui.dp(this@RdprojectImportActivity, 12), 0)
            }
        root.addView(statusText)

        operationStatus = OperationStatusView(this).apply { visibility = View.GONE }
        root.addView(operationStatus)

        val scroll = ScrollView(this)
        container =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(this@RdprojectImportActivity, 12),
                    Ui.dp(this@RdprojectImportActivity, 10),
                    Ui.dp(this@RdprojectImportActivity, 12),
                    Ui.dp(this@RdprojectImportActivity, 12),
                )
            }
        scroll.addView(container)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        bottomButton =
            Ui.actionButton(this, "Імпортувати", primary = true) { onBottomButton() }.apply {
                visibility = View.GONE
            }
        root.addView(
            bottomButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(
                    Ui.dp(this@RdprojectImportActivity, 12),
                    Ui.dp(this@RdprojectImportActivity, 6),
                    Ui.dp(this@RdprojectImportActivity, 12),
                    Ui.dp(this@RdprojectImportActivity, 10),
                )
            },
        )
        return root
    }

    private fun render() {
        if (!::container.isInitialized) return
        val state = runStore.load()
        renderedPhase = state.phase
        buttonOpensProject = false
        container.removeAllViews()
        if (state.isRunning || state.isTerminal) {
            renderRun(state)
            return
        }
        operationStatus.visibility = View.GONE
        val loaded = preview
        when {
            loaded != null -> renderPreview(loaded)
            previewError != null -> {
                statusText.setTextColor(Ui.danger)
                statusText.text = previewError
                container.addView(
                    hint(
                        "Тут відкривається файл проєкту .rdproject — його робить ⋮ проєкту → " +
                            "«Поділитися проєктом одним файлом (.rdproject)».",
                    ),
                )
                incomingInfo?.let { container.addView(hint(it)) }
                if (previewError == RdprojectImportSupport.FILE_PATH_NO_ACCESS) {
                    showButton("Вибрати цей файл ще раз", primary = true)
                } else {
                    showButton("Вибрати інший файл", primary = false)
                }
            }
            loading || uri != null -> {
                statusText.setTextColor(Ui.muted)
                statusText.text = "Читаю файл проєкту…"
                bottomButton.visibility = View.GONE
            }
            else -> {
                statusText.setTextColor(Ui.muted)
                statusText.text = "Файл проєкту не вибрано."
                showButton("Вибрати файл .rdproject", primary = true)
            }
        }
    }

    private fun chosen(loaded: RdprojectImportSupport.Preview): List<String> {
        val newFiles = loaded.plan.toImport.map { it.volume.file }
        val ticks = selectedFiles ?: LinkedHashSet(newFiles).also { selectedFiles = it }
        return newFiles.filter { ticks.contains(it) }
    }

    /** v0.5.133: summary, space and button follow the ticks without redrawing the list. */
    private fun updateSelection(loaded: RdprojectImportSupport.Preview) {
        val plan = loaded.plan
        val count = chosen(loaded).size
        summaryLine?.text =
            "Буде додано: $count з ${plan.toImport.size} нових · Уже є на телефоні: ${plan.alreadyPresent.size}"
        summaryLine?.setTextColor(if (count > 0) Ui.accent else Ui.muted)
        spaceLine?.let { line ->
            val need = loaded.needBytes(count)
            line.text =
                "Вільно на телефоні: " + RdprojectImportSupport.formatBytes(loaded.freeBytes) +
                    (need?.let { " · потрібно приблизно " + RdprojectImportSupport.formatBytes(it) } ?: "")
            line.setTextColor(if (loaded.lowSpace(count)) Ui.warning else Ui.muted)
        }
        toggleAllButton?.text =
            if (count == plan.toImport.size) "Зняти всі галочки" else "Вибрати всі нові"
        if (count > 0) {
            showButton("Імпортувати $count ${RdprojectImportSupport.volumeNoun(count)}", primary = true)
        } else {
            showButton("Познач томи, які додати", primary = false)
        }
    }

    private fun renderPreview(loaded: RdprojectImportSupport.Preview) {
        val plan = loaded.plan
        val toImport = plan.toImport.size
        val total = plan.volumes.size
        statusText.setTextColor(Ui.muted)
        statusText.text = loaded.fileName + " · " + RdprojectImportSupport.formatBytes(loaded.fileSizeBytes)

        container.addView(
            Ui.textView(this, plan.manifest.displayTitle, 22f, Ui.entityTitle).apply {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            },
        )
        container.addView(
            line(
                "У файлі: $total ${RdprojectImportSupport.volumeNoun(total)}",
                Ui.text,
            ),
        )
        container.addView(
            line(
                if (plan.createsProject) {
                    "Буде створено новий проєкт «${plan.targetTitle}»."
                } else {
                    "Проєкт «${plan.targetTitle}» уже є — нові томи додадуться до нього."
                },
                Ui.muted,
            ),
        )
        summaryLine = null
        spaceLine = null
        toggleAllButton = null
        container.addView(
            line(
                "Буде додано: $toImport · Уже є на телефоні: ${plan.alreadyPresent.size}",
                if (toImport > 0) Ui.accent else Ui.success,
            ).apply {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (toImport > 0) summaryLine = this
            },
        )
        if (toImport > 0) {
            container.addView(line("", Ui.muted).also { spaceLine = it })
            if (loaded.lowSpace) {
                container.addView(
                    line(
                        "Місця може не вистачити. Імпорт сам зупиниться, якщо місце закінчиться; уже додані томи лишаться.",
                        Ui.warning,
                    ),
                )
            }
        }
        container.addView(
            line("Наявні томи не змінюються і не замінюються.", Ui.muted),
        )

        if (toImport > 0) {
            container.addView(
                line("Галочка — том буде додано. Зніми її з томів, які не потрібні.", Ui.muted),
            )
            container.addView(
                Ui.actionButton(this, "Зняти всі галочки") {
                    val newFiles = plan.toImport.map { it.volume.file }
                    val ticks = selectedFiles ?: LinkedHashSet(newFiles)
                    selectedFiles =
                        if (newFiles.all { ticks.contains(it) }) LinkedHashSet() else LinkedHashSet(newFiles)
                    render()
                }.also { toggleAllButton = it },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@RdprojectImportActivity, 8) },
            )
        }

        plan.volumes.forEach { item -> container.addView(volumeRow(item, loaded)) }

        if (toImport > 0) {
            updateSelection(loaded)
        } else {
            container.addView(
                line("Усі томи з цього файлу вже є на телефоні — додавати нічого.", Ui.success),
                0,
            )
            showButton("Закрити", primary = false)
        }
    }

    private fun volumeRow(
        item: RdprojectVolumePlan,
        loaded: RdprojectImportSupport.Preview,
    ): View {
        if (item.status == RdprojectVolumeStatus.NEW) return newVolumeRow(item, loaded)
        val (mark, color, note) =
            when (item.status) {
                RdprojectVolumeStatus.NEW -> Triple("＋", Ui.accent, "буде додано")
                RdprojectVolumeStatus.IN_TARGET_PROJECT -> Triple("✓", Ui.success, "уже є")
                RdprojectVolumeStatus.IN_OTHER_PROJECT ->
                    Triple("✓", Ui.success, "уже є в проєкті «${item.existingProjectTitle.orEmpty()}» — не чіпаю")
            }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.roundedBackground(context = this@RdprojectImportActivity, fill = Ui.surface, stroke = Ui.border)
            setPadding(
                Ui.dp(this@RdprojectImportActivity, 12),
                Ui.dp(this@RdprojectImportActivity, 8),
                Ui.dp(this@RdprojectImportActivity, 12),
                Ui.dp(this@RdprojectImportActivity, 8),
            )
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@RdprojectImportActivity, 6) }
            addView(Ui.textView(this@RdprojectImportActivity, "$mark ${item.volume.label}", 15f, color))
            if (item.volume.title.isNotBlank() && item.volume.title != item.volume.label) {
                addView(Ui.textView(this@RdprojectImportActivity, item.volume.title, Ui.secondaryTextSp, Ui.text))
            }
            addView(Ui.textView(this@RdprojectImportActivity, note, Ui.secondaryTextSp, Ui.muted))
        }
    }

    /** v0.5.133: a new volume with a tick; tapping the row toggles it too. */
    private fun newVolumeRow(
        item: RdprojectVolumePlan,
        loaded: RdprojectImportSupport.Preview,
    ): View {
        val file = item.volume.file
        val ticked = chosen(loaded).contains(file)
        val note = TextView(this)
        fun paintNote(on: Boolean) {
            note.text = if (on) "буде додано" else "не додавати"
            note.setTextColor(if (on) Ui.muted else Ui.warning)
        }
        val check =
            CheckBox(this).apply {
                isChecked = ticked
                contentDescription = item.volume.label
                setOnCheckedChangeListener { _, on ->
                    val ticks = selectedFiles ?: LinkedHashSet<String>().also { selectedFiles = it }
                    if (on) ticks.add(file) else ticks.remove(file)
                    paintNote(on)
                    updateSelection(loaded)
                }
            }
        note.textSize = Ui.secondaryTextSp
        paintNote(ticked)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isBaselineAligned = false
            isClickable = true
            isFocusable = true
            setOnClickListener { check.toggle() }
            background = Ui.roundedBackground(context = this@RdprojectImportActivity, fill = Ui.surface, stroke = Ui.border)
            setPadding(
                Ui.dp(this@RdprojectImportActivity, 4),
                Ui.dp(this@RdprojectImportActivity, 6),
                Ui.dp(this@RdprojectImportActivity, 12),
                Ui.dp(this@RdprojectImportActivity, 6),
            )
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = Ui.dp(this@RdprojectImportActivity, 6) }
            addView(check)
            val text = LinearLayout(this@RdprojectImportActivity).apply { orientation = LinearLayout.VERTICAL }
            text.addView(Ui.textView(this@RdprojectImportActivity, item.volume.label, 15f, Ui.text))
            if (item.volume.title.isNotBlank() && item.volume.title != item.volume.label) {
                text.addView(Ui.textView(this@RdprojectImportActivity, item.volume.title, Ui.secondaryTextSp, Ui.text))
            }
            text.addView(note)
            addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun renderRun(state: RdprojectImportRunState) {
        statusText.setTextColor(Ui.muted)
        statusText.text = state.fileName
        container.addView(
            line(
                "Проєкт: «${state.projectTitle}» · вибрано ${state.wantedFiles.size} " +
                    RdprojectImportSupport.volumeNoun(state.wantedFiles.size),
                Ui.text,
            ),
        )
        if (state.isRunning) {
            container.addView(
                line(
                    "Можна згорнути застосунок — імпорт іде далі, прогрес видно в сповіщенні.",
                    Ui.muted,
                ),
            )
            bottomButton.visibility = View.GONE
        } else {
            val openable =
                state.phase != RdprojectImportPhase.FAILED || state.importedCount > 0
            if (state.projectId.isNotBlank() && ProjectStore(this).project(state.projectId) != null && openable) {
                showButton("Відкрити проєкт «${state.projectTitle}»", primary = true)
                buttonOpensProject = true
            } else {
                showButton("Закрити", primary = false)
            }
        }
        refreshRunState()
    }

    private fun refreshRunState() {
        if (!::operationStatus.isInitialized) return
        val state = runStore.load()
        if (state.phase != renderedPhase) {
            // The run started or finished since the screen was drawn.
            render()
            return
        }
        if (state.isRunning) {
            val stopping = runStore.isCancelRequested()
            operationStatus.showRunning(
                title = "Імпорт проєкту",
                detail = if (stopping) "Зупиняю після поточного безпечного кроку…" else state.message,
                current = state.progressCurrent.takeIf { state.progressTotal > 0 },
                total = state.progressTotal.takeIf { it > 0 },
                subject = state.projectTitle.ifBlank { "Renault Docs" },
                onCancel = if (stopping) null else { { confirmDialogs.show(CONFIRM_STOP) } },
            )
            return
        }
        if (!state.isTerminal) return
        if (shownTerminalAt == state.finishedAtMs && operationStatus.visibility == View.VISIBLE) return
        shownTerminalAt = state.finishedAtMs
        operationStatus.showTerminal(
            title =
                when (state.phase) {
                    RdprojectImportPhase.COMPLETE -> "Імпорт проєкту завершено"
                    RdprojectImportPhase.CANCELLED -> "Імпорт проєкту · скасовано"
                    else -> "Імпорт проєкту · помилка"
                },
            detail = state.message,
            subject = state.projectTitle.ifBlank { "Renault Docs" },
            onClose = {
                runStore.clearTerminal()
                operationStatus.hide()
                finish()
            },
        )
    }

    private fun onBottomButton() {
        val state = runStore.load()
        if (state.isTerminal) {
            val projectId = state.projectId
            val open = buttonOpensProject
            runStore.clearTerminal()
            if (open && ProjectStore(this).project(projectId) != null) {
                startActivity(ProjectActivity.intent(this, projectId))
            }
            finish()
            return
        }
        if (state.isRunning) return
        val loaded = preview
        when {
            loaded != null && loaded.plan.toImport.isNotEmpty() && chosen(loaded).isEmpty() -> Unit
            loaded != null && loaded.plan.toImport.isNotEmpty() -> startImport(loaded)
            loaded != null -> finish()
            else -> {
                uri = null
                previewError = null
                openPicker()
            }
        }
    }

    private fun startImport(loaded: RdprojectImportSupport.Preview) {
        // Re-check right before starting: the phone may have changed since the preview.
        val fresh =
            runCatching { loaded.copy(plan = RdprojectImportSupport.plan(ProjectStore(this), loaded.plan.manifest)) }
                .getOrDefault(loaded)
        val ticked = chosen(loaded).toSet()
        val wanted = fresh.plan.toImport.map { it.volume.file }.filter { ticked.contains(it) }
        if (wanted.isEmpty()) {
            preview = fresh
            render()
            return
        }
        if (!RdprojectImportService.start(this, fresh, wanted)) {
            statusText.setTextColor(Ui.danger)
            statusText.text = "Не вдалося запустити імпорт проєкту."
            return
        }
        preview = null
        render()
    }

    private fun buildStopDialog(): Dialog? {
        if (!runStore.load().isRunning) return null
        return ConfirmActions.confirmStop(
            activity = this,
            title = "Зупинити імпорт проєкту?",
            message = "Поточний том не буде додано. Уже додані томи лишаться. Файл .rdproject не змінюється.",
        ) {
            runStore.requestCancel()
        }
    }

    private fun showButton(
        label: String,
        primary: Boolean,
    ) {
        bottomButton.text = label
        bottomButton.setTextColor(if (primary) Ui.accent else Ui.text)
        bottomButton.background =
            Ui.roundedBackground(
                context = this,
                fill = if (primary) Ui.surfaceAlt else Ui.surface,
                stroke = if (primary) Ui.accent else Ui.border,
                radiusDp = 11,
            )
        bottomButton.visibility = View.VISIBLE
    }

    private fun line(
        value: String,
        color: Int,
    ): TextView =
        Ui.textView(this, value, 14f, color).apply {
            setPadding(0, Ui.dp(this@RdprojectImportActivity, 6), 0, 0)
        }

    private fun hint(value: String): TextView = line(value, Ui.muted)

    companion object {
        private const val REQUEST_PICK = 4211
        private const val REFRESH_MS = 300L
        private const val CONFIRM_STOP = "stopRdprojectImport"
        private const val STATE_URI = "rdprojectUri"
        private const val STATE_SELECTED = "rdprojectSelected"
        private const val STATE_PICKER_OPEN = "rdprojectPickerOpen"
        private const val EXTRA_PICK = "pickRdproject"

        /** Home → «Додати» → «Проєкт із файлу»: opens the file picker first. */
        fun pickIntent(activity: Activity): Intent =
            Intent(activity, RdprojectImportActivity::class.java).putExtra(EXTRA_PICK, true)
    }
}
