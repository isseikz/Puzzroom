package tokyo.isseikuzumaki.audioscriptplayer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.audioscriptplayer.audio.AudioPlayer
import tokyo.isseikuzumaki.audioscriptplayer.audio.ExoAudioPlayer
import tokyo.isseikuzumaki.audioscriptplayer.audio.MockAudioPlayer
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignmentState
import tokyo.isseikuzumaki.audioscriptplayer.data.ModelState
import tokyo.isseikuzumaki.audioscriptplayer.domain.MockWhisperWrapper
import tokyo.isseikuzumaki.audioscriptplayer.domain.TextAligner
import tokyo.isseikuzumaki.audioscriptplayer.domain.WhisperWrapper
import tokyo.isseikuzumaki.audioscriptplayer.ui.pages.AlignmentViewModelInterface
import tokyo.isseikuzumaki.audioscriptplayer.ui.state.PlayerEvent
import tokyo.isseikuzumaki.audioscriptplayer.ui.state.PlayerUiState
import tokyo.isseikuzumaki.audioscriptplayer.whisper.WhisperJniWrapper

/**
 * Android-specific ViewModel with ExoPlayer and JNI Whisper integration.
 */
class AndroidAlignmentViewModel(
    private val whisperWrapper: WhisperWrapper,
    private val audioPlayer: AudioPlayer,
    private val textAligner: TextAligner = TextAligner(),
    private val modelFileName: String = DEFAULT_MODEL_FILENAME
) : ViewModel(), AlignmentViewModelInterface {
    
    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 50L
        
        /** Default Whisper model file name */
        const val DEFAULT_MODEL_FILENAME = "ggml-tiny.en.bin"
    }
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    init {
        // Collect audio player state changes
        viewModelScope.launch {
            audioPlayer.playbackState.collect { playbackState ->
                _uiState.update { 
                    it.copy(
                        isPlaying = playbackState.isPlaying,
                        durationMs = if (playbackState.durationMs > 0) {
                            playbackState.durationMs
                        } else {
                            it.durationMs
                        }
                    )
                }
                
                // Start/stop position updates based on playback state
                if (playbackState.isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
        }
    }
    
    override fun onEvent(event: PlayerEvent) {
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
                val success = whisperWrapper.loadModel(modelFileName)
                
                if (success) {
                    _uiState.update { it.copy(modelState = ModelState.Ready) }
                } else {
                    _uiState.update { 
                        it.copy(
                            modelState = ModelState.Error,
                            modelError = "Failed to load model: $modelFileName"
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
                val audioPath = currentState.audioUri ?: "sample.wav"
                val tokens = whisperWrapper.transcribe(audioPath)
                val alignedWords = textAligner.align(tokens, currentState.scriptText)
                val duration = alignedWords.lastOrNull()?.endTime ?: 0L
                
                // Prepare audio player
                audioPlayer.prepare(audioPath)
                
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
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                val currentPosition = audioPlayer.getCurrentPosition()
                val currentWordIndex = findWordAtPosition(currentPosition)
                
                _uiState.update { 
                    it.copy(
                        currentPositionMs = currentPosition,
                        currentWordIndex = currentWordIndex
                    )
                }
                
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    private fun findWordAtPosition(positionMs: Long): Int {
        val words = _uiState.value.alignedWords
        return words.indexOfFirst { word ->
            positionMs >= word.startTime && positionMs < word.endTime
        }
    }
    
    private fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
        val currentWordIndex = findWordAtPosition(positionMs)
        _uiState.update { 
            it.copy(
                currentPositionMs = positionMs,
                currentWordIndex = currentWordIndex
            )
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
        positionUpdateJob?.cancel()
        audioPlayer.release()
        whisperWrapper.releaseModel()
    }
    
    /**
     * Factory for creating AndroidAlignmentViewModel with context.
     * 
     * @param context Android context for ExoPlayer and file access
     * @param useRealImplementations Set to true for real Whisper + ExoPlayer, false for mock
     * @param modelFileName Whisper model filename (default: ggml-tiny.en.bin)
     */
    class Factory(
        private val context: Context,
        private val useRealImplementations: Boolean = false,
        private val modelFileName: String = DEFAULT_MODEL_FILENAME
    ) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val whisperWrapper: WhisperWrapper = if (useRealImplementations) {
                WhisperJniWrapper(context)
            } else {
                MockWhisperWrapper()
            }
            
            val audioPlayer: AudioPlayer = if (useRealImplementations) {
                ExoAudioPlayer(context)
            } else {
                MockAudioPlayer()
            }
            
            return AndroidAlignmentViewModel(
                whisperWrapper = whisperWrapper,
                audioPlayer = audioPlayer,
                modelFileName = modelFileName
            ) as T
        }
    }
}
