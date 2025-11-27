package tokyo.isseikuzumaki.audioscriptplayer.domain

import tokyo.isseikuzumaki.audioscriptplayer.data.WhisperRawToken

/**
 * Interface for Whisper speech recognition.
 * In production, this would be implemented via JNI calls to whisper.cpp.
 * For prototype, a mock implementation is provided.
 */
interface WhisperWrapper {
    /**
     * Load the Whisper model from the given path.
     * @param modelPath Path to the ggml model file
     * @return true if loading succeeded
     */
    suspend fun loadModel(modelPath: String): Boolean
    
    /**
     * Check if a model is currently loaded.
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Transcribe audio file and return tokens with timestamps.
     * @param audioPath Path to the audio file
     * @return List of tokens with start/end timestamps
     */
    suspend fun transcribe(audioPath: String): List<WhisperRawToken>
    
    /**
     * Release the model from memory.
     */
    fun releaseModel()
}

/**
 * Mock implementation of WhisperWrapper for prototype testing.
 * Returns predefined transcription results.
 */
class MockWhisperWrapper : WhisperWrapper {
    private var modelLoaded = false
    
    override suspend fun loadModel(modelPath: String): Boolean {
        // Simulate loading delay
        kotlinx.coroutines.delay(1000)
        modelLoaded = true
        return true
    }
    
    override fun isModelLoaded(): Boolean = modelLoaded
    
    override suspend fun transcribe(audioPath: String): List<WhisperRawToken> {
        // Simulate transcription delay
        kotlinx.coroutines.delay(2000)
        
        // Return sample transcription data for testing
        // This represents what Whisper would return with timestamps
        return listOf(
            WhisperRawToken("Hello", 0, 500, 0.95f),
            WhisperRawToken("and", 500, 700, 0.92f),
            WhisperRawToken("welcome", 700, 1200, 0.94f),
            WhisperRawToken("to", 1200, 1400, 0.93f),
            WhisperRawToken("CNN", 1400, 1900, 0.91f),
            WhisperRawToken("news.", 1900, 2400, 0.89f),
            WhisperRawToken("Today", 2500, 3000, 0.95f),
            WhisperRawToken("we", 3000, 3200, 0.93f),
            WhisperRawToken("will", 3200, 3500, 0.92f),
            WhisperRawToken("discuss", 3500, 4200, 0.94f),
            WhisperRawToken("the", 4200, 4400, 0.91f),
            WhisperRawToken("latest", 4400, 4900, 0.93f),
            WhisperRawToken("developments", 4900, 5800, 0.88f),
            WhisperRawToken("in", 5800, 6000, 0.92f),
            WhisperRawToken("technology.", 6000, 7000, 0.90f)
        )
    }
    
    override fun releaseModel() {
        modelLoaded = false
    }
}
