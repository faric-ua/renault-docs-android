package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class ModernVolumeActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var searchInput: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var sectionScroll: ScrollView
    private lateinit var helpDialogs:
        LifecycleHelpDialogController

    private var sectionIndex:
        ModernVolumeSections? = null

    private var datasetTitle: String =
        "Renault dataset"
    private var treeUriText: String = ""
    private var classicEntrypoint: String = ""
    private var volumeTitle: String =
        "Renault volume"
    private var volumeEntrypoint: String = ""
    private var restoredQuery: String = ""
    private var restoredScrollY: Int = 0
    private var openSearchRequested: Boolean = false

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Ui.applyOrientationSystemBars(this)
        DialogUi.reapplyOrientation(this)
        window.decorView.post {
            Ui.applyOrientationSystemBars(this)
            DialogUi.reapplyOrientation(this)
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        datasetTitle =
            intent.getStringExtra(
                EXTRA_DATASET_TITLE,
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: datasetTitle

        treeUriText =
            intent.getStringExtra(
                EXTRA_TREE_URI,
            ).orEmpty()

        classicEntrypoint =
            intent.getStringExtra(
                EXTRA_CLASSIC_ENTRYPOINT,
            ).orEmpty()

        volumeTitle =
            intent.getStringExtra(
                EXTRA_VOLUME_TITLE,
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: volumeTitle

        volumeEntrypoint =
            intent.getStringExtra(
                EXTRA_VOLUME_ENTRYPOINT,
            ).orEmpty()

        restoredQuery =
            savedInstanceState
                ?.getString(
                    STATE_QUERY,
                )
                .orEmpty()

        restoredScrollY =
            savedInstanceState
                ?.getInt(
                    STATE_SCROLL_Y,
                    0,
                )
                ?: 0

        sectionIndex =
            lastNonConfigurationInstance
                as? ModernVolumeSections

        openSearchRequested =
            intent.getBooleanExtra(
                EXTRA_OPEN_SEARCH,
                false,
            )

        helpDialogs =
            LifecycleHelpDialogController(
                activity = this,
                resolve = ::helpSpec,
            )
        helpDialogs.restore(
            savedInstanceState,
        )

        if (
            treeUriText.isBlank() ||
            volumeEntrypoint.isBlank()
        ) {
            showFatalError(
                "Modern volume не отримав dataset/volume context."
            )
            return
        }

        setContentView(
            buildContent(),
        )
        helpDialogs.restoreOpen()

        if (
            restoredQuery.isNotBlank() ||
            openSearchRequested
        ) {
            searchInput.visibility =
                View.VISIBLE

            if (restoredQuery.isNotBlank()) {
                searchInput.setText(
                    restoredQuery,
                )
                searchInput.setSelection(
                    restoredQuery.length,
                )
            }

            searchInput.requestFocus()
        }

        val retainedSections =
            sectionIndex

        if (
            retainedSections != null
        ) {
            showLoadedSections(
                retainedSections,
            )
        } else {
            loadSections()
        }
    }

    override fun onRetainNonConfigurationInstance(): Any? =
        sectionIndex

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        helpDialogs.save(
            outState,
        )

        outState.putString(
            STATE_QUERY,
            if (
                ::searchInput.isInitialized
            ) {
                searchInput.text
                    .toString()
            } else {
                restoredQuery
            },
        )

        outState.putInt(
            STATE_SCROLL_Y,
            if (
                ::sectionScroll.isInitialized
            ) {
                sectionScroll.scrollY
            } else {
                restoredScrollY
            },
        )

        super.onSaveInstanceState(
            outState,
        )
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
            headerAction(
                icon =
                    R.drawable.ic_arrow_back,
                description =
                    "Назад",
            ) {
                finish()
            }
        )

        topBar.addView(
            Ui.textView(
                context = this,
                value =
                    volumeTitle,
                sizeSp = 20f,
                color =
                    Ui.entityTitle,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                maxLines = 2
                setPadding(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        6,
                    ),
                    0,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
        )

        topBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_home,
                description =
                    "Головне меню",
            ) {
                openHome()
            }
        )

        topBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_search,
                description =
                    "Пошук розділів",
            ) {
                toggleSearch()
            }
        )

        topBar.addView(
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_VOLUME,
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

        topBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_settings,
                description =
                    "Налаштування",
            ) {
                startActivity(
                    Intent(
                        this,
                        SettingsActivity::class.java,
                    )
                )
            }
        )

        root.addView(topBar)

        val contextRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        8,
                    ),
                )
            }

        statusText =
            Ui.textView(
                context = this,
                value =
                    "Читаю native index розділів…",
                sizeSp = 13f,
                color =
                    Ui.muted,
            )

        contextRow.addView(
            statusText,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
        )

        val modeSwitch =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@ModernVolumeActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp =
                            12,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        2,
                    ),
                )
            }

        modeSwitch.addView(
            Ui.modeButton(
                context =
                    this,
                label =
                    "Modern",
                active =
                    true,
            ) {},
        )

        modeSwitch.addView(
            Ui.modeButton(
                context =
                    this,
                label =
                    "Classic",
                active =
                    false,
            ) {
                openClassicVolume()
            },
        )

        contextRow.addView(
            modeSwitch,
        )

        root.addView(contextRow)

        root.addView(
            Button(this).apply {
                text =
                    "Документація"
                isAllCaps = false
                textSize = 16f
                gravity = Gravity.CENTER
                minimumHeight =
                    Ui.dp(
                        this@ModernVolumeActivity,
                        48,
                    )
                setTextColor(
                    Ui.text,
                )
                background =
                    Ui.roundedBackground(
                        context =
                            this@ModernVolumeActivity,
                        fill =
                            Ui.surfaceAlt,
                        stroke =
                            Ui.accent,
                        radiusDp = 12,
                    )
                contentDescription =
                    "Документація тому — відкрити"
                setOnClickListener {
                    openVolumeDocumentation()
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@ModernVolumeActivity,
                        10,
                    )
            }
        )

        searchInput =
            EditText(this).apply {
                hint =
                    "Пошук: 101, генератор, ABS…"
                setSingleLine(true)
                visibility =
                    View.GONE
                setTextColor(
                    Ui.text,
                )
                setHintTextColor(
                    Ui.muted,
                )
                background =
                    Ui.roundedBackground(
                        context =
                            this@ModernVolumeActivity,
                        fill =
                            Ui.surfaceAlt,
                        stroke =
                            Ui.accent,
                        radiusDp = 10,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        14,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        10,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        14,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        10,
                    ),
                )
                addTextChangedListener(
                    object :
                        TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int,
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int,
                        ) {
                            renderSections(
                                s
                                    ?.toString()
                                    .orEmpty(),
                            )
                        }

                        override fun afterTextChanged(
                            s: Editable?,
                        ) = Unit
                    }
                )
            }

        root.addView(
            searchInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@ModernVolumeActivity,
                        10,
                    )
            }
        )

        sectionScroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
            }

        listContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        2,
                    ),
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        24,
                    ),
                )
            }

        sectionScroll.addView(
            listContainer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        )

        root.addView(
            sectionScroll,
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
            HELP_VOLUME ->
                HelpDialogSpec(
                    title =
                        "Розділи тому",
                    message =
                        "Тут показані розділи цього тому. Пошук працює за кодом і назвою.\n\n" +
                            "«Classic» відкриває оригінальну навігацію Renault, а «Документація» — матеріали цього тому.\n\n" +
                            "Коди розділів показуються у зручному порядку, але їхні оригінальні позначення не змінюються.",
                )

            else ->
                null
        }

    private fun loadSections() {
        statusText.text =
            "Читаю native index розділів…"

        Thread {
            val result =
                ModernSectionsReader
                    .read(
                        context = this,
                        treeUri =
                            Uri.parse(
                                treeUriText,
                            ),
                        volumeEntrypoint =
                            volumeEntrypoint,
                    )

            runOnUiThread {
                result
                    .onSuccess {
                        sectionIndex = it
                        showLoadedSections(
                            it,
                        )
                    }
                    .onFailure {
                        showNativeFallback(
                            it.message
                                ?: "Native index розділів недоступний."
                        )
                    }
            }
        }.start()
    }

    private fun showLoadedSections(
        sections: ModernVolumeSections,
    ) {
        if (
            sections.sections.isEmpty()
        ) {
            showNativeFallback(
                "Для цього тому розділи автоматично не знайдені."
            )
            return
        }

        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Розділів: " +
                sections.sections.size +
                " · native"

        renderSections(
            searchInput.text
                .toString(),
        )

        if (
            restoredScrollY >
            0
        ) {
            sectionScroll.post {
                sectionScroll.scrollTo(
                    0,
                    restoredScrollY,
                )
            }
        }
    }

    private fun renderSections(
        query: String,
    ) {
        if (
            !::listContainer.isInitialized
        ) {
            return
        }

        val current =
            sectionIndex
                ?: return

        listContainer.removeAllViews()

        val normalizedQuery =
            query.trim()
                .lowercase(
                    Locale.ROOT,
                )

        val filtered =
            ModernSectionDisplayOrder.sorted(
                current.sections.filter {
                    normalizedQuery.isBlank() ||
                        (
                            it.code +
                                " " +
                                it.title
                        )
                            .lowercase(
                                Locale.ROOT,
                            )
                            .contains(
                                normalizedQuery,
                            )
                }
            )

        if (
            filtered.isEmpty()
        ) {
            listContainer.addView(
                infoCard(
                    "Нічого не знайдено."
                )
            )
            return
        }

        filtered.forEach { section ->
            listContainer.addView(
                sectionRow(
                    section,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@ModernVolumeActivity,
                            6,
                        )
                }
            )
        }
    }

    private fun sectionRow(
        section: ModernSection,
    ): View =
        LinearLayout(this).apply {
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
                        this@ModernVolumeActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp = 12,
                )
            setPadding(
                Ui.dp(
                    this@ModernVolumeActivity,
                    12,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    12,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
            )

            val codeBox =
                Ui.textView(
                    context =
                        this@ModernVolumeActivity,
                    value =
                        section.code,
                    sizeSp = 18f,
                    color =
                        Ui.accent,
                ).apply {
                    gravity =
                        Gravity.CENTER
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                    background =
                        Ui.roundedBackground(
                            context =
                                this@ModernVolumeActivity,
                            fill =
                                Ui.surfaceAlt,
                            stroke =
                                Ui.accent,
                            radiusDp = 10,
                        )
                }

            addView(
                codeBox,
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        64,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        52,
                    ),
                )
            )

            addView(
                Ui.textView(
                    context =
                        this@ModernVolumeActivity,
                    value =
                        section.title,
                    sizeSp = 16f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                    setPadding(
                        Ui.dp(
                            this@ModernVolumeActivity,
                            14,
                        ),
                        0,
                        Ui.dp(
                            this@ModernVolumeActivity,
                            8,
                        ),
                        0,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                )
            )

            addView(
                Ui.textView(
                    context =
                        this@ModernVolumeActivity,
                    value =
                        "›",
                    sizeSp = 28f,
                    color =
                        Ui.muted,
                ).apply {
                    gravity =
                        Gravity.CENTER
                },
                LinearLayout.LayoutParams(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        36,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        52,
                    ),
                )
            )

            setOnClickListener {
                openSection(
                    section,
                )
            }
        }

    private fun openSection(
        section: ModernSection,
    ) {
        startActivity(
            NativeSectionActivity
                .intentForSection(
                    context = this,
                    treeUri =
                        treeUriText,
                    datasetTitle =
                        datasetTitle,
                    classicEntrypoint =
                        classicEntrypoint,
                    volumeTitle =
                        volumeTitle,
                    volumeEntrypoint =
                        volumeEntrypoint,
                    section =
                        section,
                )
        )
    }

    private fun openVolumeDocumentation() {
        startActivity(
            VolumeDocumentationActivity
                .intentForVolume(
                    context = this,
                    treeUri =
                        treeUriText,
                    datasetTitle =
                        datasetTitle,
                    classicEntrypoint =
                        classicEntrypoint,
                    volumeTitle =
                        volumeTitle,
                    volumeEntrypoint =
                        volumeEntrypoint,
                )
        )
    }

    private fun openClassicVolume() {
        startActivity(
            ViewerActivity
                .intentForEntrypoint(
                    context = this,
                    title =
                        volumeTitle,
                    entrypoint =
                        volumeEntrypoint,
                    treeUri =
                        treeUriText,
                    modernDatasetTitle =
                        datasetTitle,
                    modernClassicEntrypoint =
                        classicEntrypoint,
                    modernFocusEntrypoint =
                        volumeEntrypoint,
                    modernVolumeTitle =
                        volumeTitle,
                    modernVolumeEntrypoint =
                        volumeEntrypoint,
                )
        )
    }

    private fun showNativeFallback(
        message: String,
    ) {
        sectionIndex = null
        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Сумісний режим"

        listContainer.removeAllViews()

        val card =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@ModernVolumeActivity,
                        fill =
                            Ui.surface,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernVolumeActivity,
                        16,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        16,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        16,
                    ),
                    Ui.dp(
                        this@ModernVolumeActivity,
                        16,
                    ),
                )

                addView(
                    Ui.textView(
                        context =
                            this@ModernVolumeActivity,
                        value =
                            message,
                        sizeSp = 14f,
                        color =
                            Ui.muted,
                    )
                )

                addView(
                    Button(
                        this@ModernVolumeActivity,
                    ).apply {
                        text =
                            "Відкрити документацію"
                        isAllCaps =
                            false
                        gravity =
                            Gravity.CENTER
                        setOnClickListener {
                            openClassicVolume()
                        }
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin =
                            Ui.dp(
                                this@ModernVolumeActivity,
                                12,
                            )
                    }
                )
            }

        listContainer.addView(card)
    }

    private fun infoCard(
        message: String,
    ): View =
        Ui.textView(
            context = this,
            value = message,
            sizeSp = 15f,
            color =
                Ui.muted,
        ).apply {
            setPadding(
                Ui.dp(
                    this@ModernVolumeActivity,
                    16,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    18,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    16,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    18,
                ),
            )
            background =
                Ui.roundedBackground(
                    context =
                        this@ModernVolumeActivity,
                    fill =
                        Ui.surface,
                )
        }

    private fun toggleSearch() {
        if (
            searchInput.visibility ==
            View.VISIBLE
        ) {
            hideSearch()
            return
        }

        searchInput.visibility =
            View.VISIBLE
        searchInput.requestFocus()

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE,
            ) as InputMethodManager

        imm.showSoftInput(
            searchInput,
            InputMethodManager
                .SHOW_IMPLICIT,
        )
    }

    private fun hideSearch() {
        searchInput.clearFocus()
        searchInput.visibility =
            View.GONE

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE,
            ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            searchInput.windowToken,
            0,
        )
    }

    private fun openHome() {
        startActivity(
            Intent(
                this,
                MainActivity::class.java,
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        )
    }

    private fun headerAction(
        icon: Int,
        description: String,
        onClick: () -> Unit,
    ): ImageButton =
        ImageButton(this).apply {
            setImageResource(icon)
            contentDescription =
                description
            setBackgroundColor(
                android.graphics.Color.TRANSPARENT,
            )
            minimumWidth =
                Ui.dp(
                    this@ModernVolumeActivity,
                    44,
                )
            minimumHeight =
                Ui.dp(
                    this@ModernVolumeActivity,
                    44,
                )
            setPadding(
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
                Ui.dp(
                    this@ModernVolumeActivity,
                    10,
                ),
            )
            setOnClickListener {
                onClick()
            }
        }

    private fun showFatalError(
        message: String,
    ) {
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

        root.addView(
            Ui.textView(
                context = this,
                value =
                    volumeTitle,
                sizeSp = 22f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
            }
        )

        root.addView(
            Ui.textView(
                context = this,
                value =
                    message,
                sizeSp = 15f,
                color =
                    Ui.danger,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernVolumeActivity,
                        16,
                    ),
                    0,
                    0,
                )
            }
        )

        setContentView(root)
    }

    companion object {
        private const val EXTRA_DATASET_TITLE =
            "datasetTitle"
        private const val EXTRA_TREE_URI =
            "treeUri"
        private const val EXTRA_CLASSIC_ENTRYPOINT =
            "classicEntrypoint"
        private const val EXTRA_VOLUME_TITLE =
            "volumeTitle"
        private const val EXTRA_VOLUME_ENTRYPOINT =
            "volumeEntrypoint"
        private const val STATE_QUERY =
            "query"
        private const val STATE_SCROLL_Y =
            "scrollY"
        private const val HELP_VOLUME =
            "volume"
        private const val EXTRA_OPEN_SEARCH =
            "openSearch"

        fun intentForVolume(
            context: Context,
            datasetTitle: String,
            treeUri: String,
            classicEntrypoint: String,
            volumeTitle: String,
            volumeEntrypoint: String,
            openSearch: Boolean = false,
        ): Intent =
            Intent(
                context,
                ModernVolumeActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_DATASET_TITLE,
                    datasetTitle,
                )
                putExtra(
                    EXTRA_TREE_URI,
                    treeUri,
                )
                putExtra(
                    EXTRA_CLASSIC_ENTRYPOINT,
                    classicEntrypoint,
                )
                putExtra(
                    EXTRA_VOLUME_TITLE,
                    volumeTitle,
                )
                putExtra(
                    EXTRA_VOLUME_ENTRYPOINT,
                    volumeEntrypoint,
                )
                putExtra(
                    EXTRA_OPEN_SEARCH,
                    openSearch,
                )
            }
    }
}
