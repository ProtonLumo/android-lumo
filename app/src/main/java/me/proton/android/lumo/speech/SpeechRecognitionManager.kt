package me.proton.android.lumo.speech

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import me.proton.android.lumo.speech.recognizer.LumoSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.VoskSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.android.GoogleSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.android.OnDeviceSpeechRecognizer
import me.proton.android.lumo.ui.text.UiText

private const val TAG = "SpeechRecognitionManager"

class SpeechRecognitionManager(private val context: Context) {

    interface SpeechRecognitionListener {
        fun onReadyForSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onEndOfSpeech()
        fun onError(errorMessage: UiText, isInitialisation: Boolean)
        fun onPartialResults(text: String, isFinal: Boolean)
        fun switched()
    }

    sealed interface Engine {
        object OnDevice : Engine
        object GoogleCloud : Engine
        object Vosk : Engine
    }

    private var listener: SpeechRecognitionListener? = null

    private val androidOnDeviceEngine: LumoSpeechRecognizer
        @RequiresApi(Build.VERSION_CODES.S)
        get() = OnDeviceSpeechRecognizer(context = context)

    private val googleCloudEngine: LumoSpeechRecognizer
        get() = GoogleSpeechRecognizer(context = context)

    private val voskEngine: LumoSpeechRecognizer by lazy {
        VoskSpeechRecognizer(context)
    }

    private var current: LumoSpeechRecognizer = chooseInitialEngine()

    private fun chooseInitialEngine(): LumoSpeechRecognizer =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            androidOnDeviceEngine
        } else {
            voskEngine
        }

    private fun switch(lumoSpeechRecognizer: LumoSpeechRecognizer) {
        current.removeListener()
        current.destroy()
        current = lumoSpeechRecognizer
        listener?.let { current.setListener(it) }
        listener?.switched()
        current.startListening()
    }

    fun setListener(listener: SpeechRecognitionListener) {
        wrapListener(listener).also {
            this.listener = it
            current.setListener(it)
        }
    }

    fun removeListener() {
        listener = null
        current.removeListener()
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        current.isSpeechRecognitionAvailable()

    fun startListening() {
        current.startListening()
    }

    fun cancelListening() {
        current.cancelListening()
    }

    fun destroy() {
        current.destroy()
    }

    fun isVosk(): Boolean =
        current is VoskSpeechRecognizer

    fun engineState(): Engine =
        when (current) {
            is OnDeviceSpeechRecognizer -> Engine.OnDevice
            is GoogleSpeechRecognizer -> Engine.GoogleCloud
            else -> Engine.Vosk
        }

    private fun wrapListener(real: SpeechRecognitionListener): SpeechRecognitionListener =
        object : SpeechRecognitionListener {

            override fun onReadyForSpeech() = real.onReadyForSpeech()

            override fun onRmsChanged(rmsdB: Float) =
                real.onRmsChanged(rmsdB)

            override fun onEndOfSpeech() =
                real.onEndOfSpeech()

            override fun onPartialResults(text: String, isFinal: Boolean) =
                real.onPartialResults(text, isFinal)

            override fun onError(errorMessage: UiText, isInitialisation: Boolean) {
                if (isInitialisation) {
                    when (engineState()) {
                        is Engine.OnDevice -> switch(voskEngine)
                        is Engine.Vosk -> switch(googleCloudEngine)
                        is Engine.GoogleCloud ->
                            real.onError(
                                errorMessage = errorMessage,
                                isInitialisation = false
                            )
                    }
                } else {
                    real.onError(
                        errorMessage = errorMessage,
                        isInitialisation = false
                    )
                }
            }

            override fun switched() {
                real.switched()
            }
        }
}