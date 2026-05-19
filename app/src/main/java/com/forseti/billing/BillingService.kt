package com.forseti.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Google Play Billing v7 for the single non-consumable
 * product `forseti_unlock` ($4.99 one-time unlock). Owns:
 *
 *  • Connection lifecycle to [BillingClient].
 *  • A live [productDetails] flow so the Settings screen can show pricing.
 *  • A live [isPurchased] flow used by [EntitlementManager].
 *  • [launchPurchase] for the buy-now flow and [restore] to re-query history.
 *
 * Failures (Play Services missing, sandbox device with no Play account, etc.)
 * surface as [BillingState.Unavailable] so the UI can keep the user in trial
 * mode rather than locking them out because of a billing infra problem.
 */
@Singleton
class BillingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trialPrefs: TrialPrefs
) : PurchasesUpdatedListener {

    enum class BillingState { Disconnected, Connecting, Connected, Unavailable }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _state = MutableStateFlow(BillingState.Disconnected)
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _isPurchased = MutableStateFlow(trialPrefs.cachedPurchased)
    val isPurchased: StateFlow<Boolean> = _isPurchased.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun start() {
        if (_state.value == BillingState.Connected || _state.value == BillingState.Connecting) return
        connect()
    }

    private fun connect() {
        _state.value = BillingState.Connecting
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = BillingState.Connected
                    scope.launch {
                        refreshProduct()
                        refreshPurchases()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    _state.value = BillingState.Unavailable
                    _lastError.value = result.debugMessage.ifBlank { "Billing unavailable" }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = BillingState.Disconnected
            }
        })
    }

    fun consumeError() { _lastError.value = null }

    /** One-shot pull of product metadata + price text for the Settings UI. */
    private suspend fun refreshProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
        }
    }

    /** Re-pull purchase history from Play. Called on connect, on refresh, and after each new purchase. */
    suspend fun refreshPurchases(): Boolean {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchasesAsync failed: ${result.billingResult.debugMessage}")
            return _isPurchased.value
        }
        val purchased = result.purchasesList.any { p ->
            p.products.contains(PRODUCT_ID) && p.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        result.purchasesList.forEach { ackIfNeeded(it) }
        _isPurchased.value = purchased
        trialPrefs.cachedPurchased = purchased
        return purchased
    }

    /** Public hook for the "Restore purchase" button. */
    suspend fun restore(): Boolean = refreshPurchases()

    /** Launch the Play purchase flow for the unlock SKU. */
    fun launchPurchase(activity: Activity): Boolean {
        val product = _productDetails.value
        if (product == null) {
            _lastError.value = "Product details not loaded yet — try again in a moment."
            return false
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _lastError.value = result.debugMessage.ifBlank { "Could not start the purchase flow." }
            return false
        }
        return true
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { ackIfNeeded(it) }
                scope.launch { refreshPurchases() }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* user backed out */ }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { refreshPurchases() }
            }
            else -> {
                _lastError.value = result.debugMessage.ifBlank {
                    "Purchase failed (code ${result.responseCode})."
                }
            }
        }
    }

    /**
     * Non-consumable IAPs must be acknowledged within 3 days of purchase or
     * Google auto-refunds. Doing it as soon as we see the purchase keeps us
     * compliant.
     */
    private fun ackIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        scope.launch {
            runCatching { client.acknowledgePurchase(params) }
                .onFailure { Log.w(TAG, "acknowledgePurchase failed", it) }
        }
    }

    companion object {
        const val PRODUCT_ID = "forseti_unlock"
        private const val TAG = "BillingService"
    }
}
