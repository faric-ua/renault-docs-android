package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale

/** v0.5.149: «Знайдено файлів: N» while listing (TODO №21 п. 2) and leftovers (TODO №10). */
class ListingAndLeftoversTest {
    private fun tempDir(): File = Files.createTempDirectory("listing-test").toFile()

    @Test
    fun listFilesKeepsPackageOrderAndReportsCount() {
        val root = tempDir()
        val names = listOf("b/Z.html", "a/y.html", "B/x.html", "rdpkg.json", "a/A.pdf") +
            (1..250).map { "data/p$it.htm" }
        names.forEach { File(root, it).apply { parentFile.mkdirs(); writeText(it) } }
        val reports = mutableListOf<Int>()

        val files = VolumeArchiveWriter.listFiles(root) { reports += it }

        // Same order the writers used before v0.5.149 (path lower case, then exact).
        val old =
            root.walkTopDown().filter { it.isFile }.toList().sortedWith(
                compareBy<File> { it.relativeTo(root).invariantSeparatorsPath.lowercase(Locale.ROOT) }
                    .thenBy { it.relativeTo(root).invariantSeparatorsPath },
            )
        assertEquals(old, files)
        assertEquals(255, files.size)
        assertEquals(listOf(100, 200, 255), reports)
    }

    @Test
    fun listingProgressReadsAsCounterOnTheCard() {
        // Same words RdpkgExporter.listingProgress uses (RdpkgExporter itself needs Android).
        val progress = OperationProgress(stage = "Шукаю файли тому…", itemCurrent = 1234, itemLabel = "Знайдено файлів")
        assertEquals("Шукаю файли тому… · Знайдено файлів: 1234", progress.displayText())
        assertTrue(!progress.isDeterminate)
        val parts = OperationStatusDetailFormatter.split(progress.displayText())
        assertEquals("Шукаю файли тому…", parts.stage)
        assertEquals("Знайдено файлів: 1234", parts.counter)
    }

    @Test
    fun leftoversAreOnlyUnfinishedWork() {
        val base = tempDir()
        val volumes = File(base, "rdpkg").apply { mkdirs() }
        val archives = File(base, "rdpkg-archive").apply { mkdirs() }
        File(volumes, "NT8227A").mkdirs()
        File(volumes, ".import-1111").mkdirs()
        File(volumes, ".backup-NT8228A").mkdirs()
        File(volumes, ".nomedia").writeText("")
        File(archives, "NT8266A.rdpkg").writeText("zip")
        File(archives, ".import-2222.part").writeText("half")
        File(archives, "NT8267A.rdpkg.part").writeText("half")
        File(archives, ".backup-NT8275A.rdpkg").writeText("old")

        val found = VolumeTrashCore.findLeftovers(listOf(volumes)).map { it.name }

        assertEquals(listOf(".import-1111", ".import-2222.part", "NT8267A.rdpkg.part"), found)
    }

    @Test
    fun leftoversWithoutFoldersAreEmpty() {
        assertTrue(VolumeTrashCore.findLeftovers(listOf(File(tempDir(), "rdpkg"))).isEmpty())
    }
}

