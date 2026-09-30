package com.saney.renaultdocs

import java.util.Locale

/**
 * UI-only ordering for the Modern section list.
 *
 * The converter/runtime contract keeps Renault section identifiers opaque and
 * preserves Classic source order. This helper changes only presentation order.
 */
object ModernSectionDisplayOrder {
    private val tokenRegex =
        Regex("""\d+|\D+""")

    fun sorted(
        sections: List<ModernSection>,
    ): List<ModernSection> =
        sections
            .withIndex()
            .sortedWith {
                left,
                right ->
                val byCode =
                    compareCodes(
                        left.value.code,
                        right.value.code,
                    )

                if (byCode != 0) {
                    byCode
                } else {
                    left.index.compareTo(
                        right.index,
                    )
                }
            }
            .map {
                it.value
            }

    internal fun compareCodes(
        left: String,
        right: String,
    ): Int {
        val byGroup =
            displayGroup(left).compareTo(
                displayGroup(right),
            )

        if (byGroup != 0) {
            return byGroup
        }

        val leftTokens =
            tokens(left)
        val rightTokens =
            tokens(right)

        val common =
            minOf(
                leftTokens.size,
                rightTokens.size,
            )

        for (index in 0 until common) {
            val a = leftTokens[index]
            val b = rightTokens[index]

            val compared =
                compareToken(
                    a,
                    b,
                )

            if (compared != 0) {
                return compared
            }
        }

        val byTokenCount =
            leftTokens.size.compareTo(
                rightTokens.size,
            )

        if (byTokenCount != 0) {
            return byTokenCount
        }

        return left
            .uppercase(
                Locale.ROOT,
            )
            .compareTo(
                right.uppercase(
                    Locale.ROOT,
                )
            )
    }

    private fun displayGroup(
        value: String,
    ): Int {
        val normalized =
            value.trim()
                .uppercase(
                    Locale.ROOT,
                )

        return when {
            normalized.firstOrNull()
                ?.isDigit() == true ->
                0

            normalized.startsWith(
                "R",
            ) ->
                1

            else ->
                2
        }
    }

    private fun tokens(
        value: String,
    ): List<String> =
        tokenRegex
            .findAll(
                value.trim()
                    .uppercase(
                        Locale.ROOT,
                    ),
            )
            .map {
                it.value
            }
            .toList()

    private fun compareToken(
        left: String,
        right: String,
    ): Int {
        val leftNumeric =
            left.all {
                it.isDigit()
            }
        val rightNumeric =
            right.all {
                it.isDigit()
            }

        if (
            leftNumeric &&
            rightNumeric
        ) {
            return compareNumericText(
                left,
                right,
            )
        }

        if (
            leftNumeric !=
            rightNumeric
        ) {
            return if (
                leftNumeric
            ) {
                -1
            } else {
                1
            }
        }

        return left.compareTo(
            right,
        )
    }

    private fun compareNumericText(
        left: String,
        right: String,
    ): Int {
        val normalizedLeft =
            left.trimStart(
                '0',
            ).ifEmpty {
                "0"
            }
        val normalizedRight =
            right.trimStart(
                '0',
            ).ifEmpty {
                "0"
            }

        val byLength =
            normalizedLeft.length.compareTo(
                normalizedRight.length,
            )

        if (byLength != 0) {
            return byLength
        }

        val byValue =
            normalizedLeft.compareTo(
                normalizedRight,
            )

        if (byValue != 0) {
            return byValue
        }

        return left.length.compareTo(
            right.length,
        )
    }
}
