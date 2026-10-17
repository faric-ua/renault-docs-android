package com.saney.renaultdocs

import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * TODO №19 (2026-10-17): on the M30s (Android 11) the compressor sometimes wrote a broken
 * ZIP — a different zlib error every time ([VolumeArchiveWriter] history, v0.5.142). The same
 * compressor writes exported volumes, «Поділитися томом» and `.rdproject` files.
 *
 * This check reads the ZIP **while it is being written**: the writer sends every byte both
 * to the destination and to [output]; a second thread unpacks those very bytes with
 * [ZipInputStream] (which checks each entry's size and CRC-32 against the original data) and
 * counts the files. So a file that would not open on the other phone is found right away —
 * without reading it back from Google Drive or another app. Unpacking stored or lightly
 * compressed entries is cheap compared with writing them.
 *
 * Use: write the ZIP into a tee of the destination and [output], close it, then [finish].
 * [finish] throws [DamagedOutputException] with words for the owner when the bytes do not unpack, or
 * when the number of files differs from what was written.
 *
 * Plain Kotlin + java.util.zip — tested on the JVM.
 */
class ZipStreamCheck : java.io.Closeable {
    private val pipeIn = PipedInputStream(PIPE_BYTES)
    private val pipeOut = PipedOutputStream(pipeIn)

    @Volatile private var failure: String? = null

    @Volatile private var files = 0

    @Volatile private var readerDone = false

    private val reader =
        Thread({
            try {
                ZipInputStream(pipeIn).use { zip ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val crc = CRC32()
                        var size = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            crc.update(buffer, 0, read)
                            size += read
                        }
                        if (entry.crc != -1L && entry.crc != crc.value) {
                            throw ZipException("CRC не збігся у ${entry.name}")
                        }
                        if (entry.size != -1L && entry.size != size) {
                            throw ZipException("розмір не збігся у ${entry.name}")
                        }
                        files++
                    }
                    // The table of contents follows the entries: read it to the end so the
                    // writer never waits on a full pipe.
                    while (pipeIn.read(buffer) >= 0) Unit
                }
            } catch (error: Throwable) {
                failure = error.message ?: error.javaClass.simpleName
                // Keep emptying the pipe: the writer must finish (and be told) regardless.
                runCatching {
                    val buffer = ByteArray(64 * 1024)
                    while (pipeIn.read(buffer) >= 0) Unit
                }
            } finally {
                readerDone = true
            }
        }, "zip-stream-check").apply {
            isDaemon = true
            start()
        }

    /** Where the writer sends a copy of every byte. Closing it ends the check's input. */
    val output: OutputStream =
        object : OutputStream() {
            private var open = true

            override fun write(b: Int) {
                write(byteArrayOf(b.toByte()), 0, 1)
            }

            override fun write(
                b: ByteArray,
                off: Int,
                len: Int,
            ) {
                if (!open || len == 0) return
                try {
                    pipeOut.write(b, off, len)
                } catch (error: IOException) {
                    // The checking thread is gone: stop copying, [finish] reports why.
                    open = false
                    if (failure == null) failure = error.message ?: "перевірку перервано"
                }
            }

            override fun flush() {
                if (open) runCatching { pipeOut.flush() }
            }

            override fun close() {
                if (!open) return
                open = false
                runCatching { pipeOut.close() }
            }
        }

    /**
     * Waits for the checking thread and returns the number of files it unpacked.
     * Throws when the ZIP does not unpack or holds another number of files than [expectedFiles].
     */
    fun finish(expectedFiles: Int? = null): Int {
        output.close()
        reader.join(JOIN_MS)
        if (!readerDone) throw DamagedOutputException("Перевірка файлу не закінчилась.")
        failure?.let { throw DamagedOutputException("Файл вийшов пошкоджений: $it") }
        if (expectedFiles != null && files != expectedFiles) {
            throw DamagedOutputException("Файл вийшов пошкоджений: у ньому $files файлів замість $expectedFiles.")
        }
        return files
    }

    override fun close() {
        output.close()
        runCatching { pipeIn.close() }
    }

    companion object {
        private const val PIPE_BYTES = 4 * 1024 * 1024
        private const val JOIN_MS = 5 * 60 * 1000L
    }
}

/**
 * TODO №19: the file just written does not unpack. The writer's caller removes only its own
 * unfinished file; the volume itself is never touched.
 */
class DamagedOutputException(
    message: String,
) : IOException(message + " Спробуйте ще раз або з іншого телефона. Сам том не змінено.")

/** Sends every byte to [first] and to [second]. Closing closes both. */
class TeeOutputStream(
    private val first: OutputStream,
    private val second: OutputStream,
) : OutputStream() {
    override fun write(b: Int) {
        first.write(b)
        second.write(b)
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        first.write(b, off, len)
        second.write(b, off, len)
    }

    override fun flush() {
        first.flush()
        second.flush()
    }

    override fun close() {
        try {
            first.close()
        } finally {
            second.close()
        }
    }
}
