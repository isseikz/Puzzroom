package tokyo.isseikuzumaki.unison.data

import com.puzzroom.whisper.TranscriptionSegment

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

    /**
     * Transcribe audio from PCM data
     * @param pcmData PCM audio data (16-bit, mono)
     * @param sampleRate Sample rate of the audio (default: 44100Hz)
     * @return List of transcription segments with timestamps
     */
    suspend fun transcribeAudio(pcmData: ByteArray, sampleRate: Int = 44100): Result<List<TranscriptionSegment>>

    /**
     * Load transcription from a text file URI
     * Expected format: JSON array of transcription segments
     * @param uri File URI to load
     * @return List of transcription segments with timestamps
     */
    suspend fun loadTranscriptionFromFile(uri: String): Result<List<TranscriptionSegment>>
}
