package me.proton.android.lumo.speech.recognizer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.proton.android.lumo.R
import me.proton.android.lumo.speech.SpeechRecognitionManager
import me.proton.android.lumo.speech.SpeechRecognitionManager.SpeechRecognitionListener
import me.proton.android.lumo.ui.text.UiText
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskSpeechRecognizer(
    context: Context
) : LumoSpeechRecognizer {

    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var listener: SpeechRecognitionListener? = null
    private val _speechChannel = Channel<SpeechService>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        speechService?.stop()
        speechService = null
        setupListener()

        StorageService.unpack(
            context, "model-en-us", "model",
            { model: Model? ->
                model?.let {
                    try {
                        recognizer = Recognizer(model, 16000.0f)
                        speechService = SpeechService(recognizer, 16000.0f).also {
                            listener?.onReadyForSpeech()
                            _speechChannel.trySend(it)
                        }
                    } catch (e: IOException) {
                        recognizer = null
                        listener?.onError(
                            errorMessage = UiText.StringText(
                                text = e.message ?: "Something went wrong"
                            ),
                            isInitialisation = true
                        )
                    }
                } ?: {
                    listener?.onError(
                        errorMessage = UiText.StringText(text = "Model error"),
                        isInitialisation = true
                    )
                }()
            },
            { exception: IOException? ->
                listener?.onError(
                    errorMessage = UiText.StringText(
                        text = exception?.message ?: "Something went wrong"
                    ),
                    isInitialisation = true
                )
            })
    }

    override fun setListener(listener: SpeechRecognitionManager.SpeechRecognitionListener) {
        this.listener = listener
    }

    override fun removeListener() {
        this.listener = null
    }

    override fun isSpeechRecognitionAvailable(): Boolean = true

    override fun startListening() {
        speechService?.let { _speechChannel.trySend(it) }
    }

    private fun setupListener() {
        coroutineScope.launch {
            _speechChannel.receiveAsFlow().collect { speechService ->
                speechService.startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        hypothesis?.let {
                            val json = Json.parseToJsonElement(hypothesis).jsonObject
                            val partial = json["partial"]?.jsonPrimitive?.contentOrNull
                            if (!partial.isNullOrEmpty()) {
                                listener?.onPartialResults(partial, false)
                            }
                        }
                    }

                    override fun onResult(hypothesis: String?) {
                        hypothesis?.let {
                            val json = Json.parseToJsonElement(hypothesis).jsonObject
                            val text = json["text"]?.jsonPrimitive?.contentOrNull
                            if (!text.isNullOrEmpty()) {
                                listener?.onPartialResults(text, true)
                            }
                        } ?: {
                            listener?.onError(
                                errorMessage = UiText.ResText(res = R.string.speech_error_no_match),
                                isInitialisation = false
                            )
                        }()
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        listener?.restart()
                    }

                    override fun onError(error: Exception?) {
                        listener?.onError(
                            errorMessage = UiText.StringText(
                                text = error?.message ?: "Something went wrong"
                            ),
                            isInitialisation = true
                        )
                    }

                    override fun onTimeout() {
                        listener?.restart()
                    }
                })
            }
        }
    }

    override fun cancelListening() {
        coroutineScope.cancel()
        speechService?.cancel()
    }

    override fun destroy() {
        coroutineScope.cancel()
        speechService?.stop()
        speechService?.shutdown()
    }
}