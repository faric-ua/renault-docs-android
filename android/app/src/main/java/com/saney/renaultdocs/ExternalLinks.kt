package com.saney.renaultdocs

import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * User-facing external destinations.
 *
 * Keep these in one place so UI code never duplicates URLs.
 * PROJECT_CATALOG_URL points to the public Renault Docs project catalog.
 * SUPPORT_URL stays empty until the owner chooses the final support destination.
 */
object ExternalLinks {
    const val PROJECT_CATALOG_URL =
        "https://drive.google.com/drive/folders/1UyN4UIgaNMrpG-5mLuDBd9laFEbwmb4Y"

    const val SUPPORT_URL =
        ""

    fun open(
        activity: Activity,
        url: String,
    ): Boolean {
        if (
            url.isBlank()
        ) {
            return false
        }

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    url,
                ),
            )

        return runCatching {
            activity.startActivity(
                intent,
            )
            true
        }
            .getOrDefault(
                false,
            )
    }
}
