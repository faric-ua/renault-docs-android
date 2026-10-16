package com.saney.renaultdocs

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v0.5.139: fast lossless PNG for PDF pages + disk cache of drawn pages. */
class FastPngEncoderTest {
    private fun drawing(
        width: Int,
        height: Int,
    ): IntArray {
        // White page with thin black and coloured lines, like a wiring diagram.
        val px = IntArray(width * height) { 0xFFFFFFFF.toInt() }
        for (y in 0 until height step 17) for (x in 0 until width) px[y * width + x] = 0xFF000000.toInt()
        for (x in 0 until width step 23) for (y in 0 until height) px[y * width + x] = 0xFFCC2233.toInt()
        for (i in 0 until minOf(width, height)) px[i * width + i] = 0xFF1144EE.toInt()
        return px
    }

    @Test
    fun everyPixelSurvives() {
        val w = 301
        val h = 157
        val px = drawing(w, h)
        val png = FastPngEncoder.encode(px, w, h)
        val image = ImageIO.read(ByteArrayInputStream(png))
        assertEquals(w, image.width)
        assertEquals(h, image.height)
        val back = IntArray(w * h)
        image.getRGB(0, 0, w, h, back, 0, w)
        assertArrayEquals(px.map { it or 0xFF000000.toInt() }.toIntArray(), back.map { it or 0xFF000000.toInt() }.toIntArray())
    }

    @Test
    fun alphaIsDroppedNotColours() {
        val px = intArrayOf(0x00FF0000, 0x7F00FF00, 0xFF0000FF.toInt())
        val image = ImageIO.read(ByteArrayInputStream(FastPngEncoder.encode(px, 3, 1)))
        assertEquals(0xFFFF0000.toInt(), image.getRGB(0, 0))
        assertEquals(0xFF00FF00.toInt(), image.getRGB(1, 0))
        assertEquals(0xFF0000FF.toInt(), image.getRGB(2, 0))
    }

    @Test
    fun a3DiagramPageIsSmallAndQuick() {
        val w = 1600
        val h = 1131
        val px = drawing(w, h)
        FastPngEncoder.encode(px, w, h) // warm-up
        val started = System.nanoTime()
        val png = FastPngEncoder.encode(px, w, h)
        val ms = (System.nanoTime() - started) / 1_000_000
        println("FastPngEncoder 1600x1131: ${png.size} bytes, $ms ms (JVM)")
        assertTrue(png.size < w * h)
    }

    @Test
    fun diskCacheKeepsAndTrimsOnlyItsPictures() {
        val dir = Files.createTempDirectory("pdfcache").toFile()
        try {
            val other = File(dir, "keep-me.pdf").apply { writeText("not a cache picture") }
            val cache = PdfPageDiskCache(dir, limitBytes = 3_000)
            val k1 = PdfPageDiskCache.key("pkg:nt8494", "SCH/0057_A3.PDF", 123, 456, 0, 1600)
            assertNull(cache.get(k1))
            cache.put(k1, ByteArray(1_000) { 1 })
            assertArrayEquals(ByteArray(1_000) { 1 }, cache.get(k1))
            // Another size or time of the PDF — another picture.
            assertNull(cache.get(PdfPageDiskCache.key("pkg:nt8494", "SCH/0057_A3.PDF", 124, 456, 0, 1600)))
            cache.fileFor(k1).setLastModified(1_000)
            val k2 = PdfPageDiskCache.key("pkg:nt8494", "b.pdf", 1, 1, 0, 1600)
            val k3 = PdfPageDiskCache.key("pkg:nt8494", "c.pdf", 1, 1, 0, 1600)
            cache.put(k2, ByteArray(1_000))
            cache.put(k3, ByteArray(1_000))
            cache.fileFor(k2).setLastModified(2_000)
            cache.fileFor(k3).setLastModified(3_000)
            cache.put(PdfPageDiskCache.key("pkg:nt8494", "d.pdf", 1, 1, 0, 1600), ByteArray(1_000))
            cache.trim()
            assertFalse("oldest picture goes first", cache.fileFor(k1).exists())
            assertTrue("other files are never touched", other.exists())
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".part") })
            // Too big for the whole cache — not stored.
            cache.put(PdfPageDiskCache.key("x", "y", 1, 1, 0, 1), ByteArray(5_000))
            assertNull(cache.get(PdfPageDiskCache.key("x", "y", 1, 1, 0, 1)))
        } finally {
            dir.deleteRecursively()
        }
    }
}
