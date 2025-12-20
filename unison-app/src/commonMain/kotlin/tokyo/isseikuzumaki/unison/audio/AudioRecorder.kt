package tokyo.isseikuzumaki.unison.audio

/**
 * Cross-platform audio recorder interface for recording audio
 */
interface AudioRecorder {
    /**
     * Start recording audio
     * @param outputPath The path where the audio file should be saved
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(outputPath: String): Boolean

    /**
     * Stop recording and save the audio file
     * @return The path to the saved audio file, or null if recording failed
     */
    fun stopRecording(): String?

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean

    /**
     * Release all resources held by the recorder
     */
    fun release()

    /**
     * Get the duration of the current/last recording in milliseconds
     */
    fun getRecordingDuration(): Long
}

/**
 * Expect declaration for creating platform-specific audio recorder
 */
expect fun createAudioRecorder(): AudioRecorder
