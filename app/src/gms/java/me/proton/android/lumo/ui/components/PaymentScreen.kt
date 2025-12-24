package me.proton.android.lumo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.ui.components.payment.PaymentContent
import me.proton.android.lumo.viewmodels.SubscriptionViewModel

private const val TAG = "PaymentDialog"

@Composable
fun PaymentScreen(
    paymentEvent: PaymentEvent,
    onDismiss: () -> Unit,
) {
    val viewModel: SubscriptionViewModel = hiltViewModel()
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    PaymentContent(
        paymentEvent,
        uiState,
        onDismiss,
        retryPayment = { viewModel.retryPaymentVerification() },
        launchPaymentFlow = { planToPurchase ->
            viewModel.launchBillingFlow(
                productId = planToPurchase.productId,
                offerToken = planToPurchase.offerToken,
                customerId = planToPurchase.customerId,
            )
        },
        selectPlan = { viewModel.selectPlan(it) }
    )
}
