package com.ravencore.zero.speech

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/** JNI owner for the pinned whisper.cpp runtime. 16 kHz mono PCM float input. */
class OfflineWhisperBackend : Closeable {
    private var handle: Long = 0L
    private val cancelled = AtomicBoolean(false)

    val ready: Boolean get() = handle != 0L

    @Synchronized fun load(modelPath: String): Boolean {
        close()
        cancelled.set(false)
        handle = nativeLoad(modelPath)
        return handle != 0L
    }

    @Synchronized fun transcribe(samples: FloatArray): String {
        check(handle != 0L) { "Whisper model is not loaded" }
        cancelled.set(false)
        return nativeTranscribe(handle, samples).trim()
    }

    fun cancel() {
        cancelled.set(true)
        val h = handle
        if (h != 0L) nativeCancel(h)
    }

    @Synchronized override fun close() {
        val h = handle
        handle = 0L
        if (h != 0L) nativeClose(h)
    }

    private external fun nativeLoad(modelPath: String): Long
    private external fun nativeTranscribe(handle: Long, samples: FloatArray): String
    private external fun nativeCancel(handle: Long)
    private external fun nativeClose(handle: Long)

    companion object {
        init { System.loadLibrary("raven_whisper") }
    }
}
