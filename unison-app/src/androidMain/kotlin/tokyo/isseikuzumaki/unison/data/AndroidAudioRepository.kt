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

/**
 * Android implementation of AudioRepository
 * Uses MediaExtractor and MediaCodec to decode audio files to PCM
 */
class AndroidAudioRepository(
    private val context: Context
) : AudioRepository {

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

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}
