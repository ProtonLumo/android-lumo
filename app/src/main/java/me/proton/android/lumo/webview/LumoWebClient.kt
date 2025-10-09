package me.proton.android.lumo.webview

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import me.proton.android.lumo.config.LumoConfig

class LumoWebClient(
    private val isLoading: () -> Boolean,
    private val showLoading: () -> Unit,
    private val hideLoading: (Boolean) -> Unit,
) : WebViewClient() {
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
            showLoading()
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
                hideLoading(false)
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
                    val isLoading = isLoading()
                    Log.d(
                        TAG,
                        "Safety timeout reached, current loading state: ${isLoading}"
                    )
                    if (isLoading) {
                        Log.d(
                            TAG,
                            "Forcing loading state off and setting hasSeenLumoContainer to true"
                        )
                        hideLoading(true)
                        Log.d(TAG, "State updated via ViewModel")
                    }
                }, 2000) // Reduced to 2 seconds for faster response
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during onPageFinished setup", e)
            // Ensure loading state is cleared even on error

            hideLoading(false)
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

    // Utility functions for domain checks
    private fun isLumoDomain(url: String?): Boolean = LumoConfig.isLumoDomain(url)

    private fun isAccountDomain(url: String?): Boolean = LumoConfig.isAccountDomain(url)

    companion object {
        private const val TAG = "LumoWebClient"
    }
}
