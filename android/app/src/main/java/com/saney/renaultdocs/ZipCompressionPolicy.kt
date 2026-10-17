package com.saney.renaultdocs

import android.content.Context
import android.os.Build

/**
 * v0.5.145 (Phone QA M30s 2026-10-17 04:24): on the M30s (Android 11) the compressor writes
 * broken ZIPs — first seen in volume archives (v0.5.141, a different zlib error every run),
 * then caught by [ZipStreamCheck] in «Підготувати і поділитися томом» NT8228A: «Файл вийшов
 * пошкоджений: invalid code lengths set». The A26 (Android 16) writes them correctly.
 *
 * So on such a phone volume packages are written STORED (no compressor at all, like the
 * volume archive since v0.5.142). Decided once:
 * - Android 11 and older — STORED (the phone where it breaks is Android 11; the same
 *   steps with upstream zlib on a PC never broke, so we do not trust older system zlib);
 * - any newer phone where [ZipStreamCheck] ever caught a broken file — STORED from then on
 *   ([rememberBroken]); the owner just repeats the operation.
 * Other phones keep the compressor, so files made there stay byte-for-byte the same as the
 * ones already on Google Drive (re-export compares by sha256).
 *
 * A STORED package is a normal ZIP: every version of the app, and any unzip tool, reads it.
 */
object ZipCompressionPolicy {
    private const val PREFS = "zip_compression"
    private const val KEY_BROKEN = "compressor_broken"

    fun storedOnly(context: Context): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ||
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_BROKEN, false)

    fun rememberBroken(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BROKEN, true)
            .commit()
    }
}
