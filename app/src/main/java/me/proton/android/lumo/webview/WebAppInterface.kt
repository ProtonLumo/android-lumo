package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import me.proton.android.lumo.MainActivity
import timber.log.Timber
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
            Timber.tag(TAG).i("JavaScript interface 'Android' added successfully")

            // Inject a simple test to verify interface is working
            webView.evaluateJavascript(
                "console.log('Android interface available:', typeof window.Android !== 'undefined');",
                null
            )
        } catch (e: Exception) {
            Timber.tag(MainActivity.TAG).e(e, "Error adding JavaScript interface")
        }
    }

    fun detachWebView() {
        webView?.removeJavascriptInterface("Android")
        webView = null
    }

    fun injectTheme(theme: Int, mode: Int) {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        injectTheme(webView = webView, theme = theme, mode = mode)
    }

    fun injectSpeechOutput(spokenText: String) {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        injectSpokenText(webView, spokenText)
    }

    @JavascriptInterface
    fun showPayment() {
        Timber.tag(TAG).i("showPayment called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.ShowPaymentRequested)
    }

    @JavascriptInterface
    fun showBlackFridaySale() {
        Timber.tag(TAG).i("showBlackFridaySale called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.ShowBlackFridaySale)
    }

    @JavascriptInterface
    fun startVoiceEntry() {
        Timber.tag(TAG).i("startVoiceEntry called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.StartVoiceEntryRequested)
    }

    @JavascriptInterface
    fun retryLoad() {
        Timber.tag(TAG).i("retryLoad called from JavaScript(error page)")
        _mainEventChannel.trySend(MainWebEvent.RetryLoadRequested)
    }

    @JavascriptInterface
    fun onPageTypeChanged(isLumo: Boolean, url: String) {
        Timber.tag(TAG).i("Page type changed : isLumo = $isLumo")
        _mainEventChannel.trySend(MainWebEvent.PageTypeChanged(isLumo, url))
    }

    @JavascriptInterface
    fun onNavigation(url: String, type: String) {
        Timber.tag(TAG).i("Navigation : url =$url, type = $type")
        _mainEventChannel.trySend(MainWebEvent.Navigated(url, type))
    }

    @JavascriptInterface
    fun onLumoContainerVisible() {
        Timber.tag(TAG).i("Lumo container became visible")
        _mainEventChannel.trySend(MainWebEvent.LumoContainerVisible)
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.tag(TAG).i("Web logs: $message")
    }

    @JavascriptInterface
    fun onThemeChanged(mode: String) {
        Timber.tag(TAG).i("onThemeChanged : $mode")
        _mainEventChannel.trySend(MainWebEvent.ThemeResult(mode))
    }

    @JavascriptInterface
    fun onThemeStyleChanged(themeStyle: String) {
        if (themeStyle.isNotEmpty()) {
            Timber.tag(TAG).i("onThemeChanged : $themeStyle")
            _mainEventChannel.trySend(MainWebEvent.ThemeResult(themeStyle))
        }
    }

    companion object {
        protected const val TAG = "WebAppInterface"
    }
}