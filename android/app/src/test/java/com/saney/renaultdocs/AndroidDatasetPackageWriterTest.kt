package com.saney.renaultdocs

import org.junit.Assert.assertEquals
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
}
