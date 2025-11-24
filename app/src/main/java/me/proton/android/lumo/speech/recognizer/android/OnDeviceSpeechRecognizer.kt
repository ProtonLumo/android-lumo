package me.proton.android.lumo.speech.recognizer.android

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class OnDeviceSpeechRecognizer(context: Context) : AndroidSpeechRecognizer(context) {

    override val extraErrors: Set<Int> = setOf(
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
    )

    override fun speechRecognizer(context: Context): SpeechRecognizer =
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
}