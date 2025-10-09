package me.proton.android.lumo.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun PaymentProcessingDialog(
    state: PaymentProcessingState,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PaymentProcessingScreen(
            state = state,
            onRetry = onRetry,
            onClose = onClose
        )
    }
}

@Preview(name = "Payment Processing - Loading", showBackground = true)
@Composable
fun PaymentProcessingDialogLoadingPreview() {
    LumoTheme {
        PaymentProcessingDialog(
            state = PaymentProcessingState.Loading,
            onRetry = {},
            onClose = {}
        )
    }
}

@Preview(name = "Payment Processing - Verifying", showBackground = true)
@Composable
fun PaymentProcessingDialogVerifyingPreview() {
    LumoTheme {
        PaymentProcessingDialog(
            state = PaymentProcessingState.Verifying,
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
            state = PaymentProcessingState.Error(
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
            state = PaymentProcessingState.Success,
            onRetry = {},
            onClose = {}
        )
    }
}
