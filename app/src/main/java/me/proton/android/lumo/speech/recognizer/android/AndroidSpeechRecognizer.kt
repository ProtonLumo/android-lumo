package me.proton.android.lumo.speech.recognizer.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import me.proton.android.lumo.R
import me.proton.android.lumo.speech.SpeechRecognitionManager.SpeechRecognitionListener
import me.proton.android.lumo.speech.recognizer.LumoSpeechRecognizer
import me.proton.android.lumo.ui.text.UiText
import java.util.Locale

abstract class AndroidSpeechRecognizer(private val context: Context) : LumoSpeechRecognizer {

    protected open fun extraErrors(): Set<Int> = emptySet()
    private val fatalErrors = setOf(
        SpeechRecognizer.ERROR_AUDIO,
        SpeechRecognizer.ERROR_CLIENT,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
    ) + extraErrors()

    abstract fun speechRecognizer(context: Context): SpeechRecognizer

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SpeechRecognitionListener? = null

    init {
        try {
            speechRecognizer = speechRecognizer(context)
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
                // don't call end of speech here, simply ignore it
            }

            override fun onError(error: Int) {
                // when we encounter this, the recognizer stopped so we should restart it
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    listener?.restart()
                    return
                }
                val errorMessage = getErrorMessage(error)
                val isInitialisationError = fatalErrors.contains(error)
                Log.e(TAG, "SpeechRecognizer: onError: $errorMessage (code: $error)")
                listener?.onError(
                    errorMessage = UiText.StringText(errorMessage),
                    isInitialisation = isInitialisationError
                )
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.getOrNull(0)
                Log.d(TAG, "SpeechRecognizer: onResults: $text")
                if (text != null) {
                    listener?.onPartialResults(text, true)
                } else {
                    listener?.restart()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val isFinalResult = partialResults?.getBoolean("final_result") ?: false
                val text = matches?.getOrNull(0)
                if (text != null) {
                    listener?.onPartialResults(text, isFinalResult)
                    Log.d(TAG, "SpeechRecognizer: onPartialResults: $text")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "SpeechRecognizer: onEvent: $eventType")
            }
        })
    }

    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }

    override fun removeListener() {
        this.listener = null
    }

    override fun isSpeechRecognitionAvailable(): Boolean =
        speechRecognizer != null

    override fun startListening() {
        if (speechRecognizer == null) {
            listener?.onError(
                errorMessage = UiText.ResText(R.string.speech_not_available),
                isInitialisation = true
            )
            return
        }

        speechRecognizer?.cancel()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().language
            )
            putExtra(
                RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                RecognizerIntent.LANGUAGE_SWITCH_QUICK_RESPONSE
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000
            )
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling speechRecognizer.startListening", e)
            listener?.onError(
                errorMessage = e.message?.let { UiText.StringText(it) }
                    ?: UiText.ResText(R.string.speech_error_client),
                isInitialisation = true
            )
        }

    }

    override fun cancelListening() {
        speechRecognizer?.cancel()
    }

    override fun destroy() {
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

    companion object Companion {
        private const val TAG = "OnDeviceSpeechRecognizer"
    }
}