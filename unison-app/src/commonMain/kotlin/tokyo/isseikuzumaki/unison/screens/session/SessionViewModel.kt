package tokyo.isseikuzumaki.unison.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.unison.data.AudioEngine
import tokyo.isseikuzumaki.unison.data.AudioRepository

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
                    _uiState.value = SessionUiState.Ready(
                        fileName = extractFileName(uri),
                        durationMs = audioEngine.getDurationMs(pcmData)
                    )
                },
                onFailure = { error ->
                    _uiState.value = SessionUiState.Error(error.message ?: "Failed to load audio")
                }
            )
        }
    }

    fun startRecording() {
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
                    durationMs = audioEngine.getDurationMs(pcm)
                )

                // TODO: Re-enable recording functionality after debugging microphone issues
                // Start recording - COMMENTED OUT FOR DEBUGGING
                // recordingJob = launch {
                //     try {
                //         audioEngine.startRecording().collect { chunk ->
                //             // Chunks are collected in the engine
                //         }
                //     } catch (e: Exception) {
                //         _uiState.value = SessionUiState.Error("Recording failed: ${e.message}")
                //     }
                // }
            } catch (e: Exception) {
                _uiState.value = SessionUiState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        audioEngine.stopRecording()
        audioEngine.stopPlayback()
        recordingJob?.cancel()
        recordingJob = null

        recordedPcmData = audioEngine.getRecordedData()

        if (recordedPcmData != null) {
            _uiState.value = SessionUiState.Editing(
                fileName = extractFileName(uri),
                durationMs = audioEngine.getDurationMs(originalPcmData!!),
                offsetMs = _offsetMs.value,
                balance = _balance.value
            )
        } else {
            _uiState.value = SessionUiState.Error("No recording data")
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
            durationMs = audioEngine.getDurationMs(originalPcmData!!)
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
}

sealed class SessionUiState {
    data object Loading : SessionUiState()

    data class Ready(
        val fileName: String,
        val durationMs: Long
    ) : SessionUiState()

    data class Recording(
        val fileName: String,
        val durationMs: Long
    ) : SessionUiState()

    data class Editing(
        val fileName: String,
        val durationMs: Long,
        val offsetMs: Int,
        val balance: Float
    ) : SessionUiState()

    data class Error(val message: String) : SessionUiState()
}
