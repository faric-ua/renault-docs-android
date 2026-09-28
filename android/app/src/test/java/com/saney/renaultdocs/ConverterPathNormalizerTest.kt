package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConverterPathNormalizerTest {
    private val files = setOf(
        "Version/RUS/HTM/SCH/A.HTM",
        "Version/COMMUN/PDF/SCH/0207_A3.PDF",
        "Version/COMMUN/GIF/icon.GIF",
        "Version/COMMUN/HTM/NEXT.HTM",
    )

    @Test
    fun staticCaseIsNormalizedAndFragmentIsPreserved() {
        val result = ConverterPathNormalizer.patchText(
            sourceRelativePath = "Version/RUS/HTM/SCH/A.HTM",
            input = "<a href=\"../../../COMMUN/PDF/SCH/0207_A3.pdf#viewrect=1,2,3,4\">x</a>",
            exactFiles = files,
        )

        assertTrue(
            result.text.contains(
                "../../../COMMUN/PDF/SCH/0207_A3.PDF#viewrect=1,2,3,4"
            )
        )
        assertEquals(1, result.changes.size)
        assertEquals("static_case", result.changes.single().kind)
    }

    @Test
    fun externalUrlIsNotChanged() {
        val input = "<a href=\"https://example.com/test.pdf\">x</a>"
        val result = ConverterPathNormalizer.patchText(
            sourceRelativePath = "Version/RUS/HTM/SCH/A.HTM",
            input = input,
            exactFiles = files,
        )

        assertEquals(input, result.text)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun dynamicPrintSuffixesAreFixedButCommentsStayUntouched() {
        val result = ConverterPathNormalizer.patchText(
            sourceRelativePath = "Version/COMMUN/JS/VISU.JS",
            input = "// old = \"_print\";\nx = \"_print\";\ny = \"_printNB\";\n",
            exactFiles = files,
        )

        assertTrue(result.text.contains("// old = \"_print\";"))
        assertTrue(result.text.contains("x = \"_PRINT\";"))
        assertTrue(result.text.contains("y = \"_PRINTNB\";"))
        assertEquals(
            2,
            result.changes.count { it.kind == "dynamic_print_case" },
        )
    }
}
