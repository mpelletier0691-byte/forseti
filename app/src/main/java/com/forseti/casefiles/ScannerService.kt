package com.forseti.casefiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.forseti.data.entities.CaseEntity
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Converts a captured page sequence into a single PDF inside the case workspace
 * scanner folder. Writes atomically (temp file → rename) so partial files don't
 * appear if the user backs out mid-write.
 */
@ViewModelScoped
class ScannerService @Inject constructor(
    private val folders: CaseFolderService
) {
    /**
     * Saves [pages] (in capture order) as a Letter-size PDF under the scanner
     * folder for the [case]. Returns the final file or null if the case has no
     * usable workspace.
     */
    fun savePdf(case: CaseEntity, pages: List<Bitmap>, label: String?): File? {
        if (pages.isEmpty()) return null
        val target = folders.classifyForCase(case, label)
            ?: folders.scannerFolder(case)
            ?: return null
        val name = filename(label)
        val tmp = File(target, "$name.partial")
        val final = File(target, "$name.pdf")

        val document = PdfDocument()
        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            pages.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, index + 1).create()
                val page = document.startPage(pageInfo)
                drawCenteredScaled(page.canvas, bitmap, paint)
                document.finishPage(page)
            }
            FileOutputStream(tmp).use { document.writeTo(it) }
            if (!tmp.renameTo(final)) {
                tmp.copyTo(final, overwrite = true)
                tmp.delete()
            }
            return final
        } finally {
            document.close()
        }
    }

    private fun drawCenteredScaled(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
        val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val pageAspect = PAGE_W.toFloat() / PAGE_H.toFloat()
        val targetW: Int
        val targetH: Int
        if (srcAspect >= pageAspect) {
            targetW = PAGE_W
            targetH = (PAGE_W / srcAspect).toInt()
        } else {
            targetH = PAGE_H
            targetW = (PAGE_H * srcAspect).toInt()
        }
        val left = (PAGE_W - targetW) / 2f
        val top = (PAGE_H - targetH) / 2f
        val matrix = Matrix().apply {
            postScale(targetW.toFloat() / bitmap.width, targetH.toFloat() / bitmap.height)
            postTranslate(left, top)
        }
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun filename(label: String?): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val slug = label?.let { CaseFolderService.sanitize(it).take(40) }?.takeIf { it.isNotBlank() }
        return if (slug == null) "scan_$stamp" else "scan_${stamp}_$slug"
    }

    companion object {
        private const val PAGE_W = 612
        private const val PAGE_H = 792
    }
}
