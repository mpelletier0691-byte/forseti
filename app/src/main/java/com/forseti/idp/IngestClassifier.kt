package com.forseti.idp

import android.content.Context
import com.forseti.casefiles.CaseFolderService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase B — offline schema classification from verbatim OCR text (no smoothing).
 */
@Singleton
class IngestClassifier @Inject constructor(
    @ApplicationContext context: Context,
    private val folders: CaseFolderService
) {
    private val schema: IngestSchema = runCatching {
        val raw = context.assets.open("ingest/ingest_schema.json")
            .bufferedReader()
            .use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString<IngestSchema>(raw)
    }.getOrElse { IngestSchema() }

    private val caseRegex by lazy { schema.regex.caseNumber.toRegex() }
    private val isoRegex by lazy { schema.regex.dateIso.toRegex() }
    private val usRegex by lazy { schema.regex.dateUs.toRegex() }

    fun classify(
        verbatimText: String,
        originalFilename: String,
        hintFilename: String = originalFilename
    ): ClassificationResult {
        val sample = verbatimText.take(1000)
        val hintLower = hintFilename.lowercase(Locale.US)
        val haystack = buildString {
            append(hintFilename)
            append(' ')
            append(sample)
        }.lowercase(Locale.US)

        var best: DocumentSchema? = null
        var bestScore = 0f
        var bestMatch: String? = null
        var matchSource = MatchSource.NONE
        val hits = mutableListOf<Triple<DocumentSchema, Float, String>>()

        for (docSchema in schema.schemas) {
            for (keyword in docSchema.keywords) {
                val k = keyword.lowercase(Locale.US)
                if (haystack.contains(k)) {
                    val inPath = pathContainsNeedle(hintLower, k)
                    val score = docSchema.weight * (0.9f + k.length * 0.01f)
                    hits += Triple(docSchema, score, keyword)
                    if (score > bestScore) {
                        bestScore = score
                        best = docSchema
                        bestMatch = keyword
                        matchSource = if (inPath) MatchSource.PATH_SCHEMA else MatchSource.SCHEMA_TEXT
                    }
                }
            }
            for (anchor in docSchema.anchors) {
                val a = anchor.lowercase(Locale.US)
                if (haystack.contains(a)) {
                    val score = docSchema.weight * 0.85f
                    hits += Triple(docSchema, score, anchor)
                    if (score > bestScore) {
                        bestScore = score
                        best = docSchema
                        bestMatch = anchor
                        matchSource = MatchSource.SCHEMA_TEXT
                    }
                }
            }
        }

        var docType = best?.documentType ?: "Unassigned"
        var folder = best?.folder ?: "99_Inbox"
        var schemaId: String? = best?.id
        var confidence = normalizeConfidence(bestScore, best != null, sample.isNotBlank(), matchSource)

        folders.findKeywordRoute(hintFilename)?.let { route ->
            val needle = route.needle.lowercase(Locale.US)
            val inPath = pathContainsNeedle(hintLower, needle)
            val inFilename = originalFilename.lowercase(Locale.US).contains(needle)
            val routeConfidence = when {
                inPath -> 0.92f
                inFilename -> 0.86f
                haystack.contains(needle) -> 0.78f
                else -> confidence
            }
            if (routeConfidence >= confidence || schemaId == null) {
                folder = route.folder
                docType = documentTypeForFolder(route.folder) ?: docType
                confidence = routeConfidence
                bestMatch = route.needle
                schemaId = schemaId ?: "keyword-route"
                matchSource = when {
                    inPath -> MatchSource.PATH_KEYWORD
                    inFilename -> MatchSource.FILENAME_KEYWORD
                    else -> MatchSource.KEYWORD_TEXT
                }
            }
        }

        val caseNumber = extractCaseNumber(haystack)
        val docDate = extractDate(haystack) ?: extractDateFromFilename(originalFilename)
        val alternates = buildAlternates(hits, schemaId?.takeIf { it != "keyword-route" }, sample.isNotBlank())
        val suggestedFilename = buildRename(
            docType = docType,
            caseNumber = caseNumber,
            documentDate = docDate,
            originalFilename = originalFilename
        )

        return ClassificationResult(
            documentType = docType,
            folder = folder,
            confidence = confidence,
            matchedKeyword = bestMatch,
            schemaId = schemaId,
            caseNumber = caseNumber,
            documentDate = docDate,
            suggestedFilename = suggestedFilename,
            verbatimPreview = sample.take(500),
            autoRoute = false,
            alternates = alternates
        )
    }

    fun highThreshold(): Float = schema.confidenceHigh

    fun mediumThreshold(): Float = schema.confidenceMedium

    private enum class MatchSource {
        NONE,
        PATH_SCHEMA,
        PATH_KEYWORD,
        FILENAME_KEYWORD,
        KEYWORD_TEXT,
        SCHEMA_TEXT
    }

    private fun pathContainsNeedle(pathHint: String, needle: String): Boolean {
        val pathPortion = pathHint.substringBeforeLast('/', pathHint)
        return pathPortion.contains(needle)
    }

    private fun documentTypeForFolder(folder: String): String? = when {
        folder.contains("Interrogatories") -> "Interrogatories"
        folder.contains("Complaint") -> "Complaint"
        folder.contains("Answer") -> "Answer"
        folder.contains("Orders") -> "CourtOrder"
        folder.contains("Photos") -> "PhotoEvidence"
        folder.contains("Video") -> "VideoEvidence"
        folder.contains("Screenshots") -> "Screenshot"
        folder.contains("Motions") -> "Motion"
        folder.contains("Correspondence") -> "Correspondence"
        folder.contains("Discovery") -> "Discovery"
        folder.contains("Depositions") -> "Deposition"
        folder.contains("Proof_of_Service") -> "ProofOfService"
        folder.contains("Summons") -> "Summons"
        folder.contains("Exhibits") -> "Exhibit"
        folder.contains("Hearings") -> "HearingNotice"
        else -> null
    }

    private fun buildAlternates(
        hits: List<Triple<DocumentSchema, Float, String>>,
        bestId: String?,
        hasText: Boolean
    ): List<AlternatePrediction> {
        if (hits.isEmpty()) return emptyList()
        return hits
            .groupBy { it.first.id }
            .mapNotNull { (id, group) ->
                if (id == bestId) return@mapNotNull null
                val top = group.maxByOrNull { it.second } ?: return@mapNotNull null
                val conf = normalizeConfidence(top.second, matched = true, hasText = hasText, MatchSource.SCHEMA_TEXT)
                AlternatePrediction(
                    documentType = top.first.documentType,
                    folder = top.first.folder,
                    confidence = conf
                )
            }
            .sortedByDescending { it.confidence }
            .take(2)
    }

    private fun normalizeConfidence(
        raw: Float,
        matched: Boolean,
        hasText: Boolean,
        source: MatchSource
    ): Float {
        if (!matched) return if (hasText) 0.35f else 0.15f
        return when (source) {
            MatchSource.PATH_KEYWORD, MatchSource.PATH_SCHEMA ->
                (0.88f + raw.coerceAtMost(1.3f) * 0.08f).coerceIn(0.85f, 0.98f)
            MatchSource.FILENAME_KEYWORD ->
                (0.82f + raw.coerceAtMost(1.2f) * 0.06f).coerceIn(0.78f, 0.90f)
            MatchSource.KEYWORD_TEXT ->
                0.76f.coerceIn(0.70f, 0.84f)
            MatchSource.SCHEMA_TEXT ->
                (0.52f + raw * 0.32f).coerceIn(0.68f, 0.96f)
            MatchSource.NONE ->
                if (hasText) 0.35f else 0.15f
        }
    }

    private fun extractCaseNumber(text: String): String? =
        caseRegex.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.length in 3..24 }

    private fun extractDate(text: String): String? {
        isoRegex.find(text)?.let { return it.value }
        usRegex.find(text)?.let { m ->
            val mm = m.groupValues[1].padStart(2, '0')
            val dd = m.groupValues[2].padStart(2, '0')
            val yyyy = m.groupValues[3]
            return "$yyyy-$mm-$dd"
        }
        return null
    }

    private fun extractDateFromFilename(name: String): String? {
        val m = Regex("(20\\d{2})-(\\d{2})-(\\d{2})").find(name) ?: return null
        return m.value
    }

    private fun buildRename(
        docType: String,
        caseNumber: String?,
        documentDate: String?,
        originalFilename: String
    ): String {
        val date = documentDate ?: LocalDate.now().toString()
        val caseId = (caseNumber ?: "UNK").replace(Regex("[^A-Za-z0-9\\-]"), "")
        val ext = originalFilename.substringAfterLast('.', "pdf")
        return (schema.renameTemplate
            .replace("{date}", date.replace("-", ""))
            .replace("{docType}", docType)
            .replace("{caseId}", caseId.take(24))) + ".$ext"
    }
}
