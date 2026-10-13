package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.concurrent.ConcurrentHashMap

/**
 * #261 (v0.5.125): a human label for a folder picked through Android's folder picker.
 *
 * [SafDisplayPath.tree] reads the path out of the address itself — that works for the
 * phone storage («Documents/…»), but a Google Drive address is an opaque id, so the
 * export window showed only «Папку вибрано». Here the folder's real name is asked
 * from the app that owns it (Drive), plus that app's name and, when Android allows
 * it, the account. The parent folders of a picked cloud folder are not visible to us
 * (only the picked folder is shared), so the label is «Диск · account › Folder».
 *
 * [resolve] talks to another app: call it off the main thread. [cached] is instant.
 */
object SafFolderLabel {
    private const val LOCAL_STORAGE = "com.android.externalstorage.documents"
    private val cache = ConcurrentHashMap<String, String>()

    fun cached(uriText: String?): String? = uriText?.let { cache[it] }

    fun resolve(context: Context, uriText: String?): String? {
        if (uriText.isNullOrBlank()) return null
        cache[uriText]?.let { return it }
        val local = SafDisplayPath.tree(uriText)
        val uri = runCatching { Uri.parse(uriText) }.getOrNull() ?: return local
        val authority = uri.authority
        if (authority == null || (authority == LOCAL_STORAGE && local != null)) {
            local?.let { cache[uriText] = it }
            return local
        }
        val documentId = runCatching { SafDatasetResolver.rootDocumentIdOf(uri) }.getOrNull()
        val name = documentId?.let { id ->
            runCatching {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, id)
                context.contentResolver.query(
                    documentUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null,
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull()
        }
        val provider = runCatching {
            val pm = context.packageManager
            pm.resolveContentProvider(authority, 0)?.loadLabel(pm)?.toString()
        }.getOrNull()
        val account = documentId?.let { id -> runCatching { accountOf(context, authority, id) }.getOrNull() }
        val label = format(provider, account, name) ?: local
        label?.let { cache[uriText] = it }
        return label
    }

    /**
     * The roots list of another app is usually closed to us (MANAGE_DOCUMENTS); when it
     * is open, the root's summary is the account (Drive: the e-mail). Never guessed.
     */
    private fun accountOf(context: Context, authority: String, documentId: String): String? {
        val rows = mutableListOf<Pair<String, String?>>()
        context.contentResolver.query(
            DocumentsContract.buildRootsUri(authority),
            arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID, DocumentsContract.Root.COLUMN_SUMMARY),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) rows += cursor.getString(0).orEmpty() to cursor.getString(1)
        }
        return accountFromRoots(rows, documentId)
    }

    /** Pure: one root → its summary; several → the root sharing the «acc=…» part. */
    fun accountFromRoots(rows: List<Pair<String, String?>>, documentId: String): String? {
        val withSummary = rows.filter { !it.second.isNullOrBlank() }
        if (withSummary.isEmpty()) return null
        if (rows.size == 1) return withSummary.first().second
        val accountPart = documentId.split(';').firstOrNull { it.startsWith("acc=") } ?: return null
        val matches = withSummary.filter { (rootId, _) -> rootId.split(';').contains(accountPart) }
        return matches.singleOrNull()?.second
    }

    /** Pure: «Диск · user@gmail.com › Renault Docs Projects». */
    fun format(provider: String?, account: String?, folderName: String?): String? {
        val name = folderName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val where = listOfNotNull(
            provider?.trim()?.takeIf { it.isNotEmpty() },
            account?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ")
        return if (where.isEmpty()) name else "$where › $name"
    }
}
