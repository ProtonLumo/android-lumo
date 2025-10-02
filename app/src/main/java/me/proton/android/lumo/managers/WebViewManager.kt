package me.proton.android.lumo.managers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
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

    /**
     * Handle file chooser result from activity result launcher
     */
    fun handleFileChooserResult(
        resultCode: Int,
        data: Intent?
    ) {
        val results = if (data == null || resultCode != Activity.RESULT_OK) {
            null
        } else {
            val dataString = data.dataString
            if (dataString != null) {
                arrayOf(Uri.parse(dataString))
            } else {
                data.clipData?.let { clipData ->
                    Array(clipData.itemCount) { i ->
                        clipData.getItemAt(i).uri
                    }
                }
            }
        }

        filePathCallback?.onReceiveValue(results ?: arrayOf())
        filePathCallback = null

        Log.d(TAG, "File chooser result handled. Results: ${results?.size ?: 0} files")
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
