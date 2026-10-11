package com.ravencore.zero.ai

import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface QwenBackend : AutoCloseable {
    val ready: Boolean
    fun load(path: String): Boolean
    fun generate(prompt: String, epoch: Long, onToken: (String) -> Unit): String
    fun cancel(epoch: Long)
}

/**
 * Production JNI boundary for Raven's llama.cpp engine.
 * Native callbacks are epoch-scoped so cancelled/stale generations cannot mutate current UI state.
 */
class NativeQwenBackend : QwenBackend {
    private val tokenSinks = ConcurrentHashMap<Long, (String) -> Unit>()
    @Volatile private var loaded = false
    @Volatile private var nativeAvailable = false

    init {
        nativeAvailable = try {
            System.loadLibrary("raven_qwen")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    override val ready: Boolean get() = nativeAvailable && loaded

    override fun load(path: String): Boolean {
        val model = File(path)
        require(model.isFile && model.canRead()) { "Unreadable Qwen model: $path" }
        if (!nativeAvailable) return false
        loaded = nativeLoad(model.absolutePath)
        return loaded
    }

    override fun generate(prompt: String, epoch: Long, onToken: (String) -> Unit): String {
        check(ready) { "Qwen native backend unavailable" }
        require(prompt.isNotBlank())
        tokenSinks[epoch] = onToken
        return try {
            nativeGenerate(prompt, epoch)
        } finally {
            tokenSinks.remove(epoch)
        }
    }

    @Suppress("unused") // Called by raven_qwen JNI.
    private fun onNativeToken(epoch: Long, token: String) {
        tokenSinks[epoch]?.invoke(token)
    }

    override fun cancel(epoch: Long) {
        tokenSinks.remove(epoch)
        if (nativeAvailable) nativeCancel(epoch)
    }

    override fun close() {
        tokenSinks.clear()
        if (nativeAvailable) nativeClose()
        loaded = false
    }

    private external fun nativeLoad(path: String): Boolean
    private external fun nativeGenerate(prompt: String, epoch: Long): String
    private external fun nativeCancel(epoch: Long)
    private external fun nativeClose()
}
