package tokyo.isseikuzumaki.unison.audio

/**
 * Cross-platform audio player interface for playing audio files
 */
interface AudioPlayer {
    /**
     * Load an audio file from the given URI
     * @param uri The URI of the audio file to load
     * @return Duration of the audio in milliseconds, or 0 if unable to determine
     */
    suspend fun load(uri: String): Long

    /**
     * Start playing the loaded audio
     */
    fun play()

    /**
     * Pause the currently playing audio
     */
    fun pause()

    /**
     * Stop playback and release resources
     */
    fun stop()

    /**
     * Seek to a specific position in the audio
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)

    /**
     * Get the current playback position in milliseconds
     */
    fun getCurrentPosition(): Long

    /**
     * Get the duration of the loaded audio in milliseconds
     */
    fun getDuration(): Long

    /**
     * Check if audio is currently playing
     */
    fun isPlaying(): Boolean

    /**
     * Release all resources held by the player
     */
    fun release()

    /**
     * Set a callback for when playback completes
     */
    fun setOnCompletionListener(listener: () -> Unit)
}

/**
 * Expect declaration for creating platform-specific audio player
 */
expect fun createAudioPlayer(): AudioPlayer
