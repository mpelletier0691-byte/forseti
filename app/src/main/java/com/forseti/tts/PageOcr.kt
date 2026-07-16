package com.forseti.tts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders a single PDF page and runs ML Kit text recognition. Rasterized PDFs
 * in [PdfViewer] don't expose a text layer, so OCR is the only on-device way to
 * read or copy FRCP text.
 *
 * Uses [PdfRenderer.Page.RENDER_MODE_FOR_PRINT] for sharper glyphs than
 * [RENDER_MODE_FOR_DISPLAY], and retries at several widths if the first pass
 * returns empty (common on dense legal PDFs when resolution is wrong).
 */
object PageOcr {

    private const val MAX_BITMAP_EDGE = 4096

    suspend fun extractText(
        renderer: PdfRenderer,
        pageIndex: Int,
        renderMutex: Mutex,
        targetWidthPx: Int = 2200
    ): String = extractTextInternal(renderer, pageIndex, renderMutex, targetWidthPx)

    /**
     * Same OCR pipeline as [extractText] but keeps line breaks so the result is
     * suitable for a selectable [androidx.compose.ui.text.input.TextFieldValue]
     * in the PDF "page text" dialog (long-press on a page).
     */
    suspend fun extractDisplayText(
        renderer: PdfRenderer,
        pageIndex: Int,
        renderMutex: Mutex,
        targetWidthPx: Int = 2200
    ): String = extractDisplayTextInternal(renderer, pageIndex, renderMutex, targetWidthPx)

    /**
     * OCR page 1 of a PDF from a content [Uri] — used during case-file ingest
     * when the filename is ambiguous (e.g. court e-filing "COVER PAGE" titles).
     */
    suspend fun extractTextFromPdfUri(
        context: Context,
        uri: Uri,
        renderMutex: Mutex = Mutex(),
        targetWidthPx: Int = 1600
    ): String = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext ""
        pfd.use { fd ->
            runCatching {
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount == 0) return@runCatching ""
                    extractText(renderer, 0, renderMutex, targetWidthPx)
                }
            }.getOrDefault("")
        }
    }

    private suspend fun extractTextInternal(
        renderer: PdfRenderer,
        pageIndex: Int,
        renderMutex: Mutex,
        primaryWidth: Int
    ): String = withContext(Dispatchers.IO) {
        if (pageIndex !in 0 until renderer.pageCount) return@withContext ""
        val widths = listOf(
            primaryWidth,
            3200,
            3000,
            (primaryWidth * 3 / 4).coerceAtLeast(1100),
            (primaryWidth / 2).coerceAtLeast(900)
        ).distinct()
        for (w in widths) {
            val text = runCatching {
                val bmp = renderMutex.withLock {
                    renderPageForOcr(renderer, pageIndex, w.coerceIn(900, 3600))
                } ?: return@runCatching ""
                try {
                    recognize(bmp)
                } finally {
                    runCatching { bmp.recycle() }
                }
            }.getOrElse { "" }
            if (text.isNotBlank()) return@withContext text
        }
        ""
    }

    private suspend fun extractDisplayTextInternal(
        renderer: PdfRenderer,
        pageIndex: Int,
        renderMutex: Mutex,
        primaryWidth: Int
    ): String = withContext(Dispatchers.IO) {
        if (pageIndex !in 0 until renderer.pageCount) return@withContext ""
        val widths = listOf(
            primaryWidth,
            3200,
            3000,
            (primaryWidth * 3 / 4).coerceAtLeast(1100),
            (primaryWidth / 2).coerceAtLeast(900)
        ).distinct()
        for (w in widths) {
            val text = runCatching {
                val bmp = renderMutex.withLock {
                    renderPageForOcr(renderer, pageIndex, w.coerceIn(900, 3600))
                } ?: return@runCatching ""
                try {
                    formatDisplay(recognizeRaw(bmp))
                } finally {
                    runCatching { bmp.recycle() }
                }
            }.getOrElse { "" }
            if (text.isNotBlank()) return@withContext text
        }
        ""
    }

    private fun formatDisplay(result: com.google.mlkit.vision.text.Text): String {
        val hierarchical = result.textBlocks.joinToString("\n\n") { block ->
            block.lines.joinToString("\n") { it.text }
        }.trim()
        if (hierarchical.isNotBlank()) return hierarchical
        // Some PDFs expose aggregate text but sparse block/line hierarchy — still useful for selection.
        return result.text.trim()
    }

    private suspend fun recognizeRaw(bitmap: Bitmap): com.google.mlkit.vision.text.Text =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    runCatching { recognizer.close() }
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener { e ->
                    runCatching { recognizer.close() }
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    private fun renderPageForOcr(
        renderer: PdfRenderer,
        pageIndex: Int,
        targetWidthPx: Int
    ): Bitmap? = try {
        val page = renderer.openPage(pageIndex)
        try {
            val ratio = page.height.toFloat() / page.width.toFloat()
            var w = targetWidthPx
            var h = (w * ratio).toInt().coerceAtLeast(1)
            // Avoid OOM / ML Kit instability on extreme aspect ratios.
            val maxEdge = maxOf(w, h)
            if (maxEdge > MAX_BITMAP_EDGE) {
                val scale = MAX_BITMAP_EDGE.toFloat() / maxEdge.toFloat()
                w = (w * scale).toInt().coerceAtLeast(600)
                h = (w * ratio).toInt().coerceAtLeast(1)
            }
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            bm
        } finally {
            runCatching { page.close() }
        }
    } catch (_: Throwable) {
        null
    }

    private suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                runCatching { recognizer.close() }
                val raw = result.text.trim()
                val normalized = raw.normalized()
                val out = if (normalized.isNotBlank()) normalized
                else raw.replace(Regex("\\s+"), " ").trim()
                if (cont.isActive) cont.resume(out)
            }
            .addOnFailureListener { e ->
                runCatching { recognizer.close() }
                if (cont.isActive) cont.resumeWithException(e)
            }
    }

    private fun String.normalized(): String =
        replace(Regex("-\\s*\\n\\s*"), "")
            .replace(Regex("\\n+"), " ")
            .replace(Regex(" +"), " ")
            .trim()
}
