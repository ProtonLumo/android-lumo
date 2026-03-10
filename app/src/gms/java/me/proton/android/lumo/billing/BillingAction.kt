package me.proton.android.lumo.billing

import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.text.UiText

sealed interface BillingAction {

    data class Initialize(val paymentEvent: PaymentEvent) : BillingAction
    data object RetryConnection : BillingAction
    data object BillingConnected : BillingAction
    data class BillingDisconnected(
        val reason: String?,
        val isBillingAvailable: Boolean
    ) : BillingAction
    data class ProductDetailsLoaded(
        val googleProductDetails: List<GoogleProductDetails>,
        val products: List<ProductDetails>,
        val planFeatures: List<PlanFeature>,
        val planOptions: List<JsPlanInfo>
    ) : BillingAction
    data class PurchasesLoaded(
        val purchase: GooglePurchase?,
        val renewing: Boolean = false,
        val expiry: Long = 0L,
        val subscriptionResult: SubscriptionResult,
    ) : BillingAction
    data class PurchaseUpdated(
        val purchase: GooglePurchase
    ) : BillingAction
    data class SelectPlan(val index: Int) : BillingAction
    data class LaunchPurchase(
        val productId: String,
        val offerToken: String?,
        val customerId: String?
    ) : BillingAction
    data object RetryVerification : BillingAction
    data class Error(val message: UiText) : BillingAction
    data object BackendVerificationSucceeded : BillingAction
    data class BackendVerificationFailed(
        val message: UiText
    ) : BillingAction
}
