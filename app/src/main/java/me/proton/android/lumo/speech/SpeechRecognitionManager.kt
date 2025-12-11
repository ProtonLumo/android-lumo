package me.proton.android.lumo.speech

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import me.proton.android.lumo.speech.SpeechRecognitionManager.Engine.LanguageTag.LanguageAndCountry
import me.proton.android.lumo.speech.SpeechRecognitionManager.Engine.LanguageTag.LanguageOnly
import me.proton.android.lumo.speech.recognizer.LumoSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.VoskSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.android.GoogleSpeechRecognizer
import me.proton.android.lumo.speech.recognizer.android.OnDeviceSpeechRecognizer
import me.proton.android.lumo.ui.text.UiText
import javax.inject.Inject

private const val TAG = "SpeechRecognitionManager"

class SpeechRecognitionManager @Inject constructor(private val context: Context) {

    interface SpeechRecognitionListener {
        fun onReadyForSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun restart()
        fun onError(errorMessage: UiText, isInitialisation: Boolean)
        fun onPartialResults(text: String, isFinal: Boolean)
        fun switched()
    }

    sealed interface Engine {
        data class OnDevice(val languageTag: LanguageTag) : Engine
        data class GoogleCloud(val languageTag: LanguageTag) : Engine
        data object Vosk : Engine

        sealed interface LanguageTag {
            data object LanguageOnly : LanguageTag
            data object LanguageAndCountry : LanguageTag
        }
    }

    private var listener: SpeechRecognitionListener? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private fun androidOnDeviceEngine(languageTag: Engine.LanguageTag): LumoSpeechRecognizer =
        OnDeviceSpeechRecognizer(
            context = context,
            languageTag = languageTag
        )

    private fun googleCloudEngine(languageTag: Engine.LanguageTag): LumoSpeechRecognizer =
        GoogleSpeechRecognizer(
            context = context,
            languageTag = languageTag
        )

    private val voskEngine: LumoSpeechRecognizer by lazy {
        VoskSpeechRecognizer(context)
    }

    private var current: LumoSpeechRecognizer = chooseInitialEngine()

    private fun chooseInitialEngine(): LumoSpeechRecognizer =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            androidOnDeviceEngine(languageTag = LanguageAndCountry)
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

    fun engineState(): Engine =
        with(current) {
            when (this) {
                is OnDeviceSpeechRecognizer -> Engine.OnDevice(this.languageTag)
                is GoogleSpeechRecognizer -> Engine.GoogleCloud(this.languageTag)
                else -> Engine.Vosk
            }
        }

    private fun wrapListener(real: SpeechRecognitionListener): SpeechRecognitionListener =
        object : SpeechRecognitionListener {

            override fun onReadyForSpeech() = real.onReadyForSpeech()

            override fun onRmsChanged(rmsdB: Float) =
                real.onRmsChanged(rmsdB)

            override fun restart() =
                real.restart()

            override fun onPartialResults(text: String, isFinal: Boolean) =
                real.onPartialResults(text, isFinal)

            override fun onError(errorMessage: UiText, isInitialisation: Boolean) {
                if (isInitialisation) {
                    when (val state = engineState()) {
                        is Engine.OnDevice -> switch(
                            lumoSpeechRecognizer =
                                if (state.languageTag == LanguageAndCountry) {
                                    androidOnDeviceEngine(languageTag = LanguageOnly)
                                } else {
                                    voskEngine
                                }
                        )

                        is Engine.Vosk ->
                            switch(
                                lumoSpeechRecognizer =
                                    googleCloudEngine(languageTag = LanguageAndCountry)
                            )

                        is Engine.GoogleCloud ->
                            if (state.languageTag == LanguageAndCountry) {
                                switch(
                                    lumoSpeechRecognizer =
                                        googleCloudEngine(languageTag = LanguageOnly)
                                )
                            } else {
                                real.onError(
                                    errorMessage = errorMessage,
                                    isInitialisation = false
                                )
                            }

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