package com.saney.renaultdocs

import android.content.Context
import android.os.PowerManager

internal class BackgroundWorkWakeLock(
    context: Context,
    tag: String,
) {
    private val wakeLock =
        context
            .getSystemService(
                PowerManager::class.java,
            )
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                context.packageName +
                    ":" +
                    tag,
            )
            .apply {
                setReferenceCounted(
                    false,
                )
            }

    fun acquire() {
        if (
            !wakeLock.isHeld
        ) {
            wakeLock.acquire(
                WAKE_LOCK_TIMEOUT_MS,
            )
        }
    }

    fun release() {
        if (
            wakeLock.isHeld
        ) {
            wakeLock.release()
        }
    }

    private companion object {
        private const val WAKE_LOCK_TIMEOUT_MS =
            6L *
                60L *
                60L *
                1000L
    }
}
