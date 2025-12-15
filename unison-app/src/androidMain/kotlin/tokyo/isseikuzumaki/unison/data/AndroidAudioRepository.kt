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

    override suspend fun transcribeAudio(
        pcmData: ByteArray,
        sampleRate: Int
    ): Result<List<TranscriptionSegment>> {
        // Transcription from audio is not supported in this version
        // Use loadTranscriptionFromFile() instead
        return Result.failure(Exception("Audio transcription not supported. Please provide a transcription file."))
    }

    override suspend fun loadTranscriptionFromFile(uri: String): Result<List<TranscriptionSegment>> = withContext(Dispatchers.IO) {
        try {
            val androidUri = Uri.parse(uri)
            val text = context.contentResolver.openInputStream(androidUri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Failed to open file"))

            // Parse JSON format: [{"start": 0.0, "end": 1.5, "text": "hello"}, ...]
            val segments = parseTranscriptionJson(text)
            Result.success(segments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTranscriptionJson(jsonText: String): List<TranscriptionSegment> {
        // Simple JSON parsing without external library
        // Expected format: [{"start":0.0,"end":1.5,"text":"hello"},...]
        val segments = mutableListOf<TranscriptionSegment>()

        // Remove whitespace and outer brackets
        val cleaned = jsonText.trim().removePrefix("[").removeSuffix("]")
        if (cleaned.isEmpty()) return segments

        // Split by objects (rough parsing)
        var currentPos = 0
        var braceCount = 0
        var currentObject = StringBuilder()

        for (char in cleaned) {
            when (char) {
                '{' -> {
                    braceCount++
                    currentObject.append(char)
                }
                '}' -> {
                    braceCount--
                    currentObject.append(char)
                    if (braceCount == 0 && currentObject.isNotEmpty()) {
                        parseSegmentObject(currentObject.toString())?.let { segments.add(it) }
                        currentObject.clear()
                    }
                }
                else -> {
                    if (braceCount > 0) {
                        currentObject.append(char)
                    }
                }
            }
        }

        return segments
    }

    private fun parseSegmentObject(obj: String): TranscriptionSegment? {
        try {
            // Extract values using simple string parsing
            var start = 0L
            var end = 0L
            var text = ""
            var index = 0

            // Find "start": value
            val startMatch = """"start"\s*:\s*([\d.]+)""".toRegex().find(obj)
            if (startMatch != null) {
                start = (startMatch.groupValues[1].toDouble() * 1000).toLong()
            }

            // Find "end": value
            val endMatch = """"end"\s*:\s*([\d.]+)""".toRegex().find(obj)
            if (endMatch != null) {
                end = (endMatch.groupValues[1].toDouble() * 1000).toLong()
            }

            // Find "text": "value"
            val textMatch = """"text"\s*:\s*"([^"]*)"""".toRegex().find(obj)
            if (textMatch != null) {
                text = textMatch.groupValues[1]
            }

            // Find "index": value (optional)
            val indexMatch = """"index"\s*:\s*(\d+)""".toRegex().find(obj)
            if (indexMatch != null) {
                index = indexMatch.groupValues[1].toInt()
            }

            return TranscriptionSegment(
                text = text,
                startTimeMs = start,
                endTimeMs = end,
                index = index
            )
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}
