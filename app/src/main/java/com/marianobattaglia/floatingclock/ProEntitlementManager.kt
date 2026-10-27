package com.marianobattaglia.floatingclock

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import java.security.MessageDigest
import java.util.Locale

internal enum class ActualProStatus {
    CHECKING,
    FREE,
    PRO,
    UNAVAILABLE
}

internal enum class DebugProOverride {
    USE_PLAY,
    FREE,
    PRO
}

internal enum class ProBillingFeedback {
    NONE,
    PENDING,
    CANCELED,
    ERROR,
    UNAVAILABLE,
    PRODUCT_UNAVAILABLE
}

internal enum class ReviewAccessResult {
    GRANTED,
    INVALID,
    UNAVAILABLE
}

internal data class ProEntitlementUiState(
    val actualStatus: ActualProStatus,
    val debugOverride: DebugProOverride,
    val productPrice: String? = null,
    val feedback: ProBillingFeedback = ProBillingFeedback.NONE,
    val reviewAccessConfigured: Boolean = false,
    val reviewAccessGranted: Boolean = false
) {
    val effectiveStatus: ActualProStatus
        get() = if (BuildConfig.DEBUG) {
            when (debugOverride) {
                DebugProOverride.USE_PLAY -> actualStatus
                DebugProOverride.FREE -> ActualProStatus.FREE
                DebugProOverride.PRO -> ActualProStatus.PRO
            }
        } else if (reviewAccessGranted) {
            ActualProStatus.PRO
        } else {
            actualStatus
        }
}

/** Owns the app's single Play Billing connection and never stores account or purchase tokens. */
internal class ProEntitlementManager(context: Context) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var billingClient: BillingClient? = null
    private var connecting = false
    private var closed = false

    var state by mutableStateOf(
        ProEntitlementUiState(
            actualStatus = cachedStatus(),
            debugOverride = readDebugOverride(),
            reviewAccessConfigured = reviewAccessVerifier() != null,
            reviewAccessGranted = readReviewAccessGranted()
        )
    )
        private set

    fun start() {
        closed = false
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .enableAutoServiceReconnection()
                .build()
        }
        refresh()
    }

    fun refresh() {
        if (closed) return
        val client = billingClient ?: run {
            start()
            return
        }
        if (client.isReady) {
            queryOwnedPurchases(client)
            queryProductDetails(client)
        } else if (!connecting) {
            connecting = true
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (closed) return
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryOwnedPurchases(client)
                        queryProductDetails(client)
                    } else {
                        setUnavailableFeedback()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting = false
                    if (!closed) setUnavailableFeedback()
                }
            })
        }
    }

    fun launchProPurchase(activity: Activity) {
        val client = billingClient
        if (client == null || !client.isReady) {
            state = state.copy(feedback = ProBillingFeedback.UNAVAILABLE)
            refresh()
            return
        }

        state = state.copy(feedback = ProBillingFeedback.NONE)
        queryProductDetails(client) { productDetails, offer ->
            if (closed || !client.isReady) {
                state = state.copy(feedback = ProBillingFeedback.UNAVAILABLE)
                return@queryProductDetails
            }
            val offerToken = offer.offerToken
            if (offerToken.isNullOrBlank()) {
                state = state.copy(feedback = ProBillingFeedback.PRODUCT_UNAVAILABLE)
                return@queryProductDetails
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
            val result = client.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                state = state.copy(feedback = feedbackFor(result.responseCode))
            }
        }
    }

    fun setDebugOverride(override: DebugProOverride) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().putString(PREF_DEBUG_PRO_OVERRIDE, override.name).apply()
        state = state.copy(debugOverride = override)
    }

    fun redeemReviewAccessCode(code: String): ReviewAccessResult {
        val expectedHash = reviewAccessVerifier() ?: return ReviewAccessResult.UNAVAILABLE
        val normalizedCode = code.filter(Char::isLetterOrDigit).uppercase(Locale.ROOT)
        val suppliedHash = sha256(normalizedCode)
        val matches = MessageDigest.isEqual(
            expectedHash.toByteArray(Charsets.US_ASCII),
            suppliedHash.toByteArray(Charsets.US_ASCII)
        )
        if (!matches) return ReviewAccessResult.INVALID

        prefs.edit()
            .putString(PREF_REVIEW_ACCESS_GRANTED_HASH, expectedHash)
            .apply()
        state = state.copy(reviewAccessGranted = true)
        return ReviewAccessResult.GRANTED
    }

    fun close() {
        closed = true
        connecting = false
        billingClient?.endConnection()
        billingClient = null
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (closed) return
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val matching = purchases.orEmpty().filter { PRO_PRODUCT_ID in it.products }
                if (matching.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    state = state.copy(feedback = ProBillingFeedback.PENDING)
                }
                matching.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach(::grantCompletedPurchase)
                refresh()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                state = state.copy(feedback = ProBillingFeedback.CANCELED)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> state = state.copy(feedback = feedbackFor(result.responseCode))
        }
    }

    private fun queryOwnedPurchases(client: BillingClient) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (closed) return@queryPurchasesAsync
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                setUnavailableFeedback()
                return@queryPurchasesAsync
            }

            val proPurchases = purchases.filter { PRO_PRODUCT_ID in it.products }
            val completed = proPurchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (completed.isNotEmpty()) {
                completed.forEach(::grantCompletedPurchase)
            } else {
                confirmActualStatus(ActualProStatus.FREE)
                if (proPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    state = state.copy(feedback = ProBillingFeedback.PENDING)
                }
            }
        }
    }

    private fun queryProductDetails(
        client: BillingClient,
        onProductReady: ((ProductDetails, ProductDetails.OneTimePurchaseOfferDetails) -> Unit)? = null
    ) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (closed) return@queryProductDetailsAsync
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                state = state.copy(
                    productPrice = null,
                    feedback = ProBillingFeedback.PRODUCT_UNAVAILABLE
                )
                return@queryProductDetailsAsync
            }

            val details = detailsResult.productDetailsList
                .firstOrNull { it.productId == PRO_PRODUCT_ID }
            val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: details?.oneTimePurchaseOfferDetails
            if (details == null || offer == null) {
                state = state.copy(
                    productPrice = null,
                    feedback = ProBillingFeedback.PRODUCT_UNAVAILABLE
                )
                return@queryProductDetailsAsync
            }

            state = state.copy(
                productPrice = offer.formattedPrice,
                feedback = if (state.feedback == ProBillingFeedback.PRODUCT_UNAVAILABLE) {
                    ProBillingFeedback.NONE
                } else {
                    state.feedback
                }
            )
            onProductReady?.invoke(details, offer)
        }
    }

    private fun grantCompletedPurchase(purchase: Purchase) {
        confirmActualStatus(ActualProStatus.PRO)
        state = state.copy(feedback = ProBillingFeedback.NONE)
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                if (!closed && result.responseCode != BillingClient.BillingResponseCode.OK) {
                    state = state.copy(feedback = ProBillingFeedback.ERROR)
                }
            }
        }
    }

    private fun confirmActualStatus(status: ActualProStatus) {
        if (status != ActualProStatus.FREE && status != ActualProStatus.PRO) return
        val owned = status == ActualProStatus.PRO
        prefs.edit()
            .putBoolean(PREF_PRO_STATUS_CONFIRMED, true)
            .putBoolean(PREF_PRO_PURCHASED, owned)
            .apply()
        state = state.copy(actualStatus = status)
    }

    private fun cachedStatus(): ActualProStatus {
        if (!prefs.getBoolean(PREF_PRO_STATUS_CONFIRMED, false)) {
            return ActualProStatus.CHECKING
        }
        return if (prefs.getBoolean(PREF_PRO_PURCHASED, false)) {
            ActualProStatus.PRO
        } else {
            ActualProStatus.FREE
        }
    }

    private fun readDebugOverride(): DebugProOverride {
        if (!BuildConfig.DEBUG) return DebugProOverride.USE_PLAY
        return prefs.getString(PREF_DEBUG_PRO_OVERRIDE, DebugProOverride.USE_PLAY.name)
            ?.let { stored -> DebugProOverride.entries.firstOrNull { it.name == stored } }
            ?: DebugProOverride.USE_PLAY
    }

    private fun reviewAccessVerifier(): String? {
        if (BuildConfig.DEBUG) return null
        return BuildConfig.REVIEW_ACCESS_CODE_SHA256.takeIf(String::isNotBlank)
    }

    private fun readReviewAccessGranted(): Boolean {
        val verifier = reviewAccessVerifier()
        val storedVerifier = prefs.getString(PREF_REVIEW_ACCESS_GRANTED_HASH, null)
        if (verifier == null || storedVerifier != verifier) {
            if (storedVerifier != null) {
                prefs.edit().remove(PREF_REVIEW_ACCESS_GRANTED_HASH).apply()
            }
            return false
        }
        return true
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { it.toInt().and(0xff).toString(16).padStart(2, '0') }

    private fun setUnavailableFeedback() {
        state = state.copy(
            actualStatus = if (prefs.getBoolean(PREF_PRO_STATUS_CONFIRMED, false)) {
                cachedStatus()
            } else {
                ActualProStatus.UNAVAILABLE
            },
            feedback = ProBillingFeedback.UNAVAILABLE
        )
    }

    private fun feedbackFor(responseCode: Int): ProBillingFeedback = when (responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.NETWORK_ERROR -> ProBillingFeedback.UNAVAILABLE
        else -> ProBillingFeedback.ERROR
    }

    private companion object {
        const val PRO_PRODUCT_ID = "pro_lifetime"
    }
}
