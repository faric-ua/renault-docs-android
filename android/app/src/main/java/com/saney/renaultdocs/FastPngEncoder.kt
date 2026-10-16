package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * v0.5.139 (owner 2026-10-16 21:37–21:44: «швидше проти останнього разу, але хотів би ще
 * швидше»). After v0.5.138 files on the card are read fast (log 111); what is left is how
 * a PDF page becomes a picture. Android's `Bitmap.compress(PNG, …)` uses slow, strong
 * compression and keeps the alpha channel — on a large A3 diagram on the M30s CPU that is
 * the longest step.
 *
 * This encoder writes the same lossless PNG (every pixel identical — lines and small pin
 * labels stay sharp), but:
 * - RGB without alpha (pages are drawn on white; alpha is always opaque) — 25 % less data;
 * - row filter «Sub» (good for drawings with long flat areas) and the fastest zlib level.
 *
 * Plain Kotlin + java.util.zip, no Android types — tested on the JVM (decoded back and
 * compared pixel by pixel).
 */
object FastPngEncoder {
    private val SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

    /** Rows read at once: a 3000-px page needs ~0.8 MB for a stripe, not 25 MB for all. */
    const val STRIPE_ROWS = 64

    /** [argb] — `width × height` pixels as from `Bitmap.getPixels`; alpha is ignored. */
    fun encode(
        argb: IntArray,
        width: Int,
        height: Int,
        level: Int = Deflater.BEST_SPEED,
    ): ByteArray {
        require(argb.size >= width * height) { "Not enough pixels." }
        return encodeRows(width, height, level) { y, rows, out ->
            System.arraycopy(argb, y * width, out, 0, rows * width)
        }
    }

    /**
     * [readRows] fills `out` with `rows` rows starting at row `y` (as
     * `Bitmap.getPixels(out, 0, width, 0, y, width, rows)`).
     */
    fun encodeRows(
        width: Int,
        height: Int,
        level: Int = Deflater.BEST_SPEED,
        readRows: (y: Int, rows: Int, out: IntArray) -> Unit,
    ): ByteArray {
        require(width > 0 && height > 0) { "Empty image." }
        val out = ByteArrayOutputStream(width * height / 2 + 1024)
        out.write(SIGNATURE)
        val header =
            ByteArrayOutputStream(13).also {
                DataOutputStream(it).apply {
                    writeInt(width)
                    writeInt(height)
                    writeByte(8) // bit depth
                    writeByte(2) // colour type: truecolour RGB
                    writeByte(0) // compression
                    writeByte(0) // filter method
                    writeByte(0) // no interlace
                }
            }.toByteArray()
        chunk(out, "IHDR", header)
        chunk(out, "IDAT", compressRows(width, height, level, readRows))
        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun compressRows(
        width: Int,
        height: Int,
        level: Int,
        readRows: (y: Int, rows: Int, out: IntArray) -> Unit,
    ): ByteArray {
        val stripe = IntArray(width * minOf(STRIPE_ROWS, height))
        val deflater = Deflater(level)
        val compressed = ByteArrayOutputStream(width * height / 2 + 1024)
        val row = ByteArray(1 + width * 3)
        val buffer = ByteArray(64 * 1024)
        try {
            for (y in 0 until height) {
                val inStripe = y % STRIPE_ROWS
                if (inStripe == 0) readRows(y, minOf(STRIPE_ROWS, height - y), stripe)
                row[0] = 1 // filter «Sub»: each byte minus the same byte of the pixel to the left
                var prevR = 0
                var prevG = 0
                var prevB = 0
                var i = 1
                val base = inStripe * width
                for (x in 0 until width) {
                    val p = stripe[base + x]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    row[i] = (r - prevR).toByte()
                    row[i + 1] = (g - prevG).toByte()
                    row[i + 2] = (b - prevB).toByte()
                    prevR = r
                    prevG = g
                    prevB = b
                    i += 3
                }
                deflater.setInput(row)
                while (!deflater.needsInput()) {
                    val n = deflater.deflate(buffer)
                    if (n > 0) compressed.write(buffer, 0, n)
                }
            }
            deflater.finish()
            while (!deflater.finished()) {
                val n = deflater.deflate(buffer)
                if (n > 0) compressed.write(buffer, 0, n)
            }
        } finally {
            deflater.end()
        }
        return compressed.toByteArray()
    }

    private fun chunk(
        out: ByteArrayOutputStream,
        type: String,
        data: ByteArray,
    ) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }
        DataOutputStream(out).apply {
            writeInt(data.size)
            write(typeBytes)
            write(data)
            writeInt(crc.value.toInt())
            flush()
        }
    }
}
