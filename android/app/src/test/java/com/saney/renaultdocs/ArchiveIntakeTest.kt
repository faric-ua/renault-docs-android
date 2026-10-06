package com.saney.renaultdocs

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveIntakeTest {
    @Test
    fun detectsArchiveSignatures() {
        val root = createTempDir(prefix = "archive-format-")

        try {
            val zip = File(root, "source.bin").apply {
                writeBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04, 0, 0, 0, 0))
            }
            assertEquals(
                ArchiveIntake.Format.ZIP,
                ArchiveIntake.detectFormat(zip),
            )

            val sevenZ = File(root, "source.unknown").apply {
                writeBytes(
                    byteArrayOf(
                        0x37,
                        0x7a,
                        0xbc.toByte(),
                        0xaf.toByte(),
                        0x27,
                        0x1c,
                        0,
                        0,
                    ),
                )
            }
            assertEquals(
                ArchiveIntake.Format.SEVEN_Z,
                ArchiveIntake.detectFormat(sevenZ),
            )

            val rar5 = File(root, "source.data").apply {
                writeBytes(
                    byteArrayOf(
                        0x52,
                        0x61,
                        0x72,
                        0x21,
                        0x1a,
                        0x07,
                        0x01,
                        0x00,
                    ),
                )
            }
            assertEquals(
                ArchiveIntake.Format.RAR,
                ArchiveIntake.detectFormat(rar5),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsTraversalAndAbsolutePaths() {
        val root = createTempDir(prefix = "archive-safe-")

        try {
            for (name in listOf("../evil.txt", "a/../../evil.txt", "/tmp/evil", "C:\\evil.txt")) {
                try {
                    ArchiveIntake.safeTarget(root, name)
                    fail("Expected unsafe path rejection for: $name")
                } catch (_: IllegalArgumentException) {
                    // expected
                }
            }

            val safe = ArchiveIntake.safeTarget(root, "one/two/INDEX.HTM")
            assertTrue(
                safe.canonicalPath.startsWith(
                    root.canonicalPath + File.separator,
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractsZipAndFindsNestedRenaultRawRoot() {
        val root = createTempDir(prefix = "archive-zip-")
        val archive = File(root, "old-renault.zip")
        val output = File(root, "out")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("wrapper/NT9999A/INDEX.HTM"))
                zip.write("<html>Renault</html>".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("wrapper/NT9999A/data/file.txt"))
                zip.write("payload".toByteArray())
                zip.closeEntry()
            }

            val result = ArchiveIntake.extract(
                source = archive,
                extractionRoot = output,
            )

            assertEquals(ArchiveIntake.Format.ZIP, result.format)
            assertEquals(2, result.files)
            assertEquals(1, result.rawRoots.size)
            assertEquals(
                "NT9999A",
                result.rawRoots.single().name,
            )
            assertTrue(
                File(
                    result.rawRoots.single(),
                    "data/file.txt",
                ).isFile,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsZipSlipBeforeWritingOutsideStaging() {
        val root = createTempDir(prefix = "archive-zipslip-")
        val archive = File(root, "bad.zip")
        val output = File(root, "out")
        val escaped = File(root, "escaped.txt")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("../escaped.txt"))
                zip.write("nope".toByteArray())
                zip.closeEntry()
            }

            try {
                ArchiveIntake.extract(
                    source = archive,
                    extractionRoot = output,
                )
                fail("Expected extraction to reject path traversal")
            } catch (_: IllegalStateException) {
                // expected
            }

            assertTrue(!escaped.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun findsMultipleRawRootsWithoutChoosingOneSilently() {
        val root = createTempDir(prefix = "archive-roots-")

        try {
            File(root, "a").mkdirs()
            File(root, "a/INDEX.HTM").writeText("a")
            File(root, "b").mkdirs()
            File(root, "b/ACCUEIL.HTM").writeText("b")

            val roots = ArchiveIntake.findRenaultRawRoots(root)

            assertEquals(2, roots.size)
            assertEquals(
                setOf("a", "b"),
                roots.map { it.name }.toSet(),
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
