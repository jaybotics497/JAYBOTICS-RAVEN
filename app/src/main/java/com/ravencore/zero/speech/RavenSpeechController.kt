package com.ravencore.zero.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ravencore.zero.core.CoreEvent
import com.ravencore.zero.core.RavenRuntime
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class RavenSpeechController(context: Context, private val runtime: RavenRuntime) : RecognitionListener, AutoCloseable {
    private val stt = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = null
    private val utterance = AtomicLong()
    var onText: (String) -> Unit = {}

    init {
        stt.setRecognitionListener(this)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { runtime.dispatch(CoreEvent.TtsCompleted) }
                    @Deprecated("platform callback") override fun onError(utteranceId: String?) { runtime.dispatch(CoreEvent.Fault("TTS failed")) }
                    override fun onError(utteranceId: String?, errorCode: Int) { runtime.dispatch(CoreEvent.Fault("TTS $errorCode")) }
                })
            }
        }
    }

    fun listen() {
        runtime.dispatch(CoreEvent.SttStarted)
        stt.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        runtime.dispatch(CoreEvent.TtsStarted)
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "raven-${utterance.incrementAndGet()}")
        if (result == TextToSpeech.ERROR) runtime.dispatch(CoreEvent.Fault("TTS enqueue failed"))
    }

    fun cancelSpeech() {
        stt.cancel()
        tts?.stop()
        runtime.dispatch(CoreEvent.TtsCancelled)
    }

    override fun onResults(results: Bundle) {
        runtime.dispatch(CoreEvent.SttStopped)
        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
    }
    override fun onError(error: Int) { runtime.dispatch(CoreEvent.Fault("STT $error")) }
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun close() { stt.destroy(); tts?.shutdown(); tts = null }
}
