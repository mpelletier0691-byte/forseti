package com.forseti.pdf

/**
 * One entry in the FRCP table of contents.
 *
 * @param anchor stable identifier (e.g. "rule.12.b.6") used by bookmarks/notes.
 * @param page 1-based page number in the bundled PDF (matches what the user
 *   sees in the page-counter chip). PdfViewModel.jumpTo subtracts 1 before
 *   feeding it to the lazy list.
 * @param children nested rule subsections.
 */
data class TocEntry(
    val anchor: String,
    val title: String,
    val page: Int,
    val depth: Int,
    val children: List<TocEntry> = emptyList()
)

/*
 * NOTE: We intentionally do NOT parse the PDF's bookmark stream.
 *
 * android.graphics.pdf.PdfRenderer (the framework class we now use to render
 * pages) doesn't expose document outlines, and the curated [FrcpOutline] is
 * what we'd fall back to anyway — it ships hand-tuned rule numbers and pages
 * for the bundled FRCP. If we ever want bookmark-stream parsing back, that's
 * a job for a separate parser (e.g. iText, PdfBox-Android), not the renderer.
 */
