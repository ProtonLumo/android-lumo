package me.proton.android.lumo.money_machine

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import me.proton.android.lumo.models.InAppGooglePayload
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.Payment
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.utils.PlanPricingHelper

fun parseSubscription(purchase: Purchase): Pair<Boolean, Long> {
    val renewing = purchase.isAutoRenewing
    val json = Json { ignoreUnknownKeys = true }
    val obj = json.decodeFromString<JsonObject>(purchase.originalJson)
    val expiry = obj["expiryTimeMillis"]?.jsonPrimitive?.long ?: 0L
    return renewing to expiry
}

fun buildPaymentPayload(
    purchase: Purchase,
    product: ProductDetails,
    customerId: String
): PaymentTokenPayload {
    val priceMicros =
        product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.priceAmountMicros ?: 0L

    val currency =
        product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.priceCurrencyCode ?: "USD"

    return PaymentTokenPayload(
        amount = (priceMicros / 10_000).toInt(),
        currency = currency,
        payment = Payment(
            type = "google",
            details = InAppGooglePayload(
                purchaseToken = purchase.purchaseToken,
                customerID = customerId,
                packageName = purchase.packageName,
                productID = purchase.products.first(),
                orderID = purchase.orderId.orEmpty()
            )
        )
    )
}

fun updatePlanPricing(
    plans: List<JsPlanInfo>,
    productDetails: List<ProductDetails>,
    offerId: String?
): List<JsPlanInfo> =
    PlanPricingHelper.updatePlanPricing(
        plans = plans,
        googleProducts = productDetails,
        offerId = offerId
    )
