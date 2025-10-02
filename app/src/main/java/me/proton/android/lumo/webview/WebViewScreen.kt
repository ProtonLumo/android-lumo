package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.update
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainActivityViewModel.WebEvent
import me.proton.android.lumo.config.LumoConfig

private const val TAG = "WebViewScreen"

// Utility functions for domain checks
private fun isLumoDomain(url: String?): Boolean = LumoConfig.isLumoDomain(url)
private fun isAccountDomain(url: String?): Boolean = LumoConfig.isAccountDomain(url)

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


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    activity: MainActivity,
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)

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
                setBackgroundColor(android.graphics.Color.WHITE)

                // WebView debugging configuration based on build variant
                // GrapheneOS blocks native code debugging which causes SIGSEGV crashes in production
                // Use 'noWebViewDebug' variant for GrapheneOS and privacy-focused users
                if (BuildConfig.ENABLE_WEBVIEW_DEBUG) {
                    // Only include the debugging method call in standard variants
                    if (BuildConfig.DEBUG) {
                        WebView.setWebContentsDebuggingEnabled(true)
                        Log.d(TAG, "WebView debugging enabled (debug + standard variant)")
                    } else {
                        WebView.setWebContentsDebuggingEnabled(false)
                        Log.d(TAG, "WebView debugging disabled (release build)")
                    }
                } else {
                    // For noWebViewDebug variant, completely omit the method call
                    // This ensures GrapheneOS scanners don't detect any debugging capabilities
                    Log.d(TAG, "WebView debugging completely disabled (noWebViewDebug variant)")
                }

                // Simplified keyboard detection leveraging enableEdgeToEdge() reliability
                var wasKeyboardVisible = false

                // Create simplified keyboard listener - much cleaner with enableEdgeToEdge!
                val simplifiedKeyboardListener =
                    android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        val insets = ViewCompat.getRootWindowInsets(this)
                        if (insets == null) {
                            Log.w(TAG, "WindowInsets is null - cannot detect keyboard")
                            return@OnGlobalLayoutListener
                        }

                        // With enableEdgeToEdge(), WindowInsetsCompat provides reliable keyboard detection
                        val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                        val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                        val navigationBarHeight =
                            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

                        // Get screen density for CSS pixel conversion
                        val density = resources.displayMetrics.density

                        // Subtract navigation bar height from keyboard height for accurate positioning
                        val adjustedKeyboardHeight = maxOf(0, keyboardHeight - navigationBarHeight)
                        val keyboardHeightCss = (adjustedKeyboardHeight / density).toInt()

                        Log.d(TAG, "🎯 Keyboard detection: visible=$isKeyboardVisible")
                        Log.d(TAG, "  - Raw keyboard height: ${keyboardHeight}px physical")
                        Log.d(TAG, "  - Navigation bar height: ${navigationBarHeight}px physical")
                        Log.d(
                            TAG,
                            "  - Adjusted keyboard height: ${adjustedKeyboardHeight}px physical"
                        )
                        Log.d(TAG, "  - Final CSS height: ${keyboardHeightCss}px CSS")

                        // Only notify if keyboard state actually changed
                        if (isKeyboardVisible != wasKeyboardVisible) {
                            wasKeyboardVisible = isKeyboardVisible
                            Log.d(TAG, ">>> KEYBOARD STATE CHANGED - Notifying JavaScript <<<")

                            try {
                                activity.mainActivityViewModel.onWebEvent(
                                    event = WebEvent.KeyboardVisibilityChanged(
                                        isVisible = isKeyboardVisible,
                                        keyboardHeightPx = keyboardHeightCss
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error notifying keyboard visibility change", e)
                            }
                        }
                    }

                viewTreeObserver.addOnGlobalLayoutListener(simplifiedKeyboardListener)

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

                webViewClient = object : WebViewClient() {
                    private val errorPageUrl = "file:///android_asset/network_error.html"

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: android.graphics.Bitmap?
                    ) {
                        super.onPageStarted(view, url, favicon)

                        Log.d(TAG, ">>> onPageStarted CALLED for URL: $url")

                        val isLumoDomain = isLumoDomain(url)
                        val isAccountDomain = isAccountDomain(url)

                        Log.d(
                            TAG,
                            "URL analysis for '$url': isLumoDomain=$isLumoDomain, isAccountDomain=$isAccountDomain"
                        )

                        if ((isLumoDomain || isAccountDomain) && view != null) {
                            Log.d(
                                TAG,
                                "Calling injectSignupPlanParamFix from onPageStarted for URL: $url"
                            )
                            injectSignupPlanParamFix(view)
                            // Inject keyboard handler early to avoid race conditions
                            Log.d(
                                TAG,
                                "🚀 INJECTING KEYBOARD HANDLER EARLY in onPageStarted for URL: $url"
                            )
                            injectKeyboardHandling(view)
                            Log.d(TAG, "✅ Keyboard handler injection completed in onPageStarted")
                        } else {
                            Log.d(
                                TAG,
                                "❌ Skipping keyboard injection - isLumoDomain=$isLumoDomain, isAccountDomain=$isAccountDomain, view=$view"
                            )
                        }

                        // Only show loading screen when navigating to Lumo pages
                        if (isLumoDomain) {
                            activity.mainActivityViewModel._uiState.update {
                                it.copy(isLoading = true, hasSeenLumoContainer = false)
                            }
                            Log.d(TAG, "Lumo page loading started - showing loading overlay")
                        } else {
                            Log.d(TAG, "Non-Lumo page loading - no loading overlay needed")
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        Log.d(TAG, ">>> onPageFinished CALLED for URL: $url")

                        try {
                            // *** NOW, check if it's the error page and skip the rest if it is ***
                            if (url == errorPageUrl) {
                                Log.d(TAG, "Skipping non-essential JS injection for error page.")
                                activity.mainActivityViewModel._uiState.update { it.copy(isLoading = false) }
                                return // Exit after adding the interface
                            }

                            if ((isLumoDomain(url) || isAccountDomain(url)) && view != null) {
                                Log.d(TAG, "Injecting essential JavaScript for URL: $url")
                                injectAndroidInterfacePolyfill(view) // Inject polyfill first for robust interface calls
                                injectEssentialJavascript(view)
                                injectLumoContainerCheck(view)
                                injectPromotionButtonHandlers(view)
                                injectUpgradeLinkHandlers(view)
                                themeChangeListener(view)
                                Log.d(
                                    TAG,
                                    "Calling injectSignupPlanParamFix from onPageFinished for URL: $url"
                                )
                                injectSignupPlanParamFix(view)


                                // Inject safe area insets for edge-to-edge support
                                // Use a small delay to ensure the page is fully loaded
                                Handler(Looper.getMainLooper()).postDelayed({
                                    view.requestApplyInsets() // Trigger inset application
                                }, 300)

                                // Verify Android interface is working after a brief delay
                                Handler(Looper.getMainLooper()).postDelayed({
                                    verifyAndroidInterface(view)
                                }, 1000) // Wait 1 second for all injections to complete

                                // Inject account page modifier only for account domain pages
                                if (isAccountDomain(url)) {
                                    Log.d(TAG, "Injecting account page modifier for URL: $url")
                                    injectAccountPageModifier(view)
                                }

                                // Add a safety timeout to ensure loading state is cleared
                                Handler(Looper.getMainLooper()).postDelayed({
                                    val currentState = activity.mainActivityViewModel.uiState.value
                                    Log.d(
                                        TAG,
                                        "Safety timeout reached, current loading state: ${currentState.isLoading}"
                                    )
                                    if (currentState.isLoading) {
                                        Log.d(
                                            TAG,
                                            "Forcing loading state off and setting hasSeenLumoContainer to true"
                                        )
                                        activity.runOnUiThread {
                                            activity.mainActivityViewModel._uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    hasSeenLumoContainer = true
                                                )
                                            }
                                            Log.d(TAG, "State updated via ViewModel")
                                        }
                                    }
                                }, 2000) // Reduced to 2 seconds for faster response
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error during onPageFinished setup", e)
                            // Ensure loading state is cleared even on error
                            activity.mainActivityViewModel._uiState.update { it.copy(isLoading = false) }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        // Ignore minor errors (e.g., favicon not found)
                        if (request?.isForMainFrame == true && error != null && view != null) {
                            val errorCode = error.errorCode
                            val description = error.description ?: "Unknown error"
                            val failingUrl = request.url?.toString() ?: "Unknown URL"

                            Log.e(
                                TAG,
                                "WebView Error: Code=$errorCode, Desc=$description, URL=$failingUrl"
                            )

                            // Check for common network-related errors
                            val isNetworkError = when (errorCode) {
                                ERROR_HOST_LOOKUP,
                                ERROR_CONNECT,
                                ERROR_TIMEOUT,
                                ERROR_IO,
                                ERROR_UNKNOWN,
                                ERROR_BAD_URL,
                                ERROR_UNSUPPORTED_SCHEME -> true

                                else -> false
                            }

                            if (isNetworkError) {
                                Log.i(TAG, "Network error detected. Loading custom error page.")
                                view.loadUrl(errorPageUrl)
                            } else {
                                super.onReceivedError(view, request, error)
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false

                        // Only allow configured Lumo and Account domains in the WebView
                        if (LumoConfig.isKnownDomain(url)) {
                            return false // Let WebView handle it
                        } else {
                            // Open all other domains externally
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    request.url
                                )
                                view?.context?.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to open external link: $url", e)
                            }
                            return true // Cancel loading in WebView
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        activity.filePathCallback = filePathCallback
                        val intent =
                            android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                        activity.fileChooserLauncher.launch(intent)
                        return true
                    }
                }

                // Load the INITIAL URL passed in
                Log.d(TAG, "WebView factory: Loading initial URL: $initialUrl")
                loadUrl(initialUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
} 