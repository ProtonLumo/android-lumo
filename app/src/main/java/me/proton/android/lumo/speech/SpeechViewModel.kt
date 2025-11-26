package me.proton.android.lumo.speech

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.speech.SpeechRecognitionManager.Engine
import me.proton.android.lumo.ui.text.UiText
import javax.inject.Inject

@HiltViewModel
class SpeechViewModel @Inject constructor(
    application: Application
) : ViewModel() {

    data class SpeechUiState(
        val isListening: Boolean = false,
        val partialSpokenText: String = "",
        val rmsDbValue: Float = 0f,
        val speechStatusText: UiText = UiText.StringText(""),
        val isVosk: Boolean = false,
    )

    private val _errorChannel = Channel<UiText>()
    val errors = _errorChannel.receiveAsFlow()
    private val speechRecognitionManager = SpeechRecognitionManager(application)
    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    init {
        setupSpeechRecognition()
        determineSpeechStatusText()
    }

    private fun setupSpeechRecognition() {
        speechRecognitionManager.setListener(
            object : SpeechRecognitionManager.SpeechRecognitionListener {
                private var finalBuffer: String = ""

                override fun onReadyForSpeech() {
                    _uiState.update { it.copy(isListening = true) }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _uiState.update { it.copy(rmsDbValue = rmsdB) }
                }

                override fun onEndOfSpeech() {
                    _uiState.update { it.copy() }
                }

                override fun onError(
                    errorMessage: UiText,
                    isInitialisation: Boolean
                ) {
                    _uiState.update { it.copy(isListening = false) }
                    viewModelScope.launch {
                        _errorChannel.send(errorMessage)
                    }
                }

                override fun onPartialResults(text: String, isFinal: Boolean) {
                    _uiState.update {
                        val currentText = buildString {
                            append(finalBuffer)
                            if (finalBuffer.isNotEmpty()) append(" ")
                            append(text)
                        }
                        if (isFinal) {
                            finalBuffer = currentText
                        }
                        it.copy(
                            partialSpokenText = currentText,
                        )
                    }
                }

                override fun switched() {
                    determineSpeechStatusText()
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
        val statusText = when (speechRecognitionManager.engineState()) {
            is SpeechRecognitionManager.Engine.OnDevice ->
                UiText.ResText(R.string.speech_status_on_device)

            is SpeechRecognitionManager.Engine.GoogleCloud ->
                UiText.ResText(R.string.speech_status_google)

            else -> UiText.ResText(R.string.speech_status_vosk)
        }

        _uiState.update {
            it.copy(
                speechStatusText = statusText,
                isVosk = speechRecognitionManager.engineState() == Engine.Vosk,
            )
        }
    }

    fun onSubmitTranscription(): String {
        val transcript = _uiState.value.partialSpokenText
        Log.d(TAG, "onSubmitTranscription: $transcript")

        // Reset state immediately
        _uiState.value = _uiState.value.copy()
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
            _uiState.value = _uiState.value.copy()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.cancelListening()
        speechRecognitionManager.removeListener()
        speechRecognitionManager.destroy()
    }

    companion object {
        private const val TAG = "SpeechViewModel"
    }
}