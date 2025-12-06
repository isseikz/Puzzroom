package com.puzzroom.whisper

import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

private const val LOG_TAG = "WhisperAndroid"

class WhisperAndroid : Whisper {

    override fun getVersion(): String {
        return "whisper.cpp KMP" // Can be extended to get actual version
    }

    override fun initFromFile(modelPath: String): WhisperContext {
        return WhisperContextImpl.createContextFromFile(modelPath)
    }

    override fun isLanguageSupported(languageCode: String): Boolean {
        // Basic language support check - can be extended
        return true
    }

    /**
     * Initialize Whisper context from Android assets
     *
     * @param assetManager Android AssetManager instance
     * @param assetPath Path to model file in assets (e.g., "models/ggml-base.en.bin")
     * @return WhisperContext instance for transcription
     */
    fun initFromAsset(assetManager: AssetManager, assetPath: String): WhisperContext {
        return WhisperContextImpl.createContextFromAsset(assetManager, assetPath)
    }
}

class WhisperContextImpl private constructor(private var ptr: Long) : WhisperContext {
    // Meet Whisper C++ constraint: Don't access from more than one thread at a time.
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
    private var isClosed = false

    override fun transcribe(
        audioData: FloatArray,
        params: TranscriptionParams
    ): TranscriptionResult = runBlocking {
        withContext(scope.coroutineContext) {
            checkNotClosed()
            require(ptr != 0L)

            val numThreads = if (params.threads > 0) params.threads else WhisperCpuConfig.preferredThreadCount
            Log.d(LOG_TAG, "Selecting $numThreads threads")

            WhisperLib.fullTranscribe(ptr, numThreads, audioData)
            val textCount = WhisperLib.getTextSegmentCount(ptr)

            val segments = mutableListOf<TranscriptionSegment>()
            val fullTextBuilder = StringBuilder()

            for (i in 0 until textCount) {
                val text = WhisperLib.getTextSegment(ptr, i)
                val startTime = WhisperLib.getTextSegmentT0(ptr, i)
                val endTime = WhisperLib.getTextSegmentT1(ptr, i)

                segments.add(
                    TranscriptionSegment(
                        text = text,
                        startTimeMs = startTime * 10, // Convert to milliseconds
                        endTimeMs = endTime * 10,
                        index = i
                    )
                )
                fullTextBuilder.append(text)
            }

            TranscriptionResult(
                segments = segments,
                fullText = fullTextBuilder.toString()
            )
        }
    }

    override fun close() {
        if (!isClosed && ptr != 0L) {
            runBlocking {
                withContext(scope.coroutineContext) {
                    WhisperLib.freeContext(ptr)
                    ptr = 0L
                    isClosed = true
                    Log.i(LOG_TAG, "Whisper context closed")
                }
            }
        }
    }

    private fun checkNotClosed() {
        if (isClosed) {
            throw IllegalStateException("WhisperContext has been closed")
        }
    }

    protected fun finalize() {
        close()
    }

    companion object {
        fun createContextFromFile(filePath: String): WhisperContextImpl {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw WhisperException("Couldn't create context with path $filePath")
            }
            return WhisperContextImpl(ptr)
        }

        fun createContextFromAsset(assetManager: AssetManager, assetPath: String): WhisperContextImpl {
            val ptr = WhisperLib.initContextFromAsset(assetManager, assetPath)
            if (ptr == 0L) {
                throw WhisperException("Couldn't create context from asset $assetPath")
            }
            return WhisperContextImpl(ptr)
        }

        fun getSystemInfo(): String {
            return WhisperLib.getSystemInfo()
        }
    }
}

private class WhisperLib {
    companion object {
        init {
            Log.d(LOG_TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
            var loadVfpv4 = false
            var loadV8fp16 = false
            if (isArmEabiV7a()) {
                // armeabi-v7a needs runtime detection support
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("vfpv4")) {
                        Log.d(LOG_TAG, "CPU supports vfpv4")
                        loadVfpv4 = true
                    }
                }
            } else if (isArmEabiV8a()) {
                // ARMv8.2a needs runtime detection support
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("fphp")) {
                        Log.d(LOG_TAG, "CPU supports fp16 arithmetic")
                        loadV8fp16 = true
                    }
                }
            }

            if (loadVfpv4) {
                Log.d(LOG_TAG, "Loading libwhisper_vfpv4.so")
                System.loadLibrary("whisper_vfpv4")
            } else if (loadV8fp16) {
                Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                System.loadLibrary("whisper_v8fp16_va")
            } else {
                Log.d(LOG_TAG, "Loading libwhisper.so")
                System.loadLibrary("whisper")
            }
        }

        // JNI methods
        external fun initContext(modelPath: String): Long
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long
        external fun getSystemInfo(): String
        external fun benchMemcpy(nthread: Int): String
        external fun benchGgmlMulMat(nthread: Int): String
    }
}

private fun isArmEabiV7a(): Boolean {
    return Build.SUPPORTED_ABIS[0].equals("armeabi-v7a")
}

private fun isArmEabiV8a(): Boolean {
    return Build.SUPPORTED_ABIS[0].equals("arm64-v8a")
}

private fun cpuInfo(): String? {
    return try {
        File("/proc/cpuinfo").inputStream().bufferedReader().use {
            it.readText()
        }
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Couldn't read /proc/cpuinfo", e)
        null
    }
}

/**
 * Actual implementation for Android platform
 */
actual fun Whisper.Companion.create(): Whisper {
    return WhisperAndroid()
}
