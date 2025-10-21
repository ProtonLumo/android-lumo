package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import me.proton.android.lumo.billing.BillingManagerWrapper.PaymentRequestType
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import java.util.concurrent.ConcurrentHashMap

class WebAppWithPaymentsInterface : WebAppInterface() {

    private val pendingResults =
        ConcurrentHashMap<String, CompletableDeferred<Result<PaymentJsResponse>>>()

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
            jsFunction = PaymentRequestType.PAYMENT_TOKEN
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun sendSubscriptionEvent(payload: Subscription): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = payload,
            jsFunction = PaymentRequestType.SUBSCRIPTION
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun fetchSubscriptions(): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = null,
            jsFunction = PaymentRequestType.GET_SUBSCRIPTIONS
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }

    suspend fun fetchPlans(): Result<PaymentJsResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return sendPaymentDataToWebView(
            webView = webView,
            payload = null,
            jsFunction = PaymentRequestType.GET_PLANS
        ) { transactionId, deferred ->
            pendingResults[transactionId] = deferred
        }
    }


    /**
     * Handle JavaScript result callback from MainActivity
     */
    private fun handleJavaScriptResult(
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