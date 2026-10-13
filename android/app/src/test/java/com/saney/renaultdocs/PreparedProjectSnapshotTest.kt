package com.saney.renaultdocs

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #173 (v0.5.122): a prepared .rdproject is found by the project id inside it,
 * survives a changed volume list, and a lookup never renames or deletes files.
 */
class PreparedProjectSnapshotTest {
    private val megane =
        RenaultProject(
            id = "megane-ii",
            title = "Megane II",
            model = "Megane II",
        )
    private val laguna =
        RenaultProject(
            id = "laguna-ii",
            title = "Laguna II",
            model = "Laguna II",
        )

    private fun volume(id: String) =
        ProjectVolumeRecord(
            id = id,
            title = id,
            documentCode = null,
            date = null,
            projectHint = null,
            datasetId = id,
            datasetTitle = id,
            manufacturer = "Renault",
            model = "Megane II",
            platform = null,
            yearsLabel = null,
            contentType = null,
            entrypoint = "",
            openEntrypoint = "",
            treeUri = "",
        )

    private fun rdproject(
        dir: File,
        name: String,
        projectId: String?,
        volumeIds: List<String>,
    ): File {
        val file = File(dir, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            if (projectId != null) {
                zip.putNextEntry(ZipEntry("rdproject.json"))
                val volumes =
                    volumeIds.joinToString(",") { "{\"id\":\"$it\"}" }
                zip.write(
                    ("{\"format\":\"renault-docs-project\",\"project\":{\"id\":\"$projectId\"}," +
                        "\"volumes\":[$volumes]}").toByteArray(Charsets.UTF_8),
                )
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("volumes/a.rdpkg"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }
        return file
    }

    @Test
    fun snapshotOf21VolumesIsFoundAfterThe22ndVolumeWasAdded() {
        val dir = Files.createTempDirectory("prepared").toFile()
        val old21 = (1..21).map { "v$it" }
        // name from an older metadata state — no longer the computed name
        val file = rdproject(dir, "Megane-II_X84-B84.rdproject", "megane-ii", old21)
        val now22 = (1..22).map { volume("v$it") }

        val snapshot = PreparedShareStore.projectSnapshotsIn(dir, megane, now22).single()

        assertEquals(file.absolutePath, snapshot.file.absolutePath)
        assertEquals(21, snapshot.volumeCount)
        assertFalse(snapshot.matches(now22))
        assertTrue(snapshot.matches((1..21).map { volume("v$it") }))
        assertTrue("lookup must not touch the file", file.isFile)
    }

    @Test
    fun anotherProjectsSnapshotIsNotShown() {
        val dir = Files.createTempDirectory("prepared").toFile()
        rdproject(dir, "Megane-II.rdproject", "megane-ii", listOf("v1"))

        assertTrue(PreparedShareStore.projectSnapshotsIn(dir, laguna, listOf(volume("v1"))).isEmpty())
    }

    @Test
    fun unreadableManifestFallsBackToLegacyNameWithoutRenaming() {
        val dir = Files.createTempDirectory("prepared").toFile()
        val legacy = rdproject(dir, "megane-ii.rdproject", null, emptyList())

        val snapshot = PreparedShareStore.projectSnapshotsIn(dir, megane, listOf(volume("v1"))).single()

        assertEquals("megane-ii.rdproject", snapshot.file.name)
        assertNull(snapshot.volumeCount)
        assertTrue(snapshot.matches(listOf(volume("v1"))))
        assertTrue(legacy.isFile)
        assertEquals(listOf("megane-ii.rdproject"), dir.list()!!.toList())
    }

    @Test
    fun explicitDeleteRemovesOnlyThatSnapshot() {
        val dir = Files.createTempDirectory("prepared").toFile()
        val mine = rdproject(dir, "Megane-II.rdproject", "megane-ii", listOf("v1"))
        val other = rdproject(dir, "Laguna-II.rdproject", "laguna-ii", listOf("l1"))

        val snapshot = PreparedShareStore.projectSnapshotsIn(dir, megane, listOf(volume("v1"))).single()
        assertTrue(PreparedShareStore.deleteProjectSnapshot(snapshot.file))

        assertFalse(mine.exists())
        assertTrue(other.isFile)
        assertNotNull(PreparedShareStore.projectSnapshotsIn(dir, laguna, listOf(volume("l1"))).singleOrNull())
    }

    @Test
    fun manifestParserReadsProjectIdAndVolumeIds() {
        val (id, volumes) =
            PreparedShareStore.parseManifestForTest(
                "{\"project\":{\"id\":\"megane-ii\"},\"volumes\":[{\"id\":\"a\"},{\"id\":\"b\"}]}",
            )
        assertEquals("megane-ii", id)
        assertEquals(setOf("a", "b"), volumes)
    }
}
