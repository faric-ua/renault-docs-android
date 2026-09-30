package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class ModernSectionDisplayOrderTest {
    @Test
    fun naturalSortKeepsOpaqueCodesAndOrdersNumericParts() {
        val sections =
            listOf(
                section("R70"),
                section("R15"),
                section("1013"),
                section("338"),
                section("321"),
                section("853"),
                section("101_2"),
                section("101"),
                section("101_1"),
                section("MAH"),
                section("NT"),
            )

        val sorted =
            ModernSectionDisplayOrder
                .sorted(
                    sections,
                )
                .map {
                    it.code
                }

        assertEquals(
            listOf(
                "101",
                "101_1",
                "101_2",
                "321",
                "338",
                "853",
                "1013",
                "R15",
                "R70",
                "MAH",
                "NT",
            ),
            sorted,
        )
    }

    @Test
    fun duplicateDisplayCodesKeepTheirSourceOrder() {
        val first =
            ModernSection(
                code = "101",
                title = "FIRST",
                entrypoint = "A/101_a.HTM",
            )
        val second =
            ModernSection(
                code = "101",
                title = "SECOND",
                entrypoint = "A/101_b.HTM",
            )

        val sorted =
            ModernSectionDisplayOrder
                .sorted(
                    listOf(
                        second,
                        section("103"),
                        first,
                    ),
                )

        assertEquals(
            listOf(
                second.entrypoint,
                first.entrypoint,
                "A/103.HTM",
            ),
            sorted.map {
                it.entrypoint
            },
        )
    }

    private fun section(
        code: String,
    ): ModernSection =
        ModernSection(
            code = code,
            title = "Section $code",
            entrypoint = "A/$code.HTM",
        )
}
