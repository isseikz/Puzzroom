package tokyo.isseikuzumaki.audioscriptplayer.data

import kotlinx.serialization.Serializable

/**
 * Raw token data returned from Whisper via JNI.
 * Contains the recognized text with timestamp information.
 */
@Serializable
data class WhisperRawToken(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val prob: Float
)

/**
 * Word data after alignment processing.
 * Used for UI rendering with highlight support.
 */
data class AlignedWord(
    val originalWord: String,
    val startTime: Long,
    val endTime: Long,
    val isHighlighted: Boolean = false
)

/**
 * State of the Whisper model loading.
 */
enum class ModelState {
    NotLoaded,
    Loading,
    Ready,
    Error
}

/**
 * State of the alignment processing.
 */
enum class AlignmentState {
    Idle,
    Processing,
    Completed,
    Error
}
