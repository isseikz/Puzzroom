package tokyo.isseikuzumaki.audioscriptplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignmentState
import tokyo.isseikuzumaki.audioscriptplayer.data.ModelState
import tokyo.isseikuzumaki.audioscriptplayer.domain.MockWhisperWrapper
import tokyo.isseikuzumaki.audioscriptplayer.domain.TextAligner
import tokyo.isseikuzumaki.audioscriptplayer.domain.WhisperWrapper
import tokyo.isseikuzumaki.audioscriptplayer.ui.state.PlayerEvent
import tokyo.isseikuzumaki.audioscriptplayer.ui.state.PlayerUiState

/**
 * ViewModel for the Audio-Script Alignment Player.
 * Manages model loading, alignment processing, and playback state.
 */
class AlignmentViewModel(
    private val whisperWrapper: WhisperWrapper = MockWhisperWrapper(),
    private val textAligner: TextAligner = TextAligner()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private var playbackJob: Job? = null
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.LoadModel -> loadModel()
            is PlayerEvent.ProcessAlignment -> processAlignment()
            is PlayerEvent.PlayPause -> togglePlayback()
            is PlayerEvent.Seek -> seekTo(event.positionMs)
            is PlayerEvent.UpdateScript -> updateScript(event.text)
            is PlayerEvent.SelectAudio -> selectAudio(event.uri)
            is PlayerEvent.WordClicked -> seekToWord(event.index)
        }
    }
    
    private fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(modelState = ModelState.Loading, modelError = null) }
            
            try {
                // In real implementation, would pass actual model path
                val success = whisperWrapper.loadModel("ggml-tiny.en.bin")
                
                if (success) {
                    _uiState.update { it.copy(modelState = ModelState.Ready) }
                } else {
                    _uiState.update { 
                        it.copy(
                            modelState = ModelState.Error,
                            modelError = "Failed to load model"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        modelState = ModelState.Error,
                        modelError = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
    
    private fun processAlignment() {
        val currentState = _uiState.value
        
        if (currentState.modelState != ModelState.Ready) {
            _uiState.update { 
                it.copy(alignmentError = "Model not loaded. Please load model first.")
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(alignmentState = AlignmentState.Processing, alignmentError = null)
            }
            
            try {
                // Get transcription from Whisper (mock)
                val audioPath = currentState.audioUri ?: "sample.wav"
                val tokens = whisperWrapper.transcribe(audioPath)
                
                // Align with script
                val alignedWords = textAligner.align(tokens, currentState.scriptText)
                
                // Calculate total duration from aligned words
                val duration = alignedWords.lastOrNull()?.endTime ?: 0L
                
                _uiState.update { 
                    it.copy(
                        alignmentState = AlignmentState.Completed,
                        alignedWords = alignedWords,
                        durationMs = duration
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        alignmentState = AlignmentState.Error,
                        alignmentError = e.message ?: "Alignment failed"
                    )
                }
            }
        }
    }
    
    private fun togglePlayback() {
        val currentState = _uiState.value
        
        if (currentState.alignedWords.isEmpty()) {
            _uiState.update { it.copy(alignmentError = "Process alignment first.") }
            return
        }
        
        if (currentState.isPlaying) {
            // Pause
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            // Play
            _uiState.update { it.copy(isPlaying = true) }
            startPlaybackSimulation()
        }
    }
    
    private fun startPlaybackSimulation() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val startPosition = _uiState.value.currentPositionMs
            var elapsed = 0L
            
            while (isActive) {
                val currentPosition = startPosition + elapsed
                val duration = _uiState.value.durationMs
                
                if (currentPosition >= duration) {
                    // Reached end
                    _uiState.update { 
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = 0,
                            currentWordIndex = -1
                        )
                    }
                    break
                }
                
                // Update position and find current word
                val currentWordIndex = findWordAtPosition(currentPosition)
                _uiState.update { 
                    it.copy(
                        currentPositionMs = currentPosition,
                        currentWordIndex = currentWordIndex
                    )
                }
                
                delay(50) // Update every 50ms
                elapsed += 50
            }
        }
    }
    
    private fun findWordAtPosition(positionMs: Long): Int {
        val words = _uiState.value.alignedWords
        return words.indexOfFirst { word ->
            positionMs >= word.startTime && positionMs < word.endTime
        }
    }
    
    private fun seekTo(positionMs: Long) {
        val wasPlaying = _uiState.value.isPlaying
        
        playbackJob?.cancel()
        
        val currentWordIndex = findWordAtPosition(positionMs)
        _uiState.update { 
            it.copy(
                currentPositionMs = positionMs,
                currentWordIndex = currentWordIndex
            )
        }
        
        if (wasPlaying) {
            _uiState.update { it.copy(isPlaying = true) }
            startPlaybackSimulation()
        }
    }
    
    private fun seekToWord(index: Int) {
        val words = _uiState.value.alignedWords
        if (index in words.indices) {
            seekTo(words[index].startTime)
        }
    }
    
    private fun updateScript(text: String) {
        _uiState.update { 
            it.copy(
                scriptText = text,
                alignedWords = emptyList(),
                alignmentState = AlignmentState.Idle,
                currentWordIndex = -1
            )
        }
    }
    
    private fun selectAudio(uri: String) {
        _uiState.update { 
            it.copy(
                audioUri = uri,
                alignedWords = emptyList(),
                alignmentState = AlignmentState.Idle,
                currentWordIndex = -1
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        whisperWrapper.releaseModel()
    }
}
