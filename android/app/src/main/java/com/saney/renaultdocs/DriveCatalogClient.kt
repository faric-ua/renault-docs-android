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
        onProgress: (Long, Long) -> Unit,
    ) {
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
