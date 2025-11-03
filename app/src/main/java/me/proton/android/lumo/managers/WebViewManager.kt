package me.proton.android.lumo.managers

import android.net.Uri
import android.util.Log
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

private const val TAG = "WebViewManager"

/**
 * Manager class that handles WebView-related operations including file chooser functionality.
 * Separates WebView concerns from MainActivity.
 */
class WebViewManager() {

    // File chooser callback
    var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Reference to the current WebView
    private var _webView: WebView? = null
    val webView: WebView? get() = _webView

    init {
        ServiceWorkerController.getInstance()
            .setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return null
                }
            })
    }

    /**
     * Set the WebView instance
     */
    fun setWebView(webView: WebView) {
        this._webView = webView
        Log.d(TAG, "WebView instance set")
    }

    /**
     * Execute JavaScript in the WebView
     */
    fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)? = null) {
        _webView?.evaluateJavascript(script, callback)
        Log.d(TAG, "JavaScript executed: ${script.take(100)}...")
    }

    /**
     * Load a URL in the WebView
     */
    fun loadUrl(url: String) {
        _webView?.loadUrl(url)
        Log.d(TAG, "Loading URL: $url")
    }

    /**
     * Check if WebView can go back
     */
    fun canGoBack(): Boolean {
        return _webView?.canGoBack() ?: false
    }

    /**
     * Navigate back in WebView
     */
    fun goBack() {
        _webView?.goBack()
        Log.d(TAG, "WebView navigated back")
    }

    fun clearHistory() {
        _webView?.clearHistory()
    }

    fun currentUrl(): String? =
        _webView?.url

    fun invalidate() {
        _webView?.invalidate()
    }

    /**
     * Destroy the WebView
     */
    fun destroy() {
        _webView?.destroy()
        _webView = null
        filePathCallback = null
        Log.d(TAG, "WebView destroyed")
    }
}
