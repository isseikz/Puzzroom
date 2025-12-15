package tokyo.isseikuzumaki.unison.data

/**
 * Represents a transcription segment with text and timing information
 */
data class TranscriptionSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val index: Int = 0
)
