package me.proton.android.lumo.data.mapper

import android.util.Log
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.Subscription
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.utils.ErrorClassifier

class PaymentTokenMapper(private val billingManager: BillingManager) {

    fun parsePaymentToken(
        jsResult: Result<PaymentJsResponse>,
        currencyCode: String,
    ): Subscription? {
        // Now within the call back we can do whatever else we need to do...
        jsResult.onSuccess { paymentJsResponse ->
            // Now we can access the data from the successful PaymentJsResponse
            Log.d(TAG, "JavaScript result success: $paymentJsResponse")

            // Check for error status in response
            if (paymentJsResponse.status == "error") {
                Log.e(
                    TAG,
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

            val token = try {
                // If data is a JsonObject
                if (paymentJsResponse.data?.isJsonObject == true) {
                    paymentJsResponse.data.asJsonObject?.get("Token")?.asString
                }
                // If data is directly a primitive (like a string)
                else if (paymentJsResponse.data?.isJsonPrimitive == true) {
                    paymentJsResponse.data.asString
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing token from response", e)
                billingManager._paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText(
                        "Error processing payment response: ${e.message}"
                    )
                )
                return null
            }

            if (token != null) {
                return Subscription(
                    PaymentToken = token,
                    Currency = currencyCode,
                    Cycle = 1,
                    Plans = mapOf("lumo2024" to 1),
                    CouponCode = null,
                    BillingAddress = null
                )
            } else {
                Log.e(TAG, "Token was null in payment response")
                billingManager._paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText(
                        "Payment token was not found in the server response"
                    )
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Payment token request failed", error)
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