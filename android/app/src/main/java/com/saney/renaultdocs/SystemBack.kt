package com.saney.renaultdocs

import android.app.Activity
import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/**
 * v0.5.131: system «Назад» on Android 16.
 *
 * The app targets Android 16 (targetSdk 36). There the system «Назад» no longer
 * calls Activity.onBackPressed() — the screen simply closes. Found on the phone
 * 2026-10-14 07:22: «Назад» in a guide section closed the whole guide.
 *
 * Each screen with its own «Назад» rule registers that rule here as well.
 * On Android 13–15 (no opt-in in the manifest) the system ignores this callback
 * and still calls onBackPressed(), so the behaviour is the same everywhere.
 * [handle] returns true when it consumed «Назад»; otherwise the screen finishes.
 */
object SystemBack {
    fun register(
        activity: Activity,
        handle: () -> Boolean,
    ): Any? {
        if (Build.VERSION.SDK_INT < 33) {
            return null
        }
        val callback =
            OnBackInvokedCallback {
                if (!handle()) {
                    activity.finish()
                }
            }
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback,
        )
        return callback
    }

    fun unregister(
        activity: Activity,
        token: Any?,
    ) {
        if (Build.VERSION.SDK_INT >= 33 && token is OnBackInvokedCallback) {
            activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(
                token,
            )
        }
    }
}
