package com.puzzroom.whisper

import android.os.Build

/**
 * CPU configuration helper for Whisper
 * Determines optimal thread count based on device capabilities
 */
object WhisperCpuConfig {
    /**
     * Get the preferred number of threads for Whisper processing
     * Based on available CPU cores
     */
    val preferredThreadCount: Int by lazy {
        maxOf(Runtime.getRuntime().availableProcessors() - 1, 1)
    }

    /**
     * Get device information
     */
    val deviceInfo: String by lazy {
        buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
            append("Cores: ${Runtime.getRuntime().availableProcessors()}\n")
            append("Preferred threads: $preferredThreadCount")
        }
    }
}
