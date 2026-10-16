package com.saney.renaultdocs

import java.io.File
import java.nio.file.Files
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Archives plan v2, stage 1: read a volume straight from its `.rdpkg`. */
class VolumeArchiveTest {
    private lateinit var dir: File
    private lateinit var volume: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("varch").toFile()
        volume = File(dir, "megane-ii-nt0001").apply { mkdirs() }
        write("rdpkg.json", """{"format":"renault-volume-package-v1","package_id":"megane-ii-nt0001"}""")
        write("renault-dataset.json", """{"volumes":[{}]}""")
        write("_renault/modern-sections.json", "[1,2,3]")
        write("_renault/runtime-ir/sections/v5/118.json", "{\"id\":118}")
        write("Megane II/COMMUN/PDF/SCH/0057_A3.PDF", ByteArray(70_000) { (it % 251).toByte() })
        write("Megane II/RUS/DOCUMENT/aide.pdf", ByteArray(5_000) { (it % 13).toByte() })
        write("Megane II/RUS/DOCUMENT/B-guide.html", "<p>guide</p>")
        write("Megane II/UKR/Зміст — назва.html", "<p>кирилиця</p>")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun write(
        path: String,
        text: String,
    ) = write(path, text.toByteArray(Charsets.UTF_8))

    private fun write(
        path: String,
        bytes: ByteArray,
    ) {
        File(volume, path).apply {
            parentFile.mkdirs()
            writeBytes(bytes)
        }
    }

    /** The same packing as [RdpkgZipWriter]: files only, sorted, level 1 for text, 0 else. */
    private fun pack(
        source: File = volume,
        target: File = File(dir, "out.rdpkg"),
        extra: (ZipOutputStream) -> Unit = {},
    ): File {
        val files =
            source.walkTopDown().filter { it.isFile }.toList()
                .sortedBy { it.relativeTo(source).invariantSeparatorsPath }
        ZipOutputStream(target.outputStream()).use { zip ->
            files.forEach { file ->
                val relative = file.relativeTo(source).invariantSeparatorsPath
                zip.setLevel(RdpkgZipWriterLevels.levelFor(relative))
                zip.putNextEntry(ZipEntry(relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            extra(zip)
        }
        return target
    }

    @Test
    fun everyFileReadsTheSameAsFromTheFolder() {
        VolumeArchive.open(pack()).use { archive ->
            val files = volume.walkTopDown().filter { it.isFile }.toList()
            assertEquals(files.size, archive.fileCount)
            files.forEach { file ->
                val path = file.relativeTo(volume).invariantSeparatorsPath
                val attrs = archive.attributes(path)
                assertNotNull(path, attrs)
                assertFalse(attrs!!.isDirectory)
                assertEquals(file.length(), attrs.size)
                assertArrayEquals(path, file.readBytes(), archive.open(path)!!.use { it.readBytes() })
            }
        }
    }

    @Test
    fun foldersAreMadeUpFromPathsAndListedLikeTheDisk() {
        VolumeArchive.open(pack()).use { archive ->
            assertTrue(archive.attributes("")!!.isDirectory)
            assertTrue(archive.attributes("Megane II/COMMUN/PDF")!!.isDirectory)
            val top = archive.children("")!!.map { it.name }
            // Folders first, then files, by name ignoring case — like the provider's listing.
            assertEquals(listOf("_renault", "Megane II", "rdpkg.json", "renault-dataset.json"), top)
            val docs = archive.children("Megane II/RUS/DOCUMENT")!!
            assertEquals(listOf("aide.pdf", "B-guide.html"), docs.map { it.name })
            assertEquals("Megane II/RUS/DOCUMENT/aide.pdf", docs.first().path)
            assertNull("a file is not a folder", archive.children("rdpkg.json"))
        }
    }

    @Test
    fun missingAndUnsafePathsAreNothing() {
        VolumeArchive.open(pack()).use { archive ->
            assertNull(archive.attributes("nope.json"))
            assertNull(archive.open("nope.json"))
            assertNull(archive.attributes("../rdpkg.json"))
            assertNull(archive.children("missing/folder"))
        }
    }

    @Test
    fun unsafeEntriesAreNeverOffered() {
        val target =
            pack(extra = { zip ->
                zip.putNextEntry(ZipEntry("../evil.txt"))
                zip.write("x".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("ok/../../evil2.txt"))
                zip.write("x".toByteArray())
                zip.closeEntry()
            })
        VolumeArchive.open(target).use { archive ->
            assertNull(archive.attributes("evil.txt"))
            assertNull(archive.attributes("evil2.txt"))
            assertFalse(archive.children("")!!.any { it.name.contains("evil") })
        }
    }

    @Test
    fun namesAreCleanedLikeTheImporterDoes() {
        assertEquals("a/b.pdf", VolumeArchive.cleanEntryName("/a//./b.pdf"))
        assertEquals("a/b.pdf", VolumeArchive.cleanEntryName("a\\b.pdf"))
        assertNull(VolumeArchive.cleanEntryName("a/../b.pdf"))
        assertEquals("", VolumeArchive.cleanEntryName("/"))
    }

    @Test
    fun timeIsTheArchiveFilesTime() {
        val file = pack()
        file.setLastModified(1_700_000_000_000L)
        VolumeArchive.open(file).use { archive ->
            assertEquals(file.lastModified(), archive.attributes("rdpkg.json")!!.lastModified)
            assertEquals(file.lastModified(), archive.attributes("_renault")!!.lastModified)
            assertEquals(file.length(), archive.archiveSize)
        }
    }

    @Test
    fun readSmallReadsTheManifest() {
        VolumeArchive.open(pack()).use { archive ->
            assertArrayEquals(File(volume, "rdpkg.json").readBytes(), archive.readSmall("rdpkg.json"))
            assertNull("too big", archive.readSmall("Megane II/COMMUN/PDF/SCH/0057_A3.PDF", maxBytes = 1000))
        }
    }

    @Test
    fun verifierAcceptsAnExactCopy() {
        val result = VolumeArchiveVerifier.verify(volume, pack())
        assertTrue(result.problem, result.ok)
        assertEquals(volume.walkTopDown().count { it.isFile }, result.fileCount)
    }

    @Test
    fun verifierRefusesAChangedByte() {
        val archive = pack()
        val pdf = File(volume, "Megane II/COMMUN/PDF/SCH/0057_A3.PDF")
        val bytes = pdf.readBytes()
        bytes[40_000] = (bytes[40_000] + 1).toByte()
        pdf.writeBytes(bytes)
        val result = VolumeArchiveVerifier.verify(volume, archive)
        assertFalse(result.ok)
        assertTrue(result.problem!!, result.problem!!.contains("0057_A3.PDF"))
    }

    @Test
    fun verifierRefusesAMissingOrExtraFile() {
        val archive = pack()
        write("new.json", "{}")
        val missing = VolumeArchiveVerifier.verify(volume, archive)
        assertFalse(missing.ok)
        assertTrue(missing.problem!!, missing.problem!!.contains("бракує"))

        File(volume, "new.json").delete()
        File(volume, "_renault/modern-sections.json").delete()
        val extra = VolumeArchiveVerifier.verify(volume, archive)
        assertFalse(extra.ok)
        assertTrue(extra.problem!!, extra.problem!!.contains("зайвий"))
    }

    @Test
    fun verifierRefusesADamagedArchive() {
        val archive = pack()
        val bytes = archive.readBytes()
        // Flip a byte inside the stored (level 0) PDF data — the CRC check must catch it.
        val marker = ByteArray(16) { ((it + 30_000) % 251).toByte() }
        val at = indexOf(bytes, marker)
        assertTrue("marker found", at > 0)
        bytes[at] = (bytes[at] + 1).toByte()
        archive.writeBytes(bytes)
        val result =
            runCatching { VolumeArchiveVerifier.verify(volume, archive) }
                .getOrElse { VolumeArchiveVerifier.Result(false, 0, 0L, it.message) }
        assertFalse(result.ok)
    }

    @Test(expected = OperationCancelledException::class)
    fun verifierStopsWhenCancelled() {
        VolumeArchiveVerifier.verify(volume, pack(), isCancelled = { true })
    }

    @Test
    fun fileCacheCopiesOutOnceAndReusesIt() {
        val cacheDir = File(dir, "cache")
        val cache = VolumeArchiveFileCache(cacheDir)
        VolumeArchive.open(pack()).use { archive ->
            val path = "Megane II/COMMUN/PDF/SCH/0057_A3.PDF"
            val first = cache.get(archive, path)!!
            assertTrue(first.extracted)
            assertArrayEquals(File(volume, path).readBytes(), first.file.readBytes())
            val second = cache.get(archive, path)!!
            assertFalse(second.extracted)
            assertEquals(first.file, second.file)
            assertNull(cache.get(archive, "nope.pdf"))
            assertNull("a folder is not a file", cache.get(archive, "Megane II"))
            assertTrue(cacheDir.listFiles()!!.none { it.name.endsWith(".part") })
        }
    }

    @Test
    fun fileCacheKeyChangesWhenTheArchiveIsRemade() {
        val cache = VolumeArchiveFileCache(File(dir, "cache"))
        val file = pack()
        file.setLastModified(1_600_000_000_000L)
        val before = VolumeArchive.open(file).use { cache.fileFor(it, "rdpkg.json") }
        file.setLastModified(1_650_000_000_000L)
        val after = VolumeArchive.open(file).use { cache.fileFor(it, "rdpkg.json") }
        assertFalse(before == after)
    }

    @Test
    fun fileCacheTrimTouchesOnlyItsOwnFiles() {
        val cacheDir = File(dir, "cache").apply { mkdirs() }
        val foreign = File(cacheDir, "keep-me.txt").apply { writeBytes(ByteArray(50_000)) }
        val cache = VolumeArchiveFileCache(cacheDir, limitBytes = 60_000)
        VolumeArchive.open(pack()).use { archive ->
            cache.get(archive, "Megane II/COMMUN/PDF/SCH/0057_A3.PDF")!!.file.setLastModified(1_000_000_000_000L)
            cache.get(archive, "Megane II/RUS/DOCUMENT/aide.pdf")
            cache.trim()
        }
        assertTrue(foreign.isFile)
        val own = cacheDir.listFiles()!!.filter { it.name.endsWith(VolumeArchiveFileCache.SUFFIX) }
        assertEquals("the oldest (70 000 bytes) went, the newer stayed", 1, own.size)
        assertEquals(5_000L, own.single().length())
    }

    private fun indexOf(
        haystack: ByteArray,
        needle: ByteArray,
    ): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}

/** Mirrors [RdpkgZipWriter.compressionLevelFor] without Android types. */
private object RdpkgZipWriterLevels {
    fun levelFor(path: String): Int {
        val ext = path.substringAfterLast('.', "").lowercase()
        return if (ext in setOf("htm", "html", "js", "css", "json", "txt", "xml", "svg", "csv")) Deflater.BEST_SPEED else Deflater.NO_COMPRESSION
    }
}
