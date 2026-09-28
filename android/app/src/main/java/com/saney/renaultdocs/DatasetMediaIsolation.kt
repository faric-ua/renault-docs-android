package com.saney.renaultdocs

import android.content.Context
import androidx.documentfile.provider.DocumentFile

object DatasetMediaIsolation {
    fun ensure(
        context: Context,
        root: DocumentFile,
    ): DocumentFile {
        root.findFile(
            ".nomedia",
        )?.let {
            return it
        }

        val marker =
            root.createFile(
                "application/octet-stream",
                ".nomedia",
            )
                ?: error(
                    "Не вдалося створити .nomedia у dataset."
                )

        context.contentResolver
            .openOutputStream(
                marker.uri,
                "wt",
            )
            ?.use {
                // Empty marker file.
            }
            ?: error(
                "Не вдалося записати .nomedia у dataset."
            )

        return marker
    }
}
