package com.saney.renaultdocs

import android.net.Uri

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

            // v0.5.110: also a folder inside a tree (converter copy).
            val documentId =
                SafDatasetResolver
                    .rootDocumentIdOf(
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
