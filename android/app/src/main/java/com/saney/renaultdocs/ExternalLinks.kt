package com.saney.renaultdocs

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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

    const val PROJECT_CATALOG_MANIFEST_FILE_ID =
        "1mH0YJo1ts_GzpXz9Y4bx7Aj9DKBSgwwA"

    const val PROJECT_CATALOG_MANIFEST_URL =
        "https://drive.usercontent.google.com/download?id=" +
            PROJECT_CATALOG_MANIFEST_FILE_ID +
            "&export=download&confirm=t"

    const val SUPPORT_URL =
        ""

    fun driveDownloadUrl(
        fileId: String,
    ): String =
        "https://drive.usercontent.google.com/download?id=" +
            Uri.encode(
                fileId,
            ) +
            "&export=download&confirm=t"

    /**
     * Opens a public web destination in the user's default browser when possible.
     *
     * This intentionally avoids handing the Drive catalog straight to the
     * Google Drive app, because Drive may show an account chooser on phones
     * with several Google accounts. A public catalog should not look like an
     * account connection flow inside Renault Docs.
     */
    fun openWeb(
        activity: Activity,
        url: String,
    ): Boolean {
        if (
            url.isBlank()
        ) {
            return false
        }

        val parsed =
            Uri.parse(
                url,
            )
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                parsed,
            ).apply {
                addCategory(
                    Intent.CATEGORY_BROWSABLE,
                )
            }

        val browserProbe =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://example.com",
                ),
            ).apply {
                addCategory(
                    Intent.CATEGORY_BROWSABLE,
                )
            }

        val browserPackage =
            activity.packageManager
                .resolveActivity(
                    browserProbe,
                    PackageManager.MATCH_DEFAULT_ONLY,
                )
                ?.activityInfo
                ?.packageName

        if (
            !browserPackage.isNullOrBlank()
        ) {
            intent.setPackage(
                browserPackage,
            )
        }

        return runCatching {
            activity.startActivity(
                intent,
            )
            true
        }
            .recoverCatching {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        parsed,
                    )
                )
                true
            }
            .getOrDefault(
                false,
            )
    }
}
