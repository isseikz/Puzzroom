package tokyo.isseikuzumaki.unison.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android implementation of AudioEngine
 * Uses AudioTrack for playback and AudioRecord for recording
 */
class AndroidAudioEngine : AudioEngine {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private val recordedDataStream = ByteArrayOutputStream()

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun startRecording(): Flow<ByteArray> = callbackFlow {
        withContext(Dispatchers.IO) {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            recordedDataStream.reset()
            isRecording = true

            audioRecord?.startRecording()

            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    recordedDataStream.write(chunk)
                    trySend(chunk)
                }
            }
        }

        awaitClose {
            stopRecording()
        }
    }

    override fun stopRecording() {
        isRecording = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    override fun getRecordedData(): ByteArray? {
        return recordedDataStream.toByteArray().takeIf { it.isNotEmpty() }
    }

    override suspend fun playDual(
        original: ByteArray,
        recorded: ByteArray,
        offsetMs: Int,
        balance: Float
    ): Result<Unit> {
        try {
            stopPlayback()

            // Calculate offset in bytes (16-bit = 2 bytes per sample)
            val offsetBytes = (offsetMs * SAMPLE_RATE * 2) / 1000

            // Prepare mixed audio data
            val mixedData = mixAudio(original, recorded, offsetBytes, balance)

            // Create and start AudioTrack
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            // Write audio in a background thread to avoid blocking
            CoroutineScope(Dispatchers.IO).launch {
                audioTrack?.write(mixedData, 0, mixedData.size)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun playOriginal(pcmData: ByteArray): Result<Unit> {
        try {
            stopPlayback()

            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            // Write audio in a background thread to avoid blocking
            CoroutineScope(Dispatchers.IO).launch {
                audioTrack?.write(pcmData, 0, pcmData.size)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun stopPlayback() {
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
    }

    override fun release() {
        stopRecording()
        stopPlayback()
    }

    override fun getCurrentPositionMs(): Long {
        return audioTrack?.playbackHeadPosition?.let { position ->
            (position.toLong() * 1000) / SAMPLE_RATE
        } ?: 0L
    }

    override fun getDurationMs(pcmData: ByteArray): Long {
        // 16-bit PCM = 2 bytes per sample
        val samples = pcmData.size / 2
        return (samples.toLong() * 1000) / SAMPLE_RATE
    }

    /**
     * Mix original and recorded audio with offset and balance
     * @param original Original PCM data
     * @param recorded Recorded PCM data
     * @param offsetBytes Offset in bytes (positive = delay recorded, negative = advance recorded)
     * @param balance Mix balance (0.0 = only original, 1.0 = only recorded, 0.5 = equal)
     */
    private fun mixAudio(
        original: ByteArray,
        recorded: ByteArray,
        offsetBytes: Int,
        balance: Float
    ): ByteArray {
        val originalGain = 1.0f - balance
        val recordedGain = balance

        // Determine output length
        val recordedStart = maxOf(0, offsetBytes)
        val recordedEnd = minOf(original.size, recorded.size + offsetBytes)
        val outputLength = maxOf(original.size, recorded.size + offsetBytes)

        val output = ByteArray(outputLength)

        // Process 16-bit samples (2 bytes each)
        for (i in 0 until outputLength step 2) {
            if (i >= outputLength - 1) break

            // Get original sample
            val origSample = if (i < original.size - 1) {
                (original[i].toInt() and 0xFF) or ((original[i + 1].toInt() shl 8))
            } else {
                0
            }

            // Get recorded sample with offset
            val recIndex = i - offsetBytes
            val recSample = if (recIndex >= 0 && recIndex < recorded.size - 1) {
                (recorded[recIndex].toInt() and 0xFF) or ((recorded[recIndex + 1].toInt() shl 8))
            } else {
                0
            }

            // Convert to signed 16-bit
            val origValue = if (origSample > 32767) origSample - 65536 else origSample
            val recValue = if (recSample > 32767) recSample - 65536 else recSample

            // Mix with balance
            val mixed = (origValue * originalGain + recValue * recordedGain).toInt()

            // Clamp to 16-bit range
            val clamped = mixed.coerceIn(-32768, 32767)

            // Convert back to bytes
            output[i] = (clamped and 0xFF).toByte()
            if (i + 1 < output.size) {
                output[i + 1] = ((clamped shr 8) and 0xFF).toByte()
            }
        }

        return output
    }

    companion object {
        private const val SAMPLE_RATE = 44100
    }
}
