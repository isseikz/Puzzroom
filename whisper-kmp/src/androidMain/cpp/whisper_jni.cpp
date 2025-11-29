#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// Get Whisper version
JNIEXPORT jstring JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetVersion(JNIEnv *env, jclass) {
    const char* version = whisper_version();
    return env->NewStringUTF(version);
}

// Initialize whisper context from file
JNIEXPORT jlong JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeInitFromFile(
    JNIEnv *env,
    jobject,
    jstring modelPath) {

    const char *model_path_cstr = env->GetStringUTFChars(modelPath, nullptr);

    LOGI("Initializing whisper context from: %s", model_path_cstr);

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = true; // Enable GPU acceleration if available

    struct whisper_context *ctx = whisper_init_from_file_with_params(model_path_cstr, cparams);

    env->ReleaseStringUTFChars(modelPath, model_path_cstr);

    if (ctx == nullptr) {
        LOGE("Failed to initialize whisper context");
        return 0;
    }

    LOGI("Whisper context initialized successfully");
    return reinterpret_cast<jlong>(ctx);
}

// Free whisper context
JNIEXPORT void JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeFree(JNIEnv *, jobject, jlong contextPtr) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx != nullptr) {
        whisper_free(ctx);
        LOGI("Whisper context freed");
    }
}

// Get default full parameters
JNIEXPORT jlong JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetDefaultParams(
    JNIEnv *,
    jobject,
    jlong contextPtr,
    jint strategy) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Context is null");
        return 0;
    }

    whisper_sampling_strategy sampling_strategy =
        static_cast<whisper_sampling_strategy>(strategy);

    struct whisper_full_params *params = new whisper_full_params();
    *params = whisper_full_default_params(sampling_strategy);

    return reinterpret_cast<jlong>(params);
}

// Free full parameters
JNIEXPORT void JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeFreeParams(JNIEnv *, jobject, jlong paramsPtr) {
    auto *params = reinterpret_cast<struct whisper_full_params *>(paramsPtr);
    if (params != nullptr) {
        delete params;
    }
}

// Set language
JNIEXPORT void JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeSetLanguage(
    JNIEnv *env,
    jobject,
    jlong paramsPtr,
    jstring language) {

    auto *params = reinterpret_cast<struct whisper_full_params *>(paramsPtr);
    if (params == nullptr) {
        LOGE("Params is null");
        return;
    }

    const char *lang_cstr = env->GetStringUTFChars(language, nullptr);
    params->language = lang_cstr;
    env->ReleaseStringUTFChars(language, lang_cstr);
}

// Set translate flag
JNIEXPORT void JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeSetTranslate(
    JNIEnv *,
    jobject,
    jlong paramsPtr,
    jboolean translate) {

    auto *params = reinterpret_cast<struct whisper_full_params *>(paramsPtr);
    if (params != nullptr) {
        params->translate = translate;
    }
}

// Set number of threads
JNIEXPORT void JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeSetThreads(
    JNIEnv *,
    jobject,
    jlong paramsPtr,
    jint threads) {

    auto *params = reinterpret_cast<struct whisper_full_params *>(paramsPtr);
    if (params != nullptr) {
        params->n_threads = threads;
    }
}

// Transcribe audio from float array
JNIEXPORT jint JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeTranscribe(
    JNIEnv *env,
    jobject,
    jlong contextPtr,
    jlong paramsPtr,
    jfloatArray audioData) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    auto *params = reinterpret_cast<struct whisper_full_params *>(paramsPtr);

    if (ctx == nullptr || params == nullptr) {
        LOGE("Context or params is null");
        return -1;
    }

    jsize audioLen = env->GetArrayLength(audioData);
    jfloat *audio = env->GetFloatArrayElements(audioData, nullptr);

    LOGI("Starting transcription with %d samples", audioLen);

    int result = whisper_full(ctx, *params, audio, audioLen);

    env->ReleaseFloatArrayElements(audioData, audio, JNI_ABORT);

    if (result != 0) {
        LOGE("Transcription failed with error code: %d", result);
    } else {
        LOGI("Transcription completed successfully");
    }

    return result;
}

// Get number of segments
JNIEXPORT jint JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetSegmentCount(
    JNIEnv *,
    jobject,
    jlong contextPtr) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        return 0;
    }

    return whisper_full_n_segments(ctx);
}

// Get segment text
JNIEXPORT jstring JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetSegmentText(
    JNIEnv *env,
    jobject,
    jlong contextPtr,
    jint segmentIndex) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        return env->NewStringUTF("");
    }

    const char *text = whisper_full_get_segment_text(ctx, segmentIndex);
    return env->NewStringUTF(text);
}

// Get segment start time (in milliseconds)
JNIEXPORT jlong JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetSegmentT0(
    JNIEnv *,
    jobject,
    jlong contextPtr,
    jint segmentIndex) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        return 0;
    }

    return whisper_full_get_segment_t0(ctx, segmentIndex) * 10; // Convert to milliseconds
}

// Get segment end time (in milliseconds)
JNIEXPORT jlong JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetSegmentT1(
    JNIEnv *,
    jobject,
    jlong contextPtr,
    jint segmentIndex) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        return 0;
    }

    return whisper_full_get_segment_t1(ctx, segmentIndex) * 10; // Convert to milliseconds
}

// Get full transcription as single string
JNIEXPORT jstring JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeGetFullText(
    JNIEnv *env,
    jobject,
    jlong contextPtr) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        return env->NewStringUTF("");
    }

    const int n_segments = whisper_full_n_segments(ctx);
    std::string result;

    for (int i = 0; i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        result += text;
    }

    return env->NewStringUTF(result.c_str());
}

// Check if a language is supported
JNIEXPORT jboolean JNICALL
Java_com_puzzroom_whisper_WhisperContext_nativeIsLanguageSupported(
    JNIEnv *env,
    jclass,
    jstring language) {

    const char *lang_cstr = env->GetStringUTFChars(language, nullptr);
    int lang_id = whisper_lang_id(lang_cstr);
    env->ReleaseStringUTFChars(language, lang_cstr);

    return lang_id >= 0;
}

} // extern "C"
