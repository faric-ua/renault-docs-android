package com.saney.renaultdocs

import java.io.File
import java.util.Locale

/**
 * Defensive last-mile resolver for app-private, unpacked Renault archive data.
 *
 * The ZIP/7Z/RAR intake normally supplies an exact raw root. When a harmless
 * wrapper directory reaches native preparation, resolve it only if there is
 * exactly ONE independently identifiable nested Renault root. Never guess
 * among multiple volumes or use a folder outside the current extraction.
 * No file is moved, renamed, rewritten or deleted by this resolver.
 */
internal object ArchiveNativeRawRoot {
    private val entrypoints =
        setOf("index.htm", "index.html", "accueil.htm")

    fun hasDirectEntrypoint(root: File): Boolean =
        root.isDirectory &&
            root.listFiles()
                ?.any { child ->
                    child.isFile &&
                        child.name.lowercase(Locale.ROOT) in entrypoints
                } == true

    fun resolve(sourceRoot: File): File {
        val root = sourceRoot.canonicalFile
        require(root.isDirectory) {
            "Raw-папка з архіву недоступна."
        }
        if (hasDirectEntrypoint(root)) {
            return root
        }

        val children = ArchiveIntake.findRenaultRawRoots(root)
        require(children.size == 1) {
            val count = children.size
            if (count == 0) {
                "У вибраній папці архіву немає INDEX.HTM / INDEX.HTML / ACCUEIL.HTM, " +
                    "а вкладеного raw-тому не знайдено. Перевір структуру архіву."
            } else {
                "Архів містить кілька вкладених Renault томів ($count). " +
                    "Автоматично вибирати один з них небезпечно."
            }
        }

        val selected = children.single().canonicalFile
        require(selected.path.startsWith(root.path + File.separator) &&
            hasDirectEntrypoint(selected)) {
            "Не вдалося безпечно визначити вкладений raw-том."
        }
        return selected
    }

    /** Safe, useful failure evidence; never includes SAF URI or filesystem path. */
    fun describeMismatch(root: File, scannedPaths: Set<String>): String {
        val visible = scannedPaths
            .filter { '/' !in it }
            .sorted()
            .take(8)
            .joinToString(", ")
            .ifBlank { "немає" }
        val indexEntries = scannedPaths
            .filter { entry ->
                entry.substringAfterLast('/')
                    .lowercase(Locale.ROOT) in entrypoints
            }
            .take(5)
            .joinToString(", ")
            .ifBlank { "не знайдено" }
        return "Архівний raw-root не пройшов перевірку INDEX. " +
            "Прямий INDEX: " + hasDirectEntrypoint(root) +
            "; файли верхнього рівня: " + visible +
            "; знайдені INDEX: " + indexEntries +
            ". Пакет не створено, вихідний архів не змінено."
    }
}
