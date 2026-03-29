package tokyo.isseikuzumaki.vibeterminal.domain.downloader

import okio.Path

/**
 * Platform-specific file download manager.
 * Handles saving downloaded files to appropriate platform locations.
 */
interface FileDownloader {
    /**
     * Get the default download directory for the platform.
     * @return The download directory path
     */
    fun getDownloadDirectory(): Path

    /**
     * Generate a unique local filename to avoid overwrites.
     * Appends (1), (2), etc. if file already exists.
     * @param directory The directory to check for existing files
     * @param originalName The original filename
     * @return A Path with a unique name in the directory
     */
    fun generateUniqueFilename(directory: Path, originalName: String): Path

    /**
     * Notify the system that a new file was downloaded.
     * On Android, this adds to Downloads and triggers media scan.
     * On iOS, this is a no-op (file is already visible in Files app).
     * @param file The downloaded file path
     * @param mimeType The MIME type of the file
     */
    suspend fun notifyDownloadComplete(file: Path, mimeType: String)
}
