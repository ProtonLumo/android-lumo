package me.proton.android.lumo.money_machine

import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.ui.text.UiText

data class BillingState(
    val connection: ConnectionState = ConnectionState.Idle,
    val products: List<ProductDetails> = emptyList(),
    val selectedPlanIndex: Int = 0,
    val backendHasValidSubscription: Boolean = false,
    val subscription: SubscriptionState = SubscriptionState.None,
    val payment: PaymentState = PaymentState.Idle,
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
    data object None : SubscriptionState

    data class Active(
        val renewing: Boolean,
        val expiryTimeMillis: Long
    ) : SubscriptionState
}

sealed interface PaymentState {
    data object Idle : PaymentState
    data object Verifying : PaymentState
    data object Success : PaymentState
    data class Error(val message: UiText) : PaymentState
}
