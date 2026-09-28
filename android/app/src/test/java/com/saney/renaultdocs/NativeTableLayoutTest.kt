package com.saney.renaultdocs

import org.junit.Assert.assertTrue
import org.junit.Test

class NativeTableLayoutTest {
    @Test
    fun glossaryUsesCompactCodeColumn() {
        val table =
            NativeTableData(
                rows =
                    listOf(
                        NativeTableRow(
                            cells =
                                listOf(
                                    "СОКРАЩЕНИЯ",
                                    "ПОЛНЫЕ НАИМЕНОВАНИЯ",
                                ),
                            header = true,
                        ),
                        NativeTableRow(
                            cells =
                                listOf(
                                    "B74",
                                    "ХЭТЧБЕК X74",
                                ),
                        ),
                        NativeTableRow(
                            cells =
                                listOf(
                                    "NINAV3",
                                    "СИСТЕМА НАВИГАЦИИ УРОВЕНЬ 3",
                                ),
                        ),
                    ),
            )

        val fractions =
            NativeTableLayout
                .columnFractions(table)

        assertTrue(
            fractions[0] >= 0.16f,
        )
        assertTrue(
            fractions[0] <= 0.27f,
        )
        assertTrue(
            fractions[1] >= 0.73f,
        )
    }

    @Test
    fun longHeaderContributesToCompactColumnWidthWithinCap() {
        val shortHeader =
            NativeTableData(
                rows =
                    listOf(
                        NativeTableRow(
                            cells =
                                listOf(
                                    "CODE",
                                    "DESCRIPTION",
                                ),
                            header = true,
                        ),
                        NativeTableRow(
                            cells =
                                listOf(
                                    "E2",
                                    "УРОВЕНЬ КОМПЛЕКТАЦИИ E2",
                                ),
                        ),
                    ),
            )

        val longHeader =
            NativeTableData(
                rows =
                    listOf(
                        NativeTableRow(
                            cells =
                                listOf(
                                    "ОЧЕНЬ ДЛИННОЕ НАЗВАНИЕ КОЛОНКИ",
                                    "ПОЛНЫЕ НАИМЕНОВАНИЯ",
                                ),
                            header = true,
                        ),
                        NativeTableRow(
                            cells =
                                listOf(
                                    "E2",
                                    "УРОВЕНЬ КОМПЛЕКТАЦИИ E2",
                                ),
                        ),
                    ),
            )

        val shortFractions =
            NativeTableLayout
                .columnFractions(
                    shortHeader,
                )
        val longFractions =
            NativeTableLayout
                .columnFractions(
                    longHeader,
                )

        assertTrue(
            longFractions[0] >
                shortFractions[0],
        )
        assertTrue(
            longFractions[0] <= 0.27f,
        )
        assertTrue(
            longFractions[1] >= 0.73f,
        )
    }

    @Test
    fun connectorDescriptionKeepsMostWidth() {
        val table =
            NativeTableData(
                rows =
                    listOf(
                        NativeTableRow(
                            cells =
                                listOf(
                                    "№",
                                    "mm²",
                                    "код",
                                    "опис",
                                ),
                            header = true,
                        ),
                        NativeTableRow(
                            cells =
                                listOf(
                                    "1",
                                    "0.35",
                                    "LPG",
                                    "+ ЛЕВОГО ГАБАРИТНОГО ОГНЯ Ч/З ПРЕДОХР.",
                                ),
                        ),
                    ),
            )

        val fractions =
            NativeTableLayout
                .columnFractions(table)

        assertTrue(
            fractions[3] > 0.65f,
        )
    }
}
