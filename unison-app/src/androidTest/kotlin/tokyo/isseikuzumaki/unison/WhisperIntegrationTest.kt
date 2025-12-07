package tokyo.isseikuzumaki.unison

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tokyo.isseikuzumaki.unison.data.AndroidAudioRepository
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * Whisper Integration Test
 *
 * Tests the complete flow of:
 * 1. Loading an audio file (BBC Learning English MP3)
 * 2. Transcribing with Whisper.cpp
 * 3. Verifying no OutOfMemoryError occurs
 * 4. Verifying transcription results are returned
 *
 * This test uses real audio file and real Whisper model to ensure
 * the integration works correctly in production-like conditions.
 */
@RunWith(AndroidJUnit4::class)
class WhisperIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var testAudioFile: File

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Copy test audio from androidTest/assets to app's cache directory
        // This simulates a user selecting an audio file
        testAudioFile = File(context.cacheDir, "test_audio.mp3")
        context.assets.open("test_audio.mp3").use { input ->
            FileOutputStream(testAudioFile).use { output ->
                input.copyTo(output)
            }
        }

        testAudioFile.exists() shouldBe true
        println("✅ Test audio file prepared: ${testAudioFile.absolutePath} (${testAudioFile.length()} bytes)")
    }

    @Test
    fun transcribeRealAudioFile_shouldNotCauseOutOfMemoryError() = runBlocking {
        // Given: Real audio repository with Whisper context
        val audioRepository = AndroidAudioRepository(context)

        // Load audio file and convert to PCM data
        println("📂 Loading audio file...")
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(testAudioFile.absolutePath)
            prepare()
        }
        val durationMs = mediaPlayer.duration.toLong()
        mediaPlayer.release()

        println("⏱️  Audio duration: ${durationMs}ms (${durationMs / 1000.0}s)")

        // Convert MP3 to PCM using MediaCodec or AudioRecord simulation
        // For this test, we'll use a simplified approach: decode with MediaPlayer
        val pcmData = decodeToPCM(testAudioFile)
        println("🎵 Decoded to PCM: ${pcmData.size} bytes (${pcmData.size / 1024 / 1024.0f} MB)")

        // When: Transcribe the audio
        println("🎤 Starting Whisper transcription...")
        var transcriptionSucceeded = false
        var exceptionThrown: Throwable? = null

        val result = try {
            audioRepository.transcribeAudio(pcmData, sampleRate = 44100)
        } catch (e: OutOfMemoryError) {
            exceptionThrown = e
            Result.failure(e)
        } catch (e: Exception) {
            exceptionThrown = e
            Result.failure(e)
        }

        // Then: Verify no OutOfMemoryError occurred
        exceptionThrown shouldBe null
        result.isSuccess shouldBe true

        result.onSuccess { segments ->
            transcriptionSucceeded = true
            println("✅ Transcription completed successfully!")
            println("📊 Number of segments: ${segments.size}")

            // Verify we got some transcription results
            segments.size shouldNotBe 0

            // Log first few segments
            segments.take(5).forEach { segment ->
                println("   [${segment.startTimeMs}ms - ${segment.endTimeMs}ms] ${segment.text}")
            }

            // Verify segments have valid data
            segments.forEach { segment ->
                segment.text.isNotBlank() shouldBe true
                segment.startTimeMs shouldNotBe null
                segment.endTimeMs shouldNotBe null
                segment.endTimeMs shouldBe greaterThan(segment.startTimeMs)
            }
        }

        result.onFailure { error ->
            println("❌ Transcription failed: ${error.message}")
            error.printStackTrace()
        }

        transcriptionSucceeded shouldBe true
    }

    /**
     * Decode MP3 to PCM ByteArray using MediaExtractor and MediaCodec
     */
    private fun decodeToPCM(audioFile: File): ByteArray {
        val extractor = android.media.MediaExtractor()
        extractor.setDataSource(audioFile.absolutePath)

        // Find audio track
        var trackIndex = -1
        var format: android.media.MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(android.media.MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = trackFormat
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            throw IllegalArgumentException("No audio track found in file")
        }

        extractor.selectTrack(trackIndex)

        // Create decoder
        val mime = format.getString(android.media.MediaFormat.KEY_MIME)!!
        val codec = android.media.MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmData = mutableListOf<Byte>()
        val bufferInfo = android.media.MediaCodec.BufferInfo()
        var isEOS = false

        while (!isEOS) {
            // Feed input
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)

                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                    extractor.advance()
                }
            }

            // Get output
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)!!

                if (bufferInfo.size > 0) {
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.get(chunk, 0, bufferInfo.size)
                    pcmData.addAll(chunk.toList())
                }

                codec.releaseOutputBuffer(outputIndex, false)

                if ((bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEOS = true
                }
            } else if (outputIndex == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                println("🔄 Output format changed: $newFormat")
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return pcmData.toByteArray()
    }

    private infix fun Long.greaterThan(other: Long): Boolean = this > other
}
