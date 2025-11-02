package me.proton.android.lumo.speech.recognizer

import me.proton.android.lumo.speech.SpeechRecognitionManager.SpeechRecognitionListener

interface LumoSpeechRecognizer {

    fun setListener(listener: SpeechRecognitionListener)
    fun removeListener()
    fun isSpeechRecognitionAvailable(): Boolean
    fun startListening()
    fun cancelListening()
    fun destroy()
}