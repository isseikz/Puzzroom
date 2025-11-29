package com.puzzroom.whisper

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.InputStream

/**
 * Utility functions for audio processing and model management
 */
object AudioUtils {

    /**
     * Convert PCM16 audio data to Float32 format required by Whisper
     *
     * @param pcm16 16-bit PCM audio data
     * @return Float32 audio data normalized to [-1.0, 1.0]
     */
    fun convertPcm16ToFloat32(pcm16: ShortArray): FloatArray {
        return FloatArray(pcm16.size) { i ->
            pcm16[i] / 32768.0f
        }
    }

    /**
     * Convert PCM16 ByteArray to Float32 format
     *
     * @param pcm16Bytes PCM16 audio as byte array (little-endian)
     * @return Float32 audio data normalized to [-1.0, 1.0]
     */
    fun convertPcm16BytesToFloat32(pcm16Bytes: ByteArray): FloatArray {
        val shorts = ShortArray(pcm16Bytes.size / 2)
        for (i in shorts.indices) {
            val low = pcm16Bytes[i * 2].toInt() and 0xFF
            val high = pcm16Bytes[i * 2 + 1].toInt() shl 8
            shorts[i] = (low or high).toShort()
        }
        return convertPcm16ToFloat32(shorts)
    }

    /**
     * Resample audio to 16kHz (simple downsampling)
     * Note: This is a basic implementation. For production, consider using a proper resampling library
     *
     * @param audio Input audio data
     * @param inputSampleRate Input sample rate
     * @param outputSampleRate Output sample rate (default 16000)
     * @return Resampled audio
     */
    fun resample(
        audio: FloatArray,
        inputSampleRate: Int,
        outputSampleRate: Int = 16000
    ): FloatArray {
        if (inputSampleRate == outputSampleRate) {
            return audio
        }

        val ratio = inputSampleRate.toDouble() / outputSampleRate.toDouble()
        val outputSize = (audio.size / ratio).toInt()
        val output = FloatArray(outputSize)

        for (i in output.indices) {
            val srcIndex = (i * ratio).toInt()
            if (srcIndex < audio.size) {
                output[i] = audio[srcIndex]
            }
        }

        return output
    }

    /**
     * Mix stereo audio to mono
     *
     * @param stereo Stereo audio data (interleaved L/R)
     * @return Mono audio data
     */
    fun stereoToMono(stereo: FloatArray): FloatArray {
        val mono = FloatArray(stereo.size / 2)
        for (i in mono.indices) {
            mono[i] = (stereo[i * 2] + stereo[i * 2 + 1]) / 2.0f
        }
        return mono
    }

    /**
     * Copy a model file from assets to internal storage
     *
     * @param context Android context
     * @param assetFileName Name of the model file in assets
     * @return Absolute path to the copied model file
     */
    fun copyModelFromAssets(context: Context, assetFileName: String): String {
        val file = File(context.filesDir, assetFileName)

        if (!file.exists()) {
            context.assets.open(assetFileName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file.absolutePath
    }

    /**
     * Copy model from an input stream to internal storage
     *
     * @param context Android context
     * @param inputStream Input stream of the model file
     * @param fileName Destination file name
     * @return Absolute path to the copied model file
     */
    fun copyModelFromStream(
        context: Context,
        inputStream: InputStream,
        fileName: String
    ): String {
        val file = File(context.filesDir, fileName)

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    /**
     * Get the minimum buffer size for AudioRecord at 16kHz
     *
     * @return Minimum buffer size in bytes
     */
    fun getMinBufferSize(): Int {
        return AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }

    /**
     * Create an AudioRecord instance configured for Whisper (16kHz, mono)
     *
     * @param audioSource Audio source (default: MIC)
     * @param bufferSize Buffer size in bytes (default: minimum * 4)
     * @return Configured AudioRecord instance
     */
    fun createAudioRecord(
        audioSource: Int = MediaRecorder.AudioSource.MIC,
        bufferSize: Int = getMinBufferSize() * 4
    ): AudioRecord {
        return AudioRecord(
            audioSource,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }

    /**
     * Check if the model file exists
     *
     * @param modelPath Path to the model file
     * @return true if the file exists and is readable
     */
    fun isModelFileValid(modelPath: String): Boolean {
        val file = File(modelPath)
        return file.exists() && file.canRead() && file.length() > 0
    }

    /**
     * Get the size of a model file in MB
     *
     * @param modelPath Path to the model file
     * @return Size in megabytes
     */
    fun getModelFileSizeMB(modelPath: String): Double {
        val file = File(modelPath)
        return file.length() / (1024.0 * 1024.0)
    }
}
