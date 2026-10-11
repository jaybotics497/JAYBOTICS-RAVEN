# Raven production integration status

Implemented source path:

`MainActivity -> RavenAppController -> RavenSpeechController -> ConversationEngine -> NativeQwenBackend/JNI -> RavenRuntime -> RavenStateMachine -> RavenSceneView -> RavenPresentationAdapter -> LivingWorldRenderer + RavenRenderer + SceneCameraV2`

Persistence path:

`STT transcript -> RavenMemoryStore -> prompt context -> Qwen response -> RavenMemoryStore -> TTS`

Cancellation path:

`long press -> ConversationEngine.cancel -> OperationEpoch invalidation -> nativeCancel(epoch) + STT/TTS stop`

Native engine pins:
- llama.cpp: 23b0202a189c44a54625aadcb37a946dd1d6278d
- whisper.cpp: d1be6fde11ac6e0407606b4e42fe72d34add8037
- source bundle SHA-256: e94b4d1bc206c367378700b2491401ae7ab67981ed49e6544c21855cc131a256

Still requires execution proof: Android/NDK compile, real GGUF load/generation, native cancellation, Whisper offline STT integration, release signing, Samsung A17 acceptance.
