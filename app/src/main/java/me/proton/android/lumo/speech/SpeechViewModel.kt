package me.proton.android.lumo.speech

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText

class SpeechViewModel(application: Application) : ViewModel() {

    data class SpeechUiState(
        val isListening: Boolean = false,
        val partialSpokenText: String = "",
        val rmsDbValue: Float = 0f,
        val speechStatusText: UiText = UiText.StringText(""),
    )

    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()
    private val _errorChannel = Channel<UiText>()
    val errors = _errorChannel.receiveAsFlow()
    private val speechRecognitionManager = SpeechRecognitionManager(application)

    init {
        setupSpeechRecognition()
        determineSpeechStatusText()
    }

    private fun setupSpeechRecognition() {
        speechRecognitionManager.setListener(object :
            SpeechRecognitionManager.SpeechRecognitionListener {
            override fun onReadyForSpeech() {
                _uiState.update { it.copy(isListening = true) }
            }

            override fun onBeginningOfSpeech() {
                // Nothing to do here
            }

            override fun onRmsChanged(rmsdB: Float) {
                _uiState.update { it.copy(rmsDbValue = rmsdB) }
            }

            override fun onEndOfSpeech() {
                _uiState.update { it.copy(isListening = false) }
            }

            override fun onError(errorMessage: UiText) {
                _uiState.update { it.copy(isListening = false) }
                viewModelScope.launch {
                    _errorChannel.send(errorMessage)
                }
            }

            override fun onPartialResults(text: String) {
                _uiState.update { it.copy(partialSpokenText = text) }
            }

            override fun onResults(text: String) {
                _uiState.update { it.copy(partialSpokenText = text, isListening = false) }
            }
        })
    }

    fun onStartVoiceEntryRequested() {
        Log.d(TAG, "onStartVoiceEntryRequested")
        if (!speechRecognitionManager.isSpeechRecognitionAvailable()) {
            viewModelScope.launch {
                _errorChannel.send(
                    UiText.ResText(R.string.speech_not_available)
                )
            }
            return
        }

        speechRecognitionManager.startListening()
    }

    private fun determineSpeechStatusText() {
        val statusText = if (speechRecognitionManager.isOnDeviceRecognitionAvailable()) {
            Log.d(TAG, "On-device recognition IS available.")
            UiText.ResText(R.string.speech_status_on_device)
        } else {
            Log.d(TAG, "On-device recognition NOT available.")
            UiText.ResText(R.string.speech_status_network)
        }
        _uiState.value = _uiState.value.copy(speechStatusText = statusText)
    }

    fun onCancelListening() {
        Log.d(TAG, "onCancelListening")
        speechRecognitionManager.cancelListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            partialSpokenText = ""
        )
    }

    fun onSubmitTranscription(): String {
        val transcript = _uiState.value.partialSpokenText
        Log.d(TAG, "onSubmitTranscription: $transcript")

        // Reset state immediately
        _uiState.value = _uiState.value.copy(isListening = false)
        speechRecognitionManager.cancelListening()

        return if (transcript.isNotEmpty()) {
            val escaped = transcript
                .replace("\\", "\\\\") // Must replace backslash first!
                .replace("\"", "\\\"") // Escape double quotes
                .replace("'", "\\'")   // Escape single quotes (optional but safe)
                .replace("\n", "\\n")  // Escape newlines
                .replace("\r", "\\r")  // Escape carriage returns
            "\"$escaped\""
        } else {
            Log.w(TAG, "Skipping submission, empty transcript")
            ""
        }.also {
            // Clear partial text after attempting submission
            _uiState.value = _uiState.value.copy(partialSpokenText = "")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.removeListener()
        speechRecognitionManager.destroy()
    }

    companion object {
        private const val TAG = "SpeechViewModel"
    }
}