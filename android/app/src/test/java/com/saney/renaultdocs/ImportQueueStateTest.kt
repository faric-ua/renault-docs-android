package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportQueueStateTest {
    private val uris = listOf("content://a/NT8183A.rdpkg", "content://a/NT8218A.rdpkg", "content://a/NT8236A.rdpkg")

    @Test
    fun walksThroughAllFiles() {
        val (first, afterFirst) = ImportQueueState.start("laguna", uris).next()!!
        assertEquals(uris[0], first)
        assertEquals("Том 1 з 3", afterFirst.label())
        val (second, afterSecond) = afterFirst.markDone().next()!!
        assertEquals(uris[1], second)
        assertEquals("Том 2 з 3", afterSecond.label())
        val (third, afterThird) = afterSecond.markFailed("NT8218A.rdpkg").next()!!
        assertEquals(uris[2], third)
        assertEquals("Том 3 з 3", afterThird.label())
        val end = afterThird.markDone()
        assertNull(end.next())
        assertTrue(end.isFinished)
        assertEquals("Імпортовано томів: 2 з 3. Не вийшло: NT8218A.rdpkg.", end.summary())
    }

    @Test
    fun survivesSaveAndLoad() {
        val state = ImportQueueState.start("laguna", uris).next()!!.second.markFailed("x.rdpkg")
        assertEquals(state, ImportQueueState.decode(state.encode()))
    }

    @Test
    fun brokenOrEmptyIsInactive() {
        assertFalse(ImportQueueState.decode(null).isActive)
        assertFalse(ImportQueueState.decode("garbage").isActive)
        assertFalse(ImportQueueState.start("laguna", uris.take(1)).isActive)
    }
}
