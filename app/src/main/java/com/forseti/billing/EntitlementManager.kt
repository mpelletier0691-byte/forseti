package com.forseti.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the gating verdict that the UI consults: free trial active, ending
 * soon, expired-and-locked, or permanently unlocked. Combines:
 *
 *   • [TrialPrefs.trialStartMs] — the local timestamp (with device-fingerprint
 *     anti-tamper check, restored across reinstalls via Auto Backup).
 *   • [BillingService.isPurchased] — Play Billing purchase verdict.
 *
 * The truth is recomputed continuously and surfaced as [state]. A 1-minute
 * tick keeps the time-remaining display fresh without burning battery.
 */
@Singleton
class EntitlementManager @Inject constructor(
    private val trialPrefs: TrialPrefs,
    private val billing: BillingService
) {
    sealed interface Entitlement {
        /** No purchase yet, trial running, plenty of time. */
        data class Trial(val msRemaining: Long) : Entitlement
        /** No purchase yet, less than 24h trial left. */
        data class TrialEndingSoon(val msRemaining: Long) : Entitlement
        /** No purchase, trial expired. App should block. */
        data object Expired : Entitlement
        /** User bought the unlock. Permanent. */
        data object Purchased : Entitlement
        /** Initial loading state; allow access optimistically. */
        data object Loading : Entitlement
    }

    private val _state = MutableStateFlow<Entitlement>(initialOptimisticState())
    val state: StateFlow<Entitlement> = _state.asStateFlow()

    private val tick = MutableStateFlow(System.currentTimeMillis())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        trialPrefs.ensureTrialStarted()
        billing.start()
        wireFlows()
        startTicker()
    }

    /** True if the user can use the app right now. False = lock screen. */
    fun isUnlocked(): Boolean = when (val s = _state.value) {
        Entitlement.Purchased, Entitlement.Loading -> true
        is Entitlement.Trial, is Entitlement.TrialEndingSoon -> true
        Entitlement.Expired -> false
    }

    private fun initialOptimisticState(): Entitlement {
        if (trialPrefs.cachedPurchased) return Entitlement.Purchased
        return Entitlement.Loading
    }

    private fun wireFlows() {
        combine(billing.isPurchased, tick) { purchased, now -> compute(purchased, now) }
            .onEach { _state.value = it }
            .launchIn(scope)
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                tick.update { System.currentTimeMillis() }
                delay(60_000L)
            }
        }
    }

    private fun compute(purchased: Boolean, now: Long): Entitlement {
        if (purchased) return Entitlement.Purchased
        val start = trialPrefs.trialStartMs ?: trialPrefs.ensureTrialStarted()
        val elapsed = now - start
        val remaining = TrialPrefs.TRIAL_DURATION_MS - elapsed
        return when {
            remaining <= 0L -> Entitlement.Expired
            remaining <= TrialPrefs.SOFT_REMINDER_THRESHOLD_MS -> Entitlement.TrialEndingSoon(remaining)
            else -> Entitlement.Trial(remaining)
        }
    }

    /** Helper for the Settings banner. Returns "X days, Y hours" or "Z hours". */
    fun formatRemaining(ms: Long): String {
        val totalHours = (ms / (60L * 60L * 1000L)).coerceAtLeast(0L)
        val days = totalHours / 24
        val hours = totalHours % 24
        return when {
            days > 0 -> "$days day${if (days == 1L) "" else "s"}, $hours hour${if (hours == 1L) "" else "s"}"
            totalHours > 0 -> "$totalHours hour${if (totalHours == 1L) "" else "s"}"
            else -> "less than an hour"
        }
    }
}
