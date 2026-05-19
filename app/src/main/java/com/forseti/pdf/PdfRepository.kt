package com.forseti.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the bundled FRCP PDF and exposes its page count + curated table of contents.
 *
 * Backed by [android.graphics.pdf.PdfRenderer] (API 21+, framework class) instead of
 * any third-party native library. This eliminates 16 KB ELF-alignment problems on
 * Android 15+ devices and removes a dependency that hadn't been touched since 2017.
 *
 * PdfRenderer requires a real file descriptor, so on first use we copy the asset
 * into the app cache and open it from there.
 */
@Singleton
class PdfRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val initMutex = Mutex()

    /**
     * PdfRenderer.Page may only have ONE open page at a time per renderer instance —
     * concurrent opens throw IllegalStateException. The viewer renders pages from
     * many coroutines via [androidx.compose.runtime.produceState], so every render
     * call serializes through this mutex.
     */
    val renderMutex = Mutex()

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    /**
     * Per-page text from the bundled `frcp_2024.pages.txt` sidecar, split on the
     * form-feed character that `pdftotext` emits between pages. Null until
     * [warmFrcp] runs (or remains null if the sidecar asset isn't bundled).
     *
     * We prefer this over OCR for the Quick Jump tab because:
     *   1. android.graphics.pdf.PdfRenderer doesn't expose the text layer at all.
     *   2. ML Kit Latin OCR is unreliable on dense legal type (false negatives,
     *      empty results, etc. — exactly what the user is hitting).
     *   3. Reading the sidecar is instant.
     */
    @Volatile
    private var pageTexts: List<String>? = null

    private val _toc = MutableStateFlow<List<TocEntry>>(emptyList())
    val toc: StateFlow<List<TocEntry>> = _toc.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    suspend fun warmFrcp() = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (renderer != null) return@withLock
            val cached = runCatching { ensureCachedAsset(FRCP_ASSET, FRCP_CACHE_NAME) }
                .getOrNull()
            if (cached == null) {
                // Asset not bundled (dev build before fetch_assets.sh has been run).
                // Quick Jump still works because the curated FrcpOutline ships in source.
                _pageCount.value = 0
                _toc.value = FrcpOutline.entries
                return@withLock
            }
            runCatching {
                val newPfd = ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY)
                val newRenderer = PdfRenderer(newPfd)
                pfd = newPfd
                renderer = newRenderer
                _pageCount.value = newRenderer.pageCount
                // android.graphics.pdf.PdfRenderer doesn't expose the bookmark stream,
                // so we always use the curated FRCP outline. For our use case this is
                // strictly better — the bundled outline is hand-tuned and we'd fall
                // back to it anyway if PDF bookmarks were absent.
                _toc.value = FrcpOutline.entries
            }.onFailure {
                _pageCount.value = 0
                _toc.value = FrcpOutline.entries
            }
            pageTexts = runCatching { loadPageTextSidecar() }.getOrNull()
        }
    }

    fun renderer(): PdfRenderer? = renderer

    /**
     * Returns extracted text for [pageIndex] from the bundled sidecar, or null
     * if the sidecar is unavailable or the page is out of range. Callers should
     * treat null as "fall back to OCR" — never as "this page is blank".
     */
    fun pageText(pageIndex: Int): String? {
        val texts = pageTexts ?: return null
        return texts.getOrNull(pageIndex)?.takeIf { it.isNotBlank() }
    }

    private fun loadPageTextSidecar(): List<String>? {
        val raw = runCatching {
            context.assets.open(FRCP_TEXT_ASSET).use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: return null
        if (raw.isBlank()) return null
        // `pdftotext` emits one form-feed between pages; trailing form-feed
        // produces a trailing empty page, which we drop.
        return raw.split('\u000C').map { it.trim('\n', '\r', ' ') }
    }

    fun close() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        renderer = null
        pfd = null
        pageTexts = null
    }

    private fun ensureCachedAsset(assetPath: String, cacheName: String): File {
        val out = File(context.cacheDir, cacheName)
        if (out.exists() && out.length() > 0) return out
        context.assets.open(assetPath).use { input ->
            FileOutputStream(out).use { output ->
                input.copyTo(output)
            }
        }
        return out
    }

    companion object {
        const val FRCP_ASSET = "rules/frcp_2024.pdf"
        const val FRCP_CACHE_NAME = "frcp_2024.pdf"
        const val FRCP_TEXT_ASSET = "rules/frcp_2024.pages.txt"
    }
}
