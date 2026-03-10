package me.proton.android.lumo.ui.components.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.PlansState
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.components.FeatureComparisonItem
import me.proton.android.lumo.ui.components.payment.Loader
import me.proton.android.lumo.ui.theme.LumoTheme

@Suppress("LongParameterList")
@Composable
fun PlanSelectionScreen(
    plansState: PlansState,
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectPlan: (Int) -> Unit,
    onClickPurchase: (JsPlanInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (plansState) {
            is PlansState.Loading -> Loader(
                messageRes = R.string.payment_loading_plans,
                modifier = Modifier.padding(24.dp)
            )

            is PlansState.Success -> Content(
                planOptions = plansState.planOptions,
                planFeatures = plansState.planFeatures,
                selectedPlanIndex = plansState.selectedPlanIndex,
                paymentEvent = paymentEvent,
                isDarkTheme = isDarkTheme,
                onDismiss = onDismiss,
                onSelectPlan = onSelectPlan,
                onClickPurchase = onClickPurchase,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun Content(
    planOptions: List<JsPlanInfo>,
    planFeatures: List<PlanFeature>,
    selectedPlanIndex: Int,
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectPlan: (Int) -> Unit,
    onClickPurchase: (JsPlanInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (planOptions.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Header(
                paymentEvent = paymentEvent,
            )
            if (planFeatures.isNotEmpty()) {
                planFeatures.forEach { feature ->
                    FeatureComparisonItem(
                        feature = feature,
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            planOptions.forEachIndexed { index, plan ->
                if (plan.totalPrice.isNotEmpty()) {
                    PlanSelectItem(
                        plan = plan,
                        isDarkTheme = isDarkTheme,
                        paymentEvent = paymentEvent,
                        isSelected = selectedPlanIndex == index,
                        onSelect = { onSelectPlan(index) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Footer(
                paymentEvent = paymentEvent,
                onClickPurchase = { onClickPurchase(planOptions[selectedPlanIndex]) },
                onDismiss = onDismiss,
            )
        }
    } else {
        Text(
            text = stringResource(id = R.string.payment_no_plans_available),
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun Footer(
    paymentEvent: PaymentEvent,
    onClickPurchase: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Text(
            stringResource(id = R.string.subscription_renewal),
            style = MaterialTheme.typography.bodySmall,
            color = LumoTheme.colors.textWeak,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Continue Button (Purchase)
        Button(
            onClick = { onClickPurchase() },
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFC266),
                            Color(0xFFFF9800),
                        )
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = true
        ) {
            val buttonText = when (paymentEvent) {
                PaymentEvent.Default -> R.string.subscription_buy_lumo
                PaymentEvent.BlackFriday -> R.string.subscription_claim_black_friday_deal
                PaymentEvent.SpringSale -> R.string.subscription_claim_spring_sale_deal
            }

            Text(
                text = stringResource(id = buttonText),
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
private fun ColumnScope.Header(
    paymentEvent: PaymentEvent,
) {
    Spacer(modifier = Modifier.height(16.dp))
    val paymentTitle = when (paymentEvent) {
        PaymentEvent.Default -> R.string.payment_title
        PaymentEvent.BlackFriday -> R.string.payment_black_friday_title
        PaymentEvent.SpringSale -> R.string.payment_spring_sale_title
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
        PaymentEvent.SpringSale -> R.string.payment_spring_sale_subtitle
    }
    Text(
        text = stringResource(id = paymentSubtitle),
        style = MaterialTheme.typography.bodySmall,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    )
}
