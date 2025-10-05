package me.proton.android.lumo.ui.components

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import kotlinx.coroutines.launch
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.MainUiState
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.theme.Primary
import me.proton.android.lumo.webview.WebViewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    initialUrl: String?,
    lottieComposition: LottieComposition?,
    mainScreenListeners: MainScreenListeners,
) {

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.hasSeenLumoContainer) {
        if (uiState.hasSeenLumoContainer) {
            mainScreenListeners.onWebViewCleared?.invoke()
        }
    }

    LaunchedEffect(uiState.showSpeechSheet) {
        Log.d(
            MainActivity.TAG,
            "LaunchedEffect(showSpeechSheet) triggered. showSpeechSheet = ${uiState.showSpeechSheet}"
        )
        scope.launch {
            if (uiState.showSpeechSheet) {
                Log.d(
                    MainActivity.TAG,
                    "Effect: showSpeechSheet is TRUE. Calling sheetState.show()..."
                )
                try {
                    sheetState.show()
                    Log.d(MainActivity.TAG, "Effect: sheetState.show() finished.")
                } catch (e: Exception) {
                    Log.e(MainActivity.TAG, "Error showing bottom sheet", e)
                }
            } else {
                Log.d(
                    MainActivity.TAG,
                    "Effect: showSpeechSheet is FALSE. Checking if sheet is visible..."
                )
                if (sheetState.isVisible) {
                    Log.d(
                        MainActivity.TAG,
                        "Effect: Sheet is visible. Calling sheetState.hide()..."
                    )
                    try {
                        sheetState.hide()
                        Log.d(MainActivity.TAG, "Effect: sheetState.hide() finished.")
                    } catch (e: Exception) {
                        Log.e(MainActivity.TAG, "Error hiding bottom sheet", e)
                    }
                } else {
                    Log.d(MainActivity.TAG, "Effect: Sheet is already hidden.")
                }
            }
        }
    }

    if (uiState.showSpeechSheet) {
        ModalBottomSheet(
            onDismissRequest = { mainScreenListeners.cancelSpeech?.invoke() },
            sheetState = sheetState,
            containerColor = Primary,
        ) {
            SpeechInputSheetContent(
                isListening = uiState.isListening,
                partialSpokenText = uiState.partialSpokenText,
                rmsDbValue = uiState.rmsDbValue,
                speechStatusText = uiState.speechStatusText.asString(),
                onCancel = { mainScreenListeners.cancelSpeech?.invoke() },
                onSubmit = { mainScreenListeners.submitSpeechTranscript?.invoke() }
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
                                        MainActivity.TAG,
                                        "Back button clicked, navigating to Lumo"
                                    )
                                    mainScreenListeners.handleWebViewNavigation?.invoke()
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
            val mainActivity = LocalContext.current as MainActivity
            if (initialUrl != null) {
                WebViewScreen(
                    activity = mainActivity,
                    initialUrl = initialUrl, // Pass the determined URL
                    onWebViewCreated = { createdWebView ->
                        mainScreenListeners.onWebViewCreated?.invoke(createdWebView)
                        Log.d(MainActivity.TAG, "WebView created and stored in WebViewManager.")
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
                    MainActivity.TAG,
                    "Showing loading screen with fade transition - isLoading: ${uiState.isLoading}, hasSeenLumoContainer: ${uiState.hasSeenLumoContainer}, isLumoPage: ${uiState.isLumoPage}"
                )
                LoadingScreen(preloadedComposition = lottieComposition)
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

class MainScreenListeners(
    val onWebViewCreated: ((WebView) -> Unit)? = null,
    val handleWebViewNavigation: (() -> Unit)? = null,
    val onWebViewCleared: (() -> Unit)? = null,
    val cancelSpeech: (() -> Unit)? = null,
    val submitSpeechTranscript: (() -> Unit)? = null,
)