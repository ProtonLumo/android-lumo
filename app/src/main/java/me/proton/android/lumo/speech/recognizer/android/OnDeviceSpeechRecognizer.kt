package me.proton.android.lumo.speech.recognizer.android

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import me.proton.android.lumo.speech.SpeechRecognitionManager.Engine.LanguageTag

@RequiresApi(Build.VERSION_CODES.S)
class OnDeviceSpeechRecognizer(
    context: Context,
    languageTag: LanguageTag
) : AndroidSpeechRecognizer(context, languageTag) {

    override fun extraErrors(): Set<Int> =
        setOf(
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        )

    override fun speechRecognizer(context: Context): SpeechRecognizer =
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
}