package me.proton.android.lumo.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.android.lumo.money_machine.PaymentState
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun PaymentProcessingDialog(
    state: PaymentState,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LumoTheme.colors.backgroundNorm
    ) {
        PaymentProcessingScreen(
            state = state,
            onRetry = onRetry,
            onClose = onClose
        )
    }
}

@Preview(name = "Payment Processing - Verifying", showBackground = true)
@Composable
fun PaymentProcessingDialogVerifyingPreview() {
    LumoTheme {
        PaymentProcessingDialog(
            state = PaymentState.Verifying,
            onRetry = {},
            onClose = {}
        )
    }
}

@Preview(name = "Payment Processing - Error", showBackground = true)
@Composable
fun PaymentProcessingDialogErrorPreview() {
    LumoTheme {
        PaymentProcessingDialog(
            state = PaymentState.Error(
                UiText.StringText("Payment failed. Please check your payment method and try again.")
            ),
            onRetry = {},
            onClose = {}
        )
    }
}

@Preview(name = "Payment Processing - Success", showBackground = true)
@Composable
fun PaymentProcessingDialogSuccessPreview() {
    LumoTheme {
        PaymentProcessingDialog(
            state = PaymentState.Success,
            onRetry = {},
            onClose = {}
        )
    }
}
