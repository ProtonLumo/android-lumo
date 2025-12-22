package me.proton.android.lumo.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.text.UiText

sealed interface BillingAction {

    /* Lifecycle */
    data object Initialize : BillingAction
    data object RetryConnection : BillingAction

    /* BillingClient lifecycle */
    data class BillingConnected(val success: Boolean) : BillingAction
    data class BillingDisconnected(val reason: String?) : BillingAction

    /* Data loaded from Play */
    data class ProductDetailsLoaded(
        val products: List<ProductDetails>,
        val planFeatures: List<PlanFeature>,
        val planOptions: List<JsPlanInfo>
    ) : BillingAction

    data class PurchasesLoaded(
        val purchase: Purchase?,
        val renewing: Boolean = false,
        val expiry: Long = 0L,
        val subscriptionResult: SubscriptionResult,
    ) : BillingAction

    data class PurchaseUpdated(
        val purchase: Purchase
    ) : BillingAction

    /* User intent */
    data class SelectPlan(val index: Int) : BillingAction

    data class LaunchPurchase(
        val productId: String,
        val offerToken: String?,
        val customerId: String
    ) : BillingAction

    data object RetryVerification : BillingAction

    /* Errors */
    data class Error(val message: UiText) : BillingAction

    data object BackendVerificationSucceeded : BillingAction

    data class BackendVerificationFailed(
        val message: UiText
    ) : BillingAction
}
