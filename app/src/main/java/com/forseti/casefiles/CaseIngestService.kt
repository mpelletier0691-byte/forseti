package com.forseti.casefiles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.forseti.data.entities.CaseEntity
import android.content.Intent
import com.forseti.idp.DocumentIngestPipeline
import com.forseti.idp.IngestMeta
import com.forseti.idp.IngestMetaStore
import com.forseti.idp.LayoutOcrResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bulk ingestion with offline IDP pipeline (classify + confidence routing).
 */
@Singleton
class CaseIngestService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folders: CaseFolderService,
    private val pipeline: DocumentIngestPipeline,
    private val brokkrProgress: BrokkrForgeProgress
) {
    data class Report(
        val totalDiscovered: Int,
        val imported: Int,
        val skipped: Int,
        val failed: Int,
        val inboxSuggested: Int,
        val inboxOnly: Int,
        val autoRouted: Int,
        val firstFolder: File?,
        val auditFile: File?
    ) {
        fun summary(): String = buildString {
            append("Imported $imported file")
            if (imported != 1) append("s")
            if (autoRouted > 0) append(" \u00B7 $autoRouted auto-filed (85%+)")
            if (inboxSuggested > 0) append(" \u00B7 $inboxSuggested suggested in Inbox (70\u201384%)")
            if (inboxOnly > 0) append(" \u00B7 $inboxOnly need manual sort (<70%)")
            if (skipped > 0) append(" \u00B7 $skipped skipped")
            if (failed > 0) append(" \u00B7 $failed failed")
            if (auditFile != null) append(" \u00B7 see ${auditFile.name}")
        }

        /** True only when every discovered file was attempted (import, skip, or fail). */
        fun isFullyProcessed(): Boolean =
            totalDiscovered > 0 && (imported + skipped + failed) >= totalDiscovered
    }

    suspend fun ingestTree(case: CaseEntity, treeUri: Uri): Report = withContext(Dispatchers.IO) {
        brokkrProgress.markCollecting(case.id)
        persistReadPermission(treeUri)
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IngestAbortException("Cannot access the selected folder. Pick the folder again from Brokkr Forge Process.")
        val files = mutableListOf<DocEntry>()
        collect(tree, "", files)
        if (files.isEmpty()) {
            throw IngestAbortException("No files found in the selected folder.")
        }
        ingestDocs(case, files)
    }

    suspend fun ingestUris(case: CaseEntity, uris: List<Uri>): Report = withContext(Dispatchers.IO) {
        brokkrProgress.markCollecting(case.id)
        uris.forEach { persistReadPermission(it) }
        val docs = uris.mapNotNull { uri ->
            DocumentFile.fromSingleUri(context, uri)?.let { DocEntry(it, "") }
        }
        if (docs.isEmpty()) {
            throw IngestAbortException("Could not read the selected files.")
        }
        ingestDocs(case, docs)
    }

    private data class DocEntry(val file: DocumentFile, val relativePath: String)

    private fun collect(dir: DocumentFile, prefix: String, into: MutableList<DocEntry>) {
        val children = dir.listFiles() ?: throw IngestAbortException(
            "Could not list files in the folder. Re-open Brokkr Forge Process and pick the folder again."
        )
        children.forEach { child ->
            val name = child.name ?: return@forEach
            if (name.startsWith(".")) return@forEach
            when {
                child.isDirectory -> {
                    val next = if (prefix.isEmpty()) name else "$prefix/$name"
                    collect(child, next, into)
                }
                child.isFile -> into += DocEntry(child, prefix)
            }
        }
    }

    private fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private suspend fun ingestDocs(case: CaseEntity, docs: List<DocEntry>): Report {
        val total = docs.size
        brokkrProgress.markSorting(case.id, total)
        val root = folders.ensureCaseRoot(case)
            ?: throw IngestAbortException("Case workspace could not be created. Free device storage and try again.")
        var imported = 0
        var skipped = 0
        var failed = 0
        var inboxSuggested = 0
        var inboxOnly = 0
        var autoRouted = 0
        var firstFolder: File? = null
        val auditLines = mutableListOf<String>()
        val folderTally = mutableMapOf<String, Int>()
        val seenFingerprints = mutableSetOf<String>()
        var processed = 0

        auditLines += "Forseti IDP ingest audit — ${Instant.now()}"
        auditLines += "total_discovered=$total"
        auditLines += "confidenceHigh=${pipeline.classifierThreshold()} confidenceMedium=${pipeline.classifierMediumThreshold()}"
        auditLines += ""
        auditLines += "original | saved_as | folder | confidence | type | routing | pages | tokens | sw_iter | early_exit | auto_file | alternates"
        auditLines += "-------- | -------- | ------ | ---------- | ---- | ------- | ----- | ------ | ------- | ---------- | --------- | ----------"

        for (entry in docs) {
            processed++
            brokkrProgress.tick(case.id, processed, imported, skipped, failed)
            val doc = entry.file
            val pathHint = entry.relativePath
            val uri = doc.uri
            val name = displayName(uri) ?: doc.name
            if (name == null) {
                failed++
                auditLines += "(unknown) | ERROR | — | — | — | missing filename | —"
                continue
            }
            if (name.startsWith(".")) {
                skipped++
                continue
            }
            val size = doc.length()
            val fingerprint = "${CaseFolderService.normalizeImportBaseName(name)}|$size"
            if (fingerprint in seenFingerprints) {
                skipped++
                auditLines += "${name.take(40)} | DUPLICATE (batch) | — | — | — | skipped duplicate in import | —"
                continue
            }
            if (folders.findFileByNormalizedNameAndSize(root, name, size) != null) {
                skipped++
                auditLines += "${name.take(40)} | DUPLICATE (workspace) | — | — | — | already imported | —"
                continue
            }
            seenFingerprints += fingerprint
            val mime = doc.type ?: context.contentResolver.getType(uri)
            val ext = name.substringAfterLast('.', "")
            val isPdf = mime?.lowercase()?.contains("pdf") == true || ext.equals("pdf", ignoreCase = true)
            val isImage = mime?.startsWith("image/") == true ||
                ext.lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
            val isDocx = ext.equals("docx", ignoreCase = true) ||
                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

            val result = runCatching {
                when {
                    isImage -> ingestImage(case, uri, name, pathHint)
                    isPdf -> ingestPdf(case, uri, name, pathHint)
                    isDocx -> ingestDocx(case, uri, name, pathHint)
                    else -> ingestFallback(case, uri, name, mime, ext, pathHint)
                }
            }.getOrElse { err ->
                auditLines += "${name.take(40)} | ERROR | — | — | — | ${err.message?.take(60) ?: "failed"} | —"
                null
            }
            if (result == null) {
                failed++
                continue
            }

            imported++
            if (!isImage) IngestMetaStore.save(result.saved, result.outcome.meta)
            when (result.outcome.tier) {
                com.forseti.idp.ConfidenceRouter.Tier.AUTO_FILE -> autoRouted++
                com.forseti.idp.ConfidenceRouter.Tier.INBOX_SUGGESTED -> inboxSuggested++
                com.forseti.idp.ConfidenceRouter.Tier.INBOX_ONLY -> inboxOnly++
            }
            folderTally[result.outcome.meta.suggestedFolder] =
                folderTally.getOrDefault(result.outcome.meta.suggestedFolder, 0) + 1
            if (firstFolder == null) firstFolder = result.target

            val trace = result.outcome.ocrTrace
            val altText = result.outcome.meta.alternates.take(2).joinToString("; ") {
                "${it.documentType}@${it.folder.substringAfterLast('/')} ${"%.0f".format(it.confidence * 100)}%"
            }.ifBlank { "—" }
            auditLines += "${name.take(40)} | ${result.saved.name.take(40)} | ${result.target.relativeTo(root).path} | " +
                "${"%.0f".format(result.outcome.meta.confidence * 100)}% | ${result.outcome.meta.documentType ?: "—"} | " +
                "${com.forseti.idp.ConfidenceRouter.auditLabel(result.outcome.tier, result.outcome.meta.confidence)} | " +
                "${trace?.pagesSampled ?: "—"} | ${trace?.tokensExtracted ?: "—"} | ${trace?.slidingWindowIterations ?: "—"} | " +
                "${trace?.earlyExitReason ?: "—"} | ${trace?.autoFileDecision ?: result.outcome.tier == com.forseti.idp.ConfidenceRouter.Tier.AUTO_FILE} | $altText"
            folders.appendIndex(root, "ingest → ${result.saved.name}", result.target)
        }

        auditLines += ""
        auditLines += "=== Totals by folder ==="
        folderTally.toList().sortedByDescending { it.second }.forEach { (path, count) ->
            auditLines += "$count × $path"
        }

        val auditFile = runCatching {
            File(root, INGEST_AUDIT_FILE).apply {
                writeText(auditLines.joinToString("\n"))
            }
        }.getOrNull()
        val report = Report(
            totalDiscovered = total,
            imported = imported,
            skipped = skipped,
            failed = failed,
            inboxSuggested = inboxSuggested,
            inboxOnly = inboxOnly,
            autoRouted = autoRouted,
            firstFolder = firstFolder,
            auditFile = auditFile
        )
        if (processed < total) {
            throw IngestAbortException(
                "Sorting stopped early ($processed of $total files). Open the case and try Brokkr Forge again."
            )
        }
        brokkrProgress.markDone(case.id, report)
        return report
    }

    private data class IngestResult(
        val saved: File,
        val outcome: DocumentIngestPipeline.PipelineOutcome,
        val target: File
    )

    private fun pathHaystack(pathHint: String, name: String): String =
        if (pathHint.isBlank()) name else "$pathHint/$name"

    private suspend fun ingestImage(case: CaseEntity, uri: Uri, name: String, pathHint: String): IngestResult? {
        val bmp = pipeline.decodeBitmap(uri) ?: return null
        return try {
            val hint = pathHaystack(pathHint, name)
            val outcome = pipeline.analyzeBitmap(bmp, name, hint)
            val layout = outcome.layout ?: return null
            val target = pipeline.resolveTargetFolder(case, outcome) ?: return null
            val importName = pipeline.importName(outcome, pdfBaseName(name))
            val saved = writeSandwichImage(target, importName, bmp, layout, outcome.meta) ?: return null
            IngestResult(saved, outcome, target)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    private suspend fun ingestPdf(case: CaseEntity, uri: Uri, name: String, pathHint: String): IngestResult? {
        val hint = pathHaystack(pathHint, name)
        val outcome = pipeline.analyzePdfUri(uri, name, hint)
        val target = pipeline.resolveTargetFolder(case, outcome) ?: return null
        val importName = pipeline.importName(outcome, name)
        val saved = folders.importContent(uri, target, importName) ?: return null
        return IngestResult(saved, outcome, target)
    }

    private suspend fun ingestDocx(case: CaseEntity, uri: Uri, name: String, pathHint: String): IngestResult? {
        val tmp = File.createTempFile("ingest_", ".docx", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            val text = DocxReader.extractText(tmp).orEmpty()
            val hint = pathHaystack(pathHint, name)
            val outcome = pipeline.analyzeText(name, text, hint)
            val target = pipeline.resolveTargetFolder(case, outcome) ?: return null
            val importName = pipeline.importName(outcome, name)
            val saved = folders.importContent(uri, target, importName) ?: return null
            IngestResult(saved, outcome, target)
        } finally {
            tmp.delete()
        }
    }

    private suspend fun ingestFallback(
        case: CaseEntity,
        uri: Uri,
        name: String,
        mime: String?,
        ext: String,
        pathHint: String
    ): IngestResult? {
        val hint = pathHaystack(pathHint, name)
        val decision = folders.decideIngestRoute(hint, mime, ext)
        val rawConfidence = when (decision.method) {
            "keyword" -> 0.92f
            else -> 0.2f
        }
        val classification = com.forseti.idp.ClassificationResult(
            documentType = "Unassigned",
            folder = decision.relativePath,
            confidence = rawConfidence,
            matchedKeyword = decision.matchedKeyword,
            schemaId = if (decision.method == "keyword") "keyword-fallback" else null,
            caseNumber = null,
            documentDate = null,
            suggestedFilename = name,
            verbatimPreview = "",
            autoRoute = false,
            alternates = emptyList()
        )
        val outcome = pipeline.outcomeFromClassification(classification)
        val target = pipeline.resolveTargetFolder(case, outcome) ?: return null
        val importName = pipeline.importName(outcome, name)
        val saved = folders.importContent(uri, target, importName) ?: return null
        return IngestResult(saved, outcome, target)
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
    }.getOrNull()

    private suspend fun writeSandwichImage(
        targetFolder: File,
        baseName: String,
        bitmap: Bitmap,
        layout: LayoutOcrResult,
        meta: IngestMeta
    ): File? {
        val pdfName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
        val dest = uniqueFile(targetFolder, pdfName) ?: return null
        val tmp = File(targetFolder, "${dest.name}.partial")
        pipeline.writeSandwichPdf(tmp, listOf(bitmap to layout))
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        IngestMetaStore.save(dest, meta)
        return dest
    }

    private fun uniqueFile(folder: File, name: String): File? {
        var dest = File(folder, name)
        if (!dest.exists()) return dest
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var n = 1
        while (n <= 99) {
            dest = File(folder, if (ext.isNotBlank()) "${base}_$n.$ext" else "${base}_$n")
            if (!dest.exists()) return dest
            n++
        }
        return null
    }

    private fun pdfBaseName(original: String): String {
        val ext = original.substringAfterLast('.', "")
        return if (ext.lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "heif")) {
            original.substringBeforeLast('.').ifBlank { original }
        } else {
            original
        }
    }

    private fun empty(): Report = Report(0, 0, 0, 0, 0, 0, 0, null, null)

    companion object {
        const val INGEST_AUDIT_FILE = "INGEST_AUDIT_latest.txt"
    }
}
