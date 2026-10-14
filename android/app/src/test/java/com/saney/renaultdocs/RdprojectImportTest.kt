package com.saney.renaultdocs

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** v0.5.132: import of a whole project from one .rdproject file. */
class RdprojectImportTest {
    private fun manifestJson(
        volumes: List<Triple<String, String, String?>> =
            listOf(
                Triple("nt8299a", "volumes/Megane_NT8299A.rdpkg", "NT8299A"),
                Triple("nt8340a", "volumes/Megane_NT8340A.rdpkg", "NT8340A"),
            ),
        format: String = RdprojectManifestParser.FORMAT,
        schema: Int = 1,
    ): String {
        val array = JSONArray()
        volumes.forEach { (id, file, code) ->
            val item = JSONObject().put("id", id).put("title", "Том $id").put("file", file)
                .put("vehicle_codes", JSONArray(listOf("BM0")))
            if (code != null) item.put("document_code", code).put("date", "2005-03")
            array.put(item)
        }
        return JSONObject()
            .put("format", format)
            .put("schema_version", schema)
            .put(
                "project",
                JSONObject().put("id", "megane-ii").put("title", "Megane II").put("model", "Megane II")
                    .put("vehicle_codes", JSONArray(listOf("BM0"))),
            )
            .put("volumes", array)
            .toString()
    }

    private fun zip(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun existing(
        projectId: String,
        id: String,
        code: String?,
        date: String? = "2005-03",
        title: String = "Megane II",
    ) = RdprojectExistingVolume(projectId, title, id, code, date, listOf("BM0"))

    @Test
    fun parsesManifestWrittenByExporter() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        assertEquals("megane-ii", manifest.projectId)
        assertEquals("Megane II", manifest.displayTitle)
        assertEquals(2, manifest.volumes.size)
        assertEquals("NT8299A · 2005-03", manifest.volumes[0].label)
        assertEquals("volumes/Megane_NT8299A.rdpkg", manifest.volumes[0].file)
    }

    @Test
    fun rejectsOtherFilesAndNewerFormat() {
        assertFormatError { RdprojectManifestParser.parse("not json") }
        assertFormatError { RdprojectManifestParser.parse(manifestJson(format = "renault-volume-package-v1")) }
        assertFormatError { RdprojectManifestParser.parse(manifestJson(schema = 2)) }
        assertFormatError { RdprojectManifestParser.parse(manifestJson(volumes = emptyList())) }
    }

    @Test
    fun volumePathCannotEscapeTheWorkFolder() {
        assertEquals("volumes/a.rdpkg", RdprojectManifestParser.safeVolumePath("volumes/a.rdpkg"))
        listOf(
            "volumes/../a.rdpkg", "volumes/sub/a.rdpkg", "/volumes/a.rdpkg", "a.rdpkg",
            "volumes/a.zip", "volumes/.rdpkg", "volumes/..\\a.rdpkg", "volumes/.hidden.rdpkg", "",
        ).forEach { assertNull(it, RdprojectManifestParser.safeVolumePath(it)) }
        assertFormatError {
            RdprojectManifestParser.parse(
                manifestJson(volumes = listOf(Triple("x", "volumes/../../evil.rdpkg", null))),
            )
        }
        assertFormatError {
            RdprojectManifestParser.parse(
                manifestJson(
                    volumes = listOf(Triple("a", "volumes/a.rdpkg", null), Triple("b", "volumes/a.rdpkg", null)),
                ),
            )
        }
    }

    @Test
    fun readsOnlyTheFirstEntry() {
        val bytes = zip(
            listOf(
                RdprojectManifestParser.MANIFEST_NAME to manifestJson().toByteArray(),
                "volumes/Megane_NT8299A.rdpkg" to ByteArray(10) { 1 },
            ),
        )
        assertEquals(2, RdprojectManifestParser.readFromStream(ByteArrayInputStream(bytes)).volumes.size)
        val rdpkgFirst = zip(listOf("rdpkg.json" to "{}".toByteArray()))
        assertFormatError { RdprojectManifestParser.readFromStream(ByteArrayInputStream(rdpkgFirst)) }
        assertFormatError { RdprojectManifestParser.readFromStream(ByteArrayInputStream("hello".toByteArray())) }
    }

    @Test
    fun exporterWritesManifestBeforeVolumes() {
        // The exporter sorts entries by path; the importer reads rdproject.json first.
        assertTrue(RdprojectManifestParser.MANIFEST_NAME < RdprojectManifestParser.VOLUMES_PREFIX)
    }

    @Test
    fun planSkipsVolumesAlreadyOnThePhone() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val target = RdprojectExistingProject("megane-ii", "Megane II")
        val plan = RdprojectImportPlanner.plan(manifest, target, listOf(existing("megane-ii", "nt8299a", "NT8299A")))
        assertFalse(plan.createsProject)
        assertEquals(listOf("volumes/Megane_NT8340A.rdpkg"), plan.toImport.map { it.volume.file })
        assertEquals(RdprojectVolumeStatus.IN_TARGET_PROJECT, plan.volumes[0].status)
    }

    @Test
    fun allPresentMeansNothingToImport() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val plan = RdprojectImportPlanner.plan(
            manifest,
            RdprojectExistingProject("megane-ii", "Megane II"),
            listOf(existing("megane-ii", "a", "NT8299A"), existing("megane-ii", "b", "NT8340A")),
        )
        assertTrue(plan.toImport.isEmpty())
        assertEquals(0L, RdprojectImportPlanner.estimatedNeedBytes(plan, 3_000_000_000L))
    }

    @Test
    fun needFollowsTheTickedVolumes() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val plan = RdprojectImportPlanner.plan(manifest, null, emptyList())
        val all = RdprojectImportPlanner.estimatedNeedBytes(plan, 2_000_000_000L)!!
        val one = RdprojectImportPlanner.estimatedNeedBytes(plan, 2_000_000_000L, 1)!!
        assertTrue(one < all)
        assertEquals(0L, RdprojectImportPlanner.estimatedNeedBytes(plan, 2_000_000_000L, 0))
    }

    @Test
    fun volumeInAnotherProjectIsNotTouched() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val plan = RdprojectImportPlanner.plan(
            manifest,
            null,
            listOf(existing("laguna-ii", "other-id", "NT8340A", title = "Laguna II")),
        )
        assertTrue(plan.createsProject)
        assertEquals("Megane II", plan.targetTitle)
        assertEquals(RdprojectVolumeStatus.IN_OTHER_PROJECT, plan.volumes[1].status)
        assertEquals("Laguna II", plan.volumes[1].existingProjectTitle)
        assertEquals(1, plan.toImport.size)
    }

    @Test
    fun similarCodesAreDifferentVolumes() {
        val volume = RdprojectManifestParser.parse(manifestJson()).volumes[1] // NT8340A
        assertFalse(RdprojectImportPlanner.isSameVolume(volume, existing("p", "x", "NT8342A")))
        assertFalse(RdprojectImportPlanner.isSameVolume(volume, existing("p", "x", "NT8340A", date = "2007-01")))
        assertTrue(RdprojectImportPlanner.isSameVolume(volume, existing("p", "x", "nt8340a")))
        assertFalse(RdprojectImportPlanner.isSameVolume(volume, existing("p", "x", "NT8340A", date = null)))
    }

    @Test
    fun walkerCopiesOnlyWantedVolumesAndCleansUp() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val bytes = zip(
            listOf(
                RdprojectManifestParser.MANIFEST_NAME to manifestJson().toByteArray(),
                "volumes/Megane_NT8299A.rdpkg" to ByteArray(5_000_000) { 7 },
                "volumes/Megane_NT8340A.rdpkg" to ByteArray(3_000) { 9 },
                "volumes/unknown.rdpkg" to ByteArray(10),
            ),
        )
        val work = Files.createTempDirectory("rdproject-test").toFile()
        val seen = mutableListOf<Pair<String, Long>>()
        var ticks = 0
        val missing = RdprojectArchiveWalker.walk(
            input = ByteArrayInputStream(bytes),
            manifest = manifest,
            wanted = setOf("volumes/Megane_NT8340A.rdpkg"),
            workDir = work,
            freeBytes = { Long.MAX_VALUE },
            isCancelled = { false },
            onProgress = { ticks++ },
            onVolume = { volume, file ->
                assertTrue(file.parentFile == work)
                assertTrue(file.readBytes().all { it == 9.toByte() })
                seen += volume.file to file.length()
            },
        )
        assertEquals(listOf("volumes/Megane_NT8340A.rdpkg" to 3_000L), seen)
        assertTrue(missing.isEmpty())
        assertTrue(ticks > 0)
        assertTrue("work folder must be empty", work.listFiles().orEmpty().isEmpty())
        work.delete()
    }

    @Test
    fun walkerReportsMissingVolumesAndStopsOnLowSpace() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val onlyFirst = zip(
            listOf(
                RdprojectManifestParser.MANIFEST_NAME to manifestJson().toByteArray(),
                "volumes/Megane_NT8299A.rdpkg" to ByteArray(100),
            ),
        )
        val work = Files.createTempDirectory("rdproject-test").toFile()
        val missing = RdprojectArchiveWalker.walk(
            ByteArrayInputStream(onlyFirst), manifest, emptySet(), work, { Long.MAX_VALUE }, { false }, {}, { _, _ -> fail() },
        )
        assertEquals(listOf("volumes/Megane_NT8340A.rdpkg"), missing)

        val big = zip(
            listOf(
                RdprojectManifestParser.MANIFEST_NAME to manifestJson().toByteArray(),
                "volumes/Megane_NT8299A.rdpkg" to ByteArray(9_000_000),
            ),
        )
        try {
            RdprojectArchiveWalker.walk(
                ByteArrayInputStream(big), manifest, setOf("volumes/Megane_NT8299A.rdpkg"), work,
                { 10L }, { false }, {}, { _, _ -> fail() },
            )
            fail("must stop when the phone is out of space")
        } catch (expected: RdprojectArchiveWalker.LowSpaceException) {
            // ok
        }
        assertTrue(work.listFiles().orEmpty().isEmpty())
        work.delete()
    }

    @Test
    fun walkerStopsOnCancel() {
        val manifest = RdprojectManifestParser.parse(manifestJson())
        val bytes = zip(listOf(RdprojectManifestParser.MANIFEST_NAME to manifestJson().toByteArray()))
        try {
            RdprojectArchiveWalker.walk(
                ByteArrayInputStream(bytes), manifest, emptySet(), File(System.getProperty("java.io.tmpdir")),
                { Long.MAX_VALUE }, { true }, {}, { _, _ -> },
            )
            fail("cancel must stop the walk")
        } catch (expected: OperationCancelledException) {
            // ok
        }
    }

    private fun assertFormatError(block: () -> Unit) {
        try {
            block()
            fail("expected RdprojectFormatException")
        } catch (expected: RdprojectFormatException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }
    }
}
