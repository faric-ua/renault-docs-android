package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveSourceGuardTest {
    private val megane = RenaultProject("megane-ii", "Megane II", "Megane II")
    private val kangoo = RenaultProject("kangoo-ii", "Kangoo II", "Kangoo II")
    private val laguna = RenaultProject("laguna-ii", "Laguna II", "Laguna II")

    @Test fun preparedPackageIsNeverAnOriginalArchive() {
        val original = "Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg"
        assertTrue(ArchiveSourceGuard.inputError(original, megane)!!.contains("готовий .rdpkg"))
        assertTrue(ArchiveSourceGuard.inputError("file.RDPKG", kangoo)!!.contains("готовий .rdpkg"))
    }

    @Test fun explicitKangooSourceCannotBeConvertedToMeganeOrLaguna() {
        val name = "KangooII X61_NT8486_Visu v5.0_2009.08.31(RUS).zip"
        assertNotNull(ArchiveSourceGuard.inputError(name, megane))
        assertNotNull(ArchiveSourceGuard.inputError(name, laguna))
        assertNull(ArchiveSourceGuard.inputError(name, kangoo))
        assertNotNull(ArchiveSourceGuard.rootConflict(listOf("KangooII_X61_NT8486"), megane))
    }

    @Test fun originalMatchingAndUnmarkedArchivesWork() {
        assertNull(ArchiveSourceGuard.inputError("Megane-II_NT8340A.zip", megane))
        assertNull(ArchiveSourceGuard.inputError("NT8340A_2006-04-18.7z", megane))
        assertNull(ArchiveSourceGuard.inputError("NT8340A_2006-04-18.rar", megane))
        assertNotNull(ArchiveSourceGuard.inputError("some.pdf", megane))
    }

    @Test fun platformCodeAloneDoesNotIdentifyVehicle() {
        assertNull(ArchiveSourceGuard.inputError("X61_NT8486.zip", megane))
        assertEquals(emptySet<String>(), ArchiveSourceGuard.explicitModels("X61_NT8486"))
    }

    @Test fun catalogShowsPossibilitiesNotByteIdentity() {
        val matches = listOf(
            ArchiveSourceGuard.CatalogMatch("kangoo-ii", "Kangoo II", "NT8486", true),
            ArchiveSourceGuard.CatalogMatch("megane-ii", "Megane II", "NT8486", false),
        )
        val value = ArchiveSourceGuard.summary(matches, "megane-ii")
        assertTrue(value.contains("Kangoo II"))
        assertTrue(value.contains("Megane II (поточний)"))
        assertTrue(value.contains("можливий дублікат"))
    }
}
