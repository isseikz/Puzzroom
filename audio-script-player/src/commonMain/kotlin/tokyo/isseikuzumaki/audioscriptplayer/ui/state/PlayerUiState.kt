package tokyo.isseikuzumaki.audioscriptplayer.ui.state

import tokyo.isseikuzumaki.audioscriptplayer.data.AlignedWord
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignmentState
import tokyo.isseikuzumaki.audioscriptplayer.data.ModelState

/**
 * UI state for the Audio-Script Alignment Player.
 */
data class PlayerUiState(
    // Model state
    val modelState: ModelState = ModelState.NotLoaded,
    val modelError: String? = null,
    
    // Alignment state
    val alignmentState: AlignmentState = AlignmentState.Idle,
    val alignmentError: String? = null,
    
    // Script and alignment
    val scriptText: String = SAMPLE_SCRIPT,
    val alignedWords: List<AlignedWord> = emptyList(),
    val currentWordIndex: Int = -1,
    
    // Audio playback state
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    
    // Audio file
    val audioUri: String? = null
) {
    companion object {
        const val SAMPLE_SCRIPT = "Hello and welcome to CNN news. Today we will discuss the latest developments in technology."
    }
}

/**
 * Events from the UI to the ViewModel.
 */
sealed class PlayerEvent {
    data object LoadModel : PlayerEvent()
    data object ProcessAlignment : PlayerEvent()
    data object PlayPause : PlayerEvent()
    data class Seek(val positionMs: Long) : PlayerEvent()
    data class UpdateScript(val text: String) : PlayerEvent()
    data class SelectAudio(val uri: String) : PlayerEvent()
    data class WordClicked(val index: Int) : PlayerEvent()
}
