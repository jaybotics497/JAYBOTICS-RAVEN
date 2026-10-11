# Offline STT integration

Implementation state: SOURCE COMPLETE / RUNTIME UNVERIFIED.

Production path: microphone permission -> PcmRecorder (16 kHz mono PCM16) -> FloatArray -> OfflineWhisperBackend -> raven_whisper JNI -> pinned whisper.cpp -> transcript -> RavenAppController -> ConversationEngine -> Qwen -> memory -> TTS -> RavenRuntime.

Pinned whisper.cpp revision: d1be6fde11ac6e0407606b4e42fe72d34add8037.

PASS is forbidden until Android/NDK compilation succeeds, a Whisper model is admitted, real microphone audio produces a transcript, cancellation/lifecycle are exercised, and the exact candidate is verified on the target Samsung device.
