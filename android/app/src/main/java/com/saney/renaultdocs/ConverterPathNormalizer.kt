package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

object ConverterPathNormalizer {
    private val quotedPathRegex = Regex(
        """(["'])([^"'<>\r\n]*?\.(?:htm|html|pdf|gif|ico|js)(?:#[^"'<>\r\n]*)?(?:\?[^"'<>\r\n]*)?)\1""",
        RegexOption.IGNORE_CASE,
    )

    private val unquotedAttributeRegex = Regex(
        """((?:href|src|background|action|value)\s*=\s*)([^\s"'<>]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val pathSuffixRegex = Regex(
        """\.(?:htm|html|pdf|gif|ico|js)(?:[#?]|$)""",
        RegexOption.IGNORE_CASE,
    )

    private val printNbRegex = Regex("""(["'])_printNB\1""")
    private val printRegex = Regex("""(["'])_print\1""")

    data class Change(
        val kind: String,
        val from: String,
        val to: String,
        val actual: String? = null,
        val line: Int? = null,
    )

    data class PatchResult(
        val text: String,
        val changes: List<Change>,
    )

    fun buildLowerMap(
        exactFiles: Set<String>,
    ): Map<String, List<String>> = exactFiles
        .groupBy { it.lowercase(Locale.ROOT) }

    fun patchText(
        sourceRelativePath: String,
        input: String,
        exactFiles: Set<String>,
        lowerMap: Map<String, List<String>> = buildLowerMap(exactFiles),
    ): PatchResult {
        val changes = mutableListOf<Change>()

        var text = quotedPathRegex.replace(input) { match ->
            val quote = match.groupValues[1]
            val ref = match.groupValues[2]
            val normalized = normalizeReference(
                sourceRelativePath = sourceRelativePath,
                reference = ref,
                exactFiles = exactFiles,
                lowerMap = lowerMap,
            )

            if (normalized.first != ref) {
                changes += Change(
                    kind = "static_case",
                    from = ref,
                    to = normalized.first,
                    actual = normalized.second,
                )
            }

            quote + normalized.first + quote
        }

        text = unquotedAttributeRegex.replace(text) { match ->
            val prefix = match.groupValues[1]
            val ref = match.groupValues[2]

            if (!pathSuffixRegex.containsMatchIn(ref)) {
                return@replace match.value
            }

            val normalized = normalizeReference(
                sourceRelativePath = sourceRelativePath,
                reference = ref,
                exactFiles = exactFiles,
                lowerMap = lowerMap,
            )

            if (normalized.first != ref) {
                changes += Change(
                    kind = "static_case",
                    from = ref,
                    to = normalized.first,
                    actual = normalized.second,
                )
            }

            prefix + normalized.first
        }

        if (sourceRelativePath
                .uppercase(Locale.ROOT)
                .endsWith("/COMMUN/JS/VISU.JS")
        ) {
            val lines = text.split(Regex("(?<=\n)"))
            text = buildString {
                lines.forEachIndexed { index, line ->
                    if (line.trimStart().startsWith("//")) {
                        append(line)
                        return@forEachIndexed
                    }

                    var updated = printNbRegex.replace(line) { match ->
                        match.groupValues[1] + "_PRINTNB" + match.groupValues[1]
                    }
                    updated = printRegex.replace(updated) { match ->
                        match.groupValues[1] + "_PRINT" + match.groupValues[1]
                    }

                    if (updated != line) {
                        changes += Change(
                            kind = "dynamic_print_case",
                            from = line.trimEnd('\r', '\n'),
                            to = updated.trimEnd('\r', '\n'),
                            line = index + 1,
                        )
                    }

                    append(updated)
                }
            }
        }

        return PatchResult(
            text = text,
            changes = changes,
        )
    }

    fun normalizeReference(
        sourceRelativePath: String,
        reference: String,
        exactFiles: Set<String>,
        lowerMap: Map<String, List<String>> = buildLowerMap(exactFiles),
    ): Pair<String, String?> {
        val raw = htmlUnescape(reference)
        val low = raw.lowercase(Locale.ROOT)

        if (
            low.startsWith("http://") ||
            low.startsWith("https://") ||
            low.startsWith("mailto:") ||
            low.startsWith("javascript:") ||
            low.startsWith("data:")
        ) {
            return reference to null
        }

        val (pathPart, suffix) = splitSuffix(raw)
        val decoded = percentDecode(pathPart).replace('\\', '/')

        if (decoded.isBlank()) {
            return reference to null
        }

        val sourceDir = sourceRelativePath.substringBeforeLast('/', "")
        val absoluteStyle = decoded.startsWith('/')

        val target = if (absoluteStyle) {
            normalizePath(decoded.trimStart('/'))
        } else {
            normalizePath(
                listOf(sourceDir, decoded)
                    .filter { it.isNotBlank() }
                    .joinToString("/"),
            )
        }

        if (target == ".." || target.startsWith("../")) {
            return reference to null
        }

        if (target in exactFiles) {
            return reference to null
        }

        val matches = lowerMap[target.lowercase(Locale.ROOT)].orEmpty()
        if (matches.size != 1) {
            return reference to null
        }

        val actual = matches.single()
        var correctedPath = if (absoluteStyle) {
            "/" + actual
        } else {
            relativePath(
                fromDirectory = sourceDir,
                target = actual,
            )
        }

        if ('%' in pathPart) {
            correctedPath = percentEncodePath(correctedPath)
        }

        return correctedPath + suffix to actual
    }

    private fun splitSuffix(reference: String): Pair<String, String> {
        val hash = reference.indexOf('#').takeIf { it >= 0 }
        val query = reference.indexOf('?').takeIf { it >= 0 }
        val cut = listOfNotNull(hash, query).minOrNull() ?: reference.length
        return reference.substring(0, cut) to reference.substring(cut)
    }

    private fun normalizePath(path: String): String {
        val output = mutableListOf<String>()

        path.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> {
                    if (output.isNotEmpty() && output.last() != "..") {
                        output.removeAt(output.lastIndex)
                    } else {
                        output += ".."
                    }
                }
                else -> output += part
            }
        }

        return output.joinToString("/")
    }

    private fun relativePath(
        fromDirectory: String,
        target: String,
    ): String {
        val fromParts = normalizePath(fromDirectory)
            .split('/')
            .filter { it.isNotBlank() }
        val targetParts = normalizePath(target)
            .split('/')
            .filter { it.isNotBlank() }

        var common = 0
        val max = minOf(fromParts.size, targetParts.size)
        while (
            common < max &&
            fromParts[common] == targetParts[common]
        ) {
            common += 1
        }

        val result = buildList {
            repeat(fromParts.size - common) {
                add("..")
            }
            addAll(targetParts.drop(common))
        }

        return if (result.isEmpty()) "." else result.joinToString("/")
    }

    private fun htmlUnescape(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)

    private fun percentDecode(value: String): String {
        val out = ByteArrayOutputStream()
        var index = 0

        while (index < value.length) {
            val char = value[index]
            if (
                char == '%' &&
                index + 2 < value.length &&
                value[index + 1].isHexDigit() &&
                value[index + 2].isHexDigit()
            ) {
                val byte = value
                    .substring(index + 1, index + 3)
                    .toInt(16)
                out.write(byte)
                index += 3
                continue
            }

            out.write(char.toString().toByteArray(StandardCharsets.UTF_8))
            index += 1
        }

        return out.toByteArray().toString(StandardCharsets.UTF_8)
    }

    private fun percentEncodePath(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        return buildString {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xFF
                val char = unsigned.toChar()
                val safe = (
                    char in 'a'..'z' ||
                    char in 'A'..'Z' ||
                    char in '0'..'9' ||
                    char == '/' ||
                    char == ':' ||
                    char == '.' ||
                    char == '_' ||
                    char == '-'
                )

                if (safe) {
                    append(char)
                } else {
                    append('%')
                    append(HEX[unsigned ushr 4])
                    append(HEX[unsigned and 0x0F])
                }
            }
        }
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' ||
            this in 'a'..'f' ||
            this in 'A'..'F'

    private const val HEX = "0123456789ABCDEF"
}
