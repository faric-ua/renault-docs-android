package com.saney.renaultdocs

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DriveCatalogClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    fun loadCatalog(): DriveCatalog {
        val connection =
            open(
                ExternalLinks.PROJECT_CATALOG_MANIFEST_URL,
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
            )
        } finally {
            connection.disconnect()
        }
    }

    fun downloadPackage(
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

        val connection =
            open(
                ExternalLinks.driveDownloadUrl(
                    driveFileId,
                ),
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

    private fun open(
        url: String,
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
        connection.connect()

        val code =
            connection.responseCode
        require(
            code in 200..299
        ) {
            "HTTP $code при доступі до каталогу."
        }

        return connection
    }
}
