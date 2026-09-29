package com.saney.renaultdocs

import java.util.zip.Deflater
import org.junit.Assert.assertEquals
import org.junit.Test

class RdpkgZipWriterTest {
    @Test
    fun textAndMetadataUseBestSpeedCompression() {
        listOf(
            "_renault/runtime-ir-index.json",
            "RUS/HTM/MENU/101.HTM",
            "COMMUN/JS/VISU.JS",
            "skin.css",
        ).forEach {
            path ->
            assertEquals(
                Deflater.BEST_SPEED,
                RdpkgZipWriter.compressionLevelFor(
                    relativePath = path,
                    size = 1024L,
                ),
            )
        }
    }

    @Test
    fun binaryPayloadUsesNoCompression() {
        listOf(
            "COMMUN/PDF/PC/S7.PDF",
            "COMMUN/GIF/icon.GIF",
            "_renault/fast-content-test.zip",
            "photo.jpg",
        ).forEach {
            path ->
            assertEquals(
                Deflater.NO_COMPRESSION,
                RdpkgZipWriter.compressionLevelFor(
                    relativePath = path,
                    size = 20L * 1024L * 1024L,
                ),
            )
        }
    }
}
