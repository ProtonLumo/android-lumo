package me.proton.android.lumo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.theme.LumoTheme

@Preview
@Composable
fun LoadingScreen() {
    // Define the loading messages
    val loadingMessages = listOf(
        R.string.loading_message_1,
        R.string.loading_message_2,
        R.string.loading_message_3,
        R.string.loading_message_4,
        R.string.loading_message_5,
        R.string.loading_message_6,
        R.string.loading_message_7,
        R.string.loading_message_8,
        R.string.loading_message_9,
        R.string.loading_message_10,
        R.string.loading_message_11
    )

    // State to track current message index
    var currentMessageIndex by remember {
        mutableIntStateOf((loadingMessages.indices).random())
    }
    var lottieComposition by remember {
        mutableStateOf<LottieComposition?>(null)
    }

    // Effect to rotate messages every 4 seconds
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        LottieCompositionFactory
            .fromAsset(context, "lumo-loader.json")
            .addListener { composition -> lottieComposition = composition }

        while (true) {
            delay(4000) // 4 seconds
            // Pick a random message that's different from the current one
            val availableIndices = loadingMessages.indices.filter { it != currentMessageIndex }
            if (availableIndices.isNotEmpty()) {
                currentMessageIndex = availableIndices.random()
            } else {
                // Fallback if somehow we can't find a different index
                currentMessageIndex = (currentMessageIndex + 1) % loadingMessages.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumoTheme.colors.backgroundNorm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (lottieComposition != null) {
                val progress by animateLottieCompositionAsState(
                    lottieComposition,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { progress },
                    modifier = Modifier.size(180.dp)
                )
            } else {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lumo-loader.json"))
                val progress by animateLottieCompositionAsState(
                    composition,
                    iterations = LottieConstants.IterateForever
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(180.dp) // Adjust size as needed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main "Loading Lumo" text
            Text(
                text = stringResource(id = R.string.loading),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rotating loading message
            Text(
                text = stringResource(id = loadingMessages[currentMessageIndex]),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = LumoTheme.colors.textWeak
            )
        }
    }
}
