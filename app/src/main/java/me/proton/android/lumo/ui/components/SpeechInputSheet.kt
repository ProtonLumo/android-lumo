package me.proton.android.lumo.ui.components

import android.Manifest
import android.app.Application
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.permission.rememberSinglePermission
import me.proton.android.lumo.speech.SpeechViewModel
import me.proton.android.lumo.ui.text.asString
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpeechSheet(
    onDismiss: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SpeechViewModel = hiltViewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onStartVoiceEntryRequested()

        try {
            sheetState.show()
            Log.d(MainActivity.TAG, "Effect: sheetState.show() finished.")
        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error showing bottom sheet", e)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.errors.collectLatest { error ->
                Toast.makeText(
                    context,
                    error.getText(context),
                    Toast.LENGTH_SHORT
                ).show()
                sheetState.hide()
                onDismiss()
            }
        }
    }

    val permission = rememberSinglePermission(
        permission = Manifest.permission.RECORD_AUDIO,
    )
    LaunchedEffect(permission.isGranted) {
        if (!permission.isGranted) {
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LumoTheme.colors.primary,
    ) {
        SpeechInputSheetContent(
            isListening = uiState.isListening,
            partialSpokenText = uiState.partialSpokenText,
            rmsDbValue = uiState.rmsDbValue,
            speechStatusText = uiState.speechStatusText.asString(),
            isVosk = uiState.isVosk,
            onCancel = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onCancelListening()
                }
                onDismiss()
            },
            onSubmit = {
                scope.launch {
                    sheetState.hide()
                }
                val spokenText = viewModel.onSubmitTranscription()
                onSubmitText(spokenText)
                onDismiss()
            }
        )
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
    isVosk: Boolean,
    modifier: Modifier = Modifier
) {
    var elapsedTime by remember { mutableIntStateOf(0) }

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