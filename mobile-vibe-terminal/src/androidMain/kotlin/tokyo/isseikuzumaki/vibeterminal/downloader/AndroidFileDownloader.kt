package tokyo.isseikuzumaki.vibeterminal.downloader

import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import okio.Path
import okio.Path.Companion.toOkioPath
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

    override fun getDownloadDirectory(): Path {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir).toOkioPath()
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toOkioPath()
        }
    }

    override fun generateUniqueFilename(directory: Path, originalName: String): Path {
        var file = File(directory.toFile(), originalName)
        if (!file.exists()) return file.toOkioPath()

        val nameWithoutExtension = originalName.substringBeforeLast(".", originalName)
        val extension = if (originalName.contains(".")) {
            "." + originalName.substringAfterLast(".")
        } else {
            ""
        }

        var counter = 1
        while (file.exists()) {
            file = File(directory.toFile(), "$nameWithoutExtension ($counter)$extension")
            counter++
            if (counter > 999) {
                throw IllegalStateException("Too many duplicate files")
            }
        }

        return file.toOkioPath()
    }

    override suspend fun notifyDownloadComplete(file: Path, mimeType: String) {
        val javaFile = file.toFile()
        Timber.d("Notifying download complete: ${javaFile.absolutePath}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, javaFile.name)
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
            MediaScannerConnection.scanFile(
                context,
                arrayOf(javaFile.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Timber.d("Media scan complete: $path -> $uri")
            }
        }
    }
}
