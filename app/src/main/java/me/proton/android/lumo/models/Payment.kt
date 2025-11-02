package me.proton.android.lumo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InAppGooglePayload(
    @SerialName("purchaseToken") val purchaseToken: String,
    @SerialName("customerID") val customerID: String,
    @SerialName("packageName") val packageName: String,
    @SerialName("productID") val productID: String,
    @SerialName("orderID") val orderID: String
)

@Serializable
data class Payment(
    @SerialName("Type") val type: String,
    @SerialName("Details") val details: InAppGooglePayload? = null
)

@Serializable
data class PaymentTokenPayload(
    @SerialName("Amount") val amount: Int,
    @SerialName("Currency") val currency: String,
    @SerialName("PaymentMethodID") val paymentMethodID: String? = null,
    @SerialName("Payment") val payment: Payment? = null
)

@Serializable
data class Subscription(
    @SerialName("PaymentToken") val paymentToken: String?,
    @SerialName("Cycle") val cycle: Int,
    @SerialName("Currency") val currency: String,
    @SerialName("Plans") val plans: Map<String, Int>,
    @SerialName("CouponCode") val couponCode: String?,
    @SerialName("BillingAddress") val billingAddress: String?
)