package me.proton.android.lumo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.billing.BillingDelegateImpl
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.managers.PermissionManager
import me.proton.android.lumo.managers.UIManager
import me.proton.android.lumo.managers.WebViewManager
import me.proton.android.lumo.ui.components.MainScreen
import me.proton.android.lumo.ui.components.MainScreenListeners
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.webview.addJavaScriptInterfaceSafely
import me.proton.android.lumo.webview.injectTheme
import me.proton.android.lumo.MainActivityViewModel.UiEvent as MainUiEvent


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    // Make viewModel accessible to WebAppInterface
    internal val mainActivityViewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory(application)
    }

    private lateinit var billingDelegate: BillingDelegateImpl
    private lateinit var webViewManager: WebViewManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var uiManager: UIManager
    private val _lottieComposition = MutableStateFlow<LottieComposition?>(null)
    private val lottieComposition: StateFlow<LottieComposition?> = _lottieComposition.asStateFlow()

    // Expose WebView for backward compatibility with existing code
    var webView: android.webkit.WebView?
        get() = webViewManager.webView
        set(value) {
            if (value != null) webViewManager.setWebView(value)
        }

    // Expose file path callback for backward compatibility
    var filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?
        get() = webViewManager.filePathCallback
        set(value) {
            webViewManager.filePathCallback = value
        }

    // Expose file chooser launcher for backward compatibility
    val fileChooserLauncher get() = permissionManager.fileChooserLauncher

    @SuppressLint("StateFlowValueCalledInComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        initializeManagers()

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewManager.canGoBack()) {
                    webViewManager.goBack()
                } else if (LumoConfig.isAccountDomain(webView?.url ?: "")) {
                    // Handles the case after the user logged out. In this case the log in page
                    // is displayed but the history was cleared, meaning that pressing back will
                    // close the app. However we do have the up navigation that will take the user
                    // to the Lumo screen. To keep thing consistent pressing back will also take the user
                    // to the Lumo screen.
                    webViewManager.loadUrl(LumoConfig.LUMO_URL)
                    webViewManager.clearHistory()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        Log.d(TAG, "onCreate called")
        Log.d(TAG, LumoConfig.getConfigInfo())

        LottieCompositionFactory
            .fromAsset(this, "lumo-loader.json")
            .addListener { composition ->
                _lottieComposition.value = composition
            }

        ServiceWorkerController.getInstance()
            .setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return null
                }
            })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.events.collectLatest { event ->
                    when (event) {
                        is MainUiEvent.EvaluateJavascript -> {
                            Log.d(TAG, "Received EvaluateJavascript event")
                            webViewManager.evaluateJavaScript(event.script) { result ->
                                mainActivityViewModel.handleJavascriptResult(result)
                            }
                        }

                        is MainUiEvent.ShowToast -> {
                            Log.d(TAG, "Received ShowToast event: ${event.message}")
                            Toast.makeText(
                                this@MainActivity,
                                event.message.getText(this@MainActivity),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }

                        is MainUiEvent.RequestAudioPermission -> {
                            Log.d(TAG, "Received RequestAudioPermission event")
                            permissionManager.requestRecordAudioPermission()
                        }
                    }
                }
            }
        }

        // Trigger the initial network connectivity check (independent of billing)
        mainActivityViewModel.performInitialNetworkCheck()

        // Add a global safety timer to ensure loading screen doesn't get stuck
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Global safety timeout reached for loading screen")
            val currentState = mainActivityViewModel.uiState.value
            if (currentState.isLoading) {
                Log.d(TAG, "Forcing loading screen to hide from global timer")
                mainActivityViewModel._uiState.update {
                    it.copy(isLoading = false, hasSeenLumoContainer = true)
                }
            }
        }, 5000) // Reduced to 5 seconds for faster fallback

        enableEdgeToEdge()
        setContent {
            val uiState by mainActivityViewModel.uiState.collectAsStateWithLifecycle()
            val initialUrl by mainActivityViewModel.initialUrl.collectAsStateWithLifecycle()

            val isDarkTheme = uiState.theme?.let { theme ->
                when (theme) {
                    is LumoTheme.System -> {
                        webViewManager.webView?.let {
                            injectTheme(
                                webView = it,
                                theme = if (isSystemInDarkTheme()) 15 else 14,
                                mode = 0
                            )
                        }
                        isSystemInDarkTheme()
                    }

                    is LumoTheme.Light -> {
                        webViewManager.webView?.let {
                            injectTheme(
                                webView = it,
                                theme = 14,
                                mode = 2
                            )
                        }
                        false
                    }

                    is LumoTheme.Dark -> {
                        webViewManager.webView?.let {
                            injectTheme(
                                webView = it,
                                theme = 15,
                                mode = 1
                            )
                        }
                        true
                    }
                }
            } ?: isSystemInDarkTheme()

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            Color.Transparent.toArgb(),
                        )
                    } else {
                        SystemBarStyle.light(
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb()
                        )

                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            Color.Transparent.toArgb(),
                        )
                    } else {
                        SystemBarStyle.light(
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb()
                        )
                    }
                )
            }

            LumoTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    uiState = uiState,
                    initialUrl = initialUrl,
                    lottieComposition = lottieComposition.collectAsStateWithLifecycle().value,
                    mainScreenListeners = MainScreenListeners(
                        handlePaymentDialog = {
                            webView?.let {
                                billingDelegate.ShowPaymentOrError(
                                    uiState = uiState,
                                    isDarkMode = isDarkTheme,
                                    webView = it,
                                    onDismiss = {
                                        mainActivityViewModel.dismissPaymentDialog()
                                    },
                                    onOpenUrl = { url ->
                                        startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                url.toUri()
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                        mainActivityViewModel.dismissPaymentDialog()
                                    }
                                )
                            }
                        },
                        onWebViewCreated = {
                            webViewManager.setWebView(it)
                            try {
                                addJavaScriptInterfaceSafely(
                                    webView = it,
                                    mainViewModel = mainActivityViewModel,
                                    billingDelegate = billingDelegate
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "WebView factory: Error adding JavascriptInterface",
                                    e
                                )
                            }
                        },
                        handleWebViewNavigation = {
                            if (webViewManager.canGoBack()) {
                                webViewManager.goBack()
                            } else {
                                webViewManager.loadUrl(LumoConfig.LUMO_URL)
                                webViewManager.clearHistory()
                            }
                        },
                        onWebViewCleared = {
                            webViewManager.clearHistory()
                        },
                        cancelSpeech = {
                            mainActivityViewModel.onCancelListening()
                        },
                        submitSpeechTranscript = {
                            mainActivityViewModel.onSubmitTranscription()
                        },
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewManager.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        uiManager.onConfigurationChanged(newConfig)
        webView?.invalidate()
    }

    /**
     * Initialize all manager instances
     */
    private fun initializeManagers() {
        // Initialize UI manager first to set up edge-to-edge and status bar
        uiManager = UIManager(this)
        uiManager.initializeUI()

        // Initialize WebView manager first
        webViewManager = WebViewManager()

        // Initialize permission manager with callback for permission results and WebView manager
        permissionManager = PermissionManager(this, { permission, isGranted ->
            handlePermissionResult(permission, isGranted)
        }, webViewManager)

        // Get BillingManagerWrapper from dependency provider
        billingDelegate = BillingDelegateImpl()
        billingDelegate.initialise(activity = this)

        Log.d(TAG, "All managers initialized successfully")
    }

    /**
     * Handle permission results from PermissionManager
     */
    private fun handlePermissionResult(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.RECORD_AUDIO -> {
                mainActivityViewModel.updatePermissionStatus() // Update ViewModel's knowledge regardless
                if (isGranted) {
                    Log.d(
                        TAG,
                        "RECORD_AUDIO permission granted by user, re-triggering voice entry request"
                    )
                    mainActivityViewModel.onStartVoiceEntryRequested()
                } else {
                    Log.w(TAG, "RECORD_AUDIO permission denied by user")
                    mainActivityViewModel.viewModelScope.launch {
                        mainActivityViewModel._eventChannel.send(
                            MainUiEvent.ShowToast(
                                UiText.ResText(R.string.permission_mic_rationale)
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}