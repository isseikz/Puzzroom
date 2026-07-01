package tokyo.isseikuzumaki.vibeterminal.downloader

import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import java.io.File

/**
 * JVM/Desktop implementation of FileDownloader.
 * Downloads files to the user's Downloads folder.
 */
class JvmFileDownloader : FileDownloader {
    override fun getDownloadDirectory(): File {
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }

    override fun generateUniqueFilename(directory: File, originalName: String): File {
        var file = File(directory, originalName)
        if (!file.exists()) {
            return file
        }

        val nameWithoutExtension = originalName.substringBeforeLast(".", originalName)
        val extension = if (originalName.contains(".")) {
            "." + originalName.substringAfterLast(".")
        } else {
            ""
        }

        var counter = 1
        while (file.exists()) {
            file = File(directory, "$nameWithoutExtension ($counter)$extension")
            counter++
            if (counter > 999) {
                throw IllegalStateException("Too many duplicate files")
            }
        }

        return file
    }

    override suspend fun notifyDownloadComplete(file: File, mimeType: String) {
        // No-op on desktop - files are directly accessible in Downloads folder
    }
}
