package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Archives plan v2, stage 2 (v0.5.143): an imported `.rdpkg` is kept as one file only after
 * [VolumePackageCheck] read every entry; TODO №19: a broken ZIP is caught while it is written
 * ([ZipStreamCheck]).
 */
class VolumePackageCheckTest {
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("vpcheck").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private val goodFiles: Map<String, ByteArray> =
        linkedMapOf(
            "rdpkg.json" to
                """{"schema_version":1,"format":"renault-volume-package-v1","package_id":"megane-ii-nt0001","project_id":"p1"}"""
                    .toByteArray(),
            "renault-dataset.json" to
                """{"project_id":"p1","volumes":[{"entrypoint":"Megane II/RUS/index.html"}]}""".toByteArray(),
            "Megane II/RUS/index.html" to "<p>start</p>".toByteArray(),
            "Megane II/COMMUN/PDF/SCH/0057_A3.PDF" to ByteArray(300_000) { (it % 251).toByte() },
            "Megane II/UKR/Зміст.html" to "<p>кирилиця</p>".toByteArray(),
        )

    /** Like [RdpkgZipWriter]: DEFLATED, level 1 for text and 0 for the rest (data descriptors). */
    private fun pack(
        files: Map<String, ByteArray> = goodFiles,
        name: String = "in.rdpkg",
        stored: Boolean = false,
    ): File {
        val target = File(dir, name)
        ZipOutputStream(target.outputStream()).use { zip ->
            files.forEach { (path, bytes) ->
                val entry = ZipEntry(path)
                if (stored) {
                    entry.method = ZipEntry.STORED
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = bytes.size.toLong()
                    entry.crc = CRC32().apply { update(bytes) }.value
                } else {
                    zip.setLevel(if (path.endsWith(".PDF")) Deflater.NO_COMPRESSION else Deflater.BEST_SPEED)
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return target
    }

    private fun expectBad(
        file: File,
        words: String,
    ) {
        try {
            VolumePackageCheck.check(file)
            fail("expected a bad package: $words")
        } catch (error: VolumePackageCheck.BadPackageException) {
            assertTrue("«${error.message}» має містити «$words»", error.message.orEmpty().contains(words))
        }
    }

    @Test
    fun goodPackagePassesWithIdCountAndBytes() {
        var lastDone = -1L
        var lastTotal = 0L
        val result =
            VolumePackageCheck.check(pack(), progress = { done, total ->
                assertTrue(done >= lastDone)
                lastDone = done
                lastTotal = total
            })
        assertEquals("megane-ii-nt0001", result.packageId)
        assertEquals("p1", result.projectId)
        assertEquals(goodFiles.size, result.fileCount)
        assertEquals(goodFiles.values.sumOf { it.size.toLong() }, result.bytes)
        assertEquals(lastTotal, lastDone)
    }

    @Test
    fun storedArchiveFromStageOnePasses() {
        assertEquals(goodFiles.size, VolumePackageCheck.check(pack(stored = true)).fileCount)
    }

    @Test
    fun damagedBytesInsideAnEntryFail() {
        val file = pack()
        // Flip bytes in the middle of the big PDF entry (stored deflate blocks).
        RandomAccessFile(file, "rw").use { raf ->
            val at = file.length() / 2
            raf.seek(at)
            val b = ByteArray(16)
            raf.readFully(b)
            raf.seek(at)
            raf.write(b.map { (it.toInt() xor 0x5A).toByte() }.toByteArray())
        }
        expectBad(file, "пошкоджений")
    }

    @Test
    fun cutOffFileFails() {
        val file = pack()
        val cut = File(dir, "cut.rdpkg")
        cut.writeBytes(file.readBytes().copyOf((file.length() * 2 / 3).toInt()))
        try {
            VolumePackageCheck.check(cut)
            fail("expected a failure")
        } catch (error: Exception) {
            assertTrue(error is VolumePackageCheck.BadPackageException || error is java.io.IOException)
        }
    }

    @Test
    fun missingManifestFails() {
        expectBad(pack(goodFiles - "rdpkg.json"), "rdpkg.json")
    }

    @Test
    fun wrongFormatFails() {
        val files = LinkedHashMap(goodFiles)
        files["rdpkg.json"] = """{"schema_version":1,"format":"other","package_id":"x"}""".toByteArray()
        expectBad(pack(files), "Це не Renault Docs")
    }

    @Test
    fun unsafePackageIdFails() {
        val files = LinkedHashMap(goodFiles)
        files["rdpkg.json"] = """{"schema_version":1,"format":"renault-volume-package-v1","package_id":"../x"}""".toByteArray()
        expectBad(pack(files), "package_id")
    }

    @Test
    fun missingStartPageFails() {
        expectBad(pack(goodFiles - "Megane II/RUS/index.html"), "стартову сторінку")
    }

    @Test
    fun twoVolumesFail() {
        val files = LinkedHashMap(goodFiles)
        files["renault-dataset.json"] =
            """{"volumes":[{"entrypoint":"Megane II/RUS/index.html"},{"entrypoint":"b"}]}""".toByteArray()
        expectBad(pack(files), "рівно один том")
    }

    @Test
    fun projectIdMismatchFails() {
        val files = LinkedHashMap(goodFiles)
        files["renault-dataset.json"] =
            """{"project_id":"other","volumes":[{"entrypoint":"Megane II/RUS/index.html"}]}""".toByteArray()
        expectBad(pack(files), "project_id")
    }

    @Test
    fun unsafePathFails() {
        val files = LinkedHashMap(goodFiles)
        files["../evil.txt"] = "x".toByteArray()
        expectBad(pack(files), "небезпечний шлях")
    }

    @Test
    fun fileTwiceFails() {
        val target = File(dir, "twice.rdpkg")
        ZipOutputStream(target.outputStream()).use { zip ->
            goodFiles.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
            // Same path after cleaning (backslash) — ZipOutputStream allows the different name.
            zip.putNextEntry(ZipEntry("Megane II\\RUS\\index.html"))
            zip.write("again".toByteArray())
            zip.closeEntry()
        }
        expectBad(target, "двічі")
    }

    @Test
    fun cancelStopsTheCheck() {
        try {
            VolumePackageCheck.check(pack(), isCancelled = { true })
            fail("expected cancel")
        } catch (expected: OperationCancelledException) {
        }
    }

    @Test
    fun checkedPackageReadsAsVolumeArchive() {
        val file = pack()
        VolumePackageCheck.check(file)
        VolumeArchive.open(file).use { archive ->
            assertEquals(goodFiles.size, archive.fileCount)
            goodFiles.forEach { (path, bytes) ->
                assertTrue(path, archive.open(path)!!.use { it.readBytes() }.contentEquals(bytes))
            }
        }
    }

    // --- TODO №19: ZipStreamCheck -------------------------------------------------------------

    private fun writeThroughCheck(
        files: Map<String, ByteArray>,
        damage: ((ByteArray) -> ByteArray)? = null,
    ): Pair<ByteArray, ZipStreamCheck> {
        val packed = ByteArrayOutputStream()
        ZipOutputStream(packed).use { zip ->
            files.forEach { (path, bytes) ->
                zip.setLevel(Deflater.BEST_SPEED)
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val bytes = damage?.invoke(packed.toByteArray()) ?: packed.toByteArray()
        val destination = ByteArrayOutputStream()
        val check = ZipStreamCheck()
        TeeOutputStream(destination, check.output).use { tee ->
            // Small pieces, like a real writer.
            var at = 0
            while (at < bytes.size) {
                val n = minOf(7_000, bytes.size - at)
                tee.write(bytes, at, n)
                at += n
            }
        }
        assertTrue(destination.toByteArray().contentEquals(bytes))
        return bytes to check
    }

    @Test
    fun streamCheckPassesAGoodZipAndCountsFiles() {
        val (_, check) = writeThroughCheck(goodFiles)
        check.use { assertEquals(goodFiles.size, it.finish(expectedFiles = goodFiles.size)) }
    }

    @Test
    fun streamCheckCatchesDamagedBytes() {
        val big = linkedMapOf("a.bin" to ByteArray(2_000_000) { (it * 31 % 253).toByte() }, "b.txt" to "b".toByteArray())
        val (_, check) =
            writeThroughCheck(big) { bytes ->
                bytes.copyOf().also { copy ->
                    for (i in 0 until 64) copy[copy.size / 3 + i] = (copy[copy.size / 3 + i].toInt() xor 0x33).toByte()
                }
            }
        check.use {
            try {
                it.finish(expectedFiles = 2)
                fail("expected a damaged file")
            } catch (error: DamagedOutputException) {
                assertTrue(error.message.orEmpty().contains("пошкоджений"))
                assertTrue(error.message.orEmpty().contains("Сам том не змінено"))
            }
        }
    }

    @Test
    fun streamCheckCatchesAWrongFileCount() {
        val (_, check) = writeThroughCheck(goodFiles)
        check.use {
            try {
                it.finish(expectedFiles = goodFiles.size + 1)
                fail("expected a count mismatch")
            } catch (error: DamagedOutputException) {
                assertTrue(error.message.orEmpty().contains("замість"))
            }
        }
    }

    @Test
    fun streamCheckDoesNotBlockTheWriterOnGarbage() {
        val destination = ByteArrayOutputStream()
        val check = ZipStreamCheck()
        val garbage = ByteArray(10_000_000) { (it % 7).toByte() }
        TeeOutputStream(destination, check.output).use { it.write(garbage) }
        assertEquals(garbage.size, destination.size())
        try {
            check.finish(expectedFiles = 1)
            fail("garbage is not a zip with one file")
        } catch (expected: DamagedOutputException) {
        } finally {
            check.close()
        }
    }
}
