package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
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
import androidx.documentfile.provider.DocumentFile

class SettingsActivity : Activity() {
    private lateinit var settings: AppSettings
    private lateinit var modeValue: TextView
    private lateinit var pdfZoomValue: TextView
    private lateinit var pdfStepValue: TextView
    private lateinit var storageValue: TextView
    // v0.5.149 (TODO №10): leftovers of broken-off imports / archives.
    private lateinit var leftoversValue: TextView
    private lateinit var backupValue: TextView
    private lateinit var backupWarning: TextView
    private lateinit var backupChooseButton: Button
    private lateinit var backupResetButton: Button
    private lateinit var statusText: TextView

    private var activeDialogKind: String = ""
    private lateinit var confirmDialogs: RestorableDialogs

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        settings =
            AppSettings(this)

        activeDialogKind =
            savedInstanceState
                ?.getString(
                    STATE_DIALOG_KIND,
                )
                .orEmpty()

        confirmDialogs =
            RestorableDialogs(
                activity = this,
                stateKey = "settingsConfirm",
            ) { key ->
                when (key) {
                    CONFIRM_RESET_BACKUP -> buildResetBackupDialog()
                    CONFIRM_LEFTOVERS -> buildLeftoversDialog()
                    else -> null
                }
            }
        confirmDialogs.restore(
            savedInstanceState,
        )

        setContentView(
            buildContent(),
        )

        renderValues()
        // v0.5.106: an open question comes back after rotation.
        window.decorView.post {
            confirmDialogs.restoreOpen()
        }

        when (activeDialogKind) {
            DIALOG_MODE ->
                showModeChooser()

            DIALOG_PDF_ZOOM ->
                showPdfZoomChooser()

            DIALOG_PDF_STEP ->
                showPdfStepChooser()

            DIALOG_STORAGE ->
                showStorageChooser()
        }
    }

    override fun onResume() {
        super.onResume()
        // The card may be put in or taken out while the app is in the background.
        if (::storageValue.isInitialized) renderValues()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putString(
            STATE_DIALOG_KIND,
            activeDialogKind,
        )
        confirmDialogs.save(
            outState,
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    private fun buildResetBackupDialog(): AlertDialog? =
        ConfirmActions.confirm(
            activity =
                this@SettingsActivity,
            title =
                "Скинути папку резервних копій?",
            message =
                "Застосунок забуде вибрану папку. Файли в ній не видаляються; папку можна вибрати знову.",
            confirmLabel =
                "Скинути",
            keepLabel =
                "Залишити",
        ) {
            settings
                .clearBackupFolder()

            statusText
                .setTextColor(
                    Ui.muted,
                )
            statusText.text =
                "Вибір папки резервних копій скинуто. Файли не видалялися."

            renderValues()
        }

    /**
     * v0.5.149 (TODO №10, planned 2026-10-15 «з "так" власника»): the list of what broken-off
     * imports / archives left, then «Прибрати» — into the trash, erased with the bar on the
     * home screen. Never while an import / archive runs.
     */
    private fun buildLeftoversDialog(): AlertDialog? {
        val found = VolumeTrash.leftovers(this)
        if (found.isEmpty() || VolumeTrash.leftoverWriterRunning(this)) return null
        val names = found.take(12).joinToString("\n") { "• " + it.name + if (it.isDirectory) " (папка)" else "" }
        val more = if (found.size > 12) "\n… і ще " + (found.size - 12) else ""
        return ConfirmActions.confirm(
            activity = this,
            title = "Прибрати залишки: " + found.size + "?",
            message =
                names + more +
                    "\n\nЦе недороблені файли обірваних імпортів і архівів. Томи, готові архіви й проєкти не чіпаються. " +
                    "Стирання — у фоні, смуга на головному екрані.",
            confirmLabel = "Прибрати",
            keepLabel = "Залишити",
        ) {
            val moved = VolumeTrash.clearLeftovers(this)
            statusText.setTextColor(Ui.muted)
            statusText.text =
                if (moved == null) {
                    "Зараз іде імпорт або архів — нічого не чіпав. Спробуй після кінця."
                } else {
                    "Прибрано залишків: $moved. Стираються у фоні — смуга на головному екрані."
                }
            renderValues()
        }
    }

    private fun onLeftoversClicked() {
        val found = VolumeTrash.leftovers(this)
        when {
            found.isEmpty() -> {
                statusText.setTextColor(Ui.muted)
                statusText.text = "Залишків обірваних імпортів немає — прибирати нічого."
            }
            VolumeTrash.leftoverWriterRunning(this) -> {
                statusText.setTextColor(Ui.muted)
                statusText.text = "Зараз іде імпорт або архів — залишки покажу після кінця."
            }
            else -> confirmDialogs.show(CONFIRM_LEFTOVERS)
        }
    }

    override fun onDestroy() {
        if (::confirmDialogs.isInitialized) {
            confirmDialogs.onDestroy()
        }
        super.onDestroy()
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

        if (
            requestCode !=
            REQUEST_BACKUP_FOLDER
        ) {
            return
        }

        if (
            resultCode != RESULT_OK
        ) {
            statusText.text =
                "Вибір папки резервних копій скасовано."
            return
        }

        val uri =
            data?.data

        if (uri == null) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Android не повернув папку backup."
            return
        }

        val returnedFlags =
            data?.flags ?: 0

        val flags =
            returnedFlags and
                (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

        runCatching {
            if (flags != 0) {
                contentResolver
                    .takePersistableUriPermission(
                        uri,
                        flags,
                    )
            }
        }

        val folder =
            DocumentFile.fromTreeUri(
                this,
                uri,
            )

        settings.setBackupFolder(
            uri = uri.toString(),
            name =
                folder?.name,
        )

        val friendlyPath =
            friendlyBackupPath(
                uri.toString(),
            )

        val appearsInsideDataset =
            friendlyPath
                ?.contains(
                    "_android/",
                    ignoreCase = true,
                )
                ?: false

        statusText.setTextColor(
            if (
                appearsInsideDataset
            ) {
                Ui.danger
            } else {
                Ui.accent
            },
        )

        statusText.text =
            if (
                appearsInsideDataset
            ) {
                "Папку збережено, але краще тримати резервні копії поза dataset: Documents/Renault/backups."
            } else {
                "Папку backup збережено."
            }

        renderValues()
    }

    private fun buildContent(): View {
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

        Ui.applySystemInsets(root)

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
                setPadding(
                    Ui.dp(
                        this@SettingsActivity,
                        12,
                    ),
                    Ui.dp(
                        this@SettingsActivity,
                        12,
                    ),
                    Ui.dp(
                        this@SettingsActivity,
                        12,
                    ),
                    Ui.dp(
                        this@SettingsActivity,
                        12,
                    ),
                )
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                Ui.dp(this, 48),
                Ui.dp(this, 48),
            )
        )

        topBar.addView(
            Ui.textView(
                context = this,
                value = "Налаштування",
                sizeSp = 24f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    Ui.dp(
                        this@SettingsActivity,
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
            )
        )

        root.addView(topBar)

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@SettingsActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@SettingsActivity,
                        28,
                    ),
                )
            }

        body.addView(
            sectionTitle(
                "Загальні",
            )
        )

        body.addCard(
            settingRow(
                title =
                    "Відкривати dataset",
                description =
                    "Вибери Modern або Classic як режим за замовчуванням.",
                onClick = {
                    showModeChooser()
                },
                valueHolder = {
                    modeValue = it
                },
            )
        )

        // TODO №1 (owner 2026-10-14 20:15, plan 2026-10-15 01:55): phone or memory card.
        body.addView(
            sectionTitle(
                "Томи",
            )
        )

        body.addCard(
            settingRow(
                title =
                    "Де зберігати нові томи",
                description =
                    "Телефон або карта пам'яті. Діє для всіх нових томів: «Готові проєкти», проєкт із файлу, .rdpkg, створення з архіву. Томи, що вже є, лишаються там, де лежать.",
                onClick = {
                    showStorageChooser()
                },
                valueHolder = {
                    storageValue = it
                },
            )
        )

        body.addCard(
            settingRow(
                title =
                    "Залишки обірваних імпортів",
                description =
                    "Недороблені файли, що лишились, коли імпорт або архів тому обірвався (телефон перезапустився тощо). Спершу список, прибрати — лише після «Прибрати».",
                onClick = {
                    onLeftoversClicked()
                },
                valueHolder = {
                    leftoversValue = it
                },
            )
        )

        body.addView(
            sectionTitle(
                "PDF",
            )
        )

        body.addCard(
            settingRow(
                title =
                    "Масштаб за замовчуванням",
                description =
                    "Застосовується при новому відкритті PDF.",
                onClick = {
                    showPdfZoomChooser()
                },
                valueHolder = {
                    pdfZoomValue = it
                },
            )
        )

        body.addCard(
            settingRow(
                title =
                    "Крок кнопок − / +",
                description =
                    "На скільки відсотків змінювати PDF одним натисканням.",
                onClick = {
                    showPdfStepChooser()
                },
                valueHolder = {
                    pdfStepValue = it
                },
            )
        )

        body.addView(
            sectionTitle(
                "Резервні копії",
            )
        )

        body.addCard(
            backupCard()
        )

        body.addView(
            sectionTitle(
                "Про програму",
            )
        )

        // v0.5.130: the user guide inside the app (owner 2026-10-14).
        body.addCard(
            guideCard(),
        )

        body.addCard(
            supportCard(),
        )

        body.addCard(
            infoCard(
                title =
                    "Renault Docs",
                text =
                    "Бібліотека Renault технічної документації для Android.\n\n" +
                        "Версія: v" +
                        BuildConfig.VERSION_NAME +
                        " (build " +
                        BuildConfig.VERSION_CODE +
                        ")\n" +
                        "Режим даних: SAF + Modern index + Fast Pack\n" +
                        "PDF: Android PdfRenderer + оригінальний PDF export",
            )
        )

        statusText =
            Ui.textView(
                context = this,
                value =
                    "Зміни зберігаються автоматично.",
                sizeSp = Ui.secondaryTextSp,
                color = Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@SettingsActivity,
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

    /**
     * v0.5.131: every tile gets the same gap below it — before, tiles of one
     * section touched each other («Підтримати» / «Renault Docs», the two PDF
     * rows; phone QA 2026-10-14 07:22).
     */
    private fun LinearLayout.addCard(
        card: View,
    ) {
        addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@SettingsActivity,
                        10,
                    )
            },
        )
    }

    private fun sectionTitle(
        title: String,
    ): TextView =
        Ui.textView(
            context = this,
            value = title,
            sizeSp = 18f,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            setPadding(
                0,
                Ui.dp(
                    this@SettingsActivity,
                    20,
                ),
                0,
                Ui.dp(
                    this@SettingsActivity,
                    8,
                ),
            )
        }

    private fun settingRow(
        title: String,
        description: String,
        onClick: () -> Unit,
        valueHolder: (TextView) -> Unit,
    ): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            background =
                Ui.roundedBackground(
                    context =
                        this@SettingsActivity,
                    fill = Ui.surface,
                )
            setPadding(
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = title,
                    sizeSp = 17f,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = description,
                    sizeSp = Ui.secondaryTextSp,
                    color = Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            5,
                        ),
                        0,
                        0,
                    )
                }
            )

            val value =
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = "",
                    sizeSp = Ui.valueTextSp,
                    color = Ui.accent,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            10,
                        ),
                        0,
                        0,
                    )
                }

            addView(value)
            valueHolder(value)

            setOnClickListener {
                onClick()
            }
        }

    private fun backupCard(): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@SettingsActivity,
                    fill =
                        Ui.surface,
                )
            setPadding(
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "Папка резервних копій",
                    sizeSp = 17f,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "Вибери окрему папку для резервних копій. Рекомендовано: Documents/Renault/backups.",
                    sizeSp = Ui.secondaryTextSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            5,
                        ),
                        0,
                        0,
                    )
                }
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "Вибрана папка",
                    sizeSp = Ui.actionSubtitleSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            14,
                        ),
                        0,
                        0,
                    )
                }
            )

            backupValue =
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "Не вибрано",
                    sizeSp = Ui.valueTextSp,
                    color =
                        Ui.accent,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            4,
                        ),
                        0,
                        0,
                    )
                }

            addView(backupValue)

            backupWarning =
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = "",
                    sizeSp = Ui.actionSubtitleSp,
                    color =
                        Ui.danger,
                ).apply {
                    visibility =
                        View.GONE
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            8,
                        ),
                        0,
                        0,
                    )
                }

            addView(backupWarning)

            val actions =
                LinearLayout(
                    this@SettingsActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            12,
                        ),
                        0,
                        0,
                    )
                }

            backupChooseButton =
                Button(
                    this@SettingsActivity,
                ).apply {
                    text =
                        "Вибрати папку"
                    isAllCaps =
                        false
                    textSize =
                        Ui.compactButtonSp
                    setTextColor(
                        Ui.accent,
                    )
                    gravity =
                        Gravity.CENTER
                    minHeight =
                        Ui.dp(
                            this@SettingsActivity,
                            44,
                        )
                    minimumHeight =
                        Ui.dp(
                            this@SettingsActivity,
                            44,
                        )
                    background =
                        Ui.roundedBackground(
                            context =
                                this@SettingsActivity,
                            fill =
                                Ui.surfaceAlt,
                            stroke =
                                Ui.accent,
                            radiusDp =
                                10,
                        )
                    setPadding(
                        Ui.dp(
                            this@SettingsActivity,
                            10,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            6,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            10,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            6,
                        ),
                    )
                    setOnClickListener {
                        openBackupFolderPicker()
                    }
                }

            actions.addView(
                backupChooseButton,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                )
            )

            backupResetButton =
                Button(
                    this@SettingsActivity,
                ).apply {
                    text =
                        "Скинути вибір"
                    isAllCaps =
                        false
                    textSize =
                        Ui.compactButtonSp
                    setTextColor(
                        Ui.text,
                    )
                    gravity =
                        Gravity.CENTER
                    minHeight =
                        Ui.dp(
                            this@SettingsActivity,
                            44,
                        )
                    minimumHeight =
                        Ui.dp(
                            this@SettingsActivity,
                            44,
                        )
                    background =
                        Ui.roundedBackground(
                            context =
                                this@SettingsActivity,
                            fill =
                                Ui.surfaceAlt,
                            stroke =
                                Ui.border,
                            radiusDp =
                                10,
                        )
                    setPadding(
                        Ui.dp(
                            this@SettingsActivity,
                            10,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            6,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            10,
                        ),
                        Ui.dp(
                            this@SettingsActivity,
                            6,
                        ),
                    )
                    visibility =
                        View.GONE
                    setOnClickListener {
                        // v0.5.104: forgetting the chosen folder asks first;
                        // v0.5.106: the question survives rotation.
                        confirmDialogs.show(CONFIRM_RESET_BACKUP)
                    }
                }

            actions.addView(
                backupResetButton,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@SettingsActivity,
                            6,
                        )
                }
            )

            addView(actions)
        }

    private fun guideCard():
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
            contentDescription =
                "Інструкція користувача"
            background =
                Ui.roundedBackground(
                    context =
                        this@SettingsActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.accent,
                )
            setPadding(
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
            )
            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "📖 Інструкція користувача",
                    sizeSp =
                        17f,
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
                        this@SettingsActivity,
                    value =
                        "Як додати, читати і передавати томи. Працює без інтернету.",
                    sizeSp =
                        Ui.secondaryTextSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            5,
                        ),
                        0,
                        0,
                    )
                },
            )

            setOnClickListener {
                GuideActivity.open(
                    this@SettingsActivity,
                )
            }
        }

    private fun supportCard():
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
                        this@SettingsActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.accent,
                )
            setPadding(
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value =
                        "Підтримати Renault Docs",
                    sizeSp =
                        17f,
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
                        this@SettingsActivity,
                    value =
                        "Добровільна підтримка розвитку програми. Не відкриває додаткових функцій.",
                    sizeSp =
                        Ui.secondaryTextSp,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            5,
                        ),
                        0,
                        0,
                    )
                },
            )

            setOnClickListener {
                if (
                    !ExternalLinks.openWeb(
                        this@SettingsActivity,
                        ExternalLinks.SUPPORT_URL,
                    )
                ) {
                    android.widget.Toast
                        .makeText(
                            this@SettingsActivity,
                            "Посилання на підтримку ще не налаштовано.",
                            android.widget.Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        }

    private fun infoCard(
        title: String,
        text: String,
    ): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@SettingsActivity,
                    fill = Ui.surface,
                )
            setPadding(
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
                Ui.dp(
                    this@SettingsActivity,
                    16,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = title,
                    sizeSp = 18f,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            addView(
                Ui.textView(
                    context =
                        this@SettingsActivity,
                    value = text,
                    sizeSp = Ui.secondaryTextSp,
                    color = Ui.muted,
                ).apply {
                    setPadding(
                        0,
                        Ui.dp(
                            this@SettingsActivity,
                            8,
                        ),
                        0,
                        0,
                    )
                }
            )
        }

    private fun renderValues() {
        modeValue.text =
            when (
                settings.defaultOpenMode
            ) {
                DatasetOpenMode.MODERN ->
                    "Modern"

                DatasetOpenMode.CLASSIC ->
                    "Classic"
            }

        pdfZoomValue.text =
            if (
                settings
                    .pdfDefaultZoomPercent ==
                100
            ) {
                "За шириною · 100%"
            } else {
                settings
                    .pdfDefaultZoomPercent
                    .toString() +
                    "%"
            }

        leftoversValue.text =
            VolumeTrash.leftovers(this).size.let { if (it == 0) "Немає" else "Знайдено: $it" }
        storageValue.text =
            VolumeStorage.choiceLine(this)

        pdfStepValue.text =
            "±" +
                settings
                    .pdfZoomStepPercent +
                "%"

        val backupUri =
            settings
                .backupTreeUri

        val hasBackupFolder =
            !backupUri.isNullOrBlank()

        val friendlyPath =
            backupUri
                ?.let {
                    friendlyBackupPath(it)
                }

        backupValue.text =
            if (
                hasBackupFolder
            ) {
                friendlyPath
                    ?: settings
                        .backupTreeName
                    ?: "Вибрана папка"
            } else {
                "Не вибрано"
            }

        backupChooseButton.text =
            if (
                hasBackupFolder
            ) {
                "Змінити папку"
            } else {
                "Вибрати папку"
            }

        backupResetButton.visibility =
            if (
                hasBackupFolder
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val appearsInsideDataset =
            friendlyPath
                ?.contains(
                    "_android/",
                    ignoreCase = true,
                )
                ?: false

        backupWarning.visibility =
            if (
                appearsInsideDataset
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        backupWarning.text =
            if (
                appearsInsideDataset
            ) {
                "Краще зберігати резервні копії поза dataset: Documents/Renault/backups."
            } else {
                ""
            }
    }

    private fun friendlyBackupPath(
        uriText: String,
    ): String? =
        SafDisplayPath.tree(
            uriText,
        )

    private fun showModeChooser() {
        val labels =
            arrayOf(
                "Modern",
                "Classic",
            )

        val current =
            if (
                settings.defaultOpenMode ==
                DatasetOpenMode.MODERN
            ) {
                0
            } else {
                1
            }

        activeDialogKind =
            DIALOG_MODE

        val dialog =
            AlertDialog.Builder(this)
            .setTitle(
                "Відкривати dataset",
            )
            .setSingleChoiceItems(
                labels,
                current,
            ) { dialog, which ->
                settings.defaultOpenMode =
                    if (which == 0) {
                        DatasetOpenMode.MODERN
                    } else {
                        DatasetOpenMode.CLASSIC
                    }

                dialog.dismiss()
                renderValues()
            }
            .setNegativeButton(
                "Скасувати",
                null,
            )
            .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
        }
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun showPdfZoomChooser() {
        val values =
            intArrayOf(
                85,
                100,
                120,
                150,
                200,
            )

        val labels =
            arrayOf(
                "85%",
                "За шириною · 100%",
                "120%",
                "150%",
                "200%",
            )

        val current =
            values.indexOf(
                settings
                    .pdfDefaultZoomPercent,
            ).takeIf {
                it >= 0
            } ?: 1

        activeDialogKind =
            DIALOG_PDF_ZOOM

        val dialog =
            AlertDialog.Builder(this)
            .setTitle(
                "Масштаб PDF",
            )
            .setSingleChoiceItems(
                labels,
                current,
            ) { dialog, which ->
                settings
                    .pdfDefaultZoomPercent =
                    values[which]

                dialog.dismiss()
                renderValues()
            }
            .setNegativeButton(
                "Скасувати",
                null,
            )
            .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
        }
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun showPdfStepChooser() {
        val values =
            intArrayOf(
                5,
                10,
                20,
            )

        val labels =
            arrayOf(
                "±5%",
                "±10%",
                "±20%",
            )

        val current =
            values.indexOf(
                settings
                    .pdfZoomStepPercent,
            ).takeIf {
                it >= 0
            } ?: 1

        activeDialogKind =
            DIALOG_PDF_STEP

        val dialog =
            AlertDialog.Builder(this)
            .setTitle(
                "Крок масштабу PDF",
            )
            .setSingleChoiceItems(
                labels,
                current,
            ) { dialog, which ->
                settings
                    .pdfZoomStepPercent =
                    values[which]

                dialog.dismiss()
                renderValues()
            }
            .setNegativeButton(
                "Скасувати",
                null,
            )
            .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
        }
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun showStorageChooser() {
        val cardPresent =
            VolumeStorage.cardPresent(this)
        val places =
            listOf(VolumePlace.PHONE, VolumePlace.CARD)
        val labels =
            places.map { place ->
                VolumeStoragePolicy.choiceItem(
                    place = place,
                    cardPresent = cardPresent,
                    freeBytes = VolumeStorage.freeBytes(this, place),
                )
            }.toTypedArray()
        val current =
            places.indexOf(VolumeStorage.chosenPlace(this))

        activeDialogKind =
            DIALOG_STORAGE

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Де зберігати нові томи · вільно",
                )
                .setSingleChoiceItems(
                    labels,
                    current,
                ) { dialog, which ->
                    val place = places[which]
                    if (place == VolumePlace.CARD && !cardPresent) {
                        statusText.text =
                            "Карту пам'яті не знайдено. Встав карту і спробуй ще раз."
                        statusText.setTextColor(Ui.danger)
                    } else {
                        VolumeStorage.setChosenPlace(this, place)
                        statusText.text =
                            "Нові томи: " + VolumeStoragePolicy.placeLabel(place) + "."
                        statusText.setTextColor(Ui.muted)
                    }
                    dialog.dismiss()
                    renderValues()
                }
                .setNegativeButton(
                    "Скасувати",
                    null,
                )
                .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
        }
        dialog.show()
        DialogUi.apply(
            dialog =
                dialog,
            role =
                DialogRole.CHOICE,
        )
    }

    private fun openBackupFolderPicker() {
        val intent =
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

        @Suppress("DEPRECATION")
        startActivityForResult(
            intent,
            REQUEST_BACKUP_FOLDER,
        )
    }

    companion object {
        private const val REQUEST_BACKUP_FOLDER =
            4401

        private const val CONFIRM_RESET_BACKUP =
            "resetBackupFolder"
        private const val CONFIRM_LEFTOVERS =
            "clearLeftovers"
        private const val STATE_DIALOG_KIND =
            "settingsDialogKind"
        private const val DIALOG_MODE =
            "mode"
        private const val DIALOG_PDF_ZOOM =
            "pdfZoom"
        private const val DIALOG_PDF_STEP =
            "pdfStep"
        private const val DIALOG_STORAGE =
            "volumeStorage"
    }
}
