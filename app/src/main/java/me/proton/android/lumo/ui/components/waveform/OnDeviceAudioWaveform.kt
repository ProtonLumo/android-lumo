package me.proton.android.lumo.ui.components.waveform

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.pow

@Composable
fun OnDeviceAudioWaveform(
    modifier: Modifier = Modifier,
    rmsDbValue: Float,
    barCount: Int = 30,
    barColor: Color = Color.White,
    barWidth: Float = 6f,
    gapWidth: Float = 4f,
    maxBarHeight: Float = 100f, // Max height of a bar in dp
    minBarHeight: Float = 4f,   // Min height of a bar in dp
    smoothingFactor: Float = 0.6f // Adjusted smoothing again
) {
    val audioLevels =
        remember { mutableStateListOf<Float>().apply { addAll(List(barCount) { 0.05f }) } }

    LaunchedEffect(rmsDbValue) {
        val minDb = -2f
        val maxDb = 10f
        val normalized = ((rmsDbValue - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)

        val curvedValue = normalized.pow(0.7f)
        val lastValue = audioLevels.lastOrNull() ?: 0.05f
        val smoothedValue = lastValue * smoothingFactor + curvedValue * (1f - smoothingFactor)
        val finalValue = smoothedValue.coerceIn(0.05f, 1.0f)

        // Log intermediate values
        // Limit logging frequency if needed, but let's see full data first
        Log.d(
            "AudioWaveform",
            "rmsDb: %.2f -> norm: %.2f -> curved: %.2f -> smoothed: %.2f -> final: %.2f".format(
                rmsDbValue, normalized, curvedValue, smoothedValue, finalValue
            )
        )

        // Update the list: shift and add new value
        if (audioLevels.isNotEmpty()) { // Ensure list is not empty before removing
            audioLevels.removeAt(0) // Use compatible removeAt(0) instead of removeFirst()
        }
        audioLevels.add(finalValue)
    }

    Canvas(modifier = modifier.height(maxBarHeight.dp)) {
        val totalBarWidth = barWidth + gapWidth
        // Calculate the center offset to draw the bars in the middle
        val centerOffset = (size.width - (barCount * totalBarWidth - gapWidth)) / 2f

        audioLevels.forEachIndexed { index, level ->
            // Map the normalized level (0.0-1.0) to the actual bar height
            val barHeight = max(minBarHeight, level * maxBarHeight)
            val startX = centerOffset + index * totalBarWidth
            drawLine(
                color = barColor,
                start = Offset(x = startX, y = size.height / 2f - barHeight / 2f),
                end = Offset(x = startX, y = size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round // Rounded ends for the bars
            )
        }
    }
}