package com.saney.renaultdocs

data class OperationProgress(
    val stage: String,
    val current: Int? = null,
    val total: Int? = null,
    val itemCurrent: Int? = null,
    val itemTotal: Int? = null,
    val itemLabel: String? = null,
) {
    val isDeterminate: Boolean
        get() =
            current != null &&
                total != null &&
                total > 0

    val normalizedCurrent: Int?
        get() =
            if (
                isDeterminate
            ) {
                current!!
                    .coerceIn(
                        0,
                        total!!,
                    )
            } else {
                null
            }

    fun displayText(): String {
        val compact =
            compactStage()
        val currentItem =
            itemCurrent
        val totalItems =
            itemTotal
        val label =
            itemLabel
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        return when {
            currentItem != null &&
                totalItems != null &&
                totalItems > 0 &&
                label != null ->
                compact +
                    " · " +
                    label +
                    ": " +
                    currentItem.coerceIn(
                        0,
                        totalItems,
                    ) +
                    " / " +
                    totalItems

            currentItem != null &&
                label != null ->
                compact +
                    " · " +
                    label +
                    ": " +
                    currentItem.coerceAtLeast(
                        0,
                    )

            else ->
                compact
        }
    }

    fun compactStage(): String =
        stage
            .trim()
            .replace(
                Regex("""\s+\d+\s*/\s*\d+.*$"""),
                "",
            )
            .replace(
                Regex("""\s+\d+\s+(?:файл|файли|файлів).*$"""),
                "",
            )
            .trim()
            .ifBlank {
                "Виконую…"
            }

    companion object {
        fun indeterminate(
            stage: String,
        ): OperationProgress =
            OperationProgress(
                stage =
                    stage,
            )

        fun measured(
            stage: String,
            current: Int,
            total: Int,
            itemCurrent: Int? = null,
            itemTotal: Int? = null,
            itemLabel: String? = null,
        ): OperationProgress =
            OperationProgress(
                stage =
                    stage,
                current =
                    current,
                total =
                    total,
                itemCurrent =
                    itemCurrent,
                itemTotal =
                    itemTotal,
                itemLabel =
                    itemLabel,
            )
    }
}
