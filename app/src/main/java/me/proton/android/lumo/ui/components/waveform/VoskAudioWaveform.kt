package me.proton.android.lumo.ui.components.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.pow

@Composable
fun VoskAudioWaveform(
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 30,
    barColor: Color = Color.White,
    barWidth: Float = 6f,
    gapWidth: Float = 4f,
    maxBarHeight: Float = 100f, // Max height of a bar in dp
    minBarHeight: Float = 4f,   // Min height of a bar in dp
    smoothingFactor: Float = 0.6f // Adjusted smoothing again
) {
    val scope = rememberCoroutineScope()
    val visualizer = remember { MicVisualizer() }

    var targetRms by remember { mutableFloatStateOf(0f) }
    var displayedRms by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isListening) {
        if (isListening) {
            visualizer.start(scope) { rms -> targetRms = rms }
        } else {
            visualizer.stop()
            targetRms = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            visualizer.stop()
        }
    }

    // Animate RMS at ~60 fps
    LaunchedEffect(Unit) {
        while (true) {
            displayedRms = lerp(displayedRms, targetRms, 0.2f)
            delay(16L)
        }
    }

    val audioLevels = remember {
        mutableStateListOf<Float>().apply {
            addAll(List(barCount) { 0.05f })
        }
    }

    LaunchedEffect(displayedRms) {
        val normalized = (displayedRms / 2000f).coerceIn(0f, 1f) // dynamic scale
        val curvedValue = normalized.pow(0.5f)
        val smoothed = audioLevels.last() * smoothingFactor + curvedValue * (1f - smoothingFactor)
        if (audioLevels.isNotEmpty()) audioLevels.removeAt(0)
        audioLevels.add(smoothed)
    }

    Canvas(modifier = modifier.height(maxBarHeight.dp)) {
        val totalBarWidth = barWidth + gapWidth
        val centerOffset = (size.width - (barCount * totalBarWidth - gapWidth)) / 2f
        audioLevels.forEachIndexed { i, level ->
            val barHeight = max(minBarHeight, level * maxBarHeight)
            val startX = centerOffset + i * totalBarWidth
            drawLine(
                color = barColor,
                start = Offset(startX, size.height / 2f - barHeight / 2f),
                end = Offset(startX, size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
