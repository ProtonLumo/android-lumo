package me.proton.android.lumo.featureflag

import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.GetFeaturesResponse
import me.proton.android.lumo.featureflag.model.PutFeatureResponse
import java.util.UUID

object LegacyFeatureFlagJsInjector {

    enum class FunctionCall(val functionName: String) {
        GET_FEATURE("getFeature"),
        GET_FEATURES("getFeatures"),
        UPDATE_FEATURE_VALUE("updateFeatureValue")
    }

    suspend fun getFeature(
        webView: WebView,
        featureId: FeatureId,
        deferredCreated: (String, CompletableDeferred<Result<GetFeaturesResponse>>) -> Unit,
    ): Result<GetFeaturesResponse> =
        webView.jsFunctionCall(
            featureId = featureId,
            functionCall = FunctionCall.GET_FEATURE,
            deferredCreated = deferredCreated
        )

    suspend fun getFeatures(
        webView: WebView,
        featureIds: List<FeatureId>,
        deferredCreated: (String, CompletableDeferred<Result<GetFeaturesResponse>>) -> Unit
    ) =
        webView.jsFunctionCall(
            featureIds = featureIds,
            functionCall = FunctionCall.GET_FEATURES,
            deferredCreated = deferredCreated
        )

    suspend fun updateFeatureValue(
        webView: WebView,
        featureId: FeatureId,
        isEnabled: Boolean,
        deferredCreated: (String, CompletableDeferred<Result<PutFeatureResponse>>) -> Unit
    ) =
        webView.jsFunctionCall(
            featureId = featureId,
            newValue = isEnabled,
            functionCall = FunctionCall.UPDATE_FEATURE_VALUE,
            deferredCreated = deferredCreated
        )

    private suspend fun <T> WebView.jsFunctionCall(
        featureId: FeatureId = FeatureFlag.DEFAULT.featureId,
        featureIds: List<FeatureId> = emptyList(),
        functionCall: FunctionCall,
        newValue: Boolean = false,
        deferredCreated: (String, CompletableDeferred<Result<T>>) -> Unit,
    ): Result<T> {
        val transactionId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<Result<T>>()
        deferredCreated(transactionId, deferred)

        val functionName = functionCall.functionName
        val jsFunctionCall = when (functionCall) {
            FunctionCall.GET_FEATURE ->
                "window.legacyFeatureFlagApiInstance.$functionName(\"${featureId.id}\", \"android\")"

            FunctionCall.GET_FEATURES ->
                "window.legacyFeatureFlagApiInstance.$functionName(${featureIds.toJsList()}, \"android\")"

            FunctionCall.UPDATE_FEATURE_VALUE ->
                "window.legacyFeatureFlagApiInstance.$functionName(\"${featureId.id}\", $newValue)"
        }

        val js = """
            (async function() {
                const txId = '$transactionId'; // Pass transactionId to JS
                try {
                    if (typeof Android === 'undefined' || typeof Android.postFeatureFlagResult !== 'function') {
                         console.error('Android.postFeatureFlagResult is not available. Cannot send result back.');
                         // If no callback was provided originally, this is fine. If one was, we can't fulfill it.
                         return; // Exit early
                    }
                                    
                    if (window.legacyFeatureFlagApiInstance && typeof window.legacyFeatureFlagApiInstance.$functionName === 'function') {
                        const result = await $jsFunctionCall;
                        const resultJson = JSON.stringify({ status: 'success', data: result });
                        Android.postFeatureFlagResult(txId, resultJson, '${functionCall.name}');
                    } else {
                        const errorMsg = 'legacyFeatureFlagApiInstance or $functionName not found';
                        console.error(errorMsg);
                        const errorJson = JSON.stringify({ status: 'error', message: errorMsg });
                        Android.postFeatureFlagResult(txId, errorJson, '${functionCall.name}');
                    }
                } catch (e) {
                    const errorMessage = e instanceof Error ? e.message : String(e);
                    console.error('Error executing $functionName:', errorMessage);
                    const errorJson = JSON.stringify({ status: 'error', message: 'JS Error: ' + errorMessage });
                    // Ensure we still call back even on error
                    if (typeof Android !== 'undefined' && typeof Android.postFeatureFlagResult === 'function') {
                         Android.postFeatureFlagResult(txId, errorJson, '${functionCall.name}');
                    } else {
                         console.error('Android interface not available to report JS error.');
                    }
                }
            })();
        """.trimIndent()

        withContext(Dispatchers.Main) {
            evaluateJavascript(js, null)
        }

        return deferred.await()
    }

    private fun List<FeatureId>.toJsList(): String {
        val sb = StringBuilder()
        sb.append("[")
        this.forEach {
            sb.append("\"${it.id}\",")
        }
        sb.append("]")

        return sb.toString()
    }

}