package com.saney.renaultdocs

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Signals Android 15+ dataSync foreground-service quota exhaustion.
 *
 * onTimeout MUST still stopSelf() within seconds; this signal cooperatively
 * prevents further progress updates and success publication by workers which
 * happen to keep running briefly while Android tears the service down.
 *
 * It does not make long blocking I/O magically interruptible or promise
 * automatic restart after the dataSync quota is exhausted.
 */
internal class DataSyncTimeoutGate {
    private val expired = AtomicBoolean(false)

    val isExpired: Boolean
        get() = expired.get()

    fun expire(): Boolean = expired.compareAndSet(false, true)

    fun checkActive() {
        if (isExpired) throw DataSyncQuotaExpiredException()
    }
}

internal class DataSyncQuotaExpiredException : RuntimeException(
    "Android досягнув ліміту фонової роботи dataSync. Незавершену операцію зупинено; уже готові дані збережено.",
)

internal object DataSyncTimeoutUi {
    const val MESSAGE =
        "Android досягнув ліміту фонової роботи dataSync. " +
            "Незавершену операцію зупинено. Уже завершені томи не видаляються. " +
            "Відкрий Renault Docs, щоб перевірити результат перед новим запуском."
}
