package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.runtime.Composable
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.ui.components.PurchaseLinkDialog

class BillingDelegateImpl() : BillingDelegate {
    override fun initialise(activity: MainActivity) {
    }

    override fun handleJavaScriptResult(
        transactionId: String,
        resultJson: String
    ): Boolean = false

    @Composable
    override fun ShowPaymentOrError(
        uiState: MainUiState,
        isDarkMode: Boolean,
        webView: WebView,
        onDismiss: () -> Unit,
        onOpenUrl: (String) -> Unit,
    ) {
        if (uiState.showPaymentDialog) {
            PurchaseLinkDialog(
                onDismissRequest = onDismiss,
                onOpenUrl = onOpenUrl
            )
        }
    }
}