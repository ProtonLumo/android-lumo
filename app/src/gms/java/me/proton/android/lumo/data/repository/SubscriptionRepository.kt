package me.proton.android.lumo.data.repository

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.ui.components.PaymentProcessingState

/**
 * Repository interface for handling subscription-related operations
 */
interface SubscriptionRepository {
    /**
     * Get Google Play product details
     *
     * @return Flow of Google Play product details
     */
    fun getGooglePlayProducts(): Flow<List<ProductDetails>>

    /**
     * Update plan pricing information with Google Play data
     *
     * @param plans List of plans to update
     * @param productDetails List of Google Play product details
     * @return Updated plans with pricing information
     */
    fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        productDetails: List<ProductDetails>
    ): List<JsPlanInfo>

    /**
     * Get Google Play subscription status
     *
     * @return Triple of (isActive, isAutoRenewing, expiryTimeMillis)
     */
    fun getGooglePlaySubscriptionStatus(): Triple<Boolean, Boolean, Long>

    /**
     * Refresh subscription status from Google Play
     */
    fun refreshGooglePlaySubscriptionStatus()

    /**
     * Invalidate cached subscription data and force fresh queries
     */
    fun invalidateSubscriptionCache()

    fun getPaymentProcessingState(): Flow<PaymentProcessingState?>

    fun isRefreshingPurchases(): Flow<Boolean>

    fun triggerSubscriptionRecovery()

    fun retryPaymentVerification()

    fun resetPaymentState()

    fun launchBillingFlowForProduct(
        productId: String,
        offerToken: String?,
        customerID: String? = null,
        getBillingResult: (BillingClient?, BillingFlowParams) -> BillingResult?
    )

    suspend fun fetchSubscriptions(): SubscriptionResult

    suspend fun fetchPlans(): PlanResult
}