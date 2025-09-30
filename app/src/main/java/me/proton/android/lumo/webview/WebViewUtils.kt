package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.WebView
import me.proton.android.lumo.MainActivity

/**
 * Safely adds the JavaScript interface to the WebView with proper error handling
 * and prevents duplicate registrations
 */
fun addJavaScriptInterfaceSafely(webView: WebView, activity: MainActivity) {
    try {
        // Remove any existing interface first to prevent duplicates
        webView.removeJavascriptInterface("Android")

        val webAppInterface = WebAppInterface(activity.viewModel)
        // Add the interface
        webView.addJavascriptInterface(
            webAppInterface,
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
