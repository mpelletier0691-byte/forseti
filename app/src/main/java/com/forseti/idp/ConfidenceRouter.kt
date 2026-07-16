package com.forseti.idp

/**
 * Confidence-tier routing policy (thresholds from [IngestSchema]).
 * Legal / privileged folder rules live in schema keywords — this only maps
 * a numeric confidence score to auto-file vs inbox behavior.
 */
object ConfidenceRouter {

    enum class Tier {
        /** 85%–100%: auto-route to predicted folder */
        AUTO_FILE,
        /** 70%–84%: inbox + suggested folder + one-tap File Here */
        INBOX_SUGGESTED,
        /** &lt;70%: inbox only, no suggestion */
        INBOX_ONLY
    }

    fun tier(confidence: Float, hasSchemaMatch: Boolean, high: Float, medium: Float): Tier = when {
        !hasSchemaMatch || confidence < medium -> Tier.INBOX_ONLY
        confidence >= high -> Tier.AUTO_FILE
        else -> Tier.INBOX_SUGGESTED
    }

    fun routingKey(tier: Tier): String = when (tier) {
        Tier.AUTO_FILE -> "auto-filed"
        Tier.INBOX_SUGGESTED -> "inbox-suggested"
        Tier.INBOX_ONLY -> "inbox-only"
    }

    fun auditLabel(tier: Tier, confidence: Float): String = when (tier) {
        Tier.AUTO_FILE -> "AUTO-FILED (confidence: ${"%.2f".format(confidence)})"
        Tier.INBOX_SUGGESTED -> "INBOX + suggested (${"%.0f".format(confidence * 100)}%)"
        Tier.INBOX_ONLY -> "INBOX only (${"%.0f".format(confidence * 100)}%)"
    }
}
