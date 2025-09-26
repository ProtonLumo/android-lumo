package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.runtime.Composable
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.ui.components.SimpleAlertDialog

class BillingDelegateImpl() : BillingDelegate {
    override fun initialise(activity: MainActivity) {
    }

    override fun getPlansFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)?
    ) {
    }

    override fun getSubscriptionsFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)?
    ) {
    }

    override fun handleJavaScriptResult(
        transactionId: String,
        resultJson: String
    ): Boolean = false

    @Composable
    override fun ShowPaymentOrError(
        uiState: MainUiState,
        onDismiss: () -> Unit
    ) {
        SimpleAlertDialog(uiState.showPaymentDialog, onDismiss)
    }
}