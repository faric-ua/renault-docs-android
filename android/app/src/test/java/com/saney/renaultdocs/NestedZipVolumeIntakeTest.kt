package com.saney.renaultdocs

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NestedZipVolumeIntakeTest {
    private fun archive(destination: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(destination.outputStream()).use { zip ->
            entries.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    @Test
    fun outerZipWithThreeInnerArchivesFindsThreeSeparateRawRoots() {
        val work = createTempDirectory("renault-nested-zip-").toFile()
        try {
            val nested = listOf("NT8266", "NT8228A", "NT8274")
                .map { code ->
                    File(work, code + ".zip").also { inner ->
                        archive(
                            inner,
                            mapOf(
                                "folder_" + code + "/index.html" to
                                    ("<html>" + code + "</html>").toByteArray(),
                                "folder_" + code + "/data/page.htm" to
                                    "payload".toByteArray(),
                            ),
                        )
                    }
                }
            val outer = File(work, "Megane IIx.zip")
            archive(
                outer,
                nested.associate {
                    ("archives/" + it.name) to it.readBytes()
                },
            )
            val stage = File(work, "extracted")
            val outerResult = ArchiveIntake.extract(
                source = outer,
                extractionRoot = stage,
                allowNoRawRoots = true,
            )
            assertTrue(outerResult.rawRoots.isEmpty())

            val result = NestedZipVolumeIntake.expandOneLevel(
                extractionRoot = stage,
                outerEntries = outerResult.files + outerResult.directories,
                outerExpandedBytes = outerResult.extractedBytes,
            ) ?: error("Expected three embedded ZIPs")

            assertEquals(3, result.rawRoots.size)
            assertEquals(
                setOf("folder_NT8266", "folder_NT8228A", "folder_NT8274"),
                result.rawRoots.map { it.name }.toSet(),
            )
            assertTrue(result.rawRoots.all { File(it, "index.html").isFile })
            assertTrue(result.rawRoots.all { it.canonicalPath.startsWith(stage.canonicalPath) })
            assertTrue(nested.all { it.isFile }) // user originals remain unchanged
        } finally {
            work.deleteRecursively()
        }
    }

    @Test
    fun rejectsZipSlipInsideAnInnerArchive() {
        val work = createTempDirectory("renault-nested-slip-").toFile()
        try {
            val inner = File(work, "bad.zip")
            archive(inner, mapOf("../escape.txt" to "bad".toByteArray()))
            val outer = File(work, "outer.zip")
            archive(outer, mapOf("bad.zip" to inner.readBytes()))
            val stage = File(work, "extracted")
            val extracted = ArchiveIntake.extract(outer, stage, allowNoRawRoots = true)
            try {
                NestedZipVolumeIntake.expandOneLevel(
                    stage,
                    extracted.files + extracted.directories,
                    extracted.extractedBytes,
                )
                fail("Expected traversal rejection")
            } catch (_: IllegalArgumentException) {
                assertTrue(!File(work, "escape.txt").exists())
            }
        } finally {
            work.deleteRecursively()
        }
    }

    @Test
    fun doesNotRecurseIntoAThirdArchiveLevel() {
        val work = createTempDirectory("renault-deep-zip-").toFile()
        try {
            val deepest = File(work, "inner.zip")
            archive(deepest, mapOf("NT8200/index.html" to "ok".toByteArray()))
            val middle = File(work, "middle.zip")
            archive(middle, mapOf("inner.zip" to deepest.readBytes()))
            val outer = File(work, "outer.zip")
            archive(outer, mapOf("middle.zip" to middle.readBytes()))
            val stage = File(work, "extracted")
            val extracted = ArchiveIntake.extract(outer, stage, allowNoRawRoots = true)
            try {
                NestedZipVolumeIntake.expandOneLevel(
                    stage,
                    extracted.files + extracted.directories,
                    extracted.extractedBytes,
                )
                fail("Expected deeper nesting to be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Глибше вкладені"))
            }
        } finally {
            work.deleteRecursively()
        }
    }

    @Test
    fun honoursCancellationBeforeNestedZipExtraction() {
        val work = createTempDirectory("renault-cancel-zip-").toFile()
        try {
            val stage = File(work, "stage")
            assertTrue(stage.mkdirs())
            try {
                NestedZipVolumeIntake.expandOneLevel(
                    stage,
                    0,
                    0L,
                    isCancelled = { true },
                )
                fail("Cancelled extraction was not rejected")
            } catch (_: ConversionCancelledException) {
                // no archive output attempted
            }
        } finally {
            work.deleteRecursively()
        }
    }
}
