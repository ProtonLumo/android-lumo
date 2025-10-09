package me.proton.android.lumo.data.mapper

import android.util.Log
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText

class SubscriptionPurchaseHandler(private val billingManager: BillingManager) {

    fun handleSubscriptionEvent(jsResult: Result<PaymentJsResponse>) {
        jsResult.onSuccess { response ->
            // Success! Payment is fully processed
            Log.d(
                TAG,
                "Subscription activated successfully: $response"
            )
            billingManager._paymentProcessingState.value =
                PaymentProcessingState.Success
        }.onFailure { error ->
            Log.e(TAG, "Subscription request failed", error)
            billingManager._paymentProcessingState.value =
                PaymentProcessingState.Error(
                    UiText.StringText(
                        "Could not activate subscription: ${error.message}"
                    )
                )
        }
    }

    companion object {
        private const val TAG = "SubscriptionPurchaseHandler"
    }
}