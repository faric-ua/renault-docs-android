package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DriveCatalogClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    /** Drive answers these when a file is not open to this person. */
    private val CLOSED_CODES = setOf(401, 403, 404)

    /** How the last catalogue load reached Drive (status line «Каталог vN · …»). */
    @Volatile
    var lastVia: DriveVia? = null
        private set

    /**
     * 0.6.6 (#133 closed catalogue, option (а)): through the signed-in account first, the old
     * public link as a fallback while the owner's folder is still open. Background thread.
     */
    fun loadCatalog(
        context: Context,
    ): DriveCatalog {
        val (connection, via) =
            openFile(
                context,
                ExternalLinks.PROJECT_CATALOG_MANIFEST_FILE_ID,
            )

        return try {
            val raw =
                connection.inputStream
                    .bufferedReader(
                        Charsets.UTF_8,
                    )
                    .use {
                        it.readText()
                    }

            DriveCatalogParser.parse(
                raw,
            ).also {
                lastVia = via
            }
        } finally {
            connection.disconnect()
        }
    }

    fun downloadPackage(
        context: Context,
        driveFileId: String,
        destination: File,
        expectedBytes: Long,
        expectedSha256: String? = null,
        onProgress: (Long, Long) -> Unit,
    ) {
        // #162 stage 2 (v0.5.123): the exact bytes the catalog promises.
        val digest =
            java.security.MessageDigest.getInstance(
                "SHA-256",
            )
        destination.parentFile?.mkdirs()
        val temporary =
            File(
                destination.parentFile,
                destination.name + ".part",
            )
        temporary.delete()

        val (connection, _) =
            openFile(
                context,
                driveFileId,
            )

        try {
            val contentType =
                connection.contentType
                    .orEmpty()
                    .lowercase()

            require(
                !contentType.contains(
                    "text/html",
                )
            ) {
                "Google Drive повернув HTML замість .rdpkg."
            }

            val total =
                expectedBytes
                    .takeIf {
                        it > 0L
                    }
                    ?: connection.contentLengthLong
                        .takeIf {
                            it > 0L
                        }
                    ?: 0L

            BufferedInputStream(
                connection.inputStream,
            ).use { input ->
                FileOutputStream(
                    temporary,
                ).buffered().use { output ->
                    val buffer =
                        ByteArray(
                            1024 * 1024,
                        )
                    var done =
                        0L

                    while (true) {
                        val read =
                            input.read(
                                buffer,
                            )
                        if (read < 0) {
                            break
                        }
                        if (read == 0) {
                            continue
                        }

                        output.write(
                            buffer,
                            0,
                            read,
                        )
                        digest.update(
                            buffer,
                            0,
                            read,
                        )
                        done +=
                            read
                        onProgress(
                            done,
                            total,
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        require(
            temporary.isFile &&
                temporary.length() > 0L
        ) {
            "Завантажений .rdpkg порожній."
        }

        val actualSha =
            digest.digest().joinToString("") {
                "%02x".format(it)
            }
        val sizeOk =
            expectedBytes <= 0L ||
                temporary.length() == expectedBytes
        val shaOk =
            expectedSha256.isNullOrBlank() ||
                actualSha.equals(
                    expectedSha256.trim(),
                    ignoreCase = true,
                )
        if (!sizeOk || !shaOk) {
            // Only our own temporary download in the app cache is removed.
            temporary.delete()
            error(
                if (!sizeOk) {
                    "Розмір завантаженого .rdpkg не збігається з каталогом — том не встановлено."
                } else {
                    "Контрольна сума .rdpkg не збігається з каталогом — том не встановлено."
                }
            )
        }

        destination.delete()
        require(
            temporary.renameTo(
                destination,
            )
        ) {
            "Не вдалося завершити завантаження .rdpkg."
        }
    }

    /**
     * One Drive file. With a signed-in account and Google's permission — the Drive API
     * (`files/<id>?alt=media`, works for files the owner shared with this account and for
     * files open «to anyone with the link»). Otherwise, or if the account path is refused,
     * the old public link. If neither works — a plain message for the person.
     */
    private fun openFile(
        context: Context,
        fileId: String,
    ): Pair<HttpURLConnection, DriveVia> {
        var accountProblem: String? = null
        val token =
            DriveAccess.tokenSilently(
                context,
            )

        if (token != null) {
            val connection =
                connect(
                    driveApiUrl(
                        fileId,
                    ),
                    token,
                )
            val code =
                connection.responseCode
            if (code in 200..299) {
                return connection to DriveVia.ACCOUNT
            }
            connection.disconnect()
            accountProblem =
                when (code) {
                    401 -> {
                        DriveAccess.forget()
                        DriveAccessTexts.SESSION_ENDED
                    }
                    404 ->
                        DriveAccessTexts.noAccess(
                            GoogleAccountStore(context).current()?.email,
                        )
                    else ->
                        DriveAccessTexts.API_REFUSED + " (HTTP $code)"
                }
        }

        val connection =
            connect(
                ExternalLinks.driveDownloadUrl(
                    fileId,
                ),
                null,
            )
        val code =
            connection.responseCode
        val html =
            connection.contentType
                .orEmpty()
                .lowercase()
                .contains(
                    "text/html",
                )
        if (code in 200..299 && !html) {
            return connection to DriveVia.PUBLIC
        }
        connection.disconnect()

        if (accountProblem == null && !html && code !in 200..299 && code !in CLOSED_CODES) {
            // A server or network problem, not a closed folder.
            error(
                "HTTP $code при доступі до каталогу.",
            )
        }

        throw DriveAccessException(
            accountProblem
                ?: if (DriveAccess.isSignedIn(context)) {
                    DriveAccessTexts.SESSION_ENDED
                } else {
                    DriveAccessTexts.CLOSED_SIGN_IN
                },
        )
    }

    fun driveApiUrl(
        fileId: String,
    ): String =
        "https://www.googleapis.com/drive/v3/files/" +
            Uri.encode(
                fileId,
            ) +
            "?alt=media&supportsAllDrives=true"

    private fun connect(
        url: String,
        bearer: String?,
    ): HttpURLConnection {
        val connection =
            URL(
                url,
            ).openConnection() as
                HttpURLConnection

        connection.instanceFollowRedirects =
            true
        connection.connectTimeout =
            CONNECT_TIMEOUT_MS
        connection.readTimeout =
            READ_TIMEOUT_MS
        connection.setRequestProperty(
            "User-Agent",
            "Renault-Docs-Android",
        )
        if (bearer != null) {
            // In memory only; never logged or written anywhere.
            connection.setRequestProperty(
                "Authorization",
                "Bearer $bearer",
            )
        }
        connection.connect()

        return connection
    }
}
