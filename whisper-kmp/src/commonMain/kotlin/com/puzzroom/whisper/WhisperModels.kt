package com.puzzroom.whisper

/**
 * Represents a transcription segment with text and timing information
 */
data class TranscriptionSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val index: Int
)

/**
 * Complete transcription result
 */
data class TranscriptionResult(
    val segments: List<TranscriptionSegment>,
    val fullText: String
)

/**
 * Sampling strategy for transcription
 */
enum class SamplingStrategy(val value: Int) {
    GREEDY(0),
    BEAM_SEARCH(1)
}

/**
 * Transcription parameters
 */
data class TranscriptionParams(
    val language: String = "en",
    val translate: Boolean = false,
    val threads: Int = 4,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.GREEDY,
    val noContext: Boolean = false,
    val singleSegment: Boolean = false,
    val printProgress: Boolean = false,
    val printRealtime: Boolean = false,
    val printTimestamps: Boolean = true
)

/**
 * Exception thrown when Whisper operations fail
 */
class WhisperException(message: String, cause: Throwable? = null) : Exception(message, cause)
