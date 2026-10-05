package com.saney.renaultdocs

data class OperationProgress(
    val stage: String,
    val current: Int? = null,
    val total: Int? = null,
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
        ): OperationProgress =
            OperationProgress(
                stage =
                    stage,
                current =
                    current,
                total =
                    total,
            )
    }
}
