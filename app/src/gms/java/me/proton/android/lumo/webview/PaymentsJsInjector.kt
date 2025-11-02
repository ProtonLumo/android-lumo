package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.proton.android.lumo.billing.BillingManagerWrapper.PaymentRequestType
import me.proton.android.lumo.models.PaymentJsResponse
import java.util.UUID


/**
 * Generic method to send data to the WebView's JavaScript API using JavascriptInterface for callback
 */
suspend inline fun <reified T> sendPaymentDataToWebView(
    webView: WebView,
    payload: T?, // Payload is nullable
    jsFunction: PaymentRequestType,
    deferredCreated: (String, CompletableDeferred<Result<PaymentJsResponse>>) -> Unit,
): Result<PaymentJsResponse> {
    val payloadJson = payload?.let { Json.encodeToString(it) } ?: "null"
    val payloadLog = payload?.let { Json.encodeToString(it) } ?: "No payload"

    // Generate a unique ID for this transaction
    val transactionId = UUID.randomUUID().toString()
    val deferred = CompletableDeferred<Result<PaymentJsResponse>>()
    deferredCreated(transactionId, deferred)

    Log.d("PaymentsJsInjector", "Sending ${jsFunction.name.lowercase()} (ID: $transactionId)...")
    Log.d("PaymentsJsInjector", "Payload: $payloadLog")

    val jsFunctionCall =
        if (jsFunction == PaymentRequestType.GET_PLANS || jsFunction == PaymentRequestType.GET_SUBSCRIPTIONS) {
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

    withContext(Dispatchers.Main) {
        webView.evaluateJavascript(js, null)
    }

    return deferred.await()
}