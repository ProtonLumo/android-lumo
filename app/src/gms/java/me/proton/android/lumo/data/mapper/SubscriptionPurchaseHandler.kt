package me.proton.android.lumo.data.mapper

import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText
import timber.log.Timber

class SubscriptionPurchaseHandler(private val billingManager: BillingManager) {

    fun handleSubscriptionEvent(jsResult: Result<PaymentJsResponse>) {
        jsResult.onSuccess { response ->
            // Success! Payment is fully processed
            Timber.tag(TAG).i("Subscription activated successfully: $response")
            billingManager._paymentProcessingState.value =
                PaymentProcessingState.Success
        }.onFailure { error ->
            Timber.tag(TAG).e(error, "Subscription request failed")
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