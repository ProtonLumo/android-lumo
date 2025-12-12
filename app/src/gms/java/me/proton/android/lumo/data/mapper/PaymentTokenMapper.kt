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

class PaymentTokenMapper(private val billingManager: BillingManager) {

    fun parsePaymentToken(
        jsResult: Result<PaymentJsResponse>,
        currencyCode: String,
    ): Subscription? {
        // Now within the call back we can do whatever else we need to do...
        jsResult.onSuccess { paymentJsResponse ->
            // Now we can access the data from the successful PaymentJsResponse
            Timber.tag(TAG).i("JavaScript result success: $paymentJsResponse")

            // Check for error status in response
            if (paymentJsResponse.status == "error") {
                Timber.tag(TAG)
                    .e(
                        "Error in payment token response: ${paymentJsResponse.message}"
                    )
                billingManager._paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText(
                        paymentJsResponse.message
                            ?: "Unknown error creating payment token"
                    )
                )
                return null
            }

            val token: String? = try {
                when (paymentJsResponse.data) {
                    is JsonObject -> paymentJsResponse.data["Token"]?.jsonPrimitive?.contentOrNull
                    is JsonPrimitive -> paymentJsResponse.data.contentOrNull
                    else -> null
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(
                    e, "Error parsing token from response"
                )
                billingManager._paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText(
                        "Error processing payment response: ${e.message}"
                    )
                )
                null
            }

            if (token != null) {
                return Subscription(
                    paymentToken = token,
                    currency = currencyCode,
                    cycle = 1,
                    plans = mapOf("lumo2024" to 1),
                    couponCode = null,
                    billingAddress = null
                )
            } else {
                Timber.tag(TAG).e(
                    "Token was null in payment response"
                )
                billingManager._paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText(
                        "Payment token was not found in the server response"
                    )
                )
            }
        }.onFailure { error ->
            Timber.tag(TAG).e(
                error, "Payment token request failed "
            )
            // Use professional error classification
            val errorInfo = ErrorClassifier.classify(error)
            billingManager._paymentProcessingState.value = when (errorInfo.type) {
                ErrorClassifier.ErrorType.Network,
                ErrorClassifier.ErrorType.Timeout,
                ErrorClassifier.ErrorType.SSL -> PaymentProcessingState.NetworkError(
                    errorInfo.getUserMessage()
                )

                else -> PaymentProcessingState.Error(
                    errorInfo.getUserMessage()
                )
            }
        }

        return null
    }

    companion object {
        private const val TAG = "PaymentTokenMapper"
    }
}