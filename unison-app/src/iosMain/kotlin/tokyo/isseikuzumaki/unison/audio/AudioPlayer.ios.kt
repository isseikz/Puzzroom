package tokyo.isseikuzumaki.unison.audio

import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of AudioPlayer using AVAudioPlayer
 */
class IOSAudioPlayer : AudioPlayer {
    private var audioPlayer: AVAudioPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    private val delegate = AudioPlayerDelegate()

    init {
        delegate.onCompletion = { onCompletionListener?.invoke() }
    }

    override suspend fun load(uri: String): Long = withContext(Dispatchers.Main) {
        try {
            // Release any existing player
            release()

            val url = NSURL.URLWithString(uri) ?: return@withContext 0L

            audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null).apply {
                this.delegate = this@IOSAudioPlayer.delegate
                prepareToPlay()
            }

            getDuration()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun play() {
        try {
            audioPlayer?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun pause() {
        try {
            audioPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        try {
            audioPlayer?.stop()
            audioPlayer?.currentTime = 0.0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun seekTo(positionMs: Long) {
        try {
            audioPlayer?.currentTime = positionMs / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentPosition(): Long {
        return try {
            ((audioPlayer?.currentTime ?: 0.0) * 1000).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun getDuration(): Long {
        return try {
            ((audioPlayer?.duration ?: 0.0) * 1000).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun isPlaying(): Boolean {
        return try {
            audioPlayer?.playing ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun release() {
        try {
            audioPlayer?.stop()
            audioPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    /**
     * Delegate for AVAudioPlayer callbacks
     */
    private class AudioPlayerDelegate : NSObject(), AVAudioPlayerDelegateProtocol {
        var onCompletion: (() -> Unit)? = null

        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            onCompletion?.invoke()
        }
    }
}

/**
 * Actual implementation of createAudioPlayer for iOS
 */
actual fun createAudioPlayer(): AudioPlayer {
    return IOSAudioPlayer()
}
