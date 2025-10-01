package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.ui.components.PaymentDialog
import me.proton.android.lumo.ui.components.SimpleAlertDialog

class BillingDelegateImpl() : BillingDelegate {

    private lateinit var billingManagerWrapper: BillingManagerWrapper

    override fun initialise(activity: MainActivity) {
        billingManagerWrapper = DependencyProvider.getBillingManagerWrapper(activity)
        billingManagerWrapper.initializeBilling()
    }

    override fun getPlansFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)?
    ) {
        billingManagerWrapper.getPlansFromWebView(webView, callback)
    }

    override fun getSubscriptionsFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)?
    ) {
        billingManagerWrapper.getSubscriptionsFromWebView(webView, callback)
    }

    override fun handleJavaScriptResult(transactionId: String, resultJson: String): Boolean =
        billingManagerWrapper.handleJavaScriptResult(transactionId, resultJson)

    @Composable
    override fun ShowPaymentOrError(
        uiState: MainUiState,
        onDismiss: () -> Unit,
    ) {
        billingManagerWrapper.getBillingManager()?.let { manager ->
            PaymentDialog(
                visible = uiState.showPaymentDialog,
                isDarkTheme = isSystemInDarkTheme(),
                billingManager = manager,
                onDismiss = onDismiss
            )
        } ?: run {
            // When billing is unavailable, show a simple dialog informing the user
            SimpleAlertDialog(uiState.showPaymentDialog, onDismiss)
        }
    }
}