package com.saney.renaultdocs

import java.net.URI

object DatasetVirtualUrl {
    const val HOST = "renault.local"
    private const val SCHEME = "https"

    fun urlFor(relativePath: String): String {
        val safePath = normalizeRelativePath(relativePath)
            ?: throw IllegalArgumentException("Unsafe dataset path: $relativePath")

        return URI(
            SCHEME,
            HOST,
            "/" + safePath,
            null,
        ).toASCIIString()
    }

    fun relativePath(url: String): String? {
        val uri = runCatching {
            URI(url)
        }.getOrNull() ?: return null

        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) {
            return null
        }
        if (!uri.host.equals(HOST, ignoreCase = true)) {
            return null
        }

        return normalizeRelativePath(
            uri.path.orEmpty().trimStart('/'),
        )
    }

    fun isLocal(url: String): Boolean =
        relativePath(url) != null

    private fun normalizeRelativePath(path: String): String? {
        val normalized = path
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." }

        if (normalized.isEmpty()) {
            return ""
        }

        if (normalized.any { it == ".." }) {
            return null
        }

        return normalized.joinToString("/")
    }
}
