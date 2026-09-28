package com.saney.renaultdocs

import android.net.Uri
import android.provider.DocumentsContract

object SafDisplayPath {
    fun tree(
        uriText: String?,
    ): String? {
        if (
            uriText.isNullOrBlank()
        ) {
            return null
        }

        return runCatching {
            val uri =
                Uri.parse(
                    uriText,
                )

            val documentId =
                DocumentsContract
                    .getTreeDocumentId(
                        uri,
                    )

            val storage =
                documentId
                    .substringBefore(
                        ':',
                        "",
                    )

            val path =
                documentId
                    .substringAfter(
                        ':',
                        "",
                    )
                    .let {
                        Uri.decode(
                            it,
                        )
                    }
                    .replace(
                        '\\',
                        '/',
                    )
                    .trim(
                        '/',
                    )

            when {
                storage.equals(
                    "primary",
                    ignoreCase = true,
                ) &&
                    path.isNotBlank() ->
                    path

                storage.equals(
                    "primary",
                    ignoreCase = true,
                ) ->
                    "Внутрішня пам’ять"

                path.isNotBlank() &&
                    storage.isNotBlank() ->
                    "$storage/$path"

                storage.isNotBlank() ->
                    storage

                else ->
                    null
            }
        }.getOrNull()
    }
}
