package tokyo.isseikuzumaki.unison.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of AudioPlayer using MediaPlayer
 */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var currentUri: String? = null

    override suspend fun load(uri: String): Long = withContext(Dispatchers.IO) {
        try {
            // Release any existing player
            release()

            currentUri = uri
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                prepare()
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                }
            }

            getDuration()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun play() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            currentUri = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
}

/**
 * Actual implementation of createAudioPlayer for Android
 * Note: Context should be injected from the Android layer
 */
actual fun createAudioPlayer(): AudioPlayer {
    // This will be called from Android-specific code where we have context
    throw IllegalStateException("Use createAudioPlayer(context) from Android code")
}

/**
 * Android-specific factory function that takes context
 */
fun createAudioPlayer(context: Context): AudioPlayer {
    return AndroidAudioPlayer(context)
}
