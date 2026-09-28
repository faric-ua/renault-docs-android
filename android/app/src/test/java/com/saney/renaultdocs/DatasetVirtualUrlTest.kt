package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetVirtualUrlTest {
    @Test
    fun entrypointBecomesLocalVirtualUrl() {
        assertEquals(
            "https://renault.local/_renault/START.html",
            DatasetVirtualUrl.urlFor("_renault/START.html"),
        )
    }

    @Test
    fun spacesAreEncodedButRoundTripToRelativePath() {
        val url = DatasetVirtualUrl.urlFor(
            "Laguna X74 NT8183A 2001_01_22/INDEX.HTM"
        )

        assertTrue(url.contains("%20"))
        assertEquals(
            "Laguna X74 NT8183A 2001_01_22/INDEX.HTM",
            DatasetVirtualUrl.relativePath(url),
        )
    }

    @Test
    fun traversalIsRejected() {
        assertNull(
            DatasetVirtualUrl.relativePath(
                "https://renault.local/../secret.txt"
            )
        )
    }

    @Test
    fun externalHostIsNotLocal() {
        assertFalse(
            DatasetVirtualUrl.isLocal(
                "https://example.com/test.htm"
            )
        )
    }
}
