package me.proton.android.lumo.models


data class InAppGooglePayload(
    val purchaseToken: String,
    val customerID: String,
    val packageName: String,
    val productID: String,
    val orderID: String
)

data class Payment(
    val type: String,
    val details: InAppGooglePayload? = null
)

data class PaymentTokenPayload(
    val amount: Int,
    val currency: String,
    val paymentMethodID: String? = null,
    val payment: Payment? = null
)

data class Subscription(
    val paymentToken: String?,
    val cycle: Int,
    val currency: String,
    val plans: Map<String, Int>,
    val couponCode: String?,
    val billingAddress: String?
)