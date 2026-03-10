package me.proton.android.lumo.billing

import me.proton.android.lumo.MainActivityViewModel.PaymentEvent.Default
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.utils.takeIf

@Suppress("LongMethod")
fun billingReducer(
    state: BillingState,
    action: BillingAction
): Pair<BillingState, List<BillingEffect>> =
    when (action) {
        is BillingAction.Initialize -> {
            state.copy(
                connection = ConnectionState.Connecting,
                error = null,
                subscriptionState = SubscriptionState.Loading,
                plansState = PlansState.Loading,
                paymentState = PaymentState.Idle,
                paymentEvent = action.paymentEvent,
            ) to listOf(BillingEffect.ConnectBilling)
        }

        BillingAction.RetryConnection -> {
            state.copy(
                connection = ConnectionState.Connecting
            ) to listOf(BillingEffect.ConnectBilling)
        }

        is BillingAction.BillingConnected -> {
            state.copy(
                connection = ConnectionState.Connected
            ) to listOf(BillingEffect.QueryProducts)
        }

        is BillingAction.BillingDisconnected -> {
            if (action.isBillingAvailable) {
                state.copy(
                    connection = ConnectionState.Connecting
                ) to listOf(BillingEffect.ConnectBilling)
            } else {
                state.copy(
                    connection = ConnectionState.Unavailable,
                    error = action.reason.fromDebugMessage()
                ) to listOf()
            }
        }

        is BillingAction.ProductDetailsLoaded -> {
            val products = action.googleProductDetails
            val planOptions = if (state.paymentEvent == Default) {
                action.planOptions
            } else {
                action.planOptions.reversed()
            }
            state.copy(
                plansState = PlansState.Success(
                    planOptions = planOptions,
                    planFeatures = action.planFeatures,
                    googleProductDetails = products,
                    productDetails = action.products,
                ),
            ) to listOf(BillingEffect.QueryPurchases)
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

                else ->
                    SubscriptionState.Active(
                        subscriptions = subscriptionResult.subscriptions,
                        renewing = action.renewing,
                        expiryTimeMillis = action.expiry,
                        internal = false,
                    )
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
                ) to listOfNotNull(
                    BillingEffect.SendPaymentToken(
                        buildPaymentPayload(
                            purchase = action.purchase,
                            product = product,
                            customerId = state.customerId
                        )
                    ),
                    BillingEffect.AcknowledgePurchase(
                        action.purchase.purchaseToken
                    ).takeIf { !action.purchase.isAcknowledged }
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
                is PlansState.Success -> planState.getProductDetail(action.productId)
            }

            if (product == null) {
                state to emptyList() // todo: handle error
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
                state.plansState is PlansState.Success
            ) {
                val purchase = state.subscriptionState.purchase
                val productId = purchase.products.first()
                val customerId = purchase.obfuscatedAccountId
                state.plansState.googleProductDetails
                    .find { it.productId == productId }
                    ?.let { product ->
                        state.copy(
                            paymentState = PaymentState.Verifying,
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
