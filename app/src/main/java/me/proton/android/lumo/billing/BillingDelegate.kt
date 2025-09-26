package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.runtime.Composable
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.models.PaymentJsResponse

interface BillingDelegate {

    fun initialise(activity: MainActivity)
    fun getPlansFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    )
    fun getSubscriptionsFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    )
    fun handleJavaScriptResult(transactionId: String, resultJson: String): Boolean
    @Composable
    fun ShowPaymentOrError(
        uiState: MainUiState,
        onDismiss: () -> Unit,
    )
}