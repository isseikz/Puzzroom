package tokyo.isseikuzumaki.unison.data

/**
 * Repository layer: Data loading
 * Loads audio files and converts them to PCM data
 */
interface AudioRepository {
    /**
     * Load PCM data from a file URI
     * Decodes audio to: 16-bit, 44.1kHz, Mono PCM format
     * @param uri File URI to load
     * @return PCM data as ByteArray
     */
    suspend fun loadPcmData(uri: String): Result<ByteArray>

    /**
     * Validate if the given URI points to an audio file
     * @param uri File URI to validate
     * @return true if valid audio file, false otherwise
     */
    suspend fun validateAudioFile(uri: String): Boolean
}
