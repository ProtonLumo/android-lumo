package me.proton.android.lumo.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel

private const val TAG = "PaymentDialog"

@Composable
fun PaymentScreen(
    isReady: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mainActivity = context as MainActivity
    val viewModel: SubscriptionViewModel = hiltViewModel()
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(isReady) {
        if (isReady) {
            viewModel.refreshSubscriptionStatus()
        }
    }

    // Check for subscription sync mismatch after BOTH loading operations are complete
    LaunchedEffect(
        uiState.isLoadingSubscriptions,
        uiState.isRefreshingPurchases,
        uiState.hasValidSubscription
    ) {
        if (!uiState.isLoadingSubscriptions &&
            !uiState.isRefreshingPurchases &&
            !uiState.hasValidSubscription
        ) {
            Log.d(
                TAG,
                "Both loading operations complete, checking for subscription sync mismatch..."
            )
            // Check if there's a mismatch that needs recovery
            if (viewModel.checkSubscriptionSyncMismatch()) {
                // Trigger the recovery flow
                viewModel.triggerSubscriptionRecovery()
            }
        }
    }

    // If payment is being processed, show that screen instead
    uiState.paymentProcessingState?.let {
        PaymentProcessingDialog(
            state = it,
            onRetry = {
                viewModel.retryPaymentVerification()
            },
            onClose = {
                onDismiss()
                viewModel.resetPaymentState()
            }
        )

        return
    }

    // Check if user already has a valid subscription
    if (uiState.hasValidSubscription) {
        SubscriptionOverviewDialog(
            subscriptions = uiState.subscriptions,
            googleProductDetails = uiState.googleProductDetails,
            getSubscriptionPaymentStatus = {
                viewModel.getGooglePlaySubscriptionStatus()
            },
            onClose = onDismiss
        )
        return
    }


    PlanSelectionScreen(
        uiState = uiState,
        onDismiss = onDismiss,
        onPlanSelected = { viewModel.selectPlan(it) },
        onPurchaseClicked = { planToPurchase ->
            viewModel.launchBillingFlowForProduct(
                productId = planToPurchase.productId,
                offerToken = planToPurchase.offerToken,
                customerID = planToPurchase.customerId,
                getBillingResult = { billingClient, billingParams ->
                    billingClient?.launchBillingFlow(mainActivity, billingParams)
                },
            )
        },
        onClearError = { viewModel.clearError() }
    )
}

// Preview Functions for Different Dialog States
@Preview(name = "Loading Subscriptions", showBackground = true)
@Preview(
    name = "Dark - Loading Subscriptions",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL
)
@Composable
fun PaymentDialogLoadingSubscriptionsPreview() {
    LumoTheme {
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = true,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = false,
                planOptions = emptyList(),
                selectedPlan = null,
                planFeatures = emptyList(),
                errorMessage = null,
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

@Preview(name = "Loading Plans", showBackground = true)
@Preview(name = "Dark - Loading Plans", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogLoadingPlansPreview() {
    LumoTheme {
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = false,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = true,
                planOptions = emptyList(),
                selectedPlan = null,
                planFeatures = emptyList(),
                errorMessage = null,
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

@Preview(name = "Error State", showBackground = true)
@Preview(name = "Dark - Error State", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogErrorPreview() {
    LumoTheme {
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = false,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = false,
                planOptions = emptyList(),
                selectedPlan = null,
                planFeatures = emptyList(),
                errorMessage = UiText.StringText("There was a problem loading subscriptions"),
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

@Preview(name = "No Plans Available", showBackground = true)
@Preview(name = "Dark - No Plans Available", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogNoPlansPreview() {
    LumoTheme {
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = false,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = false,
                planOptions = emptyList(),
                selectedPlan = null,
                planFeatures = emptyList(),
                errorMessage = null,
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

@Preview(name = "Plans Available", showBackground = true)
@Preview(name = "Dark - Plans Available", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogPlansAvailablePreview() {
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
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = false,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = false,
                planOptions = mockPlans,
                selectedPlan = mockPlans.first(),
                planFeatures = mockFeatures,
                errorMessage = null,
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

@Preview(name = "Plans with Error", showBackground = true)
@Preview(name = "Dark - Plans with Error", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogPlansWithErrorPreview() {
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
        )
    )
    LumoTheme {
        PlanSelectionScreen(
            uiState = SubscriptionViewModel.UiState(
                isLoadingSubscriptions = false,
                subscriptions = emptyList(),
                hasValidSubscription = false,
                isLoadingPlans = false,
                planOptions = mockPlans,
                selectedPlan = null,
                planFeatures = emptyList(),
                errorMessage = UiText.StringText("Failed to load plans: Network error"),
                paymentProcessingState = null,
                isRefreshingPurchases = false,
                googleProductDetails = emptyList(),
                theme = null,
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {},
            onClearError = {},
        )
    }
}

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
            state = PaymentProcessingState.Loading,
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
            state = PaymentProcessingState.Verifying,
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
            state = PaymentProcessingState.Error(
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
            state = PaymentProcessingState.NetworkError(
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
            state = PaymentProcessingState.Success,
            onRetry = { /* Preview - no action */ },
            onClose = { /* Preview - no action */ }
        )
    }
}