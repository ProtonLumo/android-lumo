package me.proton.android.lumo.models

import com.google.gson.annotations.SerializedName


data class InAppGooglePayload(
    val purchaseToken: String,
    val customerID: String,
    val packageName: String,
    val productID: String,
    val orderID: String
)

data class Payment(
    @SerializedName("Type")
    val type: String,
    @SerializedName("Details")
    val details: InAppGooglePayload? = null
)

data class PaymentTokenPayload(
    @SerializedName("Amount")
    val amount: Int,
    @SerializedName("Currency")
    val currency: String,
    @SerializedName("PaymentMethodID")
    val paymentMethodID: String? = null,
    @SerializedName("Payment")
    val payment: Payment? = null
)

data class Subscription(
    @SerializedName("PaymentToken")
    val paymentToken: String?,
    @SerializedName("Cycle")
    val cycle: Int,
    @SerializedName("Currency")
    val currency: String,
    @SerializedName("Plans")
    val plans: Map<String, Int>,
    @SerializedName("CouponCode")
    val couponCode: String?,
    @SerializedName("BillingAddress")
    val billingAddress: String?
)