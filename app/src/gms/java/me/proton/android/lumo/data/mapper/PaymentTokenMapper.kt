package me.proton.android.lumo.data.mapper

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.Subscription
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.utils.ErrorClassifier
import timber.log.Timber

class PaymentTokenMapper {

    fun parse(
        jsResult: Result<PaymentJsResponse>,
        currencyCode: String,
    ): Result<Subscription> {

        return jsResult.mapCatching { response ->

            if (response.status == "error") {
                error(
                    response.message ?: "Unknown error creating payment token"
                )
            }

            val token = extractToken(response)
                ?: error("Payment token not found in server response")

            Subscription(
                paymentToken = token,
                currency = currencyCode,
                cycle = 1,
                plans = mapOf("lumo2024" to 1),
                couponCode = null,
                billingAddress = null
            )
        }
    }

    private fun extractToken(response: PaymentJsResponse): String? =
        try {
            when (val data = response.data) {
                is JsonObject -> data["Token"]?.jsonPrimitive?.contentOrNull
                is JsonPrimitive -> data.contentOrNull
                else -> null
            }
        } catch (e: Exception) {
            null
        }
}
