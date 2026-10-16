package com.saney.renaultdocs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileInputStream
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
        /** v0.5.138: the file itself, for volumes of our own provider (opened directly). */
        val file: File? = null,
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

    private val appContext: Context =
        context.applicationContext

    // v0.5.110: a dataset may be addressed as a folder inside a permitted
    // tree (…/tree/<parent>/document/<copy>), e.g. a converter copy inside
    // the chosen destination. Its root is that folder, not the parent tree.
    private val rootDocumentId =
        rootDocumentIdOf(treeUri)

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

    // Stage 0 of the read-from-.rdpkg plan (TODO №16): which volume and where it lies.
    private val ownPackageId: String? by lazy {
        if (treeUri.authority == LocalDatasetDocumentsProvider.authority) {
            VolumeReadStats.packageIdOf(rootDocumentId)
        } else {
            null
        }
    }

    /** The tree may address a folder inside the volume (`pkg:<id>/<sub>`): that folder. */
    private val ownSubPath: String by lazy {
        val id = ownPackageId ?: return@lazy ""
        val tail = rootDocumentId.removePrefix("pkg:").removePrefix(id).trimStart('/', ':')
        normalize(tail).orEmpty()
    }

    private val statsVolume: String by lazy {
        ownPackageId ?: if (treeUri.authority == LocalDatasetDocumentsProvider.authority) rootDocumentId else "saf"
    }

    private val statsPlace: String by lazy {
        VolumeReadStats.placeLabel(
            ownPackageId?.let { runCatching { VolumeStorage.placeOf(appContext, it) }.getOrNull() },
        )
    }

    /**
     * v0.5.138 (log 110, M30s): our own volumes have known paths, so a file is checked
     * directly — one file-system call — instead of listing every folder on the way
     * (330 entries took 11–12.5 s on the memory card). Shared by the whole app
     * ([VolumeFileIndex]). Null — not our volume, or not found exactly; the old
     * listing (which also matches a different letter case) is used then.
     */
    private fun directNode(normalized: String): Node? {
        val packageId = ownPackageId ?: return null
        // Cached per tree: the same file reached through a sub-folder tree has another uri.
        val cacheKey = rootDocumentId + "|" + normalized
        VolumeFileIndex.nodeCached<Node>(packageId, cacheKey)?.let { return it }
        val inVolume =
            when {
                ownSubPath.isBlank() -> normalized
                normalized.isBlank() -> ownSubPath
                else -> "$ownSubPath/$normalized"
            }
        val root =
            VolumeFileIndex.root(packageId) {
                runCatching {
                    LocalDatasetDocumentsProvider.packageDirectory(appContext, packageId)
                }.getOrNull()?.takeIf { it.isDirectory }
            } ?: return null
        val file = VolumeFileIndex.fileIn(root, inVolume)
        val attributes = VolumeFileIndex.attributes(file) ?: return null
        val documentId = LocalDatasetDocumentsProvider.documentId(packageId, inVolume)
        val node =
            Node(
                relativePath = normalized,
                name = if (normalized.isBlank()) "" else normalized.substringAfterLast('/'),
                documentId = documentId,
                mimeType = LocalDatasetDocumentsProvider.mimeTypeFor(file.name, attributes.isDirectory),
                uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                file = file,
            )
        VolumeFileIndex.rememberNode(packageId, cacheKey, node)
        return node
    }

    private fun timedOpen(
        relativePath: String,
        open: (Node) -> Any?,
    ): Any? {
        val node = resolve(relativePath)
        if (node == null) {
            VolumeReadStats.recordOpen(statsVolume, 0L, found = false)
            return null
        }
        if (node.isDirectory) return null
        val started = SystemClock.elapsedRealtime()
        val result = open(node)
        val ms = SystemClock.elapsedRealtime() - started
        val totals = VolumeReadStats.recordOpen(statsVolume, ms, found = result != null)
        runCatching {
            if (ms >= VolumeReadStats.SLOW_OPEN_MS) {
                Log.i(VolumeReadStats.TAG, VolumeReadStats.openLine(statsVolume, statsPlace, relativePath, ms))
            }
            if (VolumeReadStats.shouldSummarize(totals)) {
                Log.i(VolumeReadStats.TAG, VolumeReadStats.summaryLine(statsVolume, statsPlace, totals))
            }
        }
        return result
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

        directNode(normalized)?.let {
            nodeCache[normalized] = it
            return it
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
    ): InputStream? =
        timedOpen(relativePath) { node ->
            // v0.5.138: our own volume — open the file directly, without a round trip
            // through the provider (which found the volume and the path again).
            node.file?.let { runCatching { FileInputStream(it) }.getOrNull() }
                ?: contentResolver.openInputStream(
                    node.uri,
                )
        } as InputStream?

    fun openFileDescriptor(
        relativePath: String,
    ): ParcelFileDescriptor? =
        timedOpen(relativePath) { node ->
            node.file?.let {
                runCatching { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull()
            }
                ?: contentResolver.openFileDescriptor(
                    node.uri,
                    "r",
                )
        } as ParcelFileDescriptor?

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

            val listingStarted = SystemClock.elapsedRealtime()
            val children = queryChildren(
                directoryPath = directoryPath,
                directory = directory,
            )
            val listingMs = SystemClock.elapsedRealtime() - listingStarted
            val listingTotals = VolumeReadStats.recordListing(statsVolume, children.size, listingMs)
            runCatching {
                if (listingMs >= VolumeReadStats.SLOW_LISTING_MS) {
                    Log.i(
                        VolumeReadStats.TAG,
                        VolumeReadStats.listingLine(statsVolume, statsPlace, directoryPath, children.size, listingMs),
                    )
                    Log.i(VolumeReadStats.TAG, VolumeReadStats.summaryLine(statsVolume, statsPlace, listingTotals))
                }
            }

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
        /** Root folder id for a tree address or for a folder inside a tree. */
        fun rootDocumentIdOf(
            uri: Uri,
        ): String {
            val segments = uri.pathSegments
            return if (
                segments.size >= 4 &&
                segments[0] == "tree" &&
                segments[2] == "document"
            ) {
                segments[3]
            } else {
                DocumentsContract.getTreeDocumentId(uri)
            }
        }

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
