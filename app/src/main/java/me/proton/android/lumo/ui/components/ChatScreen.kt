package me.proton.android.lumo.ui.components

import android.util.Log
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.theme.LumoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    webView: WebView,
    hasSeenLumoContainer: Boolean,
    shouldShowBackButton: Boolean,
    isLoading: Boolean,
    isLumoPage: Boolean,
    mainScreenListeners: MainScreenListeners,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (shouldShowBackButton) {
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
                                    mainScreenListeners.handleWebViewNavigation()
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
                                color = LumoTheme.colors.textNorm
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
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            val showLoading = isLoading && !hasSeenLumoContainer && isLumoPage
            Log.d(
                MainActivity.TAG,
                "Showing loading screen with fade transition - " +
                        "isLoading: $isLoading, " +
                        "hasSeenLumoContainer: $hasSeenLumoContainer, " +
                        "isLumoPage: $isLumoPage"
            )
            LoadingScreen(show = showLoading)
        }
    }
}

@Composable
private fun LoadingScreen(show: Boolean) {
    // Overlay LoadingScreen if loading (use only ViewModel state)
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(
            animationSpec = tween(150)
        ),
        exit = fadeOut(
            animationSpec = tween(200)
        )
    ) {
        LoadingScreen()
    }
}

class MainScreenListeners(
    val handleWebViewNavigation: () -> Unit,
)