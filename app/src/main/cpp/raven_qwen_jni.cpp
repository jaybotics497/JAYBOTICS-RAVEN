#include <jni.h>
#include <android/log.h>

#include <llama.h>
#include "chat.h"

#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#define TAG "RavenQwen"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

namespace {

std::mutex g_lifecycle_mutex;
std::mutex g_generation_mutex;

llama_model * g_model = nullptr;
std::atomic<bool> g_backend_ready{false};

/*
 * Every cancel request advances this generation-independent epoch.
 *
 * A running generation captures the value before decode begins. The llama
 * abort callback can therefore stop GGML compute without taking Raven's
 * lifecycle mutex or waiting for token generation to return.
 */
std::atomic<uint64_t> g_cancel_epoch{0};

struct RavenAbortState {
    uint64_t generation_epoch;
};

bool raven_abort_callback(void * data) {
    auto * state = static_cast<RavenAbortState *>(data);
    return state != nullptr &&
           g_cancel_epoch.load(std::memory_order_acquire) !=
               state->generation_epoch;
}

void release_model_locked() {
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

bool generation_cancelled(const RavenAbortState & state) {
    return g_cancel_epoch.load(std::memory_order_acquire) !=
           state.generation_epoch;
}

void throw_illegal_state(JNIEnv * env, const char * message) {
    jclass ex = env->FindClass("java/lang/IllegalStateException");
    if (ex != nullptr) {
        env->ThrowNew(ex, message);
        env->DeleteLocalRef(ex);
    }
}

bool contains_think_markup(const std::string & text) {
    return text.find("<think>") != std::string::npos ||
           text.find("</think>") != std::string::npos;
}

bool emit_token(
    JNIEnv * env,
    jobject thiz,
    jlong epoch,
    const std::string & token
) {
    jclass cls = env->GetObjectClass(thiz);
    if (cls == nullptr) {
        return false;
    }

    jmethodID cb = env->GetMethodID(
        cls,
        "onNativeToken",
        "(JLjava/lang/String;)V"
    );

    if (cb == nullptr) {
        env->DeleteLocalRef(cls);
        return false;
    }

    jstring text = env->NewStringUTF(token.c_str());
    if (text == nullptr) {
        env->DeleteLocalRef(cls);
        return false;
    }

    env->CallVoidMethod(thiz, cb, epoch, text);

    env->DeleteLocalRef(text);
    env->DeleteLocalRef(cls);

    return !env->ExceptionCheck();
}

std::string build_qwen_prompt(
    const llama_model * model,
    const std::string & user_prompt
) {
    auto templates = common_chat_templates_init(
        model,
        ""
    );

    if (!templates) {
        throw std::runtime_error(
            "Qwen chat template initialization failed"
        );
    }

    common_chat_templates_inputs inputs;
    inputs.use_jinja = true;
    inputs.enable_thinking = false;
    inputs.add_generation_prompt = true;
    inputs.reasoning_format = COMMON_REASONING_FORMAT_NONE;

    common_chat_msg user;
    user.role = "user";
    user.content = user_prompt;
    inputs.messages.push_back(std::move(user));

    const common_chat_params applied =
        common_chat_templates_apply(templates.get(), inputs);

    if (applied.prompt.empty()) {
        throw std::runtime_error(
            "Qwen chat template produced an empty prompt"
        );
    }

    return applied.prompt;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeLoad(
    JNIEnv * env,
    jobject,
    jstring path_
) {
    if (path_ == nullptr) {
        return JNI_FALSE;
    }

    const char * path = env->GetStringUTFChars(path_, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    std::string model_path(path);
    env->ReleaseStringUTFChars(path_, path);

    /*
     * A load/close cannot overlap active inference. Generation owns
     * g_generation_mutex for the complete lifetime of its llama_context.
     */
    std::lock_guard<std::mutex> generation_guard(g_generation_mutex);
    std::lock_guard<std::mutex> lifecycle_guard(g_lifecycle_mutex);

    if (!g_backend_ready.exchange(true)) {
        llama_backend_init();
    }

    release_model_locked();

    llama_model_params params = llama_model_default_params();

    g_model = llama_model_load_from_file(
        model_path.c_str(),
        params
    );

    if (g_model == nullptr) {
        LOGE("Failed to load Qwen model");
        return JNI_FALSE;
    }

    LOGI("Qwen model loaded");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeGenerate(
    JNIEnv * env,
    jobject thiz,
    jstring prompt_,
    jlong epoch
) {
    if (prompt_ == nullptr) {
        return env->NewStringUTF("");
    }

    const char * raw = env->GetStringUTFChars(prompt_, nullptr);
    if (raw == nullptr) {
        return nullptr;
    }

    std::string user_prompt(raw);
    env->ReleaseStringUTFChars(prompt_, raw);

    /*
     * Raven currently serializes Qwen inference. This also guarantees that
     * nativeClose/nativeLoad cannot free or replace the model while a context
     * references it.
     */
    std::lock_guard<std::mutex> generation_guard(g_generation_mutex);

    llama_model * model = nullptr;
    {
        std::lock_guard<std::mutex> lifecycle_guard(g_lifecycle_mutex);
        model = g_model;
    }

    if (model == nullptr) {
        throw_illegal_state(env, "Qwen model is not loaded");
        return nullptr;
    }

    const RavenAbortState abort_state{
        g_cancel_epoch.load(std::memory_order_acquire)
    };

    try {
        const std::string formatted_prompt =
            build_qwen_prompt(model, user_prompt);

        if (generation_cancelled(abort_state)) {
            return env->NewStringUTF("");
        }

        const llama_vocab * vocab = llama_model_get_vocab(model);
        if (vocab == nullptr) {
            throw std::runtime_error("Qwen vocabulary unavailable");
        }

        const int32_t token_count = -llama_tokenize(
            vocab,
            formatted_prompt.c_str(),
            static_cast<int32_t>(formatted_prompt.size()),
            nullptr,
            0,
            true,
            true
        );

        if (token_count <= 0) {
            throw std::runtime_error(
                "Qwen prompt tokenization failed"
            );
        }

        std::vector<llama_token> prompt_tokens(
            static_cast<size_t>(token_count)
        );

        const int32_t actual_tokens = llama_tokenize(
            vocab,
            formatted_prompt.c_str(),
            static_cast<int32_t>(formatted_prompt.size()),
            prompt_tokens.data(),
            static_cast<int32_t>(prompt_tokens.size()),
            true,
            true
        );

        if (actual_tokens < 0) {
            throw std::runtime_error(
                "Qwen prompt tokenization failed"
            );
        }

        prompt_tokens.resize(static_cast<size_t>(actual_tokens));

        llama_context_params cparams =
            llama_context_default_params();

        cparams.n_ctx = 4096;
        cparams.n_batch = 512;
        cparams.n_threads = 4;
        cparams.n_threads_batch = 4;

        llama_context * raw_ctx =
            llama_init_from_model(model, cparams);

        if (raw_ctx == nullptr) {
            throw std::runtime_error(
                "Qwen context creation failed"
            );
        }

        std::unique_ptr<llama_context, decltype(&llama_free)>
            ctx(raw_ctx, llama_free);

        RavenAbortState callback_state = abort_state;

        llama_set_abort_callback(
            ctx.get(),
            raven_abort_callback,
            &callback_state
        );

        llama_batch prompt_batch = llama_batch_get_one(
            prompt_tokens.data(),
            static_cast<int32_t>(prompt_tokens.size())
        );

        if (llama_decode(ctx.get(), prompt_batch) != 0) {
            if (generation_cancelled(callback_state)) {
                return env->NewStringUTF("");
            }

            throw std::runtime_error(
                "Qwen prompt decode failed"
            );
        }

        llama_sampler_chain_params sampler_params =
            llama_sampler_chain_default_params();

        llama_sampler * raw_sampler =
            llama_sampler_chain_init(sampler_params);

        if (raw_sampler == nullptr) {
            throw std::runtime_error(
                "Qwen sampler creation failed"
            );
        }

        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>
            sampler(raw_sampler, llama_sampler_free);

        /*
         * Greedy decoding is intentional for the admission path. It matches
         * Raven's already-proven Qwen3 no-thinking harness and removes
         * stochastic behavior from release verification.
         */
        llama_sampler_chain_add(
            sampler.get(),
            llama_sampler_init_greedy()
        );

        std::string output;

        constexpr int MAX_NEW_TOKENS = 512;

        for (int i = 0; i < MAX_NEW_TOKENS; ++i) {
            if (generation_cancelled(callback_state)) {
                return env->NewStringUTF("");
            }

            const llama_token token = llama_sampler_sample(
                sampler.get(),
                ctx.get(),
                -1
            );

            if (llama_vocab_is_eog(vocab, token)) {
                break;
            }

            char stack_piece[512];

            int32_t piece_size = llama_token_to_piece(
                vocab,
                token,
                stack_piece,
                sizeof(stack_piece),
                0,
                true
            );

            std::string piece;

            if (piece_size < 0) {
                std::vector<char> dynamic_piece(
                    static_cast<size_t>(-piece_size)
                );

                piece_size = llama_token_to_piece(
                    vocab,
                    token,
                    dynamic_piece.data(),
                    static_cast<int32_t>(dynamic_piece.size()),
                    0,
                    true
                );

                if (piece_size > 0) {
                    piece.assign(
                        dynamic_piece.data(),
                        static_cast<size_t>(piece_size)
                    );
                }
            } else if (piece_size > 0) {
                piece.assign(
                    stack_piece,
                    static_cast<size_t>(piece_size)
                );
            }

            if (!piece.empty()) {
                output += piece;

                /*
                 * Fail closed if a future model/template regression exposes
                 * hidden reasoning despite enable_thinking=false.
                 */
                if (contains_think_markup(output)) {
                    throw std::runtime_error(
                        "Qwen hidden reasoning markup detected"
                    );
                }

                if (!emit_token(env, thiz, epoch, piece)) {
                    if (env->ExceptionCheck()) {
                        return nullptr;
                    }

                    throw std::runtime_error(
                        "Qwen token callback failed"
                    );
                }
            }

            llama_token next = token;

            llama_batch next_batch =
                llama_batch_get_one(&next, 1);

            if (llama_decode(ctx.get(), next_batch) != 0) {
                if (generation_cancelled(callback_state)) {
                    return env->NewStringUTF("");
                }

                throw std::runtime_error(
                    "Qwen generation decode failed"
                );
            }
        }

        if (generation_cancelled(callback_state)) {
            return env->NewStringUTF("");
        }

        if (output.empty()) {
            throw std::runtime_error(
                "Qwen generated an empty response"
            );
        }

        if (contains_think_markup(output)) {
            throw std::runtime_error(
                "Qwen hidden reasoning markup detected"
            );
        }

        return env->NewStringUTF(output.c_str());

    } catch (const std::exception & e) {
        LOGE("Qwen generation failed: %s", e.what());
        throw_illegal_state(env, e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeCancel(
    JNIEnv *,
    jobject,
    jlong
) {
    /*
     * Deliberately takes no Raven mutex. Cancellation must remain callable
     * while llama_decode/GGML compute is active.
     */
    g_cancel_epoch.fetch_add(1, std::memory_order_acq_rel);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ravencore_zero_ai_NativeQwenBackend_nativeClose(
    JNIEnv *,
    jobject
) {
    /*
     * Interrupt an active generation before waiting for its ownership lock.
     */
    g_cancel_epoch.fetch_add(1, std::memory_order_acq_rel);

    std::lock_guard<std::mutex> generation_guard(g_generation_mutex);
    std::lock_guard<std::mutex> lifecycle_guard(g_lifecycle_mutex);

    release_model_locked();

    if (g_backend_ready.exchange(false)) {
        llama_backend_free();
    }
}
