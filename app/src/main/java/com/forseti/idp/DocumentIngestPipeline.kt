package com.forseti.idp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.forseti.casefiles.CaseFolderService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline IDP pipeline — Phase A (preprocess + sandwich), B (classify), C (route by confidence tier).
 *
 * Tier policy (thresholds from ingest_schema.json):
 * - 85%+ → auto-file
 * - 70–84% → inbox + suggested folder
 * - &lt;70% → inbox only
 */
@Singleton
class DocumentIngestPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocr: OcrLayoutEngine,
    private val classifier: IngestClassifier,
    private val lateBinding: LateBindingClassifier,
    private val folders: CaseFolderService
) {
    data class PipelineOutcome(
        val classification: ClassificationResult,
        val layout: LayoutOcrResult?,
        val meta: IngestMeta,
        val tier: ConfidenceRouter.Tier,
        val ocrTrace: LateBindingClassifier.Trace? = null
    )

    private val renderMutex = Mutex()

    suspend fun analyzeBitmap(
        bitmap: Bitmap,
        originalFilename: String,
        hintFilename: String = originalFilename
    ): PipelineOutcome = withContext(Dispatchers.Default) {
        val cleaned = ScanPreprocessor.preprocess(bitmap)
        val layout = ocr.recognize(cleaned)
        val trace = lateBinding.classifyWithLateBinding(
            fullText = layout.verbatimText,
            originalFilename = originalFilename,
            hintFilename = hintFilename,
            pagesSampled = 1,
            totalPageCount = 1
        )
        finalize(trace.classification, layout, trace)
    }

    fun analyzeText(
        originalFilename: String,
        text: String,
        hintFilename: String = originalFilename
    ): PipelineOutcome {
        val trace = lateBinding.classifyWithLateBinding(
            fullText = text,
            originalFilename = originalFilename,
            hintFilename = hintFilename,
            pagesSampled = 1,
            totalPageCount = 1
        )
        return finalize(trace.classification, null, trace)
    }

    suspend fun analyzePdfUri(uri: Uri, originalFilename: String, pathHint: String = originalFilename): PipelineOutcome =
        withContext(Dispatchers.IO) {
            val hint = "$pathHint $originalFilename"
            val sampled = runCatching {
                IngestOcrSampler.extractSampledTextFromPdfUri(context, uri, renderMutex)
            }.getOrElse { IngestOcrSampler.SampledPdfText("", 0, emptyList()) }
            val trace = runCatching {
                lateBinding.classifyWithLateBinding(
                    fullText = sampled.text,
                    originalFilename = originalFilename,
                    hintFilename = "$hint ${sampled.text}",
                    pagesSampled = sampled.pagesSampled,
                    totalPageCount = sampled.pageCount.coerceAtLeast(1)
                )
            }.getOrElse {
                val fallback = classifier.classify("", originalFilename, hint)
                LateBindingClassifier.Trace(
                    classification = fallback,
                    pagesSampled = sampled.pagesSampled,
                    tokensExtracted = 0,
                    slidingWindowIterations = 0,
                    earlyExitReason = "ocr_failed",
                    classificationConfidence = fallback.confidence,
                    autoFileDecision = false
                )
            }
            finalize(trace.classification, null, trace)
        }

    fun outcomeFromClassification(classification: ClassificationResult): PipelineOutcome =
        finalize(classification, null, null)

    private fun finalize(
        classification: ClassificationResult,
        layout: LayoutOcrResult?,
        trace: LateBindingClassifier.Trace?
    ): PipelineOutcome {
        val tier = ConfidenceRouter.tier(
            confidence = classification.confidence,
            hasSchemaMatch = classification.schemaId != null,
            high = classifier.highThreshold(),
            medium = classifier.mediumThreshold()
        )
        val routed = classification.copy(autoRoute = tier == ConfidenceRouter.Tier.AUTO_FILE)
        val meta = routed.toMeta(tier, trace)
        return PipelineOutcome(routed, layout, meta, tier, trace)
    }

    suspend fun writeSandwichPdf(
        output: File,
        pages: List<Pair<Bitmap, LayoutOcrResult>>
    ) = withContext(Dispatchers.IO) {
        val inputs = pages.map { (bmp, layout) ->
            SandwichPdfWriter.PageInput(ScanPreprocessor.preprocess(bmp), layout)
        }
        SandwichPdfWriter.write(output, inputs)
    }

    fun classifierThreshold(): Float = classifier.highThreshold()

    fun classifierMediumThreshold(): Float = classifier.mediumThreshold()

    fun resolveTargetFolder(case: com.forseti.data.entities.CaseEntity, outcome: PipelineOutcome): File? {
        val root = folders.ensureCaseRoot(case) ?: return null
        val rel = when (outcome.tier) {
            ConfidenceRouter.Tier.AUTO_FILE -> outcome.classification.folder
            ConfidenceRouter.Tier.INBOX_SUGGESTED,
            ConfidenceRouter.Tier.INBOX_ONLY -> "99_Inbox"
        }
        return File(root, rel).apply { mkdirs() }
    }

    fun importName(outcome: PipelineOutcome, fallback: String): String =
        if (outcome.tier == ConfidenceRouter.Tier.AUTO_FILE) {
            CaseFolderService.sanitize(outcome.classification.suggestedFilename)
        } else {
            fallback
        }

    private fun ClassificationResult.toMeta(
        tier: ConfidenceRouter.Tier,
        trace: LateBindingClassifier.Trace?
    ): IngestMeta {
        val showSuggestion = tier == ConfidenceRouter.Tier.INBOX_SUGGESTED
        return IngestMeta(
            documentType = documentType.takeIf { it != "Unassigned" },
            confidence = confidence,
            suggestedFolder = if (showSuggestion) folder else "99_Inbox",
            matchedKeyword = matchedKeyword,
            caseNumber = caseNumber,
            documentDate = documentDate,
            suggestedFilename = suggestedFilename,
            routing = ConfidenceRouter.routingKey(tier),
            verbatimPreview = verbatimPreview,
            alternates = if (showSuggestion) {
                alternates.map { AlternatePredictionMeta(it.documentType, it.folder, it.confidence) }
            } else {
                emptyList()
            },
            confidenceTier = ConfidenceRouter.routingKey(tier),
            pagesSampled = trace?.pagesSampled,
            tokensExtracted = trace?.tokensExtracted,
            slidingWindowIterations = trace?.slidingWindowIterations,
            earlyExitReason = trace?.earlyExitReason,
            classificationConfidence = trace?.classificationConfidence,
            autoFileDecision = trace?.let { tier == ConfidenceRouter.Tier.AUTO_FILE }
        )
    }

    suspend fun decodeBitmap(uri: Uri, maxEdge: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return@runCatching null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var size = 1
        while (width / size > maxEdge || height / size > maxEdge) size *= 2
        return size
    }

    suspend fun layoutFromPdfUri(uri: Uri): LayoutOcrResult? = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
        pfd.use { fd ->
            runCatching {
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount == 0) return@runCatching null
                    val page = renderer.openPage(0)
                    try {
                        val ratio = page.height.toFloat() / page.width
                        val w = 1600
                        val h = (w * ratio).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val cleaned = ScanPreprocessor.preprocess(bmp)
                        ocr.recognize(cleaned)
                    } finally {
                        page.close()
                    }
                }
            }.getOrNull()
        }
    }
}
