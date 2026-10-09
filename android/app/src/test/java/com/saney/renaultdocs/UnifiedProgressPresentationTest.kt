package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnifiedProgressPresentationTest {
    @Test
    fun fileCounterIsAlwaysSeparateFromStage() {
        val detail = OperationStatusDetailFormatter.split(
            "Розпаковую ZIP… · Файлів: 2312 / 3360",
        )
        assertEquals("Розпаковую ZIP…", detail.stage)
        assertEquals("Файлів: 2312 / 3360", detail.counter)
    }

    @Test
    fun missingTotalRetainsCounterSlotAndStableStage() {
        val detail = OperationStatusDetailFormatter.split("Копіюю… · Файлів: 121")
        assertEquals("Копіюю…", detail.stage)
        assertEquals("Файлів: 121", detail.counter)
        assertNull(OperationStatusDetailFormatter.split("Готую…").counter)
    }

    @Test
    fun volumeIdentityAndFilenameAreNotAltered() {
        val detail = OperationStatusDetailFormatter.split(
            "Пакую том NT8340A · 2006-04-18… · Файлів: 41 / 100",
        )
        assertEquals("Пакую том NT8340A · 2006-04-18…", detail.stage)
        assertEquals("Файлів: 41 / 100", detail.counter)
    }

    @Test
    fun normalizedProgressClampsAndUsesLongArithmetic() {
        assertEquals(500, SharedOperationProgressBar.normalized(1, 2))
        assertEquals(1000, SharedOperationProgressBar.normalized(Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(0, SharedOperationProgressBar.normalized(-3, 100))
        assertEquals(1000, SharedOperationProgressBar.normalized(120, 100))
        assertNull(SharedOperationProgressBar.normalized(2, 0))
        assertNull(SharedOperationProgressBar.normalized(null, 20))
    }
}
