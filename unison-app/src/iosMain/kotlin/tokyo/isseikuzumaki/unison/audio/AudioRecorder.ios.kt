package tokyo.isseikuzumaki.unison.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * iOS implementation of AudioRecorder using AVAudioRecorder
 */
class IOSAudioRecorder : AudioRecorder {
    private var audioRecorder: AVAudioRecorder? = null
    private var recordingStartTime: Long = 0
    private var currentOutputPath: String? = null
    private var isRecordingFlag = false

    override fun startRecording(outputPath: String): Boolean {
        return try {
            // Release any existing recorder
            release()

            currentOutputPath = outputPath
            recordingStartTime = System.currentTimeMillis()

            // Configure audio session for recording
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryRecord, null)
            audioSession.setActive(true, null)

            // Set up recording settings
            val settings = mapOf(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 2,
                AVEncoderAudioQualityKey to AVAudioQualityHigh
            )

            val url = NSURL.fileURLWithPath(outputPath)
            audioRecorder = AVAudioRecorder(url, settings, null).apply {
                prepareToRecord()
                record()
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
                audioRecorder?.stop()

                // Deactivate audio session
                val audioSession = AVAudioSession.sharedInstance()
                audioSession.setActive(false, null)

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
            audioRecorder?.stop()
            audioRecorder = null
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
 * Actual implementation of createAudioRecorder for iOS
 */
actual fun createAudioRecorder(): AudioRecorder {
    return IOSAudioRecorder()
}
