package com.forseti.casefiles

import com.forseti.data.entities.CaseEntity
import com.forseti.idp.DocumentIngestPipeline
import com.forseti.idp.IngestMetaStore
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner capture → sandwich PDF + IDP classification routing.
 */
@Singleton
class ScannerService @Inject constructor(
    private val folders: CaseFolderService,
    private val pipeline: DocumentIngestPipeline
) {
    suspend fun savePdf(case: CaseEntity, pages: List<Bitmap>, label: String?): File? =
        withContext(Dispatchers.IO) {
            if (pages.isEmpty()) return@withContext null
            val hint = label ?: "scan"
            val analyzed = pages.map { pipeline.analyzeBitmap(it, "$hint.jpg") }
            val primary = analyzed.first()
            val target = pipeline.resolveTargetFolder(case, primary)
                ?: folders.scannerFolder(case)
                ?: return@withContext null

            val baseName = pipeline.importName(primary, filename(hint))
            val final = File(target, if (baseName.endsWith(".pdf", true)) baseName else "$baseName.pdf")
            val tmp = File(target, "${final.name}.partial")

            val pagePairs = pages.zip(analyzed).mapNotNull { (bmp, out) ->
                out.layout?.let { bmp to it }
            }
            if (pagePairs.isEmpty()) return@withContext null

            pipeline.writeSandwichPdf(tmp, pagePairs)
            if (!tmp.renameTo(final)) {
                tmp.copyTo(final, overwrite = true)
                tmp.delete()
            }
            IngestMetaStore.save(final, primary.meta)
            final
        }

    private fun filename(label: String?): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val slug = label?.let { CaseFolderService.sanitize(it).take(40) }?.takeIf { it.isNotBlank() }
        return if (slug == null) "scan_$stamp" else "scan_${stamp}_$slug"
    }
}
