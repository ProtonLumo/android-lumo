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
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.managers.PermissionManager
import me.proton.android.lumo.managers.WebViewManager
import me.proton.android.lumo.navigation.NavRoutes
import me.proton.android.lumo.navigation.paymentRoutes
import me.proton.android.lumo.permission.rememberSinglePermission
import me.proton.android.lumo.ui.components.ChatScreen
import me.proton.android.lumo.ui.components.PurchaseLinkDialog
import me.proton.android.lumo.ui.components.SpeechSheet
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.webview.LumoChromeClient
import me.proton.android.lumo.webview.LumoWebClient
import me.proton.android.lumo.webview.createWebView
import me.proton.android.lumo.webview.injectSpokenText
import me.proton.android.lumo.MainActivityViewModel.UiEvent as MainUiEvent


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    internal val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory()
    }

    private lateinit var webViewManager: WebViewManager
    private lateinit var permissionManager: PermissionManager
    private val webBridge = DependencyProvider.getWebBridge()

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
                } else if (LumoConfig.isAccountDomain(webViewManager.currentUrl() ?: "")) {
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


        ServiceWorkerController.getInstance()
            .setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return null
                }
            })
        // Trigger the initial network connectivity check (independent of billing)
        viewModel.performInitialNetworkCheck()

        // Add a global safety timer to ensure loading screen doesn't get stuck
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Global safety timeout reached for loading screen")
            val currentState = viewModel.uiState.value
            if (currentState.isLoading) {

                Log.d(TAG, "Forcing loading screen to hide from global timer")
                viewModel.hideLoading()
            }
        }, 5000) // Reduced to 5 seconds for faster fallback

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val initialUrl by viewModel.initialUrl.collectAsStateWithLifecycle()
            val lumoWebClient = LumoWebClient(
                isLoading = { uiState.isLoading },
                showLoading = { viewModel.showLoading() },
                hideLoading = { viewModel.hideLoading(it) }
            )

            val lumoChromeClient = LumoChromeClient(showFileChooser = ::showFileChooser)

            val webView = remember {
                createWebView(
                    context = this,
                    initialUrl = initialUrl,
                    lumoWebClient = lumoWebClient,
                    lumoChromeClient = lumoChromeClient,
                    onAttach = { webBridge.attachWebView(it) },
                    keyboardVisibilityChanged = { isVisible, keyboardHeight ->
                        viewModel.onKeyboardVisibilityChanged(
                            isVisible = isVisible,
                            keyboardHeightPx = keyboardHeight
                        )
                    }
                )
            }
            webViewManager.setWebView(webView)

            viewModel.setupThemeUpdates(isSystemInDarkTheme())
            viewModel.attachPermissionContract(
                permissionContract = rememberSinglePermission(
                    permission = Manifest.permission.RECORD_AUDIO,
                    onGranted = { viewModel.startVoiceEntry() },
                    onDenied = { }
                )
            )

            val isDarkTheme = uiState.theme?.let { theme ->
                when (theme) {
                    is AppStyle.System -> isSystemInDarkTheme()
                    is AppStyle.Dark -> true
                    is AppStyle.Light -> false
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
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.events.collectLatest { event ->
                            when (event) {
                                is MainUiEvent.EvaluateJavascript -> {
                                    Log.d(TAG, "Received EvaluateJavascript event")
                                    webViewManager.evaluateJavaScript(event.script) { result ->
                                        viewModel.handleJavascriptResult(result)
                                    }
                                }

                                is MainUiEvent.ShowToast -> {
                                    Log.d(TAG, "Received ShowToast event: ${event.message}")
                                    Toast.makeText(
                                        this@MainActivity,
                                        event.message.getText(this@MainActivity),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                is MainUiEvent.ShowPaymentDialog -> {
                                    if (DependencyProvider.isPaymentAvailable()) {
                                        navController.navigate(
                                            NavRoutes.Subscription(
                                                event.paymentEvent
                                            )
                                        )
                                    } else {
                                        navController.navigate(NavRoutes.NoPayment)
                                    }
                                }

                                is MainUiEvent.ShowSpeechSheet -> {
                                    navController.navigate(NavRoutes.SpeechToText)
                                }
                            }
                        }
                    }
                }
                MainScreen(
                    navController = navController,
                    uiState = uiState,
                    webView = webView
                )
            }
        }
    }

    @Composable
    private fun MainScreen(
        navController: NavHostController,
        uiState: MainUiState,
        webView: WebView,
    ) {
        LaunchedEffect(uiState.hasSeenLumoContainer) {
            if (uiState.hasSeenLumoContainer) {
                webViewManager.clearHistory()
            }
        }

        NavHost(
            navController = navController,
            startDestination = NavRoutes.Chat
        ) {
            composable<NavRoutes.Chat> {
                ChatScreen(
                    webView = webView,
                    hasSeenLumoContainer = uiState.hasSeenLumoContainer,
                    shouldShowBackButton = uiState.shouldShowBackButton,
                    isLoading = uiState.isLoading,
                    isLumoPage = uiState.isLumoPage,
                    handleWebViewNavigation = remember {
                        {
                            if (webViewManager.canGoBack()) {
                                webViewManager.goBack()
                            } else {
                                webViewManager.loadUrl(LumoConfig.LUMO_URL)
                                webViewManager.clearHistory()
                            }
                        }
                    },
                )
            }
            paymentRoutes(
                isReady = !uiState.isLoading && uiState.hasSeenLumoContainer,
                onDismiss = { navController.popBackStack() }
            )
            dialog<NavRoutes.NoPayment> {
                PurchaseLinkDialog(
                    onOpenUrl = {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                it.toUri()
                            )
                        )
                    },
                    onDismissRequest = { navController.popBackStack() },
                )
            }
            dialog<NavRoutes.SpeechToText> {
                SpeechSheet(
                    onDismiss = { navController.popBackStack() },
                    onSubmitText = { injectSpokenText(webView = webView, text = it) }
                )
            }
        }
    }

    private fun showFileChooser() {
        filePathCallback = filePathCallback
        val intent =
            Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        fileChooserLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewManager.destroy()
        webBridge.detachWebView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        webViewManager.invalidate()
    }

    /**
     * Initialize all manager instances
     */
    private fun initializeManagers() {
        // Initialize WebView manager first
        webViewManager = WebViewManager()

        // Initialize permission manager with callback for permission results and WebView manager
        permissionManager = PermissionManager(
            activity = this,
            webViewManager = webViewManager
        )

        Log.d(TAG, "All managers initialized successfully")
    }

    companion object {
        const val TAG = "MainActivity"
    }
}