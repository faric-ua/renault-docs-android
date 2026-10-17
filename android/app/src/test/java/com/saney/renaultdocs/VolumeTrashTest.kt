package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VolumeTrashTest {
    private fun tempDir(): File = Files.createTempDirectory("trash-test").toFile()

    private fun volumeFolder(root: File, id: String, files: Int): File {
        val folder = File(root, id)
        repeat(files) { i ->
            val sub = File(folder, "data/part${i % 3}")
            sub.mkdirs()
            File(sub, "page$i.html").writeText("x".repeat(i + 1))
        }
        File(folder, "rdpkg.json").writeText("{}")
        return folder
    }

    @Test
    fun moveInRenamesIntoTrashWithNomediaAndKeepsNeighbours() {
        val base = tempDir()
        val volumes = File(base, "rdpkg").apply { mkdirs() }
        val folder = volumeFolder(volumes, "NT8227A", 10)
        val neighbour = volumeFolder(volumes, "NT8228A", 3)
        val trash = VolumeTrashCore.trashDirFor(volumes)
        assertEquals(File(base, "rdpkg-trash"), trash)

        val moved = VolumeTrashCore.moveIn(folder, trash, 123L)
        assertNotNull(moved)
        assertFalse(folder.exists())
        assertTrue(File(trash, ".nomedia").isFile)
        assertTrue(File(moved, "rdpkg.json").isFile)
        assertTrue(neighbour.isDirectory)
        assertEquals(1, VolumeTrashCore.pendingItems(listOf(trash)))

        // Same name, same stamp again — never overwrites what is already in the trash.
        val again = volumeFolder(volumes, "NT8227A", 2)
        val moved2 = VolumeTrashCore.moveIn(again, trash, 123L)
        assertNotNull(moved2)
        assertTrue(moved2 != moved)
        assertEquals(2, VolumeTrashCore.pendingItems(listOf(trash)))
    }

    @Test
    fun moveInOfMissingItemChangesNothing() {
        val base = tempDir()
        val trash = File(base, "rdpkg-trash")
        assertNull(VolumeTrashCore.moveIn(File(base, "rdpkg/NOPE"), trash, 1L))
        assertEquals(0, VolumeTrashCore.pendingItems(listOf(trash)))
    }

    @Test
    fun eraseAllEmptiesOnlyTheTrash() {
        val base = tempDir()
        val volumes = File(base, "rdpkg").apply { mkdirs() }
        val archives = File(base, "rdpkg-archive").apply { mkdirs() }
        val keep = volumeFolder(volumes, "NT8342A", 5)
        val keepArchive = File(archives, "NT8342A.rdpkg").apply { writeText("zip") }
        val trash = VolumeTrashCore.trashDirFor(volumes)
        VolumeTrashCore.moveIn(volumeFolder(volumes, "NT8227A", 40), trash, 1L)
        VolumeTrashCore.moveIn(File(archives, "NT8228A.rdpkg").apply { writeText("zip") }, trash, 1L)

        val reports = mutableListOf<VolumeTrashCore.EraseProgress>()
        val result = VolumeTrashCore.eraseAll(listOf(trash), reportEvery = 10) { reports += it }

        assertEquals(42L, result.deletedFiles) // 40 pages + rdpkg.json + one archive
        assertEquals(0, result.leftItems)
        assertFalse(result.cancelled)
        assertTrue(trash.isDirectory)
        assertTrue(File(trash, ".nomedia").isFile)
        assertEquals(0, VolumeTrashCore.pendingItems(listOf(trash)))
        // Two volumes: each starts with «counting», then a known number of files.
        // Counting reports at the start of each volume (+ every 250 found — not reached here).
        assertEquals(2, reports.count { it.counting })
        assertTrue(reports.all { it.itemTotal == 2 })
        assertEquals(setOf(1, 41), reports.filter { !it.counting }.map { it.itemFiles }.toSet())
        assertEquals(42L, reports.last().deletedFiles)
        assertEquals(1.0, reports.last().fraction, 1e-9)
        // The share done never goes back.
        val fractions = reports.map { it.fraction }
        assertEquals(fractions, fractions.sorted())
        // Nothing outside the trash was touched.
        assertTrue(File(keep, "rdpkg.json").isFile)
        assertTrue(keepArchive.isFile)
    }

    @Test
    fun countingReportsFoundFiles() {
        val base = tempDir()
        val volumes = File(base, "rdpkg").apply { mkdirs() }
        val trash = VolumeTrashCore.trashDirFor(volumes)
        VolumeTrashCore.moveIn(volumeFolder(volumes, "BIG", 600), trash, 1L)
        val reports = mutableListOf<VolumeTrashCore.EraseProgress>()
        VolumeTrashCore.eraseAll(listOf(trash)) { reports += it }
        val counting = reports.filter { it.counting }.map { it.itemDone }
        assertEquals(listOf(0, 250, 500), counting)
    }

    @Test
    fun cancelledEraseLeavesTheRestForNextStart() {
        val base = tempDir()
        val volumes = File(base, "rdpkg").apply { mkdirs() }
        val trash = VolumeTrashCore.trashDirFor(volumes)
        VolumeTrashCore.moveIn(volumeFolder(volumes, "A", 30), trash, 1L)
        VolumeTrashCore.moveIn(volumeFolder(volumes, "B", 30), trash, 1L)
        var calls = 0
        val first = VolumeTrashCore.eraseAll(listOf(trash), isCancelled = { ++calls > 20 })
        assertTrue(first.cancelled)
        assertTrue(first.leftItems >= 1)
        val second = VolumeTrashCore.eraseAll(listOf(trash))
        assertFalse(second.cancelled)
        assertEquals(0, second.leftItems)
        assertEquals(62L, first.deletedFiles + second.deletedFiles)
    }

    @Test
    fun eraseDoesNotFollowSymbolicLinks() {
        val base = tempDir()
        val outside = File(base, "outside").apply { mkdirs() }
        val precious = File(outside, "keep.txt").apply { writeText("keep") }
        val trash = File(base, "rdpkg-trash").apply { mkdirs() }
        val item = File(trash, "X-1").apply { mkdirs() }
        File(item, "a.txt").writeText("a")
        val linked =
            runCatching { Files.createSymbolicLink(File(item, "link").toPath(), outside.toPath()) }.isSuccess
        VolumeTrashCore.eraseAll(listOf(trash))
        assertFalse(item.exists())
        if (linked) assertTrue(precious.isFile)
    }

    @Test
    fun remainingTimeFromTheSpeedSoFar() {
        // Volume 2 of 4, half done → 37.5 % after 30 s → ~50 s left.
        val p = VolumeTrashCore.EraseProgress(2, 4, 100, 50, 150, 30_000L)
        assertEquals(0.375, p.fraction, 1e-9)
        assertEquals(50_000L, p.remainingMs)
        assertNull(VolumeTrashCore.EraseProgress(1, 4, 100, 1, 1, 30_000L).remainingMs) // too early
        assertNull(VolumeTrashCore.EraseProgress(2, 4, 100, 50, 150, 2_000L).remainingMs) // too soon
        assertEquals("Том 2 з 4 · 37 % · залишилось менше хвилини", VolumeDeleteChoice.eraseCounter(p))
        assertEquals("Том 3 з 4 · рахую файли…", VolumeDeleteChoice.eraseCounter(VolumeTrashCore.EraseProgress(3, 4, -1, 0, 9, 1_000L)))
        // v0.5.151 (TODO №24): how many found so far while counting.
        assertEquals("Том 3 з 4 · рахую файли… 1250", VolumeDeleteChoice.eraseCounter(VolumeTrashCore.EraseProgress(3, 4, -1, 1250, 9, 1_000L)))
        assertEquals(
            "Рахую файли тому 3… знайдено: 1250. Стерто вже: 9.",
            VolumeDeleteChoice.eraseDetail(VolumeTrashCore.EraseProgress(3, 4, -1, 1250, 9, 1_000L)),
        )
        assertEquals("~12 хв", VolumeDeleteChoice.durationLabel(12 * 60_000L))
        assertEquals("~1 год 05 хв", VolumeDeleteChoice.durationLabel(65 * 60_000L))
        assertEquals("менше хвилини", VolumeDeleteChoice.durationLabel(59_000L))
        assertTrue(VolumeDeleteChoice.eraseDoneDetail(5000, 23, 14 * 60_000L, 0).startsWith("Стерто файлів: 5000 (23 томи) за 14 хв."))
    }

    @Test
    fun defaultSelectionTicksOnlyUnpackedFoldersOnTheCard() {
        val items =
            listOf(
                VolumeDeleteItem("1", "NT8227A · 2005", "p1", onCard = true, unpackedFolder = true, usedInOtherProject = false),
                VolumeDeleteItem("2", "NT8183A · 2002", "p2", onCard = true, unpackedFolder = false, usedInOtherProject = false),
                VolumeDeleteItem("3", "NT8339A · 2006", "p3", onCard = false, unpackedFolder = true, usedInOtherProject = false),
                VolumeDeleteItem("4", "NT8340A · 2006", "p4", onCard = true, unpackedFolder = true, usedInOtherProject = true),
                VolumeDeleteItem("5", "SAF", null, onCard = false, unpackedFolder = false, usedInOtherProject = false),
            )
        assertEquals(setOf("1"), VolumeDeleteChoice.defaultSelection(items))
        assertTrue(items[3].label.isNotBlank() && !items[3].erasesFiles)
        assertFalse(items[4].erasesFiles)
        assertTrue(VolumeDeleteChoice.placeLabel(items[0]).contains("карта"))
        assertTrue(VolumeDeleteChoice.placeLabel(items[4]).contains("лише запис"))
    }

    @Test
    fun wordsForTheOwner() {
        assertEquals("1 том", VolumeDeleteChoice.volumeCount(1))
        assertEquals("3 томи", VolumeDeleteChoice.volumeCount(3))
        assertEquals("18 томів", VolumeDeleteChoice.volumeCount(18))
        assertEquals("Видалити 18 томів назавжди?", VolumeDeleteChoice.confirmTitle(18))
        val item = VolumeDeleteItem("1", "NT8227A · 2005-03-01", "p1", true, true, false)
        val message = VolumeDeleteChoice.confirmMessage("Megane II", listOf(item))
        assertTrue(message.contains("• NT8227A · 2005-03-01"))
        assertTrue(message.contains("Megane II"))
        assertTrue(message.contains("імпортом заново"))
        val result =
            VolumeDeleteChoice.resultMessage(VolumeDeleteOutcome(listOf("NT8227A"), emptyList(), listOf("NT8228A (x)")))
        assertTrue(result.startsWith("Видалено з проєкту: 1 том."))
        assertTrue(result.contains("Файли 1 тому стираються"))
        val many = VolumeDeleteChoice.resultMessage(VolumeDeleteOutcome(List(23) { "NT$it" }, emptyList(), emptyList()))
        assertTrue(many.contains("Файли 23 томів стираються"))
        assertTrue(result.contains("стираються у фоні"))
        assertTrue(result.contains("NT8228A (x)"))
    }
}
