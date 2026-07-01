package tokyo.isseikuzumaki.vibeterminal.domain.downloader

import java.io.File

/**
 * Platform-specific file download manager.
 * Handles saving downloaded files to appropriate platform locations.
 */
interface FileDownloader {
    /**
     * Get the default download directory for the platform.
     * @return The download directory
     */
    fun getDownloadDirectory(): File

    /**
     * Generate a unique local filename to avoid overwrites.
     * Appends (1), (2), etc. if file already exists.
     * @param directory The directory to check for existing files
     * @param originalName The original filename
     * @return A File with a unique name in the directory
     */
    fun generateUniqueFilename(directory: File, originalName: String): File

    /**
     * Notify the system that a new file was downloaded.
     * On Android, this adds to Downloads and triggers media scan.
     * On other platforms, this may be a no-op.
     * @param file The downloaded file
     * @param mimeType The MIME type of the file
     */
    suspend fun notifyDownloadComplete(file: File, mimeType: String)
}
