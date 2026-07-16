package com.forseti.idp

import android.content.Context
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.forseti.tts.PageOcr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Multi-page OCR sampling for ingest classification (additive pipeline layer).
 * Does not alter [PageOcr] recognition internals — only orchestrates which pages to read.
 */
object IngestOcrSampler {

    data class SampledPdfText(
        val text: String,
        val pageCount: Int,
        val sampledPageIndices: List<Int>
    ) {
        val pagesSampled: Int get() = sampledPageIndices.size
    }

    /** Page indices for ingest: 1, 2, last, and middle (when document has more than 6 pages). */
    fun pageIndicesForSampling(pageCount: Int): List<Int> {
        if (pageCount <= 0) return emptyList()
        val indices = linkedSetOf(0, 1, pageCount - 1)
        if (pageCount > 6) {
            indices += pageCount / 2
        }
        return indices.filter { it in 0 until pageCount }.sorted()
    }

    suspend fun extractSampledTextFromPdfUri(
        context: Context,
        uri: Uri,
        renderMutex: Mutex,
        targetWidthPx: Int = 1200
    ): SampledPdfText = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return@withContext SampledPdfText("", 0, emptyList())
        pfd.use { fd ->
            runCatching {
                PdfRenderer(fd).use { renderer ->
                    val pageCount = renderer.pageCount
                    if (pageCount == 0) return@runCatching SampledPdfText("", 0, emptyList())
                    val indices = pageIndicesForSampling(pageCount)
                    val parts = mutableListOf<String>()
                    for (idx in indices) {
                        val pageText = runCatching {
                            PageOcr.extractText(renderer, idx, renderMutex, targetWidthPx)
                        }.getOrElse { "" }
                        if (pageText.isNotBlank()) {
                            parts += pageText
                        }
                    }
                    SampledPdfText(
                        text = parts.joinToString("\n\n"),
                        pageCount = pageCount,
                        sampledPageIndices = indices
                    )
                }
            }.getOrDefault(SampledPdfText("", 0, emptyList()))
        }
    }
}
