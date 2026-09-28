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
