package com.puzzroom.whisper

/**
 * Main interface for Whisper speech recognition
 */
interface Whisper {

    /**
     * Get the version of the Whisper library
     */
    fun getVersion(): String

    /**
     * Initialize Whisper with a model file
     *
     * @param modelPath Path to the GGML model file
     * @return WhisperContext instance for transcription
     * @throws WhisperException if initialization fails
     */
    fun initFromFile(modelPath: String): WhisperContext

    /**
     * Check if a language is supported
     *
     * @param languageCode ISO language code (e.g., "en", "es", "fr")
     * @return true if the language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean

    companion object {
        /**
         * Get the platform-specific Whisper instance
         */
        expect fun create(): Whisper
    }
}

/**
 * Whisper context for performing transcriptions
 */
interface WhisperContext : AutoCloseable {

    /**
     * Transcribe audio data
     *
     * @param audioData PCM audio data (16kHz, mono, float32)
     * @param params Transcription parameters
     * @return Transcription result with segments and full text
     * @throws WhisperException if transcription fails
     */
    fun transcribe(
        audioData: FloatArray,
        params: TranscriptionParams = TranscriptionParams()
    ): TranscriptionResult

    /**
     * Release native resources
     */
    override fun close()
}
