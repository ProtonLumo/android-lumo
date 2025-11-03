package me.proton.android.lumo.speech.recognizer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresApi
import me.proton.android.lumo.R
import me.proton.android.lumo.speech.SpeechRecognitionManager
import me.proton.android.lumo.speech.SpeechRecognitionManager.SpeechRecognitionListener
import me.proton.android.lumo.ui.text.UiText

@RequiresApi(Build.VERSION_CODES.S)
class OnDeviceSpeechRecognizer(
    private val context: Context
) : LumoSpeechRecognizer {

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SpeechRecognitionListener? = null

    init {
        try {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            setupSpeechRecognizerListener()
            Log.d(TAG, "SpeechRecognizer initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer not available on this device.")
            speechRecognizer = null
        }
    }

    private fun setupSpeechRecognizerListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "SpeechRecognizer: onReadyForSpeech")
                listener?.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {
                listener?.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "SpeechRecognizer: onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: onEndOfSpeech")
                listener?.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "SpeechRecognizer: onError: $errorMessage (code: $error)")
                listener?.onError(UiText.StringText(errorMessage))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.getOrNull(0)
                Log.d(TAG, "SpeechRecognizer: onResults: $text")
                if (text != null) {
                    listener?.onResults(text)
                } else {
                    listener?.onError(UiText.ResText(R.string.speech_error_no_match))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.getOrNull(0)
                if (text != null) {
                    listener?.onPartialResults(text)
                    Log.d(TAG, "SpeechRecognizer: onPartialResults: $text")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "SpeechRecognizer: onEvent: $eventType")
            }
        })
    }

    override fun setListener(listener: SpeechRecognitionManager.SpeechRecognitionListener) {
        this.listener = listener
    }

    override fun removeListener() {
        this.listener = null
    }

    override fun isSpeechRecognitionAvailable(): Boolean =
        speechRecognizer != null

    override fun startListening() {
        if (speechRecognizer == null) {
            Log.e(TAG, "SpeechRecognizer not initialized.")
            listener?.onError(
                UiText.ResText(R.string.speech_not_available)
            )
            return
        }

        Log.d(TAG, "Explicitly calling speechRecognizer.cancel() before starting")
        speechRecognizer?.cancel() // Explicitly cancel any previous recognition

        Log.d(TAG, "Starting speech recognition listener")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            val locale = java.util.Locale.getDefault()
            Log.d(TAG, "Requesting speech recognition for locale: $locale")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            // Don't set prefer offline, rely on system default based on availability check
        }

        try {
            Log.d(TAG, "Calling speechRecognizer.startListening...")
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "speechRecognizer.startListening call finished.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling speechRecognizer.startListening", e)
            listener?.onError(
                e.message?.let {
                    UiText.StringText(it)
                } ?: UiText.ResText(R.string.speech_error_client)
            )
        }

    }

    override fun cancelListening() {
        Log.d(TAG, "Cancelling speech recognition")
        speechRecognizer?.cancel()
    }

    override fun destroy() {
        Log.d(TAG, "Destroying speech recognizer")
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_error_audio)
            SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_error_client)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.speech_error_insufficient_permissions)
            SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.speech_error_network)
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.speech_error_network_timeout)
            SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.speech_error_no_match)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.speech_error_recognizer_busy)
            SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_error_server)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.speech_error_speech_timeout)
            else -> context.getString(R.string.speech_error_unknown)
        }
    }

    companion object {
        private const val TAG = "OnDeviceSpeechRecognizer"
    }
}