package com.forseti.drafts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.forseti.billing.findActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Prints a generated PDF or a bundled asset using the Android print framework.
 * Lets the user choose "Save as PDF" or any installed printer.
 */
object DraftPrinting {

    fun print(context: Context, file: File, jobName: String) {
        runCatching {
            val host = context.findActivity() ?: context
            val printManager = host.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: error("Print service unavailable")
            printManager.print(
                jobName,
                FilePrintAdapter(file, jobName),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                    .build()
            )
        }.onFailure { Log.e("DraftPrinting", "print failed: ${it.message}", it) }
    }

    fun share(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * [share] wrapped in a chooser intent. Always gives the user the share-sheet
     * (avoids silently launching whatever single PDF handler happens to be installed)
     * and adds NEW_TASK so callers can launch from non-Activity contexts.
     */
    fun shareChooser(context: Context, file: File, jobName: String): Intent =
        Intent.createChooser(share(context, file), "Share $jobName")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Returns null if the asset path is missing (bad install / fetch script not run). */
    fun copyAssetToCache(context: Context, assetPath: String): File? = runCatching {
        val name = assetPath.substringAfterLast('/')
        val out = File(context.cacheDir, "asset_$name")
        if (out.exists() && out.length() > 0) return@runCatching out
        context.assets.open(assetPath).use { input ->
            FileOutputStream(out).use { input.copyTo(it) }
        }
        out
    }.getOrNull()
}

private class FilePrintAdapter(
    private val file: File,
    private val jobName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            FileInputStream(file).use { input ->
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (t: Throwable) {
            callback?.onWriteFailed(t.message)
        }
    }
}
