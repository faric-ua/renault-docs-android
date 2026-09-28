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
                file =
                    resolved.file,
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
            !parent.file.isDirectory
        ) {
            throw FileNotFoundException(
                "Document is not a directory: " +
                    parentDocumentId,
            )
        }

        return MatrixCursor(
            projection
                ?: DEFAULT_DOCUMENT_PROJECTION,
        ).apply {
            parent.file
                .listFiles()
                .orEmpty()
                .sortedWith(
                    compareBy<File> {
                        !it.isDirectory
                    }.thenBy {
                        it.name.lowercase(
                            Locale.ROOT,
                        )
                    },
                )
                .forEach {
                    child ->
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
                        file =
                            child,
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
            !resolved.file.isFile
        ) {
            throw FileNotFoundException(
                "Document is not a file: " +
                    documentId,
            )
        }

        return ParcelFileDescriptor.open(
            resolved.file,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    override fun getDocumentType(
        documentId: String,
    ): String =
        mimeType(
            resolve(
                documentId,
            ).file,
        )

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

        val parentPath =
            parent.file.canonicalPath
        val childPath =
            child.file.canonicalPath

        return childPath ==
            parentPath ||
            childPath.startsWith(
                parentPath +
                    File.separator,
            )
    }

    private fun MatrixCursor.include(
        documentId: String,
        file: File,
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
                        file.name

                    DocumentsContract.Document
                        .COLUMN_MIME_TYPE ->
                        mimeType(
                            file,
                        )

                    DocumentsContract.Document
                        .COLUMN_SIZE ->
                        if (
                            file.isFile
                        ) {
                            file.length()
                        } else {
                            null
                        }

                    DocumentsContract.Document
                        .COLUMN_LAST_MODIFIED ->
                        file.lastModified()

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

        val root =
            packageDirectory(
                appContext,
                packageId,
            )
                .canonicalFile

        if (
            !root.isDirectory
        ) {
            throw FileNotFoundException(
                "Renault package is not installed: " +
                    packageId,
            )
        }

        val normalized =
            SafDatasetResolver
                .normalize(
                    relativePath,
                )
                ?: throw FileNotFoundException(
                    "Invalid package path.",
                )

        val file =
            if (
                normalized.isBlank()
            ) {
                root
            } else {
                File(
                    root,
                    normalized,
                ).canonicalFile
            }

        val rootPath =
            root.canonicalPath
        val filePath =
            file.canonicalPath

        if (
            filePath !=
            rootPath &&
            !filePath.startsWith(
                rootPath +
                    File.separator,
            )
        ) {
            throw FileNotFoundException(
                "Package path escapes its root.",
            )
        }

        if (
            !file.exists()
        ) {
            throw FileNotFoundException(
                "Package document not found: " +
                    normalized,
            )
        }

        return ResolvedDocument(
            packageId =
                packageId,
            relativePath =
                normalized,
            file =
                file,
        )
    }

    private fun mimeType(
        file: File,
    ): String {
        if (
            file.isDirectory
        ) {
            return DocumentsContract
                .Document
                .MIME_TYPE_DIR
        }

        val extension =
            file.extension
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

    private data class ResolvedDocument(
        val packageId: String,
        val relativePath: String,
        val file: File,
    )

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

        fun packageDirectory(
            context: android.content.Context,
            packageId: String,
        ): File {
            validatePackageId(
                packageId,
            )

            return File(
                File(
                    context.noBackupFilesDir,
                    PACKAGE_DIRECTORY,
                ),
                packageId,
            )
        }

        fun packagesDirectory(
            context: android.content.Context,
        ): File =
            File(
                context.noBackupFilesDir,
                PACKAGE_DIRECTORY,
            )

        private fun documentId(
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
