package me.proton.android.lumo.models

/**
 * Represents a subscription entitlement feature description
 */
data class SubscriptionEntitlement(
    val type: String, val text: String, val iconName: String, val hint: String? = null
)

data class SubscriptionItemResponse(
    val ID: String,
    val InvoiceID: String,
    val Cycle: Int,
    val PeriodStart: Long,
    val PeriodEnd: Long,
    val CreateTime: Long,
    val CouponCode: String?,
    val Currency: String,
    val Amount: Int,
    val Discount: Int,
    val RenewDiscount: Int,
    val RenewAmount: Int,
    val Renew: Int,
    val External: Int,
    val BillingPlatform: Int,
    val IsTrial: Boolean,
    val CustomerID: String?,
    val Title: String? = null,
    val Description: String? = null,
    val Name: String? = null,
    val CycleDescription: String? = null,
    val Offer: String? = null,
    val Entitlements: List<SubscriptionEntitlement>? = null,
    val Decorations: List<String>? = null
)

data class SubscriptionsResponse(
    val Code: Number,
    val Subscriptions: List<SubscriptionItemResponse>,
    val UpcomingSubscriptions: List<SubscriptionItemResponse>? = null,
    val uid: String? = null
)

data class SubscriptionPlan(
    val productId: String,
    val planName: String,
    val durationMonths: Int,
    val description: String = "",
    var price: String = "",
    var formattedPrice: String = "",
    var periodText: String = "",
    var priceAmountMicros: Long = 0
)
