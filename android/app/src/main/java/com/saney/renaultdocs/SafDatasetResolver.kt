package com.saney.renaultdocs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.ParcelFileDescriptor
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SafDatasetResolver(
    context: Context,
    private val treeUri: Uri,
) {
    data class Node(
        val relativePath: String,
        val name: String,
        val documentId: String,
        val mimeType: String,
        val uri: Uri,
    ) {
        val isDirectory: Boolean
            get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    private data class DirectoryIndex(
        val exact: Map<String, Node>,
        val uniqueLowercase: Map<String, Node>,
    )

    private val contentResolver: ContentResolver =
        context.contentResolver

    private val rootDocumentId =
        DocumentsContract.getTreeDocumentId(treeUri)

    private val root = Node(
        relativePath = "",
        name = "",
        documentId = rootDocumentId,
        mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
        uri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            rootDocumentId,
        ),
    )

    private val nodeCache =
        ConcurrentHashMap<String, Node>()

    private val missingPaths =
        ConcurrentHashMap.newKeySet<String>()

    private val directoryCache =
        ConcurrentHashMap<String, DirectoryIndex>()

    private val directoryLocks =
        ConcurrentHashMap<String, Any>()

    init {
        nodeCache[""] = root
    }

    fun resolve(
        relativePath: String,
    ): Node? {
        val normalized = normalize(relativePath)
            ?: return null

        nodeCache[normalized]?.let {
            return it
        }

        if (normalized in missingPaths) {
            return null
        }

        if (normalized.isBlank()) {
            return root
        }

        var current = root
        var currentPath = ""

        for (part in normalized.split('/')) {
            val childPath = if (currentPath.isBlank()) {
                part
            } else {
                "$currentPath/$part"
            }

            val cached =
                nodeCache[childPath]

            if (cached != null) {
                current = cached
                currentPath = childPath
                continue
            }

            if (!current.isDirectory) {
                missingPaths += childPath
                return null
            }

            val index = directoryIndex(
                directoryPath = currentPath,
                directory = current,
            )

            val next = index.exact[part]
                ?: index.uniqueLowercase[
                    part.lowercase(Locale.ROOT)
                ]

            if (next == null) {
                missingPaths += childPath
                return null
            }

            nodeCache[childPath] = next
            current = next
            currentPath = childPath
        }

        return current
    }

    fun openInputStream(
        relativePath: String,
    ): InputStream? {
        val node = resolve(relativePath)
            ?: return null

        if (node.isDirectory) {
            return null
        }

        return contentResolver.openInputStream(
            node.uri,
        )
    }

    fun openFileDescriptor(
        relativePath: String,
    ): ParcelFileDescriptor? {
        val node = resolve(relativePath)
            ?: return null

        if (node.isDirectory) {
            return null
        }

        return contentResolver.openFileDescriptor(
            node.uri,
            "r",
        )
    }

    private fun directoryIndex(
        directoryPath: String,
        directory: Node,
    ): DirectoryIndex {
        directoryCache[directoryPath]?.let {
            return it
        }

        val lock = directoryLocks.computeIfAbsent(
            directoryPath,
        ) {
            Any()
        }

        synchronized(lock) {
            directoryCache[directoryPath]?.let {
                return it
            }

            val children = queryChildren(
                directoryPath = directoryPath,
                directory = directory,
            )

            val exact = children.associateBy {
                it.name
            }

            val grouped = children.groupBy {
                it.name.lowercase(Locale.ROOT)
            }

            val uniqueLowercase = buildMap {
                grouped.forEach { (key, values) ->
                    if (values.size == 1) {
                        put(
                            key,
                            values.single(),
                        )
                    }
                }
            }

            return DirectoryIndex(
                exact = exact,
                uniqueLowercase = uniqueLowercase,
            ).also {
                directoryCache[directoryPath] = it
            }
        }
    }

    private fun queryChildren(
        directoryPath: String,
        directory: Node,
    ): List<Node> {
        val childrenUri =
            DocumentsContract
                .buildChildDocumentsUriUsingTree(
                    treeUri,
                    directory.documentId,
                )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        val result = mutableListOf<Node>()

        contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            )
            val nameIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
            val mimeIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )

            while (cursor.moveToNext()) {
                val documentId =
                    cursor.getString(idIndex)
                val name =
                    cursor.getString(nameIndex)
                val mimeType =
                    cursor.getString(mimeIndex)
                        ?: "application/octet-stream"

                val path = if (
                    directoryPath.isBlank()
                ) {
                    name
                } else {
                    "$directoryPath/$name"
                }

                result += Node(
                    relativePath = path,
                    name = name,
                    documentId = documentId,
                    mimeType = mimeType,
                    uri = DocumentsContract
                        .buildDocumentUriUsingTree(
                            treeUri,
                            documentId,
                        ),
                )
            }
        }

        return result
    }

    companion object {
        fun normalize(
            path: String,
        ): String? {
            val parts = path
                .replace('\\', '/')
                .split('/')
                .filter {
                    it.isNotBlank() &&
                        it != "."
                }

            if (parts.any { it == ".." }) {
                return null
            }

            return parts.joinToString("/")
        }
    }
}
