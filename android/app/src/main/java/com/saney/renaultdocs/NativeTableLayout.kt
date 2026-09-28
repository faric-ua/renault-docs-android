package com.saney.renaultdocs

import kotlin.math.max

object NativeTableLayout {
    fun columnFractions(
        table: NativeTableData,
    ): FloatArray {
        val columnCount =
            table.rows.maxOfOrNull {
                it.cells.size
            }
                ?: 1

        if (columnCount <= 1) {
            return floatArrayOf(1f)
        }

        fun maxColumnLength(
            column: Int,
        ): Int =
            table.rows.maxOfOrNull {
                visualLength(
                    it.cells
                        .getOrNull(column)
                        .orEmpty(),
                )
            } ?: 1

        fun capCompactColumns(
            values: FloatArray,
            maxTotal: Float,
        ): FloatArray {
            val total = values.sum()
            if (total <= maxTotal) {
                return values
            }

            val scale =
                maxTotal / total

            return FloatArray(
                values.size,
            ) {
                values[it] * scale
            }
        }

        return when (columnCount) {
            2 -> {
                // A glossary/metadata first column is compact: size it from
                // the longest real value or header and give all remaining
                // width to the description column.
                val first =
                    (
                        0.10f +
                            maxColumnLength(0)
                                .coerceAtMost(16) *
                                0.0085f
                    )
                        .coerceIn(
                            0.16f,
                            0.27f,
                        )

                floatArrayOf(
                    first,
                    1f - first,
                )
            }

            3 -> {
                val compact =
                    capCompactColumns(
                        floatArrayOf(
                            (
                                0.045f +
                                    maxColumnLength(0)
                                        .coerceAtMost(10) *
                                        0.013f
                            )
                                .coerceIn(
                                    0.085f,
                                    0.18f,
                                ),
                            (
                                0.045f +
                                    maxColumnLength(1)
                                        .coerceAtMost(12) *
                                        0.013f
                            )
                                .coerceIn(
                                    0.095f,
                                    0.20f,
                                ),
                        ),
                        maxTotal = 0.38f,
                    )

                floatArrayOf(
                    compact[0],
                    compact[1],
                    1f -
                        compact[0] -
                        compact[1],
                )
            }

            4 -> {
                // Pin/contact tables: №, mm² and wire code are technical
                // compact tokens. Size them from real content/header length;
                // the description column receives the full remainder.
                val compact =
                    capCompactColumns(
                        floatArrayOf(
                            (
                                0.038f +
                                    maxColumnLength(0)
                                        .coerceAtMost(6) *
                                        0.012f
                            )
                                .coerceIn(
                                    0.060f,
                                    0.10f,
                                ),
                            (
                                0.038f +
                                    maxColumnLength(1)
                                        .coerceAtMost(7) *
                                        0.013f
                            )
                                .coerceIn(
                                    0.085f,
                                    0.13f,
                                ),
                            (
                                0.038f +
                                    maxColumnLength(2)
                                        .coerceAtMost(10) *
                                        0.012f
                            )
                                .coerceIn(
                                    0.075f,
                                    0.16f,
                                ),
                        ),
                        maxTotal = 0.34f,
                    )

                floatArrayOf(
                    compact[0],
                    compact[1],
                    compact[2],
                    1f -
                        compact[0] -
                        compact[1] -
                        compact[2],
                )
            }

            else -> {
                val compact =
                    capCompactColumns(
                        FloatArray(
                            columnCount - 1,
                        ) { column ->
                            (
                                0.040f +
                                    maxColumnLength(
                                        column
                                    )
                                        .coerceAtMost(12) *
                                        0.012f
                            )
                                .coerceIn(
                                    0.07f,
                                    0.16f,
                                )
                        },
                        maxTotal = 0.50f,
                    )

                FloatArray(
                    columnCount,
                ) { column ->
                    if (
                        column ==
                        columnCount - 1
                    ) {
                        1f -
                            compact.sum()
                    } else {
                        compact[column]
                    }
                }
            }
        }
    }

    private fun visualLength(
        value: String,
    ): Int {
        var units = 0f

        for (char in value.trim()) {
            units +=
                when {
                    char.isWhitespace() ->
                        0.45f

                    char in
                        ".,:;|/\\-_()[]" ->
                        0.65f

                    else ->
                        1f
                }
        }

        return max(
            1,
            units
                .toInt(),
        )
    }
}
