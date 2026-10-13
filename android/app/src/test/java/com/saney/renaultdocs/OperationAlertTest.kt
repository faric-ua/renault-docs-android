package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class OperationAlertTest {
    @Test
    fun finishedOperationsSoundAsDone() {
        assertEquals(OperationAlert.Kind.DONE, OperationAlert.kindOf("Окремі .rdpkg готові: 17 / 17"))
        assertEquals(OperationAlert.Kind.DONE, OperationAlert.kindOf("Проєкт готовий для поширення."))
        assertEquals(OperationAlert.Kind.DONE, OperationAlert.kindOf("Експорт .rdpkg завершено."))
    }

    @Test
    fun errorsSoundAsErrors() {
        assertEquals(OperationAlert.Kind.ERROR, OperationAlert.kindOf("Помилка експорту окремих томів."))
        assertEquals(OperationAlert.Kind.ERROR, OperationAlert.kindOf("Не вдалося імпортувати том"))
    }

    @Test
    fun ownCancelStaysSilent() {
        assertEquals(OperationAlert.Kind.SILENT, OperationAlert.kindOf("Експорт окремих томів скасовано."))
    }
}
