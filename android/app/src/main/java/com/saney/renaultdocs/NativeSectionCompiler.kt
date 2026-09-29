package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * Kotlin parity layer for core/sections.py.
 *
 * Section discovery runs against app-private normalized File staging. It does
 * not execute Renault JavaScript; it discovers local navigation targets from
 * tolerant legacy HTML parsing and preserves source order + opaque identifiers.
 */
object NativeSectionCompiler {
    const val SCHEMA_VERSION =
        2

    const val FILENAME =
        "modern-sections.json"

    private const val MAX_HTML_BYTES =
        2_000_000L

    private val htmlExtensions =
        setOf(
            "htm",
            "html",
        )

    private val navigationNameHints =
        listOf(
            "menu",
            "nav",
            "navi",
            "sommaire",
            "summary",
            "index",
            "left",
            "tree",
            "toc",
            "list",
            "rubrique",
            "fonction",
        )

    private val sectionRegex =
        Regex(
            """^\s*([A-Za-z0-9][A-Za-z0-9_-]{1,15})\s*(?:[-–—:.;]+\s*)?(.*?)\s*$"""
        )

    private val sectionIdRegex =
        Regex(
            """^(?:\d{3,4}|[A-Za-z]{2,3}|[A-Za-z]{1,3}\d{1,4})$"""
        )

    private val jsHtmlRegex =
        Regex(
            """["']([^"']+?\.(?:html?))(?:[?#][^"']*)?["']""",
            RegexOption.IGNORE_CASE,
        )

    private val charsetRegex =
        Regex(
            """charset\s*=\s*["']?\s*([A-Za-z0-9._-]+)""",
            RegexOption.IGNORE_CASE,
        )

    data class CompileResult(
        val path: File,
        val data: JSONObject,
        val sectionCount: Int,
    )

    private data class NavigationEntry(
        val order: Int,
        val reference: String,
        val text: String,
    )

    private data class ParsedPage(
        val frames: List<String>,
        val entries: List<NavigationEntry>,
    )

    fun compile(
        outputRoot: File,
        volumes: List<JSONObject>,
        progress: ((String) -> Unit)? = null,
    ): CompileResult {
        val root =
            outputRoot.canonicalFile
        val packageRoot =
            File(
                root,
                NativeVolumeCompiler.PACKAGE_DIR,
            )

        require(
            packageRoot.isDirectory ||
                packageRoot.mkdirs(),
        ) {
            "Не вдалося створити _renault для section index."
        }

        val data =
            buildModernSectionsIndex(
                outputRoot =
                    root,
                volumes =
                    volumes,
                progress =
                    progress,
            )

        val target =
            File(
                packageRoot,
                FILENAME,
            )

        target.writeText(
            data.toString(
                2,
            ) +
                "\n",
            Charsets.UTF_8,
        )

        return CompileResult(
            path =
                target,
            data =
                data,
            sectionCount =
                data.optInt(
                    "section_count",
                    0,
                ),
        )
    }

    fun buildModernSectionsIndex(
        outputRoot: File,
        volumes: List<JSONObject>,
        progress: ((String) -> Unit)? = null,
    ): JSONObject {
        val indexedVolumes =
            JSONArray()
        var totalSections =
            0

        volumes.forEachIndexed {
            index,
            volume ->
            val result =
                discoverVolumeSections(
                    outputRoot =
                        outputRoot,
                    volume =
                        volume,
                )

            indexedVolumes.put(
                result,
            )

            val sectionCount =
                result
                    .optJSONArray(
                        "sections",
                    )
                    ?.length()
                    ?: 0

            totalSections +=
                sectionCount

            val label =
                volume
                    .optString(
                        "document_code",
                    )
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: volume
                        .optString(
                            "title",
                        )
                        .takeIf {
                            it.isNotBlank()
                        }
                    ?: "volume-" +
                        (
                            index +
                                1
                        )

            progress?.invoke(
                "Sections: " +
                    (
                        index +
                            1
                    ) +
                    "/" +
                    volumes.size +
                    " · " +
                    label +
                    " · " +
                    sectionCount +
                    " розділів"
            )
        }

        return JSONObject()
            .put(
                "schema_version",
                SCHEMA_VERSION,
            )
            .put(
                "source",
                "legacy-html-navigation",
            )
            .put(
                "volume_count",
                indexedVolumes.length(),
            )
            .put(
                "section_count",
                totalSections,
            )
            .put(
                "volumes",
                indexedVolumes,
            )
    }

    fun discoverVolumeSections(
        outputRoot: File,
        volume: JSONObject,
    ): JSONObject {
        val root =
            outputRoot.canonicalFile
        val entrypoint =
            volume
                .optString(
                    "entrypoint",
                )
                .trim()

        if (
            entrypoint.isBlank()
        ) {
            return volumeResult(
                volume =
                    volume,
                sections =
                    emptyList(),
                sourceFile =
                    null,
            )
        }

        val entryFile =
            File(
                root,
                entrypoint,
            )
                .canonicalFile

        if (
            !isWithin(
                entryFile,
                root,
            )
        ) {
            return volumeResult(
                volume =
                    volume,
                sections =
                    emptyList(),
                sourceFile =
                    null,
            )
        }

        val volumeRoot =
            volumeRoot(
                outputRoot =
                    root,
                volume =
                    volume,
                entryFile =
                    entryFile,
            )

        val sections =
            mutableListOf<JSONObject>()
        val positions =
            linkedMapOf<String, Int>()
        var sectionSource:
            File? =
            null

        val queue =
            ArrayDeque<File>()
        val visited =
            linkedSetOf<File>()

        queue.add(
            entryFile,
        )

        while (
            queue.isNotEmpty() &&
            visited.size <
            64
        ) {
            val current =
                queue.removeFirst()
                    .canonicalFile

            if (
                !visited.add(
                    current,
                )
            ) {
                continue
            }

            val parsed =
                parseHtmlFile(
                    current,
                )
                    ?: continue

            val found =
                extractSections(
                    parsed =
                        parsed,
                    currentFile =
                        current,
                    outputRoot =
                        root,
                )

            if (
                found.isNotEmpty()
            ) {
                if (
                    sectionSource ==
                    null
                ) {
                    sectionSource =
                        current
                }

                mergeSections(
                    destination =
                        sections,
                    positions =
                        positions,
                    incoming =
                        found,
                )
            }

            parsed.frames
                .forEach {
                    reference ->
                    val target =
                        resolveReference(
                            currentFile =
                                current,
                            reference =
                                reference,
                            outputRoot =
                                root,
                        )

                    if (
                        target !=
                        null &&
                        target.isFile &&
                        isHtml(
                            target,
                        ) &&
                        target !in
                        visited
                    ) {
                        queue.add(
                            target,
                        )
                    }
                }
        }

        if (
            sections.size <
            4
        ) {
            for (
                current in
                fallbackCandidates(
                    volumeRoot =
                        volumeRoot,
                    alreadySeen =
                        visited,
                )
            ) {
                val parsed =
                    parseHtmlFile(
                        current,
                    )
                        ?: continue

                val found =
                    extractSections(
                        parsed =
                            parsed,
                        currentFile =
                            current,
                        outputRoot =
                            root,
                    )

                if (
                    found.isNotEmpty()
                ) {
                    if (
                        sectionSource ==
                        null
                    ) {
                        sectionSource =
                            current
                    }

                    mergeSections(
                        destination =
                            sections,
                        positions =
                            positions,
                        incoming =
                            found,
                    )
                }

                if (
                    sections.size >=
                    8
                ) {
                    break
                }
            }
        }

        val sourceRelative =
            sectionSource
                ?.takeIf {
                    isWithin(
                        it,
                        root,
                    )
                }
                ?.relativeTo(
                    root,
                )
                ?.invariantSeparatorsPath

        return volumeResult(
            volume =
                volume,
            sections =
                sections,
            sourceFile =
                sourceRelative,
        )
    }

    private fun volumeResult(
        volume: JSONObject,
        sections: List<JSONObject>,
        sourceFile: String?,
    ): JSONObject {
        val result =
            JSONObject()

        listOf(
            "id",
            "title",
            "document_code",
            "date",
            "entrypoint",
        ).forEach {
            key ->
            if (
                volume.has(
                    key,
                ) &&
                !volume.isNull(
                    key,
                )
            ) {
                result.put(
                    key,
                    volume.get(
                        key,
                    ),
                )
            }
        }

        if (
            sourceFile !=
            null
        ) {
            result.put(
                "source_file",
                sourceFile,
            )
        }

        result.put(
            "sections",
            JSONArray(
                sections,
            ),
        )

        return result
    }

    private fun extractSections(
        parsed: ParsedPage,
        currentFile: File,
        outputRoot: File,
    ): List<JSONObject> {
        val result =
            mutableListOf<JSONObject>()

        parsed.entries
            .sortedBy {
                it.order
            }
            .forEach {
                candidate ->
                val target =
                    resolveReference(
                        currentFile =
                            currentFile,
                        reference =
                            candidate.reference,
                        outputRoot =
                            outputRoot,
                    )

                if (
                    target ==
                    null ||
                    !target.isFile ||
                    !isHtml(
                        target,
                    )
                ) {
                    return@forEach
                }

                val label =
                    parseSectionLabel(
                        text =
                            candidate.text,
                        target =
                            target,
                    )
                        ?: return@forEach

                result +=
                    JSONObject()
                        .put(
                            "code",
                            label.first,
                        )
                        .put(
                            "title",
                            label.second,
                        )
                        .put(
                            "entrypoint",
                            target
                                .relativeTo(
                                    outputRoot,
                                )
                                .invariantSeparatorsPath,
                        )
            }

        return result
    }

    private fun parseSectionLabel(
        text: String,
        target: File,
    ): Pair<String, String>? {
        val cleaned =
            cleanText(
                text,
            )

        val match =
            sectionRegex.matchEntire(
                cleaned,
            )

        if (
            match !=
            null
        ) {
            val code =
                match.groupValues[
                    1
                ]
                    .uppercase(
                        Locale.ROOT,
                    )

            if (
                looksLikeSectionId(
                    code,
                )
            ) {
                val title =
                    cleanText(
                        match.groupValues[
                            2
                        ],
                    )

                return code to
                    (
                        title.ifBlank {
                            "Розділ " +
                                code
                        }
                    )
            }
        }

        val stem =
            target
                .nameWithoutExtension
                .uppercase(
                    Locale.ROOT,
                )

        if (
            !looksLikeSectionId(
                stem,
            )
        ) {
            return null
        }

        val prefix =
            Regex(
                "^\\s*" +
                    Regex.escape(
                        stem,
                    ) +
                    "\\s*(?:[-–—:.;]+\\s*)?",
                RegexOption.IGNORE_CASE,
            )

        val title =
            cleanText(
                prefix.replaceFirst(
                    cleaned,
                    "",
                ),
            )

        return stem to
            (
                title.ifBlank {
                    "Розділ " +
                        stem
                }
            )
    }

    fun looksLikeSectionId(
        value: String,
    ): Boolean =
        sectionIdRegex.matches(
            value.trim(),
        )

    private fun mergeSections(
        destination: MutableList<JSONObject>,
        positions: MutableMap<String, Int>,
        incoming: List<JSONObject>,
    ) {
        incoming.forEach {
            section ->
            val code =
                section.getString(
                    "code",
                )
            val entrypoint =
                section.getString(
                    "entrypoint",
                )
            val key =
                code.lowercase(
                    Locale.ROOT,
                ) +
                    "\u0000" +
                    entrypoint.lowercase(
                        Locale.ROOT,
                    )

            val position =
                positions[
                    key
                ]

            if (
                position ==
                null
            ) {
                positions[
                    key
                ] =
                    destination.size

                destination +=
                    section

                return@forEach
            }

            val previous =
                destination[
                    position
                ]
            val previousTitle =
                previous.getString(
                    "title",
                )
            val incomingTitle =
                section.getString(
                    "title",
                )
            val previousIsGeneric =
                previousTitle ==
                    "Розділ " +
                    code
            val incomingIsBetter =
                !incomingTitle.startsWith(
                    "Розділ ",
                ) &&
                    (
                        previousIsGeneric ||
                            incomingTitle.length >
                            previousTitle.length
                    )

            if (
                incomingIsBetter
            ) {
                destination[
                    position
                ] =
                    section
            }
        }
    }

    private fun fallbackCandidates(
        volumeRoot: File,
        alreadySeen: Set<File>,
    ): List<File> {
        if (
            !volumeRoot.isDirectory
        ) {
            return emptyList()
        }

        return volumeRoot
            .walkTopDown()
            .filter {
                path ->
                path.isFile &&
                    isHtml(
                        path,
                    ) &&
                    path.canonicalFile !in
                    alreadySeen
            }
            .map {
                it.canonicalFile
            }
            .sortedWith(
                compareBy<File> {
                    path ->
                    val name =
                        path.name.lowercase(
                            Locale.ROOT,
                        )

                    if (
                        navigationNameHints.any {
                            hint ->
                            hint in
                                name
                        }
                    ) {
                        0
                    } else {
                        1
                    }
                }.thenBy {
                    path ->
                    path.relativeTo(
                        volumeRoot,
                    )
                        .invariantSeparatorsPath
                        .count {
                            char ->
                            char ==
                                '/'
                        }
                }.thenBy {
                    path ->
                    safeSize(
                        path,
                    )
                }.thenBy {
                    path ->
                    path.relativeTo(
                        volumeRoot,
                    )
                        .invariantSeparatorsPath
                        .lowercase(
                            Locale.ROOT,
                        )
                }
            )
            .take(
                300,
            )
            .toList()
    }

    private fun parseHtmlFile(
        path: File,
    ): ParsedPage? {
        if (
            !path.isFile ||
            !isHtml(
                path,
            ) ||
            safeSize(
                path,
            ) >
            MAX_HTML_BYTES
        ) {
            return null
        }

        val raw =
            runCatching {
                path.readBytes()
            }.getOrNull()
                ?: return null

        val text =
            decodeHtml(
                raw,
            )

        val document =
            runCatching {
                Jsoup.parse(
                    text,
                )
            }.getOrNull()
                ?: return null

        val all =
            document.getAllElements()
        val order =
            linkedMapOf<Element, Int>()

        all.forEachIndexed {
            index,
            element ->
            order[
                element
            ] =
                index
        }

        val frames =
            mutableListOf<String>()
        val entries =
            mutableListOf<NavigationEntry>()

        all.forEach {
            element ->
            when (
                element.normalName()
            ) {
                "frame",
                "iframe",
                -> {
                    element
                        .attr(
                            "src",
                        )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            frames +=
                                it
                        }
                }

                "a" -> {
                    val reference =
                        linkReference(
                            href =
                                element.attr(
                                    "href",
                                ),
                            onclick =
                                element.attr(
                                    "onclick",
                                ),
                        )
                            ?: return@forEach

                    val label =
                        cleanText(
                            element.text(),
                        )

                    if (
                        label.isNotBlank()
                    ) {
                        entries +=
                            NavigationEntry(
                                order =
                                    order[
                                        element
                                    ]
                                        ?: 0,
                                reference =
                                    reference,
                                text =
                                    label,
                            )
                    }
                }

                "area" -> {
                    val reference =
                        linkReference(
                            href =
                                element.attr(
                                    "href",
                                ),
                            onclick =
                                element.attr(
                                    "onclick",
                                ),
                        )
                            ?: return@forEach

                    val label =
                        cleanText(
                            element.attr(
                                "alt",
                            )
                                .ifBlank {
                                    element.attr(
                                        "title",
                                    )
                                },
                        )

                    if (
                        label.isNotBlank()
                    ) {
                        entries +=
                            NavigationEntry(
                                order =
                                    order[
                                        element
                                    ]
                                        ?: 0,
                                reference =
                                    reference,
                                text =
                                    label,
                            )
                    }
                }

                "tr" -> {
                    val firstAnchor =
                        element
                            .getElementsByTag(
                                "a",
                            )
                            .firstOrNull {
                                anchor ->
                                linkReference(
                                    href =
                                        anchor.attr(
                                            "href",
                                        ),
                                    onclick =
                                        anchor.attr(
                                            "onclick",
                                        ),
                                ) !=
                                    null
                            }
                            ?: return@forEach

                    val reference =
                        linkReference(
                            href =
                                firstAnchor.attr(
                                    "href",
                                ),
                            onclick =
                                firstAnchor.attr(
                                    "onclick",
                                ),
                        )
                            ?: return@forEach
                    val label =
                        cleanText(
                            element.text(),
                        )

                    if (
                        label.isNotBlank()
                    ) {
                        entries +=
                            NavigationEntry(
                                order =
                                    order[
                                        element
                                    ]
                                        ?: 0,
                                reference =
                                    reference,
                                text =
                                    label,
                            )
                    }
                }
            }
        }

        return ParsedPage(
            frames =
                frames,
            entries =
                entries,
        )
    }

    private fun decodeHtml(
        raw: ByteArray,
    ): String {
        val head =
            raw
                .copyOfRange(
                    0,
                    minOf(
                        8192,
                        raw.size,
                    ),
                )
                .toString(
                    Charsets.ISO_8859_1,
                )

        val encodings =
            buildList {
                charsetRegex
                    .find(
                        head,
                    )
                    ?.groupValues
                    ?.getOrNull(
                        1,
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it,
                        )
                    }

                add(
                    "UTF-8",
                )
                add(
                    "windows-1251",
                )
                add(
                    "windows-1252",
                )
                add(
                    "ISO-8859-1",
                )
            }

        val seen =
            mutableSetOf<String>()

        encodings.forEach {
            name ->
            val key =
                name.lowercase(
                    Locale.ROOT,
                )

            if (
                !seen.add(
                    key,
                )
            ) {
                return@forEach
            }

            val decoded =
                runCatching {
                    val charset =
                        java.nio.charset.Charset
                            .forName(
                                name,
                            )
                    val decoder =
                        charset
                            .newDecoder()
                            .onMalformedInput(
                                CodingErrorAction.REPORT,
                            )
                            .onUnmappableCharacter(
                                CodingErrorAction.REPORT,
                            )

                    decoder
                        .decode(
                            ByteBuffer.wrap(
                                raw,
                            )
                        )
                        .toString()
                }.getOrNull()

            if (
                decoded !=
                null
            ) {
                return decoded
            }
        }

        return raw.toString(
            Charsets.UTF_8,
        )
    }

    private fun linkReference(
        href: String,
        onclick: String,
    ): String? {
        val cleanHref =
            Parser
                .unescapeEntities(
                    href,
                    false,
                )
                .trim()

        if (
            cleanHref.isNotBlank() &&
            cleanHref !=
            "#"
        ) {
            if (
                !cleanHref
                    .lowercase(
                        Locale.ROOT,
                    )
                    .startsWith(
                        "javascript:",
                    )
            ) {
                return cleanHref
            }

            jsHtmlRegex
                .find(
                    cleanHref,
                )
                ?.groupValues
                ?.getOrNull(
                    1,
                )
                ?.let {
                    return it
                }
        }

        val cleanOnclick =
            Parser.unescapeEntities(
                onclick,
                false,
            )

        return jsHtmlRegex
            .find(
                cleanOnclick,
            )
            ?.groupValues
            ?.getOrNull(
                1,
            )
    }

    private fun resolveReference(
        currentFile: File,
        reference: String,
        outputRoot: File,
    ): File? {
        var cleaned =
            Parser
                .unescapeEntities(
                    reference,
                    false,
                )
                .trim()

        if (
            cleaned.isBlank()
        ) {
            return null
        }

        val lowered =
            cleaned.lowercase(
                Locale.ROOT,
            )

        if (
            listOf(
                "http:",
                "https:",
                "mailto:",
                "data:",
            ).any {
                lowered.startsWith(
                    it,
                )
            }
        ) {
            return null
        }

        if (
            lowered.startsWith(
                "javascript:",
            )
        ) {
            cleaned =
                jsHtmlRegex
                    .find(
                        cleaned,
                    )
                    ?.groupValues
                    ?.getOrNull(
                        1,
                    )
                    ?: return null
        }

        val cut =
            listOf(
                cleaned.indexOf(
                    '#',
                ),
                cleaned.indexOf(
                    '?',
                ),
            )
                .filter {
                    it >=
                        0
                }
                .minOrNull()
                ?: cleaned.length

        val pathText =
            percentDecode(
                cleaned
                    .substring(
                        0,
                        cut,
                    )
                    .replace(
                        '\\',
                        '/',
                    )
                    .trim(),
            )

        if (
            pathText.isBlank()
        ) {
            return null
        }

        val target =
            if (
                pathText.startsWith(
                    '/',
                )
            ) {
                File(
                    outputRoot,
                    pathText.trimStart(
                        '/',
                    ),
                )
            } else {
                File(
                    currentFile.parentFile,
                    pathText,
                )
            }
                .canonicalFile

        if (
            !isWithin(
                target,
                outputRoot,
            )
        ) {
            return null
        }

        return target
    }

    private fun percentDecode(
        value: String,
    ): String {
        if (
            '%' !in
            value
        ) {
            return value
        }

        val output =
            ByteArrayOutputStream()
        var index =
            0

        while (
            index <
            value.length
        ) {
            val char =
                value[
                    index
                ]

            if (
                char ==
                '%' &&
                index +
                    2 <
                value.length &&
                value[
                    index +
                        1
                ].isHexDigit() &&
                value[
                    index +
                        2
                ].isHexDigit()
            ) {
                output.write(
                    value
                        .substring(
                            index +
                                1,
                            index +
                                3,
                        )
                        .toInt(
                            16,
                        )
                )
                index +=
                    3
                continue
            }

            output.write(
                char
                    .toString()
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
            )
            index +=
                1
        }

        return output
            .toByteArray()
            .toString(
                StandardCharsets.UTF_8,
            )
    }

    private fun volumeRoot(
        outputRoot: File,
        volume: JSONObject,
        entryFile: File,
    ): File {
        val sourceFolder =
            volume
                .optString(
                    "source_folder",
                )
                .trim()

        if (
            sourceFolder.isNotBlank()
        ) {
            val candidate =
                File(
                    outputRoot,
                    sourceFolder,
                )
                    .canonicalFile

            if (
                candidate.isDirectory &&
                isWithin(
                    candidate,
                    outputRoot,
                )
            ) {
                return candidate
            }
        }

        return entryFile.parentFile
            ?: outputRoot
    }

    private fun isHtml(
        path: File,
    ): Boolean =
        path
            .extension
            .lowercase(
                Locale.ROOT,
            ) in
            htmlExtensions

    private fun isWithin(
        path: File,
        root: File,
    ): Boolean =
        path
            .canonicalFile
            .toPath()
            .startsWith(
                root
                    .canonicalFile
                    .toPath(),
            )

    private fun safeSize(
        path: File,
    ): Long =
        runCatching {
            path.length()
        }.getOrDefault(
            MAX_HTML_BYTES +
                1,
        )

    private fun cleanText(
        value: String,
    ): String =
        Parser
            .unescapeEntities(
                value,
                false,
            )
            .replace(
                '\u00a0',
                ' ',
            )
            .trim()
            .replace(
                Regex(
                    """\s+"""
                ),
                " ",
            )

    private fun Char.isHexDigit(): Boolean =
        this in
            '0'..'9' ||
            this in
            'a'..'f' ||
            this in
            'A'..'F'
}
