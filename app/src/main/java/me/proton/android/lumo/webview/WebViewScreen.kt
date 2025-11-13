package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.MainActivity

private const val TAG = "WebViewScreen"

@SuppressLint("SetJavaScriptEnabled")
fun createWebView(
    context: Context,
    initialUrl: String,
    lumoWebClient: LumoWebClient,
    lumoChromeClient: LumoChromeClient,
    onAttach: (WebView) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Modern WebView settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            // Enable caching for faster subsequent loads
            cacheMode = WebSettings.LOAD_DEFAULT
            val customUserAgent = generateCustomUserAgent()
            userAgentString = customUserAgent
            Log.d(TAG, "Custom User Agent set: $customUserAgent")
        }

        // Set WebView background to white to match loading screen and prevent flashing
        setBackgroundColor(Color.WHITE)

        toggleDebug()

        // Keep the window insets listener for edge-to-edge insets only
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            // Handle system bar insets for edge-to-edge
            val systemBarsInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // Normalize height for high-DPI displays
            val density = view.resources.displayMetrics.density

            // Inject safe area insets into the webpage for edge-to-edge design
            injectSafeAreaInsets(this@apply, systemBarsInsets, imeHeight, density)

            insets
        }

        webViewClient = lumoWebClient
        webChromeClient = lumoChromeClient

        try {
            onAttach(this)
        } catch (e: Exception) {
            Log.e(
                MainActivity.Companion.TAG,
                "WebView factory: Error adding JavascriptInterface",
                e
            )
        }


        // Load the INITIAL URL passed in
        Log.d(TAG, "WebView factory: Loading initial URL: $initialUrl")
        loadUrl(initialUrl)
    }
}

// WebView debugging configuration based on build variant
// GrapheneOS blocks native code debugging which causes SIGSEGV crashes in production
// Use 'noWebViewDebug' variant for GrapheneOS and privacy-focused users
private fun toggleDebug() {
    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
        Log.d(TAG, "WebView debugging enabled (debug)")
    } else {
        Log.d(TAG, "WebView debugging completely disabled")
    }
}

/**
 * Generate custom user agent string in format: ProtonLumo/(version) (Platform PlatformVersion; Device Name)
 * Example: ProtonLumo/1.0 (Android 12; Samsung Galaxy)
 */
private fun generateCustomUserAgent(): String {
    val appVersion = BuildConfig.VERSION_NAME
    val androidVersion = android.os.Build.VERSION.RELEASE
    val deviceManufacturer = android.os.Build.MANUFACTURER
    val deviceModel = android.os.Build.MODEL

    // Clean up device name - combine manufacturer and model, but avoid duplication
    val deviceName = if (deviceModel.startsWith(deviceManufacturer, ignoreCase = true)) {
        deviceModel
    } else {
        "$deviceManufacturer $deviceModel"
    }

    return "ProtonLumo/$appVersion (Android $androidVersion; $deviceName)"
}

/**
 * Inject safe area insets into the webpage for proper edge-to-edge handling
 * This is the recommended approach for WebView apps that own their content
 */
private fun injectSafeAreaInsets(
    webView: WebView,
    systemBarsInsets: androidx.core.graphics.Insets,
    imeHeight: Int,
    density: Float
) {
    try {
        // Convert pixels to density-independent pixels for CSS
        val topDp = (systemBarsInsets.top / density)
        val rightDp = (systemBarsInsets.right / density)
        val bottomDp =
            maxOf(systemBarsInsets.bottom, imeHeight) / density // Use larger of nav bar or keyboard
        val leftDp = (systemBarsInsets.left / density)

        // Inject CSS variables for safe area insets
        val safeAreaJs = """
            document.documentElement.style.setProperty('--safe-area-inset-top', '${topDp}px');
            document.documentElement.style.setProperty('--safe-area-inset-right', '${rightDp}px');
            document.documentElement.style.setProperty('--safe-area-inset-bottom', '${bottomDp}px');
            document.documentElement.style.setProperty('--safe-area-inset-left', '${leftDp}px');
            
            // Also set standard env() variables if supported
            if (typeof CSS !== 'undefined' && CSS.supports && CSS.supports('padding', 'env(safe-area-inset-top)')) {
                document.documentElement.style.setProperty('--safe-area-inset-top', 'env(safe-area-inset-top, ${topDp}px)');
                document.documentElement.style.setProperty('--safe-area-inset-right', 'env(safe-area-inset-right, ${rightDp}px)');
                document.documentElement.style.setProperty('--safe-area-inset-bottom', 'env(safe-area-inset-bottom, ${bottomDp}px)');
                document.documentElement.style.setProperty('--safe-area-inset-left', 'env(safe-area-inset-left, ${leftDp}px)');
            }
            
            console.log('Safe area insets injected:', {top: ${topDp}, right: ${rightDp}, bottom: ${bottomDp}, left: ${leftDp}});
        """.trimIndent()

        webView.evaluateJavascript(safeAreaJs, null)
        Log.d(
            TAG,
            "Safe area insets injected: top=${topDp}dp, right=${rightDp}dp, bottom=${bottomDp}dp, left=${leftDp}dp"
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error injecting safe area insets", e)
    }
}