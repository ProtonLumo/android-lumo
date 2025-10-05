package me.proton.android.lumo.data.repository

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.models.SubscriptionsResponse
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.utils.FeatureExtractor
import me.proton.android.lumo.utils.PlanExtractor
import me.proton.android.lumo.utils.PlanPricingHelper


private const val TAG = "SubscriptionRepository"

/**
 * Implementation of the SubscriptionRepository interface
 *
 * @param billingManager The BillingManager for Google Play integration
 */
class SubscriptionRepositoryImpl(
    private val billingManager: BillingManager?
) : SubscriptionRepository {

    override fun extractPlanFeatures(response: PaymentJsResponse): List<PlanFeature> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot extract features: Data is null or not a JSON object")
            return emptyList()
        }

        val dataObject = response.data.asJsonObject

        if (dataObject.has("Plans") && dataObject.get("Plans").isJsonArray) {
            val plansArray = dataObject.getAsJsonArray("Plans")
            if (plansArray.size() > 0) {
                val firstPlanObject = plansArray[0].asJsonObject
                return FeatureExtractor.extractPlanFeatures(firstPlanObject)
            }
        }

        return emptyList()
    }

    override fun extractPlans(response: PaymentJsResponse): List<JsPlanInfo> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot extract plans: Data is null or not a JSON object")
            return emptyList()
        }

        return PlanExtractor.extractPlans(dataObject = response.data.asJsonObject)
    }

    override fun hasValidSubscription(subscriptions: List<SubscriptionItemResponse>): Boolean {
        Log.e(TAG, "$subscriptions")
        return subscriptions.any { subscription ->
            // Check for Lumo or Visionary plans
            subscription.Name != null &&
                    (subscription.Name.contains("lumo", ignoreCase = true) ||
                            subscription.Name.contains("visionary", ignoreCase = true))
        }
    }

    /**
     * Parse subscriptions from API response
     */
    fun parseSubscriptions(response: PaymentJsResponse): List<SubscriptionItemResponse> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot parse subscriptions: Data is null or not a JSON object")
            return emptyList()
        }

        val gson = Gson()
        val dataObject = response.data.asJsonObject

        try {
            // Try parsing as SubscriptionsResponse (multiple subscriptions)
            if (dataObject.has("Subscriptions")) {
                val subscriptionsResponse = gson.fromJson(
                    response.data,
                    SubscriptionsResponse::class.java
                )

                Log.d(
                    TAG,
                    "Parsed multiple subscriptions: ${subscriptionsResponse.Subscriptions.size}"
                )
                return subscriptionsResponse.Subscriptions
            }
            // Try parsing as SubscriptionResponse (single subscription)
            else if (dataObject.has("Subscription")) {
                val subscriptionResponse = gson.fromJson(
                    response.data,
                    SubscriptionsResponse::class.java
                )

                Log.d(TAG, "Parsed single subscription response")
                return subscriptionResponse.Subscriptions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subscriptions: ${e.message}", e)
        }

        return emptyList()
    }

    override fun getGooglePlayProducts(): Flow<List<ProductDetails>> {
        return billingManager?.productDetailsList
            ?: flowOf(emptyList())
    }

    override fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        productDetails: List<ProductDetails>
    ): List<JsPlanInfo> {
        return PlanPricingHelper.updatePlanPricing(plans, productDetails)
    }

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
        TODO("Not yet implemented")
    }
}