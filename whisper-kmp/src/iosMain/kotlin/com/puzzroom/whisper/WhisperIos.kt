package com.puzzroom.whisper

/**
 * iOS implementation of Whisper interface (Stub)
 */
class WhisperIos : Whisper {
    override fun getVersion(): String {
        return "Stub"
    }

    override fun initFromFile(modelPath: String): WhisperContext {
        throw UnsupportedOperationException("Whisper is not yet supported on iOS")
    }

    override fun isLanguageSupported(languageCode: String): Boolean {
        return false
    }
}

/**
 * Actual implementation for iOS platform
 */
actual fun Whisper.Companion.create(): Whisper = WhisperIos()
