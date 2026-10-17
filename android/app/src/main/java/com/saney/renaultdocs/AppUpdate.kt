package com.saney.renaultdocs

import java.security.MessageDigest
import java.util.Locale

/**
 * v0.5.150 (owner 2026-10-17 15:49–15:50): update Renault Docs from Google Drive —
 * «перевірити оновлення → вікно: є нова версія, коротко що змінилося, «Детальніше» →
 * людина сама вирішує, оновлювати чи ні».
 *
 * Ported from Track Catcher `DriveUpdate` (phone PASS there; itself from Dorama T88), same
 * files and checks, plus what's new: `changes` (this version, a few lines) and `details`
 * (the last versions, for «Детальніше»).
 *
 * The author's phone creates three things on its own Google Drive once (Android file window,
 * no Google sign-in in the app) and from then on always writes into the same files, so the
 * links never change: the APK and a small version file, both «Усі, хто має посилання». Any
 * phone reads the version file without sign-in, compares versionCode, shows what's new,
 * downloads the APK, checks size, SHA-256, package, version and signing key — and only then
 * opens Android's «Оновити цей застосунок?».
 *
 * Google Play forbids apps that update themselves outside Play — a Play build must have
 * [AppUpdateSwitch.ENABLED] = false (BuildConfig.DRIVE_UPDATES) and no REQUEST_INSTALL_PACKAGES.
 *
 * Plain Kotlin (no Android) — tested on the JVM. Own tiny JSON: org.json is a stub in JVM tests.
 */
object AppUpdate {
    const val FORMAT = "renault-docs-update/1"
    const val APK_FILE_NAME = "Renodocs.apk"
    const val VERSION_FILE_NAME = "renodocs-onovlennia.json"

    /** Short «what changed» is cut to this many characters (the window stays small). */
    const val MAX_CHANGES = 600

    /** «Детальніше» keeps this many characters (the version file is read up to 64 KB). */
    const val MAX_DETAILS = 24_000

    class WrongFileException(message: String) : IllegalArgumentException(message)

    const val LINK_HINT =
        "Потрібне посилання саме на маленький файл версії «$VERSION_FILE_NAME», не на APK. " +
            "Найпростіше: на телефоні автора «Опублікувати цю версію на Диск» → «Поділитися посиланням на оновлення»."

    fun linkIsApkMessage(size: Long): String =
        "За цим посиланням — сам застосунок (APK" + (if (size > 0) ", ${formatMb(size)}" else "") +
            "), а не файл версії.\n\n$LINK_HINT"

    /** From the version files read — the one with the highest versionCode (first on a tie). */
    fun newest(read: List<Pair<String, Manifest>>): Pair<String, Manifest>? =
        read.fold(null as Pair<String, Manifest>?) { best, cur ->
            if (best == null || cur.second.versionCode > best.second.versionCode) cur else best
        }

    /** Pasted now — only it; otherwise the saved one, then the one built into the app. */
    fun manifestCandidates(
        pasted: String?,
        saved: String?,
        builtIn: String,
    ): List<String> =
        if (!pasted.isNullOrBlank()) {
            listOf(pasted)
        } else {
            listOfNotNull(saved?.takeIf { it.isNotBlank() }, builtIn.takeIf { it.isNotBlank() }).distinct()
        }

    private val BARE_ID = Regex("^[A-Za-z0-9_-]{20,80}$")
    private val PATH_ID = Regex("/(?:file/)?d/([A-Za-z0-9_-]{20,80})")
    private val QUERY_ID = Regex("[?&]id=([A-Za-z0-9_-]{20,80})")
    private val SHA256 = Regex("^[0-9a-f]{64}$")

    /**
     * What Google Drive's «Копіювати посилання» gives (`…/file/d/ID/view?usp=sharing`), the old
     * `open?id=ID` / `uc?id=ID`, or a bare ID. A folder link — null on purpose (a folder cannot be
     * read without signing in).
     */
    fun fileIdFromLink(text: String?): String? {
        val value = text?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (BARE_ID.matches(value)) return value
        val lower = value.lowercase(Locale.ROOT)
        if (!lower.contains("google.com")) return null
        if (lower.contains("/folders/")) return null
        PATH_ID.find(value)?.let { return it.groupValues[1] }
        QUERY_ID.find(value)?.let { return it.groupValues[1] }
        return null
    }

    /** Direct download of an open file; `confirm=t` skips «can't scan for viruses». */
    fun downloadUrl(fileId: String): String = "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"

    fun viewUrl(fileId: String): String = "https://drive.google.com/file/d/$fileId/view"

    /** A closed or deleted file comes back as an HTML sign-in / error page. */
    fun looksLikeHtml(
        contentType: String?,
        head: ByteArray?,
        length: Int,
    ): Boolean {
        if (contentType != null && contentType.lowercase(Locale.ROOT).contains("text/html")) return true
        if (head == null) return false
        val end = minOf(length, head.size)
        var start = 0
        while (start < end && head[start].toInt().toChar().isWhitespace()) start++
        if (start >= end) return false
        val prefix = String(head, start, minOf(15, end - start), Charsets.US_ASCII).lowercase(Locale.ROOT)
        return prefix.startsWith("<!doctype") || prefix.startsWith("<html")
    }

    /** An APK is a ZIP: starts with «PK». */
    fun looksLikeApk(
        head: ByteArray?,
        length: Int,
    ): Boolean = head != null && length >= 2 && head.size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()

    fun sha256Hex(digest: ByteArray): String = digest.joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    fun newSha256(): MessageDigest = MessageDigest.getInstance("SHA-256")

    fun formatMb(bytes: Long): String = String.format(Locale.ROOT, "%.1f МБ", bytes / (1024.0 * 1024.0)).replace('.', ',')

    /** The version file the author's phone writes. */
    data class Manifest(
        val packageName: String,
        val versionCode: Long,
        val versionName: String,
        val apkSha256: String,
        val apkSize: Long,
        val apkId: String,
        val publishedAt: Long,
        /** What changed in this version — a few lines (may be empty). */
        val changes: String = "",
        /** «Детальніше»: the last versions, newest first (may be empty). */
        val details: String = "",
    ) {
        fun toJson(): String =
            buildString {
                append("{\n")
                val fields =
                    listOf(
                        "format" to FORMAT,
                        "package" to packageName,
                        "versionCode" to versionCode,
                        "versionName" to versionName,
                        "apkSha256" to apkSha256,
                        "apkSize" to apkSize,
                        "apkId" to apkId,
                        "apkLink" to viewUrl(apkId),
                        "publishedAt" to publishedAt,
                        "changes" to changes.take(MAX_CHANGES),
                        "details" to details.take(MAX_DETAILS),
                    )
                fields.forEachIndexed { i, (k, v) ->
                    append("  ").append(MiniJson.quote(k)).append(": ")
                    append(if (v is String) MiniJson.quote(v) else v.toString())
                    append(if (i == fields.lastIndex) "\n" else ",\n")
                }
                append("}\n")
            }

        companion object {
            /** Reads a version file. Not ours / incomplete — an exception with a sentence for the screen. */
            fun parse(text: String?): Manifest {
                val json =
                    try {
                        MiniJson.parseFlat(text.orEmpty())
                    } catch (e: IllegalArgumentException) {
                        throw WrongFileException("За цим посиланням — не файл версії Renodocs.\n\n$LINK_HINT")
                    }
                if (json["format"] != FORMAT) {
                    if (json["format"] == PLACEHOLDER_FORMAT) throw IllegalArgumentException("Автор ще не опублікував жодної версії.")
                    throw WrongFileException("Це не файл оновлень Renodocs (інший формат).\n\n$LINK_HINT")
                }
                val pkg = json["package"] as? String ?: ""
                val code = (json["versionCode"] as? Long) ?: -1L
                val name = json["versionName"] as? String ?: ""
                val sha = (json["apkSha256"] as? String ?: "").lowercase(Locale.ROOT)
                val size = (json["apkSize"] as? Long) ?: -1L
                val apkId = json["apkId"] as? String ?: ""
                if (pkg.isEmpty() || code <= 0 || !SHA256.matches(sha) || size <= 0 || !BARE_ID.matches(apkId)) {
                    throw IllegalArgumentException("Файл версії на Диску неповний — автор має опублікувати версію ще раз.")
                }
                return Manifest(
                    packageName = pkg,
                    versionCode = code,
                    versionName = name.ifEmpty { code.toString() },
                    apkSha256 = sha,
                    apkSize = size,
                    apkId = apkId,
                    publishedAt = (json["publishedAt"] as? Long) ?: 0L,
                    changes = (json["changes"] as? String).orEmpty().take(MAX_CHANGES),
                    details = (json["details"] as? String).orEmpty().take(MAX_DETAILS),
                )
            }
        }
    }

    const val PLACEHOLDER_FORMAT = "renault-docs-update/0"

    /** Until the author publishes — the version file needs content so Drive can give a link. */
    const val PLACEHOLDER = "{\"format\": \"$PLACEHOLDER_FORMAT\", \"note\": \"ще не опубліковано\"}\n"

    enum class Verdict { NEWER, SAME, OLDER, OTHER_PACKAGE }

    fun compare(
        m: Manifest,
        installedPackage: String,
        installedCode: Long,
    ): Verdict =
        when {
            m.packageName != installedPackage -> Verdict.OTHER_PACKAGE
            m.versionCode > installedCode -> Verdict.NEWER
            m.versionCode == installedCode -> Verdict.SAME
            else -> Verdict.OLDER
        }

    /** The «є нова версія» window text: versions, size, then what changed (if the author wrote it). */
    fun newerMessage(
        m: Manifest,
        installedName: String,
        installedCode: Long,
    ): String =
        buildString {
            append("На Диску: ").append(m.versionName).append(" (").append(m.versionCode).append(")\n")
            append("У тебе: ").append(installedName).append(" (").append(installedCode).append(")\n")
            append("Розмір: ").append(formatMb(m.apkSize))
            if (m.changes.isNotBlank()) {
                append("\n\nЩо змінилось:\n").append(m.changes.trim())
            }
            append("\n\nПроєкти, томи й налаштування лишаються. Оновлювати не обов'язково — можна «Не зараз».")
        }

    /**
     * The downloaded APK against the version file and the installed app. null — all matched;
     * otherwise a sentence for the screen.
     */
    fun checkDownloaded(
        m: Manifest,
        sha256: String,
        size: Long,
        apkPackage: String?,
        apkVersionCode: Long,
        apkCertSha256: String?,
        installedCertSha256: String?,
    ): String? {
        if (size != m.apkSize || !m.apkSha256.equals(sha256, ignoreCase = true)) {
            return "Завантажений файл не збігається з файлом версії (контрольна сума). " +
                "Можливо, автор саме зараз публікує нову версію — спробуй через кілька хвилин."
        }
        if (apkPackage == null) return "Android не зміг прочитати завантажений APK."
        if (apkPackage != m.packageName) return "Завантажений APK — для іншого застосунку ($apkPackage). Не встановлюю."
        if (apkVersionCode != m.versionCode) {
            return "Версія всередині APK ($apkVersionCode) не та, що у файлі версії (${m.versionCode}). Не встановлюю."
        }
        if (apkCertSha256 == null || installedCertSha256 == null) {
            return "Не вдалося звірити ключ підпису APK. Не встановлюю."
        }
        if (!apkCertSha256.equals(installedCertSha256, ignoreCase = true)) {
            return "APK підписаний іншим ключем, ніж встановлений застосунок. Не встановлюю — це може бути чужий файл."
        }
        return null
    }
}

/**
 * «Що нового» built into the app (`assets/whats_new.txt`), written by Claude with each release:
 *
 * ```
 * 0.5.150
 * • …
 * • …
 *
 * 0.5.149
 * • …
 * ```
 * A block starts with a line that is only a version name. The publishing phone puts its own
 * version's block into `changes` and the last few blocks into `details`.
 */
object WhatsNew {
    private val VERSION_LINE = Regex("^\\s*(\\d+(?:\\.\\d+)+)\\s*$")

    fun blocks(text: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var version: String? = null
        val body = StringBuilder()
        fun flush() {
            val v = version ?: return
            out += v to body.toString().trim()
        }
        for (line in text.lines()) {
            val m = VERSION_LINE.matchEntire(line)
            if (m != null) {
                flush()
                version = m.groupValues[1]
                body.setLength(0)
            } else if (version != null) {
                body.append(line).append('\n')
            }
        }
        flush()
        return out
    }

    /** This version's lines, or "" when the file has no block for it. */
    fun forVersion(
        text: String,
        versionName: String,
    ): String = blocks(text).firstOrNull { it.first == versionName }?.second.orEmpty()

    /** The last [count] blocks (newest first, as in the file) with their version lines. */
    fun recent(
        text: String,
        count: Int,
    ): String =
        blocks(text).take(count).joinToString("\n\n") { (v, body) -> if (body.isBlank()) v else "$v\n$body" }
}

/** A flat JSON object: strings, whole numbers, true/false/null. Anything nested — an error. */
internal object MiniJson {
    fun quote(s: String): String =
        buildString {
            append('"')
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
                }
            }
            append('"')
        }

    fun parseFlat(text: String): Map<String, Any?> {
        val p = Parser(text)
        p.ws()
        p.expect('{')
        val out = LinkedHashMap<String, Any?>()
        p.ws()
        if (p.peek() == '}') {
            p.i++
        } else {
            while (true) {
                p.ws()
                val key = p.string()
                p.ws()
                p.expect(':')
                p.ws()
                out[key] = p.value()
                p.ws()
                when (p.next()) {
                    ',' -> continue
                    '}' -> break
                    else -> throw IllegalArgumentException("json")
                }
            }
        }
        p.ws()
        if (p.i != text.length) throw IllegalArgumentException("json")
        return out
    }

    private class Parser(val s: String) {
        var i = 0

        fun peek(): Char = if (i < s.length) s[i] else throw IllegalArgumentException("json")

        fun next(): Char = peek().also { i++ }

        fun ws() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun expect(c: Char) {
            if (next() != c) throw IllegalArgumentException("json")
        }

        fun string(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                when (val c = next()) {
                    '"' -> return sb.toString()
                    '\\' ->
                        when (val e = next()) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw IllegalArgumentException("json")
                                sb.append(s.substring(i, i + 4).toIntOrNull(16)?.toChar() ?: throw IllegalArgumentException("json"))
                                i += 4
                            }
                            else -> throw IllegalArgumentException("json: \\$e")
                        }
                    else -> sb.append(c)
                }
            }
        }

        fun value(): Any? {
            val c = peek()
            return when {
                c == '"' -> string()
                c == '-' || c.isDigit() -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
                    val raw = s.substring(start, i)
                    raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: throw IllegalArgumentException("json")
                }
                s.startsWith("true", i) -> {
                    i += 4
                    true
                }
                s.startsWith("false", i) -> {
                    i += 5
                    false
                }
                s.startsWith("null", i) -> {
                    i += 4
                    null
                }
                else -> throw IllegalArgumentException("json")
            }
        }
    }
}
