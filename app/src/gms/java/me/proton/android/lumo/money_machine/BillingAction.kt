package me.proton.android.lumo.money_machine

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
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
        val products: List<ProductDetails>
    ) : BillingAction

    data class PurchasesLoaded(
        val purchases: List<Purchase>
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
