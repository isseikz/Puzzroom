package tokyo.isseikuzumaki.vibeterminal.downloader

import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import java.io.File

/**
 * iOS stub implementation of FileDownloader.
 * File download is not yet supported on iOS.
 */
class FileDownloaderStub : FileDownloader {
    override fun getDownloadDirectory(): File {
        throw NotImplementedError("File download not supported on iOS yet")
    }

    override fun generateUniqueFilename(directory: File, originalName: String): File {
        throw NotImplementedError("File download not supported on iOS yet")
    }

    override suspend fun notifyDownloadComplete(file: File, mimeType: String) {
        // No-op on iOS
    }
}
