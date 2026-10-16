package com.saney.renaultdocs

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale

class LocalDatasetDocumentsProvider :
    DocumentsProvider() {
    override fun onCreate(): Boolean =
        true

    override fun queryRoots(
        projection: Array<out String>?,
    ): Cursor =
        MatrixCursor(
            projection
                ?: DEFAULT_ROOT_PROJECTION,
        )

    override fun queryDocument(
        documentId: String,
        projection: Array<out String>?,
    ): Cursor {
        val resolved =
            resolve(
                documentId,
            )

        return MatrixCursor(
            projection
                ?: DEFAULT_DOCUMENT_PROJECTION,
        ).apply {
            include(
                documentId =
                    documentId,
                name =
                    resolved.name,
                attributes =
                    resolved.attributes,
            )
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val parent =
            resolve(
                parentDocumentId,
            )

        if (
            !parent.attributes.isDirectory
        ) {
            throw FileNotFoundException(
                "Document is not a directory: " +
                    parentDocumentId,
            )
        }

        // Archives plan v2, stage 1: a volume read from its archive lists the archive's
        // table of contents — no file-system call at all.
        parent.archive?.let { archive ->
            val listed =
                archive.children(parent.relativePath)
                    ?: throw FileNotFoundException("Document is not a directory: " + parentDocumentId)
            return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
                listed.forEach { child ->
                    include(
                        documentId = documentId(parent.packageId, child.path),
                        name = child.name,
                        attributes = child.attributes,
                    )
                }
            }
        }

        // v0.5.138 (log 110): one file-system call per entry instead of 4–5 — on the
        // memory card each call goes through FUSE (~30 ms).
        val folder =
            parent.file
                ?: throw FileNotFoundException("Document is not a directory: " + parentDocumentId)
        val children =
            folder
                .listFiles()
                .orEmpty()
                .mapNotNull { child ->
                    VolumeFileIndex.attributes(child)?.let { child to it }
                }
                .sortedWith(
                    compareBy<Pair<File, VolumeFileIndex.Attributes>> {
                        !it.second.isDirectory
                    }.thenBy {
                        it.first.name.lowercase(
                            Locale.ROOT,
                        )
                    },
                )

        return MatrixCursor(
            projection
                ?: DEFAULT_DOCUMENT_PROJECTION,
        ).apply {
            children.forEach { (child, attributes) ->
                val relative =
                    if (
                        parent.relativePath
                            .isBlank()
                    ) {
                        child.name
                    } else {
                        parent.relativePath +
                            "/" +
                            child.name
                    }

                include(
                    documentId =
                        documentId(
                            packageId =
                                parent.packageId,
                            relativePath =
                                relative,
                        ),
                    name =
                        child.name,
                    attributes =
                        attributes,
                )
            }
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        if (
            mode !=
            "r"
        ) {
            throw FileNotFoundException(
                "Renault package documents are read-only.",
            )
        }

        val resolved =
            resolve(
                documentId,
            )

        if (
            resolved.attributes.isDirectory
        ) {
            throw FileNotFoundException(
                "Document is not a file: " +
                    documentId,
            )
        }

        resolved.archive?.let { archive ->
            return openFromArchive(archive, resolved)
        }

        return ParcelFileDescriptor.open(
            requireNotNull(resolved.file),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    /**
     * Archives plan v2, stage 1. A PDF needs a real file (PdfRenderer jumps around in it):
     * it is copied out of the archive once into the phone cache ([VolumeArchiveFileCache]).
     * Anything else is streamed through a pipe straight from the archive.
     */
    private fun openFromArchive(
        archive: VolumeArchive,
        resolved: ResolvedDocument,
    ): ParcelFileDescriptor {
        val appContext = requireNotNull(context)
        if (resolved.name.endsWith(".pdf", ignoreCase = true)) {
            val real =
                runCatching {
                    VolumeArchiveAccess.realFile(appContext, archive, resolved.packageId, resolved.relativePath)
                }.getOrElse {
                    VolumeFileIndex.forget(resolved.packageId)
                    null
                } ?: throw FileNotFoundException("Package document not found: " + resolved.relativePath)
            return ParcelFileDescriptor.open(real, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val input =
            runCatching { archive.open(resolved.relativePath) }.getOrElse {
                VolumeFileIndex.forget(resolved.packageId)
                null
            } ?: throw FileNotFoundException("Package document not found: " + resolved.relativePath)
        val pipe = ParcelFileDescriptor.createReliablePipe()
        Thread {
            val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
            try {
                input.use { it.copyTo(output, 64 * 1024) }
                output.close()
            } catch (error: Exception) {
                // The reader sees an error, not a silently cut file.
                runCatching { pipe[1].closeWithError(error.message ?: "archive read failed") }
                runCatching { input.close() }
            }
        }.apply {
            name = "rdpkg-archive-pipe"
            isDaemon = true
        }.start()
        return pipe[0]
    }

    override fun getDocumentType(
        documentId: String,
    ): String =
        resolve(
            documentId,
        ).let {
            mimeTypeFor(
                it.name,
                it.attributes.isDirectory,
            )
        }

    override fun isChildDocument(
        parentDocumentId: String,
        documentId: String,
    ): Boolean {
        val parent =
            runCatching {
                resolve(
                    parentDocumentId,
                )
            }
                .getOrNull()
                ?: return false
        val child =
            runCatching {
                resolve(
                    documentId,
                )
            }
                .getOrNull()
                ?: return false

        if (
            parent.packageId !=
            child.packageId
        ) {
            return false
        }

        // v0.5.138: compare the normalized paths inside the volume (no `..` can be in
        // them) instead of canonical paths — each canonicalisation is several FUSE calls.
        val parentPath =
            parent.relativePath
        val childPath =
            child.relativePath

        return parentPath.isBlank() ||
            childPath ==
            parentPath ||
            childPath.startsWith(
                "$parentPath/",
            )
    }

    private fun MatrixCursor.include(
        documentId: String,
        name: String,
        attributes: VolumeFileIndex.Attributes,
    ) {
        val values =
            arrayOfNulls<Any>(
                columnNames.size,
            )

        columnNames.forEachIndexed {
            index,
            column ->
            values[index] =
                when (
                    column
                ) {
                    DocumentsContract.Document
                        .COLUMN_DOCUMENT_ID ->
                        documentId

                    DocumentsContract.Document
                        .COLUMN_DISPLAY_NAME ->
                        name

                    DocumentsContract.Document
                        .COLUMN_MIME_TYPE ->
                        mimeTypeFor(
                            name,
                            attributes.isDirectory,
                        )

                    DocumentsContract.Document
                        .COLUMN_SIZE ->
                        if (
                            attributes.isDirectory
                        ) {
                            null
                        } else {
                            attributes.size
                        }

                    DocumentsContract.Document
                        .COLUMN_LAST_MODIFIED ->
                        attributes.lastModified

                    DocumentsContract.Document
                        .COLUMN_FLAGS ->
                        0

                    else ->
                        null
                }
        }

        addRow(
            values,
        )
    }

    private fun resolve(
        documentId: String,
    ): ResolvedDocument {
        require(
            documentId.startsWith(
                DOCUMENT_PREFIX,
            ),
        ) {
            "Unknown Renault package document id."
        }

        val tail =
            documentId.removePrefix(
                DOCUMENT_PREFIX,
            )

        val separator =
            tail.indexOf(
                '/',
            )

        val packageId =
            if (
                separator >=
                0
            ) {
                tail.substring(
                    0,
                    separator,
                )
            } else {
                tail
            }

        val relativePath =
            if (
                separator >=
                0
            ) {
                tail.substring(
                    separator +
                        1,
                )
            } else {
                ""
            }

        validatePackageId(
            packageId,
        )

        val appContext =
            requireNotNull(
                context,
            )

        val normalized =
            SafDatasetResolver
                .normalize(
                    relativePath,
                )
                ?: throw FileNotFoundException(
                    "Invalid package path.",
                )

        // Archives plan v2, stage 1: a volume with a checked archive is read from it.
        VolumeArchiveAccess.active(appContext, packageId)?.let { archive ->
            val attributes =
                archive.attributes(normalized)
                    ?: throw FileNotFoundException("Package document not found: " + normalized)
            return ResolvedDocument(
                packageId = packageId,
                relativePath = normalized,
                file = null,
                attributes = attributes,
                archive = archive,
            )
        }

        // v0.5.138 (log 110, M30s): the volume folder is found once and remembered
        // ([VolumeFileIndex]); the document is checked with one file-system call.
        // Before: phone + card lookups and three canonical paths on every request.
        // `normalized` has no `..` and installed volumes contain no symbolic links
        // ([VolumeFileIndex.attributes] refuses them), so the path stays inside root.
        repeat(2) { attempt ->
            val root =
                VolumeFileIndex.root(packageId) {
                    packageDirectory(
                        appContext,
                        packageId,
                    ).takeIf { it.isDirectory }
                }

            if (root == null) {
                // TODO №1 (2026-10-15): a volume put on the memory card while the card
                // is out — say so plainly; it is not broken and nothing is deleted.
                if (VolumeStorage.state(appContext, packageId) == VolumeState.CARD_MISSING) {
                    throw FileNotFoundException(VolumeStoragePolicy.CARD_MISSING_MESSAGE)
                }
                throw FileNotFoundException(
                    "Renault package is not installed: " +
                        packageId,
                )
            }

            val file =
                VolumeFileIndex.fileIn(
                    root,
                    normalized,
                )

            val attributes =
                VolumeFileIndex.attributes(
                    file,
                )

            if (attributes != null) {
                return ResolvedDocument(
                    packageId =
                        packageId,
                    relativePath =
                        normalized,
                    file =
                        file,
                    attributes =
                        attributes,
                )
            }

            // The remembered folder may be gone (card taken out, volume moved): look
            // for the volume once more before saying the document is missing.
            if (attempt == 0 && !root.isDirectory) {
                VolumeFileIndex.forget(packageId)
            } else {
                throw FileNotFoundException(
                    "Package document not found: " +
                        normalized,
                )
            }
        }

        throw FileNotFoundException(
            "Package document not found: " +
                normalized,
        )
    }

    private data class ResolvedDocument(
        val packageId: String,
        val relativePath: String,
        /** The unpacked file; null when the volume is read from its archive. */
        val file: File?,
        val attributes: VolumeFileIndex.Attributes,
        val archive: VolumeArchive? = null,
    ) {
        val name: String
            get() = file?.name ?: relativePath.substringAfterLast('/').ifEmpty { packageId }
    }

    companion object {
        private const val DOCUMENT_PREFIX =
            "pkg:"
        private const val PACKAGE_DIRECTORY =
            "rdpkg"

        val authority: String =
            BuildConfig.APPLICATION_ID +
                ".datasets"

        private val DEFAULT_ROOT_PROJECTION =
            arrayOf(
                DocumentsContract.Root
                    .COLUMN_ROOT_ID,
                DocumentsContract.Root
                    .COLUMN_DOCUMENT_ID,
                DocumentsContract.Root
                    .COLUMN_TITLE,
                DocumentsContract.Root
                    .COLUMN_FLAGS,
                DocumentsContract.Root
                    .COLUMN_MIME_TYPES,
            )

        private val DEFAULT_DOCUMENT_PROJECTION =
            arrayOf(
                DocumentsContract.Document
                    .COLUMN_DOCUMENT_ID,
                DocumentsContract.Document
                    .COLUMN_DISPLAY_NAME,
                DocumentsContract.Document
                    .COLUMN_MIME_TYPE,
                DocumentsContract.Document
                    .COLUMN_SIZE,
                DocumentsContract.Document
                    .COLUMN_LAST_MODIFIED,
                DocumentsContract.Document
                    .COLUMN_FLAGS,
            )

        fun treeUriFor(
            packageId: String,
        ): Uri {
            validatePackageId(
                packageId,
            )

            return DocumentsContract
                .buildTreeDocumentUri(
                    authority,
                    documentId(
                        packageId =
                            packageId,
                        relativePath =
                            "",
                    ),
                )
        }

        fun packageIdFromTreeUri(
            uri: Uri,
        ): String? {
            if (
                uri.authority !=
                authority
            ) {
                return null
            }

            val treeDocumentId =
                runCatching {
                    DocumentsContract
                        .getTreeDocumentId(
                            uri,
                        )
                }
                    .getOrNull()
                    ?: return null

            if (
                !treeDocumentId.startsWith(
                    DOCUMENT_PREFIX,
                )
            ) {
                return null
            }

            val packageId =
                treeDocumentId
                    .removePrefix(
                        DOCUMENT_PREFIX,
                    )
                    .substringBefore(
                        '/',
                    )
                    .trim()

            return runCatching {
                validatePackageId(
                    packageId,
                )
                packageId
            }
                .getOrNull()
        }

        /**
         * TODO №1 (2026-10-15): the volume may lie in the phone or on the memory card
         * ([VolumeStorage]). Returns where it is now; when it is nowhere, the phone
         * path (which does not exist) — callers check `isDirectory` as before.
         */
        fun packageDirectory(
            context: android.content.Context,
            packageId: String,
        ): File {
            validatePackageId(
                packageId,
            )

            return VolumeStorage.locate(context, packageId)
                ?: File(
                    VolumeStorage.phoneRoot(context),
                    packageId,
                )
        }

        /** The phone folder of volumes (before TODO №1 the only one). */
        fun packagesDirectory(
            context: android.content.Context,
        ): File = VolumeStorage.phoneRoot(context)

        /** Same type as before v0.5.138, from the name only — no file-system call. */
        fun mimeTypeFor(
            name: String,
            isDirectory: Boolean,
        ): String {
            if (isDirectory) {
                return DocumentsContract
                    .Document
                    .MIME_TYPE_DIR
            }

            val extension =
                name.substringAfterLast('.', "")
                    .lowercase(
                        Locale.ROOT,
                    )

            return MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(
                    extension,
                )
                ?: when (
                    extension
                ) {
                    "htm",
                    "html" ->
                        "text/html"

                    "json" ->
                        "application/json"

                    "pdf" ->
                        "application/pdf"

                    else ->
                        "application/octet-stream"
                }
        }

        fun documentId(
            packageId: String,
            relativePath: String,
        ): String =
            DOCUMENT_PREFIX +
                packageId +
                if (
                    relativePath.isBlank()
                ) {
                    ""
                } else {
                    "/" +
                        relativePath
                }

        private fun validatePackageId(
            packageId: String,
        ) {
            require(
                packageId.matches(
                    Regex(
                        "[A-Za-z0-9._-]{1,160}",
                    ),
                ),
            ) {
                "Invalid Renault package id."
            }
        }
    }
}
