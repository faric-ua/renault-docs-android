package com.saney.renaultdocs

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v0.5.150: on/off for updating from Google Drive. A Google Play build must turn it off
 * (`DRIVE_UPDATES` in build.gradle.kts) and drop REQUEST_INSTALL_PACKAGES — Play forbids apps
 * that update themselves outside Play.
 */
object AppUpdateSwitch {
    val ENABLED: Boolean get() = BuildConfig.DRIVE_UPDATES
}

/**
 * State that survives rotation and moving between screens: the work runs on a background thread,
 * a window (new after rotation) only shows the current text. One update action at a time.
 */
object AppUpdateState {
    @Volatile var busy = false
    var openKey: String? = null
    var progressTitle = ""
    var progressText = ""
    var progressFinished = false
    var verdict: AppUpdate.Manifest? = null
    var problem: String? = null
    var problemOffersLink = false
    var pendingApk: File? = null
    internal var dialog: AlertDialog? = null
    internal var progressView: TextView? = null
    internal var progressButton: TextView? = null

    private var host: Activity? = null
    private val pending = ArrayList<(Activity) -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun attach(activity: Activity) {
        host = activity
        val todo = ArrayList(pending)
        pending.clear()
        todo.forEach { it(activity) }
    }

    fun detach(activity: Activity) {
        if (host === activity) host = null
        progressView = null
        progressButton = null
    }

    /** The screen goes away (rotation too): its window is closed; [openKey] keeps it for the new screen. */
    fun onHostDestroy(activity: Activity) {
        val d = dialog ?: return
        if (d.ownerActivity === activity) {
            runCatching { d.dismiss() }
            dialog = null
        }
    }

    /** Run on the main thread on the visible screen; hidden — when the app is opened again. */
    fun onMain(action: (Activity) -> Unit) {
        main.post {
            val h = host
            if (h != null && !h.isFinishing && !h.isDestroyed) action(h) else pending += action
        }
    }

    fun setProgress(
        text: String,
        finished: Boolean = false,
    ) {
        main.post {
            progressText = text
            progressFinished = finished
            progressView?.text = text
            progressButton?.text = if (finished) "Закрити" else "Сховати (робота йде далі)"
        }
    }
}

/**
 * v0.5.150 (owner 2026-10-17 15:49–15:50): «Оновлення застосунку» in Settings → «Про програму».
 * For everyone: «Перевірити оновлення» (from the author's Google Drive) with what's new and
 * «Детальніше». For the author: «Опублікувати цю версію на Диск». MainActivity checks quietly
 * once a day and speaks only when there is a newer version.
 * Ported from Track Catcher `UpdateUi` (phone PASS), in Renault Docs dialogs ([DialogUi]).
 */
class AppUpdateUi(
    private val host: Activity,
) {
    private val prefs: SharedPreferences = host.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dp(v: Int) = Ui.dp(host, v)

    private fun toast(text: String) = Toast.makeText(host, text, Toast.LENGTH_SHORT).show()

    // ---------- card in Settings ----------

    fun card(): View =
        LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.roundedBackground(host, Ui.surface, Ui.accent)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(
                Ui.textView(host, "Оновлення застосунку", 17f, Ui.entityTitle).apply {
                    setTypeface(typeface, Typeface.BOLD)
                },
            )
            addView(
                Ui.textView(
                    host,
                    "Зараз: v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}). " +
                        if (manifestId() == null) {
                            "Посилання на оновлення ще не задано — натисни «Перевірити оновлення» й встав посилання від автора."
                        } else {
                            "Нові версії беруться з Google Диска автора. Перед оновленням видно, що змінилось. Проєкти й томи лишаються."
                        },
                    14f,
                    Ui.muted,
                ).apply { setPadding(0, dp(6), 0, dp(10)) },
            )
            addView(cardButton("Перевірити оновлення", Ui.accent) { check(quiet = false) })
            if (manifestId() != null) {
                addView(cardButton("Інше посилання на оновлення", Ui.muted) { askForLink() })
            }
            fun autoLabel() =
                if (prefs.getBoolean(KEY_AUTO, true)) {
                    "Перевіряти самому раз на день: так ✓"
                } else {
                    "Перевіряти самому раз на день: ні"
                }
            lateinit var autoButton: TextView
            autoButton =
                cardButton(autoLabel(), Ui.muted) {
                    val on = !prefs.getBoolean(KEY_AUTO, true)
                    prefs.edit().putBoolean(KEY_AUTO, on).apply()
                    autoButton.text = autoLabel()
                    toast(if (on) "Перевірятиму раз на день — скажу лише про нову версію" else "Сам не перевірятиму")
                }
            addView(autoButton)
            addView(cardButton("Опублікувати цю версію на Диск (автор)", Ui.muted) { showPublishSheet() })
        }

    private fun cardButton(
        label: String,
        stroke: Int,
        action: () -> Unit,
    ): TextView =
        Ui.textView(host, label, 16f, if (stroke == Ui.accent) Ui.accent else Ui.text).apply {
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            minHeight = dp(48)
            background = Ui.roundedBackground(host, Ui.surfaceAlt, stroke, 11)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { action() }
            layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(8)
                }
        }

    // ---------- dialogs (one at a time, restored after rotation) ----------

    private fun show(
        key: String,
        dialog: AlertDialog,
        role: DialogRole,
    ) {
        AppUpdateState.openKey = key
        AppUpdateState.dialog?.takeIf { it !== dialog && it.ownerActivity === host }?.let { old ->
            old.setOnDismissListener(null)
            runCatching { old.dismiss() }
        }
        dialog.setOwnerActivity(host)
        AppUpdateState.dialog = dialog
        dialog.setOnDismissListener {
            if (AppUpdateState.dialog === dialog) AppUpdateState.dialog = null
            if (!host.isChangingConfigurations && AppUpdateState.openKey == key) AppUpdateState.openKey = null
            if (key == KEY_PROGRESS) {
                AppUpdateState.progressView = null
                AppUpdateState.progressButton = null
            }
        }
        dialog.show()
        DialogUi.apply(dialog, role)
    }

    /** Every update window is built here and shown by [show] (themed by [DialogUi]). */
    private fun builder() = AlertDialog.Builder(host)

    /** After rotation / coming back: the same window again. */
    fun restoreOpen() {
        when (AppUpdateState.openKey) {
            KEY_PUBLISH -> showPublishSheet()
            KEY_LINK -> askForLink()
            KEY_PROGRESS -> showProgress()
            KEY_VERDICT -> AppUpdateState.verdict?.let { showVerdict(it) }
            KEY_DETAILS -> AppUpdateState.verdict?.let { showDetails(it) }
            KEY_PROBLEM -> AppUpdateState.problem?.let { showProblem(it, AppUpdateState.problemOffersLink) }
            KEY_PERMISSION -> AppUpdateState.pendingApk?.let { install(it) }
            KEY_RESET -> confirmReset()
        }
    }

    private fun message(
        key: String,
        title: String,
        text: String,
        role: DialogRole,
        positive: String?,
        negative: String,
        neutral: String? = null,
        onNeutral: () -> Unit = {},
        onNegative: () -> Unit = {},
        onPositive: () -> Unit = {},
    ) {
        val b =
            builder()
                .setTitle(title)
                .setMessage(text)
                .setNegativeButton(negative) { _, _ ->
                    AppUpdateState.openKey = null
                    onNegative()
                }
        if (positive != null) {
            b.setPositiveButton(positive) { _, _ ->
                AppUpdateState.openKey = null
                onPositive()
            }
        }
        if (neutral != null) {
            b.setNeutralButton(neutral) { _, _ ->
                AppUpdateState.openKey = null
                onNeutral()
            }
        }
        show(key, b.create(), role)
    }

    // ---------- author: publishing ----------

    fun showPublishSheet() {
        val apkUri = prefs.getString(KEY_APK_URI, "").orEmpty()
        val jsonUri = prefs.getString(KEY_JSON_URI, "").orEmpty()
        val apkId = prefs.getString(KEY_APK_ID, "").orEmpty()
        val jsonId = prefs.getString(KEY_JSON_ID, "").orEmpty()
        val apkReady = apkUri.isNotBlank()
        val jsonReady = jsonUri.isNotBlank()
        val apkLink = apkId.isNotBlank()
        val jsonLink = jsonId.isNotBlank()

        lateinit var dialog: AlertDialog
        val box =
            LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(8), dp(18), dp(8))
            }
        box.addView(
            Ui.textView(
                host,
                "Ця версія: ${BuildConfig.VERSION_NAME} · збірка ${BuildConfig.VERSION_CODE}\n" +
                    "Один раз: два файли на Диску (кроки 1–2) і посилання на них (кроки 3–4). " +
                    "Далі після кожної нової версії — лише «Опублікувати зараз».",
                14f,
                Ui.muted,
            ),
        )

        fun step(
            label: String,
            enabled: Boolean,
            done: Boolean,
            action: () -> Unit,
        ) {
            box.addView(
                Ui.textView(host, label, 16f, if (done) Ui.success else if (enabled) Ui.accent else Ui.muted).apply {
                    minHeight = dp(48)
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = enabled
                    isEnabled = enabled
                    background = Ui.roundedBackground(host, Ui.surfaceAlt, if (enabled) Ui.accent else Ui.border, 11)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    setOnClickListener {
                        AppUpdateState.openKey = null
                        dialog.dismiss()
                        action()
                    }
                    layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                },
            )
        }
        step(
            if (apkReady) "1. Файл APK — готово: ${prefs.getString(KEY_APK_NAME, "")} ✓" else "1. Створити файл APK на Диску",
            enabled = !apkReady,
            done = apkReady,
        ) { createFile(REQ_APK, "application/vnd.android.package-archive", AppUpdate.APK_FILE_NAME) }
        step(
            if (jsonReady) "2. Файл версії — готово: ${prefs.getString(KEY_JSON_NAME, "")} ✓" else "2. Створити файл версії на Диску",
            enabled = apkReady && !jsonReady,
            done = jsonReady,
        ) { createFile(REQ_JSON, "application/json", AppUpdate.VERSION_FILE_NAME) }
        step(
            if (apkLink) "3. Посилання на APK — вставлено ✓" else "3. Вставити посилання на APK",
            enabled = apkReady && jsonReady && !apkLink,
            done = apkLink,
        ) { if (pasteLink(KEY_APK_ID)) showPublishSheet() }
        step(
            if (jsonLink) "4. Посилання на файл версії — вставлено ✓" else "4. Вставити посилання на файл версії",
            enabled = apkReady && jsonReady && apkLink && !jsonLink,
            done = jsonLink,
        ) { if (pasteLink(KEY_JSON_ID)) showPublishSheet() }
        if (!(apkReady && jsonReady && apkLink && jsonLink)) {
            box.addView(
                Ui.textView(
                    host,
                    "Кроки 1–2: у вікні вибору натисни ☰ ліворуч → «Google Диск» → папка AppsAndroidSync → RenaultDocs → App → «Зберегти».\n" +
                        "Кроки 3–4: відкрий Google Диск → ⋮ біля файлу → «Надати доступ» → «Усі, хто має посилання» → " +
                        "«Копіювати посилання» → повернись сюди й натисни крок.",
                    13f,
                    Ui.muted,
                ).apply { setPadding(0, dp(10), 0, 0) },
            )
        }
        if (jsonLink && BUILT_IN_MANIFEST_ID.isNotBlank() && jsonId != BUILT_IN_MANIFEST_ID) {
            box.addView(
                Ui.textView(
                    host,
                    "Увага: файл версії не той, що вбудований у застосунок. Телефони без вставленого посилання цієї " +
                        "публікації не побачать — скажи про це в чаті Renault Docs.",
                    13.5f,
                    Ui.warning,
                ).apply { setPadding(0, dp(10), 0, 0) },
            )
        }
        val ready = apkReady && jsonReady && apkLink
        step("Опублікувати зараз", enabled = ready, done = false) { publish() }
        prefs.getString(KEY_LAST_RESULT, "").orEmpty().takeIf { it.isNotBlank() }?.let {
            box.addView(Ui.textView(host, it, 13.5f, Ui.muted).apply { setPadding(0, dp(8), 0, 0) })
        }
        if (jsonLink) step("Поділитися посиланням на оновлення", enabled = true, done = false) { shareLink(jsonId) }
        if (apkReady || jsonReady || apkLink || jsonLink) {
            step("Почати налаштування заново", enabled = true, done = false) { confirmReset() }
        }
        dialog =
            builder()
                .setTitle("Публікація версії на Диск")
                .setView(ScrollView(host).apply { addView(box) })
                .setNegativeButton("Закрити") { _, _ -> AppUpdateState.openKey = null }
                .create()
        show(KEY_PUBLISH, dialog, DialogRole.CHOICE)
    }

    @Suppress("DEPRECATION")
    private fun createFile(
        request: Int,
        mime: String,
        name: String,
    ) {
        val i =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mime)
                .putExtra(Intent.EXTRA_TITLE, name)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
        toast("У вікні вибору натисни ☰ ліворуч → «Google Диск».")
        try {
            host.startActivityForResult(i, request)
        } catch (e: ActivityNotFoundException) {
            toast("На цьому телефоні немає вікна створення файлу")
        }
    }

    /** After the file window: keep a lasting permission for the file and remember it. */
    fun onFilePicked(
        request: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || data == null || uri == null) {
            showPublishSheet()
            return
        }
        try {
            var flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (flags == 0) flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            host.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            showProblem("Android не дав постійного доступу до цього файлу. Створи файл саме через «Google Диск» у меню ☰ вікна вибору.", offerLink = false)
            return
        }
        val apk = request == REQ_APK
        var name = if (apk) AppUpdate.APK_FILE_NAME else AppUpdate.VERSION_FILE_NAME
        runCatching {
            host.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) name = c.getString(0)
            }
        }
        prefs.edit()
            .putString(if (apk) KEY_APK_URI else KEY_JSON_URI, uri.toString())
            .putString(if (apk) KEY_APK_NAME else KEY_JSON_NAME, name)
            .apply()
        if (!apk) {
            // The version file needs content so Drive can give a link to it.
            val resolver = host.contentResolver
            Thread({ runCatching { writeText(resolver, uri, AppUpdate.PLACEHOLDER) } }, "update-json-placeholder").start()
        }
        showPublishSheet()
    }

    /** Link from the clipboard → file ID; none — a window with the steps and null. */
    private fun linkFromClipboard(): String? {
        val text =
            runCatching {
                val clip = (host.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip
                if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(host)?.toString().orEmpty() else ""
            }.getOrDefault("")
        val id = AppUpdate.fileIdFromLink(text)
        if (id == null) {
            val shown = if (text.isBlank()) "" else "\n\nЗараз у буфері: «" + (if (text.length > 80) text.take(80) + "…" else text) + "»"
            showProblem(
                "У буфері немає посилання на файл Google Диска.\n\nВідкрий Google Диск → ⋮ біля файлу → «Надати доступ» → " +
                    "«Усі, хто має посилання» → «Копіювати посилання». Потім повернись сюди й натисни кнопку ще раз.$shown",
                offerLink = false,
            )
        }
        return id
    }

    /** Author steps 3–4. The same link in both steps is a mistake. */
    private fun pasteLink(key: String): Boolean {
        val id = linkFromClipboard() ?: return false
        val other = prefs.getString(if (key == KEY_APK_ID) KEY_JSON_ID else KEY_APK_ID, "").orEmpty()
        if (id == other) {
            showProblem(
                "Це те саме посилання, що вже вставлене в " + (if (key == KEY_APK_ID) "кроці 4" else "кроці 3") + ".\n\n" +
                    "Крок 3 — посилання на «${AppUpdate.APK_FILE_NAME}», крок 4 — на «${AppUpdate.VERSION_FILE_NAME}».",
                offerLink = false,
            )
            return false
        }
        val edit = prefs.edit().putString(key, id)
        if (key == KEY_JSON_ID) edit.putString(KEY_MANIFEST_ID, id)
        edit.apply()
        toast("Посилання вставлено")
        return true
    }

    private fun checkPastedLink() {
        val id = linkFromClipboard() ?: return
        toast("Посилання вставлено")
        check(quiet = false, pasted = id)
    }

    private fun shareLink(jsonId: String) {
        val send =
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                Intent.EXTRA_TEXT,
                "Оновлення Renault Docs: ${AppUpdate.viewUrl(jsonId)}\n" +
                    "Скопіюй це посилання, потім у Renault Docs: «Налаштування» → «Оновлення застосунку» → " +
                    "«Перевірити оновлення» → «Вставити посилання».",
            )
        try {
            host.startActivity(Intent.createChooser(send, "Поділитися посиланням"))
        } catch (e: ActivityNotFoundException) {
            toast("Немає застосунку, щоб поділитись")
        }
    }

    /** Asks first (owner's rule: any reset / delete asks). Nothing on Drive changes. */
    private fun confirmReset() {
        message(
            key = KEY_RESET,
            title = "Почати заново?",
            text =
                "Телефон забуде обидва файли й посилання. На Диску нічого не зміниться — старі файли можна вибрати знову " +
                    "або прибрати вручну в Google Диску.",
            role = DialogRole.DANGER,
            positive = "Почати заново",
            negative = "Скасувати",
            onNegative = { showPublishSheet() },
        ) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            listOf(KEY_APK_URI, KEY_JSON_URI).mapNotNull { prefs.getString(it, "")?.takeIf { s -> s.isNotBlank() } }.forEach { u ->
                runCatching { host.contentResolver.releasePersistableUriPermission(Uri.parse(u), flags) }
            }
            val keepManifest = prefs.getString(KEY_MANIFEST_ID, "")
            val keepAuto = prefs.getBoolean(KEY_AUTO, true)
            prefs.edit().clear().putString(KEY_MANIFEST_ID, keepManifest).putBoolean(KEY_AUTO, keepAuto).apply()
            showPublishSheet()
        }
    }

    private fun publish() {
        if (AppUpdateState.busy) {
            toast("Уже працюю…")
            return
        }
        val apkUri = prefs.getString(KEY_APK_URI, "").orEmpty()
        val jsonUri = prefs.getString(KEY_JSON_URI, "").orEmpty()
        val apkId = prefs.getString(KEY_APK_ID, "").orEmpty()
        if (apkUri.isBlank() || jsonUri.isBlank() || apkId.isBlank()) {
            showPublishSheet()
            return
        }
        val info = host.applicationInfo
        if (!info.splitSourceDirs.isNullOrEmpty()) {
            showProblem("Ця версія встановлена кількома файлами (split APK) — її не передати одним APK. Встав версію з меню Renault Docs і спробуй ще раз.", offerLink = false)
            return
        }
        val source = File(info.sourceDir)
        val resolver = host.contentResolver
        val pkg = host.packageName
        val whatsNew = readWhatsNew(host)
        startProgress("Публікую версію на Диск", "Копіюю APK на Диск…")
        Thread({
            var ok = false
            val outcome =
                try {
                    val total = source.length()
                    val sha = AppUpdate.newSha256()
                    var done = 0L
                    val raw =
                        runCatching { resolver.openOutputStream(Uri.parse(apkUri), "wt") }.getOrNull()
                            ?: resolver.openOutputStream(Uri.parse(apkUri), "w")
                            ?: throw IOException("файл APK на Диску не відкрився для запису")
                    raw.use { out ->
                        FileInputStream(source).use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var lastShown = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                out.write(buffer, 0, n)
                                sha.update(buffer, 0, n)
                                done += n
                                if (done - lastShown > 1024 * 1024 || done == total) {
                                    lastShown = done
                                    AppUpdateState.setProgress("Копіюю APK на Диск… ${AppUpdate.formatMb(done)} з ${AppUpdate.formatMb(total)}")
                                }
                            }
                        }
                    }
                    val manifest =
                        AppUpdate.Manifest(
                            packageName = pkg,
                            versionCode = BuildConfig.VERSION_CODE.toLong(),
                            versionName = BuildConfig.VERSION_NAME,
                            apkSha256 = AppUpdate.sha256Hex(sha.digest()),
                            apkSize = done,
                            apkId = apkId,
                            publishedAt = System.currentTimeMillis(),
                            changes = WhatsNew.forVersion(whatsNew, BuildConfig.VERSION_NAME),
                            details = WhatsNew.recent(whatsNew, DETAILS_VERSIONS),
                        )
                    AppUpdateState.setProgress("Записую файл версії…")
                    writeText(resolver, Uri.parse(jsonUri), manifest.toJson())
                    ok = true
                    "Опубліковано ${BuildConfig.VERSION_NAME} (${AppUpdate.formatMb(done)}) · " +
                        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date()) +
                        "\nGoogle Диск вивантажує файл у фоні — інші телефони побачать його за кілька хвилин."
                } catch (e: SecurityException) {
                    "Доступ до файлу на Диску втрачено — «Почати налаштування заново»."
                } catch (e: Exception) {
                    "Не вдалося: " + (e.message ?: "невідома помилка")
                }
            prefs.edit().putString(KEY_LAST_RESULT, outcome).apply()
            AppUpdateState.busy = false
            AppUpdateState.setProgress(outcome, finished = true)
            AppUpdateState.onMain {
                Toast.makeText(it, if (ok) "Опубліковано на Диск" else "Публікація не вдалася", Toast.LENGTH_SHORT).show()
            }
        }, "update-publish").start()
    }

    // ---------- every phone: check, download, install ----------

    fun manifestId(): String? =
        prefs.getString(KEY_MANIFEST_ID, "")?.takeIf { it.isNotBlank() } ?: BUILT_IN_MANIFEST_ID.takeIf { it.isNotBlank() }

    /** Opening the app: at most once in 24 h; silent unless there is a newer version. */
    fun autoCheck() {
        if (!AppUpdateSwitch.ENABLED) return
        if (manifestId() == null || !prefs.getBoolean(KEY_AUTO, true)) return
        val last = prefs.getLong(KEY_LAST_AUTO, 0L)
        if (System.currentTimeMillis() - last < AUTO_CHECK_MS) return
        prefs.edit().putLong(KEY_LAST_AUTO, System.currentTimeMillis()).apply()
        check(quiet = true)
    }

    fun check(
        quiet: Boolean,
        pasted: String? = null,
    ) {
        val saved = prefs.getString(KEY_MANIFEST_ID, "")?.takeIf { it.isNotBlank() }
        val candidates = AppUpdate.manifestCandidates(pasted, saved, BUILT_IN_MANIFEST_ID)
        if (candidates.isEmpty()) {
            if (!quiet) askForLink()
            return
        }
        if (AppUpdateState.busy) {
            if (!quiet) toast("Уже працюю…")
            return
        }
        AppUpdateState.busy = true
        if (!quiet && pasted == null) toast("Перевіряю оновлення…")
        val pkg = host.packageName
        val store = prefs
        Thread({
            var manifest: AppUpdate.Manifest? = null
            var problem: String? = null
            val read = ArrayList<Pair<String, AppUpdate.Manifest>>()
            for (id in candidates) {
                try {
                    read += id to AppUpdate.Manifest.parse(AppUpdateHttp.readText(id, 64 * 1024))
                } catch (e: AppUpdate.WrongFileException) {
                    problem = e.message
                    if (pasted == null && id == saved) store.edit().remove(KEY_MANIFEST_ID).apply()
                } catch (e: IllegalArgumentException) {
                    problem = e.message
                } catch (e: Exception) {
                    problem = e.message ?: "Немає зв'язку з Google Диском."
                }
            }
            AppUpdate.newest(read)?.let { (id, m) ->
                manifest = m
                problem = null
                if (pasted != null) {
                    store.edit().putString(KEY_MANIFEST_ID, id).apply()
                } else if (saved != null && id != saved) {
                    store.edit().remove(KEY_MANIFEST_ID).apply()
                }
            }
            AppUpdateState.busy = false
            val found = manifest
            if (quiet) {
                if (found != null && AppUpdate.compare(found, pkg, BuildConfig.VERSION_CODE.toLong()) == AppUpdate.Verdict.NEWER) {
                    AppUpdateState.onMain { AppUpdateUi(it).showVerdict(found) }
                }
                return@Thread
            }
            AppUpdateState.onMain { h ->
                if (found == null) {
                    AppUpdateUi(h).showProblem(problem ?: "Невідома помилка.", offerLink = true)
                } else {
                    AppUpdateUi(h).showVerdict(found)
                }
            }
        }, "update-check").start()
    }

    private fun askForLink() {
        message(
            key = KEY_LINK,
            title = "Звідки брати оновлення?",
            text =
                "Потрібне посилання на файл оновлень на Google Диску автора. Скопіюй його (автор надсилає посилання) " +
                    "і натисни «Вставити посилання».",
            role = DialogRole.CHOICE,
            positive = "Вставити посилання",
            negative = "Скасувати",
        ) { checkPastedLink() }
    }

    fun showProblem(
        text: String,
        offerLink: Boolean,
    ) {
        AppUpdateState.problem = text
        AppUpdateState.problemOffersLink = offerLink
        message(
            key = KEY_PROBLEM,
            title = "Оновлення: не вдалося",
            text = text + if (offerLink) "\n\nБез інтернету перевірка не працює." else "",
            role = DialogRole.HELP,
            positive = if (offerLink) "Інше посилання" else null,
            negative = "Закрити",
            onNegative = { AppUpdateState.problem = null },
        ) {
            AppUpdateState.problem = null
            checkPastedLink()
        }
    }

    /**
     * Owner 15:50: «є нова версія, і внизу коротко описано, що змінилося, і посилання… більше
     * інформації… щоб людина знала, чи вона готова оновлювати».
     */
    fun showVerdict(m: AppUpdate.Manifest) {
        AppUpdateState.verdict = m
        val mine = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val theirs = "${m.versionName} (${m.versionCode})"
        when (AppUpdate.compare(m, host.packageName, BuildConfig.VERSION_CODE.toLong())) {
            AppUpdate.Verdict.NEWER ->
                message(
                    key = KEY_VERDICT,
                    title = "Є нова версія ${m.versionName}",
                    text = AppUpdate.newerMessage(m, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong()),
                    role = DialogRole.CONFIRM,
                    positive = "Оновити",
                    negative = "Не зараз",
                    neutral = if (m.details.isNotBlank()) "Детальніше" else null,
                    onNeutral = { showDetails(m) },
                    onNegative = { AppUpdateState.verdict = null },
                ) {
                    AppUpdateState.verdict = null
                    download(m)
                }
            AppUpdate.Verdict.SAME ->
                message(
                    key = KEY_VERDICT,
                    title = "У тебе остання версія",
                    text = "Встановлено $mine — така сама, як на Диску.$OTHER_LINK_HINT",
                    role = DialogRole.HELP,
                    positive = "Інше посилання",
                    negative = "Закрити",
                    onNegative = { AppUpdateState.verdict = null },
                ) { checkPastedLink() }
            AppUpdate.Verdict.OLDER ->
                message(
                    key = KEY_VERDICT,
                    title = "У тебе новіша версія",
                    text = "Встановлено $mine, на Диску старіша: $theirs.$OTHER_LINK_HINT",
                    role = DialogRole.HELP,
                    positive = "Інше посилання",
                    negative = "Закрити",
                    onNegative = { AppUpdateState.verdict = null },
                ) { checkPastedLink() }
            AppUpdate.Verdict.OTHER_PACKAGE ->
                message(
                    key = KEY_VERDICT,
                    title = "Інший варіант застосунку",
                    text = "На Диску версія для ${m.packageName}, а в тебе ${host.packageName}.",
                    role = DialogRole.HELP,
                    positive = "Інше посилання",
                    negative = "Закрити",
                    onNegative = { AppUpdateState.verdict = null },
                ) { checkPastedLink() }
        }
    }

    /** «Детальніше»: what changed in the last versions; back to the same «є нова версія». */
    private fun showDetails(m: AppUpdate.Manifest) {
        AppUpdateState.verdict = m
        val text =
            Ui.textView(host, m.details.ifBlank { m.changes }.ifBlank { "Автор не описав змін." }, 15f, Ui.text).apply {
                setPadding(dp(20), dp(8), dp(20), dp(8))
                setTextIsSelectable(true)
            }
        val dialog =
            builder()
                .setTitle("Що нового · ${m.versionName}")
                .setView(ScrollView(host).apply { addView(text) })
                .setPositiveButton("Назад") { _, _ ->
                    AppUpdateState.openKey = null
                    showVerdict(m)
                }
                .create()
        show(KEY_DETAILS, dialog, DialogRole.HELP)
    }

    private fun download(m: AppUpdate.Manifest) {
        if (AppUpdateState.busy) return
        val dir = File(host.cacheDir, UPDATE_DIR)
        val pm = host.packageManager
        val installedCert = installedCertSha256()
        startProgress("Завантажую оновлення ${m.versionName}", "Починаю…")
        Thread({
            val file = File(dir, "renault-docs-update.apk")
            val problem: String? =
                try {
                    dir.mkdirs()
                    // Only this folder of the app's own cache (an older downloaded update).
                    dir.listFiles()?.forEach { it.delete() }
                    val result =
                        FileOutputStream(file).use { out ->
                            AppUpdateHttp.download(m.apkId, out, MAX_APK_BYTES, expectApk = true) { done, total ->
                                AppUpdateState.setProgress(
                                    "Завантажено ${AppUpdate.formatMb(done)} з ${AppUpdate.formatMb(if (total > 0) total else m.apkSize)}",
                                )
                            }
                        }
                    AppUpdateState.setProgress("Перевіряю файл…")
                    val archive = archiveInfo(pm, file)
                    AppUpdate.checkDownloaded(
                        m,
                        result.sha256,
                        result.size,
                        archive?.packageName,
                        archive?.let { versionCodeOf(it) } ?: -1L,
                        archive?.let { certSha256(it) },
                        installedCert,
                    )
                } catch (e: Exception) {
                    e.message ?: "Завантаження обірвалось."
                }
            AppUpdateState.busy = false
            if (problem != null) {
                file.delete()
                AppUpdateState.setProgress(problem, finished = true)
            } else {
                AppUpdateState.setProgress("Файл перевірено: розмір, контрольна сума, версія й ключ підпису збіглися.", finished = true)
                AppUpdateState.pendingApk = file
                AppUpdateState.onMain { h ->
                    if (AppUpdateState.openKey == KEY_PROGRESS) AppUpdateState.openKey = null
                    AppUpdateUi(h).install(file)
                }
            }
        }, "update-download").start()
    }

    fun install(file: File) {
        if (!file.isFile) {
            AppUpdateState.pendingApk = null
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !host.packageManager.canRequestPackageInstalls()) {
            AppUpdateState.pendingApk = file
            message(
                key = KEY_PERMISSION,
                title = "Один дозвіл — один раз",
                text =
                    "Файл перевірено. Щоб Android дозволив Renault Docs встановити оновлення, на наступному екрані увімкни " +
                        "«Дозволити з цього джерела» й натисни «Назад» — встановлення почнеться саме.",
                role = DialogRole.CONFIRM,
                positive = "Відкрити дозвіл",
                negative = "Скасувати",
                onNegative = { AppUpdateState.pendingApk = null },
            ) { openInstallPermission() }
            return
        }
        AppUpdateState.pendingApk = null
        try {
            val uri = FileProvider.getUriForFile(host, host.packageName + ".files", file)
            val i =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            host.startActivity(i)
        } catch (e: Exception) {
            showProblem("Не вдалося відкрити встановлення: " + (e.message ?: "невідома помилка"), offerLink = false)
        }
    }

    private fun openInstallPermission() {
        try {
            host.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + host.packageName)),
            )
        } catch (e: Exception) {
            runCatching {
                host.startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + host.packageName)),
                )
            }
        }
    }

    /** Back from «Дозволити з цього джерела» — installing goes on by itself. */
    fun resumePendingInstall() {
        val file = AppUpdateState.pendingApk ?: return
        if (AppUpdateState.openKey == KEY_PERMISSION) return
        if (!file.isFile) {
            AppUpdateState.pendingApk = null
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || host.packageManager.canRequestPackageInstalls()) install(file)
    }

    private fun startProgress(
        title: String,
        text: String,
    ) {
        AppUpdateState.busy = true
        AppUpdateState.progressTitle = title
        AppUpdateState.progressText = text
        AppUpdateState.progressFinished = false
        showProgress()
    }

    private fun showProgress() {
        val status =
            Ui.textView(host, AppUpdateState.progressText, 15f, Ui.muted).apply {
                setPadding(dp(20), dp(8), dp(20), dp(8))
            }
        val dialog =
            builder()
                .setTitle(AppUpdateState.progressTitle)
                .setView(status)
                .setPositiveButton(if (AppUpdateState.progressFinished) "Закрити" else "Сховати (робота йде далі)") { _, _ ->
                    AppUpdateState.openKey = null
                }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        show(KEY_PROGRESS, dialog, DialogRole.HELP)
        AppUpdateState.progressView = status
        AppUpdateState.progressButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    }

    private fun installedCertSha256(): String? =
        runCatching {
            @Suppress("DEPRECATION")
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            certSha256(host.packageManager.getPackageInfo(host.packageName, flags))
        }.getOrNull()

    companion object {
        /**
         * The version file on the author's Drive built into the app, so other phones check
         * without pasting a link: `AppsAndroidSync/RenaultDocs/App/renault-docs-onovlennia.json`
         * (created by the author's phone 2026-10-17 16:08, «Усі, хто має посилання»; first
         * publication — 0.5.150 / 166). If the author creates the file again, the publish window warns.
         */
        const val BUILT_IN_MANIFEST_ID = "1eCbsQzdHb0LclRL64nDjGCU3ICWELNC0"

        private const val OTHER_LINK_HINT =
            "\n\nАвтор казав, що є новіша? Скопіюй посилання від нього й натисни «Інше посилання»."

        const val REQ_APK = 4501
        const val REQ_JSON = 4502
        const val UPDATE_DIR = "app-update"
        private const val DETAILS_VERSIONS = 8
        private const val MAX_APK_BYTES = 200L * 1024 * 1024
        private const val AUTO_CHECK_MS = 24L * 60 * 60 * 1000
        private const val PREFS = "app_update"

        private const val KEY_APK_URI = "apk_uri"
        private const val KEY_APK_NAME = "apk_name"
        private const val KEY_JSON_URI = "json_uri"
        private const val KEY_JSON_NAME = "json_name"
        private const val KEY_APK_ID = "apk_id"
        private const val KEY_JSON_ID = "json_id"
        private const val KEY_MANIFEST_ID = "manifest_id"
        private const val KEY_LAST_RESULT = "last_publish_result"
        private const val KEY_AUTO = "auto_check"
        private const val KEY_LAST_AUTO = "last_auto_check"

        const val KEY_PUBLISH = "update:publish"
        const val KEY_RESET = "update:reset"
        const val KEY_LINK = "update:link"
        const val KEY_PROGRESS = "update:progress"
        const val KEY_VERDICT = "update:verdict"
        const val KEY_DETAILS = "update:details"
        const val KEY_PROBLEM = "update:problem"
        const val KEY_PERMISSION = "update:permission"

        fun isRequest(requestCode: Int): Boolean = requestCode == REQ_APK || requestCode == REQ_JSON

        fun writeText(
            resolver: android.content.ContentResolver,
            uri: Uri,
            text: String,
        ) {
            val out =
                runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
                    ?: resolver.openOutputStream(uri, "w")
                    ?: throw IOException("файл версії на Диску не відкрився для запису")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }

        /** «Що нового» built into this APK (assets/whats_new.txt), or "". */
        fun readWhatsNew(context: Context): String =
            runCatching {
                context.assets.open("whats_new.txt").use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrDefault("")

        @Suppress("DEPRECATION")
        private fun archiveInfo(
            pm: PackageManager,
            file: File,
        ): PackageInfo? =
            runCatching {
                val flags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
                pm.getPackageArchiveInfo(file.absolutePath, flags)
            }.getOrNull()

        @Suppress("DEPRECATION")
        private fun versionCodeOf(info: PackageInfo): Long =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

        @Suppress("DEPRECATION")
        private fun certSha256(info: PackageInfo): String? =
            runCatching {
                val signer =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        info.signingInfo?.apkContentsSigners?.firstOrNull()
                    } else {
                        info.signatures?.firstOrNull()
                    }
                signer?.let { AppUpdate.sha256Hex(AppUpdate.newSha256().digest(it.toByteArray())) }
            }.getOrNull()
    }
}
