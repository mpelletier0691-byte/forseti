package com.forseti.casefiles

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

/**
 * Best-effort text extraction from a Microsoft Word `.docx` file, with zero
 * third-party dependencies.
 *
 * `.docx` is a ZIP archive whose `word/document.xml` entry holds the body in
 * OOXML. Text runs live in `<w:t>` elements, paragraphs in `<w:p>`, soft
 * line breaks in `<w:br/>`, and tabs in `<w:tab/>`. That's enough structure
 * to give the reader something legible — formatting (bold, italics, lists,
 * tables, headers, footers, images, footnotes) is intentionally dropped to
 * keep the parser tight.
 *
 * If the parse fails for any reason (corrupt zip, non-OOXML body, etc.) we
 * return `null` so callers can fall back to the system "Open in another app"
 * intent for that file.
 */
object DocxReader {
    private const val TAG = "DocxReader"
    private const val DOCUMENT_XML = "word/document.xml"

    /**
     * Runs the parse on [Dispatchers.IO]. Suitable to use directly from a
     * Compose `LaunchedEffect`.
     */
    suspend fun extractText(file: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(DOCUMENT_XML)
                    ?: return@use null
                zip.getInputStream(entry).use { input ->
                    val handler = DocxTextHandler()
                    // SAXParserFactory is reused per-call rather than a
                    // singleton because some platform implementations are
                    // not thread-safe across coroutines.
                    val parser = SAXParserFactory.newInstance().apply {
                        // The XML in document.xml has no external DTD; turn
                        // off DTD loading defensively against XXE.
                        runCatching {
                            setFeature(
                                "http://apache.org/xml/features/disallow-doctype-decl",
                                true
                            )
                        }
                    }.newSAXParser()
                    parser.parse(input, handler)
                    handler.result()
                }
            }
        }.onFailure {
            Log.w(TAG, "extractText failed for ${file.name}: ${it.message}")
        }.getOrNull()
    }

    private class DocxTextHandler : DefaultHandler() {
        private val out = StringBuilder()
        private var inText = false

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            val name = (qName ?: localName ?: return).substringAfter(':')
            when (name) {
                "t" -> inText = true
                "br" -> appendNewlineIfNeeded()
                "tab" -> out.append('\t')
            }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (inText && ch != null && length > 0) {
                out.append(ch, start, length)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val name = (qName ?: localName ?: return).substringAfter(':')
            when (name) {
                "t" -> inText = false
                "p" -> {
                    // End of paragraph: ensure a blank line between
                    // paragraphs so the result reads as prose rather than
                    // one giant run-on string.
                    appendNewlineIfNeeded()
                    out.append('\n')
                }
            }
        }

        private fun appendNewlineIfNeeded() {
            if (out.isEmpty() || out.last() != '\n') out.append('\n')
        }

        fun result(): String = out.toString().trimEnd()
    }
}
