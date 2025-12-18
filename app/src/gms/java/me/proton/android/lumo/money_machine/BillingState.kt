package me.proton.android.lumo.money_machine

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.text.UiText

data class BillingState(
    val connection: ConnectionState = ConnectionState.Idle,
    val subscriptionState: SubscriptionState = SubscriptionState.Loading,
    val plansState: PlansState = PlansState.Loading,
    val paymentState: PaymentState = PaymentState.Idle,
    val error: UiText? = null,
    val customerId: String = ""
)

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data object Unavailable : ConnectionState
}

sealed interface SubscriptionState {
    data object Loading : SubscriptionState
    data object None : SubscriptionState
    data class Mismatch(
        val purchase: Purchase,
    ) : SubscriptionState
    data class Active(
        val subscriptions: List<SubscriptionItemResponse>,
        val renewing: Boolean = false,
        val expiryTimeMillis: Long = 0,
        val internal: Boolean = true,
    ) : SubscriptionState
}

sealed interface PlansState {
    data object Loading : PlansState
    data class Success(
        val selectedPlanIndex: Int = 0,
        val planOptions: List<JsPlanInfo> = emptyList(),
        val planFeatures: List<PlanFeature> = emptyList(),
        val googleProductDetails: List<ProductDetails> = emptyList(),
    ) : PlansState
}

sealed interface PaymentState {
    data object Idle : PaymentState
    data object Verifying : PaymentState
    data object Success : PaymentState
    data class Error(val message: UiText) : PaymentState
}
