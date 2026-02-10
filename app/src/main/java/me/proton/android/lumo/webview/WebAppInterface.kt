package me.proton.android.lumo.webview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.featureflag.LegacyFeatureFlagJsInjector
import me.proton.android.lumo.featureflag.mapper.mapResponse
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.GetFeaturesResponse
import me.proton.android.lumo.featureflag.model.PutFeatureResponse
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

@SuppressLint("StaticFieldLeak")
@Suppress("TooManyFunctions")
open class WebAppInterface {

    @Volatile
    protected var webView: WebView? = null

    private val _mainEventChannel = Channel<MainWebEvent>()
    val mainEventChannel = _mainEventChannel.receiveAsFlow()
    private val pendingFeatureFlagResults =
        ConcurrentHashMap<String, CompletableDeferred<Result<GetFeaturesResponse>>>()
    private val pendingUpdateFeatureFlagResults =
        ConcurrentHashMap<String, CompletableDeferred<Result<PutFeatureResponse>>>()

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
            Timber.tag(TAG).i("JavaScript interface 'Android' added successfully")

            // Inject a simple test to verify interface is working
            webView.evaluateJavascript(
                "console.log('Android interface available:', typeof window.Android !== 'undefined');",
                null
            )
        } catch (e: IllegalStateException) {
            Timber.tag(MainActivity.TAG).e(e, "Illegal state adding JavaScript interface")
        } catch (e: SecurityException) {
            Timber.tag(MainActivity.TAG).e(e, "Security exception adding JavaScript interface")
        }
    }

    fun detachWebView() {
        webView?.removeJavascriptInterface("Android")
        webView = null
    }

    fun injectTheme(theme: Int, mode: Int) {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        injectTheme(webView = webView, theme = theme, mode = mode)
    }

    fun injectSpeechOutput(spokenText: String) {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        injectSpokenText(webView, spokenText)
    }

    @JavascriptInterface
    fun showPayment() {
        Timber.tag(TAG).i("showPayment called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.ShowPaymentRequested)
    }

    @JavascriptInterface
    fun showBlackFridaySale() {
        Timber.tag(TAG).i("showBlackFridaySale called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.ShowBlackFridaySale)
    }

    @JavascriptInterface
    fun startVoiceEntry() {
        Timber.tag(TAG).i("startVoiceEntry called from JavaScript")
        _mainEventChannel.trySend(MainWebEvent.StartVoiceEntryRequested)
    }

    @JavascriptInterface
    fun retryLoad() {
        Timber.tag(TAG).i("retryLoad called from JavaScript(error page)")
        _mainEventChannel.trySend(MainWebEvent.RetryLoadRequested)
    }

    @JavascriptInterface
    fun onPageTypeChanged(isLumo: Boolean, url: String) {
        Timber.tag(TAG).i("Page type changed : isLumo = $isLumo")
        _mainEventChannel.trySend(MainWebEvent.PageTypeChanged(isLumo, url))
    }

    @JavascriptInterface
    fun onNavigation(url: String, type: String) {
        Timber.tag(TAG).i("Navigation : url =$url, type = $type")
        _mainEventChannel.trySend(MainWebEvent.Navigated(url, type))
    }

    @JavascriptInterface
    fun onLumoContainerVisible() {
        Timber.tag(TAG).i("Lumo container became visible")
        _mainEventChannel.trySend(MainWebEvent.LumoContainerVisible)
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.tag(TAG).i("Web logs: $message")
    }

    @JavascriptInterface
    fun onThemeChanged(mode: String) {
        Timber.tag(TAG).i("onThemeChanged : $mode")
        _mainEventChannel.trySend(MainWebEvent.ThemeResult(mode))
    }

    @JavascriptInterface
    fun onThemeStyleChanged(themeStyle: String) {
        if (themeStyle.isNotEmpty()) {
            Timber.tag(TAG).i("onThemeChanged : $themeStyle")
            _mainEventChannel.trySend(MainWebEvent.ThemeResult(themeStyle))
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @JavascriptInterface
    fun postFeatureFlagResult(transactionId: String, resultJson: String, functionName: String) {
        Timber.tag(TAG).d("postFeatureFlagResult received for ID $transactionId: $resultJson")

        // Retrieve and remove the original callback
        val functionCall = LegacyFeatureFlagJsInjector.FunctionCall.valueOf(functionName)
        when (functionCall) {
            LegacyFeatureFlagJsInjector.FunctionCall.GET_FEATURE,
            LegacyFeatureFlagJsInjector.FunctionCall.GET_FEATURES -> {
                val deferred = pendingFeatureFlagResults.remove(transactionId)
                if (deferred == null) {
                    Timber.tag(TAG).e("No callback found for transaction ID: $transactionId")
                    return
                }
                deferred.mapResponse(json, resultJson)
            }

            LegacyFeatureFlagJsInjector.FunctionCall.UPDATE_FEATURE_VALUE -> {
                val deferred = pendingUpdateFeatureFlagResults.remove(transactionId)
                if (deferred == null) {
                    Timber.tag(TAG).e("No callback found for transaction ID: $transactionId")
                    return
                }
                deferred.mapResponse(json, resultJson)
            }
        }
    }

    suspend fun getFeature(featureId: FeatureId): Result<GetFeaturesResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return LegacyFeatureFlagJsInjector.getFeature(
            webView = webView,
            featureId = featureId,
        ) { transactionId, deferred ->
            pendingFeatureFlagResults[transactionId] = deferred
        }
    }

    suspend fun getFeatures(featureIds: List<FeatureId>): Result<GetFeaturesResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return LegacyFeatureFlagJsInjector.getFeatures(
            webView = webView,
            featureIds = featureIds,
        ) { transactionId, deferred ->
            pendingFeatureFlagResults[transactionId] = deferred
        }
    }

    suspend fun updateFeatureValue(
        featureId: FeatureId,
        isEnabled: Boolean,
    ): Result<PutFeatureResponse> {
        val webView = webView ?: throw IllegalStateException("WebView not attached")

        return LegacyFeatureFlagJsInjector.updateFeatureValue(
            webView = webView,
            featureId = featureId,
            isEnabled = isEnabled,
        ) { transactionId, deferred ->
            pendingUpdateFeatureFlagResults[transactionId] = deferred
        }
    }

    companion object {
        protected const val TAG = "WebAppInterface"
    }
}
