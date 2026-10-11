package com.ravencore.zero.app

import android.content.Context
import android.os.Environment
import com.ravencore.zero.ai.ConversationEngine
import com.ravencore.zero.ai.ModelVerifier
import com.ravencore.zero.ai.NativeQwenBackend
import com.ravencore.zero.core.CoreEvent
import com.ravencore.zero.core.Capability
import com.ravencore.zero.core.CapabilityRegistry
import com.ravencore.zero.core.OperationEpoch
import com.ravencore.zero.core.RavenRuntime
import com.ravencore.zero.data.RavenMemoryStore
import com.ravencore.zero.speech.RavenSpeechController
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the production Core -> STT -> Qwen -> memory -> TTS loop. */
class RavenAppController(
    context: Context,
    val runtime: RavenRuntime = RavenRuntime(),
) : AutoCloseable {
    companion object {
        const val CANONICAL_QWEN_SHA256 = "52280f27475f3a015d31697378de88077b67e55e8c3f19c5fad2c341806af3c5"
    }

    private val app = context.applicationContext
    private val closed = AtomicBoolean(false)
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "raven-ai").apply { isDaemon = true } }
    private val epoch = OperationEpoch()
    val capabilities = CapabilityRegistry()
    private val backend = NativeQwenBackend()
    private val conversation = ConversationEngine(runtime, epoch, backend)
    private val memory = RavenMemoryStore(app)
    val speech = RavenSpeechController(app, runtime)

    @Volatile var lastTranscript: String = ""
        private set
    @Volatile var lastResponse: String = ""
        private set
    @Volatile var modelPath: String? = null
        private set

    init {
        capabilities.set(Capability.MEMORY, true)
        capabilities.set(Capability.TTS, true)
        capabilities.set(Capability.STT, true)
        capabilities.set(Capability.OFFLINE, true)
        speech.onText = ::submitTranscript
        worker.execute { capabilities.set(Capability.QWEN, admitBestModel()) }
    }

    fun listen() {
        if (!closed.get()) speech.listen()
    }

    fun submitTranscript(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || closed.get()) return
        lastTranscript = clean
        memory.remember("last_user_text", clean)
        worker.execute {
            try {
                if (!backend.ready && !admitBestModel()) {
                    capabilities.set(Capability.QWEN, false)
                    runtime.dispatch(CoreEvent.ServiceUnavailable)
                    return@execute
                }
                capabilities.set(Capability.QWEN, true)
                val answer = conversation.ask(buildPrompt(clean))
                if (closed.get()) return@execute
                lastResponse = answer
                memory.remember("last_raven_response", answer)
                speech.speak(answer)
            } catch (t: Throwable) {
                if (!closed.get()) runtime.dispatch(CoreEvent.Fault(t.message ?: "Raven conversation failed"))
            }
        }
    }

    fun cancel() {
        conversation.cancel()
        speech.cancelSpeech()
    }

    private fun buildPrompt(userText: String): String {
        val previousUser = memory.recall("last_user_text_before_current")
        val previousRaven = memory.recall("last_raven_response")
        memory.remember("last_user_text_before_current", userText)
        return buildString {
            append("You are Raven, the private on-device Jaybotics assistant. Be concise, useful, and truthful.\n")
            if (!previousUser.isNullOrBlank() && !previousRaven.isNullOrBlank()) {
                append("Previous user: ").append(previousUser).append('\n')
                append("Previous Raven: ").append(previousRaven).append('\n')
            }
            append("User: ").append(userText).append("\nRaven:")
        }
    }

    private fun candidateModels(): Sequence<File> = sequence {
        memory.recall("qwen_model_path")?.let { yield(File(it)) }
        yield(File(app.filesDir, "models/Qwen3-1.7B-Q4_K_M.gguf"))
        yield(File(app.getExternalFilesDir(null), "models/Qwen3-1.7B-Q4_K_M.gguf"))
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        yield(File(downloads, "Raven Files/Models/Qwen3-1.7B-Q4_K_M.gguf"))
        yield(File(downloads, "Raven Files/Models/Qwen3-1.7B-Q4_K_M (1).gguf"))
        yield(File(downloads, "Raven Files/Models/Qwen3-1.7B-Q4_K_M (2).gguf"))
        yield(File(downloads, "Raven Files/Models/Qwen3-1.7B-Q4_K_M-1 (1).gguf"))
    }.distinctBy { it.absolutePath }

    @Synchronized private fun admitBestModel(): Boolean {
        if (backend.ready) return true
        for (file in candidateModels()) {
            if (!file.isFile || !file.canRead()) continue
            try {
                // Known canonical hash is enforced when it matches the canonical filename.
                val expected = if (file.name == "Qwen3-1.7B-Q4_K_M.gguf") CANONICAL_QWEN_SHA256 else null
                ModelVerifier.admit(file, expected)
                if (backend.load(file.absolutePath)) {
                    modelPath = file.absolutePath
                    memory.remember("qwen_model_path", file.absolutePath)
                    return true
                }
            } catch (_: Throwable) { /* continue to the next admitted candidate */ }
        }
        return false
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try { conversation.cancel() } catch (_: Throwable) {}
        try { speech.close() } catch (_: Throwable) {}
        try { backend.close() } catch (_: Throwable) {}
        worker.shutdownNow()
    }
}
