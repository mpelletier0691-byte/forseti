package com.forseti.billing

import android.content.Context
import android.provider.Settings
import com.forseti.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local persistence for the 3-day free trial.
 *
 * Storage strategy:
 *   • Saved in `forseti_prefs.xml`, which is included in `backup_rules.xml`
 *     and `data_extraction_rules.xml`. That means a reinstall on the **same
 *     Google account / device** will restore the trial start via Auto Backup,
 *     so the trial cannot be reset by a simple uninstall + reinstall.
 *   • A SHA-256 fingerprint of the Android-ID is stored alongside the
 *     timestamp. On read we re-derive the fingerprint and refuse trusts the
 *     timestamp only when both match — moving the prefs file to a different
 *     device (e.g. via root) is detected and the trial is reset there.
 *   • The actual entitlement decision is made by [EntitlementManager], which
 *     also consults Google Play Billing. If Play Billing reports the user
 *     already purchased `forseti_unlock` (under any device on the same
 *     Google account), the unlock is honored regardless of trial state.
 */
@Singleton
class TrialPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("forseti_prefs", Context.MODE_PRIVATE)

    /** Epoch-ms when the trial first started, or `null` if it hasn't yet. */
    val trialStartMs: Long?
        get() {
            val raw = prefs.getLong(KEY_START_MS, 0L).takeIf { it > 0L } ?: return null
            val storedFp = prefs.getString(KEY_FINGERPRINT, null)
            return if (storedFp == deviceFingerprint()) raw else null
        }

    /**
     * Idempotently records the trial start. Subsequent calls are no-ops, so
     * this is safe to invoke from every cold start.
     */
    fun ensureTrialStarted(): Long {
        trialStartMs?.let { return it }
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_START_MS, now)
            .putString(KEY_FINGERPRINT, deviceFingerprint())
            .apply()
        return now
    }

    /**
     * Mark the welcome dialog as shown. Used so we only present it on the
     * very first launch, not on every cold start during the trial.
     */
    var welcomeShown: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_SHOWN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WELCOME_SHOWN, value).apply()
        }

    /**
     * Mark the soft "ending soon" reminder as shown so it doesn't pop every
     * time the user reopens the app during the final 24h.
     */
    var endingSoonShown: Boolean
        get() = prefs.getBoolean(KEY_ENDING_SOON_SHOWN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENDING_SOON_SHOWN, value).apply()
        }

    /**
     * Best-effort cache of the Play Billing purchase verdict so we don't have
     * to wait for the billing client to bind before deciding whether to lock
     * the UI on a cold start. The truth source is still [BillingService].
     */
    var cachedPurchased: Boolean
        get() = prefs.getBoolean(KEY_PURCHASED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PURCHASED, value).apply()
        }

    private fun deviceFingerprint(): String {
        val id = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: ""
        }.getOrDefault("")
        return sha256("forseti|$id")
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Production closed/Play builds: 3-day trial. Debug APK: 30 days for dev QA. */
        val TRIAL_DURATION_MS: Long =
            if (BuildConfig.DEBUG) 30L * 24L * 60L * 60L * 1000L
            else 3L * 24L * 60L * 60L * 1000L

        const val SOFT_REMINDER_THRESHOLD_MS: Long = 24L * 60L * 60L * 1000L

        private const val KEY_START_MS = "trial_start_ms"
        private const val KEY_FINGERPRINT = "trial_device_fp"
        private const val KEY_WELCOME_SHOWN = "trial_welcome_shown"
        private const val KEY_ENDING_SOON_SHOWN = "trial_ending_soon_shown"
        private const val KEY_PURCHASED = "billing_purchased_cache"
    }
}
