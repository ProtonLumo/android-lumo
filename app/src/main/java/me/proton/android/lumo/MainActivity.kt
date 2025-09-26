package me.proton.android.lumo

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import me.proton.android.lumo.models.Feature
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.speech.SpeechRecognitionManager
import me.proton.android.lumo.ui.components.LoadingScreen
import me.proton.android.lumo.ui.components.SpeechInputSheetContent
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.ui.theme.Primary
import me.proton.android.lumo.webview.WebViewScreen
import me.proton.android.lumo.webview.injectTheme

private const val TAG = "MainActivity"


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    // Make viewModel accessible to WebAppInterface
    internal val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory(application)
    }

    private lateinit var billingDelegate: BillingDelegateImpl
    private lateinit var webViewManager: WebViewManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var uiManager: UIManager

    // Speech Recognition
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

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

        LottieCompositionFactory.fromAsset(this, "lumo-loader.json").addListener { composition ->
            _lottieComposition.value = composition
        }

        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(this)

        ServiceWorkerController.getInstance()
            .setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return null
                }
            })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is UiEvent.EvaluateJavascript -> {
                            Log.d(TAG, "Received EvaluateJavascript event")
                            webViewManager.evaluateJavaScript(event.script) { result ->
                                viewModel.handleJavascriptResult(result)
                            }
                        }

                        is UiEvent.ShowToast -> {
                            Log.d(TAG, "Received ShowToast event: ${event.message}")
                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG)
                                .show()
                        }

                        is UiEvent.RequestAudioPermission -> {
                            Log.d(TAG, "Received RequestAudioPermission event")
                            permissionManager.requestRecordAudioPermission()
                        }

                        is UiEvent.ForwardBillingResult -> {
                            // Previously done via MainActivity.postResult(); now fully event-driven.
                            billingDelegate.handleJavaScriptResult(
                                event.transactionId,
                                event.resultJson
                            )
                        }
                    }
                }
            }
        }

        // Trigger the initial network connectivity check (independent of billing)
        viewModel.performInitialNetworkCheck()

        // Add a global safety timer to ensure loading screen doesn't get stuck
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Global safety timeout reached for loading screen")
            val currentState = viewModel.uiState.value
            if (currentState.isLoading) {
                Log.d(TAG, "Forcing loading screen to hide from global timer")
                viewModel._uiState.update {
                    it.copy(isLoading = false, hasSeenLumoContainer = true)
                }
            }
        }, 5000) // Reduced to 5 seconds for faster fallback

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val initialUrl by viewModel.initialUrl.collectAsStateWithLifecycle()
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

            val barColor = MaterialTheme.colorScheme.background.toArgb()
            LaunchedEffect(isDarkTheme) {
                if (isDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(
                            barColor,
                        ),
                        navigationBarStyle = SystemBarStyle.dark(
                            barColor,
                        ),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            barColor, barColor,
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            barColor, barColor,
                        ),
                    )
                }
            }

            LumoTheme(darkTheme = isDarkTheme) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                LaunchedEffect(uiState.hasSeenLumoContainer) {
                    if (uiState.hasSeenLumoContainer) {
                        webViewManager.clearHistory()
                    }
                }

                LaunchedEffect(uiState.showSpeechSheet) {
                    Log.d(
                        TAG,
                        "LaunchedEffect(showSpeechSheet) triggered. showSpeechSheet = ${uiState.showSpeechSheet}"
                    )
                    scope.launch {
                        if (uiState.showSpeechSheet) {
                            Log.d(
                                TAG,
                                "Effect: showSpeechSheet is TRUE. Calling sheetState.show()..."
                            )
                            try {
                                sheetState.show()
                                Log.d(TAG, "Effect: sheetState.show() finished.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error showing bottom sheet", e)
                            }
                        } else {
                            Log.d(
                                TAG,
                                "Effect: showSpeechSheet is FALSE. Checking if sheet is visible..."
                            )
                            if (sheetState.isVisible) {
                                Log.d(TAG, "Effect: Sheet is visible. Calling sheetState.hide()...")
                                try {
                                    sheetState.hide()
                                    Log.d(TAG, "Effect: sheetState.hide() finished.")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error hiding bottom sheet", e)
                                }
                            } else {
                                Log.d(TAG, "Effect: Sheet is already hidden.")
                            }
                        }
                    }
                }

                if (uiState.showSpeechSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.onCancelListening() },
                        sheetState = sheetState,
                        containerColor = Primary,
                    ) {
                        SpeechInputSheetContent(
                            isListening = uiState.isListening,
                            partialSpokenText = uiState.partialSpokenText,
                            rmsDbValue = uiState.rmsDbValue,
                            speechStatusText = uiState.speechStatusText,
                            onCancel = { viewModel.onCancelListening() },
                            onSubmit = { viewModel.onSubmitTranscription() }
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (uiState.shouldShowBackButton) {
                            TopAppBar(
                                title = {},
                                navigationIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp)) // clip ripple to rounded shape
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(
                                                    // ripple params
                                                    bounded = true,
                                                )
                                            ) {
                                                Log.d(
                                                    TAG,
                                                    "Back button clicked, navigating to Lumo"
                                                )
                                                if (webViewManager.canGoBack()) {
                                                    webViewManager.goBack()
                                                } else {
                                                    webViewManager.loadUrl(LumoConfig.LUMO_URL)
                                                    webViewManager.clearHistory()
                                                }
                                            }
                                            .padding(all = 8.dp) // optional padding
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.lumo_icon),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.height(25.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(id = R.string.back_to_lumo),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Always show WebViewScreen if initialUrl is not null
                        if (initialUrl != null) {
                            WebViewScreen(
                                activity = this@MainActivity,
                                initialUrl = initialUrl!!, // Pass the determined URL
                                onWebViewCreated = { createdWebView ->
                                    webViewManager.setWebView(createdWebView)
                                    Log.d(TAG, "WebView created and stored in WebViewManager.")
                                    // Let the WebView client handle loading state transitions
                                    // Remove redundant timeout that causes race conditions
                                }
                            )
                        }

                        // Overlay LoadingScreen if loading (use only ViewModel state)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.isLoading && !uiState.hasSeenLumoContainer && uiState.isLumoPage,
                            enter = androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(150)
                            ),
                            exit = androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(200)
                            )
                        ) {
                            Log.d(
                                TAG,
                                "Showing loading screen with fade transition - isLoading: ${uiState.isLoading}, hasSeenLumoContainer: ${uiState.hasSeenLumoContainer}, isLumoPage: ${uiState.isLumoPage}"
                            )
                            LoadingScreen(lottieComposition.collectAsStateWithLifecycle().value)
                        }
                        if (initialUrl != null) {
                            Log.d(TAG, "Showing, or trying to show PaymentDialog. ")
                            billingDelegate.ShowPaymentOrError(uiState) {
                                viewModel.dismissPaymentDialog()
                            }
                        }
                        if (initialUrl == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text("Error determining initial URL.")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionManager.destroy() // Release the recognizer
        webViewManager.destroy()
    }

    fun getPlansFromWebView(
        webView: android.webkit.WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        billingDelegate.getPlansFromWebView(webView, callback)
    }

    fun getSubscriptionsFromWebView(
        webView: android.webkit.WebView,
        callback: ((Result<PaymentJsResponse>) -> Unit)? = null
    ) {
        billingDelegate.getSubscriptionsFromWebView(webView, callback)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        uiManager.onConfigurationChanged(newConfig)
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
                viewModel.updatePermissionStatus() // Update ViewModel's knowledge regardless
                if (isGranted) {
                    Log.d(
                        TAG,
                        "RECORD_AUDIO permission granted by user, re-triggering voice entry request"
                    )
                    viewModel.onStartVoiceEntryRequested()
                } else {
                    Log.w(TAG, "RECORD_AUDIO permission denied by user")
                    viewModel.viewModelScope.launch {
                        viewModel._eventChannel.send(UiEvent.ShowToast(getString(R.string.permission_mic_rationale)))
                    }
                }
            }
        }
    }

    companion object {
        private val features = listOf(
            Feature("Daily chats", false, true),
            Feature("Chat history", false, true),
            Feature("Starred chats", false, true),
            Feature("Large uploads", false, true),
            Feature("Priority access at peak times", false, true),
            Feature("Priority customer support", false, true)
        )
    }
}