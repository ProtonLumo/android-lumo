package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.billing.BillingManagerWrapper.PAYMENT_REQUEST_TYPE
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import java.util.concurrent.ConcurrentHashMap
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

@SuppressLint("StaticFieldLeak")
object WebAppInterface {

    private const val TAG = "WebAppInterface"

    @Volatile
    private var webView: WebView? = null

    private val pendingResults =
        ConcurrentHashMap<String, CompletableDeferred<Result<PaymentJsResponse>>>()

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

    @JavascriptInterface
    fun postResult(transactionId: String, resultJson: String) {
        Log.d(TAG, "postResult received: id=$transactionId")
        handleJavaScriptResult(transactionId, resultJson)
    }

    suspend fun sendPaymentToken(payload: PaymentTokenPayload): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = payload,
            jsFunction = PAYMENT_REQUEST_TYPE.PAYMENT_TOKEN
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun sendSubscriptionEvent(payload: Subscription): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = payload,
            jsFunction = PAYMENT_REQUEST_TYPE.SUBSCRIPTION
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun fetchSubscriptions(): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = null,
            jsFunction = PAYMENT_REQUEST_TYPE.GET_SUBSCRIPTIONS
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun fetchPlans(): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = null,
            jsFunction = PAYMENT_REQUEST_TYPE.GET_PLANS
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }


    /**
     * Handle JavaScript result callback from MainActivity
     */
    fun handleJavaScriptResult(
        transactionId: String,
        resultJson: String
    ) {
        Log.d(TAG, "handleJavaScriptResult received for ID $transactionId: $resultJson")

        // Retrieve and remove the original callback
        val deferred = pendingResults.remove(transactionId)
        if (deferred == null) {
            Log.e(TAG, "No callback found for transaction ID: $transactionId")
            return
        }

        // Process the result string and invoke the callback
        val finalResult = processJavascriptResult(resultJson, Gson())
        deferred.complete(finalResult)
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
}