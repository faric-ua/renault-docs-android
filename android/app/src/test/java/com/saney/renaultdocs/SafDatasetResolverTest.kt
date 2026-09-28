package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafDatasetResolverTest {
    @Test
    fun normalizeRemovesDotAndDuplicateSeparators() {
        assertEquals(
            "A/B/C.HTM",
            SafDatasetResolver.normalize(
                "A//./B/C.HTM"
            ),
        )
    }

    @Test
    fun normalizeRejectsTraversal() {
        assertNull(
            SafDatasetResolver.normalize(
                "A/../secret.txt"
            )
        )
    }
}
