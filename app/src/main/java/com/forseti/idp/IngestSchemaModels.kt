package com.forseti.idp

import kotlinx.serialization.Serializable

@Serializable
data class IngestSchema(
    val version: Int = 1,
    val confidenceHigh: Float = 0.85f,
    val confidenceMedium: Float = 0.70f,
    val renameTemplate: String = "{date}_{docType}_{caseId}_v1",
    val schemas: List<DocumentSchema> = emptyList(),
    val regex: RegexPatterns = RegexPatterns()
)

@Serializable
data class DocumentSchema(
    val id: String,
    val documentType: String,
    val folder: String,
    val keywords: List<String> = emptyList(),
    val anchors: List<String> = emptyList(),
    val weight: Float = 1f
)

@Serializable
data class RegexPatterns(
    val caseNumber: String = "",
    val dateIso: String = "",
    val dateUs: String = ""
)

data class AlternatePrediction(
    val documentType: String,
    val folder: String,
    val confidence: Float
)

data class ClassificationResult(
    val documentType: String,
    val folder: String,
    val confidence: Float,
    val matchedKeyword: String?,
    val schemaId: String?,
    val caseNumber: String?,
    val documentDate: String?,
    val suggestedFilename: String,
    val verbatimPreview: String,
    val autoRoute: Boolean,
    val alternates: List<AlternatePrediction> = emptyList()
)

data class LayoutTextSpan(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class LayoutOcrResult(
    val verbatimText: String,
    val spans: List<LayoutTextSpan>,
    val pageWidth: Int,
    val pageHeight: Int
)

@Serializable
data class AlternatePredictionMeta(
    val documentType: String,
    val folder: String,
    val confidence: Float
)

@Serializable
data class IngestMeta(
    val documentType: String?,
    val confidence: Float,
    val suggestedFolder: String,
    val matchedKeyword: String?,
    val caseNumber: String?,
    val documentDate: String?,
    val suggestedFilename: String,
    val routing: String,
    val verbatimPreview: String,
    val alternates: List<AlternatePredictionMeta> = emptyList(),
    val confidenceTier: String? = null,
    /** Ingest pipeline audit — additive, optional for backwards compatibility. */
    val pagesSampled: Int? = null,
    val tokensExtracted: Int? = null,
    val slidingWindowIterations: Int? = null,
    val earlyExitReason: String? = null,
    val classificationConfidence: Float? = null,
    val autoFileDecision: Boolean? = null
)
