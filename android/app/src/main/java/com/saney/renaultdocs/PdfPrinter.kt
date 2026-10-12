package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * v0.5.115 (owner request 2026-10-12): «Друк» opens the Android print window,
 * where the owner picks a printer or «Зберегти як PDF» (the PDF printer).
 *
 * The PDF goes to the print system as it is — no re-rendering, no loss of
 * quality. Nothing is written next to the original files.
 */
object PdfPrinter {
    /** [write] fills the print job with ready PDF bytes; runs off the main thread. */
    fun print(
        activity: Activity,
        fileName: String,
        write: (OutputStream) -> Unit,
    ) {
        val manager =
            activity.getSystemService(
                Context.PRINT_SERVICE,
            ) as? PrintManager

        if (manager == null) {
            Toast
                .makeText(
                    activity,
                    "Друк недоступний на цьому телефоні.",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        val name =
            safeFileName(fileName)

        manager.print(
            name.removeSuffix(".pdf"),
            StreamPdfAdapter(
                name = name,
                write = write,
            ),
            PrintAttributes.Builder().build(),
        )
    }

    /** v0.5.116: window.print() of an original page — the page as shown. */
    fun printPage(
        activity: Activity,
        webView: WebView,
    ) {
        val manager =
            activity.getSystemService(
                Context.PRINT_SERVICE,
            ) as? PrintManager

        if (manager == null) {
            Toast
                .makeText(
                    activity,
                    "Друк недоступний на цьому телефоні.",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        val name =
            safeFileName(
                webView.title
                    ?.takeIf { it.isNotBlank() }
                    ?: "renault-page",
            ).removeSuffix(".pdf")

        manager.print(
            name,
            webView.createPrintDocumentAdapter(name),
            PrintAttributes.Builder().build(),
        )
    }

    fun safeFileName(
        raw: String,
    ): String {
        val base =
            raw
                .substringAfterLast('/')
                .replace(Regex("""[\\/:*?"<>|]"""), "_")
                .trim()
                .ifBlank { "renault-document" }
        return if (
            base.endsWith(".pdf", ignoreCase = true)
        ) {
            base
        } else {
            "$base.pdf"
        }
    }

    private class StreamPdfAdapter(
        private val name: String,
        private val write: (OutputStream) -> Unit,
    ) : PrintDocumentAdapter() {
        private val main =
            Handler(Looper.getMainLooper())

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }
            callback.onLayoutFinished(
                PrintDocumentInfo
                    .Builder(name)
                    .setContentType(
                        PrintDocumentInfo.CONTENT_TYPE_DOCUMENT,
                    )
                    .setPageCount(
                        PrintDocumentInfo.PAGE_COUNT_UNKNOWN,
                    )
                    .build(),
                oldAttributes != newAttributes,
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback,
        ) {
            Thread {
                val result =
                    runCatching {
                        FileOutputStream(
                            destination.fileDescriptor,
                        ).use { output ->
                            write(output)
                        }
                    }
                main.post {
                    when {
                        cancellationSignal.isCanceled ->
                            callback.onWriteCancelled()
                        result.isSuccess ->
                            callback.onWriteFinished(
                                arrayOf(PageRange.ALL_PAGES),
                            )
                        else ->
                            callback.onWriteFailed(
                                result.exceptionOrNull()?.message
                                    ?: "Не вдалося підготувати PDF до друку.",
                            )
                    }
                }
            }.start()
        }
    }
}
