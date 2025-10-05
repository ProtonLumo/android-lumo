package me.proton.android.lumo.billing

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.ui.components.PaymentDialog

class BillingDelegateImpl() : BillingDelegate {

    private lateinit var billingManagerWrapper: BillingManagerWrapper

    override fun initialise(activity: MainActivity) {
        billingManagerWrapper = DependencyProvider.getBillingManagerWrapper()
        billingManagerWrapper.initializeBilling(activity)
    }

    override fun handleJavaScriptResult(transactionId: String, resultJson: String): Boolean =
        billingManagerWrapper.handleJavaScriptResult(transactionId, resultJson)
}