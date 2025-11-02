package me.proton.android.lumo.speech

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import me.proton.android.lumo.speech.recognizer.LumoSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.OnDeviceSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.VoskSpeechRecognizer
import me.proton.android.lumo.ui.text.UiText

private const val TAG = "SpeechRecognitionManager"

/**
 * Handles speech recognition functionality.
 */
class SpeechRecognitionManager(private val context: Context) {

    // Listener to communicate with the UI layer
    interface SpeechRecognitionListener {
        fun onReadyForSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onEndOfSpeech()
        fun onError(errorMessage: UiText)
        fun onPartialResults(text: String)
        fun onResults(text: String)
    }

    private val speechRecognizer: LumoSpeechRecognizer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isOnDeviceRecognitionAvailable()) {
                OnDeviceSpeechRecognizer(context)
            } else {
                VoskSpeechRecognizer(context)
            }
        } else {
            VoskSpeechRecognizer(context)
        }

    fun setListener(listener: SpeechRecognitionListener) {
        speechRecognizer.setListener(listener)
    }

    fun removeListener() {
        speechRecognizer.removeListener()
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        speechRecognizer.isSpeechRecognitionAvailable()

    fun isOnDeviceRecognitionAvailable(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }

    fun startListening() {
        speechRecognizer.startListening()
    }

    fun cancelListening() {
        speechRecognizer.cancelListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }

    fun isVosk(): Boolean =
        speechRecognizer is VoskSpeechRecognizer
}