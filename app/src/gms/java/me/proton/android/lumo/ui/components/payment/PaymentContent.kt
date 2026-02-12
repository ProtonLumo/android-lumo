package me.proton.android.lumo.ui.components.payment

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.BillingState
import me.proton.android.lumo.billing.ConnectionState
import me.proton.android.lumo.billing.GooglePurchase
import me.proton.android.lumo.billing.PaymentState
import me.proton.android.lumo.billing.PlansState
import me.proton.android.lumo.billing.SubscriptionState
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionEntitlement
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.components.plan.PlanSelectionScreen
import me.proton.android.lumo.ui.components.subscription.SubscriptionOverviewDialog
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.ui.theme.AppStyle.Companion.isDarkTheme
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel

@Suppress("LongMethod", "LongParameterList")
@Composable
fun PaymentContent(
    uiState: SubscriptionViewModel.UiState,
    onDismiss: () -> Unit,
    retryPayment: () -> Unit,
    launchPaymentFlow: (JsPlanInfo) -> Unit,
    selectPlan: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val billingState = uiState.billingState
    val subscriptionState = billingState.subscriptionState
    val paymentState = billingState.paymentState
    val plansState = billingState.plansState

    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color = LumoTheme.colors.backgroundNorm)
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Header(
                paymentEvent = uiState.paymentEvent,
                isDarkTheme = uiState.theme.isDarkTheme()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = LumoTheme.colors.textNorm
                )
            }
        }

        if (billingState.error != null) {
            PaymentError(
                title = stringResource(R.string.error_generic),
                message = billingState.error.getText(context),
                modifier = Modifier.padding(24.dp)
            )
        } else {
            when (paymentState) {
                is PaymentState.Idle ->
                    when (subscriptionState) {
                        is SubscriptionState.Active -> {
                            SubscriptionOverviewDialog(
                                activeSubscriptions = subscriptionState,
                                googleProductDetails = if (plansState is PlansState.Success) {
                                    plansState.googleProductDetails
                                } else {
                                    emptyList()
                                },
                                onClose = onDismiss
                            )
                        }

                        is SubscriptionState.Loading ->
                            Loader(
                                modifier = Modifier.padding(24.dp),
                                messageRes = R.string.payment_checking
                            )

                        is SubscriptionState.Mismatch -> SubscriptionRecoveryContent(
                            modifier = Modifier.padding(24.dp),
                            onClose = onDismiss,
                            onRetry = retryPayment
                        )

                        is SubscriptionState.None ->
                            PlanSelectionScreen(
                                plansState = plansState,
                                paymentEvent = uiState.paymentEvent,
                                isDarkTheme = uiState.theme.isDarkTheme(),
                                onDismiss = onDismiss,
                                onSelectPlan = { selectPlan(it) },
                                onClickPurchase = { planToPurchase ->
                                    launchPaymentFlow(planToPurchase)
                                },
                            )
                    }

                is PaymentState.Success -> PaymentSuccessContent(
                    modifier = Modifier.padding(24.dp),
                    onClose = onDismiss
                )

                is PaymentState.Verifying ->
                    PaymentVerifyingContent(modifier = Modifier.padding(24.dp))

                is PaymentState.Error -> PaymentErrorContent(
                    modifier = Modifier.padding(24.dp),
                    message = paymentState.message.getText(context),
                    onRetry = retryPayment,
                    onClose = onDismiss
                )
            }
        }
    }
}

@Preview(name = "Loading Subscriptions", showBackground = true)
@Preview(
    name = "Dark - Loading Subscriptions",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun LoadingSubscriptionsPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.Loading,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Idle,
                    error = null,
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Loading Plans", showBackground = true)
@Preview(
    name = "Dark - Loading Plans",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun LoadingPlansPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Idle,
                    error = null,
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "General error", showBackground = true)
@Preview(
    name = "Dark - General error",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun GeneralErrorPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Idle,
                    error = UiText.StringText("General error"),
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Payment error", showBackground = true)
@Preview(
    name = "Dark - Payment error",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentErrorPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Error(
                        message = UiText.StringText("Payment error")
                    ),
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Subscription mismatch", showBackground = true)
@Preview(
    name = "Dark - Subscription mismatch",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun SubscriptionMismatchPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.Mismatch(
                        purchase = GooglePurchase()
                    ),
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Idle,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Payment verifying", showBackground = true)
@Preview(
    name = "Dark - Payment verifying",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentVerifyingPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Verifying,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Payment success", showBackground = true)
@Preview(
    name = "Dark - Payment success",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentSuccessPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Success,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Active subscription", showBackground = true)
@Preview(
    name = "Dark - Active subscription",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun ActiveSubscriptionPreview() {
    val subscription = SubscriptionItemResponse(
        id = "aaa==",
        invoiceId = "-aaa-7gm5YLf215MEgZCdzOtLW5psxgB8oNc8OnoFRykab4Z23EGEW1ka3GtQPF9xwx9-VUA==",
        title = "Mail Plus",
        description = "Current plan",
        name = "mail2022",
        cycle = 12,
        cycleDescription = "For 1 year",
        currency = "CHF",
        amount = 4788,
        offer = "default",
        periodStart = System.currentTimeMillis() / 1000,
        periodEnd = (System.currentTimeMillis() + 365 * 24 * 60 * 60) / 1000,
        createTime = System.currentTimeMillis() / 1000,
        couponCode = null,
        discount = 0,
        renewDiscount = 0,
        renewAmount = 4788,
        renew = 0,
        external = 0,
        billingPlatform = 1,
        entitlements = listOf(
            SubscriptionEntitlement(
                type = "description",
                iconName = "checkmark",
                text = "And the free features of all other Proton products!"
            )
        ),
        decorations = emptyList(),
        isTrial = false,
        customerID = null
    )
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.Active(
                        subscriptions = listOf(subscription),
                        renewing = true,
                        expiryTimeMillis = 0L,
                        internal = true,
                    ),
                    plansState = PlansState.Loading,
                    paymentState = PaymentState.Idle,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Suppress("LongMethod")
@Preview(name = "Select plan", showBackground = true)
@Preview(
    name = "Dark - Select plan",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun SelectPlanPreview() {
    val mockPlans = listOf(
        JsPlanInfo(
            id = "lumo-plus-monthly",
            name = "Lumo Plus",
            duration = UiText.StringText("1 month"),
            cycle = 1,
            description = "Monthly subscription",
            productId = "lumo_plus_monthly",
            customerId = "customer123",
            pricePerMonth = "$9.99",
            totalPrice = "$9.99",
            savings = null,
            offerToken = "token123"
        ),
        JsPlanInfo(
            id = "lumo-plus-annual",
            name = "Lumo Plus",
            duration = UiText.StringText("12 months"),
            cycle = 12,
            description = "Annual subscription",
            productId = "lumo_plus_annual",
            customerId = "customer123",
            pricePerMonth = "$7.99",
            totalPrice = "$95.99",
            savings = "Save 20%",
            offerToken = "token456"
        )
    )

    val mockFeatures = listOf(
        PlanFeature(
            name = "AI Responses",
            freeText = "Limited",
            paidText = "Unlimited",
            iconName = "ai-responses"
        ),
        PlanFeature(
            name = "Priority Support",
            freeText = "Standard",
            paidText = "Priority",
            iconName = "priority-support"
        ),
        PlanFeature(
            name = "Advanced Features",
            freeText = "Basic",
            paidText = "Advanced",
            iconName = "advanced-features"
        )
    )
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Success(
                        selectedPlanIndex = 0,
                        planOptions = mockPlans,
                        planFeatures = mockFeatures,
                        googleProductDetails = emptyList(),
                    ),
                    paymentState = PaymentState.Idle,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}

@Preview(name = "Empty plans", showBackground = true)
@Preview(
    name = "Dark - Empty plans",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun EmptyPlansPreview() {
    LumoTheme {
        PaymentContent(
            uiState = SubscriptionViewModel.UiState(
                billingState = BillingState(
                    connection = ConnectionState.Connected,
                    subscriptionState = SubscriptionState.None,
                    plansState = PlansState.Success(
                        selectedPlanIndex = 0,
                        planOptions = emptyList(),
                        planFeatures = emptyList(),
                        googleProductDetails = emptyList(),
                    ),
                    paymentState = PaymentState.Idle,
                    error = null
                ),
                theme = if (isSystemInDarkTheme()) AppStyle.Dark else AppStyle.Light
            ),
            onDismiss = {},
            retryPayment = {},
            launchPaymentFlow = {},
            selectPlan = {},
        )
    }
}









