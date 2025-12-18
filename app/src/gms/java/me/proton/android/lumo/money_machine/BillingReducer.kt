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
                subscriptionState = SubscriptionState.Loading,
                plansState = PlansState.Loading,
                paymentState = PaymentState.Idle,
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
                ) to listOf(BillingEffect.QueryProducts, BillingEffect.QueryPurchases)
            }
        }

        is BillingAction.BillingDisconnected -> {
            state.copy(
                connection = ConnectionState.Connecting
            ) to listOf(BillingEffect.ConnectBilling)
        }

        is BillingAction.ProductDetailsLoaded -> {
            state.copy(
                plansState = PlansState.Success(
                    planOptions = action.planOptions,
                    planFeatures = action.planFeatures,
                    googleProductDetails = action.products,
                ),
            ) to emptyList()
        }

        is BillingAction.PurchasesLoaded -> {
            val purchase = action.purchase
            val subscriptionResult = action.subscriptionResult

            val subscriptionState = when {
                purchase == null && !subscriptionResult.hasValidSubscription ->
                    SubscriptionState.None

                purchase == null && subscriptionResult.hasValidSubscription ->
                    SubscriptionState.Active(subscriptions = subscriptionResult.subscriptions)

                purchase != null && !subscriptionResult.hasValidSubscription ->
                    SubscriptionState.Mismatch(purchase = purchase)

                purchase != null && subscriptionResult.hasValidSubscription -> {
                    SubscriptionState.Active(
                        subscriptions = subscriptionResult.subscriptions,
                        renewing = action.renewing,
                        expiryTimeMillis = action.expiry,
                        internal = false,
                    )
                }

                else -> SubscriptionState.None
            }

            state.copy(subscriptionState = subscriptionState) to emptyList()
        }

        is BillingAction.PurchaseUpdated -> {
            val product = when (val planState = state.plansState) {
                is PlansState.Loading -> null
                is PlansState.Success -> planState.googleProductDetails.getOrNull(planState.selectedPlanIndex)
            }

            if (product == null) {
                state.copy(
                    paymentState = PaymentState.Error(
                        UiText.StringText("Product details missing")
                    )
                ) to emptyList()
            } else {
                state.copy(
                    paymentState = PaymentState.Verifying
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
            when (val planState = state.plansState) {
                is PlansState.Loading -> state to emptyList()
                is PlansState.Success ->
                    state.copy(
                        plansState = planState.copy(selectedPlanIndex = action.index)
                    ) to emptyList()
            }
        }

        is BillingAction.LaunchPurchase -> {
            val product = when (val planState = state.plansState) {
                is PlansState.Loading -> null
                is PlansState.Success ->
                    planState.googleProductDetails.firstOrNull() { it.productId == action.productId }
            }

            if (product == null) {
                state to emptyList()
            } else {
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
        }

        BillingAction.RetryVerification -> {
            if (state.subscriptionState is SubscriptionState.Mismatch &&
                state.plansState is PlansState.Success) {
                val purchase = state.subscriptionState.purchase
                val productId = purchase.products.first()
                val customerId = purchase.accountIdentifiers?.obfuscatedAccountId ?: ""
                state.plansState.googleProductDetails
                    .find { it.productId == productId }
                    ?.let { product ->
                        state.copy(
                            paymentState = PaymentState.Verifying,
                            subscriptionState = SubscriptionState.None,
                            customerId = customerId,
                        ) to listOf(
                            BillingEffect.SendPaymentToken(
                                buildPaymentPayload(
                                    purchase = state.subscriptionState.purchase,
                                    product = product,
                                    customerId = customerId
                                )
                            )
                        )
                    } ?: (state.copy(error = UiText.StringText("Unable to retry")) to emptyList())

            } else {
                state.copy(error = UiText.StringText("Unable to retry")) to emptyList()
            }
        }

        is BillingAction.Error -> {
            state.copy(error = action.message) to emptyList()
        }

        is BillingAction.BackendVerificationSucceeded -> {
            state.copy(
                paymentState = PaymentState.Success,
                customerId = "",
                error = null
            ) to emptyList()
        }

        is BillingAction.BackendVerificationFailed -> {
            state.copy(
                paymentState = PaymentState.Error(
                    message = action.message,
                )
            ) to emptyList()
        }
    }
