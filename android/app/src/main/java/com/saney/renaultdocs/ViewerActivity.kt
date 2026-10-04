package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ViewerActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var titleView: TextView
    private lateinit var statusView: TextView
    private lateinit var datasetClient: SafDatasetWebViewClient
    private lateinit var searchRow: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchCount: TextView
    private lateinit var appToolbar: LinearLayout
    private lateinit var workspaceFrame: FrameLayout
    private lateinit var viewerWorkspace: LinearLayout
    private lateinit var companionContainer: LinearLayout
    private lateinit var companionHeader: LinearLayout
    private lateinit var splitDivider: View
    private lateinit var splitOverlayControls: LinearLayout
    private lateinit var companionWebView: WebView
    private lateinit var companionClient: SafDatasetWebViewClient
    private lateinit var companionTitleView: TextView

    private val splitControlsHandler =
        Handler(
            Looper.getMainLooper(),
        )

    private val hideSplitControlsRunnable =
        Runnable {
            if (
                pdfFullscreen &&
                pdfCompanionVisible
            ) {
                splitControlsVisible =
                    false
                applySplitFocusMode()
            }
        }

    private var pendingPdfPath: String? = null
    private var pdfCompanionVisible: Boolean = false
    private var companionLoaded: Boolean = false
    private var restoredCompanionWebState: Bundle? = null
    private var pdfFullscreen: Boolean = false
    private var fullscreenControlSyncGeneration: Int = 0
    private var splitRatio: Float = 0.55f
    private var splitControlsVisible: Boolean = false
    private var searchVisibleBeforeFullscreen:
        Boolean = false
    private var treeUriText: String = ""
    private var modernDatasetTitle: String = ""
    private var modernClassicEntrypoint: String = ""
    private var modernFocusEntrypoint: String = ""
    private var modernVolumeTitle: String = ""
    private var modernVolumeEntrypoint: String = ""
    private var modernSectionCode: String = ""
    private var modernSectionLegacyEntrypoint: String = ""
    private var restoredSearchQuery: String = ""
    private var restoredSearchVisible: Boolean = false
    private var hybridSectionMode: Boolean = false
    private var sectionNavigatorLoading: Boolean = false
    private var sectionNavigatorSections:
        List<ModernSection> =
        emptyList()

    private var activeDialogKind: String = ""
    private var restoredSectionNavigatorQuery:
        String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingPdfPath =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_PDF_PATH,
                )

        pdfFullscreen =
            savedInstanceState
                ?.getBoolean(
                    STATE_PDF_FULLSCREEN,
                    false,
                )
                ?: false

        pdfCompanionVisible =
            savedInstanceState
                ?.getBoolean(
                    STATE_PDF_COMPANION_VISIBLE,
                    false,
                )
                ?: false

        splitRatio =
            savedInstanceState
                ?.getFloat(
                    STATE_PDF_SPLIT_RATIO,
                    0.55f,
                )
                ?.coerceIn(
                    0.20f,
                    0.80f,
                )
                ?: 0.55f

        splitControlsVisible =
            savedInstanceState
                ?.getBoolean(
                    STATE_PDF_SPLIT_CONTROLS_VISIBLE,
                    false,
                )
                ?: false

        restoredCompanionWebState =
            savedInstanceState
                ?.getBundle(
                    STATE_PDF_COMPANION_WEBVIEW,
                )

        restoredSearchQuery =
            savedInstanceState
                ?.getString(
                    STATE_SEARCH_QUERY,
                )
                .orEmpty()

        restoredSearchVisible =
            savedInstanceState
                ?.getBoolean(
                    STATE_SEARCH_VISIBLE,
                    false,
                )
                ?: false

        activeDialogKind =
            savedInstanceState
                ?.getString(
                    STATE_DIALOG_KIND,
                )
                .orEmpty()
        restoredSectionNavigatorQuery =
            savedInstanceState
                ?.getString(
                    STATE_SECTION_NAV_QUERY,
                )
                .orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: "Renault dataset"
        val entrypoint = intent.getStringExtra(EXTRA_ENTRYPOINT)
        treeUriText =
            intent.getStringExtra(
                EXTRA_TREE_URI,
            ).orEmpty()
        modernDatasetTitle =
            intent.getStringExtra(
                EXTRA_MODERN_DATASET_TITLE,
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: title
        modernClassicEntrypoint =
            intent.getStringExtra(
                EXTRA_MODERN_CLASSIC_ENTRYPOINT,
            ).orEmpty()
        modernFocusEntrypoint =
            intent.getStringExtra(
                EXTRA_MODERN_FOCUS_ENTRYPOINT,
            ).orEmpty()
        modernVolumeTitle =
            intent.getStringExtra(
                EXTRA_MODERN_VOLUME_TITLE,
            ).orEmpty()
        modernVolumeEntrypoint =
            intent.getStringExtra(
                EXTRA_MODERN_VOLUME_ENTRYPOINT,
            ).orEmpty()
        modernSectionCode =
            savedInstanceState
                ?.getString(
                    STATE_MODERN_SECTION_CODE,
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: intent.getStringExtra(
                    EXTRA_MODERN_SECTION_CODE,
                ).orEmpty()
        modernSectionLegacyEntrypoint =
            savedInstanceState
                ?.getString(
                    STATE_MODERN_SECTION_LEGACY_ENTRYPOINT,
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: intent.getStringExtra(
                    EXTRA_MODERN_SECTION_LEGACY_ENTRYPOINT,
                ).orEmpty()

        if (entrypoint.isNullOrBlank()) {
            showFatalError(
                title = title,
                message = "Dataset не передав стартову сторінку.",
            )
            return
        }

        if (treeUriText.isBlank()) {
            showFatalError(
                title = title,
                message = "Dataset не передав SAF URI.",
            )
            return
        }

        val treeUri = Uri.parse(treeUriText)
        hybridSectionMode =
            modernSectionCode
                .isNotBlank() &&
            modernVolumeEntrypoint
                .isNotBlank() &&
            entrypoint.equals(
                modernVolumeEntrypoint,
                ignoreCase = true,
            )
        val hybridSectionWarmup =
            hybridSectionMode &&
            savedInstanceState == null

        val classicVolumeMode =
            !hybridSectionMode &&
            entrypoint
                .replace(
                    "\\",
                    "/",
                )
                .endsWith(
                    "INDEX.HTM",
                    ignoreCase = true,
                )

        val legacyStandaloneMode =
            modernVolumeEntrypoint
                .isNotBlank() &&
            !entrypoint.equals(
                modernVolumeEntrypoint,
                ignoreCase = true,
            )

        datasetClient = runCatching {
            SafDatasetWebViewClient(
                context = this,
                treeUri = treeUri,
                legacyStandaloneMode =
                    legacyStandaloneMode,
                legacySectionCode =
                    modernSectionCode,
                legacySectionRootEntrypoint =
                    modernVolumeEntrypoint,
                legacySectionFallbackEntrypoint =
                    modernSectionLegacyEntrypoint,
                onHybridSectionReady = {
                        success ->
                    runOnUiThread {
                        if (
                            ::webView.isInitialized
                        ) {
                            webView.animate()
                                .alpha(1f)
                                .setDuration(
                                    140L,
                                )
                                .start()
                        }

                        if (
                            ::statusView.isInitialized
                        ) {
                            statusView.visibility =
                                if (success) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }

                            if (!success) {
                                statusView.setTextColor(
                                    Ui.danger,
                                )
                                statusView.text =
                                    "Modern shell: Classic runtime не вдалося повністю сховати."
                            }
                        }
                    }
                },
                onSavePdf = {
                    requestPdfSave(it)
                },
                onToggleFullscreen = {
                    togglePdfFullscreen()
                },
                onToggleCompanion = {
                    togglePdfCompanion()
                },
                pdfStateScope =
                    "main",
                pdfCompanionControlEnabled =
                    modernVolumeEntrypoint
                        .isNotBlank(),
            )
        }.getOrElse { error ->
            showFatalError(
                title = title,
                message = error.message
                    ?: "Android не зміг відкрити dataset.",
            )
            return
        }

        window.statusBarColor = Ui.background
        window.navigationBarColor = Ui.background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.background)
        }
        Ui.applySystemInsets(root)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        appToolbar = toolbar

        toolbar.addView(
            ImageButton(this).apply {
                setImageResource(R.drawable.ic_arrow_back)
                contentDescription = "Назад"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                minimumWidth = Ui.dp(this@ViewerActivity, 48)
                minimumHeight = Ui.dp(this@ViewerActivity, 48)
                setPadding(
                    Ui.dp(this@ViewerActivity, 12),
                    Ui.dp(this@ViewerActivity, 12),
                    Ui.dp(this@ViewerActivity, 12),
                    Ui.dp(this@ViewerActivity, 12),
                )
                setOnClickListener {
                    navigateBack()
                }
            },
            LinearLayout.LayoutParams(
                Ui.dp(this, 48),
                Ui.dp(this, 48),
            )
        )

        titleView = Ui.textView(
            context = this,
            value = title,
            sizeSp = 16f,
            color = Ui.entityTitle,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            setPadding(
                Ui.dp(this@ViewerActivity, 10),
                0,
                0,
                0,
            )
            maxLines = 2
        }

        toolbar.addView(
            titleView,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
        )

        if (
            modernClassicEntrypoint
                .isNotBlank()
        ) {
            toolbar.addView(
                Ui.modeButton(
                    context =
                        this,
                    label =
                        if (
                            hybridSectionMode
                        ) {
                            "Розділи"
                        } else {
                            "Modern"
                        },
                    active =
                        false,
                ) {
                    if (
                        hybridSectionMode
                    ) {
                        showSectionNavigator()
                    } else {
                        openModern()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    Ui.dp(
                        this,
                        38,
                    ),
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@ViewerActivity,
                            4,
                        )
                    marginEnd =
                        Ui.dp(
                            this@ViewerActivity,
                            4,
                        )
                },
            )
        }

        toolbar.addView(
            headerAction(
                icon =
                    R.drawable.ic_home,
                description =
                    "Головне меню",
            ) {
                openHome()
            }
        )

        toolbar.addView(
            headerAction(
                icon =
                    R.drawable.ic_search,
                description =
                    "Пошук на сторінці",
            ) {
                if (
                    searchRow.visibility ==
                    View.VISIBLE
                ) {
                    closePageSearch()
                } else {
                    showPageSearch()
                }
            }
        )

        if (hybridSectionMode) {
            toolbar.addView(
                Button(this).apply {
                    text = "DBG"
                    contentDescription =
                        "Frame debug"
                    isAllCaps = false
                    gravity =
                        Gravity.CENTER
                    minWidth =
                        Ui.dp(
                            this@ViewerActivity,
                            52,
                        )
                    minimumHeight =
                        Ui.dp(
                            this@ViewerActivity,
                            44,
                        )
                    setPadding(
                        Ui.dp(
                            this@ViewerActivity,
                            6,
                        ),
                        0,
                        Ui.dp(
                            this@ViewerActivity,
                            6,
                        ),
                        0,
                    )
                    setOnClickListener {
                        showFrameDebugReport()
                    }
                }
            )
        }

        toolbar.addView(
            headerAction(
                icon =
                    R.drawable.ic_settings,
                description =
                    "Налаштування",
            ) {
                startActivity(
                    Intent(
                        this@ViewerActivity,
                        SettingsActivity::class.java,
                    )
                )
            }
        )

        root.addView(toolbar)

        searchRow =
            buildSearchRow().apply {
                visibility =
                    View.GONE
            }

        root.addView(searchRow)

        statusView = Ui.textView(
            context = this,
            value =
                if (hybridSectionWarmup) {
                    "Відкриваю Modern · " +
                        modernSectionCode +
                        "…"
                } else {
                    "Відкриваю документацію…"
                },
            sizeSp = 12f,
            color = Ui.muted,
        ).apply {
            setPadding(
                0,
                Ui.dp(this@ViewerActivity, 4),
                0,
                Ui.dp(this@ViewerActivity, 6),
            )
        }
        root.addView(statusView)

        webView = WebView(this).apply {
            setBackgroundColor(
                if (classicVolumeMode) {
                    android.graphics.Color.WHITE
                } else {
                    Ui.background
                }
            )
            alpha =
                if (hybridSectionWarmup) {
                    0f
                } else {
                    1f
                }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false

            webViewClient = datasetClient

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(
                    view: WebView?,
                    pageTitle: String?,
                ) {
                    super.onReceivedTitle(
                        view,
                        pageTitle,
                    )

                    val legacyInfoPage =
                        pageTitle
                            ?.lowercase()
                            ?.contains(
                                "як користув",
                            )
                            ?: false

                    if (
                        legacyInfoPage
                    ) {
                        titleView.text =
                            compactLegacyInfoTitle(
                                pageTitle,
                            )
                    } else if (
                        !pageTitle.isNullOrBlank()
                    ) {
                        titleView.text =
                            pageTitle
                    }

                    reconcilePdfFullscreenPresentation()

                    if (
                        legacyInfoPage
                    ) {
                        statusView.setTextColor(
                            Ui.muted,
                        )
                        statusView.text =
                            "Classic-довідка · дані й кількість томів стосуються лише цього dataset."
                        statusView.visibility =
                            View.VISIBLE
                    } else if (
                        !hybridSectionWarmup ||
                        webView.alpha > 0f
                    ) {
                        statusView.visibility =
                            View.GONE
                    }
                }
            }
        }

        webView.setFindListener {
                activeMatchOrdinal,
                numberOfMatches,
                isDoneCounting,
            ->
            if (
                !isDoneCounting ||
                !::searchCount.isInitialized
            ) {
                return@setFindListener
            }

            searchCount.text =
                if (
                    numberOfMatches > 0
                ) {
                    (
                        activeMatchOrdinal + 1
                    ).toString() +
                        " / " +
                        numberOfMatches
                } else {
                    "0 / 0"
                }
        }

        viewerWorkspace =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        viewerWorkspace.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                splitRatio,
            )
        )

        splitDivider =
            buildSplitDivider().apply {
                visibility =
                    View.GONE
            }

        viewerWorkspace.addView(
            splitDivider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(
                    this,
                    12,
                ),
            )
        )

        companionContainer =
            buildCompanionContainer(
                treeUri,
            ).apply {
                visibility =
                    View.GONE
            }

        viewerWorkspace.addView(
            companionContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f - splitRatio,
            )
        )

        workspaceFrame =
            FrameLayout(this).apply {
                setBackgroundColor(
                    Ui.background,
                )
            }

        workspaceFrame.addView(
            viewerWorkspace,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        )

        splitOverlayControls =
            buildSplitOverlayControls().apply {
                visibility =
                    View.GONE
            }

        workspaceFrame.addView(
            splitOverlayControls,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        )

        root.addView(
            workspaceFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )

        attachSplitDoubleTap(
            webView,
        )
        attachSplitDoubleTap(
            companionWebView,
        )

        setContentView(root)

        val restoredCompanionHistory =
            restoredCompanionWebState
                ?.let {
                    companionWebView
                        .restoreState(it)
                }

        companionLoaded =
            restoredCompanionHistory != null

        if (
            pdfCompanionVisible &&
            modernVolumeEntrypoint
                .isNotBlank()
        ) {
            showPdfCompanion(
                loadIfNeeded =
                    !companionLoaded,
            )
        } else {
            pdfCompanionVisible =
                false
            companionContainer.visibility =
                View.GONE
        }

        if (pdfFullscreen) {
            searchVisibleBeforeFullscreen =
                restoredSearchVisible
        }

        val restoredHistory = savedInstanceState
            ?.let {
                webView.restoreState(it)
            }

        if (
            restoredHistory != null &&
            hybridSectionMode
        ) {
            webView.alpha = 1f
            statusView.visibility =
                View.GONE
        }

        if (restoredHistory == null) {
            val startUrl = runCatching {
                DatasetVirtualUrl.urlFor(entrypoint)
            }.getOrElse { error ->
                showFatalError(
                    title = title,
                    message = error.message
                        ?: "Некоректна стартова сторінка dataset.",
                )
                return
            }

            webView.loadUrl(startUrl)
        }

        reconcilePdfFullscreenPresentation()

        webView.post {
            reconcilePdfFullscreenPresentation()
        }

        if (
            restoredSearchVisible
        ) {
            showPageSearch(
                requestKeyboard = false,
            )

            if (
                restoredSearchQuery
                    .isNotBlank()
            ) {
                searchInput.setText(
                    restoredSearchQuery,
                )
                searchInput.setSelection(
                    restoredSearchQuery.length,
                )
                webView.findAllAsync(
                    restoredSearchQuery,
                )
            }
        }

        restoreTransientWindow()
    }

    private fun restoreTransientWindow() {
        when (activeDialogKind) {
            DIALOG_SECTION_NAVIGATOR ->
                showSectionNavigator()

            DIALOG_FRAME_DEBUG ->
                showFrameDebugReport()
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        if (::webView.isInitialized) {
            webView.saveState(outState)
        }

        outState.putString(
            STATE_PENDING_PDF_PATH,
            pendingPdfPath,
        )

        outState.putString(
            STATE_SEARCH_QUERY,
            if (
                ::searchInput.isInitialized
            ) {
                searchInput.text
                    .toString()
            } else {
                restoredSearchQuery
            },
        )

        outState.putBoolean(
            STATE_SEARCH_VISIBLE,
            (
                ::searchRow.isInitialized &&
                searchRow.visibility ==
                View.VISIBLE
            ) ||
                (
                    pdfFullscreen &&
                    searchVisibleBeforeFullscreen
                ),
        )

        outState.putBoolean(
            STATE_PDF_FULLSCREEN,
            pdfFullscreen,
        )

        outState.putBoolean(
            STATE_PDF_COMPANION_VISIBLE,
            pdfCompanionVisible,
        )

        outState.putFloat(
            STATE_PDF_SPLIT_RATIO,
            splitRatio,
        )

        outState.putBoolean(
            STATE_PDF_SPLIT_CONTROLS_VISIBLE,
            splitControlsVisible,
        )

        if (
            ::companionWebView.isInitialized
        ) {
            val companionState =
                Bundle()
            companionWebView.saveState(
                companionState,
            )
            outState.putBundle(
                STATE_PDF_COMPANION_WEBVIEW,
                companionState,
            )
        }

        outState.putString(
            STATE_MODERN_SECTION_CODE,
            modernSectionCode,
        )
        outState.putString(
            STATE_MODERN_SECTION_LEGACY_ENTRYPOINT,
            modernSectionLegacyEntrypoint,
        )
        outState.putString(
            STATE_DIALOG_KIND,
            activeDialogKind,
        )
        outState.putString(
            STATE_SECTION_NAV_QUERY,
            restoredSectionNavigatorQuery,
        )
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Uses platform document result for minSdk 26 compatibility.")
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
            REQUEST_SAVE_PDF
        ) {
            return
        }

        val sourcePath =
            pendingPdfPath

        pendingPdfPath = null

        if (
            resultCode != RESULT_OK
        ) {
            showTransientStatus(
                "Збереження PDF скасовано.",
            )
            return
        }

        val destinationUri =
            data?.data

        if (
            sourcePath.isNullOrBlank() ||
            destinationUri == null
        ) {
            showTransientStatus(
                "Android не повернув файл призначення.",
                isError = true,
            )
            return
        }

        showTransientStatus(
            "Зберігаю PDF…",
        )

        Thread {
            datasetClient
                .copyPdfTo(
                    relativePath =
                        sourcePath,
                    destinationUri =
                        destinationUri,
                )
                .onSuccess { bytes ->
                    runOnUiThread {
                        showTransientStatus(
                            "PDF збережено · " +
                                formatBytes(bytes),
                        )

                        Toast
                            .makeText(
                                this,
                                "PDF збережено",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        showTransientStatus(
                            error.message
                                ?: "Не вдалося зберегти PDF.",
                            isError = true,
                        )
                    }
                }
        }.start()
    }

    override fun onBackPressed() {
        if (pdfFullscreen) {
            pdfFullscreen = false
            applyPdfFullscreen(
                enabled = false,
            )
            return
        }

        navigateBack()
    }

    override fun onConfigurationChanged(
        newConfig: Configuration,
    ) {
        super.onConfigurationChanged(
            newConfig,
        )

        if (
            pdfCompanionVisible &&
            ::viewerWorkspace.isInitialized
        ) {
            applySplitRatio()
        }

        reconcilePdfFullscreenPresentation()

        window.decorView.post {
            if (
                pdfCompanionVisible &&
                ::viewerWorkspace.isInitialized
            ) {
                applySplitRatio()
            }

            reconcilePdfFullscreenPresentation()
        }

        window.decorView.postDelayed(
            {
                reconcilePdfFullscreenPresentation()
            },
            180L,
        )
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean,
    ) {
        super.onWindowFocusChanged(
            hasFocus,
        )

        if (hasFocus) {
            reconcilePdfFullscreenPresentation()
        }
    }

    override fun onDestroy() {
        splitControlsHandler
            .removeCallbacks(
                hideSplitControlsRunnable,
            )

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.setFindListener(null)
            webView.webChromeClient = null
            webView.destroy()
        }

        if (::companionWebView.isInitialized) {
            companionWebView.stopLoading()
            companionWebView.webChromeClient =
                null
            companionWebView.destroy()
        }

        if (::companionClient.isInitialized) {
            companionClient.close()
        }

        if (::datasetClient.isInitialized) {
            datasetClient.close()
        }

        super.onDestroy()
    }

    private fun compactLegacyInfoTitle(
        pageTitle: String?,
    ): String {
        val pageIdentity =
            pageTitle
                .orEmpty()
                .replaceFirst(
                    Regex(
                        "^\\s*Як\\s+користув(?:атися|атись)\\s*[—–-]\\s*",
                        RegexOption.IGNORE_CASE,
                    ),
                    "",
                )
                .trim()

        val source =
            pageIdentity
                .ifBlank {
                    modernDatasetTitle
                        .trim()
                }
                .ifBlank {
                    "Renault Docs"
                }

        val withoutBrand =
            source
                .replaceFirst(
                    Regex(
                        "^Renault\\s+",
                        RegexOption.IGNORE_CASE,
                    ),
                    "",
                )
                .trim()
                .ifBlank {
                    source
                }

        val match =
            Regex(
                "^(.*?)(?:\\s*[·,|]\\s*|\\s+)(\\d{4})\\s*[-–—]\\s*(\\d{4})\\z",
            ).matchEntire(
                withoutBrand,
            )

        if (
            match == null
        ) {
            return withoutBrand
        }

        val name =
            match.groupValues[1]
                .trim()
        val years =
            match.groupValues[2] +
                "–" +
                match.groupValues[3]

        return if (
            name.isBlank()
        ) {
            withoutBrand
        } else {
            name +
                "\n" +
                years
        }
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
                    this@ViewerActivity,
                    44,
                )
            minimumHeight =
                Ui.dp(
                    this@ViewerActivity,
                    44,
                )
            setPadding(
                Ui.dp(
                    this@ViewerActivity,
                    10,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    10,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    10,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    10,
                ),
            )
            setOnClickListener {
                onClick()
            }
        }

    private fun buildSearchRow(): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@ViewerActivity,
                    fill =
                        Ui.surface,
                    radiusDp = 10,
                )
            setPadding(
                Ui.dp(
                    this@ViewerActivity,
                    8,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    6,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    8,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    6,
                ),
            )

            val queryLine =
                LinearLayout(
                    this@ViewerActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            searchInput =
                EditText(
                    this@ViewerActivity,
                ).apply {
                    hint =
                        "Пошук на сторінці"
                    setSingleLine(true)
                    setTextColor(
                        Ui.text,
                    )
                    setHintTextColor(
                        Ui.muted,
                    )
                    textSize = 18f
                    background =
                        Ui.roundedBackground(
                            context =
                                this@ViewerActivity,
                            fill =
                                Ui.surfaceAlt,
                            stroke =
                                Ui.border,
                            radiusDp = 8,
                        )
                    setPadding(
                        Ui.dp(
                            this@ViewerActivity,
                            12,
                        ),
                        0,
                        Ui.dp(
                            this@ViewerActivity,
                            12,
                        ),
                        0,
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
                                val query =
                                    s
                                        ?.toString()
                                        .orEmpty()

                                if (
                                    !::webView.isInitialized
                                ) {
                                    return
                                }

                                if (
                                    query.isBlank()
                                ) {
                                    webView
                                        .clearMatches()
                                    searchCount
                                        .text =
                                        "0 / 0"
                                } else {
                                    webView
                                        .findAllAsync(
                                            query,
                                        )
                                }
                            }

                            override fun afterTextChanged(
                                s: Editable?,
                            ) = Unit
                        }
                    )
                }

            queryLine.addView(
                searchInput,
                LinearLayout.LayoutParams(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        46,
                    ),
                    1f,
                )
            )

            searchCount =
                Ui.textView(
                    context =
                        this@ViewerActivity,
                    value =
                        "0 / 0",
                    sizeSp = 14f,
                    color =
                        Ui.muted,
                ).apply {
                    gravity =
                        Gravity.CENTER
                    minWidth =
                        Ui.dp(
                            this@ViewerActivity,
                            64,
                        )
                    setPadding(
                        Ui.dp(
                            this@ViewerActivity,
                            8,
                        ),
                        0,
                        0,
                        0,
                    )
                }

            queryLine.addView(
                searchCount,
            )

            addView(
                queryLine,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            )

            val controls =
                LinearLayout(
                    this@ViewerActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        0,
                        Ui.dp(
                            this@ViewerActivity,
                            6,
                        ),
                        0,
                        0,
                    )
                }

            controls.addView(
                searchButton(
                    "↑",
                    "Попередній збіг",
                ) {
                    if (
                        ::webView.isInitialized
                    ) {
                        webView.findNext(
                            false,
                        )
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        44,
                    ),
                    1f,
                ).apply {
                    marginEnd =
                        Ui.dp(
                            this@ViewerActivity,
                            4,
                        )
                }
            )

            controls.addView(
                searchButton(
                    "↓",
                    "Наступний збіг",
                ) {
                    if (
                        ::webView.isInitialized
                    ) {
                        webView.findNext(
                            true,
                        )
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        44,
                    ),
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@ViewerActivity,
                            2,
                        )
                    marginEnd =
                        Ui.dp(
                            this@ViewerActivity,
                            2,
                        )
                }
            )

            controls.addView(
                searchButton(
                    "×",
                    "Закрити пошук",
                ) {
                    closePageSearch()
                },
                LinearLayout.LayoutParams(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        44,
                    ),
                    1f,
                ).apply {
                    marginStart =
                        Ui.dp(
                            this@ViewerActivity,
                            4,
                        )
                }
            )

            addView(
                controls,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            )
        }

    private fun searchButton(
        label: String,
        description: String,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            contentDescription =
                description
            isAllCaps = false
            gravity =
                Gravity.CENTER
            minWidth =
                Ui.dp(
                    this@ViewerActivity,
                    42,
                )
            minimumHeight =
                Ui.dp(
                    this@ViewerActivity,
                    42,
                )
            setPadding(
                0,
                0,
                0,
                0,
            )
            setOnClickListener {
                onClick()
            }
        }

    private fun showPageSearch(
        requestKeyboard: Boolean = true,
    ) {
        if (
            !::searchRow.isInitialized
        ) {
            return
        }

        searchRow.visibility =
            View.VISIBLE
        searchInput.requestFocus()

        val existingQuery =
            searchInput.text
                ?.toString()
                .orEmpty()

        if (
            existingQuery.isNotBlank() &&
            ::webView.isInitialized
        ) {
            webView.findAllAsync(
                existingQuery,
            )
        }

        if (
            requestKeyboard
        ) {
            val imm =
                getSystemService(
                    INPUT_METHOD_SERVICE,
                ) as
                    InputMethodManager

            imm.showSoftInput(
                searchInput,
                InputMethodManager
                    .SHOW_IMPLICIT,
            )
        }
    }

    private fun closePageSearch() {
        if (
            !::searchRow.isInitialized
        ) {
            return
        }

        if (
            ::webView.isInitialized
        ) {
            webView.clearMatches()
        }

        searchInput.clearFocus()
        searchRow.visibility =
            View.GONE

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE,
            ) as
                InputMethodManager

        imm.hideSoftInputFromWindow(
            searchInput.windowToken,
            0,
        )
    }

    private fun showSectionNavigator() {
        activeDialogKind =
            DIALOG_SECTION_NAVIGATOR

        if (
            !hybridSectionMode ||
            modernVolumeEntrypoint
                .isBlank()
        ) {
            activeDialogKind = ""
            openModern()
            return
        }

        if (
            sectionNavigatorSections
                .isNotEmpty()
        ) {
            showSectionNavigatorDialog()
            return
        }

        if (sectionNavigatorLoading) {
            return
        }

        sectionNavigatorLoading = true

        showTransientStatus(
            "Читаю список розділів…",
        )

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
                            modernVolumeEntrypoint,
                    )

            runOnUiThread {
                sectionNavigatorLoading =
                    false

                result
                    .onSuccess {
                        sectionNavigatorSections =
                            it.sections

                        statusView.visibility =
                            View.GONE

                        if (
                            sectionNavigatorSections
                                .isEmpty()
                        ) {
                            activeDialogKind = ""
                            showTransientStatus(
                                "Native список розділів порожній.",
                                isError = true,
                            )
                        } else {
                            showSectionNavigatorDialog()
                        }
                    }
                    .onFailure { error ->
                        activeDialogKind = ""
                        showTransientStatus(
                            error.message
                                ?: "Не вдалося прочитати Modern-розділи.",
                            isError = true,
                        )
                    }
            }
        }.start()
    }

    private fun showSectionNavigatorDialog() {
        val sections =
            sectionNavigatorSections

        if (sections.isEmpty()) {
            return
        }

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ViewerActivity,
                        8,
                    ),
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    0,
                )
            }

        val search =
            EditText(this).apply {
                hint =
                    "Пошук: 101, генератор, ABS…"
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
                            this@ViewerActivity,
                        fill =
                            Ui.surfaceAlt,
                        stroke =
                            Ui.border,
                        radiusDp = 10,
                    )
                setPadding(
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    0,
                )
            }

        val count =
            Ui.textView(
                context = this,
                value = "",
                sizeSp = 12f,
                color = Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        6,
                    ),
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        6,
                    ),
                )
            }

        val list =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val scroll =
            ScrollView(this).apply {
                addView(
                    list,
                    android.widget.FrameLayout
                        .LayoutParams(
                            android.widget.FrameLayout
                                .LayoutParams
                                .MATCH_PARENT,
                            android.widget.FrameLayout
                                .LayoutParams
                                .WRAP_CONTENT,
                        )
                )
            }

        root.addView(
            search,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                Ui.dp(
                    this,
                    48,
                ),
            )
        )
        root.addView(count)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                (
                    resources
                        .displayMetrics
                        .heightPixels *
                        0.58f
                    )
                    .toInt()
                    .coerceAtLeast(
                        Ui.dp(
                            this,
                            260,
                        )
                    ),
            )
        )

        lateinit var dialog:
            AlertDialog

        fun render(
            query: String,
        ) {
            val normalized =
                query.trim()
                    .lowercase(
                        java.util.Locale.ROOT,
                    )

            val filtered =
                sections.filter { section ->
                    normalized.isBlank() ||
                        (
                            section.code +
                                " " +
                                section.title
                        )
                            .lowercase(
                                java.util.Locale.ROOT,
                            )
                            .contains(
                                normalized,
                            )
                }

            count.text =
                "Знайдено: " +
                    filtered.size +
                    " / " +
                    sections.size

            list.removeAllViews()

            if (filtered.isEmpty()) {
                list.addView(
                    Ui.textView(
                        context =
                            this@ViewerActivity,
                        value =
                            "Нічого не знайдено.",
                        sizeSp = 15f,
                        color = Ui.muted,
                    ).apply {
                        setPadding(
                            Ui.dp(
                                this@ViewerActivity,
                                8,
                            ),
                            Ui.dp(
                                this@ViewerActivity,
                                16,
                            ),
                            Ui.dp(
                                this@ViewerActivity,
                                8,
                            ),
                            Ui.dp(
                                this@ViewerActivity,
                                16,
                            ),
                        )
                    }
                )
                return
            }

            filtered.forEach { section ->
                val active =
                    section.code ==
                        modernSectionCode

                list.addView(
                    Button(
                        this@ViewerActivity,
                    ).apply {
                        text =
                            (
                                if (active) {
                                    "✓ "
                                } else {
                                    ""
                                }
                            ) +
                                section.code +
                                " · " +
                                section.title
                        isAllCaps = false
                        gravity =
                            Gravity.START or
                                Gravity.CENTER_VERTICAL
                        minimumHeight =
                            Ui.dp(
                                this@ViewerActivity,
                                48,
                            )
                        setOnClickListener {
                            if (active) {
                                dialog.dismiss()
                            } else {
                                switchLiveSection(
                                    section =
                                        section,
                                    dialog =
                                        dialog,
                                )
                            }
                        }
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                            .MATCH_PARENT,
                        LinearLayout.LayoutParams
                            .WRAP_CONTENT,
                    ).apply {
                        bottomMargin =
                            Ui.dp(
                                this@ViewerActivity,
                                4,
                            )
                    }
                )
            }
        }

        search.addTextChangedListener(
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
                    restoredSectionNavigatorQuery =
                        s
                            ?.toString()
                            .orEmpty()
                    render(
                        restoredSectionNavigatorQuery,
                    )
                }

                override fun afterTextChanged(
                    s: Editable?,
                ) = Unit
            }
        )

        dialog =
            AlertDialog
                .Builder(this)
                .setTitle(
                    "Розділи · " +
                        modernVolumeTitle
                            .takeIf {
                                it.isNotBlank()
                            }
                            .orEmpty(),
                )
                .setView(root)
                .setPositiveButton(
                    "Повний Modern",
                ) { _, _ ->
                    openModern()
                }
                .setNegativeButton(
                    "Закрити",
                    null,
                )
                .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
            restoredSectionNavigatorQuery = ""
        }

        if (
            restoredSectionNavigatorQuery
                .isNotBlank()
        ) {
            search.setText(
                restoredSectionNavigatorQuery,
            )
            search.setSelection(
                restoredSectionNavigatorQuery.length,
            )
        } else {
            render("")
        }

        dialog.show()
    }

    private fun switchLiveSection(
        section: ModernSection,
        dialog: AlertDialog,
    ) {
        if (
            !::webView.isInitialized ||
            !::datasetClient.isInitialized
        ) {
            return
        }

        showTransientStatus(
            "Перемикаю на " +
                section.code +
                "…",
        )

        datasetClient
            .switchHybridSection(
                view = webView,
                sectionCode =
                    section.code,
            ) { success ->
                runOnUiThread {
                    if (success) {
                        modernSectionCode =
                            section.code
                        modernSectionLegacyEntrypoint =
                            section.entrypoint

                        titleView.text =
                            section.code +
                                " · " +
                                section.title

                        statusView.visibility =
                            View.GONE

                        dialog.dismiss()

                        Toast
                            .makeText(
                                this,
                                "Розділ " +
                                    section.code,
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    } else {
                        showTransientStatus(
                            "Не вдалося перемкнутися на " +
                                section.code +
                                ". Зніми DBG у цьому стані.",
                            isError = true,
                        )
                    }
                }
            }
    }

    private fun showFrameDebugReport() {
        if (
            !::webView.isInitialized ||
            !::datasetClient.isInitialized
        ) {
            return
        }

        showTransientStatus(
            "Збираю frame-tree…",
        )

        datasetClient
            .collectFrameDebugReport(
                view = webView,
            ) { report ->
                runOnUiThread {
                    statusView.visibility =
                        View.GONE
                    showFrameDebugDialog(
                        report,
                    )
                }
            }
    }

    private fun showFrameDebugDialog(
        report: String,
    ) {
        activeDialogKind =
            DIALOG_FRAME_DEBUG
        val reportView =
            TextView(this).apply {
                text = report
                textSize = 11f
                setTextColor(
                    Ui.text,
                )
                setTextIsSelectable(
                    true,
                )
                typeface =
                    android.graphics.Typeface
                        .MONOSPACE
                setPadding(
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                    Ui.dp(
                        this@ViewerActivity,
                        12,
                    ),
                )
            }

        val scroll =
            ScrollView(this).apply {
                addView(
                    reportView,
                )
            }

        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle(
                "Frame debug · " +
                    modernSectionCode,
            )
            .setView(scroll)
            .setPositiveButton(
                "Копіювати",
            ) { _, _ ->
                val clipboard =
                    getSystemService(
                        CLIPBOARD_SERVICE,
                    ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "Renault frame debug",
                        report,
                    )
                )

                Toast
                    .makeText(
                        this,
                        "Frame debug скопійовано",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
            .setNegativeButton(
                "Закрити",
                null,
            )
            .create()

        dialog.setOnDismissListener {
            activeDialogKind = ""
        }
        dialog.show()
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

    private fun openModern() {
        if (
            modernClassicEntrypoint
                .isBlank()
        ) {
            return
        }

        val target =
            if (
                modernVolumeEntrypoint
                    .isNotBlank()
            ) {
                ModernVolumeActivity
                    .intentForVolume(
                        context = this,
                        datasetTitle =
                            modernDatasetTitle,
                        treeUri =
                            treeUriText,
                        classicEntrypoint =
                            modernClassicEntrypoint,
                        volumeTitle =
                            modernVolumeTitle
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?: titleView.text
                                    .toString(),
                        volumeEntrypoint =
                            modernVolumeEntrypoint,
                    )
            } else {
                ModernDatasetActivity
                    .intentForDataset(
                        context = this,
                        title =
                            modernDatasetTitle,
                        treeUri =
                            treeUriText,
                        classicEntrypoint =
                            modernClassicEntrypoint,
                        focusEntrypoint =
                            modernFocusEntrypoint
                                .takeIf {
                                    it.isNotBlank()
                                },
                    )
            }

        startActivity(
            target.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        )
    }

    private fun requestPdfSave(
        relativePath: String,
    ) {
        if (
            pendingPdfPath != null
        ) {
            return
        }

        pendingPdfPath =
            relativePath

        val suggestedName =
            relativePath
                .substringAfterLast('/')
                .takeIf {
                    it.isNotBlank()
                }
                ?: "renault-document.pdf"

        val intent =
            Intent(
                Intent.ACTION_CREATE_DOCUMENT,
            ).apply {
                addCategory(
                    Intent.CATEGORY_OPENABLE,
                )
                type =
                    "application/pdf"
                putExtra(
                    Intent.EXTRA_TITLE,
                    suggestedName,
                )
            }

        @Suppress("DEPRECATION")
        startActivityForResult(
            intent,
            REQUEST_SAVE_PDF,
        )
    }

    private fun showTransientStatus(
        message: String,
        isError: Boolean = false,
    ) {
        if (
            !::statusView.isInitialized
        ) {
            return
        }

        statusView.visibility =
            View.VISIBLE
        statusView.setTextColor(
            if (isError) {
                Ui.danger
            } else {
                Ui.muted
            }
        )
        statusView.text =
            message
    }

    private fun formatBytes(
        bytes: Long,
    ): String {
        if (
            bytes <
            1024L * 1024L
        ) {
            return (
                bytes / 1024L
            ).coerceAtLeast(1L)
                .toString() +
                " KB"
        }

        val mb =
            bytes.toDouble() /
                (1024.0 * 1024.0)

        return String.format(
            java.util.Locale.ROOT,
            "%.1f MB",
            mb,
        )
    }

    private fun buildSplitDivider(): View =
        TextView(this).apply {
            text = "━━━━"
            gravity =
                Gravity.CENTER
            textSize = 11f
            setTextColor(
                Ui.accent,
            )
            contentDescription =
                "Роздільник документів. Перетягніть вгору або вниз."
            background =
                Ui.roundedBackground(
                    context =
                        this@ViewerActivity,
                    fill =
                        Ui.surfaceAlt,
                    stroke =
                        Ui.border,
                    radiusDp = 6,
                )

            setOnTouchListener {
                    view,
                    event ->
                if (!pdfCompanionVisible) {
                    return@setOnTouchListener false
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.parent
                            ?.requestDisallowInterceptTouchEvent(
                                true,
                            )
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val workspaceLocation =
                            IntArray(2)
                        viewerWorkspace
                            .getLocationOnScreen(
                                workspaceLocation,
                            )

                        val usableHeight =
                            (
                                viewerWorkspace.height -
                                    splitDivider.height
                            )
                                .coerceAtLeast(1)

                        val relativeY =
                            (
                                event.rawY -
                                    workspaceLocation[1] -
                                    splitDivider.height /
                                        2f
                            )
                                .coerceIn(
                                    usableHeight *
                                        0.20f,
                                    usableHeight *
                                        0.80f,
                                )

                        splitRatio =
                            (
                                relativeY /
                                    usableHeight
                            )
                                .coerceIn(
                                    0.20f,
                                    0.80f,
                                )

                        applySplitRatio()
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        view.parent
                            ?.requestDisallowInterceptTouchEvent(
                                false,
                            )
                        true
                    }

                    else ->
                        false
                }
            }
        }

    private fun applySplitRatio() {
        if (
            !::webView.isInitialized ||
            !::companionContainer.isInitialized
        ) {
            return
        }

        splitRatio =
            splitRatio.coerceIn(
                0.20f,
                0.80f,
            )

        (
            webView.layoutParams as?
                LinearLayout.LayoutParams
        )?.let {
            it.height = 0
            it.weight =
                splitRatio
            webView.layoutParams =
                it
        }

        (
            companionContainer.layoutParams as?
                LinearLayout.LayoutParams
        )?.let {
            it.height = 0
            it.weight =
                1f - splitRatio
            companionContainer.layoutParams =
                it
        }

        viewerWorkspace.requestLayout()
    }

    private fun buildSplitOverlayControls():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER
            setPadding(
                Ui.dp(
                    this@ViewerActivity,
                    6,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    4,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    6,
                ),
                Ui.dp(
                    this@ViewerActivity,
                    4,
                ),
            )
            background =
                Ui.roundedBackground(
                    context =
                        this@ViewerActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.accent,
                    radiusDp = 12,
                )
            elevation =
                Ui.dp(
                    this@ViewerActivity,
                    8,
                ).toFloat()

            addView(
                Button(
                    this@ViewerActivity,
                ).apply {
                    text = "← Док"
                    isAllCaps = false
                    contentDescription =
                        "Назад у документації"
                    minimumHeight =
                        Ui.dp(
                            this@ViewerActivity,
                            42,
                        )
                    setOnClickListener {
                        companionBack()

                        if (
                            pdfFullscreen &&
                            pdfCompanionVisible
                        ) {
                            showSplitControlsTemporarily()
                        }
                    }
                }
            )

            addView(
                Button(
                    this@ViewerActivity,
                ).apply {
                    text = "× Док"
                    isAllCaps = false
                    contentDescription =
                        "Закрити другий документ"
                    minimumHeight =
                        Ui.dp(
                            this@ViewerActivity,
                            42,
                        )
                    setOnClickListener {
                        hidePdfCompanion()
                    }
                }
            )

            addView(
                Button(
                    this@ViewerActivity,
                ).apply {
                    text = "⛶"
                    isAllCaps = false
                    contentDescription =
                        "Вийти з повноекранного режиму"
                    minWidth = 0
                    minimumWidth =
                        Ui.dp(
                            this@ViewerActivity,
                            48,
                        )
                    minimumHeight =
                        Ui.dp(
                            this@ViewerActivity,
                            42,
                        )
                    setOnClickListener {
                        if (pdfFullscreen) {
                            togglePdfFullscreen()
                        }
                    }
                }
            )
        }

    private fun attachSplitDoubleTap(
        target: View,
    ) {
        val detector =
            GestureDetector(
                this,
                object :
                    GestureDetector
                        .SimpleOnGestureListener() {
                    override fun onDown(
                        event: MotionEvent,
                    ): Boolean =
                        true

                    override fun onDoubleTap(
                        event: MotionEvent,
                    ): Boolean {
                        if (
                            !pdfFullscreen ||
                            !pdfCompanionVisible
                        ) {
                            return false
                        }

                        showSplitControlsTemporarily()
                        return true
                    }
                },
            )

        target.setOnTouchListener {
                _,
                event ->
            detector.onTouchEvent(
                event,
            )
            false
        }
    }

    private fun showSplitControlsTemporarily() {
        if (
            !pdfFullscreen ||
            !pdfCompanionVisible
        ) {
            return
        }

        splitControlsVisible =
            true
        applySplitFocusMode()

        splitControlsHandler
            .removeCallbacks(
                hideSplitControlsRunnable,
            )
        splitControlsHandler
            .postDelayed(
                hideSplitControlsRunnable,
                SPLIT_CONTROLS_TIMEOUT_MS,
            )
    }

    private fun applySplitFocusMode() {
        val active =
            pdfFullscreen &&
                pdfCompanionVisible

        if (!active) {
            splitControlsHandler
                .removeCallbacks(
                    hideSplitControlsRunnable,
                )
        }

        if (
            ::companionHeader.isInitialized
        ) {
            companionHeader.visibility =
                if (active) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        if (
            ::splitOverlayControls.isInitialized
        ) {
            splitOverlayControls.visibility =
                if (
                    active &&
                    splitControlsVisible
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        val script =
            "window.renaultSetSplitFocus && " +
                "window.renaultSetSplitFocus(" +
                active.toString() +
                "," +
                splitControlsVisible.toString() +
                ");"

        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                script,
                null,
            )
        }

        if (::companionWebView.isInitialized) {
            companionWebView.evaluateJavascript(
                script,
                null,
            )
        }
    }

    private fun buildCompanionContainer(
        treeUri: Uri,
    ): LinearLayout {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@ViewerActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp = 0,
                    )
            }

        companionHeader =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        2,
                    ),
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        2,
                    ),
                )
            }

        companionHeader.addView(
            headerAction(
                icon =
                    R.drawable.ic_arrow_back,
                description =
                    "Назад у документації",
            ) {
                companionBack()
            }
        )

        companionTitleView =
            Ui.textView(
                context = this,
                value =
                    "Документація тому",
                sizeSp = 14f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                maxLines = 1
                setPadding(
                    Ui.dp(
                        this@ViewerActivity,
                        6,
                    ),
                    0,
                    Ui.dp(
                        this@ViewerActivity,
                        6,
                    ),
                    0,
                )
            }

        companionHeader.addView(
            companionTitleView,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
        )

        companionHeader.addView(
            Button(this).apply {
                text = "✕"
                isAllCaps = false
                contentDescription =
                    "Закрити документацію поруч"
                minWidth = 0
                minimumWidth =
                    Ui.dp(
                        this@ViewerActivity,
                        44,
                    )
                minimumHeight =
                    Ui.dp(
                        this@ViewerActivity,
                        40,
                    )
                setOnClickListener {
                    hidePdfCompanion()
                }
            }
        )

        root.addView(companionHeader)

        companionClient =
            SafDatasetWebViewClient(
                context = this,
                treeUri = treeUri,
                legacyStandaloneMode =
                    true,
                onSavePdf = {
                    requestPdfSave(it)
                },
                onToggleFullscreen = {
                    togglePdfFullscreen()
                },
                pdfStateScope =
                    "companion",
                pdfCompanionControlEnabled =
                    false,
                pdfCompactMode =
                    true,
            )

        companionWebView =
            WebView(this).apply {
                setBackgroundColor(
                    Ui.background,
                )

                settings.javaScriptEnabled =
                    true
                settings.domStorageEnabled =
                    true
                settings.allowFileAccess =
                    false
                settings.allowContentAccess =
                    false
                settings.cacheMode =
                    WebSettings.LOAD_DEFAULT
                settings.loadsImagesAutomatically =
                    true
                settings.useWideViewPort =
                    true
                settings.loadWithOverviewMode =
                    true
                settings.builtInZoomControls =
                    true
                settings.displayZoomControls =
                    false
                settings.setSupportMultipleWindows(
                    false,
                )
                settings.javaScriptCanOpenWindowsAutomatically =
                    false

                webViewClient =
                    companionClient

                webChromeClient =
                    object :
                        WebChromeClient() {
                        override fun onReceivedTitle(
                            view: WebView?,
                            pageTitle: String?,
                        ) {
                            super.onReceivedTitle(
                                view,
                                pageTitle,
                            )

                            companionTitleView.text =
                                when {
                                    pageTitle.isNullOrBlank() ->
                                        "Документація тому"

                                    pageTitle.equals(
                                        "PDF",
                                        ignoreCase = true,
                                    ) ->
                                        "PDF · документація"

                                    else ->
                                        pageTitle
                                }

                            reconcilePdfFullscreenPresentation()
                        }
                    }
            }

        root.addView(
            companionWebView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )

        return root
    }

    private fun togglePdfCompanion() {
        if (
            modernVolumeEntrypoint
                .isBlank()
        ) {
            showTransientStatus(
                "Для цього PDF контекст тому відсутній.",
                isError = true,
            )
            return
        }

        if (pdfCompanionVisible) {
            hidePdfCompanion()
        } else {
            if (pdfFullscreen) {
                splitControlsVisible =
                    false
            }

            showPdfCompanion(
                loadIfNeeded =
                    !companionLoaded,
            )
        }
    }

    private fun showPdfCompanion(
        loadIfNeeded: Boolean,
    ) {
        if (
            !::companionContainer.isInitialized
        ) {
            return
        }

        pdfCompanionVisible = true
        companionContainer.visibility =
            View.VISIBLE
        splitDivider.visibility =
            View.VISIBLE
        applySplitRatio()

        if (loadIfNeeded) {
            loadCompanionDocumentation()
        }

        syncPdfCompanionControl()
        applySplitFocusMode()
    }

    private fun hidePdfCompanion() {
        if (
            !::companionContainer.isInitialized
        ) {
            return
        }

        pdfCompanionVisible = false
        companionContainer.visibility =
            View.GONE
        splitDivider.visibility =
            View.GONE
        splitControlsVisible =
            false
        reconcilePdfFullscreenPresentation()
    }

    private fun companionBack() {
        if (
            ::companionWebView.isInitialized &&
            companionWebView.canGoBack()
        ) {
            companionWebView.goBack()
            return
        }

        hidePdfCompanion()
    }

    private fun loadCompanionDocumentation() {
        if (
            modernVolumeEntrypoint
                .isBlank() ||
            treeUriText.isBlank()
        ) {
            showCompanionMessage(
                "Контекст тому для документації відсутній."
            )
            return
        }

        companionLoaded = true
        companionTitleView.text =
            "Документація · завантаження…"

        showCompanionMessage(
            "Відкриваю документацію тому…"
        )

        Thread {
            val result =
                RuntimeIrReader
                    .readVolumeDocumentationForVolume(
                        context = this,
                        treeUri =
                            Uri.parse(
                                treeUriText,
                            ),
                        volumeEntrypoint =
                            modernVolumeEntrypoint,
                    )

            runOnUiThread {
                result
                    .onSuccess {
                        companionTitleView.text =
                            "Документація тому"

                        companionWebView
                            .loadDataWithBaseURL(
                                "https://" +
                                    DatasetVirtualUrl.HOST +
                                    "/__renault_companion__/index.html",
                                VolumeDocumentationWebPage
                                    .html(it),
                                "text/html",
                                "UTF-8",
                                null,
                            )
                    }
                    .onFailure {
                        companionLoaded = false
                        companionTitleView.text =
                            "Документація тому"
                        showCompanionMessage(
                            it.message
                                ?: "Документацію тому не вдалося відкрити."
                        )
                    }
            }
        }.start()
    }

    private fun showCompanionMessage(
        message: String,
    ) {
        if (
            !::companionWebView.isInitialized
        ) {
            return
        }

        val escaped =
            message
                .replace(
                    "&",
                    "&amp;",
                )
                .replace(
                    "<",
                    "&lt;",
                )
                .replace(
                    ">",
                    "&gt;",
                )

        val html =
            """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <style>
                html,body {
                  margin:0;
                  min-height:100%;
                  background:#0e1116;
                  color:#f3f6f8;
                  font-family:system-ui,sans-serif;
                }
                body {
                  padding:14px;
                }
                .card {
                  padding:12px;
                  border:1px solid #344050;
                  border-radius:10px;
                  background:#171c24;
                }
              </style>
            </head>
            <body>
              <div class="card">$escaped</div>
            </body>
            </html>
            """.trimIndent()

        companionWebView
            .loadDataWithBaseURL(
                "https://" +
                    DatasetVirtualUrl.HOST +
                    "/__renault_companion__/message.html",
                html,
                "text/html",
                "UTF-8",
                null,
            )
    }

    private fun syncPdfCompanionControl() {
        if (!::webView.isInitialized) {
            return
        }

        webView.evaluateJavascript(
            "window.renaultSetCompanion && " +
                "window.renaultSetCompanion(" +
                pdfCompanionVisible.toString() +
                ");",
            null,
        )
    }

    private fun reconcilePdfFullscreenPresentation() {
        if (
            !::appToolbar.isInitialized ||
            !::searchRow.isInitialized ||
            !::statusView.isInitialized
        ) {
            return
        }

        applyPdfFullscreen(
            enabled =
                pdfFullscreen,
        )
        syncPdfCompanionControl()
    }

    private fun togglePdfFullscreen() {
        val next =
            !pdfFullscreen

        if (
            next &&
            pdfCompanionVisible
        ) {
            splitControlsVisible =
                false
        }

        pdfFullscreen =
            next

        applyPdfFullscreen(
            enabled = pdfFullscreen,
        )
        syncPdfFullscreenControl()
    }

    private fun syncPdfFullscreenControl() {
        val expected =
            pdfFullscreen
        val generation =
            ++fullscreenControlSyncGeneration

        if (::webView.isInitialized) {
            syncPdfFullscreenControl(
                target = webView,
                expected = expected,
                generation = generation,
                attempt = 0,
            )
        }

        if (::companionWebView.isInitialized) {
            syncPdfFullscreenControl(
                target =
                    companionWebView,
                expected = expected,
                generation = generation,
                attempt = 0,
            )
        }
    }

    private fun syncPdfFullscreenControl(
        target: WebView,
        expected: Boolean,
        generation: Int,
        attempt: Int,
    ) {
        if (
            generation !=
                fullscreenControlSyncGeneration ||
            expected !=
                pdfFullscreen
        ) {
            return
        }

        val script =
            "(function(){" +
                "if(typeof window.renaultSetFullscreen!=='function')" +
                "{return false;}" +
                "return window.renaultSetFullscreen(" +
                expected.toString() +
                ")===true;" +
                "})()"

        target.evaluateJavascript(
            script,
        ) { result ->
            if (
                generation !=
                    fullscreenControlSyncGeneration ||
                expected !=
                    pdfFullscreen
            ) {
                return@evaluateJavascript
            }

            if (result == "true") {
                return@evaluateJavascript
            }

            if (
                attempt >=
                FULLSCREEN_CONTROL_SYNC_MAX_ATTEMPTS
            ) {
                return@evaluateJavascript
            }

            val delay =
                FULLSCREEN_CONTROL_SYNC_BASE_DELAY_MS +
                    (
                        attempt *
                            FULLSCREEN_CONTROL_SYNC_STEP_DELAY_MS
                    )

            target.postDelayed(
                {
                    syncPdfFullscreenControl(
                        target = target,
                        expected = expected,
                        generation =
                            generation,
                        attempt =
                            attempt + 1,
                    )
                },
                delay,
            )
        }
    }

    private fun applyPdfFullscreen(
        enabled: Boolean,
    ) {
        if (
            !::appToolbar.isInitialized ||
            !::searchRow.isInitialized ||
            !::statusView.isInitialized
        ) {
            return
        }

        if (enabled) {
            searchVisibleBeforeFullscreen =
                searchRow.visibility ==
                    View.VISIBLE
            appToolbar.visibility =
                View.GONE
            searchRow.visibility =
                View.GONE
            statusView.visibility =
                View.GONE
        } else {
            appToolbar.visibility =
                View.VISIBLE

            if (
                searchVisibleBeforeFullscreen
            ) {
                searchRow.visibility =
                    View.VISIBLE
            }

            searchVisibleBeforeFullscreen =
                false
        }

        if (
            enabled
        ) {
            setSystemBarsHidden(
                hidden = true,
            )
        } else {
            Ui.applyOrientationSystemBars(
                this,
            )
        }
        syncPdfFullscreenControl()
        applySplitFocusMode()

        if (
            enabled &&
            pdfCompanionVisible &&
            splitControlsVisible
        ) {
            splitControlsHandler
                .removeCallbacks(
                    hideSplitControlsRunnable,
                )
            splitControlsHandler
                .postDelayed(
                    hideSplitControlsRunnable,
                    SPLIT_CONTROLS_TIMEOUT_MS,
                )
        }
    }

    @Suppress("DEPRECATION")
    private fun setSystemBarsHidden(
        hidden: Boolean,
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            val controller =
                window.insetsController

            if (hidden) {
                controller?.hide(
                    WindowInsets.Type
                        .statusBars() or
                        WindowInsets.Type
                            .navigationBars(),
                )
                controller?.systemBarsBehavior =
                    WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(
                    WindowInsets.Type
                        .statusBars() or
                        WindowInsets.Type
                            .navigationBars(),
                )
            }
        } else {
            window.decorView
                .systemUiVisibility =
                if (hidden) {
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                } else {
                    View.SYSTEM_UI_FLAG_VISIBLE
                }
        }
    }

    private fun navigateBack() {
        if (pdfCompanionVisible) {
            if (
                ::companionWebView.isInitialized &&
                companionWebView.canGoBack()
            ) {
                companionWebView.goBack()
            } else {
                hidePdfCompanion()
            }
            return
        }

        if (
            ::webView.isInitialized &&
            webView.canGoBack()
        ) {
            webView.goBack()
            return
        }

        finish()
    }

    private fun showFatalError(
        title: String,
        message: String,
    ) {
        window.statusBarColor = Ui.background
        window.navigationBarColor = Ui.background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.background)
        }
        Ui.applySystemInsets(root)

        root.addView(
            Ui.textView(
                context = this,
                value = title,
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
                    Ui.dp(this@ViewerActivity, 16),
                    0,
                    0,
                )
            }
        )

        setContentView(root)
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ENTRYPOINT = "entrypoint"
        private const val EXTRA_TREE_URI = "treeUri"
        private const val EXTRA_MODERN_DATASET_TITLE =
            "modernDatasetTitle"
        private const val EXTRA_MODERN_CLASSIC_ENTRYPOINT =
            "modernClassicEntrypoint"
        private const val EXTRA_MODERN_FOCUS_ENTRYPOINT =
            "modernFocusEntrypoint"
        private const val EXTRA_MODERN_VOLUME_TITLE =
            "modernVolumeTitle"
        private const val EXTRA_MODERN_VOLUME_ENTRYPOINT =
            "modernVolumeEntrypoint"
        private const val EXTRA_MODERN_SECTION_CODE =
            "modernSectionCode"
        private const val EXTRA_MODERN_SECTION_LEGACY_ENTRYPOINT =
            "modernSectionLegacyEntrypoint"

        private const val REQUEST_SAVE_PDF =
            4301
        private const val STATE_PENDING_PDF_PATH =
            "pendingPdfPath"
        private const val STATE_SEARCH_QUERY =
            "searchQuery"
        private const val STATE_SEARCH_VISIBLE =
            "searchVisible"
        private const val STATE_PDF_FULLSCREEN =
            "pdfFullscreen"
        private const val STATE_PDF_COMPANION_VISIBLE =
            "pdfCompanionVisible"
        private const val STATE_PDF_COMPANION_WEBVIEW =
            "pdfCompanionWebView"
        private const val STATE_PDF_SPLIT_RATIO =
            "pdfSplitRatio"
        private const val STATE_PDF_SPLIT_CONTROLS_VISIBLE =
            "pdfSplitControlsVisible"
        private const val STATE_MODERN_SECTION_CODE =
            "modernSectionCode"
        private const val STATE_MODERN_SECTION_LEGACY_ENTRYPOINT =
            "modernSectionLegacyEntrypoint"
        private const val STATE_DIALOG_KIND =
            "viewerDialogKind"
        private const val STATE_SECTION_NAV_QUERY =
            "sectionNavigatorQuery"

        private const val DIALOG_SECTION_NAVIGATOR =
            "sectionNavigator"
        private const val DIALOG_FRAME_DEBUG =
            "frameDebug"

        private const val SPLIT_CONTROLS_TIMEOUT_MS =
            3200L

        private const val FULLSCREEN_CONTROL_SYNC_MAX_ATTEMPTS =
            8
        private const val FULLSCREEN_CONTROL_SYNC_BASE_DELAY_MS =
            80L
        private const val FULLSCREEN_CONTROL_SYNC_STEP_DELAY_MS =
            120L

        fun intent(
            context: Context,
            record: DatasetRecord,
        ): Intent =
            intentForEntrypoint(
                context = context,
                title = record.title,
                entrypoint =
                    record.openEntrypoint,
                treeUri =
                    record.treeUri,
                modernDatasetTitle =
                    record.title,
                modernClassicEntrypoint =
                    record.openEntrypoint,
            )

        fun intentForEntrypoint(
            context: Context,
            title: String,
            entrypoint: String,
            treeUri: String,
            modernDatasetTitle: String? = null,
            modernClassicEntrypoint: String? = null,
            modernFocusEntrypoint: String? = null,
            modernVolumeTitle: String? = null,
            modernVolumeEntrypoint: String? = null,
            modernSectionCode: String? = null,
            modernSectionLegacyEntrypoint: String? = null,
        ): Intent = Intent(
            context,
            ViewerActivity::class.java,
        ).apply {
            putExtra(
                EXTRA_TITLE,
                title,
            )
            putExtra(
                EXTRA_ENTRYPOINT,
                entrypoint,
            )
            putExtra(
                EXTRA_TREE_URI,
                treeUri,
            )

            modernDatasetTitle
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_DATASET_TITLE,
                        it,
                    )
                }

            modernClassicEntrypoint
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_CLASSIC_ENTRYPOINT,
                        it,
                    )
                }

            modernFocusEntrypoint
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_FOCUS_ENTRYPOINT,
                        it,
                    )
                }

            modernVolumeTitle
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_VOLUME_TITLE,
                        it,
                    )
                }

            modernVolumeEntrypoint
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_VOLUME_ENTRYPOINT,
                        it,
                    )
                }

            modernSectionCode
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_SECTION_CODE,
                        it,
                    )
                }

            modernSectionLegacyEntrypoint
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    putExtra(
                        EXTRA_MODERN_SECTION_LEGACY_ENTRYPOINT,
                        it,
                    )
                }
        }
    }
}
