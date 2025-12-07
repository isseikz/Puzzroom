package tokyo.isseikuzumaki.unison.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.puzzroom.whisper.Whisper
import com.puzzroom.whisper.WhisperAndroid
import com.puzzroom.whisper.WhisperContext
import com.puzzroom.whisper.TranscriptionParams
import com.puzzroom.whisper.TranscriptionSegment
import io.github.aakira.napier.Napier

/**
 * Android implementation of AudioRepository
 * Uses MediaExtractor and MediaCodec to decode audio files to PCM
 */
class AndroidAudioRepository(
    private val context: Context
) : AudioRepository {

    private var whisperContext: WhisperContext? = null

    init {
        // Initialize Whisper model on creation
        try {
            val whisper = WhisperAndroid()
            whisperContext = whisper.initFromAsset(
                assetManager = context.assets,
                assetPath = "models/ggml-base.en.bin"
            )
        } catch (e: Exception) {
            // Log error but don't crash - transcription will fail gracefully
            android.util.Log.e("AndroidAudioRepository", "Failed to initialize Whisper", e)
        }
    }

    override suspend fun validateAudioFile(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val androidUri = Uri.parse(uri)
            val mimeType = context.contentResolver.getType(androidUri)
            mimeType?.startsWith("audio/") == true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun loadPcmData(uri: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val androidUri = Uri.parse(uri)
            val extractor = MediaExtractor()

            context.contentResolver.openFileDescriptor(androidUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)

                // Find audio track
                val trackIndex = findAudioTrack(extractor)
                    ?: return@withContext Result.failure(Exception("No audio track found"))

                extractor.selectTrack(trackIndex)
                val format = extractor.getTrackFormat(trackIndex)

                // Create decoder
                val mime = format.getString(MediaFormat.KEY_MIME)
                    ?: return@withContext Result.failure(Exception("No MIME type found"))

                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val pcmData = decodeToPcm(extractor, codec)

                codec.stop()
                codec.release()
                extractor.release()

                Result.success(pcmData)
            } ?: Result.failure(Exception("Failed to open file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return null
    }

    private fun decodeToPcm(extractor: MediaExtractor, codec: MediaCodec): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS) {
            // Feed input
            if (!sawInputEOS) {
                val inputBufferId = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    inputBuffer?.let {
                        val sampleSize = extractor.readSampleData(it, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }
            }

            // Get output
            val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputBufferId >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferId)

                if (bufferInfo.size > 0 && outputBuffer != null) {
                    // Convert to byte array
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputStream.write(chunk)
                }

                codec.releaseOutputBuffer(outputBufferId, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
            }
        }

        return outputStream.toByteArray()
    }

    override suspend fun transcribeAudio(
        pcmData: ByteArray,
        sampleRate: Int
    ): Result<List<TranscriptionSegment>> = withContext(Dispatchers.IO) {
        Napier.i { "Start transcribe audio" }
        try {
            val context = whisperContext
                ?: return@withContext Result.failure(Exception("Whisper not initialized"))

            // Convert PCM16 to Float32 for Whisper
            val audioFloat = convertPcm16ToFloat32(pcmData)

            // Resample to 16kHz if needed (Whisper requirement)
            val audioResampled = if (sampleRate != 16000) {
                resampleTo16kHz(audioFloat, sampleRate)
            } else {
                audioFloat
            }

            // Transcribe with timestamps
            val result = context.transcribe(
                audioData = audioResampled,
                params = TranscriptionParams(
                    language = "en",
                    threads = 0, // Auto-detect
                    printTimestamps = true
                )
            )

            Result.success(result.segments)
        } catch (e: Exception) {
            android.util.Log.e("AndroidAudioRepository", "Transcription failed", e)
            Result.failure(e)
        }
    }

    /**
     * Convert PCM16 to Float32 format required by Whisper
     * Processes in chunks to avoid OutOfMemoryError with large audio files
     */
    private fun convertPcm16ToFloat32(pcm16: ByteArray): FloatArray {
        val numSamples = pcm16.size / 2
        val result = FloatArray(numSamples)

        // Process in 1MB chunks to avoid memory issues
        val chunkSize = 1024 * 1024 / 2 // 512K samples (1MB of bytes)
        val buffer = ByteBuffer.allocate(minOf(chunkSize * 2, pcm16.size))
            .order(ByteOrder.LITTLE_ENDIAN)

        var offset = 0
        var resultIndex = 0

        while (offset < pcm16.size) {
            val bytesToRead = minOf(chunkSize * 2, pcm16.size - offset)
            buffer.clear()
            buffer.put(pcm16, offset, bytesToRead)
            buffer.flip()

            val shortBuffer = buffer.asShortBuffer()
            val samplesInChunk = bytesToRead / 2

            for (i in 0 until samplesInChunk) {
                result[resultIndex++] = shortBuffer.get(i) / 32768.0f
            }

            offset += bytesToRead
        }

        return result
    }

    /**
     * Resample audio to 16kHz (Whisper requirement)
     * Uses simple linear interpolation
     */
    private fun resampleTo16kHz(audioData: FloatArray, originalSampleRate: Int): FloatArray {
        if (originalSampleRate == 16000) {
            return audioData
        }

        val ratio = originalSampleRate / 16000.0
        val newSize = (audioData.size / ratio).toInt()

        return FloatArray(newSize) { i ->
            val srcIndex = (i * ratio).toInt()
            if (srcIndex < audioData.size) audioData[srcIndex] else 0f
        }
    }

    fun cleanup() {
        whisperContext?.close()
        whisperContext = null
    }

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}
