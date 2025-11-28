package me.proton.android.lumo.speech

import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.webview.injectSpokenText
import javax.inject.Inject

interface SpeechRepository {

    fun injectText(spokenText: String)
}

class SpeechRepositoryImpl @Inject constructor(
    private val webBridge: WebAppInterface
) : SpeechRepository {
    override fun injectText(spokenText: String) {
        webBridge.injectSpeechOutput(spokenText)
    }
}