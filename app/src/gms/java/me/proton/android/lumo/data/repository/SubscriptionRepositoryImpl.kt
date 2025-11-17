package me.proton.android.lumo.data.repository

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.data.mapper.PlanMapper
import me.proton.android.lumo.data.mapper.SubscriptionMapper
import me.proton.android.lumo.data.mapper.SubscriptionPurchaseHandler
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.utils.PlanPricingHelper
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface


private const val TAG = "SubscriptionRepository"

/**
 * Implementation of the SubscriptionRepository interface
 *
 * @param billingManager The BillingManager for Google Play integration
 */
class SubscriptionRepositoryImpl(
    private val billingManager: BillingManager?,
    private val webBridge: WebAppWithPaymentsInterface,
    private val subscriptionMapper: SubscriptionMapper = SubscriptionMapper,
    private val planMapper: PlanMapper = PlanMapper,
    private val paymentTokenMapper: PaymentTokenMapper? = billingManager?.let {
        PaymentTokenMapper(
            billingManager = it
        )
    },
    private val subscriptionPurchaseHandler: SubscriptionPurchaseHandler? = billingManager?.let {
        SubscriptionPurchaseHandler(
            billingManager = it
        )
    }
) : SubscriptionRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        coroutineScope.launch {
            billingManager?.purchaseChannel?.collect {
                var result = sendPaymentToken(payload = it)
                val subscription = paymentTokenMapper?.parsePaymentToken(
                    jsResult = result,
                    currencyCode = it.currency
                )
                subscription?.let {
                    result = sendSubscriptionEvent(subscription)
                    subscriptionPurchaseHandler?.handleSubscriptionEvent(jsResult = result)
                } ?: run { Log.e(TAG, "Failed to load payment token") }
            }
        }
    }

    override fun getGooglePlayProducts(): Flow<List<ProductDetails>> {
        return billingManager?.productDetailsList ?: flowOf(emptyList())
    }

    override fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        productDetails: List<ProductDetails>,
        offerId: String?,
    ): List<JsPlanInfo> =
        PlanPricingHelper.updatePlanPricing(
            plans = plans,
            googleProducts = productDetails,
            offerId = offerId
        )

    override fun getGooglePlaySubscriptionStatus(): Triple<Boolean, Boolean, Long> {
        return billingManager?.getSubscriptionStatus()
            ?: Triple(false, false, 0L)
    }

    override fun refreshGooglePlaySubscriptionStatus() {
        billingManager?.refreshPurchaseStatus(forceRefresh = true)
    }

    override fun invalidateSubscriptionCache() {
        billingManager?.invalidateCache()
    }

    override fun getPaymentProcessingState(): Flow<PaymentProcessingState?> =
        billingManager?.paymentProcessingState ?: flowOf(null)

    override fun isRefreshingPurchases(): Flow<Boolean> =
        billingManager?.isRefreshingPurchases ?: flowOf(false)

    override fun triggerSubscriptionRecovery() {
        billingManager?.triggerSubscriptionRecovery()
    }

    override fun retryPaymentVerification() {
        billingManager?.retryPaymentVerification()
    }

    override fun resetPaymentState() {
        billingManager?.retryPaymentVerification()
    }

    override fun launchBillingFlowForProduct(
        productId: String,
        offerToken: String?,
        customerID: String?,
        getBillingResult: (BillingClient?, BillingFlowParams) -> BillingResult?
    ) {
        billingManager?.launchBillingFlowForProduct(
            productId = productId,
            offerToken = offerToken,
            customerID = customerID,
            getBillingResult = getBillingResult,
        )
    }

    override suspend fun fetchSubscriptions(): SubscriptionResult {
        val subscriptionResult = webBridge.fetchSubscriptions()
        return subscriptionMapper.parseSubscriptions(subscriptionResult)
    }

    override suspend fun fetchPlans(): PlanResult {
        val planResult = webBridge.fetchPlans()
        return planMapper.parsePlans(planResult)
    }

    private suspend fun sendPaymentToken(payload: PaymentTokenPayload): Result<PaymentJsResponse> =
        webBridge.sendPaymentToken(payload)

    private suspend fun sendSubscriptionEvent(payload: Subscription): Result<PaymentJsResponse> =
        webBridge.sendSubscriptionEvent(payload)
}