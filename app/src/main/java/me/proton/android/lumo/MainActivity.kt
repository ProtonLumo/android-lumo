package me.proton.android.lumo

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.managers.WebViewManager
import me.proton.android.lumo.navigation.NavRoutes
import me.proton.android.lumo.navigation.paymentRoutes
import me.proton.android.lumo.permission.rememberSinglePermission
import me.proton.android.lumo.review.InAppReviewManager
import me.proton.android.lumo.ui.components.ChatScreen
import me.proton.android.lumo.ui.components.ChatScreenFlags
import me.proton.android.lumo.ui.components.dialog.PermissionDialog
import me.proton.android.lumo.ui.components.dialog.PurchaseLinkDialog
import me.proton.android.lumo.ui.components.speech.SpeechSheet
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.usecase.IsPaymentAvailableUseCase
import me.proton.android.lumo.utils.openExternalUrl
import me.proton.android.lumo.utils.openSettings
import me.proton.android.lumo.webview.LumoChromeClient
import me.proton.android.lumo.webview.LumoWebClient
import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.webview.createWebView
import timber.log.Timber
import javax.inject.Inject
import me.proton.android.lumo.MainActivityViewModel.UiEvent as MainUiEvent


@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var webBridge: WebAppInterface
    @Inject
    lateinit var isPaymentAvailable: IsPaymentAvailableUseCase
    @Inject
    lateinit var activityProvider: ActivityProvider
    @Inject
    lateinit var inAppReviewManager: InAppReviewManager
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var webViewManager: WebViewManager

    @SuppressLint("StateFlowValueCalledInComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.startAnalytics()
        webViewManager = WebViewManager()
        val lumoChromeClient = LumoChromeClient(
            activity = this,
            errorHandler = { showToast(uiText = it) }
        )

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

        Timber.tag(TAG).i("onCreate called")
        Timber.tag(TAG).i("LumoConfig.getConfigInfo()")

        // Trigger the initial network connectivity check (independent of billing)
        viewModel.performInitialNetworkCheck()

        setContent {
            MainScreen(lumoChromeClient)
        }
    }

    @Composable
    private fun MainScreen(lumoChromeClient: LumoChromeClient) {
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
                context = this,
                isDarkThemeProvider = { isDarkTheme },
                isLoading = { uiState.isLoading },
                showLoading = { viewModel.showLoading() },
                hideLoading = { viewModel.hideLoading(it) },
                onError = { showToast(it) },
            )
        }

        val webView = rememberWebView(initialUrl, lumoWebClient, lumoChromeClient)

        webViewManager.setWebView(webView)

        viewModel.setupThemeUpdates(isSystemInDarkTheme)
        viewModel.attachPermissionContract(
            permissionContract = rememberSinglePermission(
                permission = Manifest.permission.RECORD_AUDIO,
                onGrant = { viewModel.startVoiceEntry() },
                onDeny = { viewModel.showMissingPermission(it) }
            )
        )
        DisposableEffect(Unit) {
            onDispose {
                viewModel.detachPermissionContract()
            }
        }

        LaunchedEffect(isDarkTheme) {
            enableEdgeToEdge(
                statusBarStyle = systemBarStyle(isDarkTheme),
                navigationBarStyle = systemBarStyle(isDarkTheme)
            )
        }

        LumoTheme(darkTheme = isDarkTheme) {
            NavigationContainer(
                uiState = uiState,
                webView = webView
            )
        }
    }

    @Composable
    private fun rememberWebView(
        initialUrl: String,
        lumoWebClient: LumoWebClient,
        lumoChromeClient: LumoChromeClient
    ): WebView =
        remember {
            createWebView(
                context = this,
                initialUrl = initialUrl,
                lumoWebClient = lumoWebClient,
                lumoChromeClient = lumoChromeClient,
                onAttach = { webBridge.attachWebView(it) },
            )
        }

    private fun systemBarStyle(isDarkTheme: Boolean): SystemBarStyle =
        if (isDarkTheme) {
            SystemBarStyle.dark(
                Color.Transparent.toArgb(),
            )
        } else {
            SystemBarStyle.light(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        }

    private fun handleUiEvent(
        event: MainUiEvent,
        onRoute: (NavRoutes) -> Unit,
    ) {
        when (event) {
            is MainUiEvent.EvaluateJavascript -> {
                Timber.tag(TAG).i("Received EvaluateJavascript event")
                webViewManager.evaluateJavaScript(event.script) { result ->
                    viewModel.handleJavascriptResult(result)
                }
            }

            is MainUiEvent.ShowToast -> {
                Timber.tag(TAG).i("Received ShowToast event: ${event.message}")
                showToast(event.message)
            }

            is MainUiEvent.ShowPaymentDialog -> {
                if (isPaymentAvailable()) {
                    onRoute(
                        NavRoutes.Subscription(
                            event.paymentEvent
                        )
                    )
                } else {
                    onRoute(NavRoutes.NoPayment)
                }
            }

            is MainUiEvent.ShowSpeechSheet -> {
                onRoute(NavRoutes.SpeechToText)
            }

            is MainUiEvent.MissingPermission -> {
                onRoute(
                    NavRoutes.MissingPermission(
                        event.missingPermission
                    )
                )
            }
        }
    }

    @Composable
    fun NavigationContainer(
        uiState: MainUiState,
        webView: WebView,
    ) {
        val navController = rememberNavController()

        LaunchedEffect(Unit) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    handleUiEvent(event) { navController.navigate(it) }
                }
            }
        }

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
                    chatScreenFlags = ChatScreenFlags(
                        hasSeenLumoContainer = uiState.hasSeenLumoContainer,
                        shouldShowBackButton = uiState.shouldShowBackButton,
                        isLoading = uiState.isLoading,
                        isLumoPage = uiState.isLumoPage,
                    ),
                )
            }
            paymentRoutes(onDismiss = { navController.popBackStack() })
            dialog<NavRoutes.NoPayment> {
                PurchaseLinkDialog(
                    onOpenUrl = { openExternalUrl(it) },
                    onDismissRequest = { navController.popBackStack() },
                )
            }
            dialog<NavRoutes.SpeechToText> {
                SpeechSheet(onDismiss = { navController.popBackStack() })
            }
            dialog<NavRoutes.MissingPermission> {
                PermissionDialog(
                    openSettings = {
                        openSettings()
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
//        inAppReviewManager.start(this) todo; keep disabled til further notice
    }

    override fun onResume() {
        super.onResume()
        activityProvider.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        activityProvider.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.cancelAnalytics()
//        inAppReviewManager.stop() todo; keep disabled til further notice
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

    private fun showToast(uiText: UiText) {
        Toast.makeText(
            this@MainActivity,
            uiText.getText(this),
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
