package tokyo.isseikuzumaki.quickdeploy.model

/**
 * Represents the progress of an APK download.
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val startTimeMillis: Long
) {
    /**
     * Returns the download progress as a fraction between 0.0 and 1.0.
     */
    val progress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f

    /**
     * Returns the estimated remaining time in milliseconds, or -1 if cannot be calculated.
     */
    val estimatedRemainingTimeMillis: Long
        get() {
            if (bytesDownloaded <= 0 || totalBytes <= 0) return -1
            val elapsedTime = System.currentTimeMillis() - startTimeMillis
            if (elapsedTime <= 0) return -1
            val bytesPerMs = bytesDownloaded.toDouble() / elapsedTime.toDouble()
            if (bytesPerMs <= 0) return -1
            val remainingBytes = totalBytes - bytesDownloaded
            return (remainingBytes / bytesPerMs).toLong()
        }
}
