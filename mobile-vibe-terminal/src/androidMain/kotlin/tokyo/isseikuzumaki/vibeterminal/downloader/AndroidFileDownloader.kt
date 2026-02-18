package tokyo.isseikuzumaki.vibeterminal.downloader

import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import timber.log.Timber
import tokyo.isseikuzumaki.vibeterminal.VibeTerminalApp
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import java.io.File

/**
 * Android implementation of FileDownloader.
 * Uses scoped storage for Android 10+ and legacy external storage for older versions.
 */
class AndroidFileDownloader : FileDownloader {

    private val context get() = VibeTerminalApp.applicationContext

    override fun getDownloadDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use app-specific external files directory
            // Files will be visible in file managers but scoped to the app
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } else {
            // For older versions, use public Downloads directory
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
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
                // Safety limit to prevent infinite loop
                throw IllegalStateException("Too many duplicate files")
            }
        }

        return file
    }

    override suspend fun notifyDownloadComplete(file: File, mimeType: String) {
        Timber.d("Notifying download complete: ${file.absolutePath}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, add to MediaStore Downloads collection
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }

                context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                Timber.d("Added to MediaStore Downloads")
            } catch (e: Exception) {
                Timber.w(e, "Failed to add to MediaStore, file still accessible in app files")
            }
        } else {
            // For older versions, trigger media scan
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Timber.d("Media scan complete: $path -> $uri")
            }
        }
    }
}
