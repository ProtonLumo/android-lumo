package me.proton.android.lumo.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.R
import kotlin.math.max
import kotlin.math.pow

@Composable
fun AudioWaveform(
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

@Composable
fun SpeechInputSheetContent(
    isListening: Boolean,
    partialSpokenText: String,
    rmsDbValue: Float,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
    speechStatusText: String,
    modifier: Modifier = Modifier
) {
    var elapsedTime by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = speechStatusText,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Row: Cancel, Waveform/Timer, Submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel Button
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.speech_sheet_cancel_desc),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Waveform and Timer Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                AudioWaveform(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    rmsDbValue = rmsDbValue,
                    maxBarHeight = 60f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Submit Button
            Button(
                onClick = { onSubmit(partialSpokenText) },
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(id = R.string.speech_sheet_submit_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display partial results (optional, could be hidden)
        Text(
            text = partialSpokenText.ifEmpty {
                if (isListening) stringResource(id = R.string.speech_sheet_listening)
                else stringResource(id = R.string.speech_sheet_waiting)
            },
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
    }
} 