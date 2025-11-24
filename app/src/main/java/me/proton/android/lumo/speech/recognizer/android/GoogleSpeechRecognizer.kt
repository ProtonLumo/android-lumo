package me.proton.android.lumo.speech.recognizer.android

import android.content.Context
import android.speech.SpeechRecognizer

class GoogleSpeechRecognizer(context: Context) : AndroidSpeechRecognizer(context) {

    override fun speechRecognizer(context: Context): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)
}
