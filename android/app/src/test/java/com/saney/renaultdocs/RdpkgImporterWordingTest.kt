package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class RdpkgImporterWordingTest {
    @Test
    fun ukrainianFileCountUsesCorrectSingularAndPluralForms() {
        assertEquals(
            "1 файл",
            RdpkgImporter.ukrainianFileCount(1),
        )
        assertEquals(
            "2 файли",
            RdpkgImporter.ukrainianFileCount(2),
        )
        assertEquals(
            "4 файли",
            RdpkgImporter.ukrainianFileCount(4),
        )
        assertEquals(
            "5 файлів",
            RdpkgImporter.ukrainianFileCount(5),
        )
        assertEquals(
            "11 файлів",
            RdpkgImporter.ukrainianFileCount(11),
        )
        assertEquals(
            "21 файл",
            RdpkgImporter.ukrainianFileCount(21),
        )
        assertEquals(
            "22 файли",
            RdpkgImporter.ukrainianFileCount(22),
        )
        assertEquals(
            "25 файлів",
            RdpkgImporter.ukrainianFileCount(25),
        )
    }
}
