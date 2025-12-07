package tokyo.isseikuzumaki.unison.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.unison.data.AudioEngine
import tokyo.isseikuzumaki.unison.data.AudioRepository
import com.puzzroom.whisper.TranscriptionSegment
import io.github.aakira.napier.Napier

/**
 * Heavy ViewModel scoped to Session Navigation Graph
 * Handles all audio operations: loading, recording, playback, and mixing
 * Automatically cleaned up when user returns to Library screen
 */
class SessionViewModel(
    private val uri: String,
    private val audioRepository: AudioRepository,
    private val audioEngine: AudioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var originalPcmData: ByteArray? = null
    private var recordedPcmData: ByteArray? = null
    private var recordingJob: Job? = null
    private var transcriptionSegments: List<TranscriptionSegment> = emptyList()
    private var recordedTranscriptionSegments: List<TranscriptionSegment> = emptyList()

    // Sync editor settings
    private val _offsetMs = MutableStateFlow(0)
    val offsetMs: StateFlow<Int> = _offsetMs.asStateFlow()

    private val _balance = MutableStateFlow(0.5f)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    init {
        loadAudio(uri)
    }

    private fun loadAudio(uri: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading
            audioRepository.loadPcmData(uri).fold(
                onSuccess = { pcmData ->
                    originalPcmData = pcmData
                    val fileName = extractFileName(uri)
                    val durationMs = audioEngine.getDurationMs(pcmData)

                    // Show ready state first
                    _uiState.value = SessionUiState.Ready(
                        fileName = fileName,
                        durationMs = durationMs
                    )

                    // Start transcription in background
                    launch {
                        audioRepository.transcribeAudio(pcmData, sampleRate = 44100).fold(
                            onSuccess = { segments ->
                                transcriptionSegments = segments
                                // Update state with transcription
                                _uiState.value = SessionUiState.Ready(
                                    fileName = fileName,
                                    durationMs = durationMs,
                                    transcription = segments
                                )
                            },
                            onFailure = { error ->
                                // Log error but don't fail the whole UI
                                Napier.e("Transcription failed", error)
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = SessionUiState.Error(error.message ?: "Failed to load audio")
                }
            )
        }
    }

    fun startRecording() {
        Napier.i { "Start recording" }
        val pcm = originalPcmData ?: return

        viewModelScope.launch {
            try {
                // Start playback of original audio
                val playbackResult = audioEngine.playOriginal(pcm)
                playbackResult.onFailure { error ->
                    _uiState.value = SessionUiState.Error("Failed to start playback: ${error.message}")
                    return@launch
                }

                // Update UI to recording state first
                _uiState.value = SessionUiState.Recording(
                    fileName = extractFileName(uri),
                    durationMs = audioEngine.getDurationMs(pcm),
                    transcription = transcriptionSegments
                )

                 recordingJob = launch {
                     try {
                         audioEngine.startRecording().collect { chunk ->
                             // Chunks are collected in the engine
                         }
                     } catch (e: Exception) {
                         _uiState.value = SessionUiState.Error("Recording failed: ${e.message}")
                     }
                 }
            } catch (e: Exception) {
                _uiState.value = SessionUiState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        Napier.i { "Stop Recording" }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                audioEngine.stopRecording()
                audioEngine.stopPlayback()
                recordingJob?.cancel()
                recordingJob = null

                recordedPcmData = audioEngine.getRecordedData()
            }

            if (recordedPcmData != null) {
                // Show processing state while transcribing
                _uiState.value = SessionUiState.Processing(
                    fileName = extractFileName(uri),
                    message = "Transcribing your voice..."
                )

                // Transcribe recorded audio
                audioRepository.transcribeAudio(recordedPcmData!!, sampleRate = 44100).fold(
                    onSuccess = { segments ->
                        recordedTranscriptionSegments = segments

                        // Update processing message
                        _uiState.value = SessionUiState.Processing(
                            fileName = extractFileName(uri),
                            message = "Calculating sync offset..."
                        )

                        // Calculate optimal offset using transcription alignment
                        val calculatedOffset = calculateOptimalOffset(
                            originalTranscription = transcriptionSegments,
                            recordedTranscription = segments
                        )

                        // Update offset with calculated value
                        _offsetMs.value = calculatedOffset

                        // Update state with recorded transcription and auto-calculated offset
                        _uiState.value = SessionUiState.Editing(
                            fileName = extractFileName(uri),
                            durationMs = audioEngine.getDurationMs(originalPcmData!!),
                            offsetMs = calculatedOffset,
                            balance = _balance.value,
                            transcription = transcriptionSegments,
                            recordedTranscription = segments
                        )

                        Napier.i("Auto-calculated offset: ${calculatedOffset}ms")
                    },
                    onFailure = { error ->
                        Napier.e("Recorded transcription failed", error)
                        // Even if transcription fails, allow user to edit with default offset
                        _uiState.value = SessionUiState.Editing(
                            fileName = extractFileName(uri),
                            durationMs = audioEngine.getDurationMs(originalPcmData!!),
                            offsetMs = 0,
                            balance = _balance.value,
                            transcription = transcriptionSegments,
                            recordedTranscription = emptyList()
                        )
                    }
                )
            } else {
                _uiState.value = SessionUiState.Error("No recording data")
            }
        }
    }

    fun setOffset(offsetMs: Int) {
        _offsetMs.value = offsetMs
        val current = _uiState.value
        if (current is SessionUiState.Editing) {
            _uiState.value = current.copy(offsetMs = offsetMs)
        }
    }

    fun setBalance(balance: Float) {
        _balance.value = balance
        val current = _uiState.value
        if (current is SessionUiState.Editing) {
            _uiState.value = current.copy(balance = balance)
        }
    }

    fun getCurrentActiveSegment(): TranscriptionSegment? {
        val currentPositionMs = getCurrentPositionMs()
        return transcriptionSegments.firstOrNull { segment ->
            currentPositionMs in segment.startTimeMs..segment.endTimeMs
        }
    }

    fun playPreview() {
        val original = originalPcmData ?: return
        val recorded = recordedPcmData ?: return

        viewModelScope.launch {
            audioEngine.playDual(
                original = original,
                recorded = recorded,
                offsetMs = _offsetMs.value,
                balance = _balance.value
            )
        }
    }

    fun stopPreview() {
        audioEngine.stopPlayback()
    }

    fun retryRecording() {
        recordedPcmData = null
        _uiState.value = SessionUiState.Ready(
            fileName = extractFileName(uri),
            durationMs = audioEngine.getDurationMs(originalPcmData!!),
            transcription = transcriptionSegments
        )
    }

    fun getCurrentPositionMs(): Long {
        return audioEngine.getCurrentPositionMs()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        originalPcmData = null
        recordedPcmData = null
    }

    private fun extractFileName(uri: String): String {
        return uri.substringAfterLast('/').substringBeforeLast('.')
    }

    /**
     * Calculate optimal offset to align recorded audio with original audio
     * Uses transcription timestamps to find the best time alignment
     *
     * Algorithm:
     * 1. Find matching text segments between original and recorded
     * 2. Calculate time difference for each match
     * 3. Return median offset (robust to outliers)
     *
     * @param originalTranscription Transcription of original audio
     * @param recordedTranscription Transcription of recorded audio
     * @return Offset in milliseconds (negative = recorded is ahead, positive = recorded is behind)
     */
    private fun calculateOptimalOffset(
        originalTranscription: List<TranscriptionSegment>,
        recordedTranscription: List<TranscriptionSegment>
    ): Int {
        if (originalTranscription.isEmpty() || recordedTranscription.isEmpty()) {
            Napier.w("Cannot calculate offset: empty transcriptions")
            return 0
        }

        val timeOffsets = mutableListOf<Long>()

        // Match segments by text similarity
        for (recordedSeg in recordedTranscription) {
            for (originalSeg in originalTranscription) {
                // Calculate text similarity (simple word overlap)
                val similarity = calculateTextSimilarity(originalSeg.text, recordedSeg.text)

                // If similarity is high enough, use this as a matching point
                if (similarity > 0.5) {
                    // Calculate time offset
                    // Negative offset means recorded is ahead (needs to be delayed)
                    // Positive offset means recorded is behind (needs to be advanced)
                    val offset = recordedSeg.startTimeMs - originalSeg.startTimeMs
                    timeOffsets.add(offset)

                    Napier.d(
                        "Match: '${originalSeg.text.take(30)}...' @ ${originalSeg.startTimeMs}ms " +
                        "vs '${recordedSeg.text.take(30)}...' @ ${recordedSeg.startTimeMs}ms " +
                        "-> offset: ${offset}ms (similarity: ${similarity})"
                    )
                }
            }
        }

        if (timeOffsets.isEmpty()) {
            Napier.w("No matching segments found for alignment")
            return 0
        }

        // Use median offset (more robust than mean against outliers)
        val sortedOffsets = timeOffsets.sorted()
        val medianOffset = if (sortedOffsets.size % 2 == 0) {
            (sortedOffsets[sortedOffsets.size / 2 - 1] + sortedOffsets[sortedOffsets.size / 2]) / 2
        } else {
            sortedOffsets[sortedOffsets.size / 2]
        }

        // Negate the offset: if recorded is ahead, we need negative offset to delay it
        val correctionOffset = -medianOffset.toInt()

        Napier.i(
            "Alignment complete: ${timeOffsets.size} matches, median offset: ${medianOffset}ms, " +
            "correction: ${correctionOffset}ms"
        )

        return correctionOffset
    }

    /**
     * Calculate text similarity using Jaccard similarity (word overlap)
     * Returns value between 0.0 (no overlap) and 1.0 (identical)
     */
    private fun calculateTextSimilarity(text1: String, text2: String): Double {
        val words1 = text1.lowercase().trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .toSet()

        val words2 = text2.lowercase().trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .toSet()

        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return intersection.toDouble() / union.toDouble()
    }
}

sealed class SessionUiState {
    data object Loading : SessionUiState()

    data class Ready(
        val fileName: String,
        val durationMs: Long,
        val transcription: List<TranscriptionSegment> = emptyList()
    ) : SessionUiState()

    data class Recording(
        val fileName: String,
        val durationMs: Long,
        val transcription: List<TranscriptionSegment> = emptyList()
    ) : SessionUiState()

    data class Processing(
        val fileName: String,
        val message: String = "Processing audio..."
    ) : SessionUiState()

    data class Editing(
        val fileName: String,
        val durationMs: Long,
        val offsetMs: Int,
        val balance: Float,
        val transcription: List<TranscriptionSegment> = emptyList(),
        val recordedTranscription: List<TranscriptionSegment> = emptyList()
    ) : SessionUiState()

    data class Error(val message: String) : SessionUiState()
}
