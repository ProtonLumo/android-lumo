package me.proton.android.lumo.ui.components.speech

import android.Manifest
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val viewModel: SpeechViewModel = hiltViewModel()

    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.errors.collectLatest { error ->
                Toast.makeText(
                    context,
                    error.getText(context),
                    Toast.LENGTH_SHORT
                ).show()
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { onDismiss() }
            }
        }
    }

    val permission = rememberSinglePermission(
        permission = Manifest.permission.RECORD_AUDIO,
    )

    LaunchedEffect(permission.isGranted) {
        if (!permission.isGranted) {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = LumoTheme.colors.primary,
        ) {
            SpeechInputContent(
                isListening = uiState.isListening,
                partialSpokenText = uiState.partialSpokenText,
                rmsDbValue = uiState.rmsDbValue,
                speechStatusText = uiState.speechStatusText.asString(),
                isVosk = uiState.isVosk,
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                onSubmit = {
                    val spokenText = viewModel.onSubmitTranscription()
                    onSubmitText(spokenText)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        showBottomSheet = true
        viewModel.onStartVoiceEntryRequested()
    }
}