package com.saney.renaultdocs

import java.io.File
import java.util.zip.ZipEntry
import kotlin.io.path.createTempDirectory
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveIntakeTest {
    @Test
    fun detectsArchiveSignatures() {
        val root = createTempDirectory("archive-format-").toFile()

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
        val root = createTempDirectory("archive-safe-").toFile()

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
    fun inspectsZipRawRootsWithoutExtractingPayload() {
        val root = createTempDirectory("archive-inspect-").toFile()
        val archive = File(root, "old-renault.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("wrapper/NT8344/INDEX.HTM"))
                zip.write("index".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("wrapper/NT8393/ACCUEIL.HTM"))
                zip.write("home".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("wrapper/NT8393/data/huge.bin"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }

            val inspection =
                ArchiveIntake.inspectRawRoots(
                    archive,
                )

            assertEquals(
                ArchiveIntake.Format.ZIP,
                inspection.format,
            )
            assertEquals(
                setOf(
                    "wrapper/NT8344",
                    "wrapper/NT8393",
                ),
                inspection.rawRoots
                    .map {
                        it.relativePath
                    }
                    .toSet(),
            )
            assertEquals(
                setOf(
                    "NT8344",
                    "NT8393",
                ),
                inspection.rawRoots
                    .map {
                        it.leafName
                    }
                    .toSet(),
            )
            assertTrue(
                !File(
                    root,
                    "wrapper",
                ).exists(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun keepsArchiveRootRawHintForDuplicateFastPath() {
        val root = createTempDirectory("archive-root-volume-").toFile()
        val archive = File(root, "generic-renault.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(
                    ZipEntry(
                        "INDEX.HTM",
                    ),
                )
                zip.write(
                    "<html><title>Renault NT8266A</title></html>"
                        .toByteArray(),
                )
                zip.closeEntry()

                zip.putNextEntry(
                    ZipEntry(
                        "data/file.bin",
                    ),
                )
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }

            val hint =
                ArchiveIntake
                    .inspectRawRoots(
                        source = archive,
                        sourceNameHint = "generic-renault.zip",
                    )
                    .rawRoots
                    .single()

            assertEquals(
                "",
                hint.relativePath,
            )
            assertEquals(
                "generic-renault",
                hint.leafName,
            )
            assertEquals(
                "NT8266A",
                hint.documentCode,
            )
            assertTrue(
                !File(
                    root,
                    "data",
                ).exists(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun preservesTopLevelRawRootLeafNameForDuplicateFastPath() {
        val root = createTempDirectory("archive-top-level-root-").toFile()
        val archive = File(root, "renault.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(
                    ZipEntry(
                        "NT8266A_2004-06-28/INDEX.HTM",
                    ),
                )
                zip.write("<html>Renault</html>".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(
                    ZipEntry(
                        "NT8266A_2004-06-28/data/file.bin",
                    ),
                )
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }

            val hint =
                ArchiveIntake
                    .inspectRawRoots(
                        source = archive,
                    )
                    .rawRoots
                    .single()

            assertEquals(
                "NT8266A_2004-06-28",
                hint.relativePath,
            )
            assertEquals(
                "NT8266A_2004-06-28",
                hint.leafName,
            )
            assertEquals(
                "NT8266A",
                hint.documentCode,
            )
            assertTrue(
                !File(
                    root,
                    "NT8266A_2004-06-28",
                ).exists(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun probesNtIdentityFromZipIndexWithoutExtractingPayload() {
        val root = createTempDirectory("archive-identity-").toFile()
        val archive = File(root, "generic-renault.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("wrapper/raw/INDEX.HTM"))
                zip.write(
                    "<html><title>Renault NT8266A</title></html>"
                        .toByteArray(),
                )
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("wrapper/raw/data/huge.bin"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }

            val inspection =
                ArchiveIntake.inspectRawRoots(
                    source = archive,
                    sourceNameHint = "generic-renault.zip",
                )

            assertEquals(
                "NT8266A",
                inspection.rawRoots
                    .single()
                    .documentCode,
            )
            assertTrue(
                !File(
                    root,
                    "wrapper",
                ).exists(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun inspectionIgnoresAlreadyPreparedDatasetRoots() {
        val root = createTempDirectory("archive-inspect-prepared-").toFile()
        val archive = File(root, "prepared.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("NT8344/INDEX.HTM"))
                zip.write("index".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("NT8344/renault-dataset.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }

            val inspection =
                ArchiveIntake.inspectRawRoots(
                    archive,
                )

            assertTrue(
                inspection.rawRoots.isEmpty(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun multiVolumeChooserResolvesArchiveRootAndNestedRawSafely() {
        val staging = createTempDirectory("renault-archive-root-").toFile()
        try {
            File(staging, "INDEX.HTM").writeText("index")
            val nested = File(staging, "NT8341A").apply { mkdirs() }
            File(nested, "ACCUEIL.HTM").writeText("accueil")

            val roots = ArchiveIntake.findRenaultRawRoots(staging)
            assertEquals(2, roots.size)
            assertEquals(staging.canonicalFile, ArchiveIntake.resolveRawRoot(staging, ""))
            assertEquals(nested.canonicalFile, ArchiveIntake.resolveRawRoot(staging, "NT8341A"))
            assertEquals(
                "NT8340A (2006-04-18)",
                ArchiveIntake.rawSourceName(staging, staging, "NT8340A (2006-04-18).zip"),
            )
            assertEquals(
                "NT8341A",
                ArchiveIntake.rawSourceName(staging, nested, "ignored.zip"),
            )

            for (unsafe in listOf("../", "../outside", "/tmp", "C:\\\\Windows")) {
                try {
                    ArchiveIntake.resolveRawRoot(staging, unsafe)
                    fail("Unsafe chooser root should fail: " + unsafe)
                } catch (_: IllegalArgumentException) {
                    // Expected: do not permit escaping the archive staging tree.
                }
            }
            try {
                ArchiveIntake.resolveRawRoot(staging, "missing")
                fail("Missing chooser raw root must fail")
            } catch (_: IllegalArgumentException) {
                // Expected.
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    @Test
    fun archivePathsRejectDuplicatePayloadsAndCaseConflicts() {
        val paths = ArchiveIntake.EntryPathGuard()
        paths.check("NT8340A/", true)
        paths.check("NT8340A/", true) // Duplicate directory markers are harmless.
        paths.check("NT8340A/INDEX.HTM", false)

        for (name in listOf("NT8340A/INDEX.HTM", "NT8340A/index.htm")) {
            try {
                paths.check(name, false)
                fail("Ambiguous duplicate archive entry must fail: " + name)
            } catch (_: IllegalArgumentException) {
                // Expected.
            }
        }
        try {
            paths.check("NT8340A/INDEX.HTM/subdir", false)
            fail("File cannot be an ancestor of another file")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun zipPreflightRejectsCaseCollisionsInsteadOfSilentlyOverwriting() {
        val folder = createTempDirectory("archive-collision-").toFile()
        try {
            val archive = File(folder, "bad.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                for (name in listOf("raw/INDEX.HTM", "raw/index.htm")) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write("data".toByteArray())
                    zip.closeEntry()
                }
            }
            try {
                ArchiveIntake.inspectRawRoots(archive)
                fail("Expected collision rejection during archive inspection")
            } catch (_: IllegalArgumentException) {
                // Must fail before extraction or duplicate fast-path.
            }

            val staging = File(folder, "staging")
            try {
                ArchiveIntake.extract(archive, staging)
                fail("Expected extraction collision to reject source")
            } catch (_: IllegalStateException) {
                assertTrue(!staging.exists())
            }
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun extractsZipAndFindsNestedRenaultRawRoot() {
        val root = createTempDirectory("archive-zip-").toFile()
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
        val root = createTempDirectory("archive-zipslip-").toFile()
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
        val root = createTempDirectory("archive-roots-").toFile()

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
