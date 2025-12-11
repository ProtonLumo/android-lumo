package me.proton.android.lumo.speech.recognizer.android

import android.content.Context
import android.speech.SpeechRecognizer
import me.proton.android.lumo.speech.SpeechRecognitionManager.Engine.LanguageTag

class GoogleSpeechRecognizer(
    context: Context,
    languageTag: LanguageTag
) : AndroidSpeechRecognizer(context, languageTag) {

    override fun speechRecognizer(context: Context): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)
}
