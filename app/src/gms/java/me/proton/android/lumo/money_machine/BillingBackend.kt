package me.proton.android.lumo.money_machine

import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Inject

interface BillingBackend {
    suspend fun verifyPurchase(
        payload: PaymentTokenPayload
    ): BackendResult
}

class JsBillingBackend @Inject constructor(
    private val webBridge: WebAppWithPaymentsInterface,
    private val paymentTokenMapper: PaymentTokenMapper?,
) : BillingBackend {

    override suspend fun verifyPurchase(
        payload: PaymentTokenPayload
    ): BackendResult =
        try {
            val paymentResult = webBridge.sendPaymentToken(payload)

            val subscriptionResult = paymentTokenMapper?.parse(
                jsResult = paymentResult,
                currencyCode = payload.currency
            )

            if (subscriptionResult == null) {
                BackendResult.Failure(
                    message = UiText.StringText("Failed to parse subscription")
                )
            }

            if (subscriptionResult?.isSuccess == true) {
                val result = webBridge.sendSubscriptionEvent(subscriptionResult.getOrThrow())
                if (result.isSuccess) {
                    BackendResult.Success
                } else {
                    BackendResult.Failure(
                        UiText.StringText(
                            result.exceptionOrNull()?.message ?: "Unknown error")
                    )
                }
            } else {
                BackendResult.Failure(
                    UiText.StringText(
                        subscriptionResult.exceptionOrNull()?.message ?: "Unknown error")
                )
            }
        } catch (e: Exception) {
            BackendResult.Failure(
                message = UiText.StringText(
                    e.message ?: "Subscription verification failed"
                )
            )
        }
    }

sealed interface BackendResult {
    data object Success : BackendResult
    data class Failure(
        val message: UiText
    ) : BackendResult
}
