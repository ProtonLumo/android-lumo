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

    /* Backend data */
    suspend fun fetchSubscriptions(): SubscriptionResult
    suspend fun fetchPlans(): PlanResult

    /* Mapping helpers */
    fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        productDetails: List<ProductDetails>,
        offerId: String?
    ): List<JsPlanInfo>
}