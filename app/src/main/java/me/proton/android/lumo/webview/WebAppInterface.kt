package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.JavascriptInterface
import me.proton.android.lumo.MainActivityViewModel
import me.proton.android.lumo.domain.WebEvent

class WebAppInterface(private val viewModel: MainActivityViewModel) {
    @JavascriptInterface
    fun showPayment() {
        Log.d(TAG, "showPayment called from JavaScript")
        viewModel.onWebEvent(WebEvent.ShowPaymentRequested)
    }

    @JavascriptInterface
    fun startVoiceEntry() {
        Log.d(TAG, "startVoiceEntry called from JavaScript")
        viewModel.onWebEvent(WebEvent.StartVoiceEntryRequested)
    }

    @JavascriptInterface
    fun retryLoad() {
        Log.d(TAG, "retryLoad called from JavaScript (error page)")
        viewModel.onWebEvent(WebEvent.RetryLoadRequested)
    }

    @JavascriptInterface
    fun onPageTypeChanged(isLumo: Boolean, url: String) {
        Log.d(TAG, "Page type changed: isLumo = $isLumo")
        viewModel.onWebEvent(WebEvent.PageTypeChanged(isLumo, url))
    }

    @JavascriptInterface
    fun onNavigation(url: String, type: String) {
        Log.d(TAG, "Navigation: url=$url, type=$type")
        viewModel.onWebEvent(WebEvent.Navigated(url, type))
    }

    @JavascriptInterface
    fun onLumoContainerVisible() {
        Log.d(TAG, "Lumo container became visible")
        viewModel.onWebEvent(WebEvent.LumoContainerVisible)
    }

    @JavascriptInterface
    fun postResult(transactionId: String, resultJson: String) {
        Log.d(TAG, "postResult received: id=$transactionId")
        viewModel.onWebEvent(WebEvent.PostResult(transactionId, resultJson))
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "Web logs: $message")
    }

    @JavascriptInterface
    fun onThemeChanged(mode: String) {
        Log.d(TAG, "onThemeChanged: $mode")
        viewModel.onWebEvent(WebEvent.ThemeResult(mode))
    }

    companion object {
        private const val TAG = "WebAppInterface"
    }
}