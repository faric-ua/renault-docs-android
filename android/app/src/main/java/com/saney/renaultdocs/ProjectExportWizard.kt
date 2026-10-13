package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Step-by-step window for exporting separate .rdpkg volumes (#178):
 * 1) choose volumes → 2) choose folder → 3) check and start.
 *
 * Fixed header (title, step, ✕) and fixed footer (buttons in one row) stay visible;
 * only the middle scrolls. The window never owns the export: "Почати експорт" hands
 * exact IDs + folder to the caller, closes, and the existing Home status takes over.
 */
class ProjectExportWizard(
    private val activity: Activity,
    private val project: RenaultProject,
    private val plan: List<ProjectVolumeExportPlan.Item>,
    private val state: ProjectExportWizardState,
    private val onClose: () -> Unit,
    private val onPickFolder: () -> Unit,
    private val onStart: (folderUri: String, volumeIds: List<String>) -> Unit,
) {
    lateinit var dialog: AlertDialog
        private set

    private val planIds = plan.map { it.volumeId }
    // v0.5.124: volumes already in the public catalog (filled by «Лише нові для каталогу»).
    private var publishedIds: Set<String> = emptySet()
    private var catalogMessage: String = ""
    private lateinit var stepLabel: TextView
    private lateinit var body: FrameLayout
    private lateinit var footer: LinearLayout

    fun show(): AlertDialog {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(buildHeader(), matchWrap())
        root.addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
        body = FrameLayout(activity)
        // Weighted body shrinks first, so header and footer are never pushed off-screen.
        root.addView(body, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
        footer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(8))
        }
        root.addView(footer, matchWrap())

        dialog = AlertDialog.Builder(activity)
            .setView(root)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                goBack()
                true
            } else {
                keyCode == KeyEvent.KEYCODE_BACK
            }
        }
        dialog.show()
        DialogUi.apply(dialog, DialogRole.CHOICE)
        render()
        return dialog
    }

    /** Re-draws the current step in place (also after a folder was picked). */
    fun render() {
        if (!::body.isInitialized) return
        stepLabel.text = "Крок ${state.step.number} з 3 · " + when (state.step) {
            ProjectExportWizardState.Step.VOLUMES -> "Вибери томи"
            ProjectExportWizardState.Step.FOLDER -> "Куди зберегти"
            ProjectExportWizardState.Step.CONFIRM -> "Перевір і почни"
        }
        body.removeAllViews()
        footer.removeAllViews()
        when (state.step) {
            ProjectExportWizardState.Step.VOLUMES -> renderVolumes()
            ProjectExportWizardState.Step.FOLDER -> renderFolder()
            ProjectExportWizardState.Step.CONFIRM -> renderConfirm()
        }
    }

    private fun buildHeader(): View {
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(6), dp(12))
        }
        val titles = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(
            Ui.textView(activity, "Експорт томів · ${project.title}", 19f, Ui.entityTitle).apply {
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            },
        )
        stepLabel = Ui.textView(activity, "", 14f, Ui.muted).apply {
            setPadding(0, dp(2), 0, 0)
        }
        titles.addView(stepLabel)
        header.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val close = Ui.textView(activity, "✕", 22f, Ui.text).apply {
            gravity = Gravity.CENTER
            contentDescription = "Закрити"
            isClickable = true
            isFocusable = true
            minWidth = dp(48)
            minHeight = dp(48)
            setOnClickListener { close() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        return header
    }

    private fun renderVolumes() {
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(10))
        }
        val counter = Ui.textView(activity, "", 15f, Ui.text)
        val toggleAll = Ui.textView(activity, "", 15f, Ui.accent).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            isClickable = true
            isFocusable = true
            minHeight = dp(48)
            setPadding(dp(12), 0, dp(4), 0)
        }
        val summary = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(6), dp(16), 0)
        }
        summary.addView(counter, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        summary.addView(toggleAll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)))

        val next = footerButton("Далі", primary = true) { goNext() }
        val rows = mutableListOf<Pair<CheckBox, View>>()
        val notes = mutableListOf<TextView>()
        // v0.5.124 (owner): one tap — only the volumes not yet in «Готові проєкти».
        val onlyNew = Ui.textView(activity, "", 15f, Ui.accent).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minHeight = dp(44)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = Ui.roundedBackground(activity, Ui.surfaceAlt, Ui.accent, 10)
        }
        val catalogStatus = Ui.textView(activity, "", 13f, Ui.muted).apply {
            setPadding(dp(16), dp(4), dp(16), 0)
        }

        fun refresh() {
            onlyNew.text = "Вибрати лише нові для каталогу"
            catalogStatus.text = catalogMessage
            catalogStatus.visibility = if (catalogMessage.isBlank()) View.GONE else View.VISIBLE
            notes.forEachIndexed { index, note ->
                val published = planIds[index] in publishedIds
                note.visibility = if (published) View.VISIBLE else View.GONE
            }
            counter.text = "Вибрано ${state.selectedCount} із ${plan.size}"
            toggleAll.text = if (state.allSelected(planIds)) "Очистити" else "Вибрати всі"
            setEnabled(next, state.canGoNext())
            rows.forEachIndexed { index, (box, row) ->
                val checked = state.isSelected(planIds[index])
                if (box.isChecked != checked) box.isChecked = checked
                row.background = rowBackground(checked)
            }
        }

        plan.forEach { item ->
            val identity = listOfNotNull(
                item.documentCode?.takeIf { it.isNotBlank() },
                item.date?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { item.fileName }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(60)
                setPadding(dp(8), dp(8), dp(12), dp(8))
                isClickable = true
                isFocusable = true
            }
            val box = CheckBox(activity).apply {
                buttonTintList = ColorStateList.valueOf(Ui.accent)
                isClickable = false
                isFocusable = false
            }
            val texts = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(
                Ui.textView(activity, identity, 17f, Ui.text).apply {
                    setTypeface(typeface, Typeface.BOLD)
                },
            )
            texts.addView(Ui.textView(activity, item.fileName, 13f, Ui.muted))
            val note = Ui.textView(activity, "уже в каталозі «Готові проєкти»", 13f, Ui.success).apply {
                visibility = View.GONE
            }
            texts.addView(note)
            notes += note
            row.addView(box, LinearLayout.LayoutParams(dp(44), dp(44)))
            row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            })
            row.setOnClickListener {
                state.setSelected(item.volumeId, !state.isSelected(item.volumeId))
                refresh()
            }
            rows += box to row
            content.addView(row, matchWrap().apply { bottomMargin = dp(8) })
        }
        toggleAll.setOnClickListener {
            state.toggleAll(planIds)
            refresh()
        }
        onlyNew.setOnClickListener {
            catalogMessage = "Перевіряю каталог «Готові проєкти»…"
            refresh()
            Thread {
                val result = runCatching {
                    CatalogPublishedFilter.publishedVolumeIds(
                        plan,
                        CatalogPublishedFilter.catalogVolumesFor(
                            DriveCatalogClient.loadCatalog(),
                            project.id,
                        ),
                    )
                }
                activity.runOnUiThread {
                    if (!::body.isInitialized || state.step != ProjectExportWizardState.Step.VOLUMES) {
                        return@runOnUiThread
                    }
                    result
                        .onSuccess { published ->
                            publishedIds = published
                            state.selectOnly(planIds) { it !in published }
                            catalogMessage =
                                "У каталозі вже є ${published.size} із ${plan.size}. " +
                                    "Вибрано нові: ${state.selectedCount}."
                        }
                        .onFailure {
                            catalogMessage =
                                "Каталог не завантажився: " +
                                    (it.message ?: "немає інтернету?") +
                                    " Вибір не змінено."
                        }
                    refresh()
                }
            }.start()
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = false
            addView(content)
            setOnScrollChangeListener { _, _, scrollY, _, _ -> state.scrollY = scrollY }
        }
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        column.addView(summary, matchWrap())
        column.addView(
            onlyNew,
            matchWrap().apply {
                marginStart = dp(16)
                marginEnd = dp(16)
                topMargin = dp(4)
            },
        )
        column.addView(catalogStatus, matchWrap())
        column.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        body.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        footer.addView(next, buttonParams())
        refresh()
        val restoreY = state.scrollY
        scroll.post { scroll.scrollTo(0, restoreY) }
    }

    private var labelRequestedFor: String? = null

    /**
     * #261: folder name (and the app / account owning it). A cloud folder's name is
     * asked from its app on a background thread; the step re-draws when it arrives.
     */
    private fun resolvedFolderLabel(): String? {
        val uri = state.folderUri ?: return null
        SafFolderLabel.cached(uri)?.let { return it }
        if (labelRequestedFor != uri) {
            labelRequestedFor = uri
            val appContext = activity.applicationContext
            Thread {
                val label = SafFolderLabel.resolve(appContext, uri)
                if (label != null) {
                    activity.runOnUiThread {
                        if (::dialog.isInitialized && dialog.isShowing && state.folderUri == uri) render()
                    }
                }
            }.start()
        }
        return SafDisplayPath.tree(uri)
    }

    private fun renderFolder() {
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(12))
        }
        content.addView(
            Ui.textView(activity, "Куди зберегти ${volumeCountLabel(state.selectedCount)}?", 17f, Ui.text),
            matchWrap(),
        )
        val folderLabel = resolvedFolderLabel()
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rowBackground(state.folderUri != null)
        }
        card.addView(
            Ui.textView(
                activity,
                when {
                    state.folderUri == null -> "Папку ще не вибрано"
                    folderLabel.isNullOrBlank() -> "Папку вибрано"
                    else -> folderLabel
                },
                16f,
                Ui.text,
            ).apply { setTypeface(typeface, Typeface.BOLD) },
        )
        card.addView(
            Ui.textView(
                activity,
                "Краще нова порожня папка. Однойменні файли там не перезаписуються.",
                13f,
                Ui.muted,
            ).apply { setPadding(0, dp(4), 0, 0) },
        )
        content.addView(card, matchWrap().apply { topMargin = dp(12) })
        val pick = footerButton(
            if (state.folderUri == null) "Вибрати папку" else "Змінити папку",
            primary = state.folderUri == null,
        ) { onPickFolder() }
        content.addView(pick, matchWrap().apply { topMargin = dp(12) })
        body.addView(ScrollView(activity).apply { addView(content) })

        val next = footerButton("Далі", primary = state.folderUri != null) { goNext() }
        setEnabled(next, state.canGoNext())
        footer.addView(footerButton("Назад", primary = false) { goBack() }, buttonParams())
        footer.addView(next, buttonParams())
    }

    private fun renderConfirm() {
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(12))
        }
        val selected = plan.filter { state.isSelected(it.volumeId) }
        content.addView(
            Ui.textView(activity, "Буде створено ${volumeCountLabel(selected.size)} .rdpkg", 17f, Ui.text).apply {
                setTypeface(typeface, Typeface.BOLD)
            },
            matchWrap(),
        )
        content.addView(
            Ui.textView(
                activity,
                selected.joinToString("\n") { item ->
                    "• " + listOfNotNull(item.documentCode, item.date).joinToString(" · ").ifBlank { item.fileName }
                },
                15f,
                Ui.text,
            ).apply { setPadding(0, dp(6), 0, 0) },
            matchWrap(),
        )
        content.addView(
            Ui.textView(
                activity,
                "Папка: " + (resolvedFolderLabel() ?: "вибрана"),
                15f,
                Ui.text,
            ).apply { setPadding(0, dp(12), 0, 0) },
            matchWrap(),
        )
        content.addView(
            Ui.textView(
                activity,
                "Однойменні файли не перезаписуються. У кінці буде короткий JSON-звіт. " +
                    "Каталог Google Drive не змінюється.",
                13f,
                Ui.muted,
            ).apply { setPadding(0, dp(12), 0, 0) },
            matchWrap(),
        )
        body.addView(ScrollView(activity).apply { addView(content) })

        val start = footerButton("Почати експорт", primary = true) {
            val folder = state.folderUri
            if (state.canStart() && folder != null) {
                val ids = state.selectedVolumeIds
                dialog.dismiss()
                onStart(folder, ids)
            }
        }
        setEnabled(start, state.canStart())
        footer.addView(footerButton("Назад", primary = false) { goBack() }, buttonParams())
        footer.addView(start, buttonParams())
    }

    private fun goNext() {
        if (state.next()) {
            render()
        }
    }

    private fun goBack() {
        if (state.back()) {
            render()
        } else {
            close()
        }
    }

    private fun close() {
        dialog.dismiss()
        onClose()
    }

    private fun footerButton(label: String, primary: Boolean, onClick: () -> Unit): Button =
        Button(activity).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(if (primary) Ui.accent else Ui.text)
            background = Ui.roundedBackground(
                context = activity,
                fill = Ui.surfaceAlt,
                stroke = if (primary) Ui.accent else Ui.border,
                radiusDp = 10,
            )
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { onClick() }
        }

    private fun setEnabled(button: Button, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.45f
    }

    private fun rowBackground(selected: Boolean) =
        Ui.roundedBackground(
            context = activity,
            fill = Ui.surface,
            stroke = if (selected) Ui.accent else Ui.border,
            radiusDp = 12,
        )

    private fun divider(): View = View(activity).apply { setBackgroundColor(Ui.border) }

    private fun matchWrap() =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun buttonParams() =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(10)
        }

    private fun dp(value: Int) = Ui.dp(activity, value)

    private fun volumeCountLabel(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val noun = when {
            mod100 in 11..14 -> "томів"
            mod10 == 1 -> "том"
            mod10 in 2..4 -> "томи"
            else -> "томів"
        }
        return "$count $noun"
    }
}
