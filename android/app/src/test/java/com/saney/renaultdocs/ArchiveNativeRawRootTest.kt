package com.saney.renaultdocs

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveNativeRawRootTest {
    @Test
    fun actualNestedRenaultLayoutResolvesWithoutMovingOriginalArchive() {
        val temp = createTempDirectory("renault-nt8298a-").toFile()
        try {
            val archive = File(temp, "Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip")
            ZipOutputStream(archive.outputStream()).use { out ->
                val folder = "Megane II B,C,S 84/NT8298A/"
                for (name in listOf(
                    folder + "INDEX.HTM",
                    folder + "COMMUN/img.bin",
                    folder + "RUS/menu.htm",
                )) {
                    out.putNextEntry(ZipEntry(name))
                    out.write(name.toByteArray())
                    out.closeEntry()
                }
            }
            val extractionRoot = File(temp, "extracted")
            val result = ArchiveIntake.extract(archive, extractionRoot)
            assertEquals(1, result.rawRoots.size)
            val actual = result.rawRoots.single()
            assertEquals(actual.canonicalFile, ArchiveNativeRawRoot.resolve(extractionRoot))
            assertEquals(actual.canonicalFile, ArchiveNativeRawRoot.resolve(actual))

            assertTrue(File(actual, "INDEX.HTM").isFile)
            assertTrue(File(actual, "COMMUN/img.bin").isFile)
            assertTrue(File(actual, "RUS/menu.htm").isFile)
            assertTrue(archive.isFile)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun ambiguousWrapperIsNeverFlattenedOrChosenArbitrarily() {
        val root = createTempDirectory("renault-many-roots-").toFile()
        try {
            File(root, "ONE").mkdirs()
            File(root, "TWO").mkdirs()
            File(root, "ONE/INDEX.HTM").writeText("one")
            File(root, "TWO/INDEX.HTM").writeText("two")
            try {
                ArchiveNativeRawRoot.resolve(root)
                fail("Ambiguous multi-volume source must not auto-select")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains("кілька вкладених"))
            }
            assertTrue(File(root, "ONE/INDEX.HTM").exists())
            assertTrue(File(root, "TWO/INDEX.HTM").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun noEntrypointOrPreparedDatasetDoesNotPassAsRaw() {
        val root = createTempDirectory("renault-not-raw-").toFile()
        try {
            File(root, "RUS").mkdirs()
            File(root, "RUS/text.htm").writeText("not an INDEX")
            assertFalse(ArchiveNativeRawRoot.hasDirectEntrypoint(root))
            try {
                ArchiveNativeRawRoot.resolve(root)
                fail("No Renault entrypoint must not be accepted")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains("не знайдено"))
            }
            val status = ArchiveNativeRawRoot.describeMismatch(
                root,
                setOf("RUS/text.htm", "COMMUN/code.js"),
            )
            assertTrue(status.contains("знайдені INDEX: не знайдено"))
            assertFalse(status.contains(root.absolutePath))
            assertFalse(status.contains("content://"))
        } finally {
            root.deleteRecursively()
        }
    }
}
