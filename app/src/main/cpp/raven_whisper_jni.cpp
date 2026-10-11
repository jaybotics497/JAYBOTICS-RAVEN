#include <jni.h>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>
#include "whisper.h"

struct RavenWhisper {
    whisper_context * ctx = nullptr;
    std::atomic_bool cancelled{false};
    std::mutex mutex;
};

static RavenWhisper * ptr(jlong h) { return reinterpret_cast<RavenWhisper *>(h); }

extern "C" JNIEXPORT jlong JNICALL
Java_com_ravencore_zero_speech_OfflineWhisperBackend_nativeLoad(JNIEnv *env, jobject, jstring path_) {
    const char * path = env->GetStringUTFChars(path_, nullptr);
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    auto * owner = new RavenWhisper();
    owner->ctx = whisper_init_from_file_with_params(path, cp);
    env->ReleaseStringUTFChars(path_, path);
    if (!owner->ctx) { delete owner; return 0; }
    return reinterpret_cast<jlong>(owner);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ravencore_zero_speech_OfflineWhisperBackend_nativeTranscribe(JNIEnv *env, jobject, jlong handle, jfloatArray samples_) {
    auto * owner = ptr(handle);
    if (!owner || !owner->ctx) return env->NewStringUTF("");
    std::lock_guard<std::mutex> guard(owner->mutex);
    owner->cancelled.store(false);
    const jsize n = env->GetArrayLength(samples_);
    std::vector<float> samples(static_cast<size_t>(n));
    env->GetFloatArrayRegion(samples_, 0, n, samples.data());

    whisper_full_params p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.print_progress = false; p.print_realtime = false; p.print_timestamps = false; p.translate = false;
    p.no_context = true; p.single_segment = false; p.n_threads = 4;
    if (owner->cancelled.load()) return env->NewStringUTF("");
    if (whisper_full(owner->ctx, p, samples.data(), static_cast<int>(samples.size())) != 0) return env->NewStringUTF("");
    if (owner->cancelled.load()) return env->NewStringUTF("");
    std::string text;
    const int count = whisper_full_n_segments(owner->ctx);
    for (int i = 0; i < count; ++i) {
        const char * seg = whisper_full_get_segment_text(owner->ctx, i);
        if (seg) text += seg;
    }
    return env->NewStringUTF(text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_speech_OfflineWhisperBackend_nativeCancel(JNIEnv *, jobject, jlong handle) {
    if (auto * owner = ptr(handle)) owner->cancelled.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_speech_OfflineWhisperBackend_nativeClose(JNIEnv *, jobject, jlong handle) {
    auto * owner = ptr(handle); if (!owner) return;
    std::lock_guard<std::mutex> guard(owner->mutex);
    if (owner->ctx) whisper_free(owner->ctx);
    owner->ctx = nullptr;
    delete owner;
}
