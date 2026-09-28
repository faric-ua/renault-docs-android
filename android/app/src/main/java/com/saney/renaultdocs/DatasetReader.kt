package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DatasetReader {
    fun read(
        context: Context,
        treeUri: Uri,
    ): Result<DatasetRecord> = runCatching {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Android не зміг відкрити вибрану папку")

        require(root.isDirectory) {
            "Вибраний об'єкт не є папкою"
        }

        val manifest = root.findFile("renault-dataset.json")
            ?: error("У корені немає renault-dataset.json")

        require(manifest.isFile) {
            "renault-dataset.json не є файлом"
        }

        val text = context.contentResolver.openInputStream(manifest.uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Не вдалося прочитати renault-dataset.json")

        val metadata = DatasetManifestParser.parse(text)

        require(findRelative(root, metadata.openEntrypoint) != null) {
            "Не знайдено стартову сторінку: " + metadata.openEntrypoint
        }

        DatasetRecord.from(
            metadata = metadata,
            treeUri = treeUri.toString(),
        )
    }

    private fun findRelative(
        root: DocumentFile,
        relativePath: String,
    ): DocumentFile? {
        val parts = relativePath
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." }

        if (parts.any { it == ".." }) {
            return null
        }

        var current = root
        for ((index, part) in parts.withIndex()) {
            val next = current.findFile(part) ?: return null
            if (index < parts.lastIndex && !next.isDirectory) {
                return null
            }
            current = next
        }

        return current.takeIf { it.isFile }
    }
}
