package me.proton.android.lumo.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.viewmodels.SubscriptionViewModel
import me.proton.android.lumo.viewmodels.ViewModelFactory

private const val TAG = "PaymentDialog"

// Preview Functions for Different Dialog States

@PreviewLightDark
@Preview(name = "Loading Subscriptions", showBackground = true)
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

@PreviewLightDark
@Preview(name = "Loading Plans", showBackground = true)
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

@PreviewLightDark
@Preview(name = "Error State", showBackground = true)
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

@PreviewLightDark
@Preview(name = "No Plans Available", showBackground = true)
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

@PreviewLightDark
@Preview(name = "Plans Available", showBackground = true)
@Composable
fun PaymentDialogPlansAvailablePreview() {
    val mockPlans = listOf(
        JsPlanInfo(
            id = "lumo-plus-monthly",
            name = "Lumo Plus",
            duration = "1 month",
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
            duration = "12 months",
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

@PreviewLightDark
@Preview(name = "Plans with Error", showBackground = true)
@Composable
fun PaymentDialogPlansWithErrorPreview() {
    val mockPlans = listOf(
        JsPlanInfo(
            id = "lumo-plus-monthly",
            name = "Lumo Plus",
            duration = "1 month",
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
@PreviewLightDark
@Preview(name = "Payment Processing - Loading", showBackground = true)
@Composable
fun PaymentProcessingLoadingPreview() {
    PaymentProcessingScreen(
        state = PaymentProcessingState.Loading,
        onRetry = { /* Preview - no action */ },
        onClose = { /* Preview - no action */ }
    )
}

@PreviewLightDark
@Preview(name = "Payment Processing - Verifying", showBackground = true)
@Composable
fun PaymentProcessingVerifyingPreview() {
    PaymentProcessingScreen(
        state = PaymentProcessingState.Verifying,
        onRetry = { /* Preview - no action */ },
        onClose = { /* Preview - no action */ }
    )
}

@PreviewLightDark
@Preview(name = "Payment Processing - Error", showBackground = true)
@Composable
fun PaymentProcessingErrorPreview() {
    PaymentProcessingScreen(
        state = PaymentProcessingState.Error("Payment failed. Please check your payment method and try again."),
        onRetry = { /* Preview - no action */ },
        onClose = { /* Preview - no action */ }
    )
}

@Preview(name = "Payment Processing - Network Error", showBackground = true)
@Composable
fun PaymentProcessingNetworkErrorPreview() {
    PaymentProcessingScreen(
        state = PaymentProcessingState.NetworkError("Network connection failed. Please check your internet connection."),
        onRetry = { /* Preview - no action */ },
        onClose = { /* Preview - no action */ }
    )
}

@PreviewLightDark
@Preview(name = "Payment Processing - Success", showBackground = true)
@Composable
fun PaymentProcessingSuccessPreview() {
    PaymentProcessingScreen(
        state = PaymentProcessingState.Success,
        onRetry = { /* Preview - no action */ },
        onClose = { /* Preview - no action */ }
    )
}

@Composable
private fun PaymentDialogContentPreview(
    isLoadingSubscriptions: Boolean = false,
    isLoadingPlans: Boolean = false,
    errorMessage: String? = null,
    planOptions: List<JsPlanInfo> = emptyList(),
    planFeatures: List<PlanFeature> = emptyList(),
    selectedPlan: JsPlanInfo? = null
) {
    MaterialTheme {
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

@SuppressLint("DefaultLocale")
@Composable
fun PaymentDialog(
    visible: Boolean,
    billingManager: BillingManager,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mainActivity = context as? MainActivity

    // Create ViewModel using the modern factory approach
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = ViewModelFactory(mainActivity ?: return)
    )

    // Collect state from ViewModel
    val isLoadingSubscriptions by subscriptionViewModel.isLoadingSubscriptions.collectAsStateWithLifecycle()
    val isLoadingPlans by subscriptionViewModel.isLoadingPlans.collectAsStateWithLifecycle()
    val subscriptions by subscriptionViewModel.subscriptions.collectAsStateWithLifecycle()
    val hasValidSubscription by subscriptionViewModel.hasValidSubscription.collectAsStateWithLifecycle()
    val planOptions by subscriptionViewModel.planOptions.collectAsStateWithLifecycle()
    val selectedPlan by subscriptionViewModel.selectedPlan.collectAsStateWithLifecycle()
    val planFeatures by subscriptionViewModel.planFeatures.collectAsStateWithLifecycle()
    val errorMessage by subscriptionViewModel.errorMessage.collectAsStateWithLifecycle()

    // Get payment processing state from BillingManager
    val paymentProcessingState by billingManager.paymentProcessingState.collectAsStateWithLifecycle()
    val isRefreshingPurchases by billingManager.isRefreshingPurchases.collectAsStateWithLifecycle()

    // Load subscriptions whenever dialog opens
    LaunchedEffect(visible) {
        if (visible) {
            // Refresh status from Google Play and load subscriptions
            subscriptionViewModel.refreshSubscriptionStatus()
        }
    }

    // Check for subscription sync mismatch after BOTH loading operations are complete
    LaunchedEffect(isLoadingSubscriptions, isRefreshingPurchases, hasValidSubscription) {
        if (!isLoadingSubscriptions && !isRefreshingPurchases && !hasValidSubscription) {
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

    if (visible) {
        // If payment is being processed, show that screen instead
        if (paymentProcessingState != null) {
            Dialog(
                onDismissRequest = {
                    // Don't allow dismissing during loading or verification
                    if (paymentProcessingState !is PaymentProcessingState.Loading &&
                        paymentProcessingState !is PaymentProcessingState.Verifying
                    ) {
                        onDismiss()
                        billingManager.resetPaymentState()
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f) // Limit height to allow scrolling
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PaymentProcessingScreen(
                        state = paymentProcessingState!!,
                        onRetry = { billingManager.retryPaymentVerification() },
                        onClose = {
                            onDismiss()
                            billingManager.resetPaymentState()
                        }
                    )
                }
            }

            return
        }

        // Check if user already has a valid subscription
        if (hasValidSubscription) {
            Dialog(
                onDismissRequest = { onDismiss() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f) // Limit height to allow scrolling
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SubscriptionOverviewSection(
                        billingManager = billingManager,
                        subscriptions = subscriptions,
                        onClose = { onDismiss() }
                    )
                }
            }

            return
        }

        // --- Dialog UI for plan selection --- 
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f) // Limit height to allow scrolling
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close Button
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Image(
                        painter = painterResource(id = R.drawable.lumo_cat_on_laptop),
                        contentDescription = "Lumo Plus",
                        modifier = Modifier.height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    // --- Dynamic Content based on loading/error/success ---
                    when {
                        isLoadingSubscriptions -> {
                            // Show loading UI while checking subscriptions
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.payment_checking),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        isLoadingPlans -> {
                            // Show loading UI
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.payment_loading_plans),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        errorMessage != null -> {
                            // Show error state
                            Text(
                                text = errorMessage ?: stringResource(id = R.string.error_generic),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }

                        planOptions.isNotEmpty() -> {
                            // Features comparison table
                            if (planFeatures.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Empty space for feature name
                                    Spacer(modifier = Modifier.weight(1f))

                                    // Free column header
                                    Text(
                                        "Free",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center
                                    )

                                    // Plus column header
                                    Text(
                                        "Plus",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Display features
                                planFeatures.take(5).forEach { feature ->
                                    FeatureComparisonItem(feature)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Plan Selection Section
                            planOptions.forEach { plan ->
                                // Skip plans with no pricing info
                                if (plan.totalPrice.isNotEmpty()) {
                                    PlanSelectItem(
                                        plan = plan,
                                        isSelected = selectedPlan?.id == plan.id,
                                        onSelected = { subscriptionViewModel.selectPlan(plan) }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            // Display error message if any
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
                                color = Color.DarkGray,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Continue Button (Purchase)
                            Button(
                                onClick = {
                                    selectedPlan?.let { planToPurchase ->
                                        if (planToPurchase.offerToken == null && planToPurchase.totalPrice.isEmpty()) {
                                            subscriptionViewModel.clearError()
                                            return@Button
                                        }
                                        Log.d(
                                            TAG,
                                            "Purchase button clicked for plan: ${planToPurchase.id}, ProductID: ${planToPurchase.productId}, OfferToken: ${planToPurchase.offerToken}"
                                        )
                                        billingManager.launchBillingFlowForProduct(
                                            planToPurchase.productId,
                                            planToPurchase.offerToken,
                                            planToPurchase.customerId
                                        )
                                    } ?: run {
                                        Log.w(TAG, "Purchase clicked but no plan selected.")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                enabled = selectedPlan != null && selectedPlan?.totalPrice?.isNotEmpty() == true
                            ) {
                                Text(
                                    stringResource(id = R.string.subscription_buy_lumo),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { onDismiss() },
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
                            // No plans available
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
} 