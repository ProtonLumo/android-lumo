package me.proton.android.lumo.data.repository

import android.content.Context
import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.models.SubscriptionsResponse
import me.proton.android.lumo.utils.FeatureExtractor
import me.proton.android.lumo.utils.PlanExtractor
import me.proton.android.lumo.utils.PlanPricingHelper
import kotlin.coroutines.resume


private const val TAG = "SubscriptionRepository"

/**
 * Implementation of the SubscriptionRepository interface
 *
 * @param context Application context for string resources
 * @param mainActivity Reference to MainActivity for WebView access
 * @param billingManager The BillingManager for Google Play integration
 */
class SubscriptionRepositoryImpl(
    private val context: Context,
    private val mainActivity: MainActivity,
    private val billingManager: BillingManager?
) : SubscriptionRepository {

    override suspend fun getSubscriptions(): Result<PaymentJsResponse> =
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Getting user subscriptions")

            val webView = mainActivity.webView
            if (webView == null) {
                continuation.resume(Result.failure(Exception(context.getString(R.string.error_webview_not_available))))
                return@suspendCancellableCoroutine
            }

            mainActivity.getSubscriptionsFromWebView(webView) { result ->
                continuation.resume(result)
            }
        }

    override suspend fun getPlans(): Result<PaymentJsResponse> =
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Getting available subscription plans")

            val webView = mainActivity.webView
            if (webView == null) {
                continuation.resume(Result.failure(Exception(context.getString(R.string.error_webview_not_available))))
                return@suspendCancellableCoroutine
            }

            mainActivity.getPlansFromWebView(webView) { result ->
                continuation.resume(result)
            }
        }

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

        return PlanExtractor.extractPlans(response.data.asJsonObject, mainActivity)
    }

    override fun hasValidSubscription(subscriptions: List<SubscriptionItemResponse>): Boolean {
        Log.e(TAG, "$subscriptions")
        return subscriptions.any { subscription ->
            // Check for Lumo or Visionary plans
            subscription.Name?.contains("lumo", ignoreCase = true) == true ||
                    subscription.Name?.contains("visionary", ignoreCase = true) == true
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
}