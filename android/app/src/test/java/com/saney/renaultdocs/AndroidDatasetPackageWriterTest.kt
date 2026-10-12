package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidDatasetPackageWriterTest {
    @Test
    fun datasetIdUsesCanonicalSlug() {
        assertEquals(
            "megane-ii",
            AndroidDatasetPackageWriter.datasetIdFor(
                "Megane II",
            ),
        )
    }

    @Test
    fun volumeRootsContainOnlyTopLevelFoldersWithEntrypoints() {
        val roots =
            AndroidDatasetPackageWriter.discoverVolumeRoots(
                filePaths =
                    setOf(
                        "Megane NT8340A/INDEX.HTM",
                        "Megane NT8340A/IMG/a.gif",
                        "Megane NT8342A/index.htm",
                        "Backup/readme.txt",
                        "archive.zip",
                    ),
                directoryPaths =
                    setOf(
                        "Megane NT8340A",
                        "Megane NT8340A/IMG",
                        "Megane NT8342A",
                        "Backup",
                    ),
            )

        assertEquals(
            listOf(
                "Megane NT8340A",
                "Megane NT8342A",
            ),
            roots,
        )
    }

    // v0.5.107 (phone QA, Kangoo NT8486): the owner chose the volume folder itself.
    @Test
    fun volumeFolderChosenItselfHasNoVolumeRoots() {
        val files =
            setOf(
                "INDEX.HTM",
                "IMG/a.gif",
                "HTML/page.htm",
            )
        val roots =
            AndroidDatasetPackageWriter.discoverVolumeRoots(
                filePaths = files,
                directoryPaths = setOf("IMG", "HTML"),
            )

        assertEquals(emptyList<String>(), roots)
        assertTrue(AndroidDatasetPackageWriter.isVolumeFolderItself(files))
    }

    @Test
    fun parentFolderOfVolumeIsNotVolumeItself() {
        val files =
            setOf(
                "KangooII X61_NT8486_Visu v5.0_2009.08.31/INDEX.HTM",
                "KangooII X61_NT8486_Visu v5.0_2009.08.31/IMG/a.gif",
            )

        assertFalse(AndroidDatasetPackageWriter.isVolumeFolderItself(files))
        assertEquals(
            listOf("KangooII X61_NT8486_Visu v5.0_2009.08.31"),
            AndroidDatasetPackageWriter.discoverVolumeRoots(
                filePaths = files,
                directoryPaths =
                    setOf(
                        "KangooII X61_NT8486_Visu v5.0_2009.08.31",
                        "KangooII X61_NT8486_Visu v5.0_2009.08.31/IMG",
                    ),
            ),
        )
    }
}
