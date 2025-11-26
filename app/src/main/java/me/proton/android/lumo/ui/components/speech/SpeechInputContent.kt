package me.proton.android.lumo.ui.components.speech

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.components.waveform.OnDeviceAudioWaveform
import me.proton.android.lumo.ui.components.waveform.VoskAudioWaveform
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SpeechInputContent(
    isListening: Boolean,
    partialSpokenText: String,
    rmsDbValue: Float,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
    speechStatusText: String,
    isVosk: Boolean,
    modifier: Modifier = Modifier
) {
    var elapsedTime by remember { mutableIntStateOf(0) }

    // Start/stop timer based on isListening
    LaunchedEffect(isListening) {
        if (isListening) {
            elapsedTime = 0
            while (isActive) {
                delay(1_000)
                elapsedTime++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                LumoTheme.colors.primary,
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
                if (isVosk) {
                    VoskAudioWaveform(
                        maxBarHeight = 60f,
                        isListening = isListening,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    )
                } else {
                    OnDeviceAudioWaveform(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        rmsDbValue = rmsDbValue,
                        maxBarHeight = 60f
                    )
                }
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
                    tint = LumoTheme.colors.primary,
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