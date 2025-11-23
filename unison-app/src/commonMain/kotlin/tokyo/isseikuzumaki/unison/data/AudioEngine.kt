package tokyo.isseikuzumaki.unison.data

import kotlinx.coroutines.flow.Flow

/**
 * Engine layer: Playback and recording control
 */
interface AudioEngine {
    /**
     * Start recording from microphone
     * @return Flow of recorded PCM chunks
     */
    fun startRecording(): Flow<ByteArray>

    /**
     * Stop recording
     */
    fun stopRecording()

    /**
     * Get the complete recorded data
     * @return Recorded PCM data as ByteArray
     */
    fun getRecordedData(): ByteArray?

    /**
     * Play dual audio (original + recorded) with offset
     * @param original Original PCM data
     * @param recorded Recorded PCM data
     * @param offsetMs Offset in milliseconds (negative = advance recorded, positive = delay recorded)
     * @param balance Mix balance (0.0 = only original, 1.0 = only recorded, 0.5 = equal mix)
     */
    suspend fun playDual(
        original: ByteArray,
        recorded: ByteArray,
        offsetMs: Int,
        balance: Float
    ): Result<Unit>

    /**
     * Play original audio only
     * @param pcmData PCM data to play
     */
    suspend fun playOriginal(pcmData: ByteArray): Result<Unit>

    /**
     * Stop playback
     */
    fun stopPlayback()

    /**
     * Release all resources
     */
    fun release()

    /**
     * Get current playback position in milliseconds
     */
    fun getCurrentPositionMs(): Long

    /**
     * Get total duration in milliseconds
     */
    fun getDurationMs(pcmData: ByteArray): Long
}
