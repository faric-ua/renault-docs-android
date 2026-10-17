package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * v0.5.150: reads an open («Усі, хто має посилання») Google Drive file without signing in.
 * A closed or deleted file comes back as an HTML page — the first bytes are checked and the
 * screen gets a plain sentence instead of garbage. (Ported from Track Catcher `DriveHttp`.)
 */
object AppUpdateHttp {
    class Result(
        val size: Long,
        val sha256: String,
    )

    const val NOT_PUBLIC =
        "Диск не віддав файл: він не відкритий для всіх, хто має посилання, видалений, " +
            "або посилання не те. Автору: у Google Диску → файл → «Надати доступ» → «Усі, хто має посилання»."

    fun readText(
        fileId: String,
        maxBytes: Int,
    ): String {
        val out = ByteArrayOutputStream()
        download(fileId, out, maxBytes.toLong(), expectApk = false, progress = null)
        return out.toString("UTF-8")
    }

    /** Writes the file into [out]. With [expectApk] the first bytes must be a ZIP («PK»). */
    fun download(
        fileId: String,
        out: OutputStream,
        maxBytes: Long,
        expectApk: Boolean,
        progress: ((done: Long, total: Long) -> Unit)?,
    ): Result {
        val c = URL(AppUpdate.downloadUrl(fileId)).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.connectTimeout = 20_000
        c.readTimeout = 60_000
        c.setRequestProperty("User-Agent", "RenaultDocs-Updater/1")
        c.setRequestProperty("Cache-Control", "no-cache")
        try {
            val code = c.responseCode
            if (code == 401 || code == 403 || code == 404) throw IOException(NOT_PUBLIC)
            if (code !in 200..299) throw IOException("Google Диск відповів кодом $code. Спробуй пізніше.")
            val total = c.contentLengthLong
            if (total > maxBytes) {
                if (!expectApk) throw AppUpdate.WrongFileException(AppUpdate.linkIsApkMessage(total))
                throw IOException("Файл на Диску завеликий (${AppUpdate.formatMb(total)}).")
            }
            val sha = AppUpdate.newSha256()
            return c.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                var checkedHead = false
                var lastReport = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    if (!checkedHead) {
                        checkedHead = true
                        if (AppUpdate.looksLikeHtml(c.contentType, buffer, n)) throw IOException(NOT_PUBLIC)
                        if (expectApk && !AppUpdate.looksLikeApk(buffer, n)) throw IOException("На Диску за цим посиланням не APK-файл.")
                        if (!expectApk && AppUpdate.looksLikeApk(buffer, n)) {
                            throw AppUpdate.WrongFileException(AppUpdate.linkIsApkMessage(total))
                        }
                    }
                    done += n
                    if (done > maxBytes) {
                        if (!expectApk) throw AppUpdate.WrongFileException(AppUpdate.linkIsApkMessage(-1))
                        throw IOException("Файл на Диску завеликий.")
                    }
                    sha.update(buffer, 0, n)
                    out.write(buffer, 0, n)
                    if (progress != null && done - lastReport >= 256 * 1024) {
                        lastReport = done
                        progress(done, total)
                    }
                }
                if (!checkedHead) throw IOException("Файл на Диску порожній.")
                progress?.invoke(done, total)
                Result(done, AppUpdate.sha256Hex(sha.digest()))
            }
        } finally {
            c.disconnect()
        }
    }
}
