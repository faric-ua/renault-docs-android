package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseProgressUiContractTest {
    @Test
    fun wrapsLongTerminalHashesButPreservesCopyableOriginal() {
        val digest = "71948d2656429b103db988ff72e27ae4c2f53454d3f1d074fd0dc3b2b1aac3bc"
        val original = "Готово · NT8275A · 2005-01-03 · 333 native\nSHA-256: $digest"
        val display = OperationStatusDisplayFormat.wrapHashesForDisplay(original)
        assertTrue(display.contains("SHA-256:\n"))
        assertEquals(64, display.substringAfter("SHA-256:").filter { it.isLetterOrDigit() }.length)
        assertEquals(original, display.replace("\n", "").let {
            // The original first newline is retained by the preceding status;
            // only visual digest line wrapping is stripped for this comparison.
            val label = "SHA-256:"
            val labelIndex = original.indexOf(label)
            original.substring(0, labelIndex) + label + it.substringAfter(label)
        })
        assertEquals(64, digest.length)
    }

    @Test
    fun leavesUnknownOrShortHashesUntouched() {
        assertEquals("SHA-256: abc", OperationStatusDisplayFormat.wrapHashesForDisplay("SHA-256: abc"))
        assertEquals("Підготовка", OperationStatusDisplayFormat.wrapHashesForDisplay("Підготовка"))
    }

    @Test
    fun colorsDependOnRealTerminalSemantics() {
        assertEquals(OperationTerminalOutcome.SUCCESS,
            OperationTerminalOutcome.fromTitle("Створення .rdpkg завершено"))
        assertEquals(OperationTerminalOutcome.FAILED,
            OperationTerminalOutcome.fromTitle("Імпорт тому · помилка"))
        assertEquals(OperationTerminalOutcome.CANCELLED,
            OperationTerminalOutcome.fromTitle("Створення .rdpkg скасовано"))
        assertEquals(OperationTerminalOutcome.NEUTRAL,
            OperationTerminalOutcome.fromTitle("Том уже є"))
    }

    @Test
    fun extractedCountSurvivesTransitionToUnmeasuredValidation() {
        val complete = OperationProgress.measured(
            stage = "Розпаковано .rdpkg…",
            current = 337,
            total = 337,
            itemCurrent = 337,
            itemTotal = 337,
            itemLabel = "Файлів",
        )
        assertEquals(337, complete.normalizedCurrent)
        assertTrue(complete.displayText().contains("Файлів: 337 / 337"))

        val validation = OperationProgress(
            stage = "Перевіряю пакет…",
            itemCurrent = 337,
            itemTotal = 337,
            itemLabel = "Розпаковано файлів",
        )
        assertFalse(validation.isDeterminate)
        assertTrue(validation.displayText().contains("Розпаковано файлів: 337 / 337"))
    }

    @Test
    fun measuredValuesStartFromZeroAndClampSafely() {
        assertEquals(0, SharedOperationProgressBar.normalized(0, 5357))
        assertEquals(1000, SharedOperationProgressBar.normalized(5357, 5357))
        assertTrue(SharedOperationProgressBar.normalized(3432, 5357)!! in 600..700)
        assertEquals(null, SharedOperationProgressBar.normalized(null, 5357))
        assertEquals(null, SharedOperationProgressBar.normalized(12, 0))
    }
}
