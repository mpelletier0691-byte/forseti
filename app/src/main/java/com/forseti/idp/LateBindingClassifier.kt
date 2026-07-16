package com.forseti.idp

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

/**
 * Sliding-window + late-binding classification for ingest OCR text.
 * Classification is deferred until enough pages/tokens are available or
 * confidence reaches the auto-file threshold (85%+).
 *
 * Does not modify schema keywords, legal routing, or [ConfidenceRouter] thresholds.
 */
@Singleton
class LateBindingClassifier @Inject constructor(
    private val classifier: IngestClassifier
) {
    data class Trace(
        val classification: ClassificationResult,
        val pagesSampled: Int,
        val tokensExtracted: Int,
        val slidingWindowIterations: Int,
        val earlyExitReason: String?,
        val classificationConfidence: Float,
        val autoFileDecision: Boolean
    )

    companion object {
        private const val WINDOW_TOKENS = 600
        private const val MIN_TOKENS_BEFORE_CLASSIFY = 1500
        private const val MAX_TOKENS = 4000
        private const val STABILITY_DELTA = 0.02f
    }

    fun classifyWithLateBinding(
        fullText: String,
        originalFilename: String,
        hintFilename: String,
        pagesSampled: Int,
        totalPageCount: Int
    ): Trace {
        val tokens = tokenize(fullText)
        if (tokens.isEmpty()) {
            val empty = classifier.classify("", originalFilename, hintFilename)
            return trace(
                classification = empty,
                pagesSampled = pagesSampled,
                tokensExtracted = 0,
                iterations = 0,
                earlyExitReason = "no_ocr_text",
                highThreshold = classifier.highThreshold()
            )
        }

        val high = classifier.highThreshold()
        var iterations = 0
        var best: ClassificationResult? = null
        var prevConfidence: Float? = null
        var stableCount = 0
        var earlyExit: String? = null
        var tokensUsed = 0
        var offset = 0

        while (offset < tokens.size && tokensUsed < MAX_TOKENS) {
            val windowEnd = min(
                tokens.size,
                min(offset + WINDOW_TOKENS, MAX_TOKENS)
            )
            val windowText = tokens.subList(0, windowEnd).joinToString(" ")
            iterations++
            tokensUsed = windowEnd

            val result = classifier.classify(windowText, originalFilename, hintFilename)
            best = if (best == null || result.confidence > best.confidence) result else best

            val pagesGateMet = pagesSampled >= 2 || totalPageCount < 2
            val tokenGateMet = windowEnd >= MIN_TOKENS_BEFORE_CLASSIFY
            val gatesMet = pagesGateMet && (tokenGateMet || result.confidence >= high)

            if (!gatesMet) {
                if (windowEnd >= tokens.size || windowEnd >= MAX_TOKENS) break
                offset += WINDOW_TOKENS
                continue
            }

            if (result.confidence >= high) {
                earlyExit = "confidence_threshold"
                return trace(result, pagesSampled, windowEnd, iterations, earlyExit, high)
            }

            val prev = prevConfidence
            if (prev != null && abs(result.confidence - prev) < STABILITY_DELTA) {
                stableCount++
            } else {
                stableCount = 0
            }
            prevConfidence = result.confidence

            if (stableCount >= 1) {
                earlyExit = "confidence_stable"
                return trace(result, pagesSampled, windowEnd, iterations, earlyExit, high)
            }

            if (windowEnd >= tokens.size || windowEnd >= MAX_TOKENS) break
            offset += WINDOW_TOKENS
        }

        val finalResult = best ?: classifier.classify(
            tokens.take(MAX_TOKENS).joinToString(" "),
            originalFilename,
            hintFilename
        )
        val finalTokens = min(tokens.size, MAX_TOKENS)
        if (earlyExit == null) {
            earlyExit = when {
                finalTokens >= MAX_TOKENS -> "max_tokens"
                finalTokens >= tokens.size -> "exhausted_text"
                else -> "min_gates_satisfied"
            }
        }
        return trace(finalResult, pagesSampled, finalTokens, iterations, earlyExit, high)
    }

    private fun trace(
        classification: ClassificationResult,
        pagesSampled: Int,
        tokensExtracted: Int,
        iterations: Int,
        earlyExitReason: String?,
        highThreshold: Float
    ): Trace {
        val autoFile = classification.schemaId != null && classification.confidence >= highThreshold
        return Trace(
            classification = classification,
            pagesSampled = pagesSampled,
            tokensExtracted = tokensExtracted,
            slidingWindowIterations = iterations,
            earlyExitReason = earlyExitReason,
            classificationConfidence = classification.confidence,
            autoFileDecision = autoFile
        )
    }

    private fun tokenize(text: String): List<String> =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }
}
