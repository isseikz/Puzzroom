package tokyo.isseikuzumaki.vibeterminal.downloader

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader

/**
 * iOS implementation of FileDownloader.
 * Downloads go to the app's Documents/Downloads directory, accessible via Files app.
 */
@OptIn(ExperimentalForeignApi::class)
class FileDownloaderIos : FileDownloader {

    override fun getDownloadDirectory(): Path {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val docPath = requireNotNull(documentDirectory?.path) { "Could not find Documents directory" }
        val downloadsPath = "$docPath/Downloads"

        // Ensure the Downloads subdirectory exists
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(downloadsPath)) {
            fm.createDirectoryAtPath(
                path = downloadsPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        return downloadsPath.toPath()
    }

    override fun generateUniqueFilename(directory: Path, originalName: String): Path {
        val base = directory.toString()
        val candidate = "$base/$originalName"

        if (!NSFileManager.defaultManager.fileExistsAtPath(candidate)) {
            return candidate.toPath()
        }

        val nameWithoutExt = originalName.substringBeforeLast(".", originalName)
        val extension = if (originalName.contains(".")) ".${originalName.substringAfterLast(".")}" else ""

        var counter = 1
        while (counter <= 999) {
            val newPath = "$base/$nameWithoutExt ($counter)$extension"
            if (!NSFileManager.defaultManager.fileExistsAtPath(newPath)) {
                return newPath.toPath()
            }
            counter++
        }

        throw IllegalStateException("Too many duplicate files for: $originalName")
    }

    override suspend fun notifyDownloadComplete(file: Path, mimeType: String) {
        // No system-wide download notification needed on iOS.
        // Files are accessible via the Files app automatically when placed in Documents/.
    }
}
