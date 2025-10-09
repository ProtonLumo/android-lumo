package me.proton.android.lumo.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel
import me.proton.android.lumo.viewmodels.SubscriptionViewModelFactory

private const val TAG = "PaymentDialog"

@SuppressLint("DefaultLocale")
@Composable
fun PaymentDialog(
    isReady: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mainActivity = context as MainActivity
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory()
    )
    val uiState by subscriptionViewModel.uiStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(isReady) {
        if (isReady) {
            subscriptionViewModel.refreshSubscriptionStatus()
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
            if (subscriptionViewModel.checkSubscriptionSyncMismatch()) {
                // Trigger the recovery flow
                subscriptionViewModel.triggerSubscriptionRecovery()
            }
        }
    }

    // If payment is being processed, show that screen instead
    uiState.paymentProcessingState?.let {
        PaymentProcessingDialog(
            state = it,
            onRetry = {
                subscriptionViewModel.retryPaymentVerification()
            },
            onClose = {
                onDismiss() // TODO: still needed?
                subscriptionViewModel.resetPaymentState()
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
                subscriptionViewModel.getGooglePlaySubscriptionStatus()
            },
            onClose = onDismiss
        )
        return
    }


    // Dialog UI for plan selection
    PlanSelectionDialog(
        uiState = uiState,
        onDismiss = onDismiss,
        onPlanSelected = { subscriptionViewModel.selectPlan(it) },
        onPurchaseClicked = { planToPurchase ->
            subscriptionViewModel.launchBillingFlowForProduct(
                productId = planToPurchase.productId,
                offerToken = planToPurchase.offerToken,
                customerID = planToPurchase.customerId,
                getBillingResult = { billingClient, billingParams ->
                    billingClient?.launchBillingFlow(mainActivity, billingParams)
                },
            )
        },
        onClearError = { subscriptionViewModel.clearError() }
    )
}


// Preview Functions for Different Dialog States
@Preview(name = "Loading Subscriptions", showBackground = true)
@Preview(name = "Dark - Loading Subscriptions", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogLoadingSubscriptionsPreview() {
    PaymentDialogContentPreview(
        isLoadingSubscriptions = true,
        isLoadingPlans = false,
        errorMessage = null,
        planOptions = emptyList(),
        planFeatures = emptyList()
    )
}

@Preview(name = "Loading Plans", showBackground = true)
@Preview(name = "Dark - Loading Plans", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogLoadingPlansPreview() {
    PaymentDialogContentPreview(
        isLoadingSubscriptions = false,
        isLoadingPlans = true,
        errorMessage = null,
        planOptions = emptyList(),
        planFeatures = emptyList()
    )
}

@Preview(name = "Error State", showBackground = true)
@Preview(name = "Dark - Error State", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogErrorPreview() {
    PaymentDialogContentPreview(
        isLoadingSubscriptions = false,
        isLoadingPlans = false,
        errorMessage = "There was a problem loading subscriptions",
        planOptions = emptyList(),
        planFeatures = emptyList()
    )
}

@Preview(name = "No Plans Available", showBackground = true)
@Preview(name = "Dark - No Plans Available", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun PaymentDialogNoPlansPreview() {
    PaymentDialogContentPreview(
        isLoadingSubscriptions = false,
        isLoadingPlans = false,
        errorMessage = null,
        planOptions = emptyList(),
        planFeatures = emptyList()
    )
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

    PaymentDialogContentPreview(
        isLoadingSubscriptions = false,
        isLoadingPlans = false,
        errorMessage = null,
        planOptions = mockPlans,
        planFeatures = mockFeatures,
        selectedPlan = mockPlans.first()
    )
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

    PaymentDialogContentPreview(
        isLoadingSubscriptions = false,
        isLoadingPlans = false,
        errorMessage = "Failed to load plans: Network error",
        planOptions = mockPlans,
        planFeatures = emptyList(),
        selectedPlan = mockPlans.first()
    )
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

@PreviewLightDark
@Composable
private fun PaymentDialogContentPreview(
    isLoadingSubscriptions: Boolean = false,
    isLoadingPlans: Boolean = false,
    errorMessage: String? = null,
    planOptions: List<JsPlanInfo> = emptyList(),
    planFeatures: List<PlanFeature> = emptyList(),
    selectedPlan: JsPlanInfo? = null
) {
    LumoTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // Limit height to allow scrolling
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                IconButton(
                    onClick = { /* Preview - no action */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.lumo_cat_on_laptop),
                    contentDescription = "Lumo Plus",
                    modifier = Modifier.height(75.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.payment_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.payment_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                // Dynamic Content based on loading/error/success
                when {
                    isLoadingSubscriptions -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.payment_checking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    isLoadingPlans -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.payment_loading_plans),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    planOptions.isNotEmpty() -> {
                        // Features comparison table
                        if (planFeatures.isNotEmpty()) {
                            planFeatures.take(4).forEach { feature ->
                                FeatureComparisonItem(feature)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Plan Selection Section
                        planOptions.forEach { plan ->
                            if (plan.totalPrice.isNotEmpty()) {
                                PlanSelectItem(
                                    plan = plan,
                                    isSelected = selectedPlan?.id == plan.id,
                                    onSelected = { /* Preview - no action */ }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Display error message if any (when plans are loaded but there's still an error)
                        errorMessage?.let { errorMsg ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Text(
                            stringResource(id = R.string.subscription_renewal),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Continue Button (Purchase)
                        Button(
                            onClick = { /* Preview - no action */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = selectedPlan != null && selectedPlan.totalPrice.isNotEmpty()
                        ) {
                            Text(
                                stringResource(id = R.string.subscription_buy_lumo),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        TextButton(
                            onClick = { /* Preview - no action */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                "Not now",
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = stringResource(id = R.string.payment_no_plans_available),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
