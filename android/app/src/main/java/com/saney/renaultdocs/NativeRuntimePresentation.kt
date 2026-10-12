package com.saney.renaultdocs

object NativeRuntimePresentation {
    private val documentationMenuCodes =
        setOf(
            "GENE",
            "PLATFUSI",
            "AIDE",
        )

    fun menuLabel(
        raw: String,
    ): String =
        when (normalize(raw)) {
            "SCH" -> "Схеми"
            "NM" -> "Розʼєм"
            "PC" -> "Положення на авто"
            "GENE" -> "Загальна документація"
            "PLATFUSI" -> "Запобіжники"
            "AIDE" -> "Довідка"
            "CRITERE" ->
                "Критерії / скорочення"
            else -> raw.trim()
        }

    fun isDocumentationMenu(
        raw: String,
    ): Boolean =
        normalize(raw) in
            documentationMenuCodes

    /** v0.5.114: «Критерії / скорочення» is pinned above the list, not in it. */
    fun isCriteria(
        raw: String,
    ): Boolean =
        normalize(raw) == "CRITERE" ||
            raw.trim().equals(
                "Критерії / скорочення",
                ignoreCase = true,
            )

    fun isHiddenPlaceholder(
        raw: String,
    ): Boolean =
        normalize(raw) == "BLANK"

    private fun normalize(
        value: String,
    ): String =
        value
            .trim()
            .uppercase()
}
