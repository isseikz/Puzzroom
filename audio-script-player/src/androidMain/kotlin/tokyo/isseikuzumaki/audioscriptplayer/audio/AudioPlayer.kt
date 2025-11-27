package tokyo.isseikuzumaki.audioscriptplayer.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Audio playback state.
 */
data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val isReady: Boolean = false,
    val error: String? = null
)

/**
 * Interface for audio playback functionality.
 */
interface AudioPlayer {
    val playbackState: StateFlow<AudioPlaybackState>
    
    fun prepare(audioUri: String)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
    fun getCurrentPosition(): Long
}

/**
 * ExoPlayer-based implementation of AudioPlayer.
 * Provides actual audio playback functionality for Android.
 */
@OptIn(UnstableApi::class)
class ExoAudioPlayer(private val context: Context) : AudioPlayer {
    
    private var exoPlayer: ExoPlayer? = null
    
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        isReady = true,
                        durationMs = exoPlayer?.duration ?: 0,
                        error = null
                    )
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0
                    )
                }
                Player.STATE_BUFFERING -> {
                    // Buffering state
                }
                Player.STATE_IDLE -> {
                    _playbackState.value = _playbackState.value.copy(
                        isReady = false
                    )
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = isPlaying
            )
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _playbackState.value = _playbackState.value.copy(
                error = error.message ?: "Playback error",
                isPlaying = false
            )
        }
    }
    
    override fun prepare(audioUri: String) {
        release() // Release any existing player
        
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
            
            val mediaItem = when {
                audioUri.startsWith("content://") || audioUri.startsWith("file://") -> {
                    MediaItem.fromUri(Uri.parse(audioUri))
                }
                audioUri.startsWith("/") -> {
                    MediaItem.fromUri(Uri.parse("file://$audioUri"))
                }
                else -> {
                    // Assume it's an asset file - use android_asset path
                    MediaItem.fromUri(Uri.parse("file:///android_asset/$audioUri"))
                }
            }
            
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    override fun play() {
        exoPlayer?.play()
    }
    
    override fun pause() {
        exoPlayer?.pause()
    }
    
    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = positionMs
        )
    }
    
    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }
    
    override fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = AudioPlaybackState()
    }
}

/**
 * Mock implementation of AudioPlayer for testing without actual audio.
 */
class MockAudioPlayer : AudioPlayer {
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()
    
    private var mockDuration: Long = 7000 // 7 seconds mock duration
    
    override fun prepare(audioUri: String) {
        _playbackState.value = AudioPlaybackState(
            isReady = true,
            durationMs = mockDuration
        )
    }
    
    override fun play() {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }
    
    override fun pause() {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }
    
    override fun seekTo(positionMs: Long) {
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = positionMs.coerceIn(0, mockDuration)
        )
    }
    
    override fun getCurrentPosition(): Long {
        return _playbackState.value.currentPositionMs
    }
    
    override fun release() {
        _playbackState.value = AudioPlaybackState()
    }
    
    // For testing: update position externally
    fun updatePosition(positionMs: Long) {
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = positionMs.coerceIn(0, mockDuration)
        )
    }
}
