package me.proton.android.lumo.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.text.asString
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel.UiState

private const val TAG = "PlanSelectionDialog"

@Composable
fun PlanSelectionDialog(
    uiState: UiState,
    paymentEvent: PaymentEvent,
    onDismiss: () -> Unit,
    onPlanSelected: (JsPlanInfo) -> Unit,
    onPurchaseClicked: (JsPlanInfo) -> Unit,
    onClearError: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val imageRes = when (paymentEvent) {
                PaymentEvent.Default -> R.drawable.lumo_cat_on_laptop
                PaymentEvent.BlackFriday -> R.drawable.lumo_black_friday
            }
            Image(
                painter = painterResource(id = imageRes),
                contentScale = ContentScale.Inside,
                contentDescription = "Lumo Plus",
                modifier = Modifier.fillMaxWidth()
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
                uiState.isLoadingSubscriptions -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.payment_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                uiState.isLoadingPlans -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.payment_loading_plans),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage.asString(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                uiState.planOptions.isNotEmpty() -> {
                    // Features comparison table
                    if (uiState.planFeatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.planFeatures.take(5).forEach { feature ->
                            FeatureComparisonItem(feature)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Plan Selection Section
                    uiState.planOptions.forEach { plan ->
                        if (plan.totalPrice.isNotEmpty()) {
                            PlanSelectItem(
                                plan = plan,
                                isSelected = uiState.selectedPlan?.id == plan.id,
                                onSelected = { onPlanSelected(plan) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Display error message if any
                    uiState.errorMessage?.let { errorMsg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            errorMsg.asString(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        stringResource(id = R.string.subscription_renewal),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Continue Button (Purchase)
                    Button(
                        onClick = {
                            uiState.selectedPlan?.let { planToPurchase ->
                                if (planToPurchase.offerToken == null && planToPurchase.totalPrice.isEmpty()) {
                                    onClearError()
                                    return@Button
                                }
                                Log.d(
                                    TAG,
                                    "Purchase button clicked for plan: ${planToPurchase.id}, ProductID: ${planToPurchase.productId}, OfferToken: ${planToPurchase.offerToken}"
                                )
                                onPurchaseClicked(planToPurchase)
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
                        enabled = uiState.selectedPlan != null && uiState.selectedPlan.totalPrice.isNotEmpty() == true
                    ) {
                        Text(
                            stringResource(id = R.string.subscription_buy_lumo),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
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

@Preview(name = "Plan Selection - Loading Plans", showBackground = true)
@Composable
fun PlanSelectionDialogLoadingPreview() {
    LumoTheme {
        PlanSelectionDialog(
            uiState = UiState(
                isLoadingPlans = true
            ),
            paymentEvent = PaymentEvent.Default,
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {}
        ) {}
    }
}

@Preview(name = "Plan Selection - Plans Available", showBackground = true)
@Composable
fun PlanSelectionDialogPlansPreview() {
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
        )
    )

    LumoTheme {
        PlanSelectionDialog(
            uiState = UiState(
                planOptions = mockPlans,
                planFeatures = mockFeatures,
                selectedPlan = mockPlans.first()
            ),
            paymentEvent = PaymentEvent.Default,
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {}
        ) {}
    }
}
