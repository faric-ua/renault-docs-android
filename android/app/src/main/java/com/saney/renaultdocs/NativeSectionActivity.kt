package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class NativeSectionActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var menuContainer: LinearLayout
    private lateinit var bodyContainer: LinearLayout
    private lateinit var bodyScroll: ScrollView
    private lateinit var globalBar: LinearLayout
    private lateinit var contextRow: LinearLayout
    private lateinit var helpDialogs:
        LifecycleHelpDialogController

    private var currentMenuLabel: String = ""
    private var currentMenuActionId: String = ""

    private var runtimeData:
        RuntimeIrSectionData? = null
    private var runtimeDocumentation:
        JSONObject? = null
    private var documentationLoading:
        Boolean = false

    private var treeUriText: String = ""
    private var datasetTitle: String =
        "Renault dataset"
    private var classicEntrypoint: String = ""
    private var volumeTitle: String =
        "Renault volume"
    private var volumeEntrypoint: String = ""
    private var sectionCode: String = ""
    private var sectionTitle: String = ""
    private var sectionLegacyEntrypoint: String =
        ""

    private var pendingTablePdf:
        NativeTablePdfData? = null

    private var currentViewKind: String =
        VIEW_DEFAULT
    private var currentViewId: String = ""
    private var restoredViewKind: String =
        VIEW_DEFAULT
    private var restoredViewId: String = ""
    private var currentViewLabel: String = ""
    private var restoredViewLabel: String = ""
    private var currentDocumentParentPanelId: String = ""
    private var restoredDocumentParentPanelId: String = ""
    private var restoredScrollY: Int = 0

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        treeUriText =
            intent.getStringExtra(
                EXTRA_TREE_URI,
            ).orEmpty()
        datasetTitle =
            intent.getStringExtra(
                EXTRA_DATASET_TITLE,
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: datasetTitle
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
        sectionCode =
            intent.getStringExtra(
                EXTRA_SECTION_CODE,
            ).orEmpty()
        sectionTitle =
            intent.getStringExtra(
                EXTRA_SECTION_TITLE,
            ).orEmpty()
        sectionLegacyEntrypoint =
            intent.getStringExtra(
                EXTRA_SECTION_LEGACY_ENTRYPOINT,
            ).orEmpty()

        currentMenuLabel =
            savedInstanceState
                ?.getString(
                    STATE_MENU_LABEL,
                )
                .orEmpty()
        currentMenuActionId =
            savedInstanceState
                ?.getString(
                    STATE_MENU_ACTION_ID,
                )
                .orEmpty()

        restoredViewKind =
            savedInstanceState
                ?.getString(
                    STATE_VIEW_KIND,
                )
                .orEmpty()
                .ifBlank {
                    VIEW_DEFAULT
                }
        restoredViewId =
            savedInstanceState
                ?.getString(
                    STATE_VIEW_ID,
                )
                .orEmpty()
        currentViewKind =
            restoredViewKind
        currentViewId =
            restoredViewId

        restoredViewLabel =
            savedInstanceState
                ?.getString(
                    STATE_VIEW_LABEL,
                )
                .orEmpty()
        currentViewLabel =
            restoredViewLabel

        restoredDocumentParentPanelId =
            savedInstanceState
                ?.getString(
                    STATE_DOCUMENT_PARENT_PANEL_ID,
                )
                .orEmpty()
        currentDocumentParentPanelId =
            restoredDocumentParentPanelId

        restoredScrollY =
            savedInstanceState
                ?.getInt(
                    STATE_SCROLL_Y,
                    0,
                )
                ?: 0

        (
            lastNonConfigurationInstance
                as? RetainedRuntimeState
        )?.let {
            retained ->
            runtimeData =
                retained.runtimeData
            runtimeDocumentation =
                retained.runtimeDocumentation
        }

        pendingTablePdf =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_TABLE_PDF,
                )
                ?.let {
                    decodeTablePdfData(
                        it,
                    )
                }

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
            volumeEntrypoint.isBlank() ||
            sectionCode.isBlank()
        ) {
            showFatalError(
                "Native section не отримав потрібний context."
            )
            return
        }

        setContentView(
            buildContent(),
        )
        helpDialogs.restoreOpen()

        val retainedData =
            runtimeData
        if (
            retainedData != null
        ) {
            showRuntimeData(
                retainedData,
            )
        } else {
            loadRuntimeIr()
        }
    }

    override fun onRetainNonConfigurationInstance(): Any =
        RetainedRuntimeState(
            runtimeData =
                runtimeData,
            runtimeDocumentation =
                runtimeDocumentation,
        )

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putString(
            STATE_VIEW_KIND,
            currentViewKind,
        )
        outState.putString(
            STATE_VIEW_ID,
            currentViewId,
        )
        outState.putString(
            STATE_VIEW_LABEL,
            currentViewLabel,
        )
        outState.putString(
            STATE_DOCUMENT_PARENT_PANEL_ID,
            currentDocumentParentPanelId,
        )
        outState.putInt(
            STATE_SCROLL_Y,
            if (
                ::bodyScroll.isInitialized
            ) {
                bodyScroll.scrollY
            } else {
                restoredScrollY
            },
        )
        outState.putString(
            STATE_MENU_LABEL,
            currentMenuLabel,
        )
        outState.putString(
            STATE_MENU_ACTION_ID,
            currentMenuActionId,
        )
        helpDialogs.save(
            outState,
        )

        pendingTablePdf
            ?.let {
                outState.putString(
                    STATE_PENDING_TABLE_PDF,
                    encodeTablePdfData(
                        it,
                    ),
                )
            }

        super.onSaveInstanceState(
            outState,
        )
    }

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
            REQUEST_SAVE_TABLE_PDF
        ) {
            return
        }

        if (
            resultCode !=
            RESULT_OK
        ) {
            pendingTablePdf = null
            return
        }

        val target =
            data?.data
                ?: run {
                    pendingTablePdf = null
                    return
                }

        val export =
            pendingTablePdf
                ?: return

        pendingTablePdf = null

        Thread {
            val result =
                runCatching {
                    NativeTablePdfExporter
                        .write(
                            context = this,
                            uri = target,
                            data = export,
                        )
                }

            runOnUiThread {
                result
                    .onSuccess {
                        Toast
                            .makeText(
                                this,
                                "PDF таблиці збережено.",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                    .onFailure {
                        Toast
                            .makeText(
                                this,
                                it.message
                                    ?: "Не вдалося зберегти PDF.",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    }
            }
        }.start()
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

        Ui.applySystemInsets(
            root,
            horizontalDp = 12,
            topDp = 10,
            bottomDp = 12,
        )

        globalBar =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        globalBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_arrow_back,
                description =
                    "Назад до розділів",
            ) {
                finish()
            }
        )

        globalBar.addView(
            View(this),
            LinearLayout.LayoutParams(
                0,
                Ui.dp(
                    this,
                    44,
                ),
                1f,
            )
        )

        globalBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_home,
                description =
                    "Головне меню",
            ) {
                openHome()
            }
        )

        globalBar.addView(
            headerAction(
                icon =
                    R.drawable.ic_search,
                description =
                    "Пошук розділів",
            ) {
                openSectionSearch()
            }
        )

        globalBar.addView(
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_SECTION,
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

        globalBar.addView(
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

        root.addView(
            globalBar,
        )

        contextRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        6,
                    ),
                )
            }

        contextRow.addView(
            Ui.textView(
                context = this,
                value = sectionCode,
                sizeSp = 24f,
                color = Ui.accent,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
                gravity =
                    Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
                1f,
            )
        )

        val modeSwitch =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@NativeSectionActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp = 12,
                    )
                setPadding(
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
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
                openLegacyFallback()
            },
        )

        contextRow.addView(
            modeSwitch,
        )

        root.addView(
            contextRow,
        )

        statusText =
            Ui.textView(
                context = this,
                value =
                    "Читаю Runtime IR v2…",
                sizeSp = 12f,
                color = Ui.muted,
            ).apply {
                setPadding(
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                    0,
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                )
            }

        root.addView(statusText)

        menuContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        root.addView(
            menuContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@NativeSectionActivity,
                        6,
                    )
            }
        )

        applyLandscapeFocusChrome()

        bodyScroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        bodyContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    0,
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        24,
                    ),
                )
            }

        bodyScroll.addView(
            bodyContainer,
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

        root.addView(
            bodyScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                0,
                1f,
            )
        )

        return root
    }

    private fun isLandscapeOrientation(): Boolean =
        resources.configuration.orientation ==
            android.content.res.Configuration
                .ORIENTATION_LANDSCAPE

    private fun isLandscapeFocusMode(): Boolean =
        isLandscapeOrientation() &&
            currentMenuLabel.isNotBlank()

    private fun applyLandscapeFocusChrome() {
        if (
            !::globalBar.isInitialized ||
            !::contextRow.isInitialized ||
            !::statusText.isInitialized
        ) {
            return
        }

        val compact =
            isLandscapeFocusMode()

        globalBar.visibility =
            if (compact) {
                View.GONE
            } else {
                View.VISIBLE
            }
        contextRow.visibility =
            if (compact) {
                View.GONE
            } else {
                View.VISIBLE
            }

        if (compact) {
            statusText.visibility =
                View.GONE
        }
    }

    private fun selectTopMenu(
        label: String,
        actionId: String = "",
    ) {
        currentMenuLabel =
            label
        currentMenuActionId =
            actionId

        if (isLandscapeOrientation()) {
            applyLandscapeFocusChrome()
            if (
                ::menuContainer.isInitialized &&
                runtimeData != null
            ) {
                renderMenu()
            }
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
                android.graphics.Color
                    .TRANSPARENT,
            )
            minimumWidth =
                Ui.dp(
                    this@NativeSectionActivity,
                    44,
                )
            minimumHeight =
                Ui.dp(
                    this@NativeSectionActivity,
                    44,
                )
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
            )
            setOnClickListener {
                onClick()
            }
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

    private fun openSectionSearch() {
        startActivity(
            ModernVolumeActivity
                .intentForVolume(
                    context = this,
                    datasetTitle =
                        datasetTitle,
                    treeUri =
                        treeUriText,
                    classicEntrypoint =
                        classicEntrypoint,
                    volumeTitle =
                        volumeTitle,
                    volumeEntrypoint =
                        volumeEntrypoint,
                    openSearch = true,
                )
                .apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    )
                }
        )
    }

    private fun helpSpec(
        helpId: String,
    ): HelpDialogSpec? =
        when (
            helpId
        ) {
            HELP_SECTION ->
                HelpDialogSpec(
                    title =
                        "Modern розділ",
                    message =
                        "Modern показує структуровані дані Runtime IR: схеми, роз’єми, положення на авто, документацію та інші дії, які реально є в цьому розділі.\n\n" +
                            "Classic відкриває оригінальну Renault HTML-сторінку цього самого розділу.\n\n" +
                            "Коди Renault залишаються opaque: суфікси на кшталт _1/_2 мають значення лише в контексті документації й не переіменовуються програмою.",
                )

            else ->
                null
        }

    private fun loadRuntimeIr() {
        Thread {
            val result =
                RuntimeIrReader
                    .readSection(
                        context = this,
                        treeUri =
                            Uri.parse(
                                treeUriText,
                            ),
                        volumeEntrypoint =
                            volumeEntrypoint,
                        sectionCode =
                            sectionCode,
                        sectionEntrypoint =
                            sectionLegacyEntrypoint,
                    )

            runOnUiThread {
                result
                    .onSuccess {
                        runtimeData = it
                        showRuntimeData(
                            it,
                        )
                    }
                    .onFailure {
                        statusText.setTextColor(
                            Ui.danger,
                        )
                        statusText.text =
                            it.message
                                ?: "Runtime IR v2 недоступний."
                        bodyContainer
                            .removeAllViews()
                        bodyContainer.addView(
                            Button(this).apply {
                                text =
                                    "Відкрити через Classic runtime"
                                isAllCaps = false
                                setOnClickListener {
                                    openLegacyFallback()
                                }
                            }
                        )
                    }
            }
        }.start()
    }

    private fun showRuntimeData(
        data: RuntimeIrSectionData,
    ) {
        statusText.setTextColor(
            Ui.muted,
        )
        statusText.text =
            "Runtime IR v" +
                data.schemaVersion +
                " · native preview"
        statusText.visibility =
            View.GONE

        renderMenu()
        restoreViewAfterRotation()
        restoreBodyScroll()
    }

    private fun restoreBodyScroll() {
        if (
            restoredScrollY <=
            0 ||
            !::bodyScroll.isInitialized
        ) {
            return
        }

        val scrollY =
            restoredScrollY
        restoredScrollY =
            0

        bodyScroll.post {
            bodyScroll.scrollTo(
                0,
                scrollY,
            )
        }
    }

    private fun restoreViewAfterRotation() {
        currentViewKind =
            restoredViewKind
        currentViewId =
            restoredViewId

        when (restoredViewKind) {
            VIEW_PANEL -> {
                if (
                    restoredViewId.isNotBlank() &&
                    findPanel(
                        restoredViewId,
                    ) != null
                ) {
                    renderPanel(
                        restoredViewId,
                    )
                    return
                }
            }

            VIEW_DOCUMENT -> {
                if (
                    restoredViewId.isNotBlank() &&
                    findDocument(
                        restoredViewId,
                    ) != null
                ) {
                    renderOrOpenDocument(
                        documentId =
                            restoredViewId,
                        label =
                            restoredViewLabel
                                .ifBlank {
                                    "Документ"
                                },
                        parentPanelId =
                            restoredDocumentParentPanelId,
                    )
                    return
                }
            }

            VIEW_DOCUMENTATION -> {
                openDocumentation(
                    fallbackItems =
                        documentationMenuItems(),
                )
                return
            }
        }

        val menu =
            findControl(
                "menu-toolbar",
            )
        val firstPanelAction =
            menu
                ?.optJSONArray(
                    "items",
                )
                ?.let {
                    array ->
                    firstOpenPanelAction(
                        array,
                    )
                }

        if (
            firstPanelAction != null
        ) {
            val actionId =
                firstPanelAction.optString(
                    "id",
                )
            val toolbarItems =
                menu
                    ?.optJSONArray(
                        "items",
                    )
            if (toolbarItems != null) {
                for (
                    index in
                    0 until toolbarItems.length()
                ) {
                    val item =
                        toolbarItems
                            .getJSONObject(
                                index,
                            )
                    if (
                        item.optString(
                            "action_id",
                        ) == actionId
                    ) {
                        val rawLabel =
                            item.optString(
                                "label",
                                "Схеми",
                            )
                        val label =
                            if (
                                rawLabel.equals(
                                    "SCH",
                                    ignoreCase = true,
                                )
                            ) {
                                "Схеми"
                            } else {
                                NativeRuntimePresentation
                                    .menuLabel(
                                        rawLabel,
                                    )
                            }
                        selectTopMenu(
                            label = label,
                            actionId =
                                item.optString(
                                    "action_id",
                                ),
                        )
                        break
                    }
                }
            }

            handleAction(
                firstPanelAction,
            )
        } else {
            currentViewKind =
                VIEW_DEFAULT
            currentViewId = ""
            bodyContainer
                .removeAllViews()
            bodyContainer.addView(
                infoCard(
                    "Оберіть пункт меню."
                )
            )
        }
    }

    private fun documentationMenuItems():
        List<JSONObject> {
        runtimeDocumentation
            ?.optJSONArray(
                "menu_items",
            )
            ?.let {
                items ->
                return (
                    0 until items.length()
                )
                    .map {
                        items.getJSONObject(
                            it,
                        )
                    }
            }

        val toolbar =
            findControlIn(
                root =
                    runtimeData
                        ?.section,
                key =
                    "controls",
                id =
                    "menu-toolbar",
            )
        val items =
            toolbar?.optJSONArray(
                "items",
            )
                ?: JSONArray()
        val result =
            mutableListOf<JSONObject>()

        for (
            index in
            0 until items.length()
        ) {
            val item =
                items.getJSONObject(
                    index,
                )
            val label =
                item.optString(
                    "label",
                    "Action",
                )

            if (
                NativeRuntimePresentation
                    .isDocumentationMenu(
                        label,
                    )
            ) {
                result.add(
                    item,
                )
            }
        }

        return result
    }

    private fun renderMenu() {
        menuContainer.removeAllViews()

        val toolbar =
            findControl(
                "menu-toolbar",
            )

        val items =
            toolbar?.optJSONArray(
                "items",
            )
                ?: JSONArray()

        val visibleItems =
            mutableListOf<JSONObject>()
        val documentationItems =
            mutableListOf<JSONObject>()

        for (
            index in
            0 until items.length()
        ) {
            val item =
                items.getJSONObject(index)
            val rawLabel =
                item.optString(
                    "label",
                    "Action",
                )

            if (
                NativeRuntimePresentation
                    .isHiddenPlaceholder(
                        rawLabel,
                    )
            ) {
                continue
            }

            if (
                NativeRuntimePresentation
                    .isDocumentationMenu(
                        rawLabel,
                    )
            ) {
                documentationItems.add(item)
            } else {
                visibleItems.add(item)
            }
        }

        val scheme =
            menuItemByCode(
                visibleItems,
                "SCH",
            )
        val connector =
            menuItemByCode(
                visibleItems,
                "NM",
            )
        val position =
            menuItemByCode(
                visibleItems,
                "PC",
            )

        if (
            isLandscapeFocusMode()
        ) {
            val activeButton =
                if (
                    currentMenuLabel ==
                    "Документація"
                ) {
                    buildDocumentationTile(
                        documentationItems,
                    )
                } else {
                    val activeItem =
                        (
                            0 until items.length()
                        )
                            .map {
                                items.getJSONObject(
                                    it,
                                )
                            }
                            .firstOrNull {
                                it.optString(
                                    "action_id",
                                ) ==
                                    currentMenuActionId
                            }

                    buildFixedMenuTile(
                        label =
                            currentMenuLabel,
                        item =
                            activeItem,
                    )
                }

            addFullWidthMenuButton(
                activeButton,
            )
            return
        }

        val used =
            mutableSetOf<JSONObject>()

        scheme?.let(used::add)
        connector?.let(used::add)
        position?.let(used::add)

        val firstRow =
            newMenuRow()

        firstRow.addView(
            buildFixedMenuTile(
                label = "Схеми",
                item = scheme,
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
                1f,
            ).apply {
                marginEnd =
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    )
            }
        )

        firstRow.addView(
            buildFixedMenuTile(
                label = "Розʼєм",
                item = connector,
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
                1f,
            )
        )

        menuContainer.addView(
            firstRow,
            menuRowParams(),
        )

        addFullWidthMenuButton(
            buildFixedMenuTile(
                label =
                    "Положення на авто",
                item = position,
            )
        )

        addFullWidthMenuButton(
            buildDocumentationTile(
                documentationItems,
            )
        )

        for (item in visibleItems) {
            if (item in used) {
                continue
            }

            val rawLabel =
                item.optString(
                    "label",
                    "Action",
                )
            addFullWidthMenuButton(
                buildFixedMenuTile(
                    label =
                        NativeRuntimePresentation
                            .menuLabel(
                                rawLabel,
                            ),
                    item = item,
                )
            )
        }
    }

    private fun menuItemByCode(
        items: List<JSONObject>,
        code: String,
    ): JSONObject? =
        items.firstOrNull {
            it.optString(
                "label",
            )
                .trim()
                .equals(
                    code,
                    ignoreCase = true,
                )
        }

    private fun newMenuRow():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
        }

    private fun menuRowParams():
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams
                .MATCH_PARENT,
            LinearLayout.LayoutParams
                .WRAP_CONTENT,
        ).apply {
            bottomMargin =
                Ui.dp(
                    this@NativeSectionActivity,
                    4,
                )
        }

    private fun addFullWidthMenuButton(
        button: Button,
    ) {
        val row = newMenuRow()
        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
                1f,
            )
        )
        menuContainer.addView(
            row,
            menuRowParams(),
        )
    }

    private fun buildFixedMenuTile(
        label: String,
        item: JSONObject?,
    ): Button {
        val actionId =
            item
                ?.optString(
                    "action_id",
                )
                .orEmpty()
        val action =
            findAction(actionId)

        val enabled =
            item != null &&
                item.optBoolean(
                    "enabled",
                    actionId.isNotBlank(),
                ) &&
                action != null &&
                isActionResolvable(
                    action,
                )

        return menuTileButton(
            label = label,
            enabled = enabled,
        ) {
            if (
                action != null &&
                enabled
            ) {
                selectTopMenu(
                    label = label,
                    actionId =
                        actionId,
                )
                handleAction(action)
            }
        }
    }

    private fun buildDocumentationTile(
        items: List<JSONObject>,
    ): Button {
        val hasVolumeDocumentation =
            runtimeData
                ?.volumeDocumentationPath
                ?.isNotBlank() == true

        val enabled =
            hasVolumeDocumentation ||
                items.any { item ->
                    val action =
                        findAction(
                            item.optString(
                                "action_id",
                            )
                        )

                    action != null &&
                        isActionResolvable(
                            action,
                        )
                }

        return menuTileButton(
            label = "Документація",
            enabled = enabled,
        ) {
            if (enabled) {
                if (hasVolumeDocumentation) {
                    openVolumeDocumentation()
                } else {
                    selectTopMenu(
                        label =
                            "Документація",
                    )
                    openDocumentation(
                        fallbackItems =
                            items,
                    )
                }
            }
        }
    }

    private fun menuTileButton(
        label: String,
        enabled: Boolean,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            isEnabled = enabled
            minWidth = 0
            minimumWidth = 0
            minHeight =
                Ui.dp(
                    this@NativeSectionActivity,
                    44,
                )
            minimumHeight =
                Ui.dp(
                    this@NativeSectionActivity,
                    44,
                )
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                0,
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                0,
            )
            background =
                Ui.roundedBackground(
                    context =
                        this@NativeSectionActivity,
                    fill =
                        if (enabled) {
                            Ui.surfaceAlt
                        } else {
                            Ui.surface
                        },
                    stroke =
                        if (enabled) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                    radiusDp = 12,
                )
            setTextColor(
                if (enabled) {
                    Ui.text
                } else {
                    Ui.muted
                }
            )
            alpha =
                if (enabled) {
                    1f
                } else {
                    0.38f
                }
            contentDescription =
                if (enabled) {
                    label + " — відкрити"
                } else {
                    label + " — недоступно"
                }
            setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
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

    private fun openDocumentation(
        fallbackItems: List<JSONObject>,
    ) {
        val data =
            runtimeData
                ?: return

        val path =
            data
                .volumeDocumentationPath
                .orEmpty()

        if (
            path.isBlank() ||
            runtimeDocumentation != null
        ) {
            val items =
                documentationMenuItems()
                    .ifEmpty {
                        fallbackItems
                    }

            renderDocumentationMenu(
                items,
            )
            return
        }

        if (documentationLoading) {
            return
        }

        documentationLoading = true
        currentViewKind =
            VIEW_DOCUMENTATION
        currentViewId = ""
        bodyContainer.removeAllViews()
        bodyContainer.addView(
            infoCard(
                "Відкриваю документацію тому…"
            )
        )

        Thread {
            val result =
                RuntimeIrReader
                    .readVolumeDocumentation(
                        context = this,
                        treeUri =
                            Uri.parse(
                                treeUriText,
                            ),
                        path = path,
                        expectedSchema =
                            data.schemaVersion,
                        expectedCompilerPhase =
                            data.compilerPhase,
                    )

            runOnUiThread {
                documentationLoading =
                    false

                result
                    .onSuccess {
                        runtimeDocumentation =
                            it
                        renderMenu()
                        renderDocumentationMenu(
                            documentationMenuItems(),
                        )
                    }
                    .onFailure {
                        val fallback =
                            fallbackItems
                                .ifEmpty {
                                    documentationMenuItems()
                                }

                        if (
                            fallback.isNotEmpty()
                        ) {
                            renderDocumentationMenu(
                                fallback,
                            )
                        } else {
                            bodyContainer
                                .removeAllViews()
                            bodyContainer.addView(
                                infoCard(
                                    it.message
                                        ?: "Документацію тому не вдалося відкрити."
                                )
                            )
                        }
                    }
            }
        }.start()
    }

    private fun renderDocumentationMenu(
        items: List<JSONObject>,
    ) {
        currentViewKind =
            VIEW_DOCUMENTATION
        currentViewId = ""
        bodyContainer.removeAllViews()

        bodyContainer.addView(
            Ui.textView(
                context = this,
                value = "Документація",
                sizeSp = 19f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
                setPadding(
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        10,
                    ),
                )
            }
        )

        for (item in items) {
            val rawLabel =
                item.optString(
                    "label",
                    "Документ",
                )
            val action =
                findAction(
                    item.optString(
                        "action_id",
                    )
                )

            if (
                action != null &&
                isActionResolvable(
                    action,
                )
            ) {
                addActionButton(
                    action = action,
                    labelOverride =
                        NativeRuntimePresentation
                            .menuLabel(
                                rawLabel,
                            ),
                )
            } else {
                addUnavailableItem(
                    NativeRuntimePresentation
                        .menuLabel(
                            rawLabel,
                        )
                )
            }
        }
    }

    private fun renderPanel(
        panelId: String,
    ) {
        currentViewKind =
            VIEW_PANEL
        currentViewId =
            panelId
        currentViewLabel =
            ""
        currentDocumentParentPanelId =
            ""

        val panel =
            findPanel(panelId)
                ?: return

        bodyContainer.removeAllViews()

        bodyContainer.addView(
            Ui.textView(
                context = this,
                value =
                    sectionCode +
                        " — " +
                        sectionTitle,
                sizeSp = 19f,
                color = Ui.entityTitle,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
                setPadding(
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        4,
                    ),
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        8,
                    ),
                )
            }
        )

        val referencedActions =
            mutableSetOf<String>()

        val controlIds =
            panel.optJSONArray(
                "control_ids",
            )
                ?: JSONArray()

        for (
            index in
            0 until controlIds.length()
        ) {
            val control =
                findControl(
                    controlIds.getString(
                        index,
                    )
                )
                    ?: continue

            collectControlActionIds(
                control,
                referencedActions,
            )
            renderControl(
                control,
            )
        }

        val panelActions =
            panel.optJSONArray(
                "action_ids",
            )
                ?: JSONArray()

        for (
            index in
            0 until panelActions.length()
        ) {
            val actionId =
                panelActions.getString(
                    index,
                )
            if (
                actionId in
                referencedActions
            ) {
                continue
            }

            val action =
                findAction(actionId)
                    ?: continue

            addActionButton(
                action,
            )
        }
    }

    private fun renderControl(
        control: JSONObject,
    ) {
        when (
            control.optString(
                "type",
            )
        ) {
            "select" ->
                renderSelect(control)
            "document-list" ->
                renderDocumentList(
                    control,
                )
            "action-bar" -> Unit
            else ->
                bodyContainer.addView(
                    infoCard(
                        "Невідомий control: " +
                            control.optString(
                                "type",
                                "?",
                            )
                    )
                )
        }
    }

    private fun renderSelect(
        control: JSONObject,
    ) {
        val options =
            control.optJSONArray(
                "options",
            )
                ?: JSONArray()

        var currentTile:
            LinearLayout? = null

        fun commitTile() {
            val tile =
                currentTile
                    ?: return

            if (tile.childCount > 0) {
                addContentTile(tile)
            }

            currentTile = null
        }

        fun ensureTile():
            LinearLayout {
            val existing =
                currentTile
            if (existing != null) {
                return existing
            }

            val created =
                contentTile()
            currentTile = created
            return created
        }

        for (
            index in
            0 until options.length()
        ) {
            val option =
                options.getJSONObject(
                    index,
                )
            val label =
                option.optString(
                    "label",
                ).trim()
            val enabled =
                option.optBoolean(
                    "enabled",
                    false,
                )
            val kind =
                option.optString(
                    "kind",
                )

            if (!enabled) {
                if (kind == "separator") {
                    commitTile()
                    continue
                }

                if (label.isBlank()) {
                    continue
                }

                if (
                    isSelectPrompt(
                        label,
                    )
                ) {
                    commitTile()
                    bodyContainer.addView(
                        Ui.textView(
                            context = this,
                            value = label,
                            sizeSp = 13f,
                            color = Ui.muted,
                        ).apply {
                            setPadding(
                                Ui.dp(
                                    this@NativeSectionActivity,
                                    4,
                                ),
                                Ui.dp(
                                    this@NativeSectionActivity,
                                    5,
                                ),
                                Ui.dp(
                                    this@NativeSectionActivity,
                                    4,
                                ),
                                Ui.dp(
                                    this@NativeSectionActivity,
                                    6,
                                ),
                            )
                        }
                    )
                    continue
                }

                commitTile()
                currentTile =
                    contentTile().apply {
                        addView(
                            tileHeader(
                                label,
                            )
                        )
                    }
                continue
            }

            val action =
                findAction(
                    option.optString(
                        "action_id",
                    )
                )

            ensureTile().addView(
                buildBodyActionButton(
                    action = action,
                    label = label,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                ).apply {
                    topMargin =
                        Ui.dp(
                            this@NativeSectionActivity,
                            4,
                        )
                }
            )
        }

        commitTile()
    }

    private fun isSelectPrompt(
        label: String,
    ): Boolean {
        val normalized =
            label
                .trim()
                .uppercase()

        return normalized
            .startsWith(
                "ВЫБЕР",
            ) ||
            normalized
                .startsWith(
                    "ВИБЕР",
                ) ||
            normalized
                .startsWith(
                    "SELECT",
                )
    }

    private fun contentTile():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                Ui.roundedBackground(
                    context =
                        this@NativeSectionActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp = 12,
                )
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    8,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    8,
                ),
            )
        }

    private fun addContentTile(
        tile: LinearLayout,
    ) {
        bodyContainer.addView(
            tile,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@NativeSectionActivity,
                        6,
                    )
            }
        )
    }

    private fun tileHeader(
        label: String,
    ): TextView =
        Ui.textView(
            context = this,
            value = label,
            sizeSp = 13f,
            color = Ui.muted,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface
                    .BOLD,
            )
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    2,
                ),
                0,
                Ui.dp(
                    this@NativeSectionActivity,
                    2,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    2,
                ),
            )
        }

    private fun renderDocumentList(
        control: JSONObject,
    ) {
        val actionIds =
            control.optJSONArray(
                "action_ids",
            )
                ?: JSONArray()

        for (
            index in
            0 until actionIds.length()
        ) {
            val action =
                findAction(
                    actionIds.getString(
                        index,
                    )
                )
                    ?: continue

            addActionButton(action)
        }
    }

    private fun addActionButton(
        action: JSONObject,
        labelOverride: String? = null,
    ) {
        val rawLabel =
            labelOverride
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: action.optString(
                    "label",
                    "Відкрити",
                )

        val label =
            NativeRuntimePresentation
                .menuLabel(
                    rawLabel,
                )

        val tile =
            contentTile()

        tile.addView(
            buildBodyActionButton(
                action = action,
                label = label,
            )
        )

        addContentTile(tile)
    }

    private fun buildBodyActionButton(
        action: JSONObject?,
        label: String,
    ): Button {
        val enabled =
            action != null &&
                isActionResolvable(
                    action,
                )

        return Button(this).apply {
            text =
                if (enabled) {
                    "› " + label
                } else {
                    label
                }
            isAllCaps = false
            textSize = 14f
            gravity =
                Gravity.START or
                    Gravity.CENTER_VERTICAL
            minWidth = 0
            minimumWidth = 0
            minHeight =
                Ui.dp(
                    this@NativeSectionActivity,
                    42,
                )
            minimumHeight =
                Ui.dp(
                    this@NativeSectionActivity,
                    42,
                )
            isEnabled = enabled
            background =
                Ui.roundedBackground(
                    context =
                        this@NativeSectionActivity,
                    fill =
                        if (enabled) {
                            Ui.surfaceAlt
                        } else {
                            Ui.surface
                        },
                    stroke =
                        if (enabled) {
                            Ui.accent
                        } else {
                            Ui.border
                        },
                    radiusDp = 10,
                )
            setTextColor(
                if (enabled) {
                    Ui.text
                } else {
                    Ui.muted
                }
            )
            alpha =
                if (enabled) {
                    1f
                } else {
                    0.4f
                }
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                0,
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                0,
            )
            contentDescription =
                if (enabled) {
                    label + " — відкрити"
                } else {
                    label + " — недоступно"
                }
            setOnClickListener {
                if (
                    enabled &&
                    action != null
                ) {
                    handleAction(action)
                }
            }
        }
    }

    private fun addUnavailableItem(
        label: String,
    ) {
        val tile =
            contentTile()

        tile.addView(
            buildBodyActionButton(
                action = null,
                label = label,
            )
        )

        addContentTile(tile)
    }

    private fun isActionResolvable(
        action: JSONObject,
    ): Boolean =
        when (
            action.optString(
                "type",
            )
        ) {
            "route" -> {
                when (
                    action.optString(
                        "route_type",
                    )
                ) {
                    "open-panel" ->
                        findPanel(
                            action.optString(
                                "panel_id",
                            )
                        ) != null

                    "open-document" -> {
                        val documentId =
                            action.optString(
                                "document_id",
                            )
                        (
                            documentId.isNotBlank() &&
                                findDocument(
                                    documentId,
                                ) != null
                        ) ||
                            action.optString(
                                "target",
                            ).isNotBlank()
                    }

                    else ->
                        action.optString(
                            "target",
                        ).isNotBlank()
                }
            }

            "legacy-javascript",
            "print" -> true

            else -> false
        }

    private fun handleAction(
        action: JSONObject,
    ) {
        when (
            action.optString(
                "type",
            )
        ) {
            "route" -> {
                when (
                    action.optString(
                        "route_type",
                    )
                ) {
                    "open-panel" -> {
                        val panelId =
                            action.optString(
                                "panel_id",
                            )
                        if (
                            panelId.isNotBlank()
                        ) {
                            renderPanel(panelId)
                        }
                    }

                    "open-document" -> {
                        val documentId =
                            action.optString(
                                "document_id",
                            )
                        if (
                            documentId.isNotBlank()
                        ) {
                            renderOrOpenDocument(
                                documentId,
                                action.optString(
                                    "label",
                                    "Документ",
                                ),
                            )
                        } else {
                            openPath(
                                action.optString(
                                    "target",
                                ),
                                action.optString(
                                    "label",
                                    "Документ",
                                ),
                            )
                        }
                    }

                    else -> {
                        val target =
                            action.optString(
                                "target",
                            )
                        if (
                            target.isNotBlank()
                        ) {
                            openPath(
                                target,
                                action.optString(
                                    "label",
                                    "Документ",
                                ),
                            )
                        } else {
                            openLegacyFallback()
                        }
                    }
                }
            }

            "legacy-javascript" -> {
                Toast
                    .makeText(
                        this,
                        "Цей пункт ще потребує Classic runtime.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                openLegacyFallback()
            }

            "print" -> {
                Toast
                    .makeText(
                        this,
                        "Друк буде додано у native renderer пізніше.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }

            else -> {
                openLegacyFallback()
            }
        }
    }

    private fun renderOrOpenDocument(
        documentId: String,
        label: String,
    ) {
        val document =
            findDocument(documentId)
                ?: run {
                    openLegacyFallback()
                    return
                }

        when (
            document.optString(
                "type",
            )
        ) {
            "pdf" ->
                openPath(
                    document.optString(
                        "path",
                    ),
                    label,
                )
            "structured-html" ->
                renderStructuredDocument(
                    document,
                )
            "composite-document" ->
                renderCompositeDocument(
                    document,
                )
            else ->
                openPath(
                    document.optString(
                        "path",
                    ),
                    label,
                )
        }
    }

    private fun renderCompositeDocument(
        document: JSONObject,
    ) {
        currentViewKind =
            VIEW_DOCUMENT
        currentViewId =
            document.optString(
                "id",
            )
        bodyContainer.removeAllViews()

        bodyContainer.addView(
            Ui.textView(
                context = this,
                value =
                    sectionCode +
                        " — " +
                        sectionTitle,
                sizeSp = 19f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
                setPadding(
                    0,
                    0,
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        8,
                    ),
                )
            }
        )

        bodyContainer.addView(
            Ui.textView(
                context = this,
                value =
                    "Документи розʼєму",
                sizeSp = 13f,
                color = Ui.muted,
            ).apply {
                setPadding(
                    0,
                    0,
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        8,
                    ),
                )
            }
        )

        val parts =
            document.optJSONArray(
                "parts",
            )
                ?: JSONArray()

        val resolvedParts =
            mutableListOf<
                Pair<JSONObject, JSONObject>
            >()

        var hasDrawing = false
        var hasPins = false

        for (
            index in
            0 until parts.length()
        ) {
            val part =
                parts.getJSONObject(index)
            val nested =
                findDocument(
                    part.optString(
                        "document_id",
                    )
                )
                    ?: continue

            resolvedParts.add(
                Pair(
                    part,
                    nested,
                )
            )

            when (
                nested.optString(
                    "type",
                )
            ) {
                "pdf" ->
                    hasDrawing = true
                "structured-html" ->
                    hasPins = true
            }
        }

        val compositePath =
            document.optString(
                "path",
            )

        if (
            hasDrawing &&
            hasPins &&
            compositePath.isNotBlank()
        ) {
            addCompositePartButton(
                label =
                    "Схема + піни розʼєма",
            ) {
                openPath(
                    compositePath,
                    sectionCode +
                        " · схема + піни розʼєма",
                )
            }
        }

        for (
            resolved in
            resolvedParts
        ) {
            val part =
                resolved.first
            val role =
                part.optString("role")
            val nested =
                resolved.second

            when (
                nested.optString(
                    "type",
                )
            ) {
                "pdf" ->
                    addCompositePartButton(
                        label =
                            if (
                                role.equals(
                                    "dessin",
                                    ignoreCase = true,
                                )
                            ) {
                                "Схема розʼєму"
                            } else {
                                "PDF"
                            },
                    ) {
                        openPath(
                            nested.optString(
                                "path",
                            ),
                            sectionCode +
                                " · схема розʼєму",
                        )
                    }

                "structured-html" ->
                    addCompositePartButton(
                        label =
                            if (
                                role.equals(
                                    "alveoles",
                                    ignoreCase = true,
                                )
                            ) {
                                "Опис контактів"
                            } else {
                                "Опис"
                            },
                    ) {
                        renderStructuredDocument(
                            nested,
                        )
                    }

                else -> {
                    val path =
                        nested.optString(
                            "path",
                        )
                    if (path.isNotBlank()) {
                        addCompositePartButton(
                            label =
                                role
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Документ",
                        ) {
                            openPath(
                                path,
                                sectionCode +
                                    " · документ",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun addCompositePartButton(
        label: String,
        onClick: () -> Unit,
    ) {
        bodyContainer.addView(
            Button(this).apply {
                text = "› " + label
                isAllCaps = false
                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
                minimumHeight =
                    Ui.dp(
                        this@NativeSectionActivity,
                        48,
                    )
                contentDescription =
                    label + " — відкрити"
                setOnClickListener {
                    onClick()
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
                        this@NativeSectionActivity,
                        5,
                    )
            }
        )
    }

    private fun renderStructuredDocument(
        document: JSONObject,
    ) {
        currentViewKind =
            VIEW_DOCUMENT
        currentViewId =
            document.optString(
                "id",
            )
        bodyContainer.removeAllViews()

        val headings =
            structuredHeadingTexts(
                document,
            )
        val header =
            structuredHeader(
                headings,
            )
        val tables =
            structuredTables(
                document,
            )

        renderStructuredHeader(
            criteria = header.first,
            metadata = header.second,
        )

        if (tables.isEmpty()) {
            bodyContainer.addView(
                infoCard(
                    "Табличні дані відсутні."
                )
            )
            return
        }

        val export =
            NativeTablePdfData(
                title =
                    sectionCode +
                        " — " +
                        sectionTitle,
                criteria =
                    header.first,
                metadata =
                    header.second,
                tables =
                    tables,
                suggestedFileName =
                    structuredPdfFileName(
                        document =
                            document,
                        tables =
                            tables,
                    ),
            )

        bodyContainer.addView(
            Button(this).apply {
                text =
                    "Зберегти таблицю PDF"
                isAllCaps = false
                textSize = 13f
                gravity =
                    Gravity.CENTER
                minWidth = 0
                minimumWidth = 0
                minHeight =
                    Ui.dp(
                        this@NativeSectionActivity,
                        40,
                    )
                minimumHeight =
                    Ui.dp(
                        this@NativeSectionActivity,
                        40,
                    )
                background =
                    Ui.roundedBackground(
                        context =
                            this@NativeSectionActivity,
                        fill =
                            Ui.surfaceAlt,
                        stroke =
                            Ui.accent,
                        radiusDp = 10,
                    )
                setTextColor(
                    Ui.text,
                )
                setOnClickListener {
                    requestTablePdfExport(
                        export,
                    )
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
                        this@NativeSectionActivity,
                        8,
                    )
            }
        )

        val isPinesDocument =
            export.suggestedFileName
                .contains(
                    "(pines)",
                    ignoreCase = true,
                )

        for (
            tableIndex in
            tables.indices
        ) {
            bodyContainer.addView(
                buildStructuredTable(
                    table = tables[tableIndex],
                    ensurePinHeader =
                        isPinesDocument,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@NativeSectionActivity,
                            8,
                        )
                }
            )
        }
    }

    private fun structuredHeadingTexts(
        document: JSONObject,
    ): List<String> {
        val result =
            mutableListOf<String>()
        val headings =
            document.optJSONArray(
                "headings",
            )
                ?: JSONArray()

        for (
            index in
            0 until headings.length()
        ) {
            val value =
                headings
                    .getJSONObject(index)
                    .optString(
                        "text",
                    )
                    .trim()

            if (
                value.isNotBlank() &&
                value !in result
            ) {
                result.add(value)
            }
        }

        return result
    }

    private fun structuredHeader(
        headings: List<String>,
    ): Pair<
        String?,
        List<String>,
    > {
        val metadata =
            mutableListOf<String>()
        var criteria: String? = null

        val sectionPrefix =
            (
                sectionCode +
                    " " +
                    sectionTitle
            )
                .replace(
                    Regex("\\s+"),
                    " ",
                )
                .trim()

        for (heading in headings) {
            val normalized =
                heading
                    .replace(
                        Regex("\\s+"),
                        " ",
                    )
                    .trim()

            if (
                normalized.equals(
                    sectionCode,
                    ignoreCase = true,
                ) ||
                normalized.equals(
                    "CMP" + sectionCode,
                    ignoreCase = true,
                )
            ) {
                continue
            }

            if (
                normalized.startsWith(
                    sectionPrefix,
                    ignoreCase = true,
                )
            ) {
                val suffix =
                    normalized
                        .substring(
                            sectionPrefix.length,
                        )
                        .trim()
                        .trimStart(
                            '-',
                            '—',
                            ':',
                        )
                        .trim()

                if (suffix.isNotBlank()) {
                    criteria = suffix
                }
                continue
            }

            if (
                normalized.equals(
                    sectionTitle,
                    ignoreCase = true,
                )
            ) {
                continue
            }

            metadata.add(normalized)
        }

        return Pair(
            criteria,
            metadata,
        )
    }

    private fun renderStructuredHeader(
        criteria: String?,
        metadata: List<String>,
    ) {
        if (metadata.isNotEmpty()) {
            val metaTile =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER_HORIZONTAL
                    background =
                        Ui.roundedBackground(
                            context =
                                this@NativeSectionActivity,
                            fill =
                                Ui.surface,
                            stroke =
                                Ui.border,
                            radiusDp = 10,
                        )
                    setPadding(
                        Ui.dp(
                            this@NativeSectionActivity,
                            10,
                        ),
                        Ui.dp(
                            this@NativeSectionActivity,
                            7,
                        ),
                        Ui.dp(
                            this@NativeSectionActivity,
                            10,
                        ),
                        Ui.dp(
                            this@NativeSectionActivity,
                            7,
                        ),
                    )
                }

            for (
                index in
                metadata.indices
            ) {
                metaTile.addView(
                    Ui.textView(
                        context = this,
                        value =
                            metadata[index],
                        sizeSp =
                            if (index == 0) {
                                14f
                            } else {
                                13f
                            },
                        color = Ui.text,
                    ).apply {
                        gravity =
                            Gravity.CENTER
                        if (index == 0) {
                            setTypeface(
                                typeface,
                                android.graphics.Typeface
                                    .BOLD,
                            )
                        }
                    }
                )
            }

            bodyContainer.addView(
                metaTile,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                ).apply {
                    bottomMargin =
                        Ui.dp(
                            this@NativeSectionActivity,
                            8,
                        )
                }
            )
        }

        bodyContainer.addView(
            Ui.textView(
                context = this,
                value =
                    sectionCode +
                        " — " +
                        sectionTitle,
                sizeSp = 18f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
                setPadding(
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    0,
                    Ui.dp(
                        this@NativeSectionActivity,
                        2,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        3,
                    ),
                )
            }
        )

        criteria
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                bodyContainer.addView(
                    Ui.textView(
                        context = this,
                        value = it,
                        sizeSp = 14f,
                        color = Ui.accent,
                    ).apply {
                        setPadding(
                            Ui.dp(
                                this@NativeSectionActivity,
                                2,
                            ),
                            0,
                            Ui.dp(
                                this@NativeSectionActivity,
                                2,
                            ),
                            Ui.dp(
                                this@NativeSectionActivity,
                                8,
                            ),
                        )
                    }
                )
            }
    }

    private fun structuredTables(
        document: JSONObject,
    ): List<NativeTableData> {
        val result =
            mutableListOf<NativeTableData>()
        val tables =
            document.optJSONArray(
                "tables",
            )
                ?: JSONArray()

        for (
            tableIndex in
            0 until tables.length()
        ) {
            val rowsJson =
                tables
                    .getJSONObject(
                        tableIndex,
                    )
                    .optJSONArray(
                        "rows",
                    )
                    ?: JSONArray()

            val rows =
                mutableListOf<NativeTableRow>()

            for (
                rowIndex in
                0 until rowsJson.length()
            ) {
                val rowJson =
                    rowsJson.getJSONArray(
                        rowIndex,
                    )
                val cells =
                    mutableListOf<String>()
                var header = false

                for (
                    cellIndex in
                    0 until rowJson.length()
                ) {
                    val cell =
                        rowJson.getJSONObject(
                            cellIndex,
                        )
                    cells.add(
                        cell.optString(
                            "text",
                        ).trim()
                    )
                    header =
                        header ||
                            cell.optBoolean(
                                "header",
                                false,
                            )
                }

                if (
                    cells.any {
                        it.isNotBlank()
                    }
                ) {
                    rows.add(
                        NativeTableRow(
                            cells = cells,
                            header = header,
                        )
                    )
                }
            }

            if (rows.isNotEmpty()) {
                result.add(
                    NativeTableData(
                        rows = rows,
                    )
                )
            }
        }

        return result
    }

    private fun buildStructuredTable(
        table: NativeTableData,
        ensurePinHeader: Boolean = false,
    ): View {
        val sourceColumnCount =
            table.rows.maxOfOrNull {
                it.cells.size
            }
                ?: 1

        val displayRows =
            if (
                ensurePinHeader &&
                sourceColumnCount == 4 &&
                table.rows
                    .firstOrNull()
                    ?.header != true
            ) {
                listOf(
                    NativeTableRow(
                        cells =
                            listOf(
                                "№",
                                "мм²",
                                "Код",
                                "Опис",
                            ),
                        header = true,
                    )
                ) +
                    table.rows
            } else {
                table.rows
            }

        val columnCount =
            displayRows.maxOfOrNull {
                it.cells.size
            }
                ?: 1
        val compactWidths =
            IntArray(
                (columnCount - 1)
                    .coerceAtLeast(0),
            ) { column ->
                measuredCompactColumnWidth(
                    rows = displayRows,
                    column = column,
                    columnCount =
                        columnCount,
                )
            }

        val tableBody =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@NativeSectionActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp = 8,
                    )
                setPadding(
                    1,
                    1,
                    1,
                    1,
                )
            }

        for (row in displayRows) {
            val rowView =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            for (
                column in
                0 until columnCount
            ) {
                val value =
                    row.cells
                        .getOrNull(column)
                        .orEmpty()

                val emphasizeCode =
                    !row.header &&
                        columnCount == 2 &&
                        column == 0

                rowView.addView(
                    tableCell(
                        value = value,
                        header = row.header,
                        emphasize =
                            emphasizeCode,
                        column = column,
                        columnCount =
                            columnCount,
                    ),
                    if (
                        column <
                        columnCount - 1
                    ) {
                        LinearLayout.LayoutParams(
                            compactWidths[column],
                            LinearLayout.LayoutParams
                                .MATCH_PARENT,
                        )
                    } else {
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams
                                .MATCH_PARENT,
                            1f,
                        )
                    }
                )
            }

            tableBody.addView(
                rowView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                )
            )
        }

        return tableBody
    }

    private fun measuredCompactColumnWidth(
        rows: List<NativeTableRow>,
        column: Int,
        columnCount: Int,
    ): Int {
        val metrics =
            resources.displayMetrics
        var widest = 0f

        for (row in rows) {
            val value =
                row.cells
                    .getOrNull(
                        column,
                    )
                    .orEmpty()
            val paint =
                android.graphics.Paint(
                    android.graphics.Paint
                        .ANTI_ALIAS_FLAG,
                ).apply {
                    textSize =
                        android.util.TypedValue
                            .applyDimension(
                                android.util.TypedValue
                                    .COMPLEX_UNIT_SP,
                                if (row.header) {
                                    12f
                                } else {
                                    13f
                                },
                                metrics,
                            )
                    typeface =
                        if (
                            row.header ||
                            (
                                columnCount == 2 &&
                                    column == 0
                                )
                        ) {
                            android.graphics.Typeface
                                .DEFAULT_BOLD
                        } else {
                            android.graphics.Typeface
                                .DEFAULT
                        }
                }

            widest =
                maxOf(
                    widest,
                    paint.measureText(
                        value,
                    ),
                )
        }

        val horizontalPadding =
            Ui.dp(
                this,
                14,
            )
        val minimum =
            when (column) {
                0 ->
                    Ui.dp(
                        this,
                        42,
                    )

                1 ->
                    Ui.dp(
                        this,
                        48,
                    )

                else ->
                    Ui.dp(
                        this,
                        50,
                    )
            }

        return (
            widest +
                horizontalPadding
            )
                .toInt()
                .coerceAtLeast(
                    minimum,
                )
    }

    private fun tableCell(
        value: String,
        header: Boolean,
        emphasize: Boolean = false,
        column: Int,
        columnCount: Int,
    ): TextView =
        Ui.textView(
            context = this,
            value = value,
            sizeSp =
                if (header) {
                    12f
                } else {
                    13f
                },
            color =
                Ui.text,
        ).apply {
            gravity =
                if (
                    header ||
                    column <
                    columnCount - 1
                ) {
                    Gravity.CENTER
                } else {
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
                }

            if (
                header ||
                emphasize
            ) {
                setTypeface(
                    typeface,
                    android.graphics.Typeface
                        .BOLD,
                )
            }

            background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {
                        shape =
                            android.graphics.drawable
                                .GradientDrawable
                                .RECTANGLE
                        setColor(
                            if (header) {
                                Ui.surfaceAlt
                            } else {
                                Ui.surface
                            }
                        )
                        setStroke(
                            Ui.dp(
                                this@NativeSectionActivity,
                                1,
                            ),
                            Ui.border,
                        )
                    }

            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    5,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    5,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    5,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    5,
                ),
            )

            if (
                column <
                columnCount - 1
            ) {
                setSingleLine(true)
            }

            includeFontPadding = false
            setTextIsSelectable(true)
        }

    private fun structuredPdfFileName(
        document: JSONObject,
        tables: List<NativeTableData>,
    ): String {
        val path =
            document.optString(
                "path",
            )
        val fileStem =
            path
                .replace(
                    '\\',
                    '/',
                )
                .substringAfterLast(
                    '/',
                )
                .substringBeforeLast(
                    '.',
                )

        val maxColumns =
            tables.maxOfOrNull {
                table ->
                table.rows.maxOfOrNull {
                    it.cells.size
                } ?: 1
            } ?: 1

        val isPinTable =
            maxColumns >= 4

        // Connector IDs such as 101_1, 101_2 or 120_1 are opaque Renault
        // source identifiers. The suffix is not a duplicate counter and its
        // meaning must not be inferred here. Pin-table HTML commonly uses a
        // technical T_ prefix (for example T_101_1.HTM); strip only that
        // wrapper and preserve the remaining source stem verbatim.
        val sourceIdentifier =
            if (isPinTable) {
                val rawSourceIdentifier =
                    if (
                        fileStem.startsWith(
                            "T_",
                            ignoreCase = true,
                        )
                    ) {
                        fileStem.substring(2)
                    } else {
                        fileStem
                    }

                rawSourceIdentifier
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: sectionCode
            } else {
                sectionCode
            }

        val suffix =
            if (isPinTable) {
                "pines"
            } else if (
                tables.any {
                    table ->
                    table.rows.any {
                        row ->
                        row.header &&
                            row.cells.any {
                                cell ->
                                cell
                                    .uppercase()
                                    .contains(
                                        "СОКРА",
                                    )
                            }
                    }
                }
            ) {
                "abbreviations"
            } else {
                "table"
            }

        val safeIdentifier =
            sourceIdentifier
                .replace(
                    Regex(
                        "[^A-Za-z0-9_-]+"
                    ),
                    "_",
                )
                .trim(
                    '_',
                )
                .ifBlank {
                    "section"
                }

        return safeIdentifier +
            "(" +
            suffix +
            ").pdf"
    }

    private fun encodeTablePdfData(
        data: NativeTablePdfData,
    ): String {
        val root =
            JSONObject()
        root.put(
            "title",
            data.title,
        )
        root.put(
            "criteria",
            data.criteria,
        )
        root.put(
            "suggestedFileName",
            data.suggestedFileName,
        )

        val metadata =
            JSONArray()
        data.metadata.forEach {
            metadata.put(it)
        }
        root.put(
            "metadata",
            metadata,
        )

        val tables =
            JSONArray()
        for (table in data.tables) {
            val tableJson =
                JSONObject()
            val rows =
                JSONArray()

            for (row in table.rows) {
                val rowJson =
                    JSONObject()
                rowJson.put(
                    "header",
                    row.header,
                )

                val cells =
                    JSONArray()
                row.cells.forEach {
                    cells.put(it)
                }
                rowJson.put(
                    "cells",
                    cells,
                )
                rows.put(
                    rowJson,
                )
            }

            tableJson.put(
                "rows",
                rows,
            )
            tables.put(
                tableJson,
            )
        }
        root.put(
            "tables",
            tables,
        )

        return root.toString()
    }

    private fun decodeTablePdfData(
        raw: String,
    ): NativeTablePdfData? =
        runCatching {
            val root =
                JSONObject(raw)

            fun stringList(
                array: JSONArray?,
            ): List<String> {
                if (array == null) {
                    return emptyList()
                }

                return buildList {
                    for (
                        index in
                        0 until array.length()
                    ) {
                        add(
                            array.optString(
                                index,
                            )
                        )
                    }
                }
            }

            val tablesJson =
                root.optJSONArray(
                    "tables",
                )
                    ?: JSONArray()
            val tables =
                mutableListOf<NativeTableData>()

            for (
                tableIndex in
                0 until tablesJson.length()
            ) {
                val tableJson =
                    tablesJson
                        .getJSONObject(
                            tableIndex,
                        )
                val rowsJson =
                    tableJson.optJSONArray(
                        "rows",
                    )
                        ?: JSONArray()
                val rows =
                    mutableListOf<NativeTableRow>()

                for (
                    rowIndex in
                    0 until rowsJson.length()
                ) {
                    val rowJson =
                        rowsJson.getJSONObject(
                            rowIndex,
                        )
                    rows.add(
                        NativeTableRow(
                            cells =
                                stringList(
                                    rowJson
                                        .optJSONArray(
                                            "cells",
                                        )
                                ),
                            header =
                                rowJson.optBoolean(
                                    "header",
                                    false,
                                ),
                        )
                    )
                }

                tables.add(
                    NativeTableData(
                        rows = rows,
                    )
                )
            }

            NativeTablePdfData(
                title =
                    root.optString(
                        "title",
                    ),
                criteria =
                    if (
                        root.isNull(
                            "criteria",
                        )
                    ) {
                        null
                    } else {
                        root.optString(
                            "criteria",
                        )
                    },
                metadata =
                    stringList(
                        root.optJSONArray(
                            "metadata",
                        )
                    ),
                tables = tables,
                suggestedFileName =
                    root.optString(
                        "suggestedFileName",
                        "renault-table.pdf",
                    ),
            )
        }.getOrNull()

    private fun requestTablePdfExport(
        export: NativeTablePdfData,
    ) {
        pendingTablePdf = export

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
                    export.suggestedFileName,
                )
            }

        startActivityForResult(
            intent,
            REQUEST_SAVE_TABLE_PDF,
        )
    }

    private fun openPath(
        path: String,
        label: String,
    ) {
        if (path.isBlank()) {
            openLegacyFallback()
            return
        }

        startActivity(
            ViewerActivity
                .intentForEntrypoint(
                    context = this,
                    title = label,
                    entrypoint = path,
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

    private fun openLegacyFallback() {
        // "Classic" must be the untouched Classic runtime, not the old
        // hybrid target/projection flow. The previous behavior could leave
        // INDEX.HTM waiting for a target section with no named frames.
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
                )
        )
    }

    private fun firstOpenPanelAction(
        items: JSONArray,
    ): JSONObject? {
        for (
            index in
            0 until items.length()
        ) {
            val item =
                items.getJSONObject(
                    index,
                )
            val action =
                findAction(
                    item.optString(
                        "action_id",
                    )
                )
                    ?: continue

            if (
                action.optString(
                    "route_type",
                ) == "open-panel"
            ) {
                return action
            }
        }
        return null
    }

    private fun collectControlActionIds(
        control: JSONObject,
        output: MutableSet<String>,
    ) {
        val items =
            control.optJSONArray(
                "items",
            )
        if (items != null) {
            for (
                index in
                0 until items.length()
            ) {
                val actionId =
                    items
                        .getJSONObject(index)
                        .optString(
                            "action_id",
                        )
                if (actionId.isNotBlank()) {
                    output.add(actionId)
                }
            }
        }

        val options =
            control.optJSONArray(
                "options",
            )
        if (options != null) {
            for (
                index in
                0 until options.length()
            ) {
                val actionId =
                    options
                        .getJSONObject(index)
                        .optString(
                            "action_id",
                        )
                if (actionId.isNotBlank()) {
                    output.add(actionId)
                }
            }
        }

        val actionIds =
            control.optJSONArray(
                "action_ids",
            )
        if (actionIds != null) {
            for (
                index in
                0 until actionIds.length()
            ) {
                output.add(
                    actionIds.getString(
                        index,
                    )
                )
            }
        }
    }

    private fun findPanel(
        id: String,
    ): JSONObject? =
        findById(
            "panels",
            id,
        )

    private fun findControl(
        id: String,
    ): JSONObject? =
        findById(
            "controls",
            id,
        )

    private fun findAction(
        id: String,
    ): JSONObject? =
        findById(
            "actions",
            id,
        )

    private fun findDocument(
        id: String,
    ): JSONObject? =
        findById(
            "documents",
            id,
        )

    private fun findById(
        key: String,
        id: String,
    ): JSONObject? {
        if (id.isBlank()) {
            return null
        }

        findControlIn(
            root =
                runtimeData
                    ?.section,
            key = key,
            id = id,
        )?.let {
            return it
        }

        return findControlIn(
            root =
                runtimeDocumentation,
            key = key,
            id = id,
        )
    }

    private fun findControlIn(
        root: JSONObject?,
        key: String,
        id: String,
    ): JSONObject? {
        if (
            root == null ||
            id.isBlank()
        ) {
            return null
        }

        val array =
            root.optJSONArray(
                key,
            )
                ?: return null

        for (
            index in
            0 until array.length()
        ) {
            val item =
                array.getJSONObject(
                    index,
                )
            if (
                item.optString(
                    "id",
                ) == id
            ) {
                return item
            }
        }

        return null
    }

    private fun infoCard(
        message: String,
    ): View =
        Ui.textView(
            context = this,
            value = message,
            sizeSp = 14f,
            color = Ui.text,
        ).apply {
            background =
                Ui.roundedBackground(
                    context =
                        this@NativeSectionActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp = 10,
                )
            setPadding(
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    9,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    10,
                ),
                Ui.dp(
                    this@NativeSectionActivity,
                    9,
                ),
            )
        }

    private fun showFatalError(
        message: String,
    ) {
        window.statusBarColor =
            Ui.background
        window.navigationBarColor =
            Ui.background

        setContentView(
            Ui.textView(
                context = this,
                value = message,
                sizeSp = 16f,
                color = Ui.danger,
            ).apply {
                setPadding(
                    Ui.dp(
                        this@NativeSectionActivity,
                        20,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        20,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        20,
                    ),
                    Ui.dp(
                        this@NativeSectionActivity,
                        20,
                    ),
                )
            }
        )
    }

    companion object {
        private const val HELP_SECTION =
            "section"
        private const val STATE_MENU_LABEL =
            "nativeMenuLabel"
        private const val STATE_MENU_ACTION_ID =
            "nativeMenuActionId"

        private const val STATE_VIEW_KIND =
            "nativeViewKind"
        private const val STATE_VIEW_ID =
            "nativeViewId"
        private const val STATE_PENDING_TABLE_PDF =
            "pendingTablePdf"

        private const val VIEW_DEFAULT =
            "default"
        private const val VIEW_PANEL =
            "panel"
        private const val VIEW_DOCUMENT =
            "document"
        private const val VIEW_DOCUMENTATION =
            "documentation"

        private const val EXTRA_TREE_URI =
            "treeUri"
        private const val EXTRA_DATASET_TITLE =
            "datasetTitle"
        private const val EXTRA_CLASSIC_ENTRYPOINT =
            "classicEntrypoint"
        private const val EXTRA_VOLUME_TITLE =
            "volumeTitle"
        private const val EXTRA_VOLUME_ENTRYPOINT =
            "volumeEntrypoint"
        private const val EXTRA_SECTION_CODE =
            "sectionCode"
        private const val EXTRA_SECTION_TITLE =
            "sectionTitle"
        private const val EXTRA_SECTION_LEGACY_ENTRYPOINT =
            "sectionLegacyEntrypoint"
        private const val REQUEST_SAVE_TABLE_PDF =
            4107

        fun intentForSection(
            context: Context,
            treeUri: String,
            datasetTitle: String,
            classicEntrypoint: String,
            volumeTitle: String,
            volumeEntrypoint: String,
            section: ModernSection,
        ): Intent =
            Intent(
                context,
                NativeSectionActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_TREE_URI,
                    treeUri,
                )
                putExtra(
                    EXTRA_DATASET_TITLE,
                    datasetTitle,
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
                    EXTRA_SECTION_CODE,
                    section.code,
                )
                putExtra(
                    EXTRA_SECTION_TITLE,
                    section.title,
                )
                putExtra(
                    EXTRA_SECTION_LEGACY_ENTRYPOINT,
                    section.entrypoint,
                )
            }
    }
}
