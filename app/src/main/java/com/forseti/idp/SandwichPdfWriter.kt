package com.forseti.idp

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Phase A — sandwich PDF: visible cleaned bitmap + invisible verbatim text at ML Kit boxes.
 */
object SandwichPdfWriter {

    data class PageInput(val bitmap: Bitmap, val layout: LayoutOcrResult)

    private const val PAGE_W = 612f
    private const val PAGE_H = 792f

    fun write(output: File, pages: List<PageInput>) {
        require(pages.isNotEmpty())
        val chunks = mutableListOf<Chunk>()

        chunks += Chunk.Dict("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        val fontId = 1

        val pageIds = mutableListOf<Int>()
        pages.forEach { page ->
            val jpeg = bitmapToJpeg(page.bitmap)
            val placement = computePlacement(page.bitmap.width, page.bitmap.height)
            val imageName = "Im${chunks.size + 1}"

            val contentBytes = buildContentStream(page, placement, imageName)
            val contentId = chunks.size + 1
            chunks += Chunk.Stream(contentBytes, "")

            val imageId = chunks.size + 1
            val imageDict =
                "<< /Type /XObject /Subtype /Image /Width ${jpeg.second.first} /Height ${jpeg.second.second} " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.first.size} >>"
            chunks += Chunk.Stream(jpeg.first, imageDict)

            val pageId = chunks.size + 1
            val pageDict = buildString {
                append("<< /Type /Page /Parent PAGES_PLACEHOLDER /MediaBox [0 0 $PAGE_W $PAGE_H] ")
                append("/Contents $contentId 0 R ")
                append("/Resources << /XObject << /$imageName $imageId 0 R >> /Font << /F1 $fontId 0 R >> >> >>")
            }
            chunks += Chunk.Dict(pageDict)
            pageIds += pageId
        }

        val pagesId = chunks.size + 1
        val kids = pageIds.joinToString(" ") { "$it 0 R" }
        chunks += Chunk.Dict("<< /Type /Pages /Kids [ $kids ] /Count ${pageIds.size} >>")

        val catalogId = chunks.size + 1
        chunks += Chunk.Dict("<< /Type /Catalog /Pages $pagesId 0 R >>")

        // Patch page Parent refs
        pageIds.forEach { pid ->
            val idx = pid - 1
            val old = (chunks[idx] as Chunk.Dict).text
            chunks[idx] = Chunk.Dict(old.replace("PAGES_PLACEHOLDER", "$pagesId 0 R"))
        }

        writeFile(output, chunks, catalogId)
    }

    private sealed class Chunk {
        data class Dict(val text: String) : Chunk()
        data class Stream(val bytes: ByteArray, val dict: String) : Chunk()
    }

    private fun buildContentStream(page: PageInput, placement: Placement, imageName: String): ByteArray {
        val sb = StringBuilder()
        sb.append("q\n")
        sb.append("${placement.width} 0 0 ${placement.height} ${placement.left} ${placement.bottom} cm\n")
        sb.append("/$imageName Do\nQ\n")
        page.layout.spans.forEach { span ->
            val x = placement.left + span.left * placement.scaleX
            val y = placement.bottom + (page.layout.pageHeight - span.bottom) * placement.scaleY
            val text = escapePdfText(span.text)
            if (text.isBlank()) return@forEach
            sb.append("BT 3 Tr /F1 9 Tf 1 0 0 1 $x $y Tm ($text) Tj ET\n")
        }
        return sb.toString().toByteArray(Charsets.US_ASCII)
    }

    private data class Placement(
        val left: Float,
        val bottom: Float,
        val width: Float,
        val height: Float,
        val scaleX: Float,
        val scaleY: Float
    )

    private fun computePlacement(bitmapW: Int, bitmapH: Int): Placement {
        val srcAspect = bitmapW.toFloat() / bitmapH
        val pageAspect = PAGE_W / PAGE_H
        val (targetW, targetH) = if (srcAspect >= pageAspect) {
            PAGE_W to PAGE_W / srcAspect
        } else {
            PAGE_H * srcAspect to PAGE_H
        }
        val left = (PAGE_W - targetW) / 2f
        val bottom = (PAGE_H - targetH) / 2f
        return Placement(left, bottom, targetW, targetH, targetW / bitmapW, targetH / bitmapH)
    }

    private fun bitmapToJpeg(bitmap: Bitmap): Pair<ByteArray, Pair<Int, Int>> {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return out.toByteArray() to (bitmap.width to bitmap.height)
    }

    private fun escapePdfText(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\n", " ")
            .take(180)

    private fun writeFile(output: File, chunks: List<Chunk>, catalogId: Int) {
        val tmp = File(output.parentFile, "${output.name}.partial")
        FileOutputStream(tmp).use { fos ->
            val offsets = mutableListOf<Long>()
            fos.write("%PDF-1.4\n".toByteArray(Charsets.US_ASCII))
            chunks.forEachIndexed { index, chunk ->
                offsets += fos.channel.position()
                fos.write("${index + 1} 0 obj\n".toByteArray(Charsets.US_ASCII))
                when (chunk) {
                    is Chunk.Dict -> fos.write(chunk.text.toByteArray(Charsets.US_ASCII))
                    is Chunk.Stream -> {
                        val dict = chunk.dict.ifBlank { "<< /Length ${chunk.bytes.size} >>" }
                        fos.write("$dict\nstream\n".toByteArray(Charsets.US_ASCII))
                        fos.write(chunk.bytes)
                        fos.write("\nendstream".toByteArray(Charsets.US_ASCII))
                    }
                }
                fos.write("\nendobj\n".toByteArray(Charsets.US_ASCII))
            }
            val xref = fos.channel.position()
            fos.write("xref\n0 ${chunks.size + 1}\n".toByteArray(Charsets.US_ASCII))
            fos.write("0000000000 65535 f \n".toByteArray(Charsets.US_ASCII))
            offsets.forEach { fos.write(String.format("%010d 00000 n \n", it).toByteArray(Charsets.US_ASCII)) }
            fos.write(
                "trailer\n<< /Size ${chunks.size + 1} /Root $catalogId 0 R >>\nstartxref\n$xref\n%%EOF\n"
                    .toByteArray(Charsets.US_ASCII)
            )
        }
        if (!tmp.renameTo(output)) {
            tmp.copyTo(output, overwrite = true)
            tmp.delete()
        }
    }
}
