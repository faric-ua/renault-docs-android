package com.saney.renaultdocs

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

data class ConversionPackageResult(
    val entrypoint: String,
    val volumeCount: Int,
    val manifest: JSONObject,
)

object AndroidDatasetPackageWriter {
    private val entryNames =
        listOf(
            "INDEX.HTM",
            "index.htm",
            "INDEX.HTML",
            "index.html",
            "ACCUEIL.HTM",
            "accueil.htm",
        )

    private val dateRegex =
        Regex(
            """(?<year>20\d{2})[._-](?<month>\d{2})[._-](?<day>\d{2})"""
        )

    private val ntRegex =
        Regex(
            """(?:^|[^A-Z0-9])NT(?<number>\d{4}[A-Z]?)(?=$|[^A-Z0-9])""",
            RegexOption.IGNORE_CASE,
        )

    fun write(
        context: Context,
        outputRoot: DocumentFile,
        sourceName: String,
        sourceFilePaths: Set<String>,
        sourceDirectoryPaths: Set<String>,
        filesTotal: Int,
        changedFiles: Int,
        changesTotal: Int,
    ): ConversionPackageResult {
        val entrypoint =
            discoverEntrypoint(
                sourceFilePaths,
            )
                ?: error(
                    "Не знайдено INDEX/ACCUEIL у source."
                )

        val volumes =
            discoverVolumes(
                filePaths =
                    sourceFilePaths,
                directoryPaths =
                    sourceDirectoryPaths,
            )

        val packageRoot =
            outputRoot.findFile(
                "_renault",
            )?.takeIf {
                it.isDirectory
            }
                ?: outputRoot.createDirectory(
                    "_renault",
                )
                ?: error(
                    "Не вдалося створити _renault."
                )

        val datasetId =
            datasetIdFor(
                sourceName,
            )

        val modernIndex =
            JSONObject()
                .put(
                    "schema_version",
                    1,
                )
                .put(
                    "dataset",
                    JSONObject()
                        .put(
                            "id",
                            datasetId,
                        )
                        .put(
                            "title",
                            sourceName,
                        )
                        .put(
                            "manufacturer",
                            "Renault",
                        )
                        .put(
                            "model",
                            sourceName,
                        )
                        .put(
                            "content_type",
                            "technical-documentation",
                        ),
                )
                .put(
                    "navigation",
                    JSONObject()
                        .put(
                            "level",
                            "volumes",
                        )
                        .put(
                            "volumes",
                            JSONArray(
                                volumes,
                            ),
                        ),
                )

        writeUtf8(
            context,
            packageRoot,
            "modern-index.json",
            "application/json",
            modernIndex.toString(2) + "\n",
        )

        writeUtf8(
            context,
            packageRoot,
            "volumes.json",
            "application/json",
            JSONArray(
                volumes,
            ).toString(2) + "\n",
        )

        writeUtf8(
            context,
            packageRoot,
            "START.html",
            "text/html",
            renderCatalog(
                title = sourceName,
                volumes = volumes,
                legacyEntrypoint =
                    entrypoint,
            ),
        )

        writeUtf8(
            context,
            packageRoot,
            "README_UA.html",
            "text/html",
            renderReadme(
                sourceName,
                volumes.size,
            ),
        )

        val manifest =
            JSONObject()
                .put(
                    "schema_version",
                    1,
                )
                .put(
                    "id",
                    datasetId,
                )
                .put(
                    "title",
                    sourceName,
                )
                .put(
                    "project_id",
                    datasetId,
                )
                .put(
                    "manufacturer",
                    "Renault",
                )
                .put(
                    "model",
                    sourceName,
                )
                .put(
                    "content_type",
                    "technical-documentation",
                )
                .put(
                    "entrypoint",
                    entrypoint,
                )
                .put(
                    "legacy_entrypoint",
                    entrypoint,
                )
                .put(
                    "catalog_entrypoint",
                    "_renault/START.html",
                )
                .put(
                    "modern_index",
                    "_renault/modern-index.json",
                )
                .put(
                    "volumes",
                    JSONArray(
                        volumes,
                    ),
                )
                .put(
                    "viewer_profile",
                    "renault-legacy-web-v1",
                )
                .put(
                    "capabilities",
                    JSONObject()
                        .put(
                            "html_frames",
                            true,
                        )
                        .put(
                            "javascript",
                            true,
                        )
                        .put(
                            "pdf",
                            true,
                        )
                        .put(
                            "pdf_fragments",
                            true,
                        ),
                )
                .put(
                    "conversion",
                    JSONObject()
                        .put(
                            "normalized",
                            true,
                        )
                        .put(
                            "tool",
                            "renault-docs-android",
                        )
                        .put(
                            "writer",
                            "android-saf-v1",
                        )
                        .put(
                            "files_total",
                            filesTotal,
                        )
                        .put(
                            "changed_files",
                            changedFiles,
                        )
                        .put(
                            "changes_total",
                            changesTotal,
                        )
                        .put(
                            "modern_runtime_compiled",
                            false,
                        ),
                )

        writeUtf8(
            context,
            outputRoot,
            "renault-dataset.json",
            "application/json",
            manifest.toString(2) + "\n",
        )

        return ConversionPackageResult(
            entrypoint =
                entrypoint,
            volumeCount =
                volumes.size,
            manifest =
                manifest,
        )
    }

    fun datasetIdFor(
        sourceName: String,
    ): String =
        slugify(
            sourceName,
        )

    fun discoverVolumeRoots(
        filePaths: Set<String>,
        directoryPaths: Set<String>,
    ): List<String> =
        discoverVolumes(
            filePaths =
                filePaths,
            directoryPaths =
                directoryPaths,
        ).mapNotNull {
            volume ->
            volume
                .optString(
                    "source_folder",
                )
                .takeIf {
                    it.isNotBlank()
                }
        }

    private fun discoverEntrypoint(
        filePaths: Set<String>,
    ): String? {
        entryNames.forEach {
                name ->
            if (name in filePaths) {
                return name
            }
        }

        return filePaths
            .asSequence()
            .filter {
                candidate ->
                val fileName =
                    candidate
                        .substringAfterLast(
                            '/',
                        )

                entryNames.any {
                    it == fileName
                }
            }
            .sorted()
            .firstOrNull()
    }

    private fun discoverVolumes(
        filePaths: Set<String>,
        directoryPaths: Set<String>,
    ): List<JSONObject> {
        val topLevel =
            directoryPaths
                .asSequence()
                .filter {
                    '/' !in it
                }
                .filterNot {
                    folder ->
                    folder
                        .trim()
                        .lowercase(
                            Locale.ROOT,
                        ) in
                        setOf(
                            "backup",
                            "_renault",
                            "packages",
                        )
                }
                .sortedBy {
                    it.lowercase(
                        Locale.ROOT,
                    )
                }
                .toList()

        return topLevel
            .mapNotNull {
                folder ->
                val entrypoint =
                    discoverVolumeEntrypoint(
                        folder,
                        filePaths,
                    )
                        ?: return@mapNotNull null

                val date =
                    extractDate(
                        folder,
                    )
                val documentCode =
                    extractNtCode(
                        folder,
                    )
                val isVisu =
                    folder
                        .lowercase(
                            Locale.ROOT,
                        )
                        .contains(
                            "visu",
                        )

                val title =
                    buildList {
                        add(
                            documentCode
                                ?: if (isVisu) {
                                    "VISU"
                                } else {
                                    folder
                                }
                        )

                        if (date != null) {
                            add(
                                date,
                            )
                        }
                    }.joinToString(
                        " · ",
                    )

                JSONObject()
                    .put(
                        "id",
                        slugify(
                            folder,
                        ),
                    )
                    .put(
                        "title",
                        title,
                    )
                    .put(
                        "source_folder",
                        folder,
                    )
                    .put(
                        "entrypoint",
                        entrypoint,
                    )
                    .put(
                        "kind",
                        if (isVisu) {
                            "wiring-diagrams"
                        } else {
                            "technical-documentation"
                        },
                    )
                    .apply {
                        if (documentCode != null) {
                            put(
                                "document_code",
                                documentCode,
                            )
                        }

                        if (date != null) {
                            put(
                                "date",
                                date,
                            )
                        }
                    }
            }
            .sortedWith(
                compareBy<JSONObject> {
                    it.optString(
                        "date",
                        "9999-99-99",
                    )
                }.thenBy {
                    it.optString(
                        "document_code",
                        it.optString(
                            "title",
                        ),
                    )
                }
            )
    }

    private fun discoverVolumeEntrypoint(
        folder: String,
        filePaths: Set<String>,
    ): String? {
        entryNames.forEach {
                name ->
            val direct =
                "$folder/$name"

            if (direct in filePaths) {
                return direct
            }
        }

        val prefix =
            "$folder/"

        return filePaths
            .asSequence()
            .filter {
                candidate ->
                if (
                    !candidate.startsWith(
                        prefix,
                    )
                ) {
                    return@filter false
                }

                val fileName =
                    candidate
                        .substringAfterLast(
                            '/',
                        )

                entryNames.any {
                    it == fileName
                }
            }
            .sorted()
            .firstOrNull()
    }

    private fun extractDate(
        name: String,
    ): String? {
        val match =
            dateRegex.find(
                name,
            )
                ?: return null

        val year =
            match.groups["year"]
                ?.value
                ?: return null
        val month =
            match.groups["month"]
                ?.value
                ?: return null
        val day =
            match.groups["day"]
                ?.value
                ?: return null

        return "$year-$month-$day"
    }

    private fun extractNtCode(
        name: String,
    ): String? {
        val match =
            ntRegex.find(
                name,
            )
                ?: return null

        val number =
            match.groups["number"]
                ?.value
                ?: return null

        return "NT" +
            number.uppercase(
                Locale.ROOT,
            )
    }

    private fun slugify(
        value: String,
    ): String {
        val normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD,
            )
        val ascii =
            normalized
                .replace(
                    Regex(
                        """[^\p{ASCII}]"""
                    ),
                    "",
                )
        val slug =
            ascii
                .replace(
                    Regex(
                        """[^A-Za-z0-9]+"""
                    ),
                    "-",
                )
                .trim(
                    '-',
                )
                .lowercase(
                    Locale.ROOT,
                )

        return slug.ifBlank {
            "renault"
        }
    }

    private fun writeUtf8(
        context: Context,
        parent: DocumentFile,
        name: String,
        mime: String,
        text: String,
    ) {
        parent.findFile(
            name,
        )?.delete()

        val file =
            parent.createFile(
                mime,
                name,
            )
                ?: error(
                    "Не вдалося створити $name."
                )

        context.contentResolver
            .openOutputStream(
                file.uri,
                "wt",
            )
            ?.bufferedWriter(
                Charsets.UTF_8,
            )
            ?.use {
                it.write(
                    text,
                )
            }
            ?: error(
                "Не вдалося записати $name."
            )
    }

    private fun renderCatalog(
        title: String,
        volumes: List<JSONObject>,
        legacyEntrypoint: String,
    ): String {
        val cards =
            volumes.joinToString(
                "\n",
            ) {
                volume ->
                val entrypoint =
                    volume.getString(
                        "entrypoint",
                    )
                val cardTitle =
                    escapeHtml(
                        volume.getString(
                            "title",
                        )
                    )
                val subtitle =
                    escapeHtml(
                        volume.optString(
                            "source_folder",
                        )
                    )

                """
                <a class="card" href="../${escapeAttribute(entrypoint)}">
                  <div class="title">$cardTitle</div>
                  <div class="sub">$subtitle</div>
                </a>
                """.trimIndent()
            }

        return """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>${escapeHtml(title)}</title>
              <style>
                body{font-family:system-ui,sans-serif;background:#101318;color:#f3f6f8;margin:0;padding:16px}
                h1{font-size:24px}
                .card{display:block;color:#f3f6f8;text-decoration:none;background:#181d25;border:1px solid #384352;border-radius:12px;padding:14px;margin:0 0 10px}
                .title{font-size:18px;font-weight:700}
                .sub{font-size:13px;color:#aab5c2;margin-top:5px}
                .legacy{color:#76bdff}
              </style>
            </head>
            <body>
              <h1>${escapeHtml(title)}</h1>
              $cards
              <p><a class="legacy" href="../${escapeAttribute(legacyEntrypoint)}">Відкрити оригінальний INDEX</a></p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun renderReadme(
        sourceName: String,
        volumeCount: Int,
    ): String =
        """
        <!doctype html>
        <html lang="uk">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
          <title>Як користуватися — ${escapeHtml(sourceName)}</title>
          <style>
            :root{color-scheme:dark}
            body{max-width:860px;margin:0 auto;padding:14px 14px 28px;box-sizing:border-box;font-family:system-ui,-apple-system,sans-serif;font-size:15px;line-height:1.45;background:#101318;color:#f3f6f8}
            h1{margin:0 0 8px;font-size:22px;line-height:1.18}
            h2{margin:22px 0 10px;font-size:16px;line-height:1.25}
            p{margin:8px 0 12px}
            .box{margin:12px 0 16px;padding:10px 12px;border:1px solid #384352;border-radius:12px;background:#181d25;font-size:14px}
            .file-table{width:100%;table-layout:fixed;border-collapse:separate;border-spacing:0;overflow:hidden;border:1px solid #384352;border-radius:12px;background:#181d25;font-size:13px;line-height:1.35}
            .file-table th,.file-table td{box-sizing:border-box;padding:9px 8px;vertical-align:top;text-align:left;overflow-wrap:anywhere;word-break:break-word}
            .file-table th{color:#aab5c2;background:#222936;font-size:12px}
            .file-table th:first-child,.file-table td:first-child{width:43%;border-right:1px solid #384352}
            .file-table tr+tr td{border-top:1px solid #384352}
            code{padding:1px 4px;border-radius:5px;background:#202732;white-space:normal;overflow-wrap:anywhere;font-size:12px}
          </style>
        </head>
        <body>
          <h1>${escapeHtml(sourceName)}</h1>
          <p>Конвертований Renault dataset. Внутрішні файли не потрібно редагувати вручну.</p>
          <div class="box"><b>Томів у цьому dataset:</b> $volumeCount</div>

          <h2>Основні файли</h2>
          <table class="file-table">
            <thead><tr><th>Файл</th><th>Призначення</th></tr></thead>
            <tbody>
              <tr><td><code>renault-dataset.json</code></td><td>Опис dataset і точки входу Renault Docs.</td></tr>
              <tr><td><code>_renault/START.html</code></td><td>Каталог внутрішніх томів.</td></tr>
              <tr><td><code>_renault/volumes.json</code></td><td>Машиночитуваний список томів.</td></tr>
              <tr><td><code>_renault/modern-index.json</code></td><td>Швидкий індекс томів для Modern.</td></tr>
            </tbody>
          </table>

          <h2>Про пакет</h2>
          <p>Цей етап створює нормалізований Classic-ready dataset; подальша компіляція додає повний Modern Runtime IR і Fast Pack.</p>
        </body>
        </html>
        """.trimIndent()

    private fun escapeHtml(
        value: String,
    ): String =
        value
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
            .replace(
                "\"",
                "&quot;",
            )

    private fun escapeAttribute(
        value: String,
    ): String =
        escapeHtml(
            value,
        )
}
