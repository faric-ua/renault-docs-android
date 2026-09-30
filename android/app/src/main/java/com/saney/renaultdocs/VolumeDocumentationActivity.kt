package com.saney.renaultdocs

import android.app.Activity
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
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class VolumeDocumentationActivity : Activity() {
    private lateinit var bodyContainer:
        LinearLayout
    private lateinit var helpDialogs:
        LifecycleHelpDialogController

    private var datasetTitle: String =
        "Renault dataset"
    private var treeUriText: String = ""
    private var classicEntrypoint: String = ""
    private var volumeTitle: String =
        "Renault volume"
    private var volumeEntrypoint: String = ""

    private var runtimeData:
        RuntimeIrVolumeDocumentationData? =
        null

    private val panelStack =
        mutableListOf<String>()

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

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

        savedInstanceState
            ?.getStringArrayList(
                STATE_PANEL_STACK,
            )
            ?.let {
                panelStack.addAll(it)
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
            volumeEntrypoint.isBlank()
        ) {
            showFatalError(
                "Документація не отримала контекст тому."
            )
            return
        }

        setContentView(
            buildContent(),
        )
        helpDialogs.restoreOpen()

        loadDocumentation()
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putStringArrayList(
            STATE_PANEL_STACK,
            ArrayList(panelStack),
        )
        helpDialogs.save(
            outState,
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (panelStack.isNotEmpty()) {
            panelStack.removeAt(
                panelStack.lastIndex,
            )
            renderCurrent()
            return
        }

        super.onBackPressed()
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
                onBackPressed()
            }
        )

        val titleBlock =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        6,
                    ),
                    0,
                )

                addView(
                    Ui.textView(
                        context =
                            this@VolumeDocumentationActivity,
                        value =
                            "Документація",
                        sizeSp = 19f,
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
                            this@VolumeDocumentationActivity,
                        value =
                            volumeTitle,
                        sizeSp = 12f,
                        color =
                            Ui.entityTitle,
                    ).apply {
                        maxLines = 1
                    }
                )
            }

        topBar.addView(
            titleBlock,
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
            Ui.helpButton(
                context =
                    this,
            ) {
                helpDialogs.show(
                    HELP_DOCUMENTATION,
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

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        bodyContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        10,
                    ),
                    0,
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        24,
                    ),
                )
            }

        scroll.addView(
            bodyContainer,
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
            HELP_DOCUMENTATION ->
                HelpDialogSpec(
                    title =
                        "Документація тому",
                    message =
                        "Цей екран відтворює native-меню документації конкретного тому. Пункти й вкладені панелі беруться з Runtime IR і ведуть до тих самих документів, що й Classic.\n\n" +
                            "Якщо ти зайшов у вкладену панель, «Назад» повертає на попередній рівень. Відкрита панель і Help відновлюються після rotation.",
                )

            else ->
                null
        }

    private fun loadDocumentation() {
        bodyContainer.removeAllViews()
        bodyContainer.addView(
            infoCard(
                "Відкриваю документацію тому…"
            )
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
                            volumeEntrypoint,
                    )

            runOnUiThread {
                result
                    .onSuccess {
                        runtimeData = it
                        volumeTitle =
                            it.volumeTitle
                                .takeIf {
                                    title ->
                                    title.isNotBlank()
                                }
                                ?: volumeTitle

                        sanitizePanelStack()
                        renderCurrent()
                    }
                    .onFailure {
                        renderLoadFailure(
                            it.message
                                ?: "Документацію тому не вдалося відкрити."
                        )
                    }
            }
        }.start()
    }

    private fun sanitizePanelStack() {
        while (
            panelStack.isNotEmpty() &&
            findPanel(
                panelStack.last()
            ) == null
        ) {
            panelStack.removeAt(
                panelStack.lastIndex,
            )
        }
    }

    private fun renderCurrent() {
        if (runtimeData == null) {
            return
        }

        val panelId =
            panelStack.lastOrNull()

        if (panelId == null) {
            renderRoot()
        } else {
            renderPanel(panelId)
        }
    }

    private fun renderRoot() {
        bodyContainer.removeAllViews()

        val data =
            runtimeData
                ?: return

        bodyContainer.addView(
            heading(
                "Документація тому"
            )
        )

        val metadata =
            listOfNotNull(
                data.documentCode
                    ?.takeIf {
                        it.isNotBlank()
                    },
                data.date
                    ?.takeIf {
                        it.isNotBlank()
                    },
            )

        if (metadata.isNotEmpty()) {
            bodyContainer.addView(
                Ui.textView(
                    context = this,
                    value =
                        metadata.joinToString(
                            " · "
                        ),
                    sizeSp = 12f,
                    color =
                        Ui.muted,
                ).apply {
                    setPadding(
                        Ui.dp(
                            this@VolumeDocumentationActivity,
                            2,
                        ),
                        0,
                        0,
                        Ui.dp(
                            this@VolumeDocumentationActivity,
                            10,
                        ),
                    )
                }
            )
        }

        val items =
            documentation()
                ?.optJSONArray(
                    "menu_items",
                )
                ?: JSONArray()

        if (items.length() == 0) {
            bodyContainer.addView(
                infoCard(
                    "У цьому томі немає native-меню документації."
                )
            )
            return
        }

        for (
            index in
            0 until items.length()
        ) {
            val item =
                items.getJSONObject(index)
            val action =
                findAction(
                    item.optString(
                        "action_id",
                    )
                )
            val label =
                NativeRuntimePresentation
                    .menuLabel(
                        item.optString(
                            "label",
                            "Документ",
                        )
                    )

            addActionTile(
                action = action,
                label = label,
            )
        }
    }

    private fun renderPanel(
        panelId: String,
    ) {
        val panel =
            findPanel(panelId)
                ?: run {
                    panelStack.clear()
                    renderRoot()
                    return
                }

        bodyContainer.removeAllViews()

        val title =
            panel.optString(
                "title",
            )
                .ifBlank {
                    panel.optString(
                        "label",
                    )
                }
                .ifBlank {
                    "Документація"
                }

        bodyContainer.addView(
            heading(
                NativeRuntimePresentation
                    .menuLabel(title)
            )
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
            renderControl(control)
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

            addActionTile(
                action = action,
                label =
                    NativeRuntimePresentation
                        .menuLabel(
                            action.optString(
                                "label",
                                "Відкрити",
                            )
                        ),
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

            "document-list" -> {
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

                    addActionTile(
                        action = action,
                        label =
                            NativeRuntimePresentation
                                .menuLabel(
                                    action.optString(
                                        "label",
                                        "Відкрити",
                                    )
                                ),
                    )
                }
            }

            "action-bar" -> {
                val items =
                    control.optJSONArray(
                        "items",
                    )
                        ?: JSONArray()

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

                    addActionTile(
                        action = action,
                        label =
                            NativeRuntimePresentation
                                .menuLabel(
                                    item.optString(
                                        "label",
                                        action.optString(
                                            "label",
                                            "Відкрити",
                                        ),
                                    )
                                ),
                    )
                }
            }
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
                if (
                    kind == "separator" ||
                    label.isBlank()
                ) {
                    continue
                }

                bodyContainer.addView(
                    sectionLabel(label)
                )
                continue
            }

            val action =
                findAction(
                    option.optString(
                        "action_id",
                    )
                )

            addActionTile(
                action = action,
                label =
                    NativeRuntimePresentation
                        .menuLabel(label),
            )
        }
    }

    private fun addActionTile(
        action: JSONObject?,
        label: String,
    ) {
        val enabled =
            action != null &&
                isActionResolvable(
                    action,
                )

        val tile =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                background =
                    Ui.roundedBackground(
                        context =
                            this@VolumeDocumentationActivity,
                        fill =
                            Ui.surface,
                        stroke =
                            Ui.border,
                        radiusDp = 12,
                    )
                setPadding(
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        10,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        7,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        10,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        7,
                    ),
                )
            }

        tile.addView(
            Button(this).apply {
                text =
                    if (enabled) {
                        "› $label"
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
                minimumHeight =
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        46,
                    )
                isEnabled = enabled
                alpha =
                    if (enabled) {
                        1f
                    } else {
                        0.4f
                    }
                setTextColor(
                    if (enabled) {
                        Ui.text
                    } else {
                        Ui.muted
                    }
                )
                background =
                    Ui.roundedBackground(
                        context =
                            this@VolumeDocumentationActivity,
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
                contentDescription =
                    if (enabled) {
                        "$label — відкрити"
                    } else {
                        "$label — недоступно"
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
        )

        bodyContainer.addView(
            tile,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin =
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        6,
                    )
            }
        )
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
                            panelId.isNotBlank() &&
                            findPanel(panelId) != null
                        ) {
                            panelStack.add(panelId)
                            renderPanel(panelId)
                        }
                    }

                    "open-document" -> {
                        val documentId =
                            action.optString(
                                "document_id",
                            )
                        val label =
                            action.optString(
                                "label",
                                "Документ",
                            )

                        if (
                            documentId.isNotBlank()
                        ) {
                            openDocument(
                                documentId =
                                    documentId,
                                label =
                                    label,
                            )
                        } else {
                            openPath(
                                path =
                                    action.optString(
                                        "target",
                                    ),
                                label =
                                    label,
                            )
                        }
                    }

                    else ->
                        openPath(
                            path =
                                action.optString(
                                    "target",
                                ),
                            label =
                                action.optString(
                                    "label",
                                    "Документ",
                                ),
                        )
                }
            }

            "legacy-javascript" -> {
                Toast
                    .makeText(
                        this,
                        "Цей пункт відкриється через Classic runtime.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                openClassicVolume()
            }

            "print" ->
                Toast
                    .makeText(
                        this,
                        "Друк поки не підтримується на окремому екрані документації.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
        }
    }

    private fun openDocument(
        documentId: String,
        label: String,
    ) {
        val document =
            findDocument(documentId)

        if (document == null) {
            openClassicVolume()
            return
        }

        val path =
            document.optString(
                "path",
            ).trim()

        if (path.isNotBlank()) {
            openPath(
                path = path,
                label = label,
            )
            return
        }

        val parts =
            document.optJSONArray(
                "parts",
            )
                ?: JSONArray()

        for (
            index in
            0 until parts.length()
        ) {
            val nestedId =
                parts
                    .getJSONObject(index)
                    .optString(
                        "document_id",
                    )
            val nested =
                findDocument(nestedId)
                    ?: continue
            val nestedPath =
                nested.optString(
                    "path",
                ).trim()

            if (nestedPath.isNotBlank()) {
                openPath(
                    path = nestedPath,
                    label = label,
                )
                return
            }
        }

        openClassicVolume()
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

    private fun documentation():
        JSONObject? =
        runtimeData
            ?.documentation

    private fun findPanel(
        id: String,
    ): JSONObject? =
        findById(
            key = "panels",
            id = id,
        )

    private fun findControl(
        id: String,
    ): JSONObject? =
        findById(
            key = "controls",
            id = id,
        )

    private fun findAction(
        id: String,
    ): JSONObject? =
        findById(
            key = "actions",
            id = id,
        )

    private fun findDocument(
        id: String,
    ): JSONObject? =
        findById(
            key = "documents",
            id = id,
        )

    private fun findById(
        key: String,
        id: String,
    ): JSONObject? {
        if (id.isBlank()) {
            return null
        }

        val array =
            documentation()
                ?.optJSONArray(key)
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

    private fun heading(
        value: String,
    ): TextView =
        Ui.textView(
            context = this,
            value = value,
            sizeSp = 19f,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            setPadding(
                0,
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    4,
                ),
                0,
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
            )
        }

    private fun sectionLabel(
        value: String,
    ): TextView =
        Ui.textView(
            context = this,
            value = value,
            sizeSp = 13f,
            color = Ui.muted,
        ).apply {
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD,
            )
            setPadding(
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    4,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    5,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    4,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    6,
                ),
            )
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
                        this@VolumeDocumentationActivity,
                    fill =
                        Ui.surface,
                    stroke =
                        Ui.border,
                    radiusDp = 10,
                )
            setPadding(
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    12,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    12,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
            )
        }

    private fun renderLoadFailure(
        message: String,
    ) {
        bodyContainer.removeAllViews()
        bodyContainer.addView(
            infoCard(message)
        )

        bodyContainer.addView(
            Button(this).apply {
                text = "Відкрити Classic"
                isAllCaps = false
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
                        this@VolumeDocumentationActivity,
                        10,
                    )
            }
        )
    }

    private fun openPath(
        path: String,
        label: String,
    ) {
        if (path.isBlank()) {
            openClassicVolume()
            return
        }

        startActivity(
            ViewerActivity
                .intentForEntrypoint(
                    context = this,
                    title =
                        NativeRuntimePresentation
                            .menuLabel(label),
                    entrypoint =
                        path,
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
                )
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
                    this@VolumeDocumentationActivity,
                    44,
                )
            minimumHeight =
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    44,
                )
            setPadding(
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
                    10,
                ),
                Ui.dp(
                    this@VolumeDocumentationActivity,
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

        setContentView(
            Ui.textView(
                context = this,
                value = message,
                sizeSp = 16f,
                color = Ui.danger,
            ).apply {
                setPadding(
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        20,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        20,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        20,
                    ),
                    Ui.dp(
                        this@VolumeDocumentationActivity,
                        20,
                    ),
                )
            }
        )
    }

    companion object {
        private const val HELP_DOCUMENTATION =
            "documentation"
        private const val STATE_PANEL_STACK =
            "documentationPanelStack"

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

        fun intentForVolume(
            context: Context,
            treeUri: String,
            datasetTitle: String,
            classicEntrypoint: String,
            volumeTitle: String,
            volumeEntrypoint: String,
        ): Intent =
            Intent(
                context,
                VolumeDocumentationActivity::class.java,
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
            }
    }
}
