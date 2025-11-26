package me.proton.android.lumo.ui.components.waveform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MicVisualizer(
    private val sampleRate: Int = 16000,
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
) {
    private var audioRecord: AudioRecord? = null
    private var running = false

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onAmplitude: (Float) -> Unit) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        running = true
        audioRecord?.startRecording()

        scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (running) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val rms = sqrt(buffer.take(read).sumOf { (it * it).toDouble() } / read).toFloat()
                    onAmplitude(rms)
                }
            }
        }
    }

    fun stop() {
        running = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
