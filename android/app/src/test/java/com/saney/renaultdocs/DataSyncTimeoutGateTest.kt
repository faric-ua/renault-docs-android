package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DataSyncTimeoutGateTest {
    @Test
    fun startsActiveAndExpiresExactlyOnce() {
        val gate = DataSyncTimeoutGate()
        assertFalse(gate.isExpired)
        gate.checkActive()
        assertTrue(gate.expire())
        assertTrue(gate.isExpired)
        assertFalse(gate.expire())
    }

    @Test
    fun expiryBlocksFurtherProgressAndSuccess() {
        val gate = DataSyncTimeoutGate()
        var published = false
        gate.expire()
        try {
            gate.checkActive()
            published = true
            fail("Expired operation must not continue")
        } catch (expected: DataSyncQuotaExpiredException) {
            assertTrue(expected.message.orEmpty().contains("dataSync"))
        }
        assertFalse(published)
    }

    @Test
    fun neverResetsUntilASeparateNewServiceInstance() {
        val expired = DataSyncTimeoutGate()
        expired.expire()
        assertTrue(expired.isExpired)
        assertFalse(DataSyncTimeoutGate().isExpired)
        assertEquals(
            true,
            DataSyncTimeoutUi.MESSAGE.contains("Уже завершені томи не видаляються"),
        )
    }
}
