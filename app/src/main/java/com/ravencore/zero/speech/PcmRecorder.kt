package com.ravencore.zero.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Minimal bounded 16 kHz mono recorder for offline Whisper. */
class PcmRecorder(private val context: Context) {
    private val recording = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun record(maxMillis: Long = 15_000L): FloatArray {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "RECORD_AUDIO permission not granted"
        }
        check(recording.compareAndSet(false, true)) { "Recorder already active" }
        val min = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(min > 0) { "AudioRecord buffer unavailable" }
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, min * 2)
        recorder = audio
        val out = ByteArrayOutputStream()
        val buf = ByteArray(min)
        val deadline = System.nanoTime() + maxMillis * 1_000_000L
        try {
            audio.startRecording()
            while (recording.get() && System.nanoTime() < deadline) {
                val n = audio.read(buf, 0, buf.size)
                if (n > 0) out.write(buf, 0, n)
            }
        } finally {
            recording.set(false)
            try { audio.stop() } catch (_: Throwable) {}
            audio.release()
            recorder = null
        }
        val bytes = out.toByteArray()
        return FloatArray(bytes.size / 2) { i ->
            val lo = bytes[i * 2].toInt() and 0xff
            val hi = bytes[i * 2 + 1].toInt()
            ((hi shl 8) or lo).toShort() / 32768.0f
        }
    }

    fun stop() { recording.set(false) }
}
