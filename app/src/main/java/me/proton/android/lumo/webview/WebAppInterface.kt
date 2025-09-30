package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.JavascriptInterface
import me.proton.android.lumo.MainActivityViewModel
import me.proton.android.lumo.billing.BillingDelegate
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

class WebAppInterface(
    private val viewModel: MainActivityViewModel,
    private val billingDelegate: BillingDelegate,
) {
    @JavascriptInterface
    fun showPayment() {
        Log.d(TAG, "showPayment called from JavaScript")
        viewModel.onWebEvent(MainWebEvent.ShowPaymentRequested)
    }

    @JavascriptInterface
    fun startVoiceEntry() {
        Log.d(TAG, "startVoiceEntry called from JavaScript")
        viewModel.onWebEvent(MainWebEvent.StartVoiceEntryRequested)
    }

    @JavascriptInterface
    fun retryLoad() {
        Log.d(TAG, "retryLoad called from JavaScript (error page)")
        viewModel.onWebEvent(MainWebEvent.RetryLoadRequested)
    }

    @JavascriptInterface
    fun onPageTypeChanged(isLumo: Boolean, url: String) {
        Log.d(TAG, "Page type changed: isLumo = $isLumo")
        viewModel.onWebEvent(MainWebEvent.PageTypeChanged(isLumo, url))
    }

    @JavascriptInterface
    fun onNavigation(url: String, type: String) {
        Log.d(TAG, "Navigation: url=$url, type=$type")
        viewModel.onWebEvent(MainWebEvent.Navigated(url, type))
    }

    @JavascriptInterface
    fun onLumoContainerVisible() {
        Log.d(TAG, "Lumo container became visible")
        viewModel.onWebEvent(MainWebEvent.LumoContainerVisible)
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "Web logs: $message")
    }

    @JavascriptInterface
    fun onThemeChanged(mode: String) {
        Log.d(TAG, "onThemeChanged: $mode")
        viewModel.onWebEvent(MainWebEvent.ThemeResult(mode))
    }

    @JavascriptInterface
    fun postResult(transactionId: String, resultJson: String) {
        Log.d(TAG, "postResult received: id=$transactionId")
        billingDelegate.handleJavaScriptResult(transactionId, resultJson)
    }

    companion object {
        private const val TAG = "WebAppInterface"
    }
}