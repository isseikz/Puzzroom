package tokyo.isseikuzumaki.unison.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Android implementation of AudioRecorder using MediaRecorder
 */
class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartTime: Long = 0
    private var currentOutputPath: String? = null
    private var isRecordingFlag = false

    override fun startRecording(outputPath: String): Boolean {
        return try {
            // Release any existing recorder
            release()

            // Create output directory if it doesn't exist
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            currentOutputPath = outputPath
            recordingStartTime = System.currentTimeMillis()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputPath)

                prepare()
                start()
            }

            isRecordingFlag = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            release()
            false
        }
    }

    override fun stopRecording(): String? {
        return try {
            if (isRecordingFlag) {
                mediaRecorder?.apply {
                    stop()
                    reset()
                }
                isRecordingFlag = false
                currentOutputPath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            release()
        }
    }

    override fun isRecording(): Boolean {
        return isRecordingFlag
    }

    override fun release() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecordingFlag = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getRecordingDuration(): Long {
        return if (isRecordingFlag) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }
}

/**
 * Actual implementation of createAudioRecorder for Android
 * Note: Context should be injected from the Android layer
 */
actual fun createAudioRecorder(): AudioRecorder {
    // This will be called from Android-specific code where we have context
    throw IllegalStateException("Use createAudioRecorder(context) from Android code")
}

/**
 * Android-specific factory function that takes context
 */
fun createAudioRecorder(context: Context): AudioRecorder {
    return AndroidAudioRecorder(context)
}
