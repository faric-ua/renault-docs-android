package com.saney.renaultdocs

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** v0.5.138 — quick step of TODO №16 (log 110): find volume files without listing. */
class VolumeFileIndexTest {
    private lateinit var dir: File

    @Before
    fun setUp() {
        VolumeFileIndex.clearAll()
        dir = Files.createTempDirectory("vfi").toFile()
    }

    @After
    fun tearDown() {
        VolumeFileIndex.clearAll()
        dir.deleteRecursively()
    }

    @Test
    fun oneCallTellsFileFolderOrNothing() {
        val sub = File(dir, "COMMUN/PDF/SCH").apply { mkdirs() }
        val pdf = File(sub, "0057_A3.PDF").apply { writeBytes(ByteArray(1234)) }
        val fileAttrs = VolumeFileIndex.attributes(pdf)
        assertNotNull(fileAttrs)
        assertFalse(fileAttrs!!.isDirectory)
        assertEquals(1234L, fileAttrs.size)
        assertTrue(fileAttrs.lastModified > 0L)
        val dirAttrs = VolumeFileIndex.attributes(sub)
        assertTrue(dirAttrs!!.isDirectory)
        assertEquals(0L, dirAttrs.size)
        assertNull(VolumeFileIndex.attributes(File(sub, "missing.pdf")))
    }

    @Test
    fun symbolicLinksAreRefused() {
        val outside = Files.createTempFile("secret", ".txt").toFile()
        try {
            val link = File(dir, "link.pdf")
            val made = runCatching { Files.createSymbolicLink(link.toPath(), outside.toPath()) }.isSuccess
            if (made) assertNull(VolumeFileIndex.attributes(link))
        } finally {
            outside.delete()
        }
    }

    @Test
    fun volumeFolderIsFoundOnceAndForgotten() {
        var lookups = 0
        val first = VolumeFileIndex.root("nt8494") { lookups++; dir }
        val second = VolumeFileIndex.root("nt8494") { lookups++; dir }
        assertEquals(dir.canonicalFile, first)
        assertSame(first, second)
        assertEquals(1, lookups)
        VolumeFileIndex.forget("nt8494")
        VolumeFileIndex.root("nt8494") { lookups++; dir }
        assertEquals(2, lookups)
    }

    @Test
    fun missingVolumeIsNotRemembered() {
        var lookups = 0
        assertNull(VolumeFileIndex.root("gone") { lookups++; null })
        assertNull(VolumeFileIndex.root("gone") { lookups++; File(dir, "nope") })
        assertEquals(2, lookups)
    }

    @Test
    fun pathsStayInsideTheVolume() {
        assertEquals(dir, VolumeFileIndex.fileIn(dir, ""))
        assertEquals(File(dir, "a/b.pdf"), VolumeFileIndex.fileIn(dir, "a/b.pdf"))
        // The only way in is through normalize, which refuses `..`.
        assertNull(SafDatasetResolver.normalize("a/../../secret"))
    }

    @Test
    fun foundNodesAreRememberedPerVolumeAndForgotten() {
        VolumeFileIndex.rememberNode("v1", "a.pdf", "node-a")
        assertEquals("node-a", VolumeFileIndex.nodeCached<String>("v1", "a.pdf"))
        assertNull(VolumeFileIndex.nodeCached<String>("v2", "a.pdf"))
        var forgotten = ""
        VolumeFileIndex.onForget { forgotten = it }
        VolumeFileIndex.forget("v1")
        assertNull(VolumeFileIndex.nodeCached<String>("v1", "a.pdf"))
        assertEquals("v1", forgotten)
    }

    @Test
    fun memoryPerVolumeIsBounded() {
        repeat(VolumeFileIndex.MAX_NODES_PER_VOLUME + 5) { VolumeFileIndex.rememberNode("big", "f$it", it) }
        assertNull(VolumeFileIndex.nodeCached<Int>("big", "f0"))
        assertNotNull(VolumeFileIndex.nodeCached<Int>("big", "f${VolumeFileIndex.MAX_NODES_PER_VOLUME + 4}"))
    }
}
