package me.proton.android.lumo.data.repository

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
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.utils.PlanPricingHelper
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import timber.log.Timber
import javax.inject.Inject


private const val TAG = "SubscriptionRepository"

/**
 * Implementation of the SubscriptionRepository interface
 */
class SubscriptionRepositoryImpl @Inject constructor(
    private val webBridge: WebAppWithPaymentsInterface,
    private val subscriptionMapper: SubscriptionMapper,
    private val planMapper: PlanMapper
) : SubscriptionRepository {

    override suspend fun fetchSubscriptions(): SubscriptionResult {
        val result = webBridge.fetchSubscriptions()
        return subscriptionMapper.parseSubscriptions(result)
    }

    override suspend fun fetchPlans(): PlanResult {
        val result = webBridge.fetchPlans()
        return planMapper.parsePlans(result)
    }

    override fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        productDetails: List<ProductDetails>,
        offerId: String?
    ): List<JsPlanInfo> =
        PlanPricingHelper.updatePlanPricing(
            plans = plans,
            googleProducts = productDetails,
            offerId = offerId
        )
}
