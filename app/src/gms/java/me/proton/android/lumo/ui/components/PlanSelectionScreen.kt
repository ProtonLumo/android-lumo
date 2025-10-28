package me.proton.android.lumo.ui.components

import android.util.Log
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import me.proton.android.lumo.ui.theme.AppStyle.Companion.isDarkTheme
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.viewmodels.SubscriptionViewModel.UiState

private const val TAG = "PlanSelectionDialog"

@Composable
fun PlanSelectionScreen(
    uiState: UiState,
    onDismiss: () -> Unit,
    onPlanSelected: (JsPlanInfo) -> Unit,
    onPurchaseClicked: (JsPlanInfo) -> Unit,
    onClearError: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(LumoTheme.colors.backgroundNorm)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(
                paymentEvent = uiState.paymentEvent,
                isDarkTheme = uiState.theme.isDarkTheme(),
            )

            // --- Dynamic Content based on loading/error/success ---
            when {
                uiState.isLoadingSubscriptions -> {
                    CircularProgressIndicator(color = LumoTheme.colors.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.payment_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LumoTheme.colors.textWeak
                    )
                }

                uiState.isLoadingPlans -> {
                    CircularProgressIndicator(color = LumoTheme.colors.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.payment_loading_plans),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LumoTheme.colors.textWeak
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage.asString(),
                        color = LumoTheme.colors.signalDanger,
                        textAlign = TextAlign.Center
                    )
                }

                uiState.planOptions.isNotEmpty() -> {
                    // Features comparison table
                    if (uiState.planFeatures.isNotEmpty()) {
                        uiState.planFeatures.forEach { feature ->
                            FeatureComparisonItem(
                                feature = feature,
                            )
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }

                    // Plan Selection Section
                    uiState.planOptions.forEach { plan ->
                        if (plan.totalPrice.isNotEmpty()) {
                            PlanSelectItem(
                                plan = plan,
                                isDarkTheme = uiState.theme.isDarkTheme(),
                                paymentEvent = uiState.paymentEvent,
                                isSelected = uiState.selectedPlan?.id == plan.id,
                                onSelected = { onPlanSelected(plan) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Display error message if any
                    uiState.errorMessage?.let { errorMsg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            errorMsg.asString(),
                            color = LumoTheme.colors.signalDanger,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Footer(
                        uiState = uiState,
                        onClearError = onClearError,
                        onPurchaseClicked = onPurchaseClicked,
                        onDismiss = onDismiss,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(id = R.string.payment_no_plans_available),
                        color = LumoTheme.colors.textWeak,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

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
}

@Composable
private fun Footer(
    uiState: UiState,
    onClearError: () -> Unit,
    onPurchaseClicked: (JsPlanInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Text(
            stringResource(id = R.string.subscription_renewal),
            style = MaterialTheme.typography.bodySmall,
            color = LumoTheme.colors.textWeak,
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
                containerColor = LumoTheme.colors.interactionSecondary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.selectedPlan != null && uiState.selectedPlan.totalPrice.isNotEmpty()
        ) {
            Text(
                text = stringResource(id = R.string.subscription_buy_lumo),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = LumoTheme.colors.textWeak
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = stringResource(R.string.subscription_use_lumo_free),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Header(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
) {
    HeaderImage(
        paymentEvent = paymentEvent,
        isDarkTheme = isDarkTheme,
    )
    Spacer(modifier = Modifier.height(16.dp))
    val paymentTitle = when (paymentEvent) {
        PaymentEvent.Default -> R.string.payment_title
        PaymentEvent.BlackFriday -> R.string.payment_black_friday_title
    }
    Text(
        text = stringResource(id = paymentTitle),
        style = MaterialTheme.typography.titleSmall,
        color = LumoTheme.colors.textNorm,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    val paymentSubtitle = when (paymentEvent) {
        PaymentEvent.Default -> R.string.payment_subtitle
        PaymentEvent.BlackFriday -> R.string.payment_black_friday_subtitle
    }
    Text(
        text = stringResource(id = paymentSubtitle),
        style = MaterialTheme.typography.bodySmall,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    )
}

@Composable
private fun HeaderImage(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
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
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when (paymentEvent) {
                    PaymentEvent.Default ->
                        Modifier.windowInsetsPadding(WindowInsets.systemBars)

                    PaymentEvent.BlackFriday ->
                        Modifier
                }
            )
    )
}

@Preview(name = "Plan Selection - Loading Plans", showBackground = true)
@Composable
fun PlanSelectionDialogLoadingPreview() {
    LumoTheme {
        PlanSelectionScreen(
            uiState = UiState(
                isLoadingPlans = true,
                paymentEvent = PaymentEvent.Default,
            ),
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
        PlanSelectionScreen(
            uiState = UiState(
                planOptions = mockPlans,
                planFeatures = mockFeatures,
                selectedPlan = mockPlans.first(),
                paymentEvent = PaymentEvent.Default,
            ),
            onDismiss = {},
            onPlanSelected = {},
            onPurchaseClicked = {}
        ) {}
    }
}
