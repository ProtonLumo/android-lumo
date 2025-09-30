package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.runtime.Composable
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState

interface BillingDelegate {

    fun initialise(activity: MainActivity)
    fun handleJavaScriptResult(
        transactionId: String,
        resultJson: String
    ): Boolean
    @Composable
    fun ShowPaymentOrError(
        uiState: MainUiState,
        webView: WebView,
        onDismiss: () -> Unit,
    )
}