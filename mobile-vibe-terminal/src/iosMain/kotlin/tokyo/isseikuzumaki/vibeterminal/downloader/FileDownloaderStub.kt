package tokyo.isseikuzumaki.vibeterminal.downloader

import okio.Path
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader

/**
 * iOS stub implementation of FileDownloader.
 * File download is not yet supported on iOS.
 */
class FileDownloaderStub : FileDownloader {
    override fun getDownloadDirectory(): Path {
        throw NotImplementedError("File download not supported on iOS yet")
    }

    override fun generateUniqueFilename(directory: Path, originalName: String): Path {
        throw NotImplementedError("File download not supported on iOS yet")
    }

    override suspend fun notifyDownloadComplete(file: Path, mimeType: String) {
        // No-op on iOS
    }
}
