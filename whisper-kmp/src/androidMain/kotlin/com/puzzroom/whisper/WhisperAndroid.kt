package com.puzzroom.whisper

import android.util.Log

private const val TAG = "WhisperAndroid"

/**
 * Android implementation of Whisper interface
 */
class WhisperAndroid : Whisper {

    init {
        try {
            System.loadLibrary("whisper_android")
            Log.i(TAG, "Whisper native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load whisper_android library", e)
            throw WhisperException("Failed to load native library", e)
        }
    }

    override fun getVersion(): String {
        return WhisperContextImpl.nativeGetVersion()
    }

    override fun initFromFile(modelPath: String): WhisperContext {
        return WhisperContextImpl(modelPath)
    }

    override fun isLanguageSupported(languageCode: String): Boolean {
        return WhisperContextImpl.nativeIsLanguageSupported(languageCode)
    }
}

/**
 * Android implementation of WhisperContext
 */
internal class WhisperContextImpl(modelPath: String) : WhisperContext {

    private var contextPtr: Long = 0L
    private var isClosed = false

    init {
        contextPtr = nativeInitFromFile(modelPath)
        if (contextPtr == 0L) {
            throw WhisperException("Failed to initialize Whisper context from: $modelPath")
        }
        Log.i(TAG, "Whisper context initialized from: $modelPath")
    }

    override fun transcribe(
        audioData: FloatArray,
        params: TranscriptionParams
    ): TranscriptionResult {
        checkNotClosed()

        // Create native params
        val paramsPtr = nativeGetDefaultParams(contextPtr, params.samplingStrategy.value)
        if (paramsPtr == 0L) {
            throw WhisperException("Failed to create transcription parameters")
        }

        try {
            // Set parameters
            nativeSetLanguage(paramsPtr, params.language)
            nativeSetTranslate(paramsPtr, params.translate)
            nativeSetThreads(paramsPtr, params.threads)

            // Perform transcription
            val result = nativeTranscribe(contextPtr, paramsPtr, audioData)
            if (result != 0) {
                throw WhisperException("Transcription failed with error code: $result")
            }

            // Extract results
            val segmentCount = nativeGetSegmentCount(contextPtr)
            val segments = mutableListOf<TranscriptionSegment>()

            for (i in 0 until segmentCount) {
                val text = nativeGetSegmentText(contextPtr, i)
                val startTime = nativeGetSegmentT0(contextPtr, i)
                val endTime = nativeGetSegmentT1(contextPtr, i)

                segments.add(
                    TranscriptionSegment(
                        text = text,
                        startTimeMs = startTime,
                        endTimeMs = endTime,
                        index = i
                    )
                )
            }

            val fullText = nativeGetFullText(contextPtr)

            Log.i(TAG, "Transcription completed: $segmentCount segments, ${fullText.length} chars")

            return TranscriptionResult(
                segments = segments,
                fullText = fullText
            )
        } finally {
            nativeFreeParams(paramsPtr)
        }
    }

    override fun close() {
        if (!isClosed && contextPtr != 0L) {
            nativeFree(contextPtr)
            contextPtr = 0L
            isClosed = true
            Log.i(TAG, "Whisper context closed")
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
        // Native method declarations
        @JvmStatic
        external fun nativeGetVersion(): String

        @JvmStatic
        external fun nativeInitFromFile(modelPath: String): Long

        @JvmStatic
        external fun nativeFree(contextPtr: Long)

        @JvmStatic
        external fun nativeGetDefaultParams(contextPtr: Long, strategy: Int): Long

        @JvmStatic
        external fun nativeFreeParams(paramsPtr: Long)

        @JvmStatic
        external fun nativeSetLanguage(paramsPtr: Long, language: String)

        @JvmStatic
        external fun nativeSetTranslate(paramsPtr: Long, translate: Boolean)

        @JvmStatic
        external fun nativeSetThreads(paramsPtr: Long, threads: Int)

        @JvmStatic
        external fun nativeTranscribe(contextPtr: Long, paramsPtr: Long, audioData: FloatArray): Int

        @JvmStatic
        external fun nativeGetSegmentCount(contextPtr: Long): Int

        @JvmStatic
        external fun nativeGetSegmentText(contextPtr: Long, segmentIndex: Int): String

        @JvmStatic
        external fun nativeGetSegmentT0(contextPtr: Long, segmentIndex: Int): Long

        @JvmStatic
        external fun nativeGetSegmentT1(contextPtr: Long, segmentIndex: Int): Long

        @JvmStatic
        external fun nativeGetFullText(contextPtr: Long): String

        @JvmStatic
        external fun nativeIsLanguageSupported(language: String): Boolean
    }
}

/**
 * Actual implementation for Android platform
 */
actual fun Whisper.Companion.create(): Whisper = WhisperAndroid()
