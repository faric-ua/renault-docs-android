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
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>Renault Docs</title>
        </head>
        <body>
          <h1>${escapeHtml(sourceName)}</h1>
          <p>Конвертовано Renault Docs Android writer.</p>
          <p>Томів: $volumeCount.</p>
          <p>Ця хвиля створює нормалізований Classic-ready dataset. Повний Modern Runtime IR/Fast Pack компілюється наступним етапом Converter 2.0.</p>
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
