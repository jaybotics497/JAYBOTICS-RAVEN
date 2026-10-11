#include <jni.h>
#include <android/log.h>
#include <llama.h>

#include <atomic>
#include <mutex>
#include <string>
#include <unordered_set>
#include <vector>

#define TAG "RavenQwen"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
llama_model * g_model = nullptr;
std::unordered_set<jlong> g_cancelled;
std::atomic<bool> g_backend_ready{false};

void release_model_locked() {
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

bool cancelled(jlong epoch) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_cancelled.find(epoch) != g_cancelled.end();
}

void emit_token(JNIEnv * env, jobject thiz, jlong epoch, const std::string & token) {
    jclass cls = env->GetObjectClass(thiz);
    jmethodID cb = env->GetMethodID(cls, "onNativeToken", "(JLjava/lang/String;)V");
    if (!cb) return;
    jstring text = env->NewStringUTF(token.c_str());
    env->CallVoidMethod(thiz, cb, epoch, text);
    env->DeleteLocalRef(text);
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeLoad(JNIEnv * env, jobject, jstring path_) {
    const char * path = env->GetStringUTFChars(path_, nullptr);
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_backend_ready.exchange(true)) llama_backend_init();
    release_model_locked();
    llama_model_params params = llama_model_default_params();
    params.use_mmap = true;
    g_model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(path_, path);
    return g_model ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeGenerate(JNIEnv * env, jobject thiz, jstring prompt_, jlong epoch) {
    const char * raw = env->GetStringUTFChars(prompt_, nullptr);
    std::string prompt(raw);
    env->ReleaseStringUTFChars(prompt_, raw);

    llama_model * model = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        model = g_model;
        g_cancelled.erase(epoch);
    }
    if (!model) {
        jclass ex = env->FindClass("java/lang/IllegalStateException");
        env->ThrowNew(ex, "Qwen model is not loaded");
        return nullptr;
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    const int n_prompt = -llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) return env->NewStringUTF("");
    std::vector<llama_token> tokens(static_cast<size_t>(n_prompt));
    if (llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true) < 0) {
        return env->NewStringUTF("");
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 4096;
    cparams.n_batch = 512;
    cparams.n_threads = 4;
    cparams.n_threads_batch = 4;
    llama_context * ctx = llama_init_from_model(model, cparams);
    if (!ctx) return env->NewStringUTF("");

    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));
    if (llama_decode(ctx, batch) != 0) {
        llama_free(ctx);
        return env->NewStringUTF("");
    }

    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(0x52415645u));

    std::string output;
    constexpr int max_new_tokens = 512;
    for (int i = 0; i < max_new_tokens && !cancelled(epoch); ++i) {
        llama_token token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        char piece[512];
        int n = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
        if (n > 0) {
            std::string text(piece, static_cast<size_t>(n));
            output += text;
            emit_token(env, thiz, epoch, text);
        }
        llama_token next = token;
        llama_batch next_batch = llama_batch_get_one(&next, 1);
        if (llama_decode(ctx, next_batch) != 0) break;
    }

    llama_sampler_free(sampler);
    llama_free(ctx);
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_cancelled.erase(epoch);
    }
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeCancel(JNIEnv *, jobject, jlong epoch) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_cancelled.insert(epoch);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeClose(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_model_locked();
    g_cancelled.clear();
    if (g_backend_ready.exchange(false)) llama_backend_free();
}
