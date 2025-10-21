package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

@SuppressLint("StaticFieldLeak")
open class WebAppInterface {

    @Volatile
    protected var webView: WebView? = null

    private val _mainEventChannel = Channel<MainWebEvent>()
    val mainEventChannel = _mainEventChannel.receiveAsFlow()

    fun attachWebView(webView: WebView) {
        try {
            this.webView = webView
            // Remove any existing interface first to prevent duplicates
            webView.removeJavascriptInterface("Android")

            // Add the interface
            webView.addJavascriptInterface(
                this,
                "Android"
            )
            Log.d(MainActivity.TAG, "JavaScript interface 'Android' added successfully")

            // Inject a simple test to verify interface is working
            webView.evaluateJavascript(
                "console.log('Android interface available:', typeof window.Android !== 'undefined');",
                null
            )
        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error adding JavaScript interface", e)
        }
    }

    fun detachWebView() {
        webView?.removeJavascriptInterface("Android")
        webView = null
    }

    @JavascriptInterface
    fun showPayment() {
        Log.d(TAG, "showPayment called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.ShowPaymentRequested)
    }

    @JavascriptInterface
    fun startVoiceEntry() {
        Log.d(TAG, "startVoiceEntry called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.StartVoiceEntryRequested)
    }

    @JavascriptInterface
    fun retryLoad() {
        Log.d(TAG, "retryLoad called from JavaScript (error page)")
        _mainEventChannel.trySend(MainWebEvent.RetryLoadRequested)
    }

    @JavascriptInterface
    fun onPageTypeChanged(isLumo: Boolean, url: String) {
        Log.d(TAG, "Page type changed: isLumo = $isLumo")
        _mainEventChannel.trySend(MainWebEvent.PageTypeChanged(isLumo, url))
    }

    @JavascriptInterface
    fun onNavigation(url: String, type: String) {
        Log.d(TAG, "Navigation: url=$url, type=$type")
        _mainEventChannel.trySend(MainWebEvent.Navigated(url, type))
    }

    @JavascriptInterface
    fun onLumoContainerVisible() {
        Log.d(TAG, "Lumo container became visible")
        _mainEventChannel.trySend(MainWebEvent.LumoContainerVisible)
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "Web logs: $message")
    }

    @JavascriptInterface
    fun onThemeChanged(mode: String) {
        Log.d(TAG, "onThemeChanged: $mode")
        _mainEventChannel.trySend(MainWebEvent.ThemeResult(mode))
    }

    companion object {
        protected const val TAG = "WebAppInterface"
    }
}