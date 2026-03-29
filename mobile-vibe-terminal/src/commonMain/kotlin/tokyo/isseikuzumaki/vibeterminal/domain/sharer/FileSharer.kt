package tokyo.isseikuzumaki.vibeterminal.domain.sharer

import okio.Path

/**
 * Platform-specific file sharing interface.
 * Handles sharing files via the system share sheet.
 */
interface FileSharer {
    /**
     * Get the temporary directory for share preparation.
     * Files are downloaded here before being shared.
     * @return The cache directory path for temporary files
     */
    fun getShareCacheDirectory(): Path

    /**
     * Share a file using the system share sheet.
     * @param file Local file path to share
     * @param mimeType MIME type of the file
     * @return Result indicating success or failure
     */
    suspend fun shareFile(file: Path, mimeType: String): Result<Unit>

    /**
     * Clean up temporary files created for sharing.
     * Should be called periodically or after sharing completes.
     */
    suspend fun cleanupShareCache()
}
