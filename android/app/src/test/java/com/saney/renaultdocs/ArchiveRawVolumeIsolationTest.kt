package com.saney.renaultdocs

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveRawVolumeIsolationTest {
    @Test
    fun selectingParentExcludesIndependentNestedVolumesWithoutTouchingTheirFiles() {
        val root = createTempDirectory("renault-nested-archive-").toFile()
        try {
            File(root, "INDEX.HTM").writeText("parent-index")
            val assets = File(root, "images").apply { mkdirs() }
            File(assets, "parent.png").writeText("parent-image")
            val first = File(root, "NT1234A").apply { mkdirs() }
            File(first, "INDEX.HTM").writeText("nested-index")
            File(first, "part.bin").writeText("nested-payload")
            val second = File(root, "wrapper/NT5678").apply { mkdirs() }
            File(second, "ACCUEIL.HTM").writeText("second-index")

            val candidates = listOf("", "NT1234A", "wrapper/NT5678")
            val excluded = ArchiveRawVolumeIsolation.excludedDescendantRoots(
                extractionRoot = root,
                selectedRawRoot = root,
                candidateRelativePaths = candidates,
            )
            assertEquals(setOf(first.canonicalFile, second.canonicalFile), excluded)

            val parentFiles = ArchiveRawVolumeIsolation.walkSelectedRoot(root, excluded)
                .filter { it.isFile }
                .map { root.toPath().relativize(it.toPath()).toString().replace(File.separatorChar, '/') }
                .toSet()
            assertEquals(setOf("INDEX.HTM", "images/parent.png"), parentFiles)
            assertTrue(File(first, "part.bin").isFile) // Skipping never removes the originals.
            assertTrue(File(second, "ACCUEIL.HTM").isFile)

            val firstExcluded = ArchiveRawVolumeIsolation.excludedDescendantRoots(
                root, first, candidates,
            )
            assertTrue(firstExcluded.isEmpty())
            val firstFiles = ArchiveRawVolumeIsolation.walkSelectedRoot(first, firstExcluded)
                .filter { it.isFile }.map { it.name }.toSet()
            assertEquals(setOf("INDEX.HTM", "part.bin"), firstFiles)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recursivelyNestedSeparateVolumeIsNotCopiedIntoParentVolume() {
        val root = createTempDirectory("renault-raw-subvolume-").toFile()
        try {
            val parent = File(root, "NT1001").apply { mkdirs() }
            File(parent, "INDEX.HTM").writeText("parent")
            val child = File(parent, "NT1002").apply { mkdirs() }
            File(child, "INDEX.HTM").writeText("child")
            val grandchild = File(child, "NT1003").apply { mkdirs() }
            File(grandchild, "INDEX.HTM").writeText("grandchild")

            val paths = listOf("NT1001", "NT1001/NT1002", "NT1001/NT1002/NT1003")
            val parentExclusions =
                ArchiveRawVolumeIsolation.excludedDescendantRoots(root, parent, paths)
            assertEquals(setOf(child.canonicalFile, grandchild.canonicalFile), parentExclusions)
            val parentFiles = ArchiveRawVolumeIsolation.walkSelectedRoot(
                parent, parentExclusions,
            ).filter { it.isFile }.toList()
            assertEquals(listOf(File(parent, "INDEX.HTM")), parentFiles)

            val childExclusions =
                ArchiveRawVolumeIsolation.excludedDescendantRoots(root, child, paths)
            assertEquals(setOf(grandchild.canonicalFile), childExclusions)
            val childFiles = ArchiveRawVolumeIsolation.walkSelectedRoot(
                child, childExclusions,
            ).filter { it.isFile }.toList()
            assertEquals(listOf(File(child, "INDEX.HTM")), childFiles)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsEscapeOrSourceRootExclusionRatherThanDroppingWholeVolume() {
        val root = createTempDirectory("renault-isolation-reject-").toFile()
        val outsider = createTempDirectory("renault-outside-").toFile()
        try {
            File(root, "INDEX.HTM").writeText("parent")
            try {
                ArchiveRawVolumeIsolation.excludedDescendantRoots(root, root, listOf("../evil"))
                fail("Traversal must not enter the plan")
            } catch (_: IllegalArgumentException) {
                // expected
            }
            try {
                ArchiveRawVolumeIsolation.excludedDescendantRoots(root, outsider, listOf(""))
                fail("Outside selected root must be refused")
            } catch (_: IllegalArgumentException) {
                // expected
            }
            try {
                ArchiveRawVolumeIsolation.walkSelectedRoot(root, setOf(root))
                fail("The selected root must never be excluded")
            } catch (_: IllegalArgumentException) {
                // expected
            }
            assertFalse(File(outsider, "INDEX.HTM").exists())
        } finally {
            root.deleteRecursively()
            outsider.deleteRecursively()
        }
    }
}
