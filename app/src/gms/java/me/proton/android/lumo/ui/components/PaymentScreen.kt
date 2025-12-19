package me.proton.android.lumo.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.R
import me.proton.android.lumo.money_machine.PaymentState
import me.proton.android.lumo.money_machine.PlansState
import me.proton.android.lumo.money_machine.SubscriptionState
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.AppStyle.Companion.isDarkTheme
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel

private const val TAG = "PaymentDialog"

@Composable
fun PaymentScreen(
    paymentEvent: PaymentEvent,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SubscriptionViewModel = hiltViewModel()
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val billingState = uiState.billingState
    val subscriptionState = billingState.subscriptionState
    val paymentState = billingState.paymentState
    val plansState = billingState.plansState

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color = LumoTheme.colors.backgroundNorm)
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Header(
                paymentEvent = paymentEvent,
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
                            onRetry = { viewModel.retryPaymentVerification() }
                        )

                        is SubscriptionState.None ->
                            PlanSelectionScreen(
                                plansState = plansState,
                                paymentEvent = paymentEvent,
                                isDarkTheme = uiState.theme.isDarkTheme(),
                                onDismiss = onDismiss,
                                onPlanSelected = { viewModel.selectPlan(it) },
                                onPurchaseClicked = { planToPurchase ->
                                    viewModel.launchBillingFlow(
                                        productId = planToPurchase.productId,
                                        offerToken = planToPurchase.offerToken,
                                        customerId = planToPurchase.customerId!!,
                                    )
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
                    onRetry = { viewModel.retryPaymentVerification() },
                    onClose = onDismiss
                )
            }
        }
    }
}

@Composable
private fun Header(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    HeaderImage(
        paymentEvent = paymentEvent,
        isDarkTheme = isDarkTheme,
        modifier = modifier,
    )
}

@Composable
private fun HeaderImage(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val imageRes = when (paymentEvent) {
        PaymentEvent.Default -> R.drawable.lumo_cat_on_laptop
        PaymentEvent.BlackFriday ->
            if (isDarkTheme) {
                R.drawable.lumo_black_friday_dark
            } else {
                R.drawable.lumo_black_friday
            }
    }
    Image(
        painter = painterResource(id = imageRes),
        contentScale =
            when (paymentEvent) {
                PaymentEvent.Default -> ContentScale.Fit
                PaymentEvent.BlackFriday -> ContentScale.Crop
            },
        contentDescription = "Lumo Plus",
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (paymentEvent) {
                    PaymentEvent.Default ->
                        Modifier.windowInsetsPadding(WindowInsets.systemBars)

                    PaymentEvent.BlackFriday -> Modifier
                }
            )
    )
}

@Composable
fun Loader(
    messageRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = LumoTheme.colors.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.textWeak
        )
    }
}

// Preview Functions for Different Dialog States
//@Preview(name = "Loading Subscriptions", showBackground = true)
//@Preview(
//    name = "Dark - Loading Subscriptions",
//    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
//)
//@Composable
//fun PaymentDialogLoadingSubscriptionsPreview() {
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = true,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = false,
//                planOptions = emptyList(),
//                selectedPlan = null,
//                planFeatures = emptyList(),
//                errorMessage = null,
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

//@Preview(name = "Loading Plans", showBackground = true)
//@Preview(name = "Dark - Loading Plans", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
//@Composable
//fun PaymentDialogLoadingPlansPreview() {
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = false,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = true,
//                planOptions = emptyList(),
//                selectedPlan = null,
//                planFeatures = emptyList(),
//                errorMessage = null,
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

//@Preview(name = "Error State", showBackground = true)
//@Preview(name = "Dark - Error State", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
//@Composable
//fun PaymentDialogErrorPreview() {
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = false,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = false,
//                planOptions = emptyList(),
//                selectedPlan = null,
//                planFeatures = emptyList(),
//                errorMessage = UiText.StringText("There was a problem loading subscriptions"),
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

//@Preview(name = "No Plans Available", showBackground = true)
//@Preview(name = "Dark - No Plans Available", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
//@Composable
//fun PaymentDialogNoPlansPreview() {
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = false,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = false,
//                planOptions = emptyList(),
//                selectedPlan = null,
//                planFeatures = emptyList(),
//                errorMessage = null,
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

//@Preview(name = "Plans Available", showBackground = true)
//@Preview(name = "Dark - Plans Available", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
//@Composable
//fun PaymentDialogPlansAvailablePreview() {
//    val mockPlans = listOf(
//        JsPlanInfo(
//            id = "lumo-plus-monthly",
//            name = "Lumo Plus",
//            duration = UiText.StringText("1 month"),
//            cycle = 1,
//            description = "Monthly subscription",
//            productId = "lumo_plus_monthly",
//            customerId = "customer123",
//            pricePerMonth = "$9.99",
//            totalPrice = "$9.99",
//            savings = null,
//            offerToken = "token123"
//        ),
//        JsPlanInfo(
//            id = "lumo-plus-annual",
//            name = "Lumo Plus",
//            duration = UiText.StringText("12 months"),
//            cycle = 12,
//            description = "Annual subscription",
//            productId = "lumo_plus_annual",
//            customerId = "customer123",
//            pricePerMonth = "$7.99",
//            totalPrice = "$95.99",
//            savings = "Save 20%",
//            offerToken = "token456"
//        )
//    )
//
//    val mockFeatures = listOf(
//        PlanFeature(
//            name = "AI Responses",
//            freeText = "Limited",
//            paidText = "Unlimited",
//            iconName = "ai-responses"
//        ),
//        PlanFeature(
//            name = "Priority Support",
//            freeText = "Standard",
//            paidText = "Priority",
//            iconName = "priority-support"
//        ),
//        PlanFeature(
//            name = "Advanced Features",
//            freeText = "Basic",
//            paidText = "Advanced",
//            iconName = "advanced-features"
//        )
//    )
//
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = false,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = false,
//                planOptions = mockPlans,
//                selectedPlan = mockPlans.first(),
//                planFeatures = mockFeatures,
//                errorMessage = null,
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

//@Preview(name = "Plans with Error", showBackground = true)
//@Preview(name = "Dark - Plans with Error", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
//@Composable
//fun PaymentDialogPlansWithErrorPreview() {
//    val mockPlans = listOf(
//        JsPlanInfo(
//            id = "lumo-plus-monthly",
//            name = "Lumo Plus",
//            duration = UiText.StringText("1 month"),
//            cycle = 1,
//            description = "Monthly subscription",
//            productId = "lumo_plus_monthly",
//            customerId = "customer123",
//            pricePerMonth = "$9.99",
//            totalPrice = "$9.99",
//            savings = null,
//            offerToken = "token123"
//        )
//    )
//    LumoTheme {
//        PlanSelectionScreen(
//            uiState = SubscriptionViewModel.UiState(
//                isLoadingSubscriptions = false,
//                subscriptions = emptyList(),
//                hasValidSubscription = false,
//                isLoadingPlans = false,
//                planOptions = mockPlans,
//                selectedPlan = null,
//                planFeatures = emptyList(),
//                errorMessage = UiText.StringText("Failed to load plans: Network error"),
//                paymentProcessingState = PaymentState.Idle,
//                googleProductDetails = emptyList(),
//                theme = null,
//                paymentEvent = PaymentEvent.Default,
//            ),
//            onDismiss = {},
//            onPlanSelected = {},
//            onPurchaseClicked = {},
//            onClearError = {},
//        )
//    }
//}

// Payment Processing State Previews
@Preview(name = "Payment Processing - Loading", showBackground = true)
@Preview(
    name = "Dark - Payment Processing - Loading",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentProcessingLoadingPreview() {
    LumoTheme {
        PaymentProcessingScreen(
            state = PaymentState.Idle,
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}

@Preview(name = "Payment Processing - Verifying", showBackground = true)
@Preview(
    name = "Dark - Payment Processing - Verifying",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentProcessingVerifyingPreview() {
    LumoTheme {
        PaymentProcessingScreen(
            state = PaymentState.Verifying,
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}

@Preview(name = "Payment Processing - Error", showBackground = true)
@Preview(
    name = "Dark - Payment Processing - Error",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentProcessingErrorPreview() {
    LumoTheme {
        PaymentProcessingScreen(
            state = PaymentState.Error(
                UiText.StringText(
                    "Payment failed. Please check your payment method and try again."
                )
            ),
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}

@Preview(name = "Payment Processing - Network Error", showBackground = true)
@Preview(name = "Dark - Payment Processing - Network Error", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PaymentProcessingNetworkErrorPreview() {
    LumoTheme {
        PaymentProcessingScreen(
            state = PaymentState.Error(
                UiText.StringText(
                    "Network connection failed. Please check your internet connection."
                )
            ),
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}

@Preview(name = "Payment Processing - Success", showBackground = true)
@Preview(
    name = "Dark - Payment Processing - Success",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentProcessingSuccessPreview() {
    LumoTheme {
        PaymentProcessingScreen(
            state = PaymentState.Success,
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}