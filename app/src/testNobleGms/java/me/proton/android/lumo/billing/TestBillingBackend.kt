package me.proton.android.lumo.billing

import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.PaymentTokenPayload

class TestBillingBackend : BillingBackend {

    var backendResult: BackendResult = BackendResult.Success
    var subscriptionResult: SubscriptionResult = SubscriptionResult()
    var planResult: PlanResult = PlanResult()

    override suspend fun verifyPurchase(payload: PaymentTokenPayload): BackendResult =
        backendResult

    override suspend fun fetchSubscriptions(): SubscriptionResult =
        subscriptionResult

    override suspend fun fetchPlans(): PlanResult =
        planResult
}