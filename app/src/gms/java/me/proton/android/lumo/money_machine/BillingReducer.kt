package me.proton.android.lumo.money_machine

import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText

fun billingReducer(
    state: BillingState,
    action: BillingAction
): Pair<BillingState, List<BillingEffect>> =
    when (action) {
        BillingAction.Initialize -> {
            state.copy(
                connection = ConnectionState.Connecting,
                error = null,
                payment = PaymentState.Idle,
            ) to listOf(BillingEffect.ConnectBilling)
        }

        BillingAction.RetryConnection -> {
            state.copy(
                connection = ConnectionState.Connecting
            ) to listOf(BillingEffect.ConnectBilling)
        }

        is BillingAction.BillingConnected -> {
            if (!action.success) {
                state.copy(
                    connection = ConnectionState.Unavailable,
                    error = UiText.ResText(R.string.billing_unavailable_generic)
                ) to emptyList()
            } else {
                state.copy(
                    connection = ConnectionState.Connected
                ) to listOf(
                    BillingEffect.QueryPurchases,
                    BillingEffect.QueryProducts,
                )
            }
        }

        is BillingAction.BillingDisconnected -> {
            state.copy(
                connection = ConnectionState.Connecting
            ) to listOf(BillingEffect.ConnectBilling)
        }

        is BillingAction.ProductDetailsLoaded -> {
            state.copy(
                products = action.products,
                selectedPlanIndex = 0
            ) to emptyList()
        }

        is BillingAction.PurchasesLoaded -> {
            val purchase = action.purchases.firstOrNull()

            if (purchase == null) {
                state.copy(
                    subscription = SubscriptionState.None
                ) to emptyList()
            } else {
                val (renewing, expiry) = parseSubscription(purchase)
                state.copy(
                    subscription = SubscriptionState.Active(
                        renewing = renewing,
                        expiryTimeMillis = expiry
                    )
                ) to emptyList()
            }
        }

        is BillingAction.PurchaseUpdated -> {
            val product = state.products.getOrNull(state.selectedPlanIndex)

            if (product == null) {
                state.copy(
                    payment = PaymentState.Error(
                        UiText.StringText("Product details missing")
                    )
                ) to emptyList()
            } else {
                state.copy(
                    payment = PaymentState.Verifying
                ) to listOf(
                    BillingEffect.SendPaymentToken(
                        buildPaymentPayload(
                            purchase = action.purchase,
                            product = product,
                            customerId = state.customerId
                        )
                    ),
                    BillingEffect.AcknowledgePurchase(
                        action.purchase.purchaseToken
                    )
                )
            }
        }

        is BillingAction.SelectPlan -> {
            state.copy(
                selectedPlanIndex = action.index
            ) to emptyList()
        }

        is BillingAction.LaunchPurchase -> {
            val product = state.products.first { it.productId == action.productId }

            state.copy(
                customerId = action.customerId
            ) to listOf(
                BillingEffect.LaunchBillingFlow(
                    productDetails = product,
                    offerToken = action.offerToken,
                    customerId = action.customerId
                )
            )
        }
        // TODO: fix me
        BillingAction.RetryVerification -> {
            state to listOf(BillingEffect.QueryPurchases)
        }

        is BillingAction.Error -> {
            state.copy(error = action.message) to emptyList()
        }

        is BillingAction.BackendVerificationSucceeded -> {
            state.copy(
                payment = PaymentState.Success,
                customerId = "",
                error = null
            ) to emptyList()
        }

        is BillingAction.BackendVerificationFailed -> {
            state.copy(
                payment = PaymentState.Error(
                    message = action.message,
                )
            ) to emptyList()
        }
    }
