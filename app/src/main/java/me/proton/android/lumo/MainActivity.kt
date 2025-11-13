package me.proton.android.lumo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
    private val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory()
    }
    private lateinit var webViewManager: WebViewManager
    private val webBridge = DependencyProvider.getWebBridge()

    @SuppressLint("StateFlowValueCalledInComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webViewManager = WebViewManager()
        val lumoChromeClient = LumoChromeClient(activity = this)

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

        // Trigger the initial network connectivity check (independent of billing)
        viewModel.performInitialNetworkCheck()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val initialUrl by viewModel.initialUrl.collectAsStateWithLifecycle()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val isDarkTheme by remember {
                derivedStateOf {
                    uiState.theme?.let { theme ->
                        when (theme) {
                            is AppStyle.System -> isSystemInDarkTheme
                            is AppStyle.Dark -> true
                            is AppStyle.Light -> false
                        }
                    } ?: isSystemInDarkTheme
                }
            }

            val lumoWebClient = remember {
                LumoWebClient(
                    isDarkThemeProvider = { isDarkTheme },
                    isLoading = { uiState.isLoading },
                    showLoading = { viewModel.showLoading() },
                    hideLoading = { viewModel.hideLoading(it) }
                )
            }

            val webView = remember {
                createWebView(
                    context = this,
                    initialUrl = initialUrl,
                    lumoWebClient = lumoWebClient,
                    lumoChromeClient = lumoChromeClient,
                    onAttach = { webBridge.attachWebView(it) },
                )
            }
            webViewManager.setWebView(webView)

            viewModel.setupThemeUpdates(isSystemInDarkTheme())
            viewModel.attachPermissionContract(
                permissionContract = rememberSinglePermission(
                    permission = Manifest.permission.RECORD_AUDIO,
                    onGranted = { viewModel.startVoiceEntry() },
                    onDenied = {
                        Toast.makeText(
                            this,
                            R.string.permission_mic_rationale,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            )
            DisposableEffect(Unit) {
                onDispose {
                    viewModel.detachPermissionContract()
                }
            }

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
    fun MainScreen(
        navController: NavHostController,
        uiState: MainUiState,
        webView: WebView,
    ) {
        LaunchedEffect(uiState.hasSeenLumoContainer) {
            if (uiState.hasSeenLumoContainer) {
                webView.clearHistory()
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
                            if (webView.canGoBack()) {
                                webView.goBack()
                            } else {
                                webView.loadUrl(LumoConfig.LUMO_URL)
                                webView.clearHistory()
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

    override fun onDestroy() {
        super.onDestroy()
        webViewManager.destroy()
        webBridge.detachWebView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        webViewManager.invalidate()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}