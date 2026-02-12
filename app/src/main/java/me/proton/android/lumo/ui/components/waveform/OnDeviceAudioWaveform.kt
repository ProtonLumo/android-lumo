package me.proton.android.lumo.ui.components.waveform

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
import timber.log.Timber
import kotlin.math.max
import kotlin.math.pow

private const val MIN_BAR_HEIGHT = 0.05f
private const val MAX_BAR_HEIGHT = 1.0f
private const val MIN_DB = -2f
private const val MAX_DB = 10f
private const val CURVE_POWER = 0.7f

@Composable
fun OnDeviceAudioWaveform(
    rmsDbValue: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 30,
    barColor: Color = Color.White,
    barWidth: Float = 6f,
    gapWidth: Float = 4f,
    maxBarHeight: Float = 100f, // Max height of a bar in dp
    minBarHeight: Float = 4f,   // Min height of a bar in dp
    smoothingFactor: Float = 0.6f // Adjusted smoothing again
) {
    val audioLevels =
        remember { mutableStateListOf<Float>().apply { addAll(List(barCount) { MIN_BAR_HEIGHT }) } }

    LaunchedEffect(rmsDbValue) {
        val normalized = ((rmsDbValue - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f)

        val curvedValue = normalized.pow(CURVE_POWER)
        val lastValue = audioLevels.lastOrNull() ?: MIN_BAR_HEIGHT
        val smoothedValue = lastValue * smoothingFactor + curvedValue * (1f - smoothingFactor)
        val finalValue = smoothedValue.coerceIn(MIN_BAR_HEIGHT, MAX_BAR_HEIGHT)

        // Log intermediate values
        // Limit logging frequency if needed, but let's see full data first
        Timber.tag("AudioWaveform")
            .i(
                "rmsDb: %.2f -> norm: %.2f -> curved: %.2f -> smoothed: %.2f -> final: %.2f"
                    .format(
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
