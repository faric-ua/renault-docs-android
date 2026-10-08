package com.saney.renaultdocs

import java.util.Locale

/** Explicit source names beat ambiguous Renault NT/platform codes. */
object ArchiveSourceGuard {
    private val archiveTypes = setOf("zip", "7z", "rar")
    private val modelWords = listOf(
        "kangoo" to Regex("""(?i)(?:^|[^a-z])kangoo[ _.-]*(?:ii|2)?(?=$|[^a-z])"""),
        "megane" to Regex("""(?i)(?:^|[^a-z])megane[ _.-]*(?:ii|2)?(?=$|[^a-z])"""),
        "laguna" to Regex("""(?i)(?:^|[^a-z])laguna[ _.-]*(?:ii|2)?(?=$|[^a-z])"""),
    )
    fun explicitModels(text: String): Set<String> =
        modelWords.filter { (_, pattern) -> pattern.containsMatchIn(text) }
            .map { it.first }.toSet()

    fun inputError(name: String, project: RenaultProject): String? {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "rdpkg") {
            return "Це готовий .rdpkg, а не оригінальний архів. Для готового пакета використай «Авто»."
        }
        if (ext !in archiveTypes) {
            return "Обери оригінальний ZIP, 7Z або RAR; готові .rdpkg конвертувати повторно не можна."
        }
        return conflictingModel(name, project)
    }

    fun conflictingModel(name: String, project: RenaultProject): String? {
        val models = explicitModels(name)
        if (models.isEmpty()) return null
        val expected = explicitModels(project.title + " " + project.model + " " + project.id)
        if (models.size > 1 || expected.isEmpty() || expected.intersect(models).isEmpty()) {
            return "Джерело містить назву моделі \${models.sorted().joinToString("/")}, " +
                "але вибрано проєкт «\${project.title}». Підготовку зупинено."
        }
        return null
    }

    fun rootConflict(names: List<String>, project: RenaultProject): String? =
        names.asSequence().mapNotNull { conflictingModel(it, project) }.firstOrNull()

    data class CatalogMatch(
        val projectId: String,
        val projectTitle: String,
        val volumeTitle: String,
        val matchingMetadata: Boolean,
    )

    /** All registered ProjectStore projects; never crawl unrelated SAF files. */
    fun catalogMatches(
        store: ProjectStore,
        identity: RenaultVolumeIdentityMetadata,
    ): List<CatalogMatch> {
        val code = identity.documentCode ?: return emptyList()
        return store.projects().flatMap { project ->
            store.volumes(project.id).mapNotNull { volume ->
                if (!code.equals(volume.documentCode, ignoreCase = true)) return@mapNotNull null
                val sourceCodes = identity.vehicleCodes.map { it.uppercase(Locale.ROOT) }.toSet()
                val volumeCodes = volume.vehicleCodes.map { it.uppercase(Locale.ROOT) }.toSet()
                CatalogMatch(
                    project.id,
                    project.title,
                    VolumeDuplicatePreflight.label(volume),
                    identity.date != null && identity.date == volume.date &&
                        sourceCodes.isNotEmpty() && sourceCodes == volumeCodes,
                )
            }
        }
    }

    fun summary(matches: List<CatalogMatch>, activeProjectId: String): String {
        if (matches.isEmpty()) return "У каталозі проєктів збігів за номером NT немає."
        val displayed = matches.take(8).map { match ->
            val location = if (match.projectId == activeProjectId) " (поточний)" else ""
            val strength = if (match.matchingMetadata) "збіг метаданих" else "можливий дублікат"
            "• \${match.projectTitle}\${location}: \${match.volumeTitle} — \${strength}"
        }
        val extra = if (matches.size > 8) "\nЩе збігів: \${matches.size - 8}" else ""
        return "Знайдено серед усіх зареєстрованих проєктів:\n" +
            displayed.joinToString("\n") + extra
    }
}
