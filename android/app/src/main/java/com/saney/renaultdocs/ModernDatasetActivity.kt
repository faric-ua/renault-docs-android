package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class ModernDatasetActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var searchInput: EditText
    private lateinit var volumeScroll: ScrollView

    private var catalog: ModernCatalog? = null
    private var treeUriText: String = ""
    private var classicEntrypoint: String = ""
    private var fallbackTitle: String = "Renault dataset"
    private var focusEntrypoint: String = ""
    private var restoredQuery: String = ""
    private var restoredScrollY: Int = 0
    private var baseStatus: String = ""

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        treeUriText =
            intent.getStringExtra(
                EXTRA_TREE_URI,
            ).orEmpty()

        classicEntrypoint =
            intent.getStringExtra(
                EXTRA_CLASSIC_ENTRYPOINT,
            ).orEmpty()

        fallbackTitle =
            intent.getStringExtra(
                EXTRA_TITLE,
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: fallbackTitle

        focusEntrypoint =
            intent.getStringExtra(
                EXTRA_FOCUS_ENTRYPOINT,
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

        catalog =
            lastNonConfigurationInstance
                as? ModernCatalog

        if (treeUriText.isBlank()) {
            showFatalError(
                "Dataset не передав SAF URI.",
            )
            return
        }

        setContentView(
            buildContent(),
        )

        if (restoredQuery.isNotBlank()) {
            searchInput.setText(
                restoredQuery,
            )
            searchInput.setSelection(
                restoredQuery.length,
            )
        }

        loadCatalog()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
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
                        this@ModernDatasetActivity,
                        48,
                    )
                minimumHeight =
                    Ui.dp(
                        this@ModernDatasetActivity,
                        48,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernDatasetActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
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
                            this@ModernDatasetActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp =
                            12,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernDatasetActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        2,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
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
                openClassic()
            },
        )

        topBar.addView(
            View(
                this,
            ),
            LinearLayout.LayoutParams(
                0,
                1,
                1f,
            ),
        )

        topBar.addView(
            modeSwitch,
        )

        root.addView(topBar)

        root.addView(
            Ui.textView(
                context = this,
                value = fallbackTitle,
                sizeSp = 27f,
                color = Ui.entityTitle,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernDatasetActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@ModernDatasetActivity,
                        4,
                    ),
                )
            }
        )

        statusText =
            Ui.textView(
                context = this,
                value = "Читаю швидкий індекс…",
                sizeSp = 14f,
                color = Ui.muted,
            )

        root.addView(statusText)

        searchInput =
            EditText(this).apply {
                hint =
                    "Пошук: NT8236, 2005, wiring…"
                setSingleLine(true)
                setTextColor(
                    Ui.text,
                )
                setHintTextColor(
                    Ui.muted,
                )
                background =
                    Ui.roundedBackground(
                        context =
                            this@ModernDatasetActivity,
                        fill =
                            Ui.surfaceAlt,
                        stroke =
                            Ui.accent,
                        radiusDp =
                            11,
                    )
                setPadding(
                    Ui.dp(
                        this@ModernDatasetActivity,
                        14,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        10,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        14,
                    ),
                    Ui.dp(
                        this@ModernDatasetActivity,
                        10,
                    ),
                )
                addTextChangedListener(
                    object : TextWatcher {
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
                            renderVolumes(
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
                topMargin =
                    Ui.dp(
                        this@ModernDatasetActivity,
                        14,
                    )
                bottomMargin =
                    Ui.dp(
                        this@ModernDatasetActivity,
                        10,
                    )
            }
        )

        volumeScroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        listContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernDatasetActivity,
                        2,
                    ),
                    0,
                    Ui.dp(
                        this@ModernDatasetActivity,
                        24,
                    ),
                )
            }

        volumeScroll.addView(
            listContainer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        )

        root.addView(
            volumeScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )

        return root
    }

    private fun loadCatalog() {
        statusText.text =
            "Читаю швидкий індекс…"

        Thread {
            val result =
                ModernCatalogReader.read(
                    context = this,
                    treeUri =
                        Uri.parse(
                            treeUriText,
                        ),
                )

            runOnUiThread {
                result
                    .onSuccess {
                        catalog = it
                        baseStatus =
                            buildStatus(it)

                        statusText.setTextColor(
                            Ui.muted,
                        )
                        statusText.text =
                            baseStatus +
                                " · Fast Pack: готую…"

                        renderVolumes(
                            searchInput.text
                                .toString(),
                        )

                        prewarmFastPack()
                    }
                    .onFailure { error ->
                        statusText.setTextColor(
                            Ui.danger,
                        )

                        val detail =
                            error.message
                                ?: "Не вдалося прочитати Modern index."

                        statusText.text =
                            if (
                                detail.contains(
                                    "renault-dataset.json",
                                    ignoreCase =
                                        true,
                                )
                            ) {
                                "Ця стара бібліотека більше не читається за збереженим шляхом. " +
                                    "Якщо папку перенесено або видалено — додай її знову через Legacy."
                            } else {
                                detail
                            }
                    }
            }
        }.start()
    }

    private fun prewarmFastPack() {
        Thread {
            val result =
                FastContentArchive
                    .prepare(
                        context = this,
                        treeUri =
                            Uri.parse(
                                treeUriText,
                            ),
                    )

            result
                .onSuccess {
                    val hasArchive =
                        it.archive != null
                    val copied =
                        it.copiedToLocalCache

                    it.archive?.close()

                    runOnUiThread {
                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return@runOnUiThread
                        }

                        statusText.setTextColor(
                            Ui.muted,
                        )

                        statusText.text =
                            when {
                                hasArchive &&
                                    copied ->
                                    baseStatus +
                                        " · Fast Pack готовий"

                                hasArchive ->
                                    baseStatus +
                                        " · Fast Pack активний"

                                else ->
                                    baseStatus +
                                        " · SAF fallback"
                            }
                    }
                }
                .onFailure {
                    runOnUiThread {
                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return@runOnUiThread
                        }

                        statusText.setTextColor(
                            Ui.muted,
                        )
                        statusText.text =
                            baseStatus +
                                " · Fast Pack недоступний, SAF fallback"
                    }
                }
        }.start()
    }

    private fun buildStatus(
        catalog: ModernCatalog,
    ): String {
        val meta =
            listOfNotNull(
                catalog.model,
                catalog.yearsLabel,
                catalog.platform,
            ).joinToString(
                " · ",
            )

        return buildString {
            if (meta.isNotBlank()) {
                append(meta)
                append(" · ")
            }

            append("томів: ")
            append(
                catalog.volumes.size,
            )

            if (
                catalog.source ==
                "manifest-fallback"
            ) {
                append(
                    " · сумісний режим індексу",
                )
            } else {
                append(
                    " · швидкий індекс",
                )
            }
        }
    }

    private fun renderVolumes(
        query: String,
    ) {
        if (
            !::listContainer.isInitialized
        ) {
            return
        }

        val current =
            catalog
                ?: return

        listContainer.removeAllViews()

        val normalizedQuery =
            query.trim()
                .lowercase(
                    Locale.ROOT,
                )

        val filtered =
            current.volumes.filter {
                normalizedQuery.isBlank() ||
                    searchableText(it)
                        .contains(
                            normalizedQuery,
                        )
            }

        if (filtered.isEmpty()) {
            listContainer.addView(
                Ui.textView(
                    context = this,
                    value =
                        "Нічого не знайдено.",
                    sizeSp = 15f,
                    color = Ui.muted,
                ).apply {
                    setPadding(
                        Ui.dp(
                            this@ModernDatasetActivity,
                            16,
                        ),
                        Ui.dp(
                            this@ModernDatasetActivity,
                            20,
                        ),
                        Ui.dp(
                            this@ModernDatasetActivity,
                            16,
                        ),
                        Ui.dp(
                            this@ModernDatasetActivity,
                            20,
                        ),
                    )
                    background =
                        Ui.roundedBackground(
                            context =
                                this@ModernDatasetActivity,
                            fill =
                                Ui.surface,
                        )
                }
            )
            return
        }

        filtered.forEach { volume ->
            listContainer.addView(
                buildVolumeCard(
                    volume,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@ModernDatasetActivity,
                            10,
                        )
                }
            )
        }
         if (
            normalizedQuery.isBlank() &&
            focusEntrypoint.isNotBlank()
        ) {
            val focusedIndex =
                filtered.indexOfFirst {
                    it.entrypoint ==
                        focusEntrypoint
                }

            if (
                focusedIndex >= 0 &&
                ::volumeScroll.isInitialized
            ) {
                volumeScroll.post {
                    val child =
                        listContainer
                            .getChildAt(
                                focusedIndex,
                            )

                    if (child != null) {
                        volumeScroll
                            .smoothScrollTo(
                                0,
                                child.top,
                            )
                    }
                }
            }
        }
    }

    private fun searchableText(
        volume: ModernVolume,
    ): String =
        listOfNotNull(
            volume.title,
            volume.documentCode,
            volume.date,
            volume.kind,
            volume.sourceFolder,
        )
            .joinToString(" ")
            .lowercase(
                Locale.ROOT,
            )

    private fun buildVolumeCard(
        volume: ModernVolume,
    ): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            val isFocused =
                focusEntrypoint
                    .isNotBlank() &&
                    volume.entrypoint ==
                    focusEntrypoint

            background =
                Ui.roundedBackground(
                    context =
                        this@ModernDatasetActivity,
                    fill =
                        if (isFocused) {
                            Ui.surfaceAlt
                        } else {
                            Ui.surface
                        },
                    stroke =
                        if (isFocused) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                )
            setPadding(
                Ui.dp(
                    this@ModernDatasetActivity,
                    16,
                ),
                Ui.dp(
                    this@ModernDatasetActivity,
                    14,
                ),
                Ui.dp(
                    this@ModernDatasetActivity,
                    16,
                ),
                Ui.dp(
                    this@ModernDatasetActivity,
                    14,
                ),
            )

            addView(
                Ui.textView(
                    context =
                        this@ModernDatasetActivity,
                    value =
                        volume.title,
                    sizeSp = 19f,
                    color =
                        Ui.entityTitle,
                ).apply {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD,
                    )
                }
            )

            val subtitle =
                listOfNotNull(
                    volume.kind,
                    volume.sourceFolder,
                ).joinToString(
                    " · ",
                )

            if (subtitle.isNotBlank()) {
                addView(
                    Ui.textView(
                        context =
                            this@ModernDatasetActivity,
                        value =
                            subtitle,
                        sizeSp = 14f,
                        color = Ui.muted,
                    ).apply {
                        setPadding(
                            0,
                            Ui.dp(
                                this@ModernDatasetActivity,
                                6,
                            ),
                            0,
                            0,
                        )
                    }
                )
            }

            setOnClickListener {
                startActivity(
                    ModernVolumeActivity
                        .intentForVolume(
                            context =
                                this@ModernDatasetActivity,
                            datasetTitle =
                                fallbackTitle,
                            treeUri =
                                treeUriText,
                            classicEntrypoint =
                                classicEntrypoint,
                            volumeTitle =
                                volume.title,
                            volumeEntrypoint =
                                volume.entrypoint,
                        )
                )
            }
        }

    private fun openClassic() {
        if (
            classicEntrypoint.isBlank()
        ) {
            statusText.setTextColor(
                Ui.danger,
            )
            statusText.text =
                "Classic entrypoint відсутній."
            return
        }

        startActivity(
            ViewerActivity
                .intentForEntrypoint(
                    context = this,
                    title =
                        "$fallbackTitle · Classic",
                    entrypoint =
                        classicEntrypoint,
                    treeUri =
                        treeUriText,
                    modernDatasetTitle =
                        fallbackTitle,
                    modernClassicEntrypoint =
                        classicEntrypoint,
                )
        )
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
                value = fallbackTitle,
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
                value = message,
                sizeSp = 15f,
                color = Ui.danger,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ModernDatasetActivity,
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
        private const val EXTRA_TITLE =
            "title"
        private const val EXTRA_TREE_URI =
            "treeUri"
        private const val EXTRA_CLASSIC_ENTRYPOINT =
            "classicEntrypoint"
        private const val EXTRA_FOCUS_ENTRYPOINT =
            "focusEntrypoint"
        private const val STATE_QUERY =
            "query"

        fun intent(
            context: Context,
            record: DatasetRecord,
        ): Intent =
            intentForDataset(
                context = context,
                title = record.title,
                treeUri = record.treeUri,
                classicEntrypoint =
                    record.openEntrypoint,
            )

        fun intentForDataset(
            context: Context,
            title: String,
            treeUri: String,
            classicEntrypoint: String,
            focusEntrypoint: String? = null,
        ): Intent =
            Intent(
                context,
                ModernDatasetActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_TITLE,
                    title,
                )
                putExtra(
                    EXTRA_TREE_URI,
                    treeUri,
                )
                putExtra(
                    EXTRA_CLASSIC_ENTRYPOINT,
                    classicEntrypoint,
                )

                focusEntrypoint
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        putExtra(
                            EXTRA_FOCUS_ENTRYPOINT,
                            it,
                        )
                    }
            }
    }
}
