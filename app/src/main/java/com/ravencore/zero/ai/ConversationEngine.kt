package com.ravencore.zero.ai

import com.ravencore.zero.core.CoreEvent
import com.ravencore.zero.core.OperationEpoch
import com.ravencore.zero.core.RavenRuntime

class ConversationEngine(
    private val runtime: RavenRuntime,
    private val epoch: OperationEpoch,
    private val backend: QwenBackend,
) {
    fun ask(text: String, onToken: (String) -> Unit = {}): String {
        require(text.isNotBlank())
        check(backend.ready) { "Qwen model is not loaded" }
        val generation = epoch.next()
        runtime.dispatch(CoreEvent.InferenceStarted)
        return try {
            runtime.dispatch(CoreEvent.GenerationStarted)
            val result = backend.generate(text, generation) { token ->
                if (epoch.owns(generation)) onToken(token)
            }
            if (epoch.owns(generation)) runtime.dispatch(CoreEvent.GenerationCompleted)
            result
        } catch (t: Throwable) {
            if (epoch.owns(generation)) runtime.dispatch(CoreEvent.Fault(t.message ?: "generation failed"))
            throw t
        }
    }

    fun cancel() {
        val generation = epoch.current()
        epoch.cancelAll()
        backend.cancel(generation)
        runtime.dispatch(CoreEvent.GenerationCancelled)
    }
}
