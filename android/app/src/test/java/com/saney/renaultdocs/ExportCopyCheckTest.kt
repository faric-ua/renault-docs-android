package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCopyCheckTest {
    @Test
    fun delaysStartAtOnceAndStayUnderNinetySeconds() {
        assertEquals(0L, ExportCopyCheck.DELAYS_MS.first())
        assertTrue(ExportCopyCheck.DELAYS_MS.sum() <= 90_000L)
    }

    @Test
    fun mismatchMessageSaysHowMuchWasRead() {
        val text = ExportCopyCheck.mismatchMessage(100L * 1_048_576, 40L * 1_048_576, 6)
        assertTrue(text, text.contains("40,0 МБ із 100,0 МБ"))
        assertTrue(text, text.contains("6 перевірок"))
        assertTrue(ExportCopyCheck.mismatchMessage(5, 5, 6).contains("вміст інший"))
    }

    @Test
    fun summaryNamesVerifiedExistingAndNotReplaced() {
        assertEquals(
            "Готово: 17 із 17 (уже були в папці й збіглися: 8). Каталог не публікувався.",
            ExportCopyCheck.summary(17, 17, 8, 0),
        )
        assertTrue(ExportCopyCheck.summary(16, 17, 0, 1).contains("Не замінено"))
    }

    @Test
    fun folderLabelFormat() {
        assertEquals(
            "Диск · user@gmail.com › Renault Docs Projects",
            SafFolderLabel.format("Диск", "user@gmail.com", "Renault Docs Projects"),
        )
        assertEquals("Диск › Папка", SafFolderLabel.format("Диск", null, "Папка"))
        assertNull(SafFolderLabel.format("Диск", "a@b", " "))
    }

    @Test
    fun accountIsTakenOnlyFromAMatchingRoot() {
        val roots = listOf("acc=1;root" to "one@gmail.com", "acc=2;root" to "two@gmail.com")
        assertEquals("two@gmail.com", SafFolderLabel.accountFromRoots(roots, "acc=2;doc=encoded=xyz"))
        assertNull(SafFolderLabel.accountFromRoots(roots, "doc=encoded=xyz"))
        assertEquals("one@gmail.com", SafFolderLabel.accountFromRoots(listOf("r" to "one@gmail.com"), "x"))
        assertNull(SafFolderLabel.accountFromRoots(emptyList(), "acc=1"))
    }
}
