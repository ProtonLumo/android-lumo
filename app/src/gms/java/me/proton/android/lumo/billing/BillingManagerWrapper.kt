package me.proton.android.lumo.billing

import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "BillingManagerWrapper"

/**
 * Wrapper class that handles all billing-related operations and Google Play Services integration.
 * Provides a clean interface for MainActivity while handling billing availability gracefully.
 */
class BillingManagerWrapper(private val activity: MainActivity) {

    interface BillingCallbacks {
        fun sendPaymentTokenToWebView(
            payload: PaymentTokenPayload,
            callback: ((Result<PaymentJsResponse>) -> Unit)? = null
        )

        fun sendSubscriptionEventToWebView(
            payload: Subscription,
            callback: ((Result<PaymentJsResponse>) -> Unit)? = null
        )
    }

    private var billingManager: BillingManager? = null

    // Map to store callbacks for JS results
    private val pendingJsCallbacks =
        ConcurrentHashMap<String, (Result<PaymentJsResponse>) -> Unit>()

    /**
     * Enum defining the types of JavaScript functions to invoke in the WebView
     */
    enum class PAYMENT_REQUEST_TYPE(val functionName: String) {
        PAYMENT_TOKEN("postPaymentToken"),
        SUBSCRIPTION("postSubscription"),
        GET_PLANS("getPlans"),
        GET_SUBSCRIPTIONS("getSubscriptions")
    }

    /**
     * Initialize billing with comprehensive Google Services availability check
     */
    fun initializeBilling() {
        try {
            Log.d(TAG, "=== BILLING INITIALIZATION CHECK ===")

            // 1. Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val playServicesStatus = googleApiAvailability.isGooglePlayServicesAvailable(activity)

            when (playServicesStatus) {
                ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "✅ Google Play Services is available")
                    initializeBillingManager()
                }

                ConnectionResult.SERVICE_MISSING -> {
                    Log.w(TAG, "❌ Google Play Services is not installed")
                    handleBillingUnavailable("Google Play Services is not available on this device")
                }

                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "❌ Google Play Services needs to be updated")
                    handleBillingUnavailable("Google Play Services needs to be updated")
                }

                ConnectionResult.SERVICE_DISABLED -> {
                    Log.w(TAG, "❌ Google Play Services is disabled")
                    handleBillingUnavailable("Google Play Services is disabled on this device")
                }

                else -> {
                    Log.w(TAG, "❌ Google Play Services unavailable: $playServicesStatus")
                    handleBillingUnavailable("Google Play Services is not available (code: $playServicesStatus)")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during billing initialization", e)
            handleBillingUnavailable("Billing services are not available: ${e.message}")
        }
    }

    private fun initializeBillingManager() {
        // 2. Check if Google Play Store app is installed and accessible
        try {
            val pInfo = activity.packageManager.getPackageInfo("com.android.vending", 0)
            Log.d(TAG, "✅ Google Play Store version: ${pInfo.versionName}")

            // 3. Initialize BillingManager
            try {
                Log.d(TAG, "Initializing BillingManager...")
                val tempBillingManager =
                    BillingManager(
                        activity = activity,
                        billingCallbacks = object : BillingCallbacks {
                            override fun sendPaymentTokenToWebView(
                                payload: PaymentTokenPayload,
                                callback: ((Result<PaymentJsResponse>) -> Unit)?
                            ) {
                                activity.runOnUiThread {
                                    activity.webView?.let {
                                        sendPaymentTokenToWebView(it, payload, callback)
                                    }
                                }
                            }

                            override fun sendSubscriptionEventToWebView(
                                payload: Subscription,
                                callback: ((Result<PaymentJsResponse>) -> Unit)?
                            ) {
                                activity.runOnUiThread {
                                    activity.webView?.let {
                                        sendSubscriptionEventToWebView(it, payload, callback)
                                    }
                                }
                            }
                        }
                    )

                // Check if BillingClient was created successfully
                if (tempBillingManager.isBillingAvailable()) {
                    billingManager = tempBillingManager
                    Log.d(TAG, "✅ BillingManager initialized successfully")
                } else {
                    Log.w(TAG, "⚠️ BillingClient creation failed - billing unavailable")
                    handleBillingUnavailable("Google Play Billing API is not available on this device")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ BillingManager initialization failed", e)
                val errorMessage = e.message ?: "Unknown error"
                val reason = when {
                    errorMessage.contains("API version is less than 3", ignoreCase = true) -> {
                        "Google Play Billing API version is too old. Please update Google Play Store."
                    }

                    errorMessage.contains("not supported", ignoreCase = true) -> {
                        "In-app purchases are not supported on this device."
                    }

                    else -> {
                        "Failed to initialize billing: $errorMessage"
                    }
                }
                handleBillingUnavailable(reason)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Play Store not accessible", e)
            handleBillingUnavailable("Google Play Store is not accessible")
        }
    }

    /**
     * Handle the case where billing is unavailable
     */
    private fun handleBillingUnavailable(reason: String) {
        Log.w(TAG, "=== BILLING UNAVAILABLE - ENTERING GRACEFUL DEGRADATION MODE ===")
        Log.w(TAG, "Reason: $reason")

        // Set states to indicate unavailability
        billingManager = null

        // Show user-friendly notification about billing unavailability
        val message = when {
            reason.contains("API version", ignoreCase = true) -> {
                "Google Play Store needs to be updated for in-app purchases. All other features will work normally."
            }

            reason.contains("not supported", ignoreCase = true) -> {
                "In-app purchases not supported on this device. All other features will work normally."
            }

            else -> {
                "In-app purchases are not available. All other features will work normally."
            }
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()

        Log.i(TAG, "✅ App will continue normally with billing features disabled")
    }

    /**
     * Get the BillingManager instance if available
     */
    fun getBillingManager(): BillingManager? = billingManager

    /**
     * Generic method to send data to the WebView's JavaScript API using JavascriptInterface for callback
     */
    private fun <T> sendDataToWebView(
        webView: WebView,
        payload: T?, // Payload is nullable
        jsFunction: PAYMENT_REQUEST_TYPE,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        val gson = Gson()
        val payloadJson = payload?.let { gson.toJson(it) } ?: "null"
        val payloadLog = payload?.let { gson.toJson(it) } ?: "No payload"

        // Generate a unique ID for this transaction
        val transactionId = UUID.randomUUID().toString()

        // Store the callback if it exists
        if (callback != null) {
            pendingJsCallbacks[transactionId] = callback
        }

        Log.d(TAG, "Sending ${jsFunction.name.lowercase()} (ID: $transactionId)...")
        Log.d(TAG, "Payload: $payloadLog")

        val jsFunctionCall =
            if (jsFunction == PAYMENT_REQUEST_TYPE.GET_PLANS || jsFunction == PAYMENT_REQUEST_TYPE.GET_SUBSCRIPTIONS) {
                "window.paymentApiInstance.${jsFunction.functionName}('android')"
            } else {
                "window.paymentApiInstance.${jsFunction.functionName}($payloadJson)"
            }

        // JS now calls AndroidInterface.postResult instead of returning a value
        val js = """
            (async function() {
                const txId = '$transactionId'; // Pass transactionId to JS
                try {
                    // Check if the Android interface exists before using it
                    if (typeof Android === 'undefined' || typeof Android.postResult !== 'function') {
                         console.error('Android.postResult is not available. Cannot send result back.');
                         // If no callback was provided originally, this is fine. If one was, we can't fulfill it.
                         return; // Exit early
                    }

                    if (window.paymentApiInstance && typeof window.paymentApiInstance.${jsFunction.functionName} === 'function') {
                        const result = await $jsFunctionCall;
                        const resultJson = JSON.stringify({ status: 'success', data: result });
                        Android.postResult(txId, resultJson);
                    } else {
                        const errorMsg = 'paymentApiInstance or ${jsFunction.functionName} not found';
                        console.error(errorMsg);
                        const errorJson = JSON.stringify({ status: 'error', message: errorMsg });
                        Android.postResult(txId, errorJson);
                    }
                } catch (e) {
                    const errorMessage = e instanceof Error ? e.message : String(e);
                    console.error('Error executing ${jsFunction.functionName}:', errorMessage);
                    const errorJson = JSON.stringify({ status: 'error', message: 'JS Error: ' + errorMessage });
                    // Ensure we still call back even on error
                    if (typeof Android !== 'undefined' && typeof Android.postResult === 'function') {
                         Android.postResult(txId, errorJson);
                    } else {
                         console.error('Android interface not available to report JS error.');
                    }
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    /**
     * Handle JavaScript result callback from MainActivity
     */
    fun handleJavaScriptResult(transactionId: String, resultJson: String): Boolean {
        Log.d(TAG, "handleJavaScriptResult received for ID $transactionId: $resultJson")

        // Retrieve and remove the original callback
        val callback = pendingJsCallbacks.remove(transactionId)
        if (callback == null) {
            Log.e(TAG, "No callback found for transaction ID: $transactionId")
            return false
        }

        // Process the result string and invoke the callback
        val finalResult = processJavascriptResult(resultJson, Gson())
        callback(finalResult)
        return true
    }

    /**
     * Helper function to process JavaScript results
     */
    private fun processJavascriptResult(
        resultString: String?,
        gson: Gson
    ): Result<PaymentJsResponse> {
        // Add check for common unresolved promise representations
        if (resultString?.startsWith("[object Promise]") == true || resultString == "undefined" || resultString == "{}") {
            Log.w(
                TAG,
                "JavaScript returned a Promise object representation, empty object, or undefined. Raw: $resultString"
            )
            return Result.failure(Exception("JavaScript promise handling error or unexpected result."))
        }
        return try {
            var processableString = resultString?.removeSurrounding("\"")
            // If the string still looks like an encoded JSON string (starts with \" and ends with \")
            // attempt to parse it as a string first to decode it
            if (processableString?.startsWith("\\\"") == true && processableString.endsWith("\\\"")) {
                try {
                    processableString = gson.fromJson(processableString, String::class.java)
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Could not decode potentially double-encoded JSON string: $processableString",
                        e
                    )
                    // Proceed with the original string if decoding fails
                }
            }
            if (processableString != null) {
                val parsedResponse = gson.fromJson(processableString, PaymentJsResponse::class.java)
                if (parsedResponse.status == "success") {
                    Result.success(parsedResponse)
                } else {
                    Result.failure(Exception(parsedResponse.message ?: "Unknown error from JS"))
                }
            } else {
                Result.failure(Exception("JavaScript returned null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing JS response: $resultString", e)
            Result.failure(Exception("Error processing JS response: ${e.message}. Raw response: $resultString"))
        }
    }

    // Convenience methods that use the generic implementation
    fun sendPaymentTokenToWebView(
        webView: WebView,
        payload: PaymentTokenPayload,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        Log.d(TAG, "Sending payment token: $payload")
        sendDataToWebView(webView, payload, PAYMENT_REQUEST_TYPE.PAYMENT_TOKEN, callback)
    }

    fun sendSubscriptionEventToWebView(
        webView: WebView,
        payload: Subscription,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        sendDataToWebView(webView, payload, PAYMENT_REQUEST_TYPE.SUBSCRIPTION, callback)
    }

    fun getPlansFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        // For getPlans, payload is null
        sendDataToWebView(webView, null, PAYMENT_REQUEST_TYPE.GET_PLANS, callback)
    }

    fun getSubscriptionsFromWebView(
        webView: WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        // For getSubscriptions, payload is null
        sendDataToWebView(webView, null, PAYMENT_REQUEST_TYPE.GET_SUBSCRIPTIONS) { result ->
            // Log the raw response for debugging
            result.onSuccess { response ->
                if (response.data != null) {
                    Log.d("Subscriptions", "Raw subscription response: ${response.data}")
                }
            }
            // Call the original callback
            callback?.invoke(result)
        }
    }
}
