/**
 * Native JNI implementation for whisper.cpp integration.
 * 
 * This file provides the JNI bridge between Kotlin and whisper.cpp.
 * It must be compiled with the whisper.cpp library.
 * 
 * Key features:
 * - token_timestamps = true for word-level timestamps
 * - Returns JSON formatted results with timing information
 * 
 * Build requirements:
 * 1. Clone whisper.cpp repository
 * 2. Build libwhisper.so for target architectures (arm64-v8a, armeabi-v7a, x86_64)
 * 3. Copy .so files to jniLibs folder
 */

#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <android/log.h>

// Uncomment when whisper.cpp is available
// #include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Helper function to escape JSON special characters.
 * Declared before use to avoid forward declaration issues.
 */
std::string escapeJson(const std::string& input) {
    std::ostringstream ss;
    for (char c : input) {
        switch (c) {
            case '"':  ss << "\\\""; break;
            case '\\': ss << "\\\\"; break;
            case '\b': ss << "\\b"; break;
            case '\f': ss << "\\f"; break;
            case '\n': ss << "\\n"; break;
            case '\r': ss << "\\r"; break;
            case '\t': ss << "\\t"; break;
            default:   ss << c; break;
        }
    }
    return ss.str();
}

extern "C" {

/**
 * Load whisper model from file path.
 * 
 * @param env JNI environment
 * @param thiz Java object reference
 * @param modelPath Path to the ggml model file
 * @return Model context pointer (as jlong) or 0 on failure
 */
JNIEXPORT jlong JNICALL
Java_tokyo_isseikuzumaki_audioscriptplayer_whisper_WhisperJniWrapper_nativeLoadModel(
        JNIEnv *env,
        jobject thiz,
        jstring modelPath) {
    
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s", path);
    
    // TODO: Uncomment when whisper.cpp is integrated
    // struct whisper_context *ctx = whisper_init_from_file(path);
    // env->ReleaseStringUTFChars(modelPath, path);
    // 
    // if (ctx == nullptr) {
    //     LOGE("Failed to load model");
    //     return 0;
    // }
    // 
    // LOGI("Model loaded successfully");
    // return reinterpret_cast<jlong>(ctx);
    
    // Placeholder: Return mock pointer for development
    env->ReleaseStringUTFChars(modelPath, path);
    LOGI("Mock model loaded (whisper.cpp not integrated yet)");
    return 1; // Non-zero indicates "success" for mock
}

/**
 * Transcribe audio file with timestamps.
 * 
 * @param env JNI environment
 * @param thiz Java object reference
 * @param modelPtr Pointer to loaded model context
 * @param audioPath Path to audio file (WAV format, 16kHz mono recommended)
 * @return JSON string with transcription tokens and timestamps
 */
JNIEXPORT jstring JNICALL
Java_tokyo_isseikuzumaki_audioscriptplayer_whisper_WhisperJniWrapper_nativeTranscribeWithTimestamps(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr,
        jstring audioPath) {
    
    const char *path = env->GetStringUTFChars(audioPath, nullptr);
    LOGI("Transcribing audio: %s", path);
    
    // TODO: Uncomment when whisper.cpp is integrated
    // struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(modelPtr);
    // 
    // // Read audio file (must be 16kHz mono float32)
    // std::vector<float> pcmf32;
    // // ... load audio data ...
    // 
    // // Configure whisper parameters with timestamp support
    // whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    // params.print_realtime   = false;
    // params.print_progress   = false;
    // params.print_timestamps = false;
    // params.print_special    = false;
    // params.token_timestamps = true;  // CRITICAL: Enable token timestamps
    // params.max_len          = 0;
    // params.thold_pt         = 0.01f;
    // params.language         = "en";
    // 
    // // Run transcription
    // if (whisper_full(ctx, params, pcmf32.data(), pcmf32.size()) != 0) {
    //     LOGE("Transcription failed");
    //     env->ReleaseStringUTFChars(audioPath, path);
    //     return nullptr;
    // }
    // 
    // // Build JSON result with timestamps
    // std::ostringstream json;
    // json << "{\"tokens\":[";
    // 
    // const int n_segments = whisper_full_n_segments(ctx);
    // bool first = true;
    // 
    // for (int i = 0; i < n_segments; i++) {
    //     const int n_tokens = whisper_full_n_tokens(ctx, i);
    //     for (int j = 0; j < n_tokens; j++) {
    //         whisper_token_data token = whisper_full_get_token_data(ctx, i, j);
    //         
    //         // Skip special tokens
    //         if (token.id >= whisper_token_eot(ctx)) continue;
    //         
    //         const char *text = whisper_full_get_token_text(ctx, i, j);
    //         if (text == nullptr || strlen(text) == 0) continue;
    //         
    //         if (!first) json << ",";
    //         first = false;
    //         
    //         // Convert timestamps from 10ms units to milliseconds
    //         long t0_ms = token.t0 * 10;
    //         long t1_ms = token.t1 * 10;
    //         
    //         json << "{\"text\":\"" << escapeJson(text) << "\","
    //              << "\"t0\":" << t0_ms << ","
    //              << "\"t1\":" << t1_ms << ","
    //              << "\"p\":" << token.p << "}";
    //     }
    // }
    // 
    // json << "]}";
    // 
    // env->ReleaseStringUTFChars(audioPath, path);
    // return env->NewStringUTF(json.str().c_str());
    
    // Placeholder: Return mock transcription for development
    env->ReleaseStringUTFChars(audioPath, path);
    
    // Mock response matching expected format
    std::string mockJson = R"({
        "tokens": [
            {"text": "Hello", "t0": 0, "t1": 500, "p": 0.95},
            {"text": "and", "t0": 500, "t1": 700, "p": 0.92},
            {"text": "welcome", "t0": 700, "t1": 1200, "p": 0.94},
            {"text": "to", "t0": 1200, "t1": 1400, "p": 0.93},
            {"text": "CNN", "t0": 1400, "t1": 1900, "p": 0.91},
            {"text": "news.", "t0": 1900, "t1": 2400, "p": 0.89},
            {"text": "Today", "t0": 2500, "t1": 3000, "p": 0.95},
            {"text": "we", "t0": 3000, "t1": 3200, "p": 0.93},
            {"text": "will", "t0": 3200, "t1": 3500, "p": 0.92},
            {"text": "discuss", "t0": 3500, "t1": 4200, "p": 0.94},
            {"text": "the", "t0": 4200, "t1": 4400, "p": 0.91},
            {"text": "latest", "t0": 4400, "t1": 4900, "p": 0.93},
            {"text": "developments", "t0": 4900, "t1": 5800, "p": 0.88},
            {"text": "in", "t0": 5800, "t1": 6000, "p": 0.92},
            {"text": "technology.", "t0": 6000, "t1": 7000, "p": 0.90}
        ]
    })";
    
    LOGI("Returning mock transcription (whisper.cpp not integrated yet)");
    return env->NewStringUTF(mockJson.c_str());
}

/**
 * Free model from memory.
 * 
 * @param env JNI environment
 * @param thiz Java object reference
 * @param modelPtr Pointer to model context to free
 */
JNIEXPORT void JNICALL
Java_tokyo_isseikuzumaki_audioscriptplayer_whisper_WhisperJniWrapper_nativeFreeModel(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr) {
    
    LOGI("Freeing model");
    
    // TODO: Uncomment when whisper.cpp is integrated
    // struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(modelPtr);
    // if (ctx != nullptr) {
    //     whisper_free(ctx);
    // }
    
    LOGI("Model freed (mock)");
}

} // extern "C"
