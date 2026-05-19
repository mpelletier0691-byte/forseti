package com.forseti.ocr

import com.forseti.drafts.DraftPrefill

/**
 * Editable model for the OCR review screen. The user sees a list of recognized
 * blocks on the left and a list of named fields on the right, then drags blocks
 * onto fields. We persist the result as a [DraftPrefill] to feed [DraftGenerator].
 */
data class FieldAssignment(
    val key: FieldKey,
    val text: String?
)

enum class FieldKey(val label: String) {
    Court("Court"),
    Plaintiff("Plaintiff(s)"),
    Defendant("Defendant(s)"),
    CaseNumber("Case Number"),
    SignerName("Your name"),
    SignerAddress("Your address"),
    SignerPhone("Your phone"),
    SignerEmail("Your email")
}

object OcrFieldMapper {
    fun emptyAssignments(): List<FieldAssignment> = FieldKey.values().map { FieldAssignment(it, null) }

    fun toPrefill(assignments: List<FieldAssignment>): DraftPrefill {
        val map = assignments.associate { it.key to it.text }
        return DraftPrefill(
            court = map[FieldKey.Court],
            plaintiff = map[FieldKey.Plaintiff],
            defendant = map[FieldKey.Defendant],
            caseNumber = map[FieldKey.CaseNumber],
            signerName = map[FieldKey.SignerName],
            signerAddress = map[FieldKey.SignerAddress],
            signerPhone = map[FieldKey.SignerPhone],
            signerEmail = map[FieldKey.SignerEmail]
        )
    }
}
