package tokyo.isseikuzumaki.audioscriptplayer.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.audioscriptplayer.data.WhisperRawToken
import tokyo.isseikuzumaki.audioscriptplayer.domain.WhisperWrapper
import java.io.File
import java.io.FileOutputStream

/**
 * Android JNI implementation of WhisperWrapper.
 * Uses whisper.cpp native library for speech recognition with timestamps.
 * 
 * Note: This requires the whisper.cpp native library (libwhisper.so) to be built
 * and included in the jniLibs folder. The native code must be modified to:
 * 1. Set token_timestamps = true in whisper_full_params
 * 2. Return structured data with timestamps instead of plain text
 */
class WhisperJniWrapper(private val context: Context) : WhisperWrapper {
    
    companion object {
        private const val TAG = "WhisperJniWrapper"
        private const val MODEL_FILENAME = "ggml-tiny.en.bin"
        
        // Load native library (matches CMake project name 'audioscriptplayer')
        init {
            try {
                System.loadLibrary("audioscriptplayer")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    private var modelPtr: Long = 0
    private var modelPath: String? = null
    
    /**
     * Copy model from assets to internal storage and load it.
     */
    override suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // First, try to copy model from assets if it exists there
            val internalModelPath = copyModelFromAssets(modelPath)
            
            // Load the model using JNI
            modelPtr = nativeLoadModel(internalModelPath)
            
            if (modelPtr != 0L) {
                this@WhisperJniWrapper.modelPath = internalModelPath
                Log.d(TAG, "Model loaded successfully: $internalModelPath")
                true
            } else {
                Log.e(TAG, "Failed to load model: nativeLoadModel returned 0")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            false
        }
    }
    
    /**
     * Copy model file from assets to internal storage.
     */
    private fun copyModelFromAssets(modelName: String): String {
        val outputFile = File(context.filesDir, modelName)
        
        // Skip if already exists
        if (outputFile.exists()) {
            Log.d(TAG, "Model file already exists: ${outputFile.absolutePath}")
            return outputFile.absolutePath
        }
        
        try {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Model copied to: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Model not found in assets, assuming direct path: $modelName")
            return modelName
        }
        
        return outputFile.absolutePath
    }
    
    override fun isModelLoaded(): Boolean = modelPtr != 0L
    
    /**
     * Transcribe audio file and return tokens with timestamps.
     * The native implementation must be configured with token_timestamps = true.
     */
    override suspend fun transcribe(audioPath: String): List<WhisperRawToken> = 
        withContext(Dispatchers.Default) {
            if (modelPtr == 0L) {
                throw IllegalStateException("Model not loaded")
            }
            
            try {
                // Call native transcription with timestamps enabled
                val jsonResult = nativeTranscribeWithTimestamps(modelPtr, audioPath)
                
                if (jsonResult.isNullOrEmpty()) {
                    Log.w(TAG, "Empty transcription result")
                    return@withContext emptyList()
                }
                
                // Parse JSON result into tokens
                parseTranscriptionResult(jsonResult)
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed: ${e.message}", e)
                throw e
            }
        }
    
    /**
     * Parse JSON transcription result into WhisperRawToken list.
     */
    private fun parseTranscriptionResult(jsonResult: String): List<WhisperRawToken> {
        return try {
            val result = Json.decodeFromString<TranscriptionResult>(jsonResult)
            result.tokens.map { token ->
                WhisperRawToken(
                    text = token.text,
                    startMs = token.t0,
                    endMs = token.t1,
                    prob = token.p
                )
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "JSON parsing error - invalid format: ${e.message}")
            Log.e(TAG, "JSON content (first 200 chars): ${jsonResult.take(200)}")
            parseFallbackResult(jsonResult)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "JSON parsing error - invalid argument: ${e.message}")
            parseFallbackResult(jsonResult)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing transcription JSON: ${e.javaClass.simpleName}: ${e.message}")
            parseFallbackResult(jsonResult)
        }
    }
    
    /**
     * Fallback parser for simpler JSON formats.
     */
    private fun parseFallbackResult(jsonResult: String): List<WhisperRawToken> {
        // If the native code returns a different format, handle it here
        Log.w(TAG, "Using fallback parser for result: $jsonResult")
        return emptyList()
    }
    
    override fun releaseModel() {
        if (modelPtr != 0L) {
            nativeFreeModel(modelPtr)
            modelPtr = 0
            modelPath = null
            Log.d(TAG, "Model released")
        }
    }
    
    // Native JNI methods
    // These must be implemented in native-lib.cpp with token_timestamps = true
    
    /**
     * Load whisper model from file path.
     * @return Model pointer or 0 on failure
     */
    private external fun nativeLoadModel(modelPath: String): Long
    
    /**
     * Transcribe audio with timestamps.
     * whisper_full_params must have token_timestamps = true
     * @return JSON string with token timestamps
     */
    private external fun nativeTranscribeWithTimestamps(modelPtr: Long, audioPath: String): String?
    
    /**
     * Free model from memory.
     */
    private external fun nativeFreeModel(modelPtr: Long)
}

/**
 * JSON structure for transcription result from native code.
 */
@Serializable
data class TranscriptionResult(
    val tokens: List<TokenData>
)

@Serializable
data class TokenData(
    val text: String,
    val t0: Long,  // Start time in milliseconds
    val t1: Long,  // End time in milliseconds
    val p: Float   // Probability
)
