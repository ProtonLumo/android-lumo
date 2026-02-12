package me.proton.android.lumo.billing

import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.data.mapper.PlanMapper
import me.proton.android.lumo.data.mapper.SubscriptionMapper
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Inject

interface BillingBackend {
    suspend fun verifyPurchase(payload: PaymentTokenPayload): BackendResult
    suspend fun fetchSubscriptions(): SubscriptionResult
    suspend fun fetchPlans(): PlanResult
}

class JsBillingBackend @Inject constructor(
    private val webBridge: WebAppWithPaymentsInterface,
    private val paymentTokenMapper: PaymentTokenMapper?,
    private val subscriptionMapper: SubscriptionMapper,
    private val planMapper: PlanMapper,
) : BillingBackend {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun verifyPurchase(
        payload: PaymentTokenPayload
    ): BackendResult =
        try {
            val paymentResult = webBridge.sendPaymentToken(payload)

            val subscriptionResult = paymentTokenMapper?.parse(
                jsResult = paymentResult,
                currencyCode = payload.currency
            )

            subscriptionResult?.let {
                if (subscriptionResult.isSuccess) {
                    val result = webBridge.sendSubscriptionEvent(subscriptionResult.getOrThrow())
                    if (result.isSuccess) {
                        BackendResult.Success
                    } else {
                        BackendResult.Failure(
                            UiText.StringText(
                                result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )
                    }
                } else {
                    BackendResult.Failure(
                        UiText.StringText(
                            subscriptionResult.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            } ?: BackendResult.Failure(
                message = UiText.StringText("Failed to parse subscription")
            )
        } catch (e: Exception) {
            BackendResult.Failure(
                message = UiText.StringText(
                    e.message ?: "Subscription verification failed"
                )
            )
        }

    override suspend fun fetchSubscriptions(): SubscriptionResult {
        val result = webBridge.fetchSubscriptions()
        return subscriptionMapper.parseSubscriptions(result)
    }

    override suspend fun fetchPlans(): PlanResult {
        val result = webBridge.fetchPlans()
        return planMapper.parsePlans(result)
    }
}


sealed interface BackendResult {
    data object Success : BackendResult
    data class Failure(
        val message: UiText
    ) : BackendResult
}
