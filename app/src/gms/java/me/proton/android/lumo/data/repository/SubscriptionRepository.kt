package me.proton.android.lumo.data.repository

import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse

/**
 * Repository interface for handling subscription-related operations
 */
interface SubscriptionRepository {
    /**
     * Get the current user's subscriptions
     *
     * @return Result containing the subscription response
     */
    suspend fun getSubscriptions(): Result<PaymentJsResponse>

    /**
     * Get available subscription plans
     *
     * @return Result containing the plans response
     */
    suspend fun getPlans(): Result<PaymentJsResponse>

    /**
     * Extract plan features from API response
     *
     * @param response The API response containing plan data
     * @return List of plan features
     */
    fun extractPlanFeatures(response: PaymentJsResponse): List<PlanFeature>

    /**
     * Extract plan information from API response
     *
     * @param response The API response containing plan data
     * @return List of extracted plan info
     */
    fun extractPlans(response: PaymentJsResponse): List<JsPlanInfo>

    /**
     * Check if user has a valid subscription
     *
     * @param subscriptions List of user's subscriptions
     * @return True if user has a valid subscription
     */
    fun hasValidSubscription(subscriptions: List<SubscriptionItemResponse>): Boolean

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

    /**
     * Open Google Play subscription management screen
     */
    fun openSubscriptionManagementScreen()
} 