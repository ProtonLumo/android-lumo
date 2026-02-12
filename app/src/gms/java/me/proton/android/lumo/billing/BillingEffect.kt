package me.proton.android.lumo.billing

import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.models.PaymentTokenPayload

sealed interface BillingEffect {

    /* Billing infrastructure */
    data object ConnectBilling : BillingEffect
    data object QueryProducts : BillingEffect
    data object QueryPurchases : BillingEffect

    data class LaunchBillingFlow(
        val productDetails: ProductDetails,
        val offerToken: String?,
        val customerId: String?
    ) : BillingEffect

    data class SendPaymentToken(
        val payload: PaymentTokenPayload
    ) : BillingEffect

    data class AcknowledgePurchase(
        val purchaseToken: String
    ) : BillingEffect
}
